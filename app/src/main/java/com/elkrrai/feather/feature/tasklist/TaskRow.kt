package com.elkrrai.feather.feature.tasklist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.model.Task
import com.elkrrai.feather.ui.components.CompletionRing
import com.elkrrai.feather.ui.components.ListShapeIcon
import com.elkrrai.feather.ui.components.MetaChip
import com.elkrrai.feather.ui.components.StrikeText
import com.elkrrai.feather.ui.components.SubtaskLeaves
import com.elkrrai.feather.ui.components.barbCount
import com.elkrrai.feather.ui.format.chipLabel
import com.elkrrai.feather.ui.format.chipText
import com.elkrrai.feather.ui.format.dueLabelFor
import com.elkrrai.feather.ui.format.label
import com.elkrrai.feather.ui.format.ringColor
import com.elkrrai.feather.ui.rememberHaptics
import com.elkrrai.feather.ui.theme.Space
import com.elkrrai.feather.ui.theme.featherColors
import com.elkrrai.feather.ui.theme.listShapeFor
import java.time.LocalDate

/** The list a task belongs to, as the row shows it: its name and which of the eight list colors it has. */
data class ListLabel(val name: String, val colorIndex: Int?)

private const val MAX_TAG_CHIPS = 3

/**
 * One task, as a card: the completion ring, the title, and a row of small chips (date, list,
 * repeat, tags, subtask progress). [list] is shown only when the view mixes several lists (smart
 * views and tag views); pass null inside a single list.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskRow(
    task: Task,
    today: LocalDate,
    onToggle: (completed: Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    list: ListLabel? = null,
    /** When set, TalkBack offers "Delete" as an action (the swipe is not reachable with it). */
    onDelete: (() -> Unit)? = null,
    /** When set, TalkBack offers pin and unpin. */
    onPinChange: ((Boolean) -> Unit)? = null,
    /** Part of the current selection (highlighted and announced as selected). */
    selected: Boolean = false,
    /** A long press, used to start selecting; null leaves the row without one. */
    onLongClick: (() -> Unit)? = null,
    /** The grip shown at the end of the row while the list can be reordered. */
    dragHandle: (@Composable () -> Unit)? = null,
    /** TalkBack actions that stand in for dragging. */
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val feather = MaterialTheme.featherColors

    val due = task.dueDate?.let { dueLabelFor(it, task.dueTime, today, task.isCompleted) }
    val waiting = due?.isWaiting == true
    val priorityLabel = task.priority.label()
    val ringDescription = if (task.priority.barbCount() > 0) "${task.title}, $priorityLabel" else task.title
    val pinnedLabel = stringResource(R.string.pinned)
    val reminderLabel = stringResource(R.string.has_reminder)
    // A finished task no longer reminds anyone.
    val hasReminder = !task.isCompleted && task.reminders.isNotEmpty()
    val recurrence = if (task.isCompleted) null else task.recurrence

    val haptics = rememberHaptics()
    val openLabel = stringResource(R.string.action_open_task)
    val selectLabel = stringResource(R.string.action_select)
    val doneActionLabel = stringResource(
        if (task.isCompleted) R.string.action_mark_not_done else R.string.action_mark_done
    )
    val deleteLabel = stringResource(R.string.action_delete)
    val pinActionLabel = stringResource(if (task.isPinned) R.string.action_unpin else R.string.action_pin)
    val waitingLabel = stringResource(R.string.task_waiting)
    val moveUpLabel = stringResource(R.string.action_move_up)
    val moveDownLabel = stringResource(R.string.action_move_down)

    val subtaskTotal = task.subtasks.size
    val subtasksDone = task.subtasks.count { it.isCompleted }
    val hasChips = due != null || list != null || recurrence != null || task.tags.isNotEmpty() || subtaskTotal > 0

    Surface(
        modifier = modifier.padding(horizontal = Space.gutter, vertical = Space.x1),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) scheme.secondaryContainer else feather.card,
        border = BorderStroke(1.dp, scheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .combinedClickable(
                    onClickLabel = openLabel,
                    onLongClickLabel = if (onLongClick != null) selectLabel else null,
                    onLongClick = onLongClick,
                    onClick = onClick,
                )
                .semantics {
                    this.selected = selected
                    if (waiting) stateDescription = waitingLabel
                    customActions = buildList {
                        add(CustomAccessibilityAction(doneActionLabel) { onToggle(!task.isCompleted); true })
                        if (onDelete != null) add(CustomAccessibilityAction(deleteLabel) { onDelete(); true })
                        if (onPinChange != null) {
                            add(CustomAccessibilityAction(pinActionLabel) { onPinChange(!task.isPinned); true })
                        }
                        if (onMoveUp != null) add(CustomAccessibilityAction(moveUpLabel) { onMoveUp(); true })
                        if (onMoveDown != null) add(CustomAccessibilityAction(moveDownLabel) { onMoveDown(); true })
                    }
                }
                .padding(start = Space.x1, end = 14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // The ring is named after its task (and its priority), so it is not just "checkbox" in a list of them.
            CompletionRing(
                checked = task.isCompleted,
                onCheckedChange = { checked ->
                    haptics.tick()
                    onToggle(checked)
                },
                ringColor = task.priority.ringColor(),
                contentDescription = ringDescription,
                barbs = task.priority.barbCount(),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = Space.x3, bottom = Space.x3),
            ) {
                // The strike grows across the title; a finished title is dimmed to the secondary color.
                StrikeText(
                    text = task.title,
                    struck = task.isCompleted,
                    activeColor = scheme.onSurface,
                    doneColor = scheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (hasChips) {
                    Spacer(Modifier.height(Space.x2))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (due != null) {
                            val container = when {
                                due.isWaiting -> scheme.surfaceVariant
                                due.isToday -> scheme.secondaryContainer
                                else -> scheme.surfaceContainer
                            }
                            val content = if (due.isToday) scheme.onSecondaryContainer else scheme.onSurfaceVariant
                            MetaChip(text = due.chipText(), container = container, content = content)
                        }
                        if (list != null) {
                            val color = list.colorIndex?.let { feather.listColors[it % feather.listColors.size] }
                                ?: feather.prioNone
                            MetaChip(
                                text = list.name,
                                container = scheme.surfaceContainer,
                                content = scheme.onSurfaceVariant,
                                leading = { ListShapeIcon(listShapeFor(list.colorIndex), color) },
                            )
                        }
                        if (recurrence != null) {
                            val plain = recurrence.interval == 1 && recurrence.weekdays.isEmpty() &&
                                recurrence.until == null && recurrence.count == null &&
                                recurrence.monthDay == null && recurrence.nthWeekday == null
                            MetaChip(
                                text = if (plain) stringResource(recurrence.frequency.chipLabel()) else recurrence.label(task.dueDate),
                                container = scheme.surfaceContainer,
                                content = scheme.onSurfaceVariant,
                                leading = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_repeat),
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = scheme.onSurfaceVariant,
                                    )
                                },
                            )
                        }
                        for (tag in task.tags.take(MAX_TAG_CHIPS)) {
                            MetaChip(
                                text = "#${tag.name}",
                                container = scheme.primaryContainer,
                                content = scheme.onPrimaryContainer,
                            )
                        }
                        if (task.tags.size > MAX_TAG_CHIPS) {
                            MetaChip(
                                text = "+${task.tags.size - MAX_TAG_CHIPS}",
                                container = scheme.primaryContainer,
                                content = scheme.onPrimaryContainer,
                            )
                        }
                        if (subtaskTotal > 0) {
                            MetaChip(
                                text = stringResource(R.string.subtasks_of, subtasksDone, subtaskTotal),
                                container = scheme.surfaceContainer,
                                content = scheme.onSurfaceVariant,
                                leading = { SubtaskLeaves(subtasksDone, subtaskTotal, scheme.onSurfaceVariant) },
                            )
                        }
                    }
                }
            }

            if (task.isPinned || hasReminder || dragHandle != null) {
                Row(
                    modifier = Modifier.height(48.dp).padding(start = Space.x2),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (task.isPinned) {
                        Icon(
                            painter = painterResource(R.drawable.ic_pin),
                            contentDescription = pinnedLabel,
                            modifier = Modifier.size(16.dp),
                            tint = scheme.primary,
                        )
                    }
                    if (hasReminder) {
                        Icon(
                            painter = painterResource(R.drawable.ic_notification),
                            contentDescription = reminderLabel,
                            modifier = Modifier.size(16.dp),
                            tint = scheme.onSurfaceVariant,
                        )
                    }
                    dragHandle?.invoke()
                }
            }
        }
    }
}
