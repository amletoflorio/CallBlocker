package com.amlet.callblocker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amlet.callblocker.R
import com.amlet.callblocker.data.db.ContactEntity
import com.amlet.callblocker.ui.components.ContactCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactListScreen(
    contacts: List<ContactEntity>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onDeleteContact: (ContactEntity) -> Unit,
    onAddContact: () -> Unit,
    onEditContact: (ContactEntity) -> Unit,
    onNavigateBack: () -> Unit
) {
    var contactToDelete by remember { mutableStateOf<ContactEntity?>(null) }

    contactToDelete?.let { contact ->
        AlertDialog(
            onDismissRequest = { contactToDelete = null },
            title = { Text(stringResource(R.string.contacts_delete_confirm)) },
            text = { Text("${contact.name}") },
            confirmButton = {
                TextButton(onClick = { onDeleteContact(contact); contactToDelete = null }) {
                    Text(stringResource(R.string.contacts_delete_confirm_btn), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { contactToDelete = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.contacts_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBack, stringResource(R.string.common_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddContact,
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text(stringResource(R.string.common_add)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                placeholder = { Text(stringResource(R.string.contacts_search_hint)) },
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Rounded.Clear, stringResource(R.string.contacts_search_clear))
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                singleLine = true
            )

            if (contacts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Rounded.ContactPhone, null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            if (searchQuery.isBlank())
                                stringResource(R.string.contacts_empty)
                            else
                                "\"$searchQuery\"",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (searchQuery.isBlank()) {
                            Text(
                                stringResource(R.string.contacts_add_description),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(contacts, key = { it.id }) { contact ->
                        ContactCard(
                            contact = contact,
                            onDelete = { contactToDelete = contact },
                            onEdit = { onEditContact(contact) }
                        )
                    }
                }
            }
        }
    }
}
