package com.amlet.callblocker.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.provider.CallLog
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telephony.SubscriptionManager
import androidx.core.app.NotificationCompat
import com.amlet.callblocker.CallBlockerApp
import com.amlet.callblocker.MainActivity
import com.amlet.callblocker.R
import com.amlet.callblocker.data.db.AppDatabase
import com.amlet.callblocker.data.db.BlockedCallEntity
import com.amlet.callblocker.data.prefs.AppPreferences
import com.amlet.callblocker.data.repository.ContactRepository
import com.amlet.callblocker.util.PhoneUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import com.amlet.callblocker.util.NotificationHelper

class CallBlockerService : CallScreeningService() {

    private lateinit var repository: ContactRepository
    private lateinit var prefs: AppPreferences
    private lateinit var db: AppDatabase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        db = try {
            (application as CallBlockerApp).database
        } catch (e: Exception) {
            AppDatabase.getInstance(applicationContext)
        }
        repository = ContactRepository(db.contactDao())
        prefs = AppPreferences(applicationContext)
        createNotificationChannel()
        // Auto-populate SIM→accountId map from existing call history (runs once).
        serviceScope.launch { buildSimMapFromCallLog() }
    }

    override fun onScreenCall(callDetails: Call.Details) {
        val rawNumber = callDetails.handle?.schemeSpecificPart
        android.util.Log.d("CallBlocker", "=== SCREEN CALL: $rawNumber ===")
        android.util.Log.d("CallBlocker", "accountHandle id: ${callDetails.accountHandle?.id}")

        // Ignore outgoing calls — never block or log them.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                if (callDetails.callDirection == Call.Details.DIRECTION_OUTGOING) {
                    android.util.Log.d("CallBlocker", "Outgoing call → SKIP")
                    respondToCall(callDetails, buildAllowResponse())
                    return
                }
            } catch (_: Exception) { }
        }

        val response = try {
            when {
                prefs.isSuspended -> {
                    android.util.Log.d("CallBlocker", "Protection suspended → ALLOW")
                    buildAllowResponse()
                }
                rawNumber.isNullOrBlank() -> {
                    android.util.Log.d("CallBlocker", "Empty number → BLOCK")
                    logBlockedCall("Unknown number", callDetails)
                    notifyBlocked("Unknown number")
                    buildBlockResponse()
                }
                !shouldProtectThisSim(callDetails) -> {
                    android.util.Log.d("CallBlocker", "SIM not protected → ALLOW")
                    buildAllowResponse()
                }
                else -> {
                    val normalised     = PhoneUtils.normalize(rawNumber)
                    val inContacts     = isInSystemContacts(rawNumber)
                    val whitelistEntry = runBlocking { repository.findByNumber(rawNumber) }
                    val inWhitelist    = whitelistEntry != null && !whitelistEntry.isExpired

                    // ── STIR/SHAKEN verification status (API 31+) ──────────────
                    // Constants from Call.Details (API 31): PASSED=1, FAILED=2, NOT_VERIFIED=3.
                    // We read them via reflection to avoid a hard API-31 compile dependency
                    // while compileSdk = 34 and minSdk = 29.
                    val verificationStatus: String? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        try {
                            val statusInt = callDetails.callerNumberVerificationStatus
                            when (statusInt) {
                                1    -> BlockedCallEntity.VERIF_PASSED        // VERIFICATION_STATUS_PASSED
                                2    -> BlockedCallEntity.VERIF_FAILED        // VERIFICATION_STATUS_FAILED
                                3    -> BlockedCallEntity.VERIF_NOT_VERIFIED  // VERIFICATION_STATUS_NOT_VERIFIED
                                else -> null
                            }
                        } catch (_: Exception) { null }
                    } else null

                    // ── Block on STIR/SHAKEN FAILED (if enabled) ───────────────
                    if (prefs.blockOnVerificationFailed &&
                        verificationStatus == BlockedCallEntity.VERIF_FAILED &&
                        shouldProtectThisSimForFeature(callDetails, prefs.stirShakenSimTarget)) {
                        android.util.Log.d("CallBlocker", "→ BLOCK (STIR/SHAKEN FAILED)")
                        logBlockedCall(rawNumber, callDetails, verificationStatus = verificationStatus)
                        notifyBlocked(rawNumber)
                        respondToCall(callDetails, buildBlockResponse())
                        return
                    }

                    android.util.Log.d("CallBlocker", "inContacts=$inContacts inWhitelist=$inWhitelist verif=$verificationStatus")

                    if (inContacts || inWhitelist) {
                        if (whitelistEntry?.isExpired == true) {
                            serviceScope.launch { repository.delete(whitelistEntry) }
                        }
                        if (inWhitelist && !inContacts && prefs.notifyOnWhitelistCall) {
                            NotificationHelper.notifyWhitelistCall(
                                this,
                                rawNumber,
                                whitelistEntry?.name
                            )
                        }
                        buildAllowResponse()
                    } else {
                        // ── Dialed-number whitelist ────────────────────────────
                        if (prefs.dialedNumberWhitelistEnabled &&
                            shouldProtectThisSimForFeature(callDetails, prefs.dialedWhitelistSimTarget)) {
                            val windowMs   = prefs.dialedWindowHours * 3_600_000L
                            val sinceMs    = System.currentTimeMillis() - windowMs
                            val wasDialed  = wasDialedRecently(rawNumber, sinceMs)
                            if (wasDialed) {
                                android.util.Log.d("CallBlocker", "→ ALLOW (dialed recently)")
                                logAllowedCall(rawNumber, callDetails, BlockedCallEntity.ALLOW_OUTGOING_RECENT, verificationStatus)
                                respondToCall(callDetails, buildAllowResponse())
                                return
                            }
                        }

                        // Advanced retry rule
                        if (prefs.retryRuleEnabled) {
                            val windowMs = prefs.retryRuleWindowMinutes * 60_000L
                            val sinceMs  = System.currentTimeMillis() - windowMs
                            val attempts = runBlocking {
                                db.blockedCallDao().countCallsSince(normalised, sinceMs)
                            }
                            if (attempts >= prefs.retryRuleAttempts) {
                                android.util.Log.d("CallBlocker", "→ ALLOW (retry rule, $attempts attempts)")
                                notifyRetryRuleAllowed(rawNumber, attempts + 1, prefs.retryRuleWindowMinutes)
                                serviceScope.launch {
                                    repository.addTemporary(rawNumber, System.currentTimeMillis() + windowMs)
                                }
                                respondToCall(callDetails, buildAllowResponse())
                                return
                            }
                        }

                        android.util.Log.d("CallBlocker", "→ BLOCK")
                        logBlockedCall(rawNumber, callDetails, verificationStatus = verificationStatus)
                        notifyBlocked(rawNumber)
                        buildBlockResponse()
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CallBlocker", "Error: ${e.message}")
            buildAllowResponse()
        }

        respondToCall(callDetails, response)
    }

    // ── SIM detection ─────────────────────────────────────────────────────────

    /**
     * Tries to resolve the SIM slot from the accountHandle.
     * On MIUI this often returns null because accountHandle itself is null.
     * In that case [logBlockedCall] will attempt a delayed CallLog lookup instead.
     */
    private fun simSlotFromHandle(callDetails: Call.Details): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val accountHandle = callDetails.accountHandle ?: return null
        val id = accountHandle.id ?: return null

        // Try SubscriptionManager first — most reliable when accountHandle is available.
        try {
            val sm   = getSystemService(SubscriptionManager::class.java)
            val subs = sm?.activeSubscriptionInfoList
            if (!subs.isNullOrEmpty()) {
                for (sub in subs) {
                    val subId = sub.subscriptionId.toString()
                    val iccId = sub.iccId ?: ""
                    if (id.contains(subId) || (iccId.isNotEmpty() && id.contains(iccId))) {
                        return if (sub.simSlotIndex == 0) "SIM1" else "SIM2"
                    }
                }
                // Single-SIM: any call must be on the only subscription.
                if (subs.size == 1) return if (subs[0].simSlotIndex == 0) "SIM1" else "SIM2"
            }
        } catch (e: Exception) {
            android.util.Log.w("CallBlocker", "SubscriptionManager failed: ${e.message}")
        }

        // Fallback: string pattern matching.
        return when {
            id.endsWith("0") || id.contains("slot0", ignoreCase = true) ||
                    id.contains("SIM1", ignoreCase = true) || id.contains("sub0", ignoreCase = true) -> "SIM1"
            id.endsWith("1") || id.contains("slot1", ignoreCase = true) ||
                    id.contains("SIM2", ignoreCase = true) || id.contains("sub1", ignoreCase = true) -> "SIM2"
            else -> null
        }
    }

    /**
     * Reads the system CallLog to find the SIM slot for a recently blocked call.
     * MIUI sets accountHandle to null in CallScreeningService but writes the correct
     * PHONE_ACCOUNT_ID into the CallLog entry within a few seconds.
     *
     * Called with a short delay after the call is rejected so the entry has time
     * to appear in the log.
     */
    private fun simSlotFromCallLog(rawNumber: String, callTimeMs: Long): String? {
        return try {
            val normalised  = PhoneUtils.normalize(rawNumber)
            val windowStart = callTimeMs - 10_000L
            val windowEnd   = callTimeMs + 30_000L

            // Include SUBSCRIPTION_ID — on MIUI iccId is empty but subId is readable from CallLog.
            val cursor = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.DATE,
                    CallLog.Calls.PHONE_ACCOUNT_ID,
                    "subscription_id"           // CallLog.Calls.SUBSCRIPTION_ID (API 26+, use literal for compat)
                ),
                "${CallLog.Calls.DATE} >= ? AND ${CallLog.Calls.DATE} <= ?",
                arrayOf(windowStart.toString(), windowEnd.toString()),
                "${CallLog.Calls.DATE} DESC"
            )

            cursor?.use { c ->
                val idxNumber    = c.getColumnIndex(CallLog.Calls.NUMBER)
                val idxAccountId = c.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID)
                val idxSubId     = c.getColumnIndex("subscription_id")   // -1 if unsupported
                while (c.moveToNext()) {
                    val logNumber = PhoneUtils.normalize(c.getString(idxNumber) ?: "")
                    if (logNumber == normalised) {
                        val accountId = c.getString(idxAccountId) ?: ""
                        val subId     = if (idxSubId >= 0) c.getInt(idxSubId) else -1
                        android.util.Log.d("CallBlocker",
                            "CallLog match: accountId='$accountId' subId=$subId")

                        // Strategy 0: user-configured accountId→SIM map — the only reliable
                        // path on MIUI where iccId is empty and subId is garbage.
                        if (accountId.isNotBlank()) {
                            val userMap = prefs.getSimAccountMap()
                            val slotFromMap = userMap[accountId.trim()]
                            if (slotFromMap != null) {
                                android.util.Log.d("CallBlocker", "Resolved $slotFromMap via user accountId map")
                                return@use slotFromMap
                            }
                            // Persist the seen accountId so the settings UI can offer to map it.
                            prefs.lastSeenAccountId = accountId.trim()
                            android.util.Log.d("CallBlocker", "Saved lastSeenAccountId='$accountId' for UI mapping")
                        }

                        // Strategy A: resolve via subId directly (works on stock Android).
                        if (subId > 0 && subId < 99999) {
                            val slot = resolveSlotFromSubId(subId)
                                ?: resolveSlotFromSubId(Math.abs(subId))
                            if (slot != null) {
                                android.util.Log.d("CallBlocker", "Resolved $slot via CallLog subId=$subId (abs=${Math.abs(subId)})")
                                return@use slot
                            }
                        }

                        // Strategy B: resolve via PHONE_ACCOUNT_ID string (iccId-based, stock Android).
                        if (accountId.isNotBlank()) {
                            val slot = resolveSlotFromAccountId(accountId)
                            if (slot != null) return@use slot
                        }

                        // Strategy 3 (MIUI last resort): if we have seen exactly 2
                        // distinct accountIds across all calls and one maps to a known slot,
                        // the other must be the remaining slot.
                        val userMap = prefs.getSimAccountMap()
                        if (userMap.size == 1 && accountId.isNotBlank() && !userMap.containsKey(accountId)) {
                            val knownSlot = userMap.values.first()
                            val deducedSlot = if (knownSlot == "SIM1") "SIM2" else "SIM1"
                            android.util.Log.d("CallBlocker",
                                "Resolved $deducedSlot via S3 deduction (other SIM is $knownSlot)")
                            return@use deducedSlot
                        }
                        android.util.Log.w("CallBlocker", "CallLog: could not resolve SIM (accountId='$accountId' subId=$subId)")
                        return@use null
                    }
                }
                android.util.Log.w("CallBlocker", "CallLog: no matching entry found for $rawNumber in window")
                null
            }
        } catch (e: Exception) {
            android.util.Log.w("CallBlocker", "CallLog SIM lookup failed: ${e.message}")
            null
        }
    }

    /**
     * Maps a CallLog subscription_id directly to a SIM slot via SubscriptionManager.
     * This is the reliable path on MIUI where iccId is empty but subId is correct.
     */
    private fun resolveSlotFromSubId(subId: Int): String? {
        return try {
            val sm   = getSystemService(SubscriptionManager::class.java)
            val subs = sm?.activeSubscriptionInfoList ?: return null
            val sub  = subs.firstOrNull { it.subscriptionId == subId } ?: return null
            if (sub.simSlotIndex == 0) "SIM1" else "SIM2"
        } catch (e: Exception) {
            android.util.Log.w("CallBlocker", "resolveSlotFromSubId failed: ${e.message}")
            null
        }
    }

    /**
     * Resolves "SIM1" / "SIM2" from a raw PHONE_ACCOUNT_ID string read from the CallLog.
     *
     * On MIUI (POCO F2 Pro and similar) the PHONE_ACCOUNT_ID is the SIM's ICCID with a
     * trailing 'F' padding nibble, exactly as stored in the EF_ICCID file on the SIM card.
     * SubscriptionManager.iccId returns the same digits WITHOUT the trailing 'F'.
     *
     * Example observed in the wild:
     *   SubscriptionManager.iccId  →  "8939104480018328021"   (19 digits, no padding)
     *   PHONE_ACCOUNT_ID (CallLog) →  "8939104480018328021F"  (19 digits + 'F' padding)
     *
     * Resolution order:
     *   1. Exact decimal ICCID match         — stock Android / some MIUI builds without 'F'.
     *   2. F-stripped match                  — MIUI: strip trailing 'F' and compare.
     *   3. Case-insensitive prefix match (≥10 chars) — covers both forms safely.
     *   4. Exact subId match                 — plain integer accountId (stock AOSP).
     *   5. Single active SIM fallback        — unambiguous on single-SIM devices.
     *
     * subId substring match is intentionally omitted: a short integer like "1" or "2"
     * appears in virtually every 20-char ICCID and produces false positives.
     */
    private fun resolveSlotFromAccountId(accountId: String): String? {
        if (accountId.isBlank()) return null
        try {
            val sm   = getSystemService(SubscriptionManager::class.java)
            val subs = sm?.activeSubscriptionInfoList

            // Log all available subscriptions to aid diagnosis.
            android.util.Log.d("CallBlocker",
                "resolveSlot: raw='$accountId' | activeSubs=${subs?.size ?: 0}")
            subs?.forEachIndexed { i, sub ->
                android.util.Log.d("CallBlocker",
                    "  sub[$i] slot=${sub.simSlotIndex} subId=${sub.subscriptionId} iccId='${sub.iccId}'")
            }

            if (subs.isNullOrEmpty()) return null

            // Normalise accountId: strip trailing 'F' padding (MIUI EF_ICCID format)
            // and any whitespace, then uppercase for case-insensitive comparison.
            fun String.normalise() = this.trim().trimEnd { it.uppercaseChar() == 'F' }.uppercase()
            val accountNorm = accountId.normalise()

            for (sub in subs) {
                val rawIccId = sub.iccId?.takeIf { it.isNotBlank() } ?: continue
                val iccNorm  = rawIccId.normalise()
                val slot     = if (sub.simSlotIndex == 0) "SIM1" else "SIM2"

                // P1: exact match on raw accountId (case-insensitive).
                if (accountId.equals(rawIccId, ignoreCase = true)) {
                    android.util.Log.d("CallBlocker", "Resolved $slot via P1 exact match")
                    return slot
                }

                // P2: both sides normalised (strips F-padding from either side).
                if (accountNorm.isNotEmpty() && accountNorm == iccNorm) {
                    android.util.Log.d("CallBlocker", "Resolved $slot via P2 normalised match ('$accountNorm'=='$iccNorm')")
                    return slot
                }

                // P3: one is a prefix of the other (≥10 chars) — handles truncated ICCIDs.
                if (accountNorm.length >= 10 && iccNorm.length >= 10) {
                    val len = minOf(accountNorm.length, iccNorm.length)
                    if (accountNorm.take(len) == iccNorm.take(len)) {
                        android.util.Log.d("CallBlocker", "Resolved $slot via P3 prefix match (len=$len)")
                        return slot
                    }
                }

                // P4: accountId is a plain integer subId (stock AOSP).
                if (accountId.trim() == sub.subscriptionId.toString()) {
                    android.util.Log.d("CallBlocker", "Resolved $slot via P4 subId match")
                    return slot
                }

                // P5: accountId contains subId as substring — some OEM builds use "subId_X" format.
                if (accountId.contains(sub.subscriptionId.toString())) {
                    android.util.Log.d("CallBlocker", "Resolved $slot via P5 subId substring match")
                    return slot
                }
            }

            // P6: only one active SIM — must be it.
            if (subs.size == 1) {
                val slot = if (subs[0].simSlotIndex == 0) "SIM1" else "SIM2"
                android.util.Log.d("CallBlocker", "Resolved $slot via P6 single-SIM fallback")
                return slot
            }

            android.util.Log.w("CallBlocker", "resolveSlot: no match found for '$accountId'")

        } catch (e: Exception) {
            android.util.Log.w("CallBlocker", "resolveSlotFromAccountId failed: ${e.message}")
        }
        return null
    }

    private fun shouldProtectThisSim(callDetails: Call.Details): Boolean {
        val protectedSim = prefs.protectedSim
        if (protectedSim == AppPreferences.SIM_BOTH) return true
        val slot = simSlotFromHandle(callDetails) ?: return true
        return when (protectedSim) {
            AppPreferences.SIM_1 -> slot == "SIM1"
            AppPreferences.SIM_2 -> slot == "SIM2"
            else -> true
        }
    }

    /**
     * Per-feature SIM guard: checks whether the call is on a SIM that the
     * specified feature target covers.
     */
    private fun shouldProtectThisSimForFeature(callDetails: Call.Details, simTarget: String): Boolean {
        if (simTarget == AppPreferences.SIM_BOTH) return true
        val slot = simSlotFromHandle(callDetails) ?: return true
        return when (simTarget) {
            AppPreferences.SIM_1 -> slot == "SIM1"
            AppPreferences.SIM_2 -> slot == "SIM2"
            else -> true
        }
    }

    /**
     * Returns true if [rawNumber] appears in the outgoing call log within the
     * [sinceMs] window.  Uses READ_CALL_LOG permission already declared in the Manifest.
     */
    private fun wasDialedRecently(rawNumber: String, sinceMs: Long): Boolean {
        return try {
            val normalised = PhoneUtils.normalize(rawNumber)
            val cursor = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE),
                "${CallLog.Calls.TYPE} = ? AND ${CallLog.Calls.DATE} >= ?",
                arrayOf(CallLog.Calls.OUTGOING_TYPE.toString(), sinceMs.toString()),
                "${CallLog.Calls.DATE} DESC"
            )
            cursor?.use { c ->
                val idxNumber = c.getColumnIndex(CallLog.Calls.NUMBER)
                while (c.moveToNext()) {
                    val logged = PhoneUtils.normalize(c.getString(idxNumber) ?: "")
                    if (logged == normalised) return@use true
                }
                false
            } ?: false
        } catch (e: Exception) {
            android.util.Log.w("CallBlocker", "wasDialedRecently failed: ${e.message}")
            false
        }
    }

    // ── Contacts ──────────────────────────────────────────────────────────────

    private fun isInSystemContacts(number: String): Boolean {
        val uri = android.net.Uri.withAppendedPath(
            android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            android.net.Uri.encode(number)
        )
        return try {
            contentResolver.query(
                uri,
                arrayOf(android.provider.ContactsContract.PhoneLookup._ID),
                null, null, null
            )?.use { it.count > 0 } ?: false
        } catch (e: Exception) { false }
    }

    // ── Response builders ─────────────────────────────────────────────────────

    private fun buildBlockResponse(): CallResponse =
        CallResponse.Builder()
            .setDisallowCall(true)
            .setRejectCall(true)
            .setSkipCallLog(false)
            .setSkipNotification(true)
            .build()

    private fun buildAllowResponse(): CallResponse =
        CallResponse.Builder().setRejectCall(false).build()

    // ── Call log ──────────────────────────────────────────────────────────────

    private fun logBlockedCall(
        number: String,
        callDetails: Call.Details,
        verificationStatus: String? = null
    ) {
        val callTimeMs      = System.currentTimeMillis()
        val simFromHandle   = simSlotFromHandle(callDetails)
        val accountHandleId = try { callDetails.accountHandle?.id } catch (e: Exception) { null }

        android.util.Log.d("CallBlocker",
            "Logging blocked call — simFromHandle=$simFromHandle accountHandleId=$accountHandleId")

        serviceScope.launch {
            // Insert with whatever SIM info we have immediately.
            val rowId = db.blockedCallDao().insertAndGetId(
                BlockedCallEntity(
                    phoneNumber        = PhoneUtils.normalize(number),
                    blockedAt          = callTimeMs,
                    simSlot            = simFromHandle,
                    callDirection      = "incoming",
                    accountHandleId    = accountHandleId,
                    callType           = BlockedCallEntity.TYPE_CALL,
                    verificationStatus = verificationStatus
                )
            )

            // If SIM was not resolved from handle (MIUI), retry via CallLog after a delay.
            if (simFromHandle == null && number != "Unknown number") {
                delay(8_000L) // wait for MIUI to write the CallLog entry (MIUI can be slow)
                val simFromLog = simSlotFromCallLog(number, callTimeMs)
                android.util.Log.d("CallBlocker", "SIM from CallLog: $simFromLog")
                if (simFromLog != null) {
                    db.blockedCallDao().updateSimSlot(rowId, simFromLog)
                    android.util.Log.d("CallBlocker", "Updated simSlot to $simFromLog for rowId=$rowId")
                }
            }

            pruneLogIfNeeded()
        }
    }

    /**
     * Logs a call that was allowed through for a specific reason (dialed whitelist, etc.)
     * so the user can see it in the log with an explanatory badge.
     */
    private fun logAllowedCall(
        number: String,
        callDetails: Call.Details,
        allowReason: String,
        verificationStatus: String? = null
    ) {
        val callTimeMs      = System.currentTimeMillis()
        val simFromHandle   = simSlotFromHandle(callDetails)
        val accountHandleId = try { callDetails.accountHandle?.id } catch (e: Exception) { null }

        serviceScope.launch {
            db.blockedCallDao().insertAndGetId(
                BlockedCallEntity(
                    phoneNumber        = PhoneUtils.normalize(number),
                    blockedAt          = callTimeMs,
                    simSlot            = simFromHandle,
                    callDirection      = "incoming",
                    accountHandleId    = accountHandleId,
                    callType           = BlockedCallEntity.TYPE_CALL,
                    verificationStatus = verificationStatus,
                    allowReason        = allowReason
                )
            )
        }
    }

    private suspend fun pruneLogIfNeeded() {
        val days = prefs.logRetentionDays
        if (days <= 0) return
        val cutoffMs = System.currentTimeMillis() - days * 24L * 60 * 60 * 1000
        db.blockedCallDao().deleteOlderThan(cutoffMs)
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(
                    AppPreferences.NOTIFICATION_CHANNEL_ID,
                    AppPreferences.NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "Notification when a call is blocked" }
            )
            nm.createNotificationChannel(
                NotificationChannel(
                    NotificationHelper.CHANNEL_WHITELIST_CALL,
                    "Whitelist calls",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "Notifications when a whitelisted number calls you" }
            )
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_RETRY_RULE,
                    "Call pass-through alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "Notified when a blocked number is allowed through by the retry rule" }
            )
        }
    }

    private fun notifyBlocked(number: String) {
        if (!prefs.notifyOnBlock) return
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !nm.areNotificationsEnabled()) return
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("navigate_to", "call_log")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        nm.notify(
            number.hashCode(),
            NotificationCompat.Builder(this, AppPreferences.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(NotificationHelper.getLargeIcon(this))
                .setColor(0xFF10B981.toInt())
                .setContentTitle(getString(R.string.notif_blocked_title))
                .setContentText(number)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build()
        )
    }

    private fun notifyRetryRuleAllowed(number: String, attempts: Int, windowMinutes: Int) {
        val nm = getSystemService(NotificationManager::class.java)
        val pi = PendingIntent.getActivity(
            this, number.hashCode() + 1,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("navigate_to", "call_log")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        nm.notify(
            NOTIF_ID_RETRY_RULE,
            NotificationCompat.Builder(this, CHANNEL_RETRY_RULE)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(NotificationHelper.getLargeIcon(this))
                .setColor(0xFF10B981.toInt())
                .setContentTitle(getString(R.string.notif_retry_rule_title))
                .setContentText(getString(R.string.notif_retry_rule_text, number, attempts, windowMinutes))
                .setStyle(NotificationCompat.BigTextStyle().bigText(
                    getString(R.string.notif_retry_rule_text, number, attempts, windowMinutes)
                ))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build()
        )
    }

    // ── Auto-build SIM→accountId map from existing call log ──────────────────

    /**
     * Scans the system CallLog for calls on each known subscription and maps
     * each PHONE_ACCOUNT_ID to its SIM slot.  This works because:
     *   - Regular (non-blocked) calls always have a valid PHONE_ACCOUNT_ID.
     *   - We correlate the accountId with its subscription via the subId column
     *     which — unlike iccId — doesn't require privileged access.
     *
     * Called once at service start when the map is still empty.
     * READ_CALL_LOG permission is already declared and granted.
     */
    private fun buildSimMapFromCallLog() {
        // Re-run if the map is empty even if previously marked as built,
        // so that a fix to the resolution logic takes effect automatically.
        if (prefs.simMapAutoBuilt && prefs.getSimAccountMap().isNotEmpty()) return

        try {
            val sm   = getSystemService(SubscriptionManager::class.java)
            val subs = sm?.activeSubscriptionInfoList
            if (subs.isNullOrEmpty() || subs.size < 2) {
                android.util.Log.d("CallBlocker", "buildSimMap: single-SIM or no subs, skipping")
                return
            }

            // Build a subId→slot lookup from SubscriptionManager (always works).
            val subIdToSlot = subs.associate { sub ->
                sub.subscriptionId to if (sub.simSlotIndex == 0) "SIM1" else "SIM2"
            }
            android.util.Log.d("CallBlocker", "buildSimMap: subIdToSlot=$subIdToSlot")

            // Query the last 200 calls — enough to cover all SIM slots in most cases.
            // LIMIT must be appended to the URI, not the sortOrder, because MIUI's
            // ContentProvider rejects "LIMIT N" in the sortOrder parameter.
            val queryUri = CallLog.Calls.CONTENT_URI.buildUpon()
                .appendQueryParameter("limit", "200")
                .build()
            val cursor = contentResolver.query(
                queryUri,
                arrayOf(
                    CallLog.Calls.PHONE_ACCOUNT_ID,
                    "subscription_id"
                ),
                null, null,
                "${CallLog.Calls.DATE} DESC"
            ) ?: run {
                android.util.Log.w("CallBlocker", "buildSimMap: cursor null")
                return
            }

            // accountId → set of subIds seen in CallLog for this accountId
            val accountSubIds = mutableMapOf<String, MutableSet<Int>>()
            cursor.use { c ->
                val idxAccId = c.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID)
                val idxSubId = c.getColumnIndex("subscription_id")
                while (c.moveToNext()) {
                    val accountId = if (idxAccId >= 0) c.getString(idxAccId)?.trim() ?: "" else ""
                    val subId     = if (idxSubId >= 0) c.getInt(idxSubId) else Int.MIN_VALUE
                    if (accountId.isBlank() || subId == Int.MIN_VALUE) continue
                    accountSubIds.getOrPut(accountId) { mutableSetOf() }.add(subId)
                    // Collect up to 200 rows then stop
                }
            }
            android.util.Log.d("CallBlocker", "buildSimMap: accountSubIds=$accountSubIds")

            val discovered = mutableMapOf<String, String>()

            // Strategy 1: direct subId match (works on stock Android)
            for ((accId, subIds) in accountSubIds) {
                for (sid in subIds) {
                    val slot = subIdToSlot[sid] ?: subIdToSlot[Math.abs(sid)]
                    if (slot != null && !discovered.containsKey(accId)) {
                        discovered[accId] = slot
                        android.util.Log.d("CallBlocker",
                            "buildSimMap: S1 accountId='$accId' → $slot (subId=$sid)")
                        break
                    }
                }
            }

            // Strategy 2: MIUI fallback — iccId is empty so we can't match by ICCID.
            // Instead, order the discovered accountIds by their representative subId
            // (using abs value for consistent ordering) and map to slots by slot index order.
            // On MIUI each SIM's CallLog subId is a large negative unique to that SIM;
            // the subscription with lower simSlotIndex has the lower abs(subId) in practice.
            if (discovered.size < subs.size && accountSubIds.size >= subs.size) {
                val unmapped = accountSubIds.keys.filter { !discovered.containsKey(it) }
                // Sort unmapped accountIds by their minimum subId (MIUI: consistent per SIM)
                val sortedUnmapped = unmapped.sortedBy { accId ->
                    accountSubIds[accId]?.minOrNull() ?: Int.MIN_VALUE
                }
                // Sort subs by simSlotIndex
                val sortedSubs = subs.sortedBy { it.simSlotIndex }
                // Map in order: first unmapped accountId → first unassigned slot
                val assignedSlots = discovered.values.toSet()
                val unassignedSubs = sortedSubs.filter {
                    val slot = if (it.simSlotIndex == 0) "SIM1" else "SIM2"
                    slot !in assignedSlots
                }
                sortedUnmapped.zip(unassignedSubs).forEach { (accId, sub) ->
                    val slot = if (sub.simSlotIndex == 0) "SIM1" else "SIM2"
                    discovered[accId] = slot
                    android.util.Log.d("CallBlocker",
                        "buildSimMap: S2(MIUI) accountId='$accId' → $slot (ordered fallback)")
                }
            }

            if (discovered.isNotEmpty()) {
                // Merge with any existing manual entries (don't overwrite user choices).
                val existing = prefs.getSimAccountMap().toMutableMap()
                var added = 0
                for ((accId, slot) in discovered) {
                    if (!existing.containsKey(accId)) {
                        existing[accId] = slot
                        added++
                    }
                }
                if (added > 0) {
                    // Persist by rebuilding the full map via repeated put.
                    for ((accId, slot) in existing) prefs.putSimAccountMapping(accId, slot)
                    prefs.simMapAutoBuilt = true   // mark only after successful save
                    android.util.Log.d("CallBlocker",
                        "buildSimMap: saved $added new mapping(s). Full map: $existing")
                }
            } else {
                android.util.Log.w("CallBlocker",
                    "buildSimMap: no valid accountId+subId pairs found in call log")
                prefs.simMapAutoBuilt = true   // avoid retrying on empty log
            }

        } catch (e: Exception) {
            android.util.Log.w("CallBlocker", "buildSimMap failed: ${e.message}")
        }
    }

    companion object {
        private const val CHANNEL_RETRY_RULE  = "retry_rule_alerts"
        private const val NOTIF_ID_RETRY_RULE = 3001
    }
}