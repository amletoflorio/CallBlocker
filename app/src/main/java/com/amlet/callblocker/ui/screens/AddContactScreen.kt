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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.amlet.callblocker.R
import com.amlet.callblocker.data.db.CategoryEntity
import com.amlet.callblocker.util.PhoneUtils

/**
 * Screen for adding a new contact to the whitelist.
 *
 * The user can optionally assign an existing category or create a new one
 * inline without leaving this screen. The [onCreateCategory] callback lets the
 * ViewModel create the category and return its generated ID so it can be
 * immediately selected.
 *
 * @param categories       Live list of available categories.
 * @param onSave           Called with (name, phone, notes, categoryId?) when the form is valid.
 * @param onCreateCategory Called with (name, emoji, callback) to create a new category on the fly.
 * @param onNavigateBack   Pops this screen from the back stack.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactScreen(
    categories: List<CategoryEntity>,
    onSave: (name: String, phone: String, notes: String, categoryId: Int?) -> Unit,
    onCreateCategory: (name: String, emoji: String, onResult: (Long) -> Unit) -> Unit,
    onNavigateBack: () -> Unit
) {
    var name          by remember { mutableStateOf("") }
    var phone         by remember { mutableStateOf("") }
    var notes         by remember { mutableStateOf("") }
    var selectedCatId by remember { mutableStateOf<Int?>(null) }

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
                title = { Text(stringResource(R.string.add_contact_title), fontWeight = FontWeight.Bold) },
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
                stringResource(R.string.add_contact_description),
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
                        onSave(name.trim(), phone.trim(), notes.trim(), selectedCatId)
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

// ── Shared composables used by Add and Edit screens ───────────────────────────

/**
 * Row of filter chips for selecting a category. Includes an "None" chip to
 * clear the selection and a text button to open the "Create category" dialog.
 *
 * Selecting a chip that is already selected deselects it (toggles to null).
 */
@Composable
internal fun CategoryPickerSection(
    categories: List<CategoryEntity>,
    selectedCategoryId: Int?,
    onSelectCategory: (Int) -> Unit,
    onCreateCategory: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.add_contact_category_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (categories.isEmpty()) {
            OutlinedButton(onClick = onCreateCategory, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Rounded.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.category_create))
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // "None" chip — always unselected state when a category is selected
                FilterChip(
                    selected = selectedCategoryId == null,
                    onClick = { onSelectCategory(-1) },
                    label = { Text(stringResource(R.string.category_none), style = MaterialTheme.typography.labelSmall) }
                )
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategoryId == cat.id,
                        onClick = { onSelectCategory(cat.id) },
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
            TextButton(onClick = onCreateCategory, contentPadding = PaddingValues(0.dp)) {
                Icon(Icons.Rounded.Add, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.category_create), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/**
 * Dialog that lets the user create a new category on the fly.
 * Asks for a name (required) and an optional emoji prefix (max 2 chars).
 */
@Composable
internal fun CreateCategoryDialog(
    onConfirm: (name: String, emoji: String) -> Unit,
    onDismiss: () -> Unit
) {
    var catName   by remember { mutableStateOf("") }
    var catEmoji  by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Label, null) },
        title = { Text(stringResource(R.string.category_create_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = catName,
                    onValueChange = { catName = it; nameError = false },
                    label = { Text(stringResource(R.string.category_name_label)) },
                    placeholder = { Text(stringResource(R.string.category_name_hint)) },
                    isError = nameError,
                    supportingText = { if (nameError) Text(stringResource(R.string.category_name_required)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = catEmoji,
                    onValueChange = { if (it.length <= 2) catEmoji = it },
                    label = { Text(stringResource(R.string.category_emoji_label)) },
                    placeholder = { Text(stringResource(R.string.category_emoji_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (catName.isBlank()) nameError = true
                else onConfirm(catName.trim(), catEmoji.trim())
            }) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}
