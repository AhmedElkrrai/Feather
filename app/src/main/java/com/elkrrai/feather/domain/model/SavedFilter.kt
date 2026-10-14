package com.elkrrai.feather.domain.model

import java.time.LocalDate

/** Which due dates a [SavedFilter] lets through. */
enum class DueWindow(val code: Int) {
    ANY(0),
    OVERDUE(1),
    TODAY(2),
    NEXT_7_DAYS(3),
    NO_DATE(4);

    companion object {
        fun fromCode(code: Int): DueWindow = entries.firstOrNull { it.code == code } ?: ANY
    }
}

/**
 * A named view the user made: every condition that is set must hold. [listId] and [tagId]
 * (when set) must match the task's list and one of its tags; [minPriority] lets through tasks
 * of that priority or higher ([Priority.NONE] means any); [due] limits the due date;
 * [hideCompleted] leaves finished tasks out.
 */
data class SavedFilter(
    val id: String,
    val name: String,
    val listId: String? = null,
    val tagId: String? = null,
    val minPriority: Priority = Priority.NONE,
    val due: DueWindow = DueWindow.ANY,
    val hideCompleted: Boolean = true,
    val sortOrder: Long = 0,
) {
    /** True if [task] passes every condition as of [today]. */
    fun accepts(task: Task, today: LocalDate): Boolean {
        if (hideCompleted && task.isCompleted) return false
        if (listId != null && task.listId != listId) return false
        if (tagId != null && task.tags.none { it.id == tagId }) return false
        if (task.priority.level < minPriority.level) return false
        val date = task.dueDate
        return when (due) {
            DueWindow.ANY -> true
            DueWindow.OVERDUE -> date != null && date.isBefore(today)
            DueWindow.TODAY -> date == today
            DueWindow.NEXT_7_DAYS -> date != null && !date.isBefore(today) && !date.isAfter(today.plusDays(NEXT_DAYS))
            DueWindow.NO_DATE -> date == null
        }
    }

    private companion object {
        const val NEXT_DAYS = 7L
    }
}
