package com.elkrrai.feather.feature.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.elkrrai.feather.R
import com.elkrrai.feather.reminders.ReminderHealth
import com.elkrrai.feather.reminders.SettingsIntents
import com.elkrrai.feather.reminders.startActivitySafely
import com.elkrrai.feather.ui.components.rememberReminderHealth
import kotlinx.coroutines.launch

/** What the user can do from the reliability helper. */
class ReliabilityActions(
    val onBack: () -> Unit,
    val onFixNotifications: () -> Unit,
    val onFixChannel: () -> Unit,
    val onFixExactAlarms: () -> Unit,
    val onFixBattery: () -> Unit,
    val onOpenAppSettings: () -> Unit,
    val onOpenVendorGuide: () -> Unit,
    val onSendTest: () -> Unit,
)

@Composable
fun ReliabilityScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReliabilityViewModel = hiltViewModel(),
) {
    val health by rememberReminderHealth()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val testScheduled = stringResource(R.string.reliability_test_scheduled)

    ReliabilityContent(
        health = health,
        manufacturer = Build.MANUFACTURER,
        snackbarHostState = snackbarHostState,
        actions = ReliabilityActions(
            onBack = onBack,
            onFixNotifications = {
                context.startActivitySafely(SettingsIntents.notifications(context), SettingsIntents.appDetails(context))
            },
            onFixChannel = {
                context.startActivitySafely(SettingsIntents.reminderChannel(context), SettingsIntents.notifications(context))
            },
            onFixExactAlarms = {
                context.startActivitySafely(SettingsIntents.exactAlarms(context), SettingsIntents.appDetails(context))
            },
            onFixBattery = {
                context.startActivitySafely(SettingsIntents.batteryOptimization(), SettingsIntents.appDetails(context))
            },
            onOpenAppSettings = { context.startActivitySafely(SettingsIntents.appDetails(context)) },
            onOpenVendorGuide = { context.startActivitySafely(SettingsIntents.vendorGuide(Build.MANUFACTURER)) },
            onSendTest = {
                viewModel.sendTestReminder()
                scope.launch { snackbarHostState.showSnackbar(testScheduled) }
            },
        ),
        modifier = modifier,
    )
}

/** Stateless reliability helper, so it can be previewed and tested. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReliabilityContent(
    health: ReminderHealth,
    manufacturer: String,
    snackbarHostState: SnackbarHostState,
    actions: ReliabilityActions,
    modifier: Modifier = Modifier,
) {
    val allGood = health.canDeliverOnTime && health.batteryUnrestricted
    val needsExactAlarmCheck = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reliability_title)) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        // The drawable is autoMirrored, so the arrow flips in RTL languages.
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(
                    if (allGood) R.string.reliability_all_good else R.string.reliability_intro
                ),
                style = MaterialTheme.typography.bodyLarge,
            )

            CheckCard(
                title = stringResource(R.string.reliability_notifications_title),
                body = stringResource(R.string.reliability_notifications_body),
                ok = health.notificationsEnabled,
                onFix = actions.onFixNotifications,
            )
            if (health.notificationsEnabled) {
                CheckCard(
                    title = stringResource(R.string.reliability_channel_title),
                    body = stringResource(R.string.reliability_channel_body),
                    ok = health.reminderChannelEnabled,
                    onFix = actions.onFixChannel,
                )
            }
            if (needsExactAlarmCheck) {
                CheckCard(
                    title = stringResource(R.string.reliability_exact_title),
                    body = stringResource(R.string.reliability_exact_body),
                    ok = health.exactAlarmsAllowed,
                    onFix = actions.onFixExactAlarms,
                )
            }
            CheckCard(
                title = stringResource(R.string.reliability_battery_title),
                body = stringResource(R.string.reliability_battery_body),
                ok = health.batteryUnrestricted,
                onFix = actions.onFixBattery,
            )

            if (SettingsIntents.isRestrictiveVendor(manufacturer)) {
                VendorCard(
                    manufacturer = manufacturer,
                    onOpenAppSettings = actions.onOpenAppSettings,
                    onOpenGuide = actions.onOpenVendorGuide,
                )
            }

            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.reliability_test_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.reliability_test_body),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(onClick = actions.onSendTest) {
                        Text(stringResource(R.string.reliability_test_button))
                    }
                }
            }
        }
    }
}

/** One thing that can block reminders: what it is, whether it is fine, and a button to fix it. */
@Composable
private fun CheckCard(
    title: String,
    body: String,
    ok: Boolean,
    onFix: () -> Unit,
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier
                        .size(12.dp)
                        .background(if (ok) OkColor else AttentionColor, CircleShape)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(
                        if (ok) R.string.reliability_status_ok else R.string.reliability_status_attention
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (ok) OkColor else AttentionColor,
                )
            }
            Text(text = body, style = MaterialTheme.typography.bodyMedium)
            if (!ok) {
                OutlinedButton(onClick = onFix) { Text(stringResource(R.string.reliability_fix)) }
            }
        }
    }
}

/** The part no API can check: the phone maker's own auto-launch and background limits. */
@Composable
private fun VendorCard(
    manufacturer: String,
    onOpenAppSettings: () -> Unit,
    onOpenGuide: () -> Unit,
) {
    val name = manufacturer.replaceFirstChar { it.uppercase() }
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.reliability_vendor_title, name),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.reliability_vendor_body, name),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenAppSettings) {
                    Text(stringResource(R.string.reliability_open_settings))
                }
                OutlinedButton(onClick = onOpenGuide) {
                    Text(stringResource(R.string.reliability_open_guide))
                }
            }
        }
    }
}

private val OkColor = Color(0xFF2E7D32)
private val AttentionColor = Color(0xFFC62828)
