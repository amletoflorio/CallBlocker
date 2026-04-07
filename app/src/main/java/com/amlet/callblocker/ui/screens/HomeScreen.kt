package com.amlet.callblocker.ui.screens

import android.content.Context
import android.telephony.SubscriptionManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amlet.callblocker.R
import com.amlet.callblocker.data.prefs.AppPreferences
import com.amlet.callblocker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    contactCount: Int,
    blockedCount: Int,
    isServiceEnabled: Boolean,
    suspendUntil: Long,
    onToggleService: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCallLog: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }

    val statusColor by animateColorAsState(
        targetValue = if (isServiceEnabled) Emerald500 else Slate400,
        animationSpec = tween(600),
        label = "statusColor"
    )

    val isDualSim = remember {
        try {
            val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            (sm.activeSubscriptionInfoList?.size ?: 1) > 1
        } catch (e: Exception) { false }
    }
    val protectedSim by remember { mutableStateOf(prefs.protectedSim) }

    var scheduleLabel by remember { mutableStateOf(prefs.scheduleActiveLabel) }
    var showScheduleOverrideDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isServiceEnabled) { scheduleLabel = prefs.scheduleActiveLabel }

    if (showScheduleOverrideDialog) {
        ScheduleOverrideDialog(
            onOverrideToday = {
                val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())
                prefs.scheduleOverriddenToday = true
                prefs.scheduleOverriddenDate  = today
                onToggleService()
                showScheduleOverrideDialog = false
            },
            onOverrideForever = {
                prefs.scheduleEnabled = false
                onToggleService()
                showScheduleOverrideDialog = false
            },
            onDismiss = { showScheduleOverrideDialog = false }
        )
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    val handleToggle: () -> Unit = {
        if (prefs.scheduleEnabled && scheduleLabel.isNotEmpty()) {
            showScheduleOverrideDialog = true
        } else {
            onToggleService()
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        if (isLandscape) {
            LandscapeHomeLayout(
                padding = padding,
                contactCount = contactCount,
                blockedCount = blockedCount,
                isServiceEnabled = isServiceEnabled,
                suspendUntil = suspendUntil,
                scheduleLabel = scheduleLabel,
                statusColor = statusColor,
                isDualSim = isDualSim,
                protectedSim = protectedSim,
                onToggleService = handleToggle,
                onNavigateToContacts = onNavigateToContacts,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToCallLog = onNavigateToCallLog
            )
        } else {
            PortraitHomeLayout(
                padding = padding,
                contactCount = contactCount,
                blockedCount = blockedCount,
                isServiceEnabled = isServiceEnabled,
                suspendUntil = suspendUntil,
                scheduleLabel = scheduleLabel,
                statusColor = statusColor,
                isDualSim = isDualSim,
                protectedSim = protectedSim,
                onToggleService = handleToggle,
                onNavigateToContacts = onNavigateToContacts,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToCallLog = onNavigateToCallLog
            )
        }
    }
}

// ── Portrait layout ───────────────────────────────────────────────────────────

@Composable
private fun PortraitHomeLayout(
    padding: PaddingValues,
    contactCount: Int,
    blockedCount: Int,
    isServiceEnabled: Boolean,
    suspendUntil: Long,
    scheduleLabel: String,
    statusColor: androidx.compose.ui.graphics.Color,
    isDualSim: Boolean,
    protectedSim: String,
    onToggleService: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCallLog: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        HomeHeader(onNavigateToSettings = onNavigateToSettings)
        Spacer(modifier = Modifier.height(40.dp))

        HomeToggleCircle(
            isServiceEnabled = isServiceEnabled,
            statusColor = statusColor,
            onToggleService = onToggleService
        )

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(
            visible = isDualSim && isServiceEnabled,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                SimProtectionBadge(protectedSim = protectedSim)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        AnimatedVisibility(
            visible = suspendUntil > 0L,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                SuspendedBanner(suspendUntil = suspendUntil)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        AnimatedVisibility(
            visible = scheduleLabel.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                ScheduleActiveBanner(label = scheduleLabel)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        StatsCard(contactCount = contactCount, blockedCount = blockedCount)
        Spacer(modifier = Modifier.height(20.dp))

        HomeButtons(
            onNavigateToContacts = onNavigateToContacts,
            onNavigateToCallLog = onNavigateToCallLog
        )

        Spacer(modifier = Modifier.height(8.dp))
        HomeDisclaimer()
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ── Landscape layout ──────────────────────────────────────────────────────────

@Composable
private fun LandscapeHomeLayout(
    padding: PaddingValues,
    contactCount: Int,
    blockedCount: Int,
    isServiceEnabled: Boolean,
    suspendUntil: Long,
    scheduleLabel: String,
    statusColor: androidx.compose.ui.graphics.Color,
    isDualSim: Boolean,
    protectedSim: String,
    onToggleService: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCallLog: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            HomeHeader(onNavigateToSettings = onNavigateToSettings)
            Spacer(modifier = Modifier.height(16.dp))
            HomeToggleCircle(isServiceEnabled = isServiceEnabled, statusColor = statusColor, onToggleService = onToggleService)
            if (isDualSim && isServiceEnabled) { Spacer(Modifier.height(8.dp)); SimProtectionBadge(protectedSim = protectedSim) }
            if (suspendUntil > 0L) { Spacer(Modifier.height(8.dp)); SuspendedBanner(suspendUntil = suspendUntil) }
            if (scheduleLabel.isNotEmpty()) { Spacer(Modifier.height(8.dp)); ScheduleActiveBanner(label = scheduleLabel) }
        }
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))
            StatsCard(contactCount = contactCount, blockedCount = blockedCount)
            HomeButtons(onNavigateToContacts = onNavigateToContacts, onNavigateToCallLog = onNavigateToCallLog)
            Spacer(Modifier.height(8.dp))
            HomeDisclaimer()
        }
    }
}

