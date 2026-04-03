package com.amlet.callblocker.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.amlet.callblocker.BuildConfig
import com.amlet.callblocker.MainActivity
import com.amlet.callblocker.R
import com.amlet.callblocker.data.prefs.AppPreferences
import com.amlet.callblocker.util.UpdateChecker

class UpdateCheckWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = AppPreferences(context)

        // Do nothing if the user has disabled auto-check or notifications
        if (!prefs.checkUpdatesEnabled) return Result.success()

        prefs.lastUpdateCheckAt = System.currentTimeMillis()

        return when (val result = UpdateChecker.checkForUpdate(BuildConfig.VERSION_NAME)) {
            is UpdateChecker.UpdateResult.UpdateAvailable -> {
                if (prefs.notifyOnUpdate) {
                    notifyUpdateAvailable(result.info)
                }
                Result.success()
            }
            is UpdateChecker.UpdateResult.UpToDate -> Result.success()
            is UpdateChecker.UpdateResult.Error    -> Result.retry()
        }
    }

    private fun notifyUpdateAvailable(info: UpdateChecker.UpdateInfo) {
        val nm = context.getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            )
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("navigate_to", "settings_updates")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.update_dialog_title))
            .setContentText(
                context.getString(R.string.update_notif_text, info.latestVersion)
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIF_ID, notification)
    }

    companion object {
        const val WORK_NAME   = "update_check"
        const val CHANNEL_ID  = "update_check"
        const val CHANNEL_NAME = "App updates"
        private const val NOTIF_ID = 2001
    }
}
