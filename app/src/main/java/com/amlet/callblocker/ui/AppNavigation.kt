package com.amlet.callblocker.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.amlet.callblocker.data.prefs.AppPreferences
import com.amlet.callblocker.ui.screens.*
import com.amlet.callblocker.ui.viewmodel.ContactViewModel
import kotlinx.coroutines.launch

object Routes {
    const val HOME = "home"
    const val CONTACTS = "contacts"
    const val ADD_CONTACT = "add_contact"
    const val SETTINGS = "settings"
    const val CALL_LOG = "call_log"
}

@Composable
fun AppNavigation(
    isRoleHeld: Boolean,
    onRequestRole: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }

    val navController = rememberNavController()
    val viewModel: ContactViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val filteredContacts by viewModel.filteredContacts.collectAsState()
    val blockedCalls by viewModel.blockedCalls.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Stato sospensione — riletto a ogni recomposition
    var isSuspended by remember { mutableStateOf(prefs.isSuspended) }

    // Il toggle è "attivo" (verde) se il ruolo è held E la protezione non è sospesa
    val isProtectionActive = isRoleHeld && !isSuspended

    // Mostra snackbar per messaggi dal ViewModel
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
            }
            viewModel.clearSnackbar()
        }
    }

    // Callback toggle: se il ruolo non è held lo richiede;
    // altrimenti attiva/disattiva la sospensione indefinita
    val onToggleProtection: () -> Unit = {
        if (!isRoleHeld) {
            onRequestRole()
        } else if (isSuspended) {
            prefs.cancelSuspend()
            isSuspended = false
        } else {
            // Sospendi indefinitamente (Long.MAX_VALUE come sentinella)
            prefs.suspendUntil = Long.MAX_VALUE
            isSuspended = true
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { _ ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    contactCount = uiState.contactCount,
                    blockedCount = uiState.blockedCount,
                    isServiceEnabled = isProtectionActive,
                    onToggleService = onToggleProtection,
                    onNavigateToContacts = { navController.navigate(Routes.CONTACTS) },
                    onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                    onNavigateToCallLog = { navController.navigate(Routes.CALL_LOG) }
                )
            }

            composable(Routes.CONTACTS) {
                ContactListScreen(
                    contacts = filteredContacts,
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    onDeleteContact = viewModel::deleteContact,
                    onAddContact = { navController.navigate(Routes.ADD_CONTACT) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.ADD_CONTACT) {
                AddContactScreen(
                    onSave = viewModel::addContact,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onExportBackup = viewModel::exportBackup,
                    onImportBackup = viewModel::importBackup,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.CALL_LOG) {
                CallLogScreen(
                    blockedCalls = blockedCalls,
                    onClearLog = viewModel::clearCallLog,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
