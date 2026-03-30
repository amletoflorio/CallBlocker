package com.amlet.callblocker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.amlet.callblocker.ui.screens.*
import com.amlet.callblocker.ui.viewmodel.ContactViewModel

object Routes {
    const val HOME = "home"
    const val CONTACTS = "contacts"
    const val ADD_CONTACT = "add_contact"
    const val SETTINGS = "settings"
    const val CALL_LOG = "call_log"
}

@Composable
fun AppNavigation(
    isServiceEnabled: Boolean,
    onToggleService: () -> Unit
) {
    val navController = rememberNavController()
    val viewModel: ContactViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val filteredContacts by viewModel.filteredContacts.collectAsState()
    val blockedCalls by viewModel.blockedCalls.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                contactCount = uiState.contactCount,
                blockedCount = uiState.blockedCount,
                isServiceEnabled = isServiceEnabled,
                onToggleService = onToggleService,
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
