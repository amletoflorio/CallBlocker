package com.amlet.callblocker

import android.app.Application
import com.amlet.callblocker.data.db.AppDatabase
import com.amlet.callblocker.util.NotificationHelper

class CallBlockerApp : Application() {
    // Il database viene inizializzato lazy (solo quando serve)
    val database by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        // Registra i canali di notifica richiesti da Android 8+
        NotificationHelper.createChannels(this)
    }
}