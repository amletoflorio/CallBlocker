package com.amlet.callblocker

import android.Manifest
import android.app.role.RoleManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.amlet.callblocker.ui.AppNavigation
import com.amlet.callblocker.ui.theme.CallBlockerTheme

class MainActivity : ComponentActivity() {

    // True = il ruolo Call Screening è assegnato a questa app
    private var isRoleHeld by mutableStateOf(false)

    private val roleRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Dopo il dialog di sistema aggiorna lo stato reale del ruolo
        checkRoleStatus()
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* gestione opzionale */ }

    override fun onResume() {
        super.onResume()
        // Aggiorna sempre al ritorno nell'app (es. dopo revoca manuale del ruolo)
        checkRoleStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissions = mutableListOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())

        checkRoleStatus()

        setContent {
            CallBlockerTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNavigation(
                        isRoleHeld = isRoleHeld,
                        onRequestRole = ::requestRole
                    )
                }
            }
        }
    }

    private fun checkRoleStatus() {
        val roleManager = getSystemService(RoleManager::class.java)
        isRoleHeld = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }

    private fun requestRole() {
        val roleManager = getSystemService(RoleManager::class.java)
        if (!roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
            roleRequestLauncher.launch(intent)
        }
    }
}
