package com.elkrrai.feather.feature.habits

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.habit.DayTime
import com.elkrrai.feather.domain.habit.MAX_STAGES
import com.elkrrai.feather.domain.habit.TimeMode
import com.elkrrai.feather.ui.components.TimeOfDayDialog
import com.elkrrai.feather.ui.format.formatDueTime
import com.elkrrai.feather.ui.theme.Space
import java.time.LocalTime
import java.util.Locale

/** What the time pickers start from when a time is first turned on: noon. */
private const val NOON = 12 * 60

private fun minuteToTime(minute: Int): LocalTime = LocalTime.of((minute / 60).coerceIn(0, 23), (minute % 60).coerceIn(0, 59))

private fun format(minute: Int, locale: Locale): String = formatDueTime(minuteToTime(minute), locale)

/** A time of day in words: "No time set", one moment, or "4:30 to 6:00 AM". */
@Composable
internal fun describeTime(time: DayTime): String {
    val locale = LocalConfiguration.current.locales[0]
    val start = time.startMinute
    return when {
        !time.isSet || start == null -> stringResource(R.string.habit_time_unset)
        time.mode == TimeMode.WINDOW && time.endMinute != null ->
            stringResource(R.string.habit_time_range, format(start, locale), format(time.endMinute, locale))
        else -> format(start, locale)
    }
}

private enum class TimePart { START, END }

/** The time of a habit or of one of its stages: none, one moment, or a window, with the clock dialogs behind the buttons. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TimeSetting(time: DayTime, onChange: (DayTime) -> Unit, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    var picking by remember { mutableStateOf<TimePart?>(null) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = time.mode == TimeMode.NONE,
                onClick = { onChange(DayTime()) },
                label = { Text(stringResource(R.string.habit_time_none)) },
            )
            FilterChip(
                selected = time.mode == TimeMode.AT,
                onClick = { onChange(DayTime(TimeMode.AT, time.startMinute ?: NOON)) },
                label = { Text(stringResource(R.string.habit_time_at)) },
            )
            FilterChip(
                selected = time.mode == TimeMode.WINDOW,
                onClick = {
                    val start = time.startMinute ?: NOON
                    onChange(DayTime(TimeMode.WINDOW, start, time.endMinute?.takeIf { it > start } ?: (start + 60).coerceAtMost(LAST_MINUTE)))
                },
                label = { Text(stringResource(R.string.habit_time_window)) },
            )
        }
        val start = time.startMinute
        when {
            time.mode == TimeMode.AT && start != null -> OutlinedButton(onClick = { picking = TimePart.START }) { Text(format(start, locale)) }
            time.mode == TimeMode.WINDOW && start != null -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { picking = TimePart.START }) {
                    Text(stringResource(R.string.habit_time_from, format(start, locale)))
                }
                OutlinedButton(onClick = { picking = TimePart.END }) {
                    Text(stringResource(R.string.habit_time_to, format(time.endMinute ?: start, locale)))
                }
            }
        }
    }
    picking?.let { part ->
        val initial = if (part == TimePart.START) time.startMinute ?: NOON else time.endMinute ?: NOON
        TimeOfDayDialog(
            initial = minuteToTime(initial),
            onConfirm = { chosen ->
                val minute = chosen.hour * 60 + chosen.minute
                onChange(
                    when (part) {
                        // a window whose end is not after its new start gets an hour
                        TimePart.START -> time.copy(
                            startMinute = minute,
                            endMinute = if (time.mode == TimeMode.WINDOW && (time.endMinute ?: 0) <= minute) {
                                (minute + 60).coerceAtMost(LAST_MINUTE)
                            } else {
                                time.endMinute
                            },
                        )
                        TimePart.END -> time.copy(endMinute = minute)
                    },
                )
                picking = null
            },
            onDismiss = { picking = null },
        )
    }
}

private const val LAST_MINUTE = 24 * 60 - 1

/**
 * The named times of the day: one row for each, shut to its name and time and open to change them, with up, down and remove, and a
 * button to add another. A blank time that was just added opens by itself.
 */
@Composable
internal fun StageList(
    stages: List<StageUi>,
    onName: (key: Long, name: String) -> Unit,
    onTime: (key: Long, time: DayTime) -> Unit,
    onMove: (key: Long, by: Int) -> Unit,
    onRemove: (key: Long) -> Unit,
    onAdd: () -> Long?,
) {
    var open by remember { mutableStateOf<Long?>(null) }
    // a blank time (just added, or made by turning the names on) opens for typing
    LaunchedEffect(stages.size) { open = stages.lastOrNull { it.name.isBlank() }?.key ?: open }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        stages.forEachIndexed { index, stage ->
            StageRow(
                number = index + 1,
                stage = stage,
                isOpen = open == stage.key,
                canMoveUp = index > 0,
                canMoveDown = index < stages.lastIndex,
                canRemove = stages.size > 1,
                onToggle = { open = if (open == stage.key) null else stage.key },
                onName = { onName(stage.key, it) },
                onTime = { onTime(stage.key, it) },
                onMove = { onMove(stage.key, it) },
                onRemove = { onRemove(stage.key) },
            )
        }
        if (stages.size < MAX_STAGES) {
            TextButton(onClick = { onAdd()?.let { open = it } }) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.habit_stage_add), modifier = Modifier.padding(start = Space.x2))
            }
        }
    }
}

@Composable
private fun StageRow(
    number: Int,
    stage: StageUi,
    isOpen: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    canRemove: Boolean,
    onToggle: () -> Unit,
    onName: (String) -> Unit,
    onTime: (DayTime) -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = scheme.surfaceContainer,
        border = BorderStroke(1.dp, if (isOpen) scheme.primary else scheme.outlineVariant),
    ) {
        Column(Modifier.fillMaxWidth().padding(Space.x3), verticalArrangement = Arrangement.spacedBy(Space.x2)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.x3),
                modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
            ) {
                Box(
                    modifier = Modifier.size(24.dp).padding(0.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(shape = CircleShape, color = scheme.surfaceVariant, modifier = Modifier.size(24.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text(number.toString(), style = MaterialTheme.typography.labelMedium) }
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stage.name.ifBlank { stringResource(R.string.habit_stage_unnamed) },
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (stage.name.isBlank()) scheme.onSurfaceVariant else scheme.onSurface,
                    )
                    Text(
                        text = describeTime(stage.time),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
            if (isOpen) {
                OutlinedTextField(
                    value = stage.name,
                    onValueChange = onName,
                    label = { Text(stringResource(R.string.habit_stage_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                TimeSetting(stage.time, onTime)
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = { onMove(-1) }, enabled = canMoveUp) {
                        Icon(
                            painterResource(R.drawable.ic_chevron_down),
                            stringResource(R.string.habit_stage_move_up),
                            modifier = Modifier.graphicsLayer { rotationZ = 180f },
                        )
                    }
                    IconButton(onClick = { onMove(1) }, enabled = canMoveDown) {
                        Icon(painterResource(R.drawable.ic_chevron_down), stringResource(R.string.habit_stage_move_down))
                    }
                    IconButton(onClick = onRemove, enabled = canRemove) {
                        Icon(painterResource(R.drawable.ic_delete), stringResource(R.string.habit_stage_remove))
                    }
                }
            }
        }
    }
}
