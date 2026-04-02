package com.amlet.callblocker.ui.screens

import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.amlet.callblocker.BuildConfig
import com.amlet.callblocker.CallBlockerApp
import com.amlet.callblocker.R
import com.amlet.callblocker.data.prefs.AppPreferences
import com.amlet.callblocker.util.UpdateChecker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

private enum class SettingsTab { PROTEZIONE, BACKUP, INFO }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onExportBackup: (Context, Uri) -> Unit,
    onImportBackup: (Context, Uri) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(SettingsTab.PROTEZIONE) }

    val tabLabels = listOf(
        stringResource(R.string.settings_tab_protection),
        stringResource(R.string.settings_tab_backup),
        stringResource(R.string.settings_tab_info)
    )
    val tabIcons = listOf(Icons.Rounded.Shield, Icons.Rounded.Backup, Icons.Rounded.Info)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBack, stringResource(R.string.common_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
            ) {
                SettingsTab.entries.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tabIcons[index], tabLabels[index], modifier = Modifier.size(18.dp)) },
                        text = { Text(tabLabels[index], style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            when (selectedTab) {
                SettingsTab.PROTEZIONE -> ProtezioneTab(context)
                SettingsTab.BACKUP     -> BackupTab(context, onExportBackup, onImportBackup)
                SettingsTab.INFO       -> InfoTab(context)
            }
        }
    }
}

// ── Tab: Protezione ───────────────────────────────────────────────────────────

@Composable
private fun ProtezioneTab(context: Context) {
    val prefs = remember { AppPreferences(context) }
    var notifyOnBlock by remember { mutableStateOf(prefs.notifyOnBlock) }
    var suspendUntil by remember { mutableStateOf(prefs.suspendUntil) }
    var showSuspendDialog by remember { mutableStateOf(false) }

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

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

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
                        Icons.Rounded.PauseCircle, null,
                        tint = if (isSuspended) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.settings_suspend_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isSuspended) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            if (isSuspended)
                                stringResource(R.string.settings_suspend_active_until, formatTime(suspendUntil))
                            else
                                stringResource(R.string.settings_suspend_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isSuspended) {
                        Button(
                            onClick = { prefs.cancelSuspend(); suspendUntil = 0L },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.settings_suspend_resume))
                        }
                    } else {
                        listOf("1h" to TimeUnit.HOURS.toMillis(1), "3h" to TimeUnit.HOURS.toMillis(3), "24h" to TimeUnit.HOURS.toMillis(24))
                            .forEach { (label, ms) ->
                                OutlinedButton(
                                    onClick = { val until = System.currentTimeMillis() + ms; prefs.suspendUntil = until; suspendUntil = until },
                                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)
                                ) { Text(label) }
                            }
                        OutlinedButton(onClick = { showSuspendDialog = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Rounded.Schedule, null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Notifications, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_notify_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.settings_notify_subtitle),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = notifyOnBlock, onCheckedChange = { prefs.notifyOnBlock = it; notifyOnBlock = it })
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ── Tab: Backup ───────────────────────────────────────────────────────────────

