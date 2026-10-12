package com.elkrrai.feather.feature.snooze

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.R

private data class SnoozePreset(@StringRes val labelRes: Int, val minutes: Long)

private val presets = listOf(
    SnoozePreset(R.string.snooze_in_10_min, 10),
    SnoozePreset(R.string.snooze_in_30_min, 30),
    SnoozePreset(R.string.snooze_in_1_hour, 60),
    SnoozePreset(R.string.snooze_in_3_hours, 180),
)

private enum class SnoozeUnit(@StringRes val labelRes: Int, val minutes: Long) {
    MINUTES(R.string.unit_minutes, 1),
    HOURS(R.string.unit_hours, 60),
    DAYS(R.string.unit_days, 24 * 60),
}

/** The longest custom snooze, in days; keeps the arithmetic far from overflow. */
private const val MAX_CUSTOM_DAYS = 365L

/** Presets first, then a custom amount. */
@Composable
fun SnoozeDialog(
    onSnoozeMinutes: (Long) -> Unit,
    onTomorrowMorning: () -> Unit,
    onDismiss: () -> Unit,
) {
    var custom by rememberSaveable { mutableStateOf(false) }

    if (custom) {
        CustomSnoozeDialog(
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomSnoozeDialog(
    onConfirm: (minutes: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var amountText by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf(SnoozeUnit.MINUTES) }
    val amount = amountText.toLongOrNull() ?: 0L
    val minutes = amount * unit.minutes
    val valid = amount > 0 && minutes <= MAX_CUSTOM_DAYS * SnoozeUnit.DAYS.minutes

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.snooze_custom_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { value -> amountText = value.filter { it.isDigit() }.take(4) },
                    label = { Text(stringResource(R.string.snooze_amount_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SnoozeUnit.entries.forEach { option ->
                        FilterChip(
                            selected = unit == option,
                            onClick = { unit = option },
                            label = { Text(stringResource(option.labelRes)) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(minutes) }, enabled = valid) {
                Text(stringResource(R.string.action_snooze))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
