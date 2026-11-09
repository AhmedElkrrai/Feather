package com.elkrrai.feather.feature.habits

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.habit.HabitLog
import com.elkrrai.feather.domain.habit.MAX_HABIT_TARGET
import com.elkrrai.feather.ui.theme.Space
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * What was logged on a day for a habit that counts an amount, as a list: tap a log to change or delete it, or add another. The day's total
 * is the sum of its logs, so changing 10 to 9 is one tap on the log and one on the minus. [usual] are the amounts the habit is most often
 * logged in (commonest first), offered as quick choices for a new log.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HabitLogDialog(
    title: String,
    weekday: String,
    isToday: Boolean,
    dayTotal: Int,
    weekTotal: Int,
    weekTarget: Int,
    logs: List<HabitLog>,
    usual: List<Int>,
    onAdd: (Int) -> Unit,
    onChange: (logId: String, amount: Int) -> Unit,
    onDelete: (logId: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var text by rememberSaveable { mutableStateOf("1") }
    val edited = logs.firstOrNull { it.id == editing }
    val value = text.toIntOrNull() ?: 0
    val locale = LocalConfiguration.current.locales[0]
    val timeFormat = remember(locale) { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale) }
    val subtitle = if (isToday) stringResource(R.string.habit_log_subtitle_today, weekday) else weekday
    val back = { editing = null; adding = false }

    when {
        edited != null -> AlertDialog(
            onDismissRequest = back,
            title = { Text(stringResource(R.string.habit_log_change_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Space.x2)) {
                    Text(
                        text = "$title, " + (edited.loggedAt?.let { timeFormat.format(it.atZone(ZoneId.systemDefault())) }
                            ?: stringResource(R.string.habit_log_earlier)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    AmountStepper(text) { text = it }
                    Text(
                        text = stringResource(R.string.habit_log_type_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(
                        onClick = {
                            onDelete(edited.id)
                            back()
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text(stringResource(R.string.habit_log_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // nothing typed is no amount: the log goes, as it does with the delete button
                        if (value >= 1) onChange(edited.id, value) else onDelete(edited.id)
                        back()
                    },
                ) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = { TextButton(onClick = back) { Text(stringResource(R.string.action_cancel)) } },
        )

        adding -> AlertDialog(
            onDismissRequest = back,
            title = { Text(stringResource(R.string.habit_log_add_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Space.x2)) {
                    Text(
                        text = "$title, " + stringResource(R.string.habit_log_now),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (usual.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.habit_log_usual),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.x2)) {
                            usual.sorted().forEach { amount ->
                                AssistChip(onClick = { text = amount.toString() }, label = { Text(amount.toString()) })
                            }
                        }
                    }
                    AmountStepper(text) { text = it }
                    Text(
                        text = stringResource(
                            if (isToday) R.string.habit_log_would_be_today else R.string.habit_log_would_be_day,
                            dayTotal + value,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAdd(value)
                        back()
                    },
                    enabled = value >= 1,
                ) { Text(stringResource(R.string.action_add)) }
            },
            dismissButton = { TextButton(onClick = back) { Text(stringResource(R.string.action_cancel)) } },
        )

        else -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Space.x2)) {
                    Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.x2), verticalArrangement = Arrangement.spacedBy(Space.x2)) {
                        InfoPill(
                            if (isToday) stringResource(R.string.habit_log_today_total, dayTotal)
                            else stringResource(R.string.habit_log_day_total, dayTotal, weekday),
                        )
                        InfoPill(stringResource(R.string.habit_log_week_total, weekTotal, weekTarget))
                    }
                    logs.forEach { log ->
                        LogRow(
                            amount = log.amount,
                            time = log.loggedAt?.let { timeFormat.format(it.atZone(ZoneId.systemDefault())) }
                                ?: stringResource(R.string.habit_log_earlier),
                        ) {
                            text = log.amount.toString()
                            editing = log.id
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            // a new log starts from the amount the habit is most often logged in
                            text = (usual.firstOrNull() ?: logs.lastOrNull()?.amount ?: 1).toString()
                            adding = true
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Icon(painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.padding(end = Space.x2))
                        Text(stringResource(R.string.habit_log_add))
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.habit_log_done)) } },
        )
    }
}

@Composable
private fun InfoPill(text: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = Space.x2, vertical = Space.x1),
        )
    }
}

/** One log: its amount, the time it was made, and a pencil. The whole row changes it. */
@Composable
private fun LogRow(amount: Int, time: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            Modifier.heightIn(min = 52.dp).padding(horizontal = Space.x3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.x3),
        ) {
            Text(text = amount.toString(), style = MaterialTheme.typography.titleLarge, modifier = Modifier.width(48.dp))
            Text(
                text = time,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(R.drawable.ic_edit),
                contentDescription = stringResource(R.string.habit_log_change_title),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** The number to set, between a minus and a plus; tapping the number types it. */
@Composable
private fun AmountStepper(text: String, onText: (String) -> Unit) {
    val value = text.toIntOrNull() ?: 0
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        FilledTonalIconButton(onClick = { onText((value - 1).coerceAtLeast(1).toString()) }) {
            Icon(painterResource(R.drawable.ic_remove), contentDescription = stringResource(R.string.habit_log_less))
        }
        OutlinedTextField(
            value = text,
            onValueChange = { onText(it.filter(Char::isDigit).trimStart('0').take(4)) },
            textStyle = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(112.dp).padding(horizontal = Space.x2),
        )
        FilledTonalIconButton(onClick = { onText((value + 1).coerceAtMost(MAX_HABIT_TARGET).toString()) }) {
            Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.habit_log_more))
        }
    }
}
