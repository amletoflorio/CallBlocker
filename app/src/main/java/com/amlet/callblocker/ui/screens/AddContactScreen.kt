package com.amlet.callblocker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.amlet.callblocker.util.PhoneUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactScreen(
    onSave: (name: String, phone: String, notes: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        nameError = if (name.isBlank()) "Inserisci un nome" else null
        phoneError = when {
            phone.isBlank() -> "Inserisci un numero"
            !PhoneUtils.isValid(phone) -> "Numero non valido (min. 6 cifre)"
            else -> null
        }
        return nameError == null && phoneError == null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aggiungi contatto", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.Close, "Annulla")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text(
                "Aggiungi un numero alla whitelist. Questi contatti possono chiamarti anche se non sono in rubrica.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Campo nome
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = null },
                label = { Text("Nome *") },
                leadingIcon = { Icon(Icons.Rounded.Person, null) },
                isError = nameError != null,
                supportingText = { nameError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            // Campo numero
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it; phoneError = null },
                label = { Text("Numero di telefono *") },
                leadingIcon = { Icon(Icons.Rounded.Phone, null) },
                isError = phoneError != null,
                supportingText = {
                    phoneError?.let { Text(it) }
                        ?: Text("Es. +39 02 1234567 oppure 3391234567")
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            // Campo note
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Note (opzionale)") },
                leadingIcon = { Icon(Icons.Rounded.Notes, null) },
                placeholder = { Text("Es. Corriere BRT, Dentista...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            // Pulsante salva
            Button(
                onClick = {
                    if (validate()) {
                        onSave(name.trim(), phone.trim(), notes.trim())
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Rounded.Check, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Salva nella whitelist", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}