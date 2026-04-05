package com.amlet.callblocker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.amlet.callblocker.data.prefs.AppPreferences

/**
 * Listens for BOOT_COMPLETED and, if the user has enabled "Reactivate on boot",
 * restores call protection to the active state by:
 *   1. Setting callProtectionEnabled = true
 *   2. Clearing any active suspension (suspendUntil = 0)
 *
 * The CallScreeningService role persists across reboots on its own — no system
 * dialog is needed. We only need to reset the in-app toggle and any suspension
 * that was left active before the reboot.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.LOCKED_BOOT_COMPLETED") return

        val prefs = AppPreferences(context)
        if (!prefs.reactivateOnBoot) return

        // Re-enable call protection toggle.
        prefs.callProtectionEnabled = true

        // Clear any suspension (including infinite ones set via Long.MAX_VALUE).
        prefs.cancelSuspend()

        android.util.Log.d("BootReceiver", "Protection restored on boot (suspension cleared)")
    }
}