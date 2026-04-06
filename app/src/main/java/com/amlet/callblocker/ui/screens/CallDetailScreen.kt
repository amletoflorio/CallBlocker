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
import androidx.compose.ui.graphics.Color
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
    val info = remember(number) { PhoneUtils.getNumberInfo(number) }
    var showWhitelistDialog by remember { mutableStateOf(false) }

    if (showWhitelistDialog) {
        WhitelistAddDialog(
            formattedNumber = info.formattedNumber,
            onConfirm = { name, notes -> onAddToWhitelist(name, notes); showWhitelistDialog = false },
            onDismiss = { showWhitelistDialog = false }
        )
    }

    val anyVerif = remember(calls) { calls.firstOrNull { it.verificationStatus != null }?.verificationStatus }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.call_detail_title), fontWeight = FontWeight.Bold) },
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Hero card ─────────────────────────────────────────────────────
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Surface(shape = CircleShape,
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.PhoneDisabled, null,
                                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(30.dp))
                            }
                        }
                        Column {
                            Text(info.formattedNumber, style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(2.dp))
                            Text(stringResource(R.string.call_detail_attempts, calls.size),
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // ── Info card ─────────────────────────────────────────────────────
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.call_detail_info_title),
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                        DetailRow(Icons.Rounded.Public, stringResource(R.string.call_detail_country), info.country)

                        info.operator?.let { op ->
                            DetailRow(Icons.Rounded.CellTower, stringResource(R.string.call_detail_operator), op)
                        }

                        DetailRow(Icons.Rounded.Phone, stringResource(R.string.call_detail_type),
                            stringResource(when (info.numberType) {
                                PhoneUtils.NumberType.MOBILE    -> R.string.call_detail_type_mobile
                                PhoneUtils.NumberType.LANDLINE  -> R.string.call_detail_type_landline
                                PhoneUtils.NumberType.VOIP      -> R.string.call_detail_type_voip
                                PhoneUtils.NumberType.TOLL_FREE -> R.string.call_detail_type_toll_free
                                PhoneUtils.NumberType.UNKNOWN   -> R.string.call_detail_type_unknown
                            }))

                        // SIM received on — show the most recent non-null simSlot
                        val latestSimSlot = remember(calls) {
                            calls.sortedByDescending { it.blockedAt }.firstOrNull { it.simSlot != null }?.simSlot
                        }
                        latestSimSlot?.let { sim ->
                            DetailRow(Icons.Rounded.SimCard, stringResource(R.string.call_detail_sim_received), sim)
                        }

                        if (calls.isNotEmpty()) {
                            val dtFmt = remember { SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.getDefault()) }
                            val latest   = remember(calls) { calls.maxByOrNull { it.blockedAt } }
                            val earliest = remember(calls) { calls.minByOrNull { it.blockedAt } }
                            DetailRow(Icons.Rounded.AccessTime, stringResource(R.string.call_detail_last_attempt),
                                latest?.let { dtFmt.format(Date(it.blockedAt)) } ?: "—")
                            if (calls.size > 1) {
                                DetailRow(Icons.Rounded.History, stringResource(R.string.call_detail_first_attempt),
                                    earliest?.let { dtFmt.format(Date(it.blockedAt)) } ?: "—")
                            }
                        }
                    }
                }
            }

            // ── STIR/SHAKEN explanation (if any call has a status) ────────────
            if (anyVerif != null) {
                item { StirShakenInfoCard(anyVerif) }
            }

            // ── Action buttons ────────────────────────────────────────────────
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (isInWhitelist) {
                        Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer) {
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.call_detail_already_whitelisted),
                                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    } else {
                        Button(onClick = { showWhitelistDialog = true }, modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Rounded.Shield, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.call_detail_add_whitelist))
                        }
                    }
                    OutlinedButton(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(number)}"))) },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Rounded.Search, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.call_detail_search_online))
                    }
                }
            }

            // ── Call history ──────────────────────────────────────────────────
            if (calls.isNotEmpty()) {
                item {
                    Text(stringResource(R.string.call_detail_history_title),
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp))
                }
                items(calls, key = { it.id }) { call -> CallAttemptRow(call) }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun StirShakenInfoCard(status: String) {
    val cardBg = when (status) {
        BlockedCallEntity.VERIF_PASSED -> Color(0xFF166534).copy(alpha = 0.10f)
        BlockedCallEntity.VERIF_FAILED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val cardFg = when (status) {
        BlockedCallEntity.VERIF_PASSED -> Color(0xFF16A34A)
        BlockedCallEntity.VERIF_FAILED -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val titleRes = when (status) {
        BlockedCallEntity.VERIF_PASSED -> R.string.call_detail_verif_passed_title
        BlockedCallEntity.VERIF_FAILED -> R.string.call_detail_verif_failed_title
        else -> R.string.call_detail_verif_not_verified_title
    }
    val bodyRes = when (status) {
        BlockedCallEntity.VERIF_PASSED -> R.string.call_detail_verif_passed_body
        BlockedCallEntity.VERIF_FAILED -> R.string.call_detail_verif_failed_body
        else -> R.string.call_detail_verif_not_verified_body
    }
    val iconVec = when (status) {
        BlockedCallEntity.VERIF_PASSED -> Icons.Rounded.VerifiedUser
        BlockedCallEntity.VERIF_FAILED -> Icons.Rounded.GppBad
        else -> Icons.Rounded.GppMaybe
    }

    Surface(shape = RoundedCornerShape(12.dp), color = cardBg, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(iconVec, null, tint = cardFg, modifier = Modifier.size(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(titleRes), style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold, color = cardFg)
                Text(stringResource(bodyRes), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun CallAttemptRow(call: BlockedCallEntity) {
    val dtFmt = remember { SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.getDefault()) }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val rowIcon = when (call.allowReason) {
                    BlockedCallEntity.ALLOW_OUTGOING_RECENT -> Icons.Rounded.PhoneCallback
                    BlockedCallEntity.ALLOW_RETRY_RULE      -> Icons.Rounded.PhoneForwarded
                    else -> Icons.Rounded.PhoneMissed
                }
                val rowTint = if (call.allowReason != null) MaterialTheme.colorScheme.primary
                              else MaterialTheme.colorScheme.error
                Icon(rowIcon, null, tint = rowTint, modifier = Modifier.size(16.dp))
                Text(dtFmt.format(Date(call.blockedAt)), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                call.simSlot?.let { sim ->
                    AttemptBadge(sim, MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        MaterialTheme.colorScheme.onSecondaryContainer)
                }
                call.verificationStatus?.let { vs -> VerificationBadge(vs) }
            }

            call.allowReason?.let { reason ->
                Spacer(Modifier.height(4.dp))
                val label = when (reason) {
                    BlockedCallEntity.ALLOW_OUTGOING_RECENT -> stringResource(R.string.call_detail_allow_reason_dialed)
                    BlockedCallEntity.ALLOW_RETRY_RULE      -> stringResource(R.string.call_detail_allow_reason_retry)
                    else -> reason
                }
                AttemptBadge(label, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    MaterialTheme.colorScheme.primary)
            }

            val hasExtras = call.callDirection != null || call.accountHandleId != null
            if (hasExtras) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    call.callDirection?.let { dir ->
                        val dirIcon = when (dir) { "Incoming" -> Icons.Rounded.CallReceived; "Outgoing" -> Icons.Rounded.CallMade; else -> Icons.Rounded.CallMissed }
                        Icon(dirIcon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(13.dp))
                        AttemptBadge(dir, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    call.accountHandleId?.let { handle ->
                        AttemptBadge(handle.take(24), MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun VerificationBadge(status: String) {
    val (label, bg, fg) = when (status) {
        BlockedCallEntity.VERIF_PASSED -> Triple(
            stringResource(R.string.call_detail_verif_passed),
            Color(0xFF166534).copy(alpha = 0.15f), Color(0xFF16A34A))
        BlockedCallEntity.VERIF_FAILED -> Triple(
            stringResource(R.string.call_detail_verif_failed),
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f), MaterialTheme.colorScheme.error)
        else -> Triple(
            stringResource(R.string.call_detail_verif_not_verified),
            MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Surface(shape = RoundedCornerShape(4.dp), color = bg) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = fg,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

@Composable
private fun AttemptBadge(text: String, containerColor: Color, contentColor: Color) {
    Surface(shape = RoundedCornerShape(4.dp), color = containerColor) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = contentColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

@Composable
private fun WhitelistAddDialog(formattedNumber: String, onConfirm: (String, String) -> Unit, onDismiss: () -> Unit) {
    var name  by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.PlaylistAdd, null) },
        title = { Text(stringResource(R.string.call_detail_whitelist_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.call_detail_whitelist_subtitle, formattedNumber),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text(stringResource(R.string.call_detail_whitelist_name_label)) },
                    placeholder = { Text(stringResource(R.string.call_detail_whitelist_name_hint)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.call_detail_whitelist_notes_label)) },
                    placeholder = { Text(stringResource(R.string.call_detail_whitelist_notes_hint)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { onConfirm(name.trim(), notes.trim()) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.call_detail_whitelist_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } }
    )
}
