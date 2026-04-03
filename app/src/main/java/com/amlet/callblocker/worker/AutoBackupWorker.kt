package com.amlet.callblocker.worker

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.amlet.callblocker.CallBlockerApp
import com.amlet.callblocker.data.backup.BackupManager
import com.amlet.callblocker.data.prefs.AppPreferences
import com.amlet.callblocker.util.NotificationHelper
import kotlinx.coroutines.flow.first
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AutoBackupWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = (context.applicationContext as CallBlockerApp).database
            val contacts = db.contactDao().getAllContacts().first()
            val prefs = AppPreferences(context)

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            val filename = "callblocker_backup_$timestamp.json"

            val folderUriString = prefs.autoBackupFolderUri
            val result = if (!folderUriString.isNullOrBlank()) {
                // User-selected folder via SAF
                val folderUri = Uri.parse(folderUriString)
                val folder = DocumentFile.fromTreeUri(context, folderUri)
                val newFile = folder?.createFile("application/json", filename)
                val fileUri = newFile?.uri

                if (fileUri != null) {
                    BackupManager.exportToUri(context, fileUri, contacts)
                } else {
                    NotificationHelper.notifyAutoBackupError(context)
                    return Result.failure()
                }
            } else {
                // Fallback: Documents/CallBlocker/
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    "CallBlocker"
                )
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, filename)
                BackupManager.exportToFile(context, file, contacts)
            }

            if (result.isSuccess) {
                prefs.lastAutoBackupAt = System.currentTimeMillis()
                NotificationHelper.notifyAutoBackupSuccess(context, contacts.size)
                Result.success()
            } else {
                NotificationHelper.notifyAutoBackupError(context)
                Result.failure()
            }
        } catch (e: Exception) {
            NotificationHelper.notifyAutoBackupError(context)
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "auto_backup"
    }
}
