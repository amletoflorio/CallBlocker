package com.amlet.callblocker.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.amlet.callblocker.data.prefs.AppPreferences
import com.amlet.callblocker.ui.screens.*
import com.amlet.callblocker.ui.viewmodel.ContactViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object Routes {
    const val HOME         = "home"
    const val CONTACTS     = "contacts"
    const val ADD_CONTACT  = "add_contact"
    const val EDIT_CONTACT = "edit_contact/{contactId}"
    const val SETTINGS     = "settings"
    const val CALL_LOG     = "call_log"
    const val CALL_DETAIL  = "call_detail/{phoneNumber}"

    fun editContact(contactId: Int) = "edit_contact/$contactId"
    fun callDetail(phoneNumber: String) =
        "call_detail/${java.net.URLEncoder.encode(phoneNumber, "UTF-8")}"
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
    val categories by viewModel.categories.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // ── Suspension state ──────────────────────────────────────────────────────
    //
    // suspendUntil is a MutableState so any write (toggle, settings, auto-expiry)
    // triggers recomposition. isSuspended is derived from it so it stays in sync
    // without any manual polling.
    //
    var suspendUntil by remember { mutableStateOf(prefs.suspendUntil) }
    val isSuspended = suspendUntil > System.currentTimeMillis()
    val isProtectionActive = isRoleHeld && !isSuspended

    // Auto-expiry: when a timed suspension is active, wait exactly until the
    // deadline and then clear it — no polling, no missed wakeups.
    LaunchedEffect(suspendUntil) {
        val remaining = suspendUntil - System.currentTimeMillis()
        // Only arm the timer for real timed suspensions (not Long.MAX_VALUE = indefinite).
        if (remaining in 1..(7 * 24 * 60 * 60 * 1000L)) {
            delay(remaining)
            prefs.cancelSuspend()
            suspendUntil = 0L
        }
    }

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
            suspendUntil = 0L
        } else {
            prefs.suspendUntil = Long.MAX_VALUE
            suspendUntil = Long.MAX_VALUE
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
                // Re-read suspended state every time Home becomes the active destination
                // (covers the case where the user changed it in Settings).
                val backStackEntry = it
                LaunchedEffect(backStackEntry) {
                    suspendUntil = prefs.suspendUntil
                }

                HomeScreen(
                    contactCount = uiState.contactCount,
                    blockedCount = uiState.blockedCount,
                    isServiceEnabled = isProtectionActive,
                    suspendUntil = if (isSuspended) suspendUntil else 0L,
                    onToggleService = onToggleProtection,
                    onNavigateToContacts = { navController.navigate(Routes.CONTACTS) },
                    onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                    onNavigateToCallLog  = { navController.navigate(Routes.CALL_LOG) }
                )
            }

            composable(Routes.CONTACTS) {
                ContactListScreen(
                    contacts = filteredContacts,
                    categories = categories,
                    categoryFilter = uiState.categoryFilter,
                    onCategoryFilterChange = viewModel::setCategoryFilter,
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    onDeleteContact = viewModel::deleteContact,
                    onAddContact = { navController.navigate(Routes.ADD_CONTACT) },
                    onEditContact = { contact ->
                        navController.navigate(Routes.editContact(contact.id))
                    },
                    onCallContact = { contact ->
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:${contact.phoneNumber}")
                        }
                        context.startActivity(intent)
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.ADD_CONTACT) {
                AddContactScreen(
                    categories = categories,
                    onSave = { name, phone, notes, categoryId ->
                        viewModel.addContact(name, phone, notes, categoryId)
                    },
                    onCreateCategory = { name, emoji, onResult ->
                        viewModel.addCategory(name, emoji, onResult)
                    },
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
                        categories = categories,
                        onSave = viewModel::updateContact,
                        onCreateCategory = { name, emoji, onResult ->
                            viewModel.addCategory(name, emoji, onResult)
                        },
                        onNavigateBack = { navController.popBackStack() }
                    )
                } else {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                }
            }

            composable(Routes.SETTINGS) {
                // Sync suspendUntil when returning from Settings so the auto-expiry
                // LaunchedEffect re-arms itself with the new deadline if changed.
                DisposableEffect(Unit) {
                    onDispose { suspendUntil = prefs.suspendUntil }
                }
                SettingsScreen(
                    onExportBackup = viewModel::exportBackup,
                    onImportBackup = viewModel::importBackup,
                    onApplyLogRetention = viewModel::applyLogRetention,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.CALL_LOG) {
                CallLogScreen(
                    blockedCalls = blockedCalls,
                    onClearLog = viewModel::clearCallLog,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenDetail = { number ->
                        navController.navigate(Routes.callDetail(number))
                    }
                )
            }

            composable(Routes.CALL_DETAIL) { backStackEntry ->
                val rawEncoded = backStackEntry.arguments?.getString("phoneNumber") ?: ""
                val phoneNumber = java.net.URLDecoder.decode(rawEncoded, "UTF-8")

                // Collect the flow here so it is stable across recompositions.
                val calls by produceState(initialValue = emptyList<com.amlet.callblocker.data.db.BlockedCallEntity>(), phoneNumber) {
                    viewModel.callsForNumber(phoneNumber).collect { value = it }
                }
                val isInWhitelist by produceState(initialValue = false, phoneNumber) {
                    viewModel.isInWhitelist(phoneNumber).collect { value = it }
                }

                CallDetailScreen(
                    number = phoneNumber,
                    calls = calls,
                    isInWhitelist = isInWhitelist,
                    onNavigateBack = { navController.popBackStack() },
                    onAddToWhitelist = { name, notes ->
                        viewModel.quickAddToWhitelist(phoneNumber, name, notes)
                    }
                )
            }
        }
    }
}
