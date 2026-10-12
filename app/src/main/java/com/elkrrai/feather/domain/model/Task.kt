package com.elkrrai.feather.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * A todo item. Due date and time are floating local values (not instants), so a task due at
 * 17:00 stays at 17:00 when the user changes timezone.
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
) {
    val isCompleted: Boolean get() = completedAt != null
}
