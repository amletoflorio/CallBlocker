package com.amlet.callblocker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.amlet.callblocker.MainActivity
import com.amlet.callblocker.R
import com.amlet.callblocker.data.db.AppDatabase
import com.amlet.callblocker.data.prefs.AppPreferences
import java.text.SimpleDateFormat
import java.util.*

/**
 * Status Widget (2x2) — protection state, whitelist count, last blocked call.
 * Tapping opens the app.
 *
 * NOTE: Room sync queries (countSync, getLastBlockedSync) must NOT run on the
 * main thread. onUpdate is called on the main thread, so we dispatch DB work
 * to a background thread and then push the result back via updateAppWidget.
 */
class StatusWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { updateWidget(context, appWidgetManager, it) }
    }

    companion object {

        fun updateWidget(context: Context, awm: AppWidgetManager, widgetId: Int) {
            val prefs = AppPreferences(context)
            val isSuspended = prefs.isSuspended

            val views = RemoteViews(context.packageName, R.layout.widget_status)

            // Tap → open app
            val openPi = PendingIntent.getActivity(
                context, 2,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_status_root, openPi)

            // Status text & icon tint (no DB needed)
            val statusText = when {
                isSuspended -> context.getString(R.string.widget_status_suspended)
                else        -> context.getString(R.string.widget_status_active)
            }
            views.setTextViewText(R.id.widget_status_text, statusText)
            views.setTextViewText(R.id.widget_app_name, context.getString(R.string.app_name))

            val tint = when {
                isSuspended -> 0xFFF59E0B.toInt()
                else        -> 0xFF10B981.toInt()
            }
            views.setInt(R.id.widget_status_icon, "setColorFilter", tint)
            views.setTextColor(R.id.widget_status_text, tint)

            // Set placeholder text while DB loads
            views.setTextViewText(R.id.widget_contact_count, "")
            views.setTextViewText(R.id.widget_last_blocked, "")

            // Push initial (non-DB) state immediately
            awm.updateAppWidget(widgetId, views)

            // Load DB data on a background thread
            Thread {
                try {
                    val db = AppDatabase.getInstance(context)
                    val contactCount = db.contactDao().countSync()
                    val lastBlocked = db.blockedCallDao().getLastBlockedSync()

                    val views2 = RemoteViews(context.packageName, R.layout.widget_status)
                    views2.setOnClickPendingIntent(R.id.widget_status_root, openPi)
                    views2.setTextViewText(R.id.widget_status_text, statusText)
                    views2.setTextViewText(R.id.widget_app_name, context.getString(R.string.app_name))
                    views2.setInt(R.id.widget_status_icon, "setColorFilter", tint)
                    views2.setTextColor(R.id.widget_status_text, tint)

                    views2.setTextViewText(
                        R.id.widget_contact_count,
                        context.getString(R.string.widget_contacts_label, contactCount)
                    )
                    if (lastBlocked != null) {
                        val fmt = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                        views2.setTextViewText(
                            R.id.widget_last_blocked,
                            "${lastBlocked.phoneNumber}  ${fmt.format(Date(lastBlocked.blockedAt))}"
                        )
                    } else {
                        views2.setTextViewText(
                            R.id.widget_last_blocked,
                            context.getString(R.string.widget_no_blocked)
                        )
                    }
                    awm.updateAppWidget(widgetId, views2)
                } catch (_: Exception) { /* widget shows partial data gracefully */ }
            }.start()
        }

        /** Called from CallBlockerService after a block event to refresh all widgets. */
        fun refreshAll(context: Context) {
            val awm = AppWidgetManager.getInstance(context)
            awm.getAppWidgetIds(ComponentName(context, StatusWidgetProvider::class.java))
                .forEach { updateWidget(context, awm, it) }
            awm.getAppWidgetIds(ComponentName(context, ToggleWidgetProvider::class.java))
                .forEach { ToggleWidgetProvider.updateWidget(context, awm, it) }
        }
    }
}
