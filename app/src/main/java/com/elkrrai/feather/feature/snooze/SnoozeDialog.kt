package com.elkrrai.feather.feature.snooze

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.R
import com.elkrrai.feather.ui.components.DurationDialog

private data class SnoozePreset(@StringRes val labelRes: Int, val minutes: Long)

private val presets = listOf(
    SnoozePreset(R.string.snooze_in_10_min, 10),
    SnoozePreset(R.string.snooze_in_30_min, 30),
    SnoozePreset(R.string.snooze_in_1_hour, 60),
    SnoozePreset(R.string.snooze_in_3_hours, 180),
)

/** Presets first, then a custom amount. */
@Composable
fun SnoozeDialog(
    onSnoozeMinutes: (Long) -> Unit,
    onTomorrowMorning: () -> Unit,
    onDismiss: () -> Unit,
) {
    var custom by rememberSaveable { mutableStateOf(false) }

    if (custom) {
        DurationDialog(
            title = stringResource(R.string.snooze_custom_title),
            confirmLabel = stringResource(R.string.action_snooze),
            onConfirm = onSnoozeMinutes,
            onDismiss = onDismiss,
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.snooze_title)) },
        text = {
            Column {
                presets.forEach { preset ->
                    OptionRow(stringResource(preset.labelRes)) { onSnoozeMinutes(preset.minutes) }
                }
                OptionRow(stringResource(R.string.snooze_tomorrow_morning), onTomorrowMorning)
                OptionRow(stringResource(R.string.snooze_custom)) { custom = true }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun OptionRow(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    )
}
