package com.amlet.callblocker.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amlet.callblocker.R
import com.amlet.callblocker.data.db.BlockedCallEntity
import com.amlet.callblocker.util.PhoneUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallDetailScreen(
    number: String,
    calls: List<BlockedCallEntity>,
    isInWhitelist: Boolean,
    onAddToWhitelist: (name: String, notes: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    // Derive all static info from the number locally — no network calls.
    val info = remember(number) { PhoneUtils.getNumberInfo(number) }

    var showWhitelistDialog by remember { mutableStateOf(false) }

    if (showWhitelistDialog) {
        WhitelistAddDialog(
            formattedNumber = info.formattedNumber,
            onConfirm = { name, notes ->
                onAddToWhitelist(name, notes)
                showWhitelistDialog = false
            },
            onDismiss = { showWhitelistDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.call_detail_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBack, stringResource(R.string.common_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Number hero card ──────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.PhoneDisabled, null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = info.formattedNumber,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.call_detail_attempts, calls.size),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // ── Number info card ──────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.call_detail_info_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        DetailRow(
                            icon = Icons.Rounded.Public,
                            label = stringResource(R.string.call_detail_country),
                            value = info.country
                        )

                        info.operator?.let { op ->
                            DetailRow(
                                icon = Icons.Rounded.CellTower,
                                label = stringResource(R.string.call_detail_operator),
                                value = op
                            )
                        }

                        DetailRow(
                            icon = Icons.Rounded.Phone,
                            label = stringResource(R.string.call_detail_type),
                            value = stringResource(
                                when (info.numberType) {
                                    PhoneUtils.NumberType.MOBILE    -> R.string.call_detail_type_mobile
                                    PhoneUtils.NumberType.LANDLINE  -> R.string.call_detail_type_landline
                                    PhoneUtils.NumberType.VOIP      -> R.string.call_detail_type_voip
                                    PhoneUtils.NumberType.TOLL_FREE -> R.string.call_detail_type_toll_free
                                    PhoneUtils.NumberType.UNKNOWN   -> R.string.call_detail_type_unknown
                                }
                            )
                        )


                        // First / last attempt timestamps.
                        if (calls.isNotEmpty()) {
                            val dtFmt = remember { SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.getDefault()) }
                            val latest = remember(calls) { calls.maxByOrNull { it.blockedAt } }
                            val earliest = remember(calls) { calls.minByOrNull { it.blockedAt } }

                            DetailRow(
                                icon = Icons.Rounded.AccessTime,
                                label = stringResource(R.string.call_detail_last_attempt),
                                value = latest?.let { dtFmt.format(Date(it.blockedAt)) } ?: "—"
                            )

                            if (calls.size > 1) {
                                DetailRow(
                                    icon = Icons.Rounded.History,
                                    label = stringResource(R.string.call_detail_first_attempt),
                                    value = earliest?.let { dtFmt.format(Date(it.blockedAt)) } ?: "—"
                                )
                            }
                        }
                    }
                }
            }

            // ── Action buttons ────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Add to whitelist — show badge if already added
                    if (isInWhitelist) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.CheckCircle, null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    stringResource(R.string.call_detail_already_whitelisted),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = { showWhitelistDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Shield, null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.call_detail_add_whitelist))
                        }
                    }

                    // Search online — opens browser, user decides what to do.
                    OutlinedButton(
                        onClick = {
                            val query = Uri.encode(number)
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.google.com/search?q=$query")
                            )
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Search, null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.call_detail_search_online))
                    }
                }
            }

            // ── Call history header ───────────────────────────────────────────
            if (calls.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.call_detail_history_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items(calls, key = { it.id }) { call ->
                    CallAttemptRow(call)
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon, null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CallAttemptRow(call: BlockedCallEntity) {
    val dtFmt = remember { SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            // ── Top row: icon + date/time + SIM badge ─────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Rounded.PhoneMissed, null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = dtFmt.format(Date(call.blockedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                call.simSlot?.let { sim ->
                    AttemptBadge(
                        text = sim,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // ── Second row: call direction + account handle (debug) ───────────
            val hasExtras = call.callDirection != null || call.accountHandleId != null
            if (hasExtras) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    call.callDirection?.let { dir ->
                        val dirIcon = when (dir) {
                            "Incoming" -> Icons.Rounded.CallReceived
                            "Outgoing" -> Icons.Rounded.CallMade
                            else       -> Icons.Rounded.CallMissed
                        }
                        Icon(
                            dirIcon, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        AttemptBadge(
                            text = dir,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    call.accountHandleId?.let { handle ->
                        AttemptBadge(
                            text = handle.take(24),   // cap length for readability
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/** Small pill-shaped badge used inside CallAttemptRow. */
@Composable
private fun AttemptBadge(
    text: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = containerColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun WhitelistAddDialog(
    formattedNumber: String,
    onConfirm: (name: String, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name  by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.PlaylistAdd, contentDescription = null) },
        title = { Text(stringResource(R.string.call_detail_whitelist_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.call_detail_whitelist_subtitle, formattedNumber),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Name field (required)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.call_detail_whitelist_name_label)) },
                    placeholder = { Text(stringResource(R.string.call_detail_whitelist_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Notes field (optional)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.call_detail_whitelist_notes_label)) },
                    placeholder = { Text(stringResource(R.string.call_detail_whitelist_notes_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), notes.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.call_detail_whitelist_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}