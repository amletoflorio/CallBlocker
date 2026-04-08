package com.amlet.callblocker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.amlet.callblocker.R
import com.amlet.callblocker.data.db.CategoryEntity
import com.amlet.callblocker.data.db.ContactEntity
import com.amlet.callblocker.util.PhoneUtils

/**
 * Screen for editing an existing whitelist contact.
 *
 * Pre-populates all fields from the [contact] argument. The category picker
 * allows changing or clearing the category assignment, and a new category can
 * be created inline via [onCreateCategory].
 *
 * @param contact          The contact to edit.
 * @param categories       Live list of available categories.
 * @param onSave           Called with the updated [ContactEntity] when the form is valid.
 * @param onCreateCategory Called with (name, emoji, callback) to create a new category on the fly.
 * @param onNavigateBack   Pops this screen from the back stack.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditContactScreen(
    contact: ContactEntity,
    categories: List<CategoryEntity>,
    onSave: (ContactEntity) -> Unit,
    onCreateCategory: (name: String, emoji: String, onResult: (Long) -> Unit) -> Unit,
    onNavigateBack: () -> Unit
) {
    var name          by remember { mutableStateOf(contact.name) }
    var phone         by remember { mutableStateOf(contact.phoneNumber) }
    var notes         by remember { mutableStateOf(contact.notes) }
    var selectedCatId by remember { mutableStateOf(contact.categoryId) }

    var nameError  by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }

    var showCreateCatDialog by remember { mutableStateOf(false) }

    val errNameRequired  = stringResource(R.string.add_contact_name_hint)
    val errPhoneRequired = stringResource(R.string.add_contact_phone_hint)
    val errPhoneInvalid  = stringResource(R.string.add_contact_phone_error)

    fun validate(): Boolean {
        nameError  = if (name.isBlank()) errNameRequired else null
        phoneError = when {
            phone.isBlank()            -> errPhoneRequired
            !PhoneUtils.isValid(phone) -> errPhoneInvalid
            else                       -> null
        }
        return nameError == null && phoneError == null
    }

    if (showCreateCatDialog) {
        CreateCategoryDialog(
            onConfirm = { catName, catEmoji ->
                onCreateCategory(catName, catEmoji) { newId ->
                    if (newId > 0) selectedCatId = newId.toInt()
                }
                showCreateCatDialog = false
            },
            onDismiss = { showCreateCatDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_contact_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.Close, stringResource(R.string.add_contact_cancel))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                stringResource(R.string.edit_contact_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = null },
                label = { Text(stringResource(R.string.add_contact_name_label)) },
                leadingIcon = { Icon(Icons.Rounded.Person, null) },
                isError = nameError != null,
                supportingText = { nameError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it; phoneError = null },
                label = { Text(stringResource(R.string.add_contact_phone_label)) },
                leadingIcon = { Icon(Icons.Rounded.Phone, null) },
                isError = phoneError != null,
                supportingText = {
                    phoneError?.let { Text(it) }
                        ?: Text(stringResource(R.string.add_contact_phone_hint))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.add_contact_notes_label)) },
                leadingIcon = { Icon(Icons.Rounded.Notes, null) },
                placeholder = { Text(stringResource(R.string.add_contact_notes_hint)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
            )

            CategoryPickerSection(
                categories = categories,
                selectedCategoryId = selectedCatId,
                onSelectCategory = { id -> selectedCatId = if (selectedCatId == id) null else id },
                onCreateCategory = { showCreateCatDialog = true }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (validate()) {
                        onSave(
                            contact.copy(
                                name = name.trim(),
                                phoneNumber = phone.trim(),
                                notes = notes.trim(),
                                categoryId = selectedCatId
                            )
                        )
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Rounded.Check, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.add_contact_save), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
