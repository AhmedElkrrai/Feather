package com.elkrrai.feather.feature.tasklist

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.model.Task
import com.elkrrai.feather.ui.format.formatDueDate
import com.elkrrai.feather.ui.format.formatDueTime
import com.elkrrai.feather.ui.format.label
import com.elkrrai.feather.ui.format.markerColor
import com.elkrrai.feather.ui.format.ringColor
import com.elkrrai.feather.ui.components.CompletionRing
import com.elkrrai.feather.ui.components.StrikeText
import com.elkrrai.feather.ui.rememberHaptics
import java.time.LocalDate

/**
 * One task. [listName] is shown in front of the due date when the view mixes several lists
 * (smart views and tag views); pass null inside a single list.
 */
@Composable
fun TaskRow(
    task: Task,
    today: LocalDate,
    onToggle: (completed: Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    listName: String? = null,
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
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val dueText = remember(task.dueDate, task.dueTime, today, locale) {
        task.dueDate?.let { date ->
            val day = formatDueDate(context, date, today, locale)
            task.dueTime?.let { "$day, ${formatDueTime(it, locale)}" } ?: day
        }
    }
    val isOverdue = !task.isCompleted && task.dueDate?.isBefore(today) == true
    val markerColor = task.priority.markerColor()
    val priorityLabel = task.priority.label()
    val pinnedLabel = stringResource(R.string.pinned)
    val reminderLabel = stringResource(R.string.has_reminder)
    // A finished task no longer reminds anyone.
    val hasReminder = !task.isCompleted && task.reminders.isNotEmpty()
    val repeatLabel = stringResource(R.string.repeat_has)
    val repeats = !task.isCompleted && task.recurrence != null

    val haptics = rememberHaptics()
    val openLabel = stringResource(R.string.action_open_task)
    val selectLabel = stringResource(R.string.action_select)
    val doneActionLabel = stringResource(
        if (task.isCompleted) R.string.action_mark_not_done else R.string.action_mark_done
    )
    val deleteLabel = stringResource(R.string.action_delete)
    val pinActionLabel = stringResource(if (task.isPinned) R.string.action_unpin else R.string.action_pin)
    val overdueLabel = stringResource(R.string.scope_overdue)
    val moveUpLabel = stringResource(R.string.action_move_up)
    val moveDownLabel = stringResource(R.string.action_move_down)

    val metaLine = listOfNotNull(listName, dueText).joinToString(" · ").ifEmpty { null }
    val subtaskTotal = task.subtasks.size
    val subtasksDone = task.subtasks.count { it.isCompleted }
    val hasSupporting = metaLine != null || task.tags.isNotEmpty() || subtaskTotal > 0

    ListItem(
        modifier = modifier
            .combinedClickable(
                onClickLabel = openLabel,
                onLongClickLabel = if (onLongClick != null) selectLabel else null,
                onLongClick = onLongClick,
                onClick = onClick,
            )
            .semantics {
                this.selected = selected
                if (isOverdue) stateDescription = overdueLabel
                customActions = buildList {
                    add(CustomAccessibilityAction(doneActionLabel) { onToggle(!task.isCompleted); true })
                    if (onDelete != null) add(CustomAccessibilityAction(deleteLabel) { onDelete(); true })
                    if (onPinChange != null) {
                        add(CustomAccessibilityAction(pinActionLabel) { onPinChange(!task.isPinned); true })
                    }
                    if (onMoveUp != null) add(CustomAccessibilityAction(moveUpLabel) { onMoveUp(); true })
                    if (onMoveDown != null) add(CustomAccessibilityAction(moveDownLabel) { onMoveDown(); true })
                }
            },
        colors = ListItemDefaults.colors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                ListItemDefaults.containerColor
            },
        ),
        leadingContent = {
            // The ring is named after its task, so it is not just "checkbox" in a list of them.
            CompletionRing(
                checked = task.isCompleted,
                onCheckedChange = { checked ->
                    haptics.tick()
                    onToggle(checked)
                },
                ringColor = task.priority.ringColor(),
                contentDescription = task.title,
            )
        },
        headlineContent = {
            // The strike grows across the title; a finished title is dimmed to the secondary color.
            StrikeText(
                text = task.title,
                struck = task.isCompleted,
                activeColor = MaterialTheme.colorScheme.onSurface,
                doneColor = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = if (hasSupporting) {
            {
                Column {
                    if (metaLine != null) {
                        Text(
                            text = metaLine,
                            color = if (isOverdue) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    if (task.tags.isNotEmpty()) {
                        Text(
                            text = task.tags.joinToString("  ") { "#${it.name}" },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (subtaskTotal > 0) {
                        Text(
                            text = stringResource(R.string.subtask_progress, subtasksDone, subtaskTotal),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            null
        },
        trailingContent = if (task.isPinned || hasReminder || repeats || markerColor != null || dragHandle != null) {
            {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (task.isPinned) {
                        Icon(
                            painter = painterResource(R.drawable.ic_pin),
                            contentDescription = pinnedLabel,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (repeats) {
                        Icon(
                            painter = painterResource(R.drawable.ic_repeat),
                            contentDescription = repeatLabel,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (hasReminder) {
                        Icon(
                            painter = painterResource(R.drawable.ic_notification),
                            contentDescription = reminderLabel,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (markerColor != null) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .background(markerColor, CircleShape)
                                .semantics { contentDescription = priorityLabel }
                        )
                    }
                    dragHandle?.invoke()
                }
            }
        } else {
            null
        },
    )
}
