package com.elkrrai.feather.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.settings.AppSettings
import com.elkrrai.feather.domain.settings.SNOOZE_MINUTE_OPTIONS
import com.elkrrai.feather.ui.components.TimeOfDayDialog
import com.elkrrai.feather.ui.components.rememberReminderHealth
import com.elkrrai.feather.ui.format.formatDueTime
import java.time.LocalTime

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenReliability: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val health by rememberReminderHealth()

    SettingsContent(
        settings = settings,
        remindersHealthy = health.canDeliverOnTime && health.batteryUnrestricted,
        onBack = onBack,
        onOpenReliability = onOpenReliability,
        onDefaultTimeChange = viewModel::setDefaultReminderTime,
        onSnoozeMinutesChange = viewModel::setSnoozeMinutes,
        onDailySummaryChange = viewModel::setDailySummaryEnabled,
        onDailySummaryTimeChange = viewModel::setDailySummaryTime,
        modifier = modifier,
    )
}

/** Stateless settings screen, so it can be previewed and tested. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    settings: AppSettings,
    remindersHealthy: Boolean,
    onBack: () -> Unit,
    onOpenReliability: () -> Unit,
    onDefaultTimeChange: (LocalTime) -> Unit,
    onSnoozeMinutesChange: (Int) -> Unit,
    onDailySummaryChange: (Boolean) -> Unit,
    onDailySummaryTimeChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val resources = LocalContext.current.resources
    var dialog by rememberSaveable { mutableStateOf<SettingsDialog?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        // The drawable is autoMirrored, so the arrow flips in RTL languages.
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionTitle(stringResource(R.string.settings_section_reminders))

            SettingsRow(
                title = stringResource(R.string.settings_default_time),
                summary = formatDueTime(settings.defaultReminderTime, locale),
                supporting = stringResource(R.string.settings_default_time_summary),
                onClick = { dialog = SettingsDialog.DefaultTime },
            )
            SettingsRow(
                title = stringResource(R.string.settings_snooze_length),
                summary = resources.getQuantityString(
                    R.plurals.duration_minutes,
                    settings.snoozeMinutes,
                    settings.snoozeMinutes,
                ),
                supporting = stringResource(R.string.settings_snooze_summary),
                onClick = { dialog = SettingsDialog.SnoozeLength },
            )
            SettingsRow(
                title = stringResource(R.string.settings_daily_summary),
                summary = null,
                supporting = stringResource(R.string.settings_daily_summary_summary),
                onClick = { onDailySummaryChange(!settings.dailySummaryEnabled) },
                trailing = {
                    Switch(
                        checked = settings.dailySummaryEnabled,
                        onCheckedChange = onDailySummaryChange,
                    )
                },
            )
            SettingsRow(
                title = stringResource(R.string.settings_daily_summary_time),
                summary = formatDueTime(settings.dailySummaryTime, locale),
                supporting = null,
                enabled = settings.dailySummaryEnabled,
                onClick = { dialog = SettingsDialog.SummaryTime },
            )
            SettingsRow(
                title = stringResource(R.string.settings_reliability),
                summary = stringResource(
                    if (remindersHealthy) R.string.settings_reliability_ok else R.string.settings_reliability_attention
                ),
                supporting = null,
                onClick = onOpenReliability,
            )

            SectionTitle(stringResource(R.string.settings_section_general))

            SettingsRow(
                title = stringResource(R.string.settings_language),
                summary = stringResource(AppLanguage.current().labelRes),
                supporting = null,
                onClick = { dialog = SettingsDialog.Language },
            )
        }
    }

    when (dialog) {
        null -> Unit

        SettingsDialog.DefaultTime -> TimeOfDayDialog(
            initial = settings.defaultReminderTime,
            onConfirm = {
                onDefaultTimeChange(it)
                dialog = null
            },
            onDismiss = { dialog = null },
        )

        SettingsDialog.SummaryTime -> TimeOfDayDialog(
            initial = settings.dailySummaryTime,
            onConfirm = {
                onDailySummaryTimeChange(it)
                dialog = null
            },
            onDismiss = { dialog = null },
        )

        SettingsDialog.SnoozeLength -> SnoozeLengthDialog(
            selected = settings.snoozeMinutes,
            onSelect = {
                onSnoozeMinutesChange(it)
                dialog = null
            },
            onDismiss = { dialog = null },
        )

        SettingsDialog.Language -> LanguageDialog(onDismiss = { dialog = null })
    }
}

/** Which settings dialog is open. An enum so it survives rotation with `rememberSaveable`. */
private enum class SettingsDialog { DefaultTime, SnoozeLength, SummaryTime, Language }

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingsRow(
    title: String,
    summary: String?,
    supporting: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    ListItem(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.38f)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        headlineContent = { Text(title) },
        supportingContent = if (summary != null || supporting != null) {
            {
                Column {
                    if (summary != null) {
                        Text(summary, color = MaterialTheme.colorScheme.primary)
                    }
                    if (supporting != null) {
                        Text(supporting, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            null
        },
        trailingContent = trailing,
    )
}

@Composable
private fun SnoozeLengthDialog(
    selected: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val resources = LocalContext.current.resources
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_snooze_length)) },
        text = {
            Column(Modifier.selectableGroup()) {
                SNOOZE_MINUTE_OPTIONS.forEach { minutes ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = minutes == selected,
                                onClick = { onSelect(minutes) },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = minutes == selected, onClick = null)
                        Spacer(Modifier.width(16.dp))
                        Text(resources.getQuantityString(R.plurals.duration_minutes, minutes, minutes))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
