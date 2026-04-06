package com.amlet.callblocker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.amlet.callblocker.data.prefs.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receives scheduled alarms from [ScheduleManager] and applies the corresponding
 * protection state change (activate or deactivate blocking).
 *
 * Also listens for BOOT_COMPLETED alongside [BootReceiver] to reschedule alarms
 * after a reboot (AlarmManager alarms do not survive reboots).
 */
class ScheduleAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.LOCKED_BOOT_COMPLETED" -> {
                // Reschedule alarms after reboot
                val prefs = AppPreferences(context)
                if (prefs.scheduleEnabled) {
                    CoroutineScope(Dispatchers.IO).launch {
                        ScheduleManager.reschedule(context)
                    }
                }
                return
            }
        }

        // Scheduled trigger
        val action  = intent.getStringExtra(ScheduleManager.EXTRA_ACTION) ?: return
        val prefs   = AppPreferences(context)
        val resumeAt = intent.getStringExtra(ScheduleManager.EXTRA_RESUME_AT_LABEL) ?: ""

        when (action) {
            ScheduleManager.ACTION_ACTIVATE -> {
                // Check if user has overridden for today
                val todayStr = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
                    .format(java.util.Date())
                if (prefs.scheduleOverriddenToday && prefs.scheduleOverriddenDate == todayStr) {
                    android.util.Log.d("ScheduleAlarm", "Skipping activation — overridden for today")
                    return
                }
                prefs.callProtectionEnabled = true
                prefs.scheduleActiveLabel = if (resumeAt.isNotEmpty())
                    context.getString(com.amlet.callblocker.R.string.home_schedule_deactivate_at, resumeAt)
                else ""
                android.util.Log.d("ScheduleAlarm", "Protection ACTIVATED by schedule")
            }
            ScheduleManager.ACTION_DEACTIVATE -> {
                val todayStr = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
                    .format(java.util.Date())
                if (prefs.scheduleOverriddenToday && prefs.scheduleOverriddenDate == todayStr) {
                    android.util.Log.d("ScheduleAlarm", "Skipping deactivation — overridden for today")
                    return
                }
                prefs.callProtectionEnabled = false
                prefs.scheduleActiveLabel = if (resumeAt.isNotEmpty())
                    context.getString(com.amlet.callblocker.R.string.home_schedule_reactivate_at, resumeAt)
                else ""
                android.util.Log.d("ScheduleAlarm", "Protection DEACTIVATED by schedule")
            }
        }
    }
}
