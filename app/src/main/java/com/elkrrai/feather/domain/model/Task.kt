package com.elkrrai.feather.domain.model

import com.elkrrai.feather.domain.recurrence.Recurrence
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * A todo item. Due date and time are floating local values (not instants), so a task due at
 * 17:00 stays at 17:00 when the user changes timezone.
 *
 * Only top-level tasks are modeled as [Task]; their checklist items are [subtasks].
 */
data class Task(
    val id: String,
    val title: String,
    val notes: String = "",
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    val priority: Priority = Priority.NONE,
    val completedAt: Instant? = null,
    val sortOrder: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
    val isPinned: Boolean = false,
    val tags: List<Tag> = emptyList(),
    val subtasks: List<Subtask> = emptyList(),
    /** Editable reminders plus any pending snooze; the editor works with the former only. */
    val reminders: List<Reminder> = emptyList(),
    /** How the task repeats, or null for a one-off. A repeating task always has a due date. */
    val recurrence: Recurrence? = null,
    /** Only set for tasks read from the trash; live tasks are never deleted. */
    val deletedAt: Instant? = null,
    /** When this task was last finished (a repeating task keeps going, so this is its history); null if never. */
    val lastCompletedAt: Instant? = null,
) {
    val isCompleted: Boolean get() = completedAt != null

    /**
     * A repeating task that was finished today and is not due again until a later day: it is done
     * for today and should not show as open until its next date arrives.
     */
    fun isRestingUntilNext(today: LocalDate, zone: ZoneId): Boolean =
        recurrence != null && completedAt == null && dueDate != null && dueDate.isAfter(today) &&
            lastCompletedAt?.atZone(zone)?.toLocalDate() == today

    /** How a resting task is shown in a Done section: finished, with its checklist ticked. */
    fun asFinishedToday(): Task =
        copy(completedAt = lastCompletedAt, subtasks = subtasks.map { it.copy(isCompleted = true) })
}
