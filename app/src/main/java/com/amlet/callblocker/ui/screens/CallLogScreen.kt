package com.amlet.callblocker.ui.screens

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amlet.callblocker.R
import com.amlet.callblocker.data.db.BlockedCallEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallLogScreen(
    blockedCalls: List<BlockedCallEntity>,
    onClearLog: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = { Icon(Icons.Rounded.DeleteForever, contentDescription = null) },
            title = { Text(stringResource(R.string.call_log_clear)) },
            text = { Text(stringResource(R.string.call_log_clear_confirm, blockedCalls.size)) },
            confirmButton = {
                Button(
                    onClick = { onClearLog(); showConfirmDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.call_log_clear)) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.call_log_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBack, stringResource(R.string.common_back))
                    }
                },
                actions = {
                    if (blockedCalls.isNotEmpty()) {
                        IconButton(onClick = { showConfirmDialog = true }) {
                            Icon(Icons.Rounded.DeleteForever, stringResource(R.string.call_log_clear),
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (blockedCalls.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Rounded.History, null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.call_log_empty),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                val todayStr = stringResource(R.string.call_log_today)
                val yesterdayStr = stringResource(R.string.call_log_yesterday)
                val dateFmt = stringResource(R.string.date_format_full)
                val blockedLabel = stringResource(R.string.call_log_blocked_label)
                val timeFmt = stringResource(R.string.date_format_time)

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val grouped = blockedCalls.groupBy { call ->
                        formatDate(call.blockedAt, todayStr, yesterdayStr, dateFmt)
                    }
                    grouped.forEach { (date, calls) ->
                        item {
                            Text(text = date,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp))
                        }
                        items(calls, key = { it.id }) { call ->
                            BlockedCallCard(call, blockedLabel, timeFmt)
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun BlockedCallCard(call: BlockedCallEntity, blockedLabel: String, timeFmt: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.PhoneDisabled, null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = call.phoneNumber,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(text = SimpleDateFormat(timeFmt, Locale.getDefault()).format(Date(call.blockedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)) {
                Text(text = blockedLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
    }
}

private fun formatDate(timestampMs: Long, today: String, yesterday: String, fmt: String): String {
    val todayCal = Calendar.getInstance()
    val callDay = Calendar.getInstance().apply { timeInMillis = timestampMs }
    return when {
        isSameDay(todayCal, callDay) -> today
        isYesterday(todayCal, callDay) -> yesterday
        else -> SimpleDateFormat(fmt, Locale.getDefault())
            .format(Date(timestampMs)).replaceFirstChar { it.uppercase() }
    }
}

private fun isSameDay(a: Calendar, b: Calendar) =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

private fun isYesterday(today: Calendar, other: Calendar): Boolean {
    val yesterday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    return isSameDay(yesterday, other)
}