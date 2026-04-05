package com.amlet.callblocker.ui.screens

import androidx.compose.foundation.clickable
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
import com.amlet.callblocker.util.PhoneUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * Groups all call attempts by phone number, showing each unique number once
 * with a count badge and the most recent attempt time.
 */
private data class BlockedNumberSummary(
    val phoneNumber: String,
    val latestAttempt: Long,
    val totalAttempts: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallLogScreen(
    blockedCalls: List<BlockedCallEntity>,
    onClearLog: () -> Unit,
    onNavigateBack: () -> Unit,
    onOpenDetail: (String) -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    // Group attempts by number, keep summary info for each.
    val summaries: List<BlockedNumberSummary> = remember(blockedCalls) {
        blockedCalls
            .groupBy { it.phoneNumber }
            .map { (number, calls) ->
                val sorted = calls.sortedByDescending { it.blockedAt }
                BlockedNumberSummary(
                    phoneNumber = number,
                    latestAttempt = sorted.first().blockedAt,
                    totalAttempts = calls.size
                )
            }
            .sortedByDescending { it.latestAttempt }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = { Icon(Icons.Rounded.DeleteForever, contentDescription = null) },
            title = { Text(stringResource(R.string.call_log_clear)) },
            text = { Text(stringResource(R.string.call_log_clear_confirm, summaries.size)) },
            confirmButton = {
                Button(
                    onClick = { onClearLog(); showConfirmDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
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
                title = {
                    Text(
                        stringResource(R.string.call_log_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBack, stringResource(R.string.common_back))
                    }
                },
                actions = {
                    if (summaries.isNotEmpty()) {
                        IconButton(onClick = { showConfirmDialog = true }) {
                            Icon(
                                Icons.Rounded.DeleteForever,
                                stringResource(R.string.call_log_clear),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (summaries.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Rounded.History, null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        stringResource(R.string.call_log_empty),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val todayStr     = stringResource(R.string.call_log_today)
                val yesterdayStr = stringResource(R.string.call_log_yesterday)
                val dateFmt      = stringResource(R.string.date_format_full)
                val timeFmt      = stringResource(R.string.date_format_time)

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val grouped = summaries.groupBy { s ->
                        formatDate(s.latestAttempt, todayStr, yesterdayStr, dateFmt)
                    }
                    grouped.forEach { (date, group) ->
                        item {
                            Text(
                                text = date,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(
                                    top = 8.dp, bottom = 4.dp, start = 4.dp
                                )
                            )
                        }
                        items(group, key = { it.phoneNumber }) { summary ->
                            BlockedNumberCard(
                                summary = summary,
                                timeFmt = timeFmt,
                                onClick = { onOpenDetail(summary.phoneNumber) }
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun BlockedNumberCard(
    summary: BlockedNumberSummary,
    timeFmt: String,
    onClick: () -> Unit
) {
    val formatted = remember(summary.phoneNumber) {
        PhoneUtils.formatForDisplay(summary.phoneNumber)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Blocked icon
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.PhoneDisabled, null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatted,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = SimpleDateFormat(timeFmt, Locale.getDefault())
                            .format(Date(summary.latestAttempt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                }
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Attempt count badge
                if (summary.totalAttempts > 1) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = stringResource(R.string.call_log_attempts, summary.totalAttempts),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                    ) {
                        Text(
                            text = stringResource(R.string.call_log_blocked_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun formatDate(
    timestampMs: Long,
    today: String,
    yesterday: String,
    fmt: String
): String {
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