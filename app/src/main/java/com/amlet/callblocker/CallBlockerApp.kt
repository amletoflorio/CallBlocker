package com.amlet.callblocker

import android.app.Application
import com.amlet.callblocker.data.db.AppDatabase
import com.amlet.callblocker.data.prefs.AppPreferences
import com.amlet.callblocker.util.NotificationHelper
import com.amlet.callblocker.worker.AutoBackupWorker
import androidx.work.*
import java.util.concurrent.TimeUnit

class CallBlockerApp : Application() {

    val database by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        scheduleAutoBackupIfNeeded()
    }

    /**
     * Pianifica (o annulla) il backup periodico in base alla preferenza dell'utente.
     * Viene chiamata all'avvio — WorkManager è idempotente: se il lavoro esiste già
     * con lo stesso nome e policy KEEP, non lo ricrea.
     */
    fun scheduleAutoBackupIfNeeded() {
        val prefs = AppPreferences(this)
        val intervalDays = prefs.autoBackupIntervalDays

        val wm = WorkManager.getInstance(this)

        if (intervalDays <= 0) {
            // Backup automatico disabilitato → cancella lavoro esistente
            wm.cancelUniqueWork(AutoBackupWorker.WORK_NAME)
            return
        }

        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(
            intervalDays.toLong(), TimeUnit.DAYS
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()

        wm.enqueueUniquePeriodicWork(
            AutoBackupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,  // non interrompere se già pianificato
            request
        )
    }

    /**
     * Ripianifica il backup con un nuovo intervallo (chiamata dalle Impostazioni
     * quando l'utente cambia la frequenza).
     */
    fun rescheduleAutoBackup() {
        val prefs = AppPreferences(this)
        val intervalDays = prefs.autoBackupIntervalDays

        val wm = WorkManager.getInstance(this)

        if (intervalDays <= 0) {
            wm.cancelUniqueWork(AutoBackupWorker.WORK_NAME)
            return
        }

        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(
            intervalDays.toLong(), TimeUnit.DAYS
        ).build()

        // REPLACE: sostituisce il lavoro esistente con il nuovo intervallo
        wm.enqueueUniquePeriodicWork(
            AutoBackupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
    }
}
