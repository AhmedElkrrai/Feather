package com.elkrrai.feather.feature.habits

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    val back = { editing = null; adding = false }
    val earlier = stringResource(R.string.habit_log_earlier)
    fun timeOf(log: HabitLog) = log.loggedAt?.let { timeFormat.format(it.atZone(ZoneId.systemDefault())) } ?: earlier

    when {
        edited != null -> LogDialogFrame(
            title = stringResource(R.string.habit_log_change_title),
            onDismiss = back,
            actions = {
                MutedButton(stringResource(R.string.action_cancel), back)
                TextButton(
                    onClick = {
                        // nothing typed is no amount: the log goes, as it does with the delete button
                        if (value >= 1) onChange(edited.id, value) else onDelete(edited.id)
                        back()
                    },
                ) { Text(stringResource(R.string.action_save)) }
            },
        ) {
            Hint("$title, ${timeOf(edited)}")
            AmountStepper(text) { text = it }
            Hint(stringResource(R.string.habit_log_type_hint), centered = true)
            TextButton(
                onClick = {
                    onDelete(edited.id)
                    back()
                },
                modifier = Modifier.align(Alignment.CenterHorizontally),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Icon(painterResource(R.drawable.ic_delete), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Space.x2))
                Text(stringResource(R.string.habit_log_delete), style = MaterialTheme.typography.labelLarge)
            }
        }

        adding -> LogDialogFrame(
            title = stringResource(R.string.habit_log_add_title),
            onDismiss = back,
            actions = {
                MutedButton(stringResource(R.string.action_cancel), back)
                TextButton(
                    onClick = {
                        onAdd(value)
                        back()
                    },
                    enabled = value >= 1,
                ) { Text(stringResource(R.string.action_add)) }
            },
        ) {
            Hint("$title, ${stringResource(R.string.habit_log_now)}")
            if (usual.isNotEmpty()) {
                Hint(stringResource(R.string.habit_log_usual), centered = true)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.x2, Alignment.CenterHorizontally)) {
                    usual.sorted().forEach { amount ->
                        QuickChip(amount.toString(), selected = value == amount) { text = amount.toString() }
                    }
                }
            }
            AmountStepper(text) { text = it }
            Hint(
                stringResource(
                    if (isToday) R.string.habit_log_would_be_today else R.string.habit_log_would_be_day,
                    dayTotal + value,
                ),
                centered = true,
            )
        }

        else -> LogDialogFrame(
            title = title,
            onDismiss = onDismiss,
            actions = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.habit_log_done)) } },
        ) {
            Hint(if (isToday) stringResource(R.string.habit_log_subtitle_today, weekday) else weekday)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.x2), verticalArrangement = Arrangement.spacedBy(Space.x2)) {
                InfoPill(
                    if (isToday) stringResource(R.string.habit_log_today_total, dayTotal)
                    else stringResource(R.string.habit_log_day_total, dayTotal, weekday),
                )
                InfoPill(stringResource(R.string.habit_log_week_total, weekTotal, weekTarget))
            }
            logs.forEach { log ->
                LogRow(amount = log.amount, time = timeOf(log)) {
                    text = log.amount.toString()
                    editing = log.id
                }
            }
            DashedButton(stringResource(R.string.habit_log_add)) {
                // a new log starts from the amount the habit is most often logged in
                text = (usual.firstOrNull() ?: logs.lastOrNull()?.amount ?: 1).toString()
                adding = true
            }
        }
    }
}

/** A dialog with a title, content and a row of buttons at the end, tighter than the stock one. */
@Composable
private fun LogDialogFrame(
    title: String,
    onDismiss: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation,
            modifier = Modifier.padding(horizontal = 24.dp).widthIn(max = 560.dp).fillMaxWidth(),
        ) {
            Column(Modifier.padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(end = 8.dp, bottom = Space.x2),
                )
                Column(
                    modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Space.x2),
                    content = content,
                )
                Row(Modifier.fillMaxWidth().padding(top = Space.x3), horizontalArrangement = Arrangement.End, content = actions)
            }
        }
    }
}

@Composable
private fun Hint(text: String, centered: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = if (centered) TextAlign.Center else null,
        modifier = if (centered) Modifier.fillMaxWidth() else Modifier,
    )
}

@Composable
private fun MutedButton(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
        Text(label)
    }
}

@Composable
private fun InfoPill(text: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
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
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            Modifier.heightIn(min = 52.dp).padding(horizontal = Space.x3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.x3),
        ) {
            Text(text = amount.toString(), style = MaterialTheme.typography.titleLarge, modifier = Modifier.widthIn(min = 36.dp))
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
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** A button with a dashed outline: the way to add another log. */
@Composable
private fun DashedButton(label: String, onClick: () -> Unit) {
    val color = MaterialTheme.colorScheme.outline
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .drawBehind {
                drawRoundRect(
                    color = color,
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 5.dp.toPx()))),
                )
            }
            .clickable(onClick = onClick)
            .padding(horizontal = Space.x3),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_add), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(Space.x2))
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

/** A round amount to pick; the one that is set is ringed. */
@Composable
private fun QuickChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = scheme.surfaceContainerLow,
        border = BorderStroke(1.dp, if (selected) scheme.primary else scheme.outlineVariant),
        modifier = Modifier.heightIn(min = 32.dp).clickable(onClick = onClick),
    ) {
        Box(Modifier.padding(horizontal = Space.x3, vertical = Space.x1), contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) scheme.primary else scheme.onSurface)
        }
    }
}

/** The number to set, between a minus and a plus; tapping the number types it. */
@Composable
private fun AmountStepper(text: String, onText: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val value = text.toIntOrNull() ?: 0
    val buttons = IconButtonDefaults.filledIconButtonColors(containerColor = scheme.surfaceContainerLow, contentColor = scheme.primary)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Space.x1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        FilledIconButton(onClick = { onText((value - 1).coerceAtLeast(1).toString()) }, colors = buttons, modifier = Modifier.size(44.dp)) {
            Icon(painterResource(R.drawable.ic_remove), contentDescription = stringResource(R.string.habit_log_less))
        }
        BasicTextField(
            value = text,
            onValueChange = { onText(it.filter(Char::isDigit).trimStart('0').take(4)) },
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 34.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = scheme.onSurface,
            ),
            cursorBrush = SolidColor(scheme.primary),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(96.dp).padding(horizontal = Space.x2),
        )
        FilledIconButton(
            onClick = { onText((value + 1).coerceAtMost(MAX_HABIT_TARGET).toString()) },
            colors = buttons,
            modifier = Modifier.size(44.dp),
        ) {
            Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.habit_log_more))
        }
    }
}
