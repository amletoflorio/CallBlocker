package com.amlet.callblocker.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
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

    /** Converte l'icona launcher in una Bitmap 192x192 da usare come large icon. */
    fun getLargeIcon(context: Context): Bitmap? = try {
        val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
        val bitmap = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable?.setBounds(0, 0, 192, 192)
        drawable?.draw(canvas)
        bitmap
    } catch (e: Exception) { null }

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)

            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_BACKUP, CHANNEL_BACKUP_NAME, NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = CHANNEL_BACKUP_DESC }
            )

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
            .setLargeIcon(getLargeIcon(context))
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
            .setLargeIcon(getLargeIcon(context))
            .setColor(0xFF10B981.toInt())
            .setContentTitle(context.getString(if (success) R.string.notif_restore_success_title else R.string.notif_restore_error_title))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIF_ID_BACKUP + 1, notification)
    }

    fun notifyAutoBackupSuccess(context: Context, contactCount: Int) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val message = context.getString(R.string.notif_auto_backup_success_text, contactCount)
        val notification = NotificationCompat.Builder(context, CHANNEL_AUTO_BACKUP)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(getLargeIcon(context))
            .setColor(0xFF10B981.toInt())
            .setContentTitle(context.getString(R.string.notif_auto_backup_success_title))
            .setContentText(message)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIF_ID_AUTO_BACKUP, notification)
    }

    fun notifyAutoBackupError(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(context, CHANNEL_AUTO_BACKUP)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(getLargeIcon(context))
            .setColor(0xFF10B981.toInt())
            .setContentTitle(context.getString(R.string.notif_auto_backup_error_title))
            .setContentText(context.getString(R.string.notif_auto_backup_error_text))
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIF_ID_AUTO_BACKUP, notification)
    }
}