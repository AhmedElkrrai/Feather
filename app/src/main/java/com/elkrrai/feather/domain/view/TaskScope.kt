package com.elkrrai.feather.domain.view

/** What the task screen is showing: one of the five views. */
sealed interface TaskScope {
    /** Open tasks due today or earlier. */
    data object Today : TaskScope

    /** Open tasks due from tomorrow up to a week ahead. */
    data object Upcoming : TaskScope

    /** Open tasks due before today. */
    data object Overdue : TaskScope

    data object Completed : TaskScope

    /** Open tasks without a due date: the Goals view. */
    data object NoDate : TaskScope
}

enum class TaskSort {
    /** Creation order, oldest first (not changed by dragging). */
    CREATED,

    /** Earliest due date first; tasks without a date last. */
    DUE_DATE,

    /** Highest priority first, then by due date. */
    PRIORITY,

    /** The order the user dragged the tasks into. */
    MANUAL,
}
