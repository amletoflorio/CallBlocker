package com.amlet.callblocker.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.amlet.callblocker.CallBlockerApp
import com.amlet.callblocker.data.backup.BackupManager
import com.amlet.callblocker.data.db.BlockedCallEntity
import com.amlet.callblocker.data.db.ContactEntity
import com.amlet.callblocker.data.repository.ContactRepository
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

    /** Stream reattivo delle chiamate bloccate per la CallLogScreen */
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
                showSnackbar("Contatto aggiunto con successo")
            } catch (e: Exception) {
                showSnackbar("Errore: ${e.message}")
            }
        }
    }

    fun deleteContact(contact: ContactEntity) {
        viewModelScope.launch {
            repository.deleteContact(contact)
            showSnackbar("${contact.name} rimosso dalla whitelist")
        }
    }

    fun clearCallLog() {
        viewModelScope.launch {
            db.blockedCallDao().deleteAll()
            showSnackbar("Log svuotato")
        }
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
                onSuccess = {
                    val msg = "Backup esportato con successo (${contacts.size} contatti)"
                    showSnackbar(msg)
                },
                onFailure = {
                    val msg = "Errore export: ${it.message}"
                    showSnackbar(msg)
                }
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
                    val msg = "Importati ${contacts.size} contatti"
                    showSnackbar(msg)
                },
                onFailure = {
                    val msg = "Errore import: ${it.message}"
                    showSnackbar(msg)
                }
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
