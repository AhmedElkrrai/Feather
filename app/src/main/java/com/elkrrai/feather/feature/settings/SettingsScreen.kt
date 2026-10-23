package com.elkrrai.feather.feature.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.settings.AppSettings
import com.elkrrai.feather.reminders.startActivitySafely
import com.elkrrai.feather.domain.settings.SNOOZE_MINUTE_OPTIONS
import com.elkrrai.feather.domain.settings.ThemeMode
import com.elkrrai.feather.domain.settings.TimeFormat
import com.elkrrai.feather.domain.settings.WeekStart
import com.elkrrai.feather.ui.components.TimeOfDayDialog
import com.elkrrai.feather.ui.components.rememberReminderHealth
import com.elkrrai.feather.ui.format.formatDueTime
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenReliability: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenTags: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
    backupViewModel: BackupViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val health by rememberReminderHealth()
    val backupState by backupViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // The file the user picked to import, kept as text so it survives rotation while the dialog is open.
    var pendingImport by rememberSaveable { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> if (uri != null) backupViewModel.exportTo(uri) }
    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri -> if (uri != null) backupViewModel.exportCsvTo(uri) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) pendingImport = uri.toString() }

    // Turning automatic backup on without a folder opens the folder picker first.
    var enableAfterFolder by rememberSaveable { mutableStateOf(false) }
    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            // Keep the permission across restarts: the daily job writes long after this screen is gone.
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            viewModel.setBackupFolder(uri.toString())
            if (enableAfterFolder) viewModel.setAutoBackupEnabled(true)
        }
        enableAfterFolder = false
    }

    // A finished export gets a small dialog with a picture; everything else stays a toast.
    var savedMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(backupState.result) {
        backupState.result?.let { result ->
            val message = backupMessage(context, result)
            if (result == BackupResult.Exported || result == BackupResult.CsvExported) {
                savedMessage = message
            } else {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
            backupViewModel.resultShown()
        }
    }
    savedMessage?.let { message -> BackupSavedDialog(detail = message, onDismiss = { savedMessage = null }) }

    SettingsContent(
        settings = settings,
        remindersHealthy = health.canDeliverOnTime && health.batteryUnrestricted,
        onBack = onBack,
        onOpenReliability = onOpenReliability,
        onOpenTags = onOpenTags,
        onDefaultTimeChange = viewModel::setDefaultReminderTime,
        onSnoozeMinutesChange = viewModel::setSnoozeMinutes,
        onDailySummaryChange = viewModel::setDailySummaryEnabled,
        onDailySummaryTimeChange = viewModel::setDailySummaryTime,
        backup = BackupActions(
            onExport = { exportLauncher.launch("feather-backup-${LocalDate.now()}.json") },
            onImport = { importLauncher.launch(arrayOf("*/*")) },
            onExportCsv = { csvLauncher.launch("feather-tasks-${LocalDate.now()}.csv") },
            onAutoBackupChange = { enable ->
                if (enable && settings.backupFolder == null) {
                    enableAfterFolder = true
                    folderLauncher.launch(null)
                } else {
                    viewModel.setAutoBackupEnabled(enable)
                }
            },
            onPickFolder = { folderLauncher.launch(null) },
            onBackupNow = backupViewModel::backupNow,
        ),
        backupBusy = backupState.busy,
        appearance = AppearanceActions(
            onThemeChange = viewModel::setThemeMode,
            onPureBlackChange = viewModel::setPureBlack,
            onDynamicColorChange = viewModel::setDynamicColor,
            onTimeFormatChange = viewModel::setTimeFormat,
            onWeekStartChange = viewModel::setWeekStart,
            onHapticsChange = viewModel::setHaptics,
            onBirdAnimationsChange = viewModel::setBirdAnimations,
            onReduceMotionChange = viewModel::setReduceMotion,
            onCelebrationsChange = viewModel::setCelebrations,
            onMorningCardChange = viewModel::setMorningCard,
            onEveningCardChange = viewModel::setEveningCard,
            onWeeklyFeatherChange = viewModel::setWeeklyFeather,
        ),
        versionName = remember { versionNameOf(context) },
        modifier = modifier,
    )

    pendingImport?.let { picked ->
        ImportModeDialog(
            onConfirm = { mode ->
                backupViewModel.importFrom(Uri.parse(picked), mode)
                pendingImport = null
            },
            onDismiss = { pendingImport = null },
        )
    }
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
    backup: BackupActions = BackupActions(),
    backupBusy: Boolean = false,
    appearance: AppearanceActions = AppearanceActions(),
    versionName: String = "",
    onOpenTags: () -> Unit = {},
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
            if (backupBusy) LinearProgressIndicator(Modifier.fillMaxWidth())

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

            AppearanceRows(
                settings = settings,
                actions = appearance,
                onPickTheme = { dialog = SettingsDialog.Theme },
                onPickTimeFormat = { dialog = SettingsDialog.TimeFormat },
                onPickWeekStart = { dialog = SettingsDialog.WeekStart },
            )

            BackupRows(backup, settings)

            SectionTitle(stringResource(R.string.settings_section_general))

            SettingsRow(
                title = stringResource(R.string.tags_title),
                summary = null,
                supporting = stringResource(R.string.tags_row_summary),
                onClick = onOpenTags,
            )

            SettingsRow(
                title = stringResource(R.string.settings_language),
                summary = stringResource(AppLanguage.current().labelRes),
                supporting = null,
                onClick = { dialog = SettingsDialog.Language },
            )

            ListItem(
                leadingContent = {
                    Image(
                        painter = painterResource(R.drawable.ic_bird_mark),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                    )
                },
                headlineContent = { Text(stringResource(R.string.settings_about)) },
                supportingContent = {
                    Column {
                        if (versionName.isNotBlank()) {
                            Text(stringResource(R.string.settings_version, versionName))
                        }
                        Text(
                            text = stringResource(R.string.settings_about_summary),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
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

        SettingsDialog.Theme -> ChoiceDialog(
            title = stringResource(R.string.settings_theme),
            options = ThemeMode.entries.map { it to stringResource(it.labelRes()) },
            selected = settings.themeMode,
            onSelect = {
                appearance.onThemeChange(it)
                dialog = null
            },
            onDismiss = { dialog = null },
        )

        SettingsDialog.TimeFormat -> ChoiceDialog(
            title = stringResource(R.string.settings_time_format),
            options = TimeFormat.entries.map { it to stringResource(it.labelRes()) },
            selected = settings.timeFormat,
            onSelect = {
                appearance.onTimeFormatChange(it)
                dialog = null
            },
            onDismiss = { dialog = null },
        )

        SettingsDialog.WeekStart -> ChoiceDialog(
            title = stringResource(R.string.settings_week_start),
            options = WeekStart.entries.map { it to it.label() },
            selected = settings.weekStart,
            onSelect = {
                appearance.onWeekStartChange(it)
                dialog = null
            },
            onDismiss = { dialog = null },
        )
    }
}

@Suppress("DEPRECATION")
private fun versionNameOf(context: Context): String = try {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
} catch (e: PackageManager.NameNotFoundException) {
    ""
}

/** Which settings dialog is open. An enum so it survives rotation with `rememberSaveable`. */
private enum class SettingsDialog { DefaultTime, SnoozeLength, SummaryTime, Language, Theme, TimeFormat, WeekStart }

@Composable
internal fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
            .semantics { heading() },
    )
}

@Composable
internal fun SettingsRow(
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
