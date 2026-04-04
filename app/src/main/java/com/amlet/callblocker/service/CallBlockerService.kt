package com.amlet.callblocker.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class CallBlockerService : CallScreeningService() {

    private lateinit var repository: ContactRepository
    private lateinit var prefs: AppPreferences
    private lateinit var db: AppDatabase

    // Coroutine scope for async operations (log writes) — cancelled in onDestroy.
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
    }

    override fun onScreenCall(callDetails: Call.Details) {
        val rawNumber = callDetails.handle?.schemeSpecificPart
        android.util.Log.d("CallBlocker", "=== INCOMING CALL: $rawNumber ===")

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
                    val inContacts = isInSystemContacts(rawNumber)
                    val inWhitelist = runBlocking { repository.findByNumber(rawNumber) != null }
                    android.util.Log.d("CallBlocker", "In contacts: $inContacts | In whitelist: $inWhitelist")

                    if (inContacts || inWhitelist) {
                        android.util.Log.d("CallBlocker", "→ ALLOW")
                        buildAllowResponse()
                    } else {
                        android.util.Log.d("CallBlocker", "→ BLOCK")
                        logBlockedCall(rawNumber, callDetails)
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

    /**
     * Returns a human-readable SIM slot label ("SIM1" / "SIM2") if determinable,
     * null otherwise (single-SIM or older API).
     */
    private fun simSlotFor(callDetails: Call.Details): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return try {
            val id = callDetails.accountHandle?.id ?: return null
            when {
                id.endsWith("0") || id.contains("slot0", ignoreCase = true) ||
                    id.contains("SIM1", ignoreCase = true) -> "SIM1"
                id.endsWith("1") || id.contains("slot1", ignoreCase = true) ||
                    id.contains("SIM2", ignoreCase = true) -> "SIM2"
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Returns true if the call's SIM slot matches the user's protection preference.
     * Always returns true on single-SIM devices or when "both" is selected.
     */
    private fun shouldProtectThisSim(callDetails: Call.Details): Boolean {
        val protectedSim = prefs.protectedSim
        if (protectedSim == AppPreferences.SIM_BOTH) return true

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val accountHandle = callDetails.accountHandle ?: return true
                val id = accountHandle.id
                when (protectedSim) {
                    AppPreferences.SIM_1 -> id.endsWith("0") || id.contains("slot0") || id.contains("SIM1")
                    AppPreferences.SIM_2 -> id.endsWith("1") || id.contains("slot1") || id.contains("SIM2")
                    else -> true
                }
            } else {
                true // Cannot determine SIM slot on older APIs — protect all.
            }
        } catch (e: Exception) {
            true // Fail safe: protect the call.
        }
    }

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
            )?.use { cursor -> cursor.count > 0 } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun buildBlockResponse(): CallResponse =
        CallResponse.Builder()
            .setDisallowCall(true)
            .setRejectCall(true)
            .setSkipCallLog(false)
            .setSkipNotification(true)
            .build()

    private fun buildAllowResponse(): CallResponse =
        CallResponse.Builder()
            .setRejectCall(false)
            .build()

    // ── Call log ──────────────────────────────────────────────────────────────

    /**
     * Persists a blocked-call record, capturing all available metadata from
     * [callDetails]: SIM slot, call direction, and raw account handle ID.
     */
    private fun logBlockedCall(number: String, callDetails: Call.Details) {
        val simSlot = simSlotFor(callDetails)
        val accountHandleId = try { callDetails.accountHandle?.id } catch (e: Exception) { null }
        val callDirection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                when (callDetails.callDirection) {
                    Call.Details.DIRECTION_INCOMING -> "Incoming"
                    Call.Details.DIRECTION_OUTGOING -> "Outgoing"
                    else -> "Unknown"
                }
            } catch (e: Exception) { null }
        } else null

        serviceScope.launch {
            try {
                db.blockedCallDao().insert(
                    BlockedCallEntity(
                        phoneNumber     = PhoneUtils.normalize(number),
                        simSlot         = simSlot,
                        callDirection   = callDirection,
                        accountHandleId = accountHandleId
                    )
                )
                pruneLogIfNeeded()
            } catch (e: Exception) {
                android.util.Log.e("CallBlocker", "Log write error: ${e.message}")
            }
        }
    }

    /**
     * Deletes log entries older than the configured retention threshold.
     * No-op when retention is set to 0 (never delete).
     */
    private suspend fun pruneLogIfNeeded() {
        val days = prefs.logRetentionDays
        if (days <= 0) return
        val cutoffMs = System.currentTimeMillis() - days * 24L * 60 * 60 * 1000
        db.blockedCallDao().deleteOlderThan(cutoffMs)
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                AppPreferences.NOTIFICATION_CHANNEL_ID,
                AppPreferences.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notification when a call is blocked"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun notifyBlocked(number: String) {
        if (!prefs.notifyOnBlock) return
        val notificationManager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!notificationManager.areNotificationsEnabled()) return
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("navigate_to", "call_log")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, AppPreferences.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.notif_blocked_title))
            .setContentText(number)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(number.hashCode(), notification)
    }
}
