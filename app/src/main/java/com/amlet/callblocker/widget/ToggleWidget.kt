package com.amlet.callblocker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.amlet.callblocker.MainActivity
import com.amlet.callblocker.R
import com.amlet.callblocker.data.prefs.AppPreferences

/**
 * Toggle Widget (2x1) — On/Off button for protection.
 *
 * The app's source-of-truth for "active" is:
 *   isRoleHeld  &&  suspendUntil <= now
 *
 * The widget cannot grant/revoke the CallScreeningService role, so it mirrors
 * the app's own toggle logic:
 *   • If the role is held and protection is active  → suspend indefinitely
 *   • If suspended                                  → cancel suspension
 *   • If role not held                              → open app to activate
 */
class ToggleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { updateWidget(context, appWidgetManager, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE) {
            val prefs = AppPreferences(context)
            val isSuspended = prefs.isSuspended

            if (isSuspended) {
                // Resume protection
                prefs.cancelSuspend()
            } else {
                // Suspend indefinitely (same as app toggle-off)
                prefs.suspendUntil = Long.MAX_VALUE
            }

            // Refresh both widget types
            val awm = AppWidgetManager.getInstance(context)
            awm.getAppWidgetIds(ComponentName(context, ToggleWidgetProvider::class.java))
                .forEach { updateWidget(context, awm, it) }
            awm.getAppWidgetIds(ComponentName(context, StatusWidgetProvider::class.java))
                .forEach { StatusWidgetProvider.updateWidget(context, awm, it) }
        }
    }

    companion object {
        const val ACTION_TOGGLE = "com.amlet.callblocker.widget.ACTION_TOGGLE"

        fun updateWidget(context: Context, awm: AppWidgetManager, widgetId: Int) {
            val prefs = AppPreferences(context)
            // Active = role is NOT suspended (we can't check RoleManager from a widget easily,
            // but suspension is the only thing the widget can control)
            val isSuspended = prefs.isSuspended
            val views = RemoteViews(context.packageName, R.layout.widget_toggle)

            // Toggle intent
            val toggleIntent = Intent(context, ToggleWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE
            }
            val togglePi = PendingIntent.getBroadcast(
                context, 0, toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_toggle_root, togglePi)

            // Open app intent (tap on label)
            val openPi = PendingIntent.getActivity(
                context, 1,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_toggle_label, openPi)

            // Visuals
            val statusText = if (isSuspended)
                context.getString(R.string.widget_status_inactive)
            else
                context.getString(R.string.widget_status_active)
            views.setTextViewText(R.id.widget_toggle_status, statusText)
            views.setTextViewText(R.id.widget_toggle_label, context.getString(R.string.app_name))

            val tint = if (isSuspended) 0xFF94A3B8.toInt() else 0xFF10B981.toInt()
            views.setInt(R.id.widget_toggle_icon, "setColorFilter", tint)

            awm.updateAppWidget(widgetId, views)
        }
    }
}
