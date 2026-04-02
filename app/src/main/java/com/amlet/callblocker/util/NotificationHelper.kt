package com.amlet.callblocker.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.amlet.callblocker.R

object NotificationHelper {

    private const val CHANNEL_BACKUP      = "backup_restore"
    private const val CHANNEL_BACKUP_NAME = "Backup e ripristino"
    private const val CHANNEL_BACKUP_DESC = "Notifiche al completamento di export e import"

    private const val CHANNEL_AUTO_BACKUP      = "auto_backup"
    private const val CHANNEL_AUTO_BACKUP_NAME = "Backup automatico"
    private const val CHANNEL_AUTO_BACKUP_DESC = "Notifiche silenziose al completamento del backup automatico"

    private const val NOTIF_ID_BACKUP      = 1001
    private const val NOTIF_ID_AUTO_BACKUP = 1003

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)

            // Canale backup manuale
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_BACKUP, CHANNEL_BACKUP_NAME, NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = CHANNEL_BACKUP_DESC }
            )

            // Canale backup automatico — silenziosa (LOW importance, no sound)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_AUTO_BACKUP, CHANNEL_AUTO_BACKUP_NAME, NotificationManager.IMPORTANCE_LOW)
                    .apply { description = CHANNEL_AUTO_BACKUP_DESC }
            )
        }
    }

    fun notifyBackupResult(context: Context, success: Boolean, message: String) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(context, CHANNEL_BACKUP)
            .setSmallIcon(
                if (success) android.R.drawable.stat_sys_upload_done
                else android.R.drawable.stat_notify_error
            )
            .setContentTitle(if (success) "Backup completato" else "Errore backup")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIF_ID_BACKUP, notification)
    }

    fun notifyRestoreResult(context: Context, success: Boolean, message: String) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(context, CHANNEL_BACKUP)
            .setSmallIcon(
                if (success) android.R.drawable.stat_sys_download_done
                else android.R.drawable.stat_notify_error
            )
            .setContentTitle(if (success) "Ripristino completato" else "Errore ripristino")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIF_ID_BACKUP + 1, notification)
    }

    /** Notifica silenziosa al termine del backup automatico */
    fun notifyAutoBackupSuccess(context: Context, contactCount: Int) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val message = context.getString(R.string.notif_auto_backup_success_text, contactCount)
        val notification = NotificationCompat.Builder(context, CHANNEL_AUTO_BACKUP)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentTitle(context.getString(R.string.notif_auto_backup_success_title))
            .setContentText(message)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIF_ID_AUTO_BACKUP, notification)
    }

    /** Notifica silenziosa in caso di errore nel backup automatico */
    fun notifyAutoBackupError(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(context, CHANNEL_AUTO_BACKUP)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(context.getString(R.string.notif_auto_backup_error_title))
            .setContentText(context.getString(R.string.notif_auto_backup_error_text))
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIF_ID_AUTO_BACKUP, notification)
    }
}
