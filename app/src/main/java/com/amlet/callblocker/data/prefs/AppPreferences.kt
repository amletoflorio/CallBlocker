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

    /** True if protection is currently suspended */
    val isSuspended: Boolean
        get() = suspendUntil > System.currentTimeMillis()

    /** Immediately cancels the suspension */
    fun cancelSuspend() {
        suspendUntil = 0L
    }

    // ── Automatic backup ─────────────────────────────────────────────────────

    /** Auto backup frequency in days (0 = disabled) */
    var autoBackupIntervalDays: Int
        get() = prefs.getInt(KEY_AUTO_BACKUP_INTERVAL, 0)
        set(value) = prefs.edit().putInt(KEY_AUTO_BACKUP_INTERVAL, value).apply()

    /** Unix timestamp (ms) of the last successful auto backup. 0 = never. */
    var lastAutoBackupAt: Long
        get() = prefs.getLong(KEY_LAST_AUTO_BACKUP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_AUTO_BACKUP, value).apply()

    /**
     * URI (string) of the folder chosen by the user for automatic backup.
     * Null = no folder selected → fallback to Documents/CallBlocker/.
     * Stored as string and converted back to Uri in the Worker.
     */
    var autoBackupFolderUri: String?
        get() = prefs.getString(KEY_AUTO_BACKUP_FOLDER_URI, null)
        set(value) = prefs.edit().putString(KEY_AUTO_BACKUP_FOLDER_URI, value).apply()

    // ── Update check ─────────────────────────────────────────────────────────

    /** True if the update check is enabled by the user */
    var checkUpdatesEnabled: Boolean
        get() = prefs.getBoolean(KEY_CHECK_UPDATES, false)
        set(value) = prefs.edit().putBoolean(KEY_CHECK_UPDATES, value).apply()

    /** True if the user wants to receive update available notifications */
    var notifyOnUpdate: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_ON_UPDATE, false)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFY_ON_UPDATE, value).apply()

    /** Unix timestamp (ms) of the last automatic update check. 0 = never. */
    var lastUpdateCheckAt: Long
        get() = prefs.getLong(KEY_LAST_UPDATE_CHECK, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_UPDATE_CHECK, value).apply()

    // ── Dual SIM ─────────────────────────────────────────────────────────────

    /**
     * Which SIM(s) to protect.
     * Values: "sim1", "sim2", "both".
     * Default: "both" (protects everything).
     */
    var protectedSim: String
        get() = prefs.getString(KEY_PROTECTED_SIM, SIM_BOTH) ?: SIM_BOTH
        set(value) = prefs.edit().putString(KEY_PROTECTED_SIM, value).apply()

    // ── Language ─────────────────────────────────────────────────────────────

    /**
     * App language override.
     * Values: "system", "en", "it".
     * Default: "system".
     */
    var appLanguage: String
        get() = prefs.getString(KEY_APP_LANGUAGE, LANG_SYSTEM) ?: LANG_SYSTEM
        set(value) = prefs.edit().putString(KEY_APP_LANGUAGE, value).apply()

    // ── UI state persistence ─────────────────────────────────────────────────

    /** Ordinal of the last selected tab in SettingsScreen. Restored after Activity recreate(). */
    var lastSettingsTab: Int
        get() = prefs.getInt(KEY_LAST_SETTINGS_TAB, 0)
        set(value) = prefs.edit().putInt(KEY_LAST_SETTINGS_TAB, value).apply()

    companion object {
        private const val PREF_NAME = "callblocker_prefs"

        private const val KEY_NOTIFY_ON_BLOCK        = "notify_on_block"
        private const val KEY_SUSPEND_UNTIL          = "suspend_until"
        private const val KEY_AUTO_BACKUP_INTERVAL   = "auto_backup_interval_days"
        private const val KEY_LAST_AUTO_BACKUP       = "last_auto_backup_at"
        private const val KEY_AUTO_BACKUP_FOLDER_URI = "auto_backup_folder_uri"
        private const val KEY_CHECK_UPDATES          = "check_updates_enabled"
        private const val KEY_NOTIFY_ON_UPDATE       = "notify_on_update"
        private const val KEY_LAST_UPDATE_CHECK      = "last_update_check_at"
        private const val KEY_PROTECTED_SIM          = "protected_sim"
        private const val KEY_APP_LANGUAGE           = "app_language"
        private const val KEY_LAST_SETTINGS_TAB      = "last_settings_tab"

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
