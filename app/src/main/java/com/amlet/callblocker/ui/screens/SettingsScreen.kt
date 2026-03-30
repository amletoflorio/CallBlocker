package com.amlet.callblocker.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.amlet.callblocker.data.prefs.AppPreferences
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onExportBackup: (Context, Uri) -> Unit,
    onImportBackup: (Context, Uri) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }

    var notifyOnBlock by remember { mutableStateOf(prefs.notifyOnBlock) }
    var suspendUntil by remember { mutableStateOf(prefs.suspendUntil) }
    var showSuspendDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { onExportBackup(context, it) } }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { onImportBackup(context, it) } }

    if (showSuspendDialog) {
        SuspendDialog(
            onDismiss = { showSuspendDialog = false },
            onConfirm = { durationMs ->
                val until = System.currentTimeMillis() + durationMs
                prefs.suspendUntil = until
                suspendUntil = until
                showSuspendDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Impostazioni", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBack, "Indietro")
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // ── Protezione ───────────────────────────────────────────────────
            SectionHeader("Protezione")

            val isSuspended = suspendUntil > System.currentTimeMillis()
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSuspended)
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                    else
                        MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.PauseCircle,
                            contentDescription = null,
                            tint = if (isSuspended)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sospendi protezione",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isSuspended)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isSuspended)
                                    "Attiva fino a: ${formatTime(suspendUntil)}"
                                else
                                    "Consenti tutte le chiamate per un periodo",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isSuspended) {
                            Button(
                                onClick = {
                                    prefs.cancelSuspend()
                                    suspendUntil = 0L
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Riattiva ora")
                            }
                        } else {
                            listOf(
                                "1h" to TimeUnit.HOURS.toMillis(1),
                                "3h" to TimeUnit.HOURS.toMillis(3),
                                "24h" to TimeUnit.HOURS.toMillis(24)
                            ).forEach { (label, ms) ->
                                OutlinedButton(
                                    onClick = {
                                        val until = System.currentTimeMillis() + ms
                                        prefs.suspendUntil = until
                                        suspendUntil = until
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(label)
                                }
                            }
                            OutlinedButton(
                                onClick = { showSuspendDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── Notifiche ────────────────────────────────────────────────────
            SectionHeader("Notifiche")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Notifica chiamate bloccate",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Ricevi una notifica ogni volta che una chiamata viene bloccata",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = notifyOnBlock,
                        onCheckedChange = { checked ->
                            prefs.notifyOnBlock = checked
                            notifyOnBlock = checked
                        }
                    )
                }
            }

            // ── Backup ───────────────────────────────────────────────────────
            SectionHeader("Backup e ripristino")

            SettingsCard(
                icon = Icons.Rounded.Upload,
                title = "Esporta whitelist",
                subtitle = "Salva i tuoi contatti consentiti in un file JSON",
                onClick = { exportLauncher.launch("callblocker_backup.json") }
            )

            SettingsCard(
                icon = Icons.Rounded.Download,
                title = "Importa whitelist",
                subtitle = "Ripristina contatti da un file di backup JSON.\nAttenzione: sovrascriverà la lista attuale.",
                onClick = { importLauncher.launch(arrayOf("application/json")) },
                isDestructive = true
            )

            // ── Informazioni ─────────────────────────────────────────────────
            SectionHeader("Informazioni")

            SettingsCard(
                icon = Icons.Rounded.Info,
                title = "Versione",
                subtitle = "1.0.0",
                onClick = {}
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Rounded.Security, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Come funziona",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "CallBlocker usa il CallScreeningService di Android. " +
                                    "Nessun dato viene inviato a server esterni. " +
                                    "Tutto rimane sul tuo dispositivo.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Crediti ──────────────────────────────────────────────────────
            SectionHeader("Crediti")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "App sviluppata da Amlet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Fatto con cura per proteggere la tua privacy",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ── Dialog durata personalizzata ─────────────────────────────────────────────

@Composable
private fun SuspendDialog(
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var days by remember { mutableStateOf(0) }
    var hours by remember { mutableStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Schedule, contentDescription = null) },
        title = { Text("Sospendi protezione") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Scegli per quanto tempo sospendere il blocco chiamate.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Giorni
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Giorni", modifier = Modifier.width(60.dp), style = MaterialTheme.typography.bodyLarge)
                    IconButton(onClick = { if (days > 0) days-- }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.Remove, contentDescription = "Meno")
                    }
                    Text("$days", style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                    IconButton(onClick = { if (days < 30) days++ }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.Add, contentDescription = "Più")
                    }
                }
                // Ore
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Ore", modifier = Modifier.width(60.dp), style = MaterialTheme.typography.bodyLarge)
                    IconButton(onClick = { if (hours > 0) hours-- }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.Remove, contentDescription = "Meno")
                    }
                    Text("$hours", style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                    IconButton(onClick = { if (hours < 23) hours++ }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.Add, contentDescription = "Più")
                    }
                }
                if (days == 0 && hours == 0) {
                    Text("Seleziona almeno 1 ora", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val ms = TimeUnit.DAYS.toMillis(days.toLong()) + TimeUnit.HOURS.toMillis(hours.toLong())
                    onConfirm(ms)
                },
                enabled = days > 0 || hours > 0
            ) { Text("Sospendi") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatTime(timestampMs: Long): String =
    SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(timestampMs))

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp, start = 4.dp)
    )
}

@Composable
private fun SettingsCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
