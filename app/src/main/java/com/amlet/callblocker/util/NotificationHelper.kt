package com.amlet.callblocker.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.amlet.callblocker.R

object NotificationHelper {

    private const val CHANNEL_BACKUP           = "backup_restore"
    private const val CHANNEL_BACKUP_NAME      = "Backup & restore"
    private const val CHANNEL_BACKUP_DESC      = "Notifications on export and import completion"

    private const val CHANNEL_AUTO_BACKUP      = "auto_backup"
    private const val CHANNEL_AUTO_BACKUP_NAME = "Automatic backup"
    private const val CHANNEL_AUTO_BACKUP_DESC = "Silent notifications on automatic backup completion"

    private const val NOTIF_ID_BACKUP      = 1001
    private const val NOTIF_ID_AUTO_BACKUP = 1003

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)

            // Manual backup channel
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_BACKUP, CHANNEL_BACKUP_NAME, NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = CHANNEL_BACKUP_DESC }
            )

            // Auto backup channel — silent (LOW importance, no sound)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_AUTO_BACKUP, CHANNEL_AUTO_BACKUP_NAME, NotificationManager.IMPORTANCE_LOW)
                    .apply { description = CHANNEL_AUTO_BACKUP_DESC }
            )
        }
    }

    fun notifyBackupResult(context: Context, success: Boolean, message: String) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(context, CHANNEL_BACKUP)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(largeIcon(context))
                .setColor(0xFF10B981.toInt())
            .setContentTitle(context.getString(if (success) R.string.notif_backup_success_title else R.string.notif_backup_error_title))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIF_ID_BACKUP, notification)
    }

    fun notifyRestoreResult(context: Context, success: Boolean, message: String) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(context, CHANNEL_BACKUP)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(largeIcon(context))
                .setColor(0xFF10B981.toInt())
            .setContentTitle(context.getString(if (success) R.string.notif_restore_success_title else R.string.notif_restore_error_title))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIF_ID_BACKUP + 1, notification)
    }

    /** Silent notification on successful automatic backup */
    fun notifyAutoBackupSuccess(context: Context, contactCount: Int) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val message = context.getString(R.string.notif_auto_backup_success_text, contactCount)
        val notification = NotificationCompat.Builder(context, CHANNEL_AUTO_BACKUP)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(largeIcon(context))
                .setColor(0xFF10B981.toInt())
            .setContentTitle(context.getString(R.string.notif_auto_backup_success_title))
            .setContentText(message)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIF_ID_AUTO_BACKUP, notification)
    }

    /** Silent notification on automatic backup error */
    fun notifyAutoBackupError(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(context, CHANNEL_AUTO_BACKUP)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(largeIcon(context))
                .setColor(0xFF10B981.toInt())
            .setContentTitle(context.getString(R.string.notif_auto_backup_error_title))
            .setContentText(context.getString(R.string.notif_auto_backup_error_text))
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIF_ID_AUTO_BACKUP, notification)
    }


    private fun largeIcon(context: android.content.Context): android.graphics.Bitmap {
        val size = (48 * context.resources.displayMetrics.density).toInt()
        val bmp = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        val drawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_notification)!!
        drawable.setBounds(0, 0, size, size)
        androidx.core.graphics.drawable.DrawableCompat.setTint(drawable, 0xFF10B981.toInt())
        drawable.draw(canvas)
        return bmp
    }
}
