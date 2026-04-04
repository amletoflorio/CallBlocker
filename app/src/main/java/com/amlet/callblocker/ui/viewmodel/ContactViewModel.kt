package com.amlet.callblocker.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.amlet.callblocker.CallBlockerApp
import com.amlet.callblocker.R
import com.amlet.callblocker.data.backup.BackupManager
import com.amlet.callblocker.data.db.BlockedCallEntity
import com.amlet.callblocker.data.db.ContactEntity
import com.amlet.callblocker.data.prefs.AppPreferences
import com.amlet.callblocker.data.repository.ContactRepository
import com.amlet.callblocker.util.PhoneUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UiState(
    val contacts: List<ContactEntity> = emptyList(),
    val contactCount: Int = 0,
    val blockedCount: Int = 0,
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val snackbarMessage: String? = null
)

class ContactViewModel(app: Application) : AndroidViewModel(app) {

    private val repository: ContactRepository
    private val db = (app as CallBlockerApp).database

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val filteredContacts: StateFlow<List<ContactEntity>>

    val blockedCalls: StateFlow<List<BlockedCallEntity>> =
        db.blockedCallDao().getAllBlocked()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        repository = ContactRepository(db.contactDao())

        filteredContacts = combine(
            repository.allContacts,
            _uiState.map { it.searchQuery }
        ) { contacts, query ->
            if (query.isBlank()) contacts
            else contacts.filter { contact ->
                contact.name.contains(query, ignoreCase = true) ||
                        contact.phoneNumber.contains(query) ||
                        contact.notes.contains(query, ignoreCase = true)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        viewModelScope.launch {
            repository.contactCount.collect { count ->
                _uiState.update { it.copy(contactCount = count) }
            }
        }

        viewModelScope.launch {
            db.blockedCallDao().getBlockedCount().collect { count ->
                _uiState.update { it.copy(blockedCount = count) }
            }
        }
    }

    fun addContact(name: String, phoneNumber: String, notes: String) {
        viewModelScope.launch {
            try {
                repository.addContact(name, phoneNumber, notes)
                showSnackbar(getApplication<Application>().getString(R.string.msg_contact_added))
            } catch (e: Exception) {
                showSnackbar(getApplication<Application>().getString(R.string.msg_generic_error, e.message))
            }
        }
    }

    fun updateContact(contact: ContactEntity) {
        viewModelScope.launch {
            try {
                repository.updateContact(contact)
                showSnackbar(getApplication<Application>().getString(R.string.msg_contact_updated))
            } catch (e: Exception) {
                showSnackbar(getApplication<Application>().getString(R.string.msg_generic_error, e.message))
            }
        }
    }

    fun getContactById(id: Int): ContactEntity? {
        return _uiState.value.contacts.firstOrNull { it.id == id }
            ?: filteredContacts.value.firstOrNull { it.id == id }
    }

    fun deleteContact(contact: ContactEntity) {
        viewModelScope.launch {
            repository.deleteContact(contact)
            showSnackbar(getApplication<Application>().getString(R.string.msg_contact_deleted, contact.name))
        }
    }

    fun clearCallLog() {
        viewModelScope.launch {
            db.blockedCallDao().deleteAll()
            showSnackbar(getApplication<Application>().getString(R.string.msg_log_cleared))
        }
    }

    /**
     * Applies the log retention policy immediately (e.g. when the user changes
     * the setting). No-op when retentionDays is 0.
     */
    fun applyLogRetention(retentionDays: Int) {
        if (retentionDays <= 0) return
        viewModelScope.launch {
            val cutoffMs = System.currentTimeMillis() - retentionDays * 24L * 60 * 60 * 1000
            db.blockedCallDao().deleteOlderThan(cutoffMs)
        }
    }

    /**
     * Adds [phoneNumber] directly to the whitelist with an auto-generated name.
     * Used by the "Add to whitelist" quick-action on the call detail screen.
     */
    /**
     * Adds [phoneNumber] to the whitelist with a user-supplied name and optional notes.
     * Called from the "Add to whitelist" dialog on the call detail screen.
     */
    fun quickAddToWhitelist(phoneNumber: String, name: String, notes: String) {
        viewModelScope.launch {
            try {
                // Check if already present to avoid duplicates.
                if (repository.findByNumber(phoneNumber) != null) {
                    showSnackbar(getApplication<Application>().getString(R.string.msg_already_in_whitelist))
                    return@launch
                }
                repository.addContact(
                    name = name.ifBlank { phoneNumber },
                    phoneNumber = phoneNumber,
                    notes = notes
                )
                showSnackbar(getApplication<Application>().getString(R.string.msg_added_to_whitelist))
            } catch (e: Exception) {
                showSnackbar(getApplication<Application>().getString(R.string.msg_generic_error, e.message))
            }
        }
    }

    /** Returns a cold flow of all blocked call attempts for the given number. */
    fun callsForNumber(number: String): Flow<List<BlockedCallEntity>> =
        db.blockedCallDao().getCallsForNumber(number)

    /**
     * Returns a cold flow that emits true when [number] is present in the whitelist.
     * Normalises the number before checking so +39xxx and 39xxx both match.
     */
    fun isInWhitelist(number: String): Flow<Boolean> =
        repository.allContacts.map { contacts ->
            val normalised = PhoneUtils.normalize(number)
            contacts.any { PhoneUtils.normalize(it.phoneNumber) == normalised }
        }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun exportBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val contacts = repository.allContacts.first()
            val result = BackupManager.exportToUri(context, uri, contacts)
            result.fold(
                onSuccess = { showSnackbar(context.getString(R.string.msg_export_success, contacts.size)) },
                onFailure = { showSnackbar(context.getString(R.string.msg_export_error, it.message)) }
            )
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun importBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = BackupManager.importFromUri(context, uri)
            result.fold(
                onSuccess = { contacts ->
                    repository.replaceAll(contacts)
                    showSnackbar(context.getString(R.string.msg_import_success, contacts.size))
                },
                onFailure = { showSnackbar(context.getString(R.string.msg_import_error, it.message)) }
            )
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private fun showSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }
}
