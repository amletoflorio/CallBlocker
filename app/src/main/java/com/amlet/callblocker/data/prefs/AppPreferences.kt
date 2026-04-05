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
    var callProtectionEnabled: Boolean
        get() = prefs.getBoolean(KEY_CALL_PROTECTION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_CALL_PROTECTION_ENABLED, value).apply()

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

    // ── UI state persistence ─────────────────────────────────────────────────

    var lastSettingsTab: Int
        get() = prefs.getInt(KEY_LAST_SETTINGS_TAB, 0)
        set(value) = prefs.edit().putInt(KEY_LAST_SETTINGS_TAB, value).apply()

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
