package com.amlet.callblocker.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import androidx.core.app.NotificationCompat
import com.amlet.callblocker.CallBlockerApp
import com.amlet.callblocker.MainActivity
import com.amlet.callblocker.R
import com.amlet.callblocker.data.db.AppDatabase
import com.amlet.callblocker.data.db.BlockedCallEntity
import com.amlet.callblocker.data.prefs.AppPreferences
import com.amlet.callblocker.data.repository.ContactRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class CallBlockerService : CallScreeningService() {

    private lateinit var repository: ContactRepository
    private lateinit var prefs: AppPreferences
    private lateinit var db: AppDatabase

    // Scope per operazioni async (salvataggio log) — annullato in onDestroy
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        db = try {
            (application as CallBlockerApp).database
        } catch (e: Exception) {
            AppDatabase.getInstance(applicationContext)
        }
        repository = ContactRepository(db.contactDao())
        prefs = AppPreferences(applicationContext)
        createNotificationChannel()
    }

    override fun onScreenCall(callDetails: Call.Details) {
        val rawNumber = callDetails.handle?.schemeSpecificPart
        android.util.Log.d("CallBlocker", "=== CHIAMATA IN ARRIVO: $rawNumber ===")

        val response = try {
            if (prefs.isSuspended) {
                android.util.Log.d("CallBlocker", "Protezione sospesa → CONSENTO")
                buildAllowResponse()
            } else if (rawNumber.isNullOrBlank()) {
                android.util.Log.d("CallBlocker", "Numero vuoto → BLOCCO")
                logBlockedCall("Numero sconosciuto")
                notifyBlocked("Numero sconosciuto")
                buildBlockResponse()
            } else {
                val inContacts = isInSystemContacts(rawNumber)
                val inWhitelist = runBlocking { repository.findByNumber(rawNumber) != null }
                android.util.Log.d("CallBlocker", "In rubrica: $inContacts | In whitelist: $inWhitelist")

                if (inContacts || inWhitelist) {
                    android.util.Log.d("CallBlocker", "→ CONSENTO")
                    buildAllowResponse()
                } else {
                    android.util.Log.d("CallBlocker", "→ BLOCCO")
                    logBlockedCall(rawNumber)
                    notifyBlocked(rawNumber)
                    buildBlockResponse()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CallBlocker", "Errore: ${e.message}")
            buildAllowResponse()
        }

        respondToCall(callDetails, response)
    }

    private fun isInSystemContacts(number: String): Boolean {
        val uri = android.net.Uri.withAppendedPath(
            android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            android.net.Uri.encode(number)
        )
        return try {
            contentResolver.query(
                uri,
                arrayOf(android.provider.ContactsContract.PhoneLookup._ID),
                null, null, null
            )?.use { cursor -> cursor.count > 0 } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun buildBlockResponse(): CallResponse =
        CallResponse.Builder()
            .setDisallowCall(true)
            .setRejectCall(true)
            .setSkipCallLog(false)
            .setSkipNotification(true)
            .build()

    private fun buildAllowResponse(): CallResponse =
        CallResponse.Builder()
            .setRejectCall(false)
            .build()

    // ── Log ──────────────────────────────────────────────────────────────────

    private fun logBlockedCall(number: String) {
        serviceScope.launch {
            try {
                db.blockedCallDao().insert(BlockedCallEntity(phoneNumber = number))
            } catch (e: Exception) {
                android.util.Log.e("CallBlocker", "Errore salvataggio log: ${e.message}")
            }
        }
    }

    // ── Notifiche ────────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                AppPreferences.NOTIFICATION_CHANNEL_ID,
                AppPreferences.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifica quando una chiamata viene bloccata"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun notifyBlocked(number: String) {
        if (!prefs.notifyOnBlock) return
        val notificationManager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!notificationManager.areNotificationsEnabled()) return
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("navigate_to", "call_log")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, AppPreferences.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Chiamata bloccata")
            .setContentText("Numero: $number")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(number.hashCode(), notification)
    }
}
