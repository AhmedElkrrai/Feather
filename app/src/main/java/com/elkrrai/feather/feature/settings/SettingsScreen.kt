package com.elkrrai.feather.feature.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
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
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.fragment.app.FragmentActivity
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
import com.elkrrai.feather.feature.habits.HabitsViewModel
import com.elkrrai.feather.security.BiometricGate
import com.elkrrai.feather.ui.firstDayOfWeek
import com.elkrrai.feather.domain.settings.AppSettings
import com.elkrrai.feather.domain.settings.BackupStyle
import com.elkrrai.feather.reminders.startActivitySafely
import com.elkrrai.feather.domain.settings.SNOOZE_MINUTE_OPTIONS
import com.elkrrai.feather.domain.settings.ThemeMode
import com.elkrrai.feather.domain.settings.TimeFormat
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
    val activity = LocalActivity.current as? FragmentActivity
    // Only a debug build offers the sample data.
    val debuggable = remember { context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0 }
    val habitsViewModel: HabitsViewModel = hiltViewModel()
    val sampleLocale = LocalConfiguration.current.locales[0]

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
            onBackupStyleChange = viewModel::setBackupStyle,
            onBackupNow = backupViewModel::backupNow,
        ),
        backupBusy = backupState.busy,
        onAddTreeStages = if (debuggable) {
            { habitsViewModel.addTreeStages(LocalDate.now(), firstDayOfWeek(sampleLocale)) }
        } else {
            null
        },
        appLockAvailable = remember { BiometricGate.canAuthenticate(context) },
        onAppLockChange = { enable ->
            // Switching the lock on or off asks first: it proves the person holding the phone is the owner.
            val host = activity
            if (host == null) {
                viewModel.setAppLock(enable)
            } else {
                BiometricGate.authenticate(
                    activity = host,
                    title = context.getString(R.string.lock_prompt_title),
                    subtitle = context.getString(if (enable) R.string.lock_prompt_turn_on else R.string.lock_prompt_turn_off),
                    onSuccess = { viewModel.setAppLock(enable) },
                )
            }
        },
        appearance = AppearanceActions(
            onThemeChange = viewModel::setThemeMode,
            onPureBlackChange = viewModel::setPureBlack,
            onDynamicColorChange = viewModel::setDynamicColor,
            onTimeFormatChange = viewModel::setTimeFormat,
            onHapticsChange = viewModel::setHaptics,
            onMenuHintsChange = viewModel::setMenuHints,
            onBirdAnimationsChange = viewModel::setBirdAnimations,
            onReduceMotionChange = viewModel::setReduceMotion,
            onCelebrationsChange = viewModel::setCelebrations,
            onMorningCardChange = viewModel::setMorningCard,
            onEveningCardChange = viewModel::setEveningCard,
            onWeeklyFeatherChange = viewModel::setWeeklyFeather,
            onHabitsInTodayChange = viewModel::setHabitsInToday,
        ),
        versionName = remember { versionNameOf(context) },
        modifier = modifier,
    )

    pendingImport?.let { picked ->
        ImportModeDialog(
            onConfirm = { mode, restoreSettings ->
                backupViewModel.importFrom(Uri.parse(picked), mode, restoreSettings)
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
    /** Whether the phone has a fingerprint, a face or a screen lock the app lock could use. */
    appLockAvailable: Boolean = true,
    onAppLockChange: (Boolean) -> Unit = {},
    /** Debug builds: makes a habit for every picture of the tree. Null in a release build. */
    onAddTreeStages: (() -> Unit)? = null,
    versionName: String = "",
    onOpenTags: () -> Unit = {},
) {
    val locale = LocalConfiguration.current.locales[0]
    val context = LocalContext.current
    val resources = context.resources
    var dialog by rememberSaveable { mutableStateOf<SettingsDialog?>(null) }

    var page by rememberSaveable { mutableStateOf(SettingsPage.Home) }
    // Back from a category goes to the list first; from the list it leaves Settings.
    BackHandler(enabled = page != SettingsPage.Home) { page = SettingsPage.Home }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(page.titleRes)) },
                navigationIcon = {
                    IconButton(onClick = { if (page == SettingsPage.Home) onBack() else page = SettingsPage.Home }) {
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
        AnimatedContent(
            targetState = page,
            modifier = Modifier.padding(padding),
            transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(100)) },
            label = "settings page",
        ) { shown ->
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (backupBusy) LinearProgressIndicator(Modifier.fillMaxWidth())

                when (shown) {
                    SettingsPage.Home -> SettingsHome(
                        settings = settings,
                        remindersHealthy = remindersHealthy,
                        appLockAvailable = appLockAvailable,
                        versionName = versionName,
                        onOpen = { page = it },
                        onOpenReliability = onOpenReliability,
                        onOpenTags = onOpenTags,
                    )

                    SettingsPage.Reminders -> {
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
                    }

                    SettingsPage.Appearance -> AppearanceRows(
                        settings = settings,
                        actions = appearance,
                        onPickTheme = { dialog = SettingsDialog.Theme },
                    )

                    SettingsPage.Today -> TodayScreenRows(settings, appearance)

                    SettingsPage.Backup -> BackupRows(backup, settings, onPickStyle = { dialog = SettingsDialog.BackupStyle })

                    SettingsPage.Privacy -> SettingsRow(
                        title = stringResource(R.string.settings_app_lock),
                        summary = null,
                        supporting = stringResource(
                            if (appLockAvailable) R.string.settings_app_lock_summary else R.string.settings_app_lock_unavailable,
                        ),
                        enabled = appLockAvailable,
                        onClick = { onAppLockChange(!settings.appLock) },
                        trailing = {
                            Switch(
                                checked = settings.appLock && appLockAvailable,
                                onCheckedChange = onAppLockChange,
                                enabled = appLockAvailable,
                            )
                        },
                    )

                    SettingsPage.LanguageTime -> {
                        SettingsRow(
                            title = stringResource(R.string.settings_language),
                            summary = stringResource(AppLanguage.current().labelRes),
                            supporting = null,
                            onClick = { dialog = SettingsDialog.Language },
                        )
                        SettingsRow(
                            title = stringResource(R.string.settings_time_format),
                            summary = stringResource(settings.timeFormat.labelRes()),
                            supporting = null,
                            onClick = { dialog = SettingsDialog.TimeFormat },
                        )
                    }

                    SettingsPage.About -> {
                        ListItem(
                            leadingContent = {
                                Image(
                                    painter = painterResource(R.drawable.settings_chick),
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
                                    Text(
                                        text = stringResource(R.string.settings_open_source, SOURCE_CODE_LINK),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.clickable {
                                            context.startActivitySafely(Intent(Intent.ACTION_VIEW, Uri.parse("https://$SOURCE_CODE_LINK")))
                                        },
                                    )
                                }
                            },
                        )
                        if (onAddTreeStages != null) {
                            SettingsRow(
                                title = stringResource(R.string.debug_add_tree_stages),
                                summary = null,
                                supporting = stringResource(R.string.debug_add_tree_stages_summary),
                                onClick = onAddTreeStages,
                            )
                        }
                    }
                }
            }
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

        SettingsDialog.BackupStyle -> ChoiceDialog(
            title = stringResource(R.string.backup_style),
            options = BackupStyle.entries.map { it to stringResource(it.labelRes()) },
            selected = settings.backupStyle,
            onSelect = {
                backup.onBackupStyleChange(it)
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
    }
}

@Suppress("DEPRECATION")
private fun versionNameOf(context: Context): String = try {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
} catch (e: PackageManager.NameNotFoundException) {
    ""
}

/** The pages of Settings: the list of categories, and one page for each. An enum so the open page survives rotation. */
private enum class SettingsPage(@StringRes val titleRes: Int) {
    Home(R.string.settings),
    Reminders(R.string.settings_section_reminders),
    Appearance(R.string.settings_section_appearance),
    Today(R.string.settings_section_today),
    Backup(R.string.settings_section_backup),
    Privacy(R.string.settings_section_privacy),
    LanguageTime(R.string.settings_section_language_time),
    About(R.string.settings_about),
}

/** The first page of Settings: a short list of categories, each with a line on how it is set now. */
@Composable
private fun SettingsHome(
    settings: AppSettings,
    remindersHealthy: Boolean,
    appLockAvailable: Boolean,
    versionName: String,
    onOpen: (SettingsPage) -> Unit,
    onOpenReliability: () -> Unit,
    onOpenTags: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val resources = LocalContext.current.resources
    fun pair(first: String, second: String) = resources.getString(R.string.settings_summary_pair, first, second)

    if (!remindersHealthy) {
        // the one thing worth saying out loud: reminders that may not arrive
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clickable(role = Role.Button, onClick = onOpenReliability),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.settings_home_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.settings_home_warning_fix),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }

    SettingsGroup {
        CategoryRow(
            title = stringResource(SettingsPage.Reminders.titleRes),
            summary = stringResource(
                R.string.settings_home_reminders_summary,
                formatDueTime(settings.defaultReminderTime, locale),
                resources.getQuantityString(R.plurals.duration_minutes, settings.snoozeMinutes, settings.snoozeMinutes),
            ),
            onClick = { onOpen(SettingsPage.Reminders) },
        )
        val theme = stringResource(settings.themeMode.labelRes())
        CategoryRow(
            title = stringResource(SettingsPage.Appearance.titleRes),
            summary = if (settings.pureBlack) pair(theme, stringResource(R.string.settings_pure_black)) else theme,
            onClick = { onOpen(SettingsPage.Appearance) },
        )
        CategoryRow(
            title = stringResource(SettingsPage.Today.titleRes),
            summary = stringResource(R.string.settings_home_today_summary),
            onClick = { onOpen(SettingsPage.Today) },
        )
    }
    SettingsGroup {
        CategoryRow(
            title = stringResource(R.string.tags_title),
            summary = stringResource(R.string.tags_row_summary),
            onClick = onOpenTags,
        )
        CategoryRow(
            title = stringResource(SettingsPage.Backup.titleRes),
            summary = settings.lastBackupAt?.let { stringResource(R.string.backup_last, formatBackupTime(it, locale)) }
                ?: stringResource(R.string.backup_never),
            onClick = { onOpen(SettingsPage.Backup) },
        )
        CategoryRow(
            title = stringResource(SettingsPage.Privacy.titleRes),
            summary = stringResource(
                if (settings.appLock && appLockAvailable) R.string.settings_home_lock_on else R.string.settings_home_lock_off,
            ),
            onClick = { onOpen(SettingsPage.Privacy) },
        )
        CategoryRow(
            title = stringResource(SettingsPage.LanguageTime.titleRes),
            summary = pair(stringResource(AppLanguage.current().labelRes), stringResource(settings.timeFormat.labelRes())),
            onClick = { onOpen(SettingsPage.LanguageTime) },
        )
        CategoryRow(
            title = stringResource(SettingsPage.About.titleRes),
            summary = if (versionName.isNotBlank()) stringResource(R.string.settings_version, versionName) else null,
            onClick = { onOpen(SettingsPage.About) },
        )
    }
}

/** A rounded block that holds a few category rows. */
@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(content = content)
    }
}

/** One category in the list: its name, how it is set now, and a chevron that points the way the page opens. */
@Composable
private fun CategoryRow(title: String, summary: String?, onClick: () -> Unit) {
    val ltr = LocalLayoutDirection.current == LayoutDirection.Ltr
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(title) },
        supportingContent = if (summary != null) {
            { Text(summary, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            null
        },
        trailingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_down),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.graphicsLayer { rotationZ = if (ltr) -90f else 90f },
            )
        },
    )
}

/** Which settings dialog is open. An enum so it survives rotation with `rememberSaveable`. */
private enum class SettingsDialog { DefaultTime, SnoozeLength, SummaryTime, Language, Theme, TimeFormat, BackupStyle }

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

/** Where the code of the app is, shown in About without the scheme. */
private const val SOURCE_CODE_LINK = "github.com/AhmedElkrrai/Feather"
