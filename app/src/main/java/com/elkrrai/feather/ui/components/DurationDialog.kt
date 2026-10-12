package com.elkrrai.feather.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.R

/** Units offered when typing a length of time. */
private enum class DurationUnit(@StringRes val labelRes: Int, val minutes: Long) {
    MINUTES(R.string.unit_minutes, 1),
    HOURS(R.string.unit_hours, 60),
    DAYS(R.string.unit_days, 24 * 60),
}

/** The longest duration the dialog accepts, in days; keeps the arithmetic far from overflow. */
private const val MAX_DAYS = 365L

/**
 * Asks for an amount and a unit (minutes, hours or days) and reports the total in minutes.
 * Used for custom snooze lengths and custom "before due" reminder offsets.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DurationDialog(
    title: String,
    confirmLabel: String,
    onConfirm: (minutes: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var amountText by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf(DurationUnit.MINUTES) }
    val amount = amountText.toLongOrNull() ?: 0L
    val minutes = amount * unit.minutes
    val valid = amount > 0 && minutes <= MAX_DAYS * DurationUnit.DAYS.minutes

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
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
                    DurationUnit.entries.forEach { option ->
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
            TextButton(onClick = { onConfirm(minutes) }, enabled = valid) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
