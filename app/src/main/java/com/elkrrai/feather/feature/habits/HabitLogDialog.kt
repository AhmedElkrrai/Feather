package com.elkrrai.feather.feature.habits

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
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
                DialogButton(stringResource(R.string.action_save)) {
                    // nothing typed is no amount: the log goes, as it does with the delete button
                    if (value >= 1) onChange(edited.id, value) else onDelete(edited.id)
                    back()
                }
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
                Text(stringResource(R.string.habit_log_delete), style = SmallText)
            }
        }

        adding -> LogDialogFrame(
            title = stringResource(R.string.habit_log_add_title),
            onDismiss = back,
            actions = {
                MutedButton(stringResource(R.string.action_cancel), back)
                DialogButton(stringResource(R.string.action_add), enabled = value >= 1) {
                    onAdd(value)
                    back()
                }
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
            actions = { DialogButton(stringResource(R.string.habit_log_done), onClick = onDismiss) },
        ) {
            // the subtitle sits close under the title, then the pills, the logs and the add box
            Hint(
                if (isToday) stringResource(R.string.habit_log_subtitle_today, weekday) else weekday,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                InfoPill(
                    if (isToday) stringResource(R.string.habit_log_today_total, dayTotal)
                    else stringResource(R.string.habit_log_day_total, dayTotal, weekday),
                    boldNumber = dayTotal,
                    modifier = Modifier.weight(0.37f).fillMaxHeight(),
                )
                InfoPill(
                    stringResource(R.string.habit_log_week_total, weekTotal, weekTarget),
                    boldNumber = weekTotal,
                    modifier = Modifier.weight(0.63f).fillMaxHeight(),
                )
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

// The sizes of the dialog's text. A small sans, quiet: the title, the subtitle, the pills and the times are all the same few sizes.
private val TitleText: TextStyle @Composable get() = MaterialTheme.typography.titleLarge.copy(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)
private val SubText: TextStyle @Composable get() = MaterialTheme.typography.bodyMedium.copy(fontSize = 23.sp, lineHeight = 30.sp)
private val SmallText: TextStyle @Composable get() = MaterialTheme.typography.bodyMedium.copy(fontSize = 20.sp, lineHeight = 26.sp)

/** The color of a row or a button circle in the dialog: a little darker and bluer than the dialog itself (the mockup's 23, 37, 48 on 31, 46, 54). */
@Composable
private fun insetColor(): Color = AlertDialogDefaults.containerColor.let { Color(it.red * 0.74f, it.green * 0.80f, it.blue * 0.89f) }

/** The color of a pill: a step darker than a row (the mockup's 20, 32, 42). */
@Composable
private fun pillColor(): Color = AlertDialogDefaults.containerColor.let { Color(it.red * 0.65f, it.green * 0.70f, it.blue * 0.78f) }

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
            shape = RoundedCornerShape(32.dp),
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation,
            modifier = Modifier.padding(horizontal = 24.dp).widthIn(max = 560.dp).fillMaxWidth(),
        ) {
            Column(Modifier.padding(start = 28.dp, end = 28.dp, top = 26.dp, bottom = 18.dp)) {
                Text(text = title, style = TitleText, modifier = Modifier.padding(bottom = 4.dp))
                Column(
                    modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content,
                )
                Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.End, content = actions)
            }
        }
    }
}

@Composable
private fun Hint(text: String, centered: Boolean = false, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = if (centered) SmallText else SubText,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = if (centered) TextAlign.Center else null,
        modifier = if (centered) modifier.fillMaxWidth() else modifier,
    )
}

@Composable
private fun DialogButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled) {
        Text(label, style = SubText.copy(fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun MutedButton(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
        Text(label, style = SubText.copy(fontWeight = FontWeight.Bold))
    }
}

/** A pill of information: the number in bold, the rest quiet. */
@Composable
private fun InfoPill(text: String, boldNumber: Int, modifier: Modifier = Modifier) {
    val number = boldNumber.toString()
    val at = text.indexOf(number)
    val styled: AnnotatedString = if (at < 0) {
        AnnotatedString(text)
    } else {
        buildAnnotatedString {
            append(text.substring(0, at))
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(number) }
            append(text.substring(at + number.length))
        }
    }
    Surface(shape = RoundedCornerShape(13.dp), color = pillColor(), modifier = modifier) {
        Text(
            text = styled,
            style = SubText,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
        )
    }
}

/** One log: its amount, the time it was made, and a pencil. The whole row changes it. */
@Composable
private fun LogRow(amount: Int, time: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(19.dp),
        color = insetColor(),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            Modifier.heightIn(min = 76.dp).padding(start = 19.dp, end = 23.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = amount.toString(),
                style = SubText.copy(fontSize = 33.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.widthIn(min = 44.dp),
            )
            Text(
                text = time,
                style = SubText,
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

/** A button with a faint dashed outline: the way to add another log. */
@Composable
private fun DashedButton(label: String, onClick: () -> Unit) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 70.dp)
            .drawBehind {
                drawRoundRect(
                    color = color,
                    cornerRadius = CornerRadius(19.dp.toPx()),
                    style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
                )
            }
            .clickable(onClick = onClick)
            .padding(horizontal = Space.x3),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("+  $label", style = SubText, color = MaterialTheme.colorScheme.primary)
    }
}

/** A round amount to pick; the one that is set is ringed. */
@Composable
private fun QuickChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = insetColor(),
        border = BorderStroke(1.dp, if (selected) scheme.primary else scheme.outlineVariant),
        modifier = Modifier.heightIn(min = 32.dp).clickable(onClick = onClick),
    ) {
        Box(Modifier.padding(horizontal = Space.x3, vertical = Space.x1), contentAlignment = Alignment.Center) {
            Text(label, style = SmallText.copy(fontWeight = FontWeight.Bold), color = if (selected) scheme.primary else scheme.onSurface)
        }
    }
}

/** The number to set, between a minus and a plus; tapping the number types it. */
@Composable
private fun AmountStepper(text: String, onText: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val value = text.toIntOrNull() ?: 0
    val buttons = IconButtonDefaults.filledIconButtonColors(containerColor = insetColor(), contentColor = scheme.primary)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Space.x1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        FilledIconButton(onClick = { onText((value - 1).coerceAtLeast(1).toString()) }, colors = buttons, modifier = Modifier.size(52.dp)) {
            Icon(painterResource(R.drawable.ic_remove), contentDescription = stringResource(R.string.habit_log_less))
        }
        BasicTextField(
            value = text,
            onValueChange = { onText(it.filter(Char::isDigit).trimStart('0').take(4)) },
            textStyle = SubText.copy(
                fontSize = 56.sp,
                lineHeight = 64.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = scheme.onSurface,
            ),
            cursorBrush = SolidColor(scheme.primary),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(144.dp).padding(horizontal = Space.x2),
        )
        FilledIconButton(
            onClick = { onText((value + 1).coerceAtMost(MAX_HABIT_TARGET).toString()) },
            colors = buttons,
            modifier = Modifier.size(52.dp),
        ) {
            Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.habit_log_more))
        }
    }
}