// ── Shared sub-composables ────────────────────────────────────────────────────

@Composable
private fun HomeHeader(onNavigateToSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            // "Call" in white, "Blocker" in emerald green
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "Call",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Blocker",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    color = Emerald500
                )
            }
            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onNavigateToSettings) {
            Icon(
                Icons.Rounded.Settings,
                contentDescription = stringResource(R.string.settings_title),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HomeToggleCircle(
    isServiceEnabled: Boolean,
    statusColor: androidx.compose.ui.graphics.Color,
    onToggleService: () -> Unit
) {
    // Pulsing ring animation when active
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isServiceEnabled) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = if (isServiceEnabled) 0.15f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Toggle press scale
    val pressScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "pressScale"
    )

    Box(contentAlignment = Alignment.Center) {
        // Outer pulsing ring
        if (isServiceEnabled) {
            Box(
                modifier = Modifier
                    .size(184.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(Emerald500.copy(alpha = pulseAlpha))
            )
        }
        // Main circle
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(160.dp)
                .scale(pressScale)
                .clip(CircleShape)
                .background(
                    if (isServiceEnabled)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
                .then(
                    if (isServiceEnabled)
                        Modifier.border(2.dp, Emerald500.copy(alpha = 0.6f), CircleShape)
                    else
                        Modifier
                )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Switch(
                    checked = isServiceEnabled,
                    onCheckedChange = { onToggleService() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.background,
                        checkedTrackColor = Emerald500,
                        uncheckedThumbColor = Slate400,
                        uncheckedTrackColor = Slate700
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isServiceEnabled)
                        stringResource(R.string.home_status_active)
                    else
                        stringResource(R.string.home_status_inactive),
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun SuspendedBanner(suspendUntil: Long) {
    val fmt = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }
    val untilLabel = remember(suspendUntil) {
        if (suspendUntil == Long.MAX_VALUE) "∞" else fmt.format(Date(suspendUntil))
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Rounded.PauseCircle, contentDescription = null,
                tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            Text(text = stringResource(R.string.home_suspended_until, untilLabel),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun ScheduleActiveBanner(label: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Rounded.Schedule, null,
                tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
            Text(text = label, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
private fun ScheduleOverrideDialog(
    onOverrideToday: () -> Unit,
    onOverrideForever: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Schedule, null) },
        title = { Text(stringResource(R.string.home_schedule_override_title)) },
        text = { Text(stringResource(R.string.home_schedule_override_body),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOverrideToday, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)) {
                    Text(stringResource(R.string.home_schedule_override_today))
                }
                OutlinedButton(onClick = onOverrideForever, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)) {
                    Text(stringResource(R.string.home_schedule_override_forever))
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        },
        dismissButton = {}
    )
}


@Composable
private fun StatsCard(contactCount: Int, blockedCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                icon = Icons.Rounded.Shield,
                label = stringResource(R.string.home_stat_contacts),
                value = "$contactCount"
            )
            HorizontalDivider(
                modifier = Modifier.height(48.dp).width(1.dp),
                color = MaterialTheme.colorScheme.outline
            )
            StatItem(
                icon = Icons.Rounded.Block,
                label = stringResource(R.string.home_stat_blocked),
                value = "$blockedCount"
            )
        }
    }
}

@Composable
private fun HomeButtons(
    onNavigateToContacts: () -> Unit,
    onNavigateToCallLog: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = onNavigateToContacts,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(Icons.Rounded.Contacts, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.home_btn_whitelist), style = MaterialTheme.typography.titleMedium)
        }
        OutlinedButton(
            onClick = onNavigateToCallLog,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Rounded.History, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.home_btn_call_log), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun HomeDisclaimer() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Info, null, tint = Amber500, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.home_info_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SimProtectionBadge(protectedSim: String) {
    val label = when (protectedSim) {
        AppPreferences.SIM_1 -> stringResource(R.string.home_sim_badge_sim1)
        AppPreferences.SIM_2 -> stringResource(R.string.home_sim_badge_sim2)
        else                 -> stringResource(R.string.home_sim_badge_both)
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Rounded.SimCard, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
