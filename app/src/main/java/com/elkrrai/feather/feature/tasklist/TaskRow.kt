package com.elkrrai.feather.feature.tasklist

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.semantics.Role
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.lerp
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
import com.elkrrai.feather.domain.focus.FocusSession
import com.elkrrai.feather.domain.focus.focusClock
import com.elkrrai.feather.domain.model.Task
import com.elkrrai.feather.ui.components.CompletionRing
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
import com.elkrrai.feather.ui.rememberReduceMotion
import com.elkrrai.feather.ui.theme.Ease
import com.elkrrai.feather.ui.theme.Space
import com.elkrrai.feather.ui.theme.featherColors
import java.time.LocalDate
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import com.elkrrai.feather.ui.components.SubtaskProgressBar
import com.elkrrai.feather.ui.theme.Dur

/** "To today": the one-tap way out of the Overdue view. */
@Composable
private fun MoveToTodayButton(title: String, onClick: () -> Unit) {
    val haptics = rememberHaptics()
    val label = stringResource(R.string.action_move_to_today, title)
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.heightIn(min = 40.dp),
    ) {
        Box(
            modifier = Modifier
                .clickable(onClickLabel = label, role = Role.Button) {
                    haptics.tick()
                    onClick()
                }
                .heightIn(min = 40.dp)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = stringResource(R.string.move_to_today_short), style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** The time left of a focus session, counting down by the second (a paused one stands still). */
@Composable
private fun FocusChip(session: FocusSession) {
    val scheme = MaterialTheme.colorScheme
    val locale = LocalConfiguration.current.locales[0]
    val now by produceState(System.currentTimeMillis(), session) {
        while (!session.isPaused) {
            value = System.currentTimeMillis()
            delay(1_000)
        }
    }
    MetaChip(
        text = focusClock(session.remaining(now), locale),
        container = scheme.tertiaryContainer,
        content = scheme.onTertiaryContainer,
        leading = {
            Icon(
                painter = painterResource(R.drawable.ic_timer),
                contentDescription = stringResource(R.string.focus_title),
                modifier = Modifier.size(12.dp),
                tint = scheme.onTertiaryContainer,
            )
        },
    )
}

/**
 * One task, as a card: the completion ring, the title, and a row of small chips (date, repeat,
 * tags, subtask progress).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskRow(
    task: Task,
    today: LocalDate,
    onToggle: (completed: Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** False in the Completed view, where a finished title is shown plainly, not struck through. */
    strikeTitle: Boolean = true,
    /** When set, TalkBack offers "Delete" as an action (the swipe is not reachable with it). */
    onDelete: (() -> Unit)? = null,
    /** When set, TalkBack offers pin and unpin. */
    onPinChange: ((Boolean) -> Unit)? = null,
    /** Part of the current selection (highlighted and announced as selected). */
    selected: Boolean = false,
    /** The task was just added: its card is tinted, then fades back to normal over 800 ms. */
    highlighted: Boolean = false,
    /** Called with the ring's position (window pixels) and the size of the burst when a task or subtask is finished. */
    onCelebrate: (origin: Offset, scale: Float) -> Unit = { _, _ -> },
    /** A long press, used to start selecting; null leaves the row without one. */
    onLongClick: (() -> Unit)? = null,
    /** The grip shown at the end of the row while the list can be reordered. */
    dragHandle: (@Composable () -> Unit)? = null,
    /** TalkBack actions that stand in for dragging. */
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    /** When both are set, the card can open to show its subtasks and take a new one. */
    onAddSubtask: ((title: String) -> Unit)? = null,
    onToggleSubtask: ((subtaskId: String) -> Unit)? = null,
    /** For a counter task: add to the count (the "+25" button, or a step taken back), and set it exactly. */
    onCounterAdd: ((amount: Int) -> Unit)? = null,
    onCounterSet: ((done: Int) -> Unit)? = null,
    /** The focus session on this task, when there is one: its time left shows as a chip. */
    focus: FocusSession? = null,
    /** In the Overdue view: puts the task on today with one tap. */
    onMoveToToday: (() -> Unit)? = null,
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
    val hasChips = due != null || recurrence != null || task.tags.isNotEmpty() || focus != null
    val counter = task.counter
    val counterState = counter?.let { stringResource(R.string.subtasks_of, it.done, it.target) }
    val canExpand = onAddSubtask != null && onToggleSubtask != null && !task.isCompleted && counter == null
    val showCounterButton = counter != null && !task.isCompleted && onCounterAdd != null
    var counterDialog by rememberSaveable(task.id) { mutableStateOf(false) }
    var expanded by rememberSaveable(task.id) { mutableStateOf(false) }

    val baseColor = if (selected) scheme.secondaryContainer else feather.card
    val cardColor by animateColorAsState(
        targetValue = if (highlighted) lerp(baseColor, scheme.primaryContainer, 0.3f) else baseColor,
        animationSpec = if (rememberReduceMotion()) snap() else tween(800, easing = Ease.Soft),
        label = "cardColor",
    )

    Surface(
        modifier = modifier.padding(horizontal = Space.gutter, vertical = Space.x1),
        shape = RoundedCornerShape(20.dp),
        color = cardColor,
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
                    if (waiting) stateDescription = waitingLabel else if (counterState != null) stateDescription = counterState
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
                onCelebrate = { origin -> onCelebrate(origin, 1f) },
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = Space.x3, bottom = Space.x3),
            ) {
                // The strike grows across the title; a finished title is dimmed to the secondary color.
                StrikeText(
                    text = task.title,
                    struck = strikeTitle && task.isCompleted,
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
                        if (focus != null) FocusChip(focus)
                        if (due != null) {
                            val container = when {
                                due.isWaiting -> scheme.surfaceVariant
                                due.isToday -> scheme.secondaryContainer
                                else -> scheme.surfaceContainer
                            }
                            val content = if (due.isToday) scheme.onSecondaryContainer else scheme.onSurfaceVariant
                            MetaChip(text = due.chipText(), container = container, content = content)
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
                        // Every tag shows; the row wraps onto another line when there are many.
                        for (tag in task.tags) {
                            MetaChip(
                                text = tag.name,
                                container = scheme.primaryContainer,
                                content = scheme.onPrimaryContainer,
                            )
                        }
                    }
                }
                // The bar fills as subtasks are finished, with the count beside it.
                if (subtaskTotal > 0) {
                    Spacer(Modifier.height(Space.x2))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Space.x2),
                    ) {
                        SubtaskProgressBar(done = subtasksDone, total = subtaskTotal, modifier = Modifier.weight(1f))
                        Text(
                            text = stringResource(R.string.subtasks_of, subtasksDone, subtaskTotal),
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
                // A counter task shows how far it has got, with the count beside the bar.
                if (counter != null) {
                    Spacer(Modifier.height(Space.x2))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Space.x2),
                    ) {
                        SubtaskProgressBar(done = counter.done, total = counter.target, modifier = Modifier.weight(1f))
                        Text(
                            text = stringResource(R.string.counter_of, counter.done, counter.target),
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
                if (canExpand) {
                    AnimatedVisibility(
                        visible = expanded,
                        enter = expandVertically(tween(Dur.Medium1, easing = Ease.Settle)) + fadeIn(tween(Dur.Short2)),
                        exit = shrinkVertically(tween(Dur.Short2, easing = Ease.Leave)) + fadeOut(tween(Dur.Micro)),
                    ) {
                        SubtaskPanel(
                            subtasks = task.subtasks,
                            onToggle = { id -> onToggleSubtask?.invoke(id) },
                            onAdd = { title -> onAddSubtask?.invoke(title) },
                            // Finishing the last subtask finishes the task, so that burst is a full one.
                            onCelebrate = { origin ->
                                onCelebrate(origin, if (subtasksDone + 1 >= subtaskTotal) 1f else 0.6f)
                            },
                            modifier = Modifier.padding(top = Space.x1),
                        )
                    }
                }
            }

            if (task.isPinned || hasReminder || dragHandle != null || canExpand || showCounterButton || onMoveToToday != null) {
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
                    if (canExpand) {
                        val rotation by animateFloatAsState(
                            targetValue = if (expanded) 180f else 0f,
                            animationSpec = tween(Dur.Short2, easing = Ease.Soft),
                            label = "chevron",
                        )
                        IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(40.dp)) {
                            Icon(
                                painter = painterResource(
                                    if (subtaskTotal > 0) R.drawable.ic_chevron_down else R.drawable.ic_add
                                ),
                                contentDescription = stringResource(
                                    when {
                                        subtaskTotal == 0 -> R.string.add_subtask
                                        expanded -> R.string.subtasks_hide
                                        else -> R.string.subtasks_show
                                    }
                                ),
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer { rotationZ = if (subtaskTotal > 0) rotation else 0f },
                                tint = scheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (onMoveToToday != null) MoveToTodayButton(title = task.title, onClick = onMoveToToday)
                    if (showCounterButton && counter != null && onCounterAdd != null) {
                        CounterStepButton(
                            counter = counter,
                            title = task.title,
                            onAdd = onCounterAdd,
                            onLongClick = { counterDialog = true },
                            onCelebrate = onCelebrate,
                        )
                    }
                    dragHandle?.invoke()
                }
            }
        }
    }

    if (counterDialog && counter != null && onCounterAdd != null && onCounterSet != null) {
        CounterDialog(
            counter = counter,
            onSet = { done ->
                onCounterSet(done)
                counterDialog = false
            },
            onTakeBack = {
                onCounterAdd(-minOf(counter.step, counter.done))
                counterDialog = false
            },
            onDismiss = { counterDialog = false },
        )
    }
}
