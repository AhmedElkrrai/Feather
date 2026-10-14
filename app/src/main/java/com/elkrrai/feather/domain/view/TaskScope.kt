package com.elkrrai.feather.domain.view

/** What the task screen is showing: a smart view, one list, or one tag. */
sealed interface TaskScope {
    /** Open tasks due today or earlier. */
    data object Today : TaskScope

    /** Open tasks due from tomorrow up to a week ahead. */
    data object Upcoming : TaskScope

    /** Open tasks due before today. */
    data object Overdue : TaskScope

    /** Every task, completed ones last. */
    data object All : TaskScope

    data object Completed : TaskScope

    /** Open tasks without a due date. */
    data object NoDate : TaskScope

    data class InList(val listId: String) : TaskScope

    data class WithTag(val tagId: String) : TaskScope
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
