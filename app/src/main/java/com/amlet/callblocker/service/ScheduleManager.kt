package com.amlet.callblocker.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.amlet.callblocker.data.db.AppDatabase
import com.amlet.callblocker.data.db.ScheduleRuleEntity
import com.amlet.callblocker.data.prefs.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manages scheduling of call-blocking activation/deactivation rules.
 *
 * Each enabled [ScheduleRuleEntity] fires two alarms per active day:
 *  - startTime: applies the rule's mode (activate or deactivate protection)
 *  - endTime: reverses the rule's mode
 *
 * Alarms are scheduled with [AlarmManager.setExactAndAllowWhileIdle] so they
 * fire even in Doze mode.
 */
object ScheduleManager {

    // Broadcast extras
    const val EXTRA_ACTION          = "schedule_action"
    const val ACTION_ACTIVATE       = "activate"
    const val ACTION_DEACTIVATE     = "deactivate"
    const val EXTRA_RULE_ID         = "rule_id"
    const val EXTRA_LABEL           = "label"
    const val EXTRA_RESUME_AT_LABEL = "resume_at_label"

    private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

    /**
     * Cancels all existing schedule alarms and reschedules from the current
     * list of enabled rules. Call this whenever rules are added, removed, or
     * toggled, and also at boot.
     */
    suspend fun reschedule(context: Context) = withContext(Dispatchers.IO) {
        val prefs = AppPreferences(context)
        if (!prefs.scheduleEnabled) {
            cancelAll(context)
            prefs.scheduleActiveLabel = ""
            return@withContext
        }

        val db    = AppDatabase.getInstance(context)
        val rules = db.scheduleRuleDao().getEnabledRules()
        cancelAll(context)
        if (rules.isEmpty()) {
            prefs.scheduleActiveLabel = ""
            return@withContext
        }

        val am       = context.getSystemService(AlarmManager::class.java)
        val now      = Calendar.getInstance()
        var minLabel = ""
        var minTrigger = Long.MAX_VALUE

        for (rule in rules) {
            val days = rule.dayList()
            for (dayOfWeek in days) {
                // Calendar day-of-week: Mon=2..Sun=1; our 1=Mon..7=Sun
                val calDay = if (dayOfWeek == 7) Calendar.SUNDAY else dayOfWeek + 1

                // Schedule startTime alarm
                val startCal = nextOccurrence(now, calDay, rule.startTime)
                val startAction = if (rule.mode == ScheduleRuleEntity.MODE_ACTIVATE)
                    ACTION_ACTIVATE else ACTION_DEACTIVATE
                val endAction = if (rule.mode == ScheduleRuleEntity.MODE_ACTIVATE)
                    ACTION_DEACTIVATE else ACTION_ACTIVATE
                val endCal = nextOccurrence(now, calDay, rule.endTime)

                scheduleAlarm(context, am, rule.id, startAction, startCal.timeInMillis, rule.endTime)
                scheduleAlarm(context, am, -(rule.id), endAction, endCal.timeInMillis, rule.startTime)

                if (startCal.timeInMillis < minTrigger) {
                    minTrigger = startCal.timeInMillis
                    minLabel   = buildLabel(context, startAction, rule.endTime)
                }
                if (endCal.timeInMillis < minTrigger) {
                    minTrigger = endCal.timeInMillis
                    minLabel   = buildLabel(context, endAction, rule.startTime)
                }
            }
        }

        // Update HomeScreen label based on current rule state
        prefs.scheduleActiveLabel = computeCurrentLabel(context, rules)
    }

    /**
     * Returns a human-readable label describing the current schedule state,
     * e.g. "Disattivo automaticamente fino alle 18:00".
     */
    private fun computeCurrentLabel(context: Context, rules: List<ScheduleRuleEntity>): String {
        val now     = Calendar.getInstance()
        val todayDow = now.get(Calendar.DAY_OF_WEEK)   // 1=Sun..7=Sat
        val todayOur = if (todayDow == Calendar.SUNDAY) 7 else todayDow - 1  // 1=Mon..7=Sun
        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        for (rule in rules) {
            if (todayOur !in rule.dayList()) continue
            val startMinutes = timeToMinutes(rule.startTime)
            val endMinutes   = timeToMinutes(rule.endTime)

            val active = if (endMinutes > startMinutes)
                nowMinutes in startMinutes until endMinutes
            else // overnight
                nowMinutes >= startMinutes || nowMinutes < endMinutes

            if (active) {
                return if (rule.mode == ScheduleRuleEntity.MODE_DEACTIVATE)
                    buildLabel(context, ACTION_ACTIVATE, rule.endTime)
                else
                    buildLabel(context, ACTION_DEACTIVATE, rule.endTime)
            }
        }
        return ""
    }

    private fun buildLabel(context: Context, nextAction: String, nextTime: String): String {
        return if (nextAction == ACTION_ACTIVATE)
            context.getString(com.amlet.callblocker.R.string.home_schedule_reactivate_at, nextTime)
        else
            context.getString(com.amlet.callblocker.R.string.home_schedule_deactivate_at, nextTime)
    }

    private fun scheduleAlarm(
        context: Context,
        am: AlarmManager,
        requestCode: Int,
        action: String,
        triggerMs: Long,
        resumeAtTime: String
    ) {
        val intent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
            putExtra(EXTRA_ACTION, action)
            putExtra(EXTRA_RULE_ID, requestCode)
            putExtra(EXTRA_RESUME_AT_LABEL, resumeAtTime)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pi    = PendingIntent.getBroadcast(context, requestCode + 10000, intent, flags)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.set(AlarmManager.RTC_WAKEUP, triggerMs, pi)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
        }
    }

    /** Cancels all previously scheduled alarms. */
    fun cancelAll(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        // Cancel up to 200 possible request codes (rule IDs ±10000)
        for (code in -100..100) {
            val intent = Intent(context, ScheduleAlarmReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                context,
                code + 10000,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            ) ?: continue
            am.cancel(pi)
        }
    }

    /** Returns the next Calendar occurrence of (dayOfWeek, HH:mm) after [now]. */
    private fun nextOccurrence(now: Calendar, calDayOfWeek: Int, time: String): Calendar {
        val (h, m) = time.split(":").map { it.toInt() }
        val cal = now.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, h)
        cal.set(Calendar.MINUTE, m)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        // Advance to the correct day of week
        var daysToAdd = (calDayOfWeek - cal.get(Calendar.DAY_OF_WEEK) + 7) % 7
        if (daysToAdd == 0 && cal.timeInMillis <= now.timeInMillis) daysToAdd = 7
        cal.add(Calendar.DAY_OF_YEAR, daysToAdd)
        return cal
    }

    private fun timeToMinutes(time: String): Int {
        val parts = time.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }
}
