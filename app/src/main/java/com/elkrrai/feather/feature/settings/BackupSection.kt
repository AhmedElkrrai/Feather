package com.elkrrai.feather.feature.settings

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.R
import com.elkrrai.feather.data.backup.AutoBackup
import com.elkrrai.feather.domain.backup.ImportMode
import com.elkrrai.feather.domain.settings.AppSettings
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** What the backup rows of the settings screen can ask for; the stateful screen supplies them. */
class BackupActions(
    val onExport: () -> Unit = {},
    val onImport: () -> Unit = {},
    val onExportCsv: () -> Unit = {},
    val onAutoBackupChange: (Boolean) -> Unit = {},
    val onPickFolder: () -> Unit = {},
    val onBackupNow: () -> Unit = {},
)

/** The rows of the "Backup and data" section. */
@Composable
internal fun BackupRows(actions: BackupActions, settings: AppSettings) {
    val locale = LocalConfiguration.current.locales[0]
    SectionTitle(stringResource(R.string.settings_section_backup))
    SettingsRow(
        title = stringResource(R.string.backup_export),
        summary = null,
        supporting = stringResource(R.string.backup_export_summary),
        onClick = actions.onExport,
    )
    SettingsRow(
        title = stringResource(R.string.backup_import),
        summary = null,
        supporting = stringResource(R.string.backup_import_summary),
        onClick = actions.onImport,
    )
    SettingsRow(
        title = stringResource(R.string.backup_csv),
        summary = null,
        supporting = stringResource(R.string.backup_csv_summary),
        onClick = actions.onExportCsv,
    )
    SettingsRow(
        title = stringResource(R.string.backup_auto),
        summary = null,
        supporting = stringResource(R.string.backup_auto_summary, AutoBackup.KEEP),
        onClick = { actions.onAutoBackupChange(!settings.autoBackupEnabled) },
        trailing = {
            Switch(checked = settings.autoBackupEnabled, onCheckedChange = actions.onAutoBackupChange)
        },
    )
    SettingsRow(
        title = stringResource(R.string.backup_folder),
        summary = settings.backupFolder?.let { folderName(it) } ?: stringResource(R.string.backup_folder_none),
        supporting = null,
        onClick = actions.onPickFolder,
    )
    SettingsRow(
        title = stringResource(R.string.backup_now),
        summary = settings.lastBackupAt?.let { stringResource(R.string.backup_last, formatBackupTime(it, locale)) }
            ?: stringResource(R.string.backup_never),
        supporting = null,
        enabled = settings.backupFolder != null,
        onClick = actions.onBackupNow,
    )
}

/** A short name for a folder chosen with the system picker, such as "Backups". */
private fun folderName(uri: String): String =
    Uri.parse(uri).lastPathSegment?.substringAfterLast(':')?.substringAfterLast('/')?.ifEmpty { null } ?: uri

private fun formatBackupTime(epochMillis: Long, locale: Locale): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(locale)
        .format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

/** Asks how an imported backup is combined with what is on the phone; "replace" needs a warning. */
@Composable
internal fun ImportModeDialog(
    onConfirm: (ImportMode) -> Unit,
    onDismiss: () -> Unit,
) {
    var mode by rememberSaveable { mutableStateOf(ImportMode.MERGE) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_import_title)) },
        text = {
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.backup_import_message),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                ModeRow(
                    selected = mode == ImportMode.MERGE,
                    title = stringResource(R.string.backup_import_merge),
                    detail = stringResource(R.string.backup_import_merge_detail),
                    onSelect = { mode = ImportMode.MERGE },
                )
                ModeRow(
                    selected = mode == ImportMode.REPLACE,
                    title = stringResource(R.string.backup_import_replace),
                    detail = stringResource(R.string.backup_import_replace_detail),
                    detailIsWarning = true,
                    onSelect = { mode = ImportMode.REPLACE },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(mode) }) { Text(stringResource(R.string.action_import)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun ModeRow(
    selected: Boolean,
    title: String,
    detail: String,
    onSelect: () -> Unit,
    detailIsWarning: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = if (detailIsWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The message for a finished backup action, in the app language. */
internal fun backupMessage(context: Context, result: BackupResult): String = when (result) {
    BackupResult.Exported -> context.getString(R.string.backup_done_export)
    BackupResult.CsvExported -> context.getString(R.string.backup_done_csv)
    is BackupResult.Imported -> context.resources.getQuantityString(
        R.plurals.backup_done_import,
        result.summary.tasks,
        result.summary.tasks,
    )
    BackupResult.NotABackup -> context.getString(R.string.backup_error_not_backup)
    BackupResult.TooNew -> context.getString(R.string.backup_error_too_new)
    BackupResult.AutoBackedUp -> context.getString(R.string.backup_done_auto)
    BackupResult.NoFolder -> context.getString(R.string.backup_error_no_folder)
    BackupResult.NoAccess -> context.getString(R.string.backup_error_no_access)
    BackupResult.Failed -> context.getString(R.string.backup_error_failed)
}
