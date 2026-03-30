package com.amlet.callblocker

import android.app.Application
import com.amlet.callblocker.data.db.AppDatabase

class CallBlockerApp : Application() {
    // Il database viene inizializzato lazy (solo quando serve)
    val database by lazy { AppDatabase.getInstance(this) }
}