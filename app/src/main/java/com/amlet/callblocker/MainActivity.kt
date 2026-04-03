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

    // True = the Call Screening role is held by this app
    private var isRoleHeld by mutableStateOf(false)

    private val roleRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // After the system dialog, update the actual role state
        checkRoleStatus()
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* optional handling */ }

    override fun attachBaseContext(newBase: Context) {
        // Apply the saved language preference before the Activity inflates any views
        super.attachBaseContext(LocaleHelper.wrapContext(newBase))
    }

    override fun onResume() {
        super.onResume()
        // Always refresh when returning to the app (e.g. after manual role revocation)
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
        // Request READ_PHONE_STATE for dual-SIM detection
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            permissions.add(Manifest.permission.READ_PHONE_STATE)
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
