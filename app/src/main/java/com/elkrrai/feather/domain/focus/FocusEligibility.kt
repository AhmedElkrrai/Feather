package com.elkrrai.feather.domain.focus

import com.elkrrai.feather.domain.model.Task
import java.time.LocalDate

/** Focus is for the tasks of today: open, and due today. Not for upcoming, overdue, undated or finished ones. */
fun Task.canFocusOn(today: LocalDate): Boolean = !isCompleted && deletedAt == null && dueDate == today

/**
 * Whether the session on [task] has nothing left to run for: the task is gone, finished, or (a
 * repeating one) has moved on to a later day. A task that only became overdue at midnight keeps
 * its session, so a session that crosses midnight is not cut off.
 */
fun focusSessionEnded(task: Task?, today: LocalDate): Boolean =
    task == null || task.isCompleted || task.deletedAt != null || task.dueDate?.isAfter(today) == true
