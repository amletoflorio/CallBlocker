package com.amlet.callblocker

import android.app.Application
import androidx.work.*
import com.amlet.callblocker.data.db.AppDatabase
import com.amlet.callblocker.data.prefs.AppPreferences
import com.amlet.callblocker.util.NotificationHelper
import com.amlet.callblocker.worker.AutoBackupWorker
import com.amlet.callblocker.worker.UpdateCheckWorker
import java.util.concurrent.TimeUnit

class CallBlockerApp : Application() {

    val database by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        scheduleAutoBackupIfNeeded()
        scheduleUpdateCheckIfNeeded()
    }

    /**
     * Schedules (or cancels) the periodic backup based on the user's preference.
     * Called at startup — WorkManager is idempotent: if the work already exists
     * with the same name and KEEP policy, it won't be recreated.
     */
    fun scheduleAutoBackupIfNeeded() {
        val prefs = AppPreferences(this)
        val intervalDays = prefs.autoBackupIntervalDays
        val wm = WorkManager.getInstance(this)

        if (intervalDays <= 0) {
            wm.cancelUniqueWork(AutoBackupWorker.WORK_NAME)
            return
        }

        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(intervalDays.toLong(), TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()

        wm.enqueueUniquePeriodicWork(
            AutoBackupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /**
     * Reschedules the backup with a new interval.
     * Called from Settings whenever the user changes the frequency.
     */
    fun rescheduleAutoBackup() {
        val prefs = AppPreferences(this)
        val intervalDays = prefs.autoBackupIntervalDays
        val wm = WorkManager.getInstance(this)

        if (intervalDays <= 0) {
            wm.cancelUniqueWork(AutoBackupWorker.WORK_NAME)
            return
        }

        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(intervalDays.toLong(), TimeUnit.DAYS).build()

        // REPLACE: replaces the existing work with the new interval
        wm.enqueueUniquePeriodicWork(
            AutoBackupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
    }

    /**
     * Schedules (or cancels) the periodic update check (every 24h).
     * Only active when the user has enabled auto-check in Settings.
     */
    fun scheduleUpdateCheckIfNeeded() {
        val prefs = AppPreferences(this)
        val wm = WorkManager.getInstance(this)

        if (!prefs.checkUpdatesEnabled) {
            wm.cancelUniqueWork(UpdateCheckWorker.WORK_NAME)
            return
        }

        val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(24, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        wm.enqueueUniquePeriodicWork(
            UpdateCheckWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
