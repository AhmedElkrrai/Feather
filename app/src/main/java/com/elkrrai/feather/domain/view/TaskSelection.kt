package com.elkrrai.feather.domain.view

import com.elkrrai.feather.domain.model.Task
import java.time.LocalDate

/** How many days ahead the Upcoming view reaches. */
const val UPCOMING_DAYS = 7L

/** True if this task belongs in [scope] as of [today]. Pure, so it is cheap to unit test. */
fun Task.matches(scope: TaskScope, today: LocalDate): Boolean = when (scope) {
    TaskScope.Today -> !isCompleted && dueDate != null && !dueDate.isAfter(today)
    TaskScope.Upcoming -> !isCompleted && dueDate != null &&
        dueDate.isAfter(today) && !dueDate.isAfter(today.plusDays(UPCOMING_DAYS))
    TaskScope.Overdue -> !isCompleted && dueDate != null && dueDate.isBefore(today)
    TaskScope.All -> true
    TaskScope.Completed -> isCompleted
    TaskScope.NoDate -> !isCompleted && dueDate == null
    is TaskScope.InList -> listId == scope.listId
    is TaskScope.WithTag -> tags.any { it.id == scope.tagId }
}

fun List<Task>.inScope(scope: TaskScope, today: LocalDate): List<Task> =
    filter { it.matches(scope, today) }

/** Number of open (not completed) tasks in [scope], for badges. */
fun List<Task>.openCountIn(scope: TaskScope, today: LocalDate): Int =
    count { !it.isCompleted && it.matches(scope, today) }

/**
 * Pinned tasks first, then open before completed, then the order chosen by [this]. Tasks
 * without a due date sort after dated ones.
 */
fun TaskSort.comparator(): Comparator<Task> {
    val noDate = Long.MAX_VALUE
    val noTime = Int.MAX_VALUE
    val byChoice: Comparator<Task> = when (this) {
        TaskSort.CREATED -> compareBy<Task> { it.createdAt }.thenBy { it.sortOrder }
        TaskSort.MANUAL -> compareBy<Task> { it.sortOrder }.thenBy { it.createdAt }
        TaskSort.DUE_DATE -> compareBy<Task> { it.dueDate?.toEpochDay() ?: noDate }
            .thenBy { it.dueTime?.toSecondOfDay() ?: noTime }
            .thenBy { it.sortOrder }
        TaskSort.PRIORITY -> compareByDescending<Task> { it.priority.level }
            .thenBy { it.dueDate?.toEpochDay() ?: noDate }
            .thenBy { it.dueTime?.toSecondOfDay() ?: noTime }
            .thenBy { it.sortOrder }
    }
    return compareBy<Task> { !it.isPinned }
        .thenBy { it.isCompleted }
        .then(byChoice)
}

/** Tasks of one calendar day, used by the Upcoming view. */
data class DayGroup(val date: LocalDate, val tasks: List<Task>)

/** Groups dated tasks by due day, earliest day first; the order inside a day is kept. */
fun List<Task>.groupedByDay(): List<DayGroup> =
    filter { it.dueDate != null }
        .groupBy { it.dueDate!! }
        .entries
        .sortedBy { it.key.toEpochDay() }
        .map { DayGroup(date = it.key, tasks = it.value) }

/**
 * Case-insensitive "contains" search over title, notes, tag names and subtask titles. A blank
 * query matches nothing.
 */
fun List<Task>.search(query: String): List<Task> {
    val q = query.trim()
    if (q.isEmpty()) return emptyList()
    return filter { task ->
        task.title.contains(q, ignoreCase = true) ||
            task.notes.contains(q, ignoreCase = true) ||
            task.tags.any { it.name.contains(q, ignoreCase = true) } ||
            task.subtasks.any { it.title.contains(q, ignoreCase = true) }
    }
}
