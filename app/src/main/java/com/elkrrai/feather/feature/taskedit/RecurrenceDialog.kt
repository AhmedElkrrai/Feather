package com.elkrrai.feather.feature.taskedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.recurrence.Frequency
import com.elkrrai.feather.domain.recurrence.NthWeekday
import com.elkrrai.feather.domain.recurrence.Recurrence
import com.elkrrai.feather.domain.recurrence.RepeatMode
import com.elkrrai.feather.ui.format.LAST_DAY_OF_MONTH
import com.elkrrai.feather.ui.format.chipLabel
import com.elkrrai.feather.ui.format.formatDueDate
import com.elkrrai.feather.ui.format.label
import com.elkrrai.feather.ui.format.monthAndDay
import com.elkrrai.feather.ui.format.orderInWeek
import com.elkrrai.feather.ui.format.unitPlural
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle

private enum class MonthPattern { DAY_OF_MONTH, NTH_WEEKDAY, LAST_DAY }

private enum class EndKind { NEVER, DATE, COUNT }

private const val MAX_INTERVAL_DIGITS = 3
private const val DEFAULT_TIMES = 5

/**
 * Chooses how a task repeats. [reference] is the task's due date (or today); it supplies the
 * weekday, day of the month, or month and day that a new rule starts from. An existing rule
 * keeps its own choices until the user changes them, so pressing OK unchanged is a no-op.
 * Confirms with null for "does not repeat".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecurrenceDialog(
    initial: Recurrence?,
    reference: LocalDate,
    onConfirm: (Recurrence?) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]

    // What the rule already says wins over the reference date, so reopening does not change it.
    val anchorDay = initial?.takeIf { it.frequency == Frequency.MONTHLY }
        ?.monthDay?.takeIf { it != LAST_DAY_OF_MONTH } ?: reference.dayOfMonth
    val anchorNth = initial?.nthWeekday ?: NthWeekday.of(reference)
    val anchorMonth = initial?.takeIf { it.frequency == Frequency.YEARLY }?.month ?: reference.monthValue
    val anchorYearDay = initial?.takeIf { it.frequency == Frequency.YEARLY }?.monthDay ?: reference.dayOfMonth

    var frequency by remember { mutableStateOf(initial?.frequency) }
    var interval by remember { mutableStateOf((initial?.interval ?: 1).toString()) }
    var weekdays by remember {
        mutableStateOf(initial?.weekdays?.takeIf { it.isNotEmpty() } ?: setOf(reference.dayOfWeek))
    }
    var monthPattern by remember {
        mutableStateOf(
            when {
                initial?.nthWeekday != null -> MonthPattern.NTH_WEEKDAY
                initial?.monthDay == LAST_DAY_OF_MONTH -> MonthPattern.LAST_DAY
                else -> MonthPattern.DAY_OF_MONTH
            }
        )
    }
    var afterCompletion by remember { mutableStateOf(initial?.mode == RepeatMode.AFTER_COMPLETION) }
    var endKind by remember {
        mutableStateOf(
            when {
                initial?.until != null -> EndKind.DATE
                initial?.count != null -> EndKind.COUNT
                else -> EndKind.NEVER
            }
        )
    }
    var until by remember { mutableStateOf(initial?.until ?: reference.plusMonths(1)) }
    var times by remember { mutableStateOf((initial?.count ?: DEFAULT_TIMES).toString()) }
    var pickingUntil by remember { mutableStateOf(false) }

    fun build(): Recurrence? {
        val chosen = frequency ?: return null
        val every = interval.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val rule = when (chosen) {
            Frequency.DAILY -> Recurrence(chosen, every)
            Frequency.WEEKLY -> Recurrence(
                chosen,
                every,
                weekdays = weekdays.ifEmpty { setOf(reference.dayOfWeek) },
            )
            Frequency.MONTHLY -> when (monthPattern) {
                MonthPattern.DAY_OF_MONTH -> Recurrence(chosen, every, monthDay = anchorDay)
                MonthPattern.NTH_WEEKDAY -> Recurrence(chosen, every, nthWeekday = anchorNth)
                MonthPattern.LAST_DAY -> Recurrence(chosen, every, monthDay = LAST_DAY_OF_MONTH)
            }
            Frequency.YEARLY -> Recurrence(chosen, every, month = anchorMonth, monthDay = anchorYearDay)
        }
        return rule.copy(
            until = if (endKind == EndKind.DATE) until else null,
            count = if (endKind == EndKind.COUNT) times.toIntOrNull()?.coerceAtLeast(1) ?: 1 else null,
            mode = if (afterCompletion) RepeatMode.AFTER_COMPLETION else RepeatMode.FIXED,
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.repeat_dialog_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = frequency == null,
                        onClick = { frequency = null },
                        label = { Text(stringResource(R.string.repeat_none)) },
                    )
                    Frequency.entries.forEach { option ->
                        FilterChip(
                            selected = frequency == option,
                            onClick = { frequency = option },
                            label = { Text(stringResource(option.chipLabel())) },
                        )
                    }
                }

                val chosen = frequency
                if (chosen != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(stringResource(R.string.repeat_every))
                        OutlinedTextField(
                            value = interval,
                            onValueChange = { interval = it.filter(Char::isDigit).take(MAX_INTERVAL_DIGITS) },
                            modifier = Modifier.width(88.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        val amount = interval.toIntOrNull()?.coerceAtLeast(1) ?: 1
                        Text(context.resources.getQuantityString(chosen.unitPlural(), amount))
                    }

                    if (!afterCompletion) {
                        when (chosen) {
                            Frequency.WEEKLY -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = stringResource(R.string.repeat_on_days),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    DayOfWeek.entries.sortedBy { orderInWeek(it, locale) }.forEach { day ->
                                        FilterChip(
                                            selected = day in weekdays,
                                            onClick = {
                                                weekdays = if (day in weekdays) weekdays - day else weekdays + day
                                            },
                                            label = { Text(day.getDisplayName(TextStyle.SHORT, locale)) },
                                        )
                                    }
                                }
                            }

                            Frequency.MONTHLY -> Column(Modifier.selectableGroup()) {
                                RadioRow(
                                    selected = monthPattern == MonthPattern.DAY_OF_MONTH,
                                    label = stringResource(R.string.repeat_monthly_on_day, anchorDay),
                                    onSelect = { monthPattern = MonthPattern.DAY_OF_MONTH },
                                )
                                RadioRow(
                                    selected = monthPattern == MonthPattern.NTH_WEEKDAY,
                                    label = anchorNth.label(context, locale),
                                    onSelect = { monthPattern = MonthPattern.NTH_WEEKDAY },
                                )
                                RadioRow(
                                    selected = monthPattern == MonthPattern.LAST_DAY,
                                    label = stringResource(R.string.repeat_monthly_last_day),
                                    onSelect = { monthPattern = MonthPattern.LAST_DAY },
                                )
                            }

                            Frequency.YEARLY -> Text(
                                text = stringResource(
                                    R.string.repeat_yearly_on,
                                    monthAndDay(locale, anchorMonth, anchorYearDay),
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Frequency.DAILY -> Unit
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.repeat_after_completion),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = stringResource(R.string.repeat_after_completion_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = afterCompletion, onCheckedChange = { afterCompletion = it })
                    }

                    Column(Modifier.selectableGroup()) {
                        Text(
                            text = stringResource(R.string.repeat_ends),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        RadioRow(
                            selected = endKind == EndKind.NEVER,
                            label = stringResource(R.string.repeat_end_never),
                            onSelect = { endKind = EndKind.NEVER },
                        )
                        RadioRow(
                            selected = endKind == EndKind.DATE,
                            label = stringResource(R.string.repeat_end_on_date),
                            onSelect = { endKind = EndKind.DATE },
                            trailing = {
                                if (endKind == EndKind.DATE) {
                                    OutlinedButton(onClick = { pickingUntil = true }) {
                                        Text(formatDueDate(context, until, LocalDate.now(), locale))
                                    }
                                }
                            },
                        )
                        RadioRow(
                            selected = endKind == EndKind.COUNT,
                            label = stringResource(R.string.repeat_end_after_count),
                            onSelect = { endKind = EndKind.COUNT },
                            trailing = {
                                if (endKind == EndKind.COUNT) {
                                    OutlinedTextField(
                                        value = times,
                                        onValueChange = { times = it.filter(Char::isDigit).take(MAX_INTERVAL_DIGITS) },
                                        modifier = Modifier.width(88.dp),
                                        singleLine = true,
                                        label = { Text(stringResource(R.string.repeat_times_label)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    )
                                }
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(build()) }) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )

    if (pickingUntil) {
        DueDatePickerDialog(
            initial = until,
            onConfirm = {
                until = it
                pickingUntil = false
            },
            onDismiss = { pickingUntil = false },
        )
    }
}

@Composable
private fun RadioRow(
    selected: Boolean,
    label: String,
    onSelect: () -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(text = label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        trailing()
    }
}
