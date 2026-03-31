package com.amlet.callblocker.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.amlet.callblocker.R

/**
 * Invia notifiche persistenti al completamento di operazioni di backup/ripristino.
 * Usa un canale dedicato "Backup" per non interferire con le notifiche di blocco chiamate.
 */
object NotificationHelper {

    private const val CHANNEL_BACKUP = "backup_restore"
    private const val CHANNEL_BACKUP_NAME = "Backup e ripristino"
    private const val CHANNEL_BACKUP_DESC = "Notifiche al completamento di export e import"

    private const val NOTIF_ID_BACKUP = 1001

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_BACKUP,
                CHANNEL_BACKUP_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_BACKUP_DESC
            }
            nm.createNotificationChannel(channel)
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
}
