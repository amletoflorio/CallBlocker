package com.amlet.callblocker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.amlet.callblocker.data.db.CategoryEntity
import com.amlet.callblocker.data.db.ContactEntity
import com.amlet.callblocker.ui.components.ContactCard

/**
 * Screen that displays the whitelist with search and optional category filtering.
 *
 * A horizontally scrollable chip row at the top allows the user to filter contacts
 * by category. Selecting the "All" chip clears the filter.
 *
 * @param contacts             Contacts already filtered by the ViewModel.
 * @param categories           All available categories (used to render the chip row).
 * @param categoryFilter       Currently selected category ID, or null for "all".
 * @param onCategoryFilterChange Callback to change the active category filter.
 * @param searchQuery          Current text in the search bar.
 * @param onSearchQueryChange  Updates the search query in the ViewModel.
 * @param onDeleteContact      Requests deletion of a contact after confirmation.
 * @param onAddContact         Navigates to the Add Contact screen.
 * @param onEditContact        Navigates to the Edit Contact screen.
 * @param onCallContact        Opens the system dialer pre-filled with the contact's number.
 * @param onNavigateBack       Pops this screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactListScreen(
    contacts: List<ContactEntity>,
    categories: List<CategoryEntity>,
    categoryFilter: Int?,
    onCategoryFilterChange: (Int?) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onDeleteContact: (ContactEntity) -> Unit,
    onAddContact: () -> Unit,
    onEditContact: (ContactEntity) -> Unit,
    onCallContact: (ContactEntity) -> Unit,
    onNavigateBack: () -> Unit
) {
    var contactToDelete by remember { mutableStateOf<ContactEntity?>(null) }

    // Build a fast lookup map so ContactCard can show the category name without extra queries.
    val categoryMap = remember(categories) { categories.associateBy { it.id } }

    contactToDelete?.let { contact ->
        AlertDialog(
            onDismissRequest = { contactToDelete = null },
            title = { Text(stringResource(R.string.contacts_delete_confirm)) },
            text = { Text(contact.name) },
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
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                placeholder = { Text(stringResource(R.string.contacts_search_hint)) },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
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

            // Category filter chips — only shown when there are categories
            if (categories.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    // "All" chip
                    item {
                        FilterChip(
                            selected = categoryFilter == null,
                            onClick = { onCategoryFilterChange(null) },
                            label = { Text(stringResource(R.string.contacts_filter_all), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    items(categories) { cat ->
                        FilterChip(
                            selected = categoryFilter == cat.id,
                            onClick = {
                                onCategoryFilterChange(if (categoryFilter == cat.id) null else cat.id)
                            },
                            label = {
                                Text(
                                    buildString {
                                        if (cat.emoji.isNotBlank()) { append(cat.emoji); append(" ") }
                                        append(cat.name)
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }
            }

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
                            if (searchQuery.isBlank() && categoryFilter == null)
                                stringResource(R.string.contacts_empty)
                            else if (searchQuery.isNotBlank())
                                "\"$searchQuery\""
                            else
                                stringResource(R.string.contacts_filter_empty),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (searchQuery.isBlank() && categoryFilter == null) {
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
                            category = contact.categoryId?.let { categoryMap[it] },
                            onDelete = { contactToDelete = contact },
                            onEdit = { onEditContact(contact) },
                            onCall = { onCallContact(contact) }
                        )
                    }
                }
            }
        }
    }
}
