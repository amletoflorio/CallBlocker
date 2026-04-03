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
    const val HOME        = "home"
    const val CONTACTS    = "contacts"
    const val ADD_CONTACT = "add_contact"
    const val EDIT_CONTACT = "edit_contact/{contactId}"
    const val SETTINGS    = "settings"
    const val CALL_LOG    = "call_log"

    fun editContact(contactId: Int) = "edit_contact/$contactId"
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

    var isSuspended by remember { mutableStateOf(prefs.isSuspended) }
    val isProtectionActive = isRoleHeld && !isSuspended

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

    val onToggleProtection: () -> Unit = {
        if (!isRoleHeld) {
            onRequestRole()
        } else if (isSuspended) {
            prefs.cancelSuspend()
            isSuspended = false
        } else {
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
                    onNavigateToCallLog  = { navController.navigate(Routes.CALL_LOG) }
                )
            }

            composable(Routes.CONTACTS) {
                ContactListScreen(
                    contacts = filteredContacts,
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    onDeleteContact = viewModel::deleteContact,
                    onAddContact = { navController.navigate(Routes.ADD_CONTACT) },
                    onEditContact = { contact ->
                        navController.navigate(Routes.editContact(contact.id))
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.ADD_CONTACT) {
                AddContactScreen(
                    onSave = viewModel::addContact,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.EDIT_CONTACT) { backStackEntry ->
                val contactId = backStackEntry.arguments?.getString("contactId")?.toIntOrNull()
                val contact = contactId?.let { id ->
                    filteredContacts.firstOrNull { it.id == id }
                }
                if (contact != null) {
                    EditContactScreen(
                        contact = contact,
                        onSave = viewModel::updateContact,
                        onNavigateBack = { navController.popBackStack() }
                    )
                } else {
                    // Contact not found — go back
                    LaunchedEffect(Unit) { navController.popBackStack() }
                }
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
