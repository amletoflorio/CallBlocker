package com.amlet.callblocker.data.prefs

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // ── Blocked call notifications ───────────────────────────────────────────

    var notifyOnBlock: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_ON_BLOCK, false)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFY_ON_BLOCK, value).apply()

    // ── Protection suspension ────────────────────────────────────────────────

    /** Unix timestamp (ms) until which protection is suspended. 0L = active (no suspension). */
    var suspendUntil: Long
        get() = prefs.getLong(KEY_SUSPEND_UNTIL, 0L)
        set(value) = prefs.edit().putLong(KEY_SUSPEND_UNTIL, value).apply()

    /** True if protection is currently suspended. */
    val isSuspended: Boolean
        get() = suspendUntil > System.currentTimeMillis()

    /** Immediately cancels the suspension. */
    fun cancelSuspend() {
        suspendUntil = 0L
    }

    // ── Call protection toggle ────────────────────────────────────────────────

    /**
     * Whether call blocking is active. True by default (existing behavior).
     */
    // callProtectionEnabled is deprecated — the service now relies solely on isSuspended.
    // Kept here to avoid breaking existing exports; always returns true.
    @Deprecated("Use isSuspended instead")
    var callProtectionEnabled: Boolean
        get() = true
        set(value) { /* no-op */ }

    // ── Boot receiver ────────────────────────────────────────────────────────

    /**
     * When true, a BroadcastReceiver for BOOT_COMPLETED will open the system dialog
     * to re-request the CallScreeningService role after a device reboot.
     */
    var reactivateOnBoot: Boolean
        get() = prefs.getBoolean(KEY_REACTIVATE_ON_BOOT, false)
        set(value) = prefs.edit().putBoolean(KEY_REACTIVATE_ON_BOOT, value).apply()

    // ── Advanced retry rule ──────────────────────────────────────────────────

    /**
     * When true, the service applies the "X attempts in Y minutes" rule:
     * if a blocked number calls more than [retryRuleAttempts] times within
     * [retryRuleWindowMinutes], the call is allowed through and a notification is shown.
     */
    var retryRuleEnabled: Boolean
        get() = prefs.getBoolean(KEY_RETRY_RULE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_RETRY_RULE_ENABLED, value).apply()

    /** Number of attempts threshold for the retry rule. Range 2–10. Default 3. */
    var retryRuleAttempts: Int
        get() = prefs.getInt(KEY_RETRY_RULE_ATTEMPTS, 3)
        set(value) = prefs.edit().putInt(KEY_RETRY_RULE_ATTEMPTS, value.coerceIn(2, 10)).apply()

    /** Time window in minutes for the retry rule. Supported: 5, 10, 30, 60. Default 10. */
    var retryRuleWindowMinutes: Int
        get() = prefs.getInt(KEY_RETRY_RULE_WINDOW, 10)
        set(value) = prefs.edit().putInt(KEY_RETRY_RULE_WINDOW, value).apply()

    // ── Automatic backup ─────────────────────────────────────────────────────

    var autoBackupIntervalDays: Int
        get() = prefs.getInt(KEY_AUTO_BACKUP_INTERVAL, 0)
        set(value) = prefs.edit().putInt(KEY_AUTO_BACKUP_INTERVAL, value).apply()

    var lastAutoBackupAt: Long
        get() = prefs.getLong(KEY_LAST_AUTO_BACKUP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_AUTO_BACKUP, value).apply()

    var autoBackupFolderUri: String?
        get() = prefs.getString(KEY_AUTO_BACKUP_FOLDER_URI, null)
        set(value) = prefs.edit().putString(KEY_AUTO_BACKUP_FOLDER_URI, value).apply()

    // ── Update check ─────────────────────────────────────────────────────────

    var checkUpdatesEnabled: Boolean
        get() = prefs.getBoolean(KEY_CHECK_UPDATES, false)
        set(value) = prefs.edit().putBoolean(KEY_CHECK_UPDATES, value).apply()

    var notifyOnUpdate: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_ON_UPDATE, false)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFY_ON_UPDATE, value).apply()

    var lastUpdateCheckAt: Long
        get() = prefs.getLong(KEY_LAST_UPDATE_CHECK, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_UPDATE_CHECK, value).apply()

    // ── Dual SIM ─────────────────────────────────────────────────────────────

    var protectedSim: String
        get() = prefs.getString(KEY_PROTECTED_SIM, SIM_BOTH) ?: SIM_BOTH
        set(value) = prefs.edit().putString(KEY_PROTECTED_SIM, value).apply()

    // ── SIM account-ID map (MIUI workaround) ────────────────────────────────
    //
    // On MIUI, neither SubscriptionManager.iccId nor TelephonyManager.simSerialNumber
    // are accessible to third-party apps. The only stable identifier available in the
    // CallLog is PHONE_ACCOUNT_ID (an ICCID-like string). We let the user map each
    // observed accountId to SIM1 or SIM2 once, then use that mapping forever.
    //
    // Stored as "accountId1=SIM1,accountId2=SIM2" in a single SharedPreferences string.

    /** Returns the full accountId→slot map. */
    fun getSimAccountMap(): Map<String, String> {
        val raw = prefs.getString(KEY_SIM_ACCOUNT_MAP, "") ?: ""
        if (raw.isBlank()) return emptyMap()
        return raw.split(",").mapNotNull { entry ->
            val parts = entry.split("=", limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank())
                parts[0].trim() to parts[1].trim()
            else null
        }.toMap()
    }

    /** Adds or updates a single accountId→slot mapping. */
    fun putSimAccountMapping(accountId: String, slot: String) {
        val map = getSimAccountMap().toMutableMap()
        map[accountId.trim()] = slot
        prefs.edit().putString(KEY_SIM_ACCOUNT_MAP, map.entries.joinToString(",") { "${it.key}=${it.value}" }).apply()
    }

    /** Removes a single accountId mapping. */
    fun removeSimAccountMapping(accountId: String) {
        val map = getSimAccountMap().toMutableMap()
        map.remove(accountId.trim())
        prefs.edit().putString(KEY_SIM_ACCOUNT_MAP, map.entries.joinToString(",") { "${it.key}=${it.value}" }).apply()
    }

    /**
     * True once the automatic CallLog scan has been attempted (regardless of result),
     * so we don't repeat it on every app start.
     */
    var simMapAutoBuilt: Boolean
        get() = prefs.getBoolean(KEY_SIM_MAP_AUTO_BUILT, false)
        set(value) = prefs.edit().putBoolean(KEY_SIM_MAP_AUTO_BUILT, value).apply()

    /**
     * The most recently observed PHONE_ACCOUNT_ID from the CallLog that could not be
     * resolved automatically. Kept as fallback in case the auto-scan yields no results.
     */
    var lastSeenAccountId: String
        get() = prefs.getString(KEY_LAST_SEEN_ACCOUNT_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_SEEN_ACCOUNT_ID, value).apply()

    // ── Language ─────────────────────────────────────────────────────────────

    var appLanguage: String
        get() = prefs.getString(KEY_APP_LANGUAGE, LANG_SYSTEM) ?: LANG_SYSTEM
        set(value) = prefs.edit().putString(KEY_APP_LANGUAGE, value).apply()

    // ── Log retention ────────────────────────────────────────────────────────

    var logRetentionDays: Int
        get() = prefs.getInt(KEY_LOG_RETENTION_DAYS, 0)
        set(value) = prefs.edit().putInt(KEY_LOG_RETENTION_DAYS, value).apply()

    // ── STIR/SHAKEN ──────────────────────────────────────────────────────────

    /**
     * When true, calls with STIR/SHAKEN FAILED status are blocked regardless
     * of whitelist/contacts. Configurable per-SIM via [stirShakenSimTarget].
     */
    var blockOnVerificationFailed: Boolean
        get() = prefs.getBoolean(KEY_BLOCK_ON_VERIF_FAILED, false)
        set(value) = prefs.edit().putBoolean(KEY_BLOCK_ON_VERIF_FAILED, value).apply()

    /** SIM target for blockOnVerificationFailed: "sim1", "sim2", or "both". */
    var stirShakenSimTarget: String
        get() = prefs.getString(KEY_STIR_SHAKEN_SIM_TARGET, SIM_BOTH) ?: SIM_BOTH
        set(value) = prefs.edit().putString(KEY_STIR_SHAKEN_SIM_TARGET, value).apply()

    // ── Dialed-number whitelist ───────────────────────────────────────────────

    /**
     * When true, if the user called a number within the last [dialedWindowHours] hours,
     * that number is automatically allowed even if not in contacts or whitelist.
     */
    var dialedNumberWhitelistEnabled: Boolean
        get() = prefs.getBoolean(KEY_DIALED_WHITELIST_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_DIALED_WHITELIST_ENABLED, value).apply()

    /** Time window in hours for dialed-number whitelist. Options: 1, 6, 24, 48. Default 6. */
    var dialedWindowHours: Int
        get() = prefs.getInt(KEY_DIALED_WINDOW_HOURS, 6)
        set(value) = prefs.edit().putInt(KEY_DIALED_WINDOW_HOURS, value).apply()

    /** SIM target for dialed-number whitelist: "sim1", "sim2", or "both". */
    var dialedWhitelistSimTarget: String
        get() = prefs.getString(KEY_DIALED_WHITELIST_SIM_TARGET, SIM_BOTH) ?: SIM_BOTH
        set(value) = prefs.edit().putString(KEY_DIALED_WHITELIST_SIM_TARGET, value).apply()

    // ── Schedule rules ───────────────────────────────────────────────────────

    /** Master toggle for the scheduling feature. */
    var scheduleEnabled: Boolean
        get() = prefs.getBoolean(KEY_SCHEDULE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_SCHEDULE_ENABLED, value).apply()

    /**
     * Human-readable label explaining the current schedule state, shown on HomeScreen
     * under the toggle.  E.g. "Disattivo automaticamente fino alle 18:00".
     * Empty string = no active schedule state to show.
     */
    var scheduleActiveLabel: String
        get() = prefs.getString(KEY_SCHEDULE_ACTIVE_LABEL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SCHEDULE_ACTIVE_LABEL, value).apply()

    /**
     * When true, the user has manually overridden the current schedule for today.
     * The schedule will resume from the next trigger.
     */
    var scheduleOverriddenToday: Boolean
        get() = prefs.getBoolean(KEY_SCHEDULE_OVERRIDDEN_TODAY, false)
        set(value) = prefs.edit().putBoolean(KEY_SCHEDULE_OVERRIDDEN_TODAY, value).apply()

    /** Date (yyyyMMdd) on which [scheduleOverriddenToday] was set — for auto-reset. */
    var scheduleOverriddenDate: String
        get() = prefs.getString(KEY_SCHEDULE_OVERRIDDEN_DATE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SCHEDULE_OVERRIDDEN_DATE, value).apply()

    // ── UI state persistence ─────────────────────────────────────────────────

    var lastSettingsTab: Int
        get() = prefs.getInt(KEY_LAST_SETTINGS_TAB, 0)
        set(value) = prefs.edit().putInt(KEY_LAST_SETTINGS_TAB, value).apply()


    // ── Full settings export / import ────────────────────────────────────────

    /**
     * Serialises all user-configurable settings to a JSON string for export.
     */
    fun exportSettingsAsJson(): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"_version\": 1")
        val exportKeys = listOf(
            KEY_NOTIFY_ON_BLOCK, KEY_CALL_PROTECTION_ENABLED,
            KEY_REACTIVATE_ON_BOOT, KEY_RETRY_RULE_ENABLED, KEY_RETRY_RULE_ATTEMPTS,
            KEY_RETRY_RULE_WINDOW, KEY_AUTO_BACKUP_INTERVAL, KEY_AUTO_BACKUP_FOLDER_URI,
            KEY_CHECK_UPDATES, KEY_NOTIFY_ON_UPDATE, KEY_PROTECTED_SIM,
            KEY_APP_LANGUAGE, KEY_LOG_RETENTION_DAYS, KEY_SIM_ACCOUNT_MAP,
            KEY_BLOCK_ON_VERIF_FAILED, KEY_STIR_SHAKEN_SIM_TARGET,
            KEY_DIALED_WHITELIST_ENABLED, KEY_DIALED_WINDOW_HOURS,
            KEY_DIALED_WHITELIST_SIM_TARGET, KEY_SCHEDULE_ENABLED
        )
        val all = prefs.all
        for (key in exportKeys) {
            val value = all[key] ?: continue
            val jsonValue = when (value) {
                is Boolean -> value.toString()
                is Int     -> value.toString()
                is Long    -> value.toString()
                is String  -> "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\"" 
                else       -> "\"$value\""
            }
            sb.append(",\n  \"$key\": $jsonValue")
        }
        sb.append("\n}")
        return sb.toString()
    }

    /**
     * Restores settings from a JSON string produced by [exportSettingsAsJson].
     * Returns true on success, false on parse error.
     */
    fun importSettingsFromJson(json: String): Boolean {
        return try {
            val editor = prefs.edit()
            val lines = json.trim().removePrefix("{").removeSuffix("}").lines()
            for (line in lines) {
                val trimmed = line.trim().trimEnd(',')
                if (trimmed.isBlank()) continue
                val colonIdx = trimmed.indexOf(':')
                if (colonIdx < 0) continue
                val rawKey = trimmed.substring(0, colonIdx).trim().trim('"')
                val rawVal = trimmed.substring(colonIdx + 1).trim()
                if (rawKey == "_version") continue
                when {
                    rawVal == "true"  -> editor.putBoolean(rawKey, true)
                    rawVal == "false" -> editor.putBoolean(rawKey, false)
                    rawVal.startsWith("\"") -> editor.putString(rawKey, rawVal.trim('"'))
                    else -> rawVal.toLongOrNull()?.let { lv ->
                        if (lv > Int.MAX_VALUE || lv < Int.MIN_VALUE) editor.putLong(rawKey, lv)
                        else editor.putInt(rawKey, lv.toInt())
                    }
                }
            }
            editor.apply()
            true
        } catch (e: Exception) {
            false
        }
    }


    companion object {
        private const val PREF_NAME = "callblocker_prefs"

        private const val KEY_NOTIFY_ON_BLOCK            = "notify_on_block"
        private const val KEY_SUSPEND_UNTIL              = "suspend_until"
        private const val KEY_CALL_PROTECTION_ENABLED    = "call_protection_enabled"
        private const val KEY_REACTIVATE_ON_BOOT         = "reactivate_on_boot"
        private const val KEY_RETRY_RULE_ENABLED         = "retry_rule_enabled"
        private const val KEY_RETRY_RULE_ATTEMPTS        = "retry_rule_attempts"
        private const val KEY_RETRY_RULE_WINDOW          = "retry_rule_window_minutes"
        private const val KEY_AUTO_BACKUP_INTERVAL       = "auto_backup_interval_days"
        private const val KEY_LAST_AUTO_BACKUP           = "last_auto_backup_at"
        private const val KEY_AUTO_BACKUP_FOLDER_URI     = "auto_backup_folder_uri"
        private const val KEY_CHECK_UPDATES              = "check_updates_enabled"
        private const val KEY_NOTIFY_ON_UPDATE           = "notify_on_update"
        private const val KEY_LAST_UPDATE_CHECK          = "last_update_check_at"
        private const val KEY_PROTECTED_SIM              = "protected_sim"
        private const val KEY_APP_LANGUAGE               = "app_language"
        private const val KEY_LOG_RETENTION_DAYS         = "log_retention_days"
        private const val KEY_LAST_SETTINGS_TAB          = "last_settings_tab"
        private const val KEY_SIM_ACCOUNT_MAP            = "sim_account_map"
        private const val KEY_LAST_SEEN_ACCOUNT_ID       = "last_seen_account_id"
        private const val KEY_SIM_MAP_AUTO_BUILT         = "sim_map_auto_built"

        // v1.7.0
        private const val KEY_BLOCK_ON_VERIF_FAILED      = "block_on_verif_failed"
        private const val KEY_STIR_SHAKEN_SIM_TARGET     = "stir_shaken_sim_target"
        private const val KEY_DIALED_WHITELIST_ENABLED   = "dialed_whitelist_enabled"
        private const val KEY_DIALED_WINDOW_HOURS        = "dialed_window_hours"
        private const val KEY_DIALED_WHITELIST_SIM_TARGET= "dialed_whitelist_sim_target"
        private const val KEY_SCHEDULE_ENABLED           = "schedule_enabled"
        private const val KEY_SCHEDULE_ACTIVE_LABEL      = "schedule_active_label"
        private const val KEY_SCHEDULE_OVERRIDDEN_TODAY  = "schedule_overridden_today"
        private const val KEY_SCHEDULE_OVERRIDDEN_DATE   = "schedule_overridden_date"

        const val NOTIFICATION_CHANNEL_ID   = "blocked_calls"
        const val NOTIFICATION_CHANNEL_NAME = "Blocked calls"

        const val SIM_1    = "sim1"
        const val SIM_2    = "sim2"
        const val SIM_BOTH = "both"

        const val LANG_SYSTEM = "system"
        const val LANG_EN     = "en"
        const val LANG_IT     = "it"
    }
}