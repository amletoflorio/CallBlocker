package com.amlet.callblocker

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.amlet.callblocker.ui.AppNavigation
import com.amlet.callblocker.ui.theme.CallBlockerTheme

class MainActivity : ComponentActivity() {

    // Tiene traccia se l'utente ha concesso il ruolo di call screening
    private var isServiceEnabled by mutableStateOf(false)

    // Launcher per richiedere il ruolo CALL_SCREENING
    private val roleRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isServiceEnabled = result.resultCode == RESULT_OK
    }

    // Launcher per i permessi runtime (READ_CONTACTS, READ_CALL_LOG)
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* gestione opzionale */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Chiedi permessi runtime al primo avvio
        val permissions = mutableListOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())

        // Controlla se il servizio è già attivo
        checkServiceStatus()

        setContent {
            CallBlockerTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNavigation(
                        isServiceEnabled = isServiceEnabled,
                        onToggleService = ::toggleService
                    )
                }
            }
        }
    }

    private fun checkServiceStatus() {
        val roleManager = getSystemService(RoleManager::class.java)
        isServiceEnabled = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }

    private fun toggleService() {
        val roleManager = getSystemService(RoleManager::class.java)

        if (roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
            // Non è possibile revocare il ruolo programmaticamente —
            // l'utente deve farlo manualmente nelle impostazioni
            isServiceEnabled = false
            // Mostra istruzioni all'utente (apri impostazioni telefono)
        } else {
            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
            roleRequestLauncher.launch(intent)
        }
    }
}