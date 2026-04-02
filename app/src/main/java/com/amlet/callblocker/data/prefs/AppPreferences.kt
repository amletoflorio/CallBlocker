package com.amlet.callblocker.data.prefs

import android.content.Context
import android.content.SharedPreferences

/**
 * Unico punto di accesso alle preferenze dell'app.
 * Usa SharedPreferences direttamente — nessuna dipendenza extra.
 */
class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // ── Notifiche chiamate bloccate ──────────────────────────────────────────

    var notifyOnBlock: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_ON_BLOCK, false)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFY_ON_BLOCK, value).apply()

    // ── Sospensione protezione ───────────────────────────────────────────────

    /**
     * Timestamp Unix (ms) fino a cui la protezione è sospesa.
     * 0L = protezione attiva (nessuna sospensione).
     */
    var suspendUntil: Long
        get() = prefs.getLong(KEY_SUSPEND_UNTIL, 0L)
        set(value) = prefs.edit().putLong(KEY_SUSPEND_UNTIL, value).apply()

    /** True se la protezione è attualmente sospesa */
    val isSuspended: Boolean
        get() = suspendUntil > System.currentTimeMillis()

    /** Annulla immediatamente la sospensione */
    fun cancelSuspend() {
        suspendUntil = 0L
    }

    // ── Backup automatico ────────────────────────────────────────────────────

    /** Frequenza backup automatico in giorni (0 = disabilitato) */
    var autoBackupIntervalDays: Int
        get() = prefs.getInt(KEY_AUTO_BACKUP_INTERVAL, 0)
        set(value) = prefs.edit().putInt(KEY_AUTO_BACKUP_INTERVAL, value).apply()

    /** Timestamp Unix (ms) dell'ultimo backup automatico completato con successo. 0 = mai. */
    var lastAutoBackupAt: Long
        get() = prefs.getLong(KEY_LAST_AUTO_BACKUP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_AUTO_BACKUP, value).apply()

    /**
     * URI (stringa) della cartella scelta dall'utente per il backup automatico.
     * Null = nessuna cartella selezionata → fallback su Documents/CallBlocker/.
     * Persiste come stringa e viene riconvertito in Uri nel Worker.
     */
    var autoBackupFolderUri: String?
        get() = prefs.getString(KEY_AUTO_BACKUP_FOLDER_URI, null)
        set(value) = prefs.edit().putString(KEY_AUTO_BACKUP_FOLDER_URI, value).apply()

    // ── Controllo aggiornamenti ──────────────────────────────────────────────

    /** True se il controllo aggiornamenti è abilitato dall'utente */
    var checkUpdatesEnabled: Boolean
        get() = prefs.getBoolean(KEY_CHECK_UPDATES, false)
        set(value) = prefs.edit().putBoolean(KEY_CHECK_UPDATES, value).apply()

    companion object {
        private const val PREF_NAME = "callblocker_prefs"
        private const val KEY_NOTIFY_ON_BLOCK        = "notify_on_block"
        private const val KEY_SUSPEND_UNTIL          = "suspend_until"
        private const val KEY_AUTO_BACKUP_INTERVAL   = "auto_backup_interval_days"
        private const val KEY_LAST_AUTO_BACKUP       = "last_auto_backup_at"
        private const val KEY_AUTO_BACKUP_FOLDER_URI = "auto_backup_folder_uri"
        private const val KEY_CHECK_UPDATES          = "check_updates_enabled"

        const val NOTIFICATION_CHANNEL_ID   = "blocked_calls"
        const val NOTIFICATION_CHANNEL_NAME = "Chiamate bloccate"
    }
}