@Composable
private fun BackupTab(context: Context, onExportBackup: (Context, Uri) -> Unit, onImportBackup: (Context, Uri) -> Unit) {
    val prefs = remember { AppPreferences(context) }
    var intervalDays by remember { mutableStateOf(prefs.autoBackupIntervalDays) }
    val lastBackupAt by remember { mutableStateOf(prefs.lastAutoBackupAt) }
    var folderUriString by remember { mutableStateOf(prefs.autoBackupFolderUri) }

    val filename = stringResource(R.string.settings_backup_filename)
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { onExportBackup(context, it) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { onImportBackup(context, it) }
    }

    // Folder picker per il backup automatico — richiede permesso persistente
    val folderPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            // Acquisisce il permesso persistente così il Worker può accedere anche in background
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            prefs.autoBackupFolderUri = uri.toString()
            folderUriString = uri.toString()
        }
    }

    // Opzioni frequenza: 0 = disabilitato, 1, 7, 30
    val intervalOptions = listOf(
        0  to stringResource(R.string.settings_auto_backup_off),
        1  to stringResource(R.string.settings_auto_backup_daily),
        7  to stringResource(R.string.settings_auto_backup_weekly),
        30 to stringResource(R.string.settings_auto_backup_monthly)
    )

    // Etichetta leggibile della cartella selezionata
    val folderLabel = folderUriString?.let { uriStr ->
        runCatching {
            val uri = Uri.parse(uriStr)
            // Estrae solo il nome della cartella dall'URI SAF (es. "Documenti/CallBackup")
            uri.lastPathSegment?.substringAfterLast(':') ?: uriStr
        }.getOrNull()
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // ── Backup automatico ────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AutoMode, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(stringResource(R.string.settings_auto_backup_title), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.settings_auto_backup_subtitle),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Chip di selezione frequenza
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    intervalOptions.forEach { (days, label) ->
                        FilterChip(
                            selected = intervalDays == days,
                            onClick = {
                                intervalDays = days
                                prefs.autoBackupIntervalDays = days
                                (context.applicationContext as CallBlockerApp).rescheduleAutoBackup()
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Selezione cartella — visibile solo se il backup automatico è abilitato
                if (intervalDays > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Folder, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.settings_auto_backup_folder),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                folderLabel ?: stringResource(R.string.settings_auto_backup_folder_default),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = { folderPickerLauncher.launch(null) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.settings_auto_backup_folder_choose))
                        }
                    }
                }

                // Ultima esportazione automatica
                if (lastBackupAt > 0L) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CheckCircle, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.settings_auto_backup_last, formatDateTime(lastBackupAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // ── Backup manuale ───────────────────────────────────────────────────
        SettingsCard(Icons.Rounded.Upload, stringResource(R.string.settings_export_title), stringResource(R.string.settings_export_subtitle),
            onClick = { exportLauncher.launch(filename) })
        SettingsCard(Icons.Rounded.Download, stringResource(R.string.settings_import_title), stringResource(R.string.settings_import_subtitle),
            onClick = { importLauncher.launch(arrayOf("application/json")) }, isDestructive = true)

        Spacer(modifier = Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
            Row(modifier = Modifier.padding(16.dp)) {
                Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(stringResource(R.string.settings_backup_info),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ── Tab: Info ─────────────────────────────────────────────────────────────────

@Composable
private fun InfoTab(context: Context) {
    val prefs = remember { AppPreferences(context) }
    val scope = rememberCoroutineScope()

    var checkUpdatesEnabled by remember { mutableStateOf(prefs.checkUpdatesEnabled) }
    var updateDialogInfo by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }
    var updateChecking by remember { mutableStateOf(false) }
    var updateUpToDate by remember { mutableStateOf(false) }
    var updateError by remember { mutableStateOf<String?>(null) }

    // Dialog: aggiornamento disponibile
    updateDialogInfo?.let { info ->
        AlertDialog(
            onDismissRequest = { updateDialogInfo = null },
            icon = { Icon(Icons.Rounded.SystemUpdate, null) },
            title = { Text(stringResource(R.string.update_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.update_dialog_body, BuildConfig.VERSION_NAME, info.latestVersion))
                    if (info.releaseNotes.isNotBlank()) {
                        HorizontalDivider()
                        Text(info.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl)))
                    updateDialogInfo = null
                }) { Text(stringResource(R.string.update_dialog_download)) }
            },
            dismissButton = {
                TextButton(onClick = { updateDialogInfo = null }) {
                    Text(stringResource(R.string.update_dialog_later))
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // ── Versione + controllo aggiornamenti ───────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.NewReleases, null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_version_title), style = MaterialTheme.typography.titleMedium)
                        Text(BuildConfig.VERSION_NAME, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Toggle controllo aggiornamenti
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_check_updates_title),
                            style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(R.string.settings_check_updates_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = checkUpdatesEnabled,
                        onCheckedChange = {
                            checkUpdatesEnabled = it
                            prefs.checkUpdatesEnabled = it
                            // Reset stati UI quando si disabilita
                            if (!it) { updateUpToDate = false; updateError = null }
                        }
                    )
                }

                // Pulsante "Controlla ora"
                if (checkUpdatesEnabled) {
                    Button(
                        onClick = {
                            updateUpToDate = false
                            updateError = null
                            updateChecking = true
                            scope.launch {
                                when (val result = UpdateChecker.checkForUpdate(BuildConfig.VERSION_NAME)) {
                                    is UpdateChecker.UpdateResult.UpdateAvailable -> {
                                        updateDialogInfo = result.info
                                    }
                                    is UpdateChecker.UpdateResult.UpToDate -> {
                                        updateUpToDate = true
                                    }
                                    is UpdateChecker.UpdateResult.Error -> {
                                        updateError = result.message
                                    }
                                }
                                updateChecking = false
                            }
                        },
                        enabled = !updateChecking,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (updateChecking) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(stringResource(R.string.settings_check_updates_btn))
                    }

                    when {
                        updateUpToDate -> {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Rounded.CheckCircle, null,
                                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Text(stringResource(R.string.settings_check_updates_up_to_date),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        updateError != null -> {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Rounded.Warning, null,
                                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Text(stringResource(R.string.settings_check_updates_error, updateError!!),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        SettingsCard(Icons.Rounded.Code, stringResource(R.string.settings_github_title), stringResource(R.string.settings_github_subtitle),
            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/amletoflorio/CallBlocker"))) })

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(modifier = Modifier.padding(16.dp)) {
                Icon(Icons.Rounded.Security, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(stringResource(R.string.settings_how_it_works_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.settings_how_it_works_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Favorite, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(stringResource(R.string.settings_credits_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.settings_credits_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ── Dialog sospensione ────────────────────────────────────────────────────────

@Composable
private fun SuspendDialog(onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    var days by remember { mutableStateOf(0) }
    var hours by remember { mutableStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Schedule, null) },
        title = { Text(stringResource(R.string.settings_suspend_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.settings_suspend_dialog_body),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_suspend_dialog_days), modifier = Modifier.width(60.dp), style = MaterialTheme.typography.bodyLarge)
                    IconButton(onClick = { if (days > 0) days-- }, modifier = Modifier.size(36.dp)) { Icon(Icons.Rounded.Remove, stringResource(R.string.common_less)) }
                    Text("$days", style = MaterialTheme.typography.titleLarge, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                    IconButton(onClick = { if (days < 30) days++ }, modifier = Modifier.size(36.dp)) { Icon(Icons.Rounded.Add, stringResource(R.string.common_more)) }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_suspend_dialog_hours), modifier = Modifier.width(60.dp), style = MaterialTheme.typography.bodyLarge)
                    IconButton(onClick = { if (hours > 0) hours-- }, modifier = Modifier.size(36.dp)) { Icon(Icons.Rounded.Remove, stringResource(R.string.common_less)) }
                    Text("$hours", style = MaterialTheme.typography.titleLarge, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                    IconButton(onClick = { if (hours < 23) hours++ }, modifier = Modifier.size(36.dp)) { Icon(Icons.Rounded.Add, stringResource(R.string.common_more)) }
                }
                if (days == 0 && hours == 0) {
                    Text(stringResource(R.string.settings_suspend_dialog_min_error),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(TimeUnit.DAYS.toMillis(days.toLong()) + TimeUnit.HOURS.toMillis(hours.toLong())) },
                enabled = days > 0 || hours > 0) { Text(stringResource(R.string.settings_suspend_dialog_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_suspend_dialog_cancel)) }
        }
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatTime(timestampMs: Long): String =
    SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(timestampMs))

private fun formatDateTime(timestampMs: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timestampMs))

@Composable
private fun SettingsCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String, subtitle: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null,
                tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium,
                    color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
