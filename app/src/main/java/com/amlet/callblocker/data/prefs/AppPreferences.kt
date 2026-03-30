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

    companion object {
        private const val PREF_NAME = "callblocker_prefs"
        private const val KEY_NOTIFY_ON_BLOCK = "notify_on_block"
        private const val KEY_SUSPEND_UNTIL = "suspend_until"

        const val NOTIFICATION_CHANNEL_ID = "blocked_calls"
        const val NOTIFICATION_CHANNEL_NAME = "Chiamate bloccate"
    }
}
