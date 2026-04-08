package com.amlet.callblocker

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.amlet.callblocker.ui.AppNavigation
import com.amlet.callblocker.ui.theme.CallBlockerTheme
import com.amlet.callblocker.util.LocaleHelper

class MainActivity : ComponentActivity() {

    private var isRoleHeld by mutableStateOf(false)

    private val roleRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkRoleStatus()
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* optional handling */ }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrapContext(newBase))
    }

    override fun onResume() {
        super.onResume()
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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            permissions.add(Manifest.permission.READ_PHONE_STATE)
        }
        permissionLauncher.launch(permissions.toTypedArray())

        checkRoleStatus()

        // Lanciato da BootReceiver dopo il riavvio: apri subito il dialog del ruolo.
        // Checked here (onCreate) and again in onNewIntent (if the Activity was already alive).
        if (intent.getBooleanExtra(EXTRA_REQUEST_ROLE_ON_LAUNCH, false)) {
            requestRole()
        }

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

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        // Handles the case where MainActivity was already in memory at boot.
        if (intent.getBooleanExtra(EXTRA_REQUEST_ROLE_ON_LAUNCH, false)) {
            requestRole()
        }
    }

    private fun checkRoleStatus() {
        val roleManager = getSystemService(RoleManager::class.java)
        isRoleHeld = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }

    private fun requestRole() {
        val roleManager = getSystemService(RoleManager::class.java)
        if (!roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
            val roleIntent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
            roleRequestLauncher.launch(roleIntent)
        }
    }

    companion object {
        const val EXTRA_REQUEST_ROLE_ON_LAUNCH = "request_role_on_launch"
    }
}
