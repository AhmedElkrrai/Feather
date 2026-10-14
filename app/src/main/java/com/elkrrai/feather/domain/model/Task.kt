package com.elkrrai.feather.domain.model

import com.elkrrai.feather.domain.recurrence.Recurrence
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * A todo item. Due date and time are floating local values (not instants), so a task due at
 * 17:00 stays at 17:00 when the user changes timezone.
 *
 * Only top-level tasks are modeled as [Task]; their checklist items are [subtasks].
 */
data class Task(
    val id: String,
    val listId: String,
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
) {
    val isCompleted: Boolean get() = completedAt != null
}
