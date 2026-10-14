package com.elkrrai.feather.ui.format

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * How a due date reads on a task's chip. Kept free of strings so the choice can be tested; the
 * words come from [dueChipText].
 */
sealed interface DueLabel {
    /** The time of day, when the task has one. */
    val time: LocalTime?

    /** Today, from 18:00 on. */
    data class Tonight(override val time: LocalTime) : DueLabel

    data class Today(override val time: LocalTime?) : DueLabel

    data class Tomorrow(override val time: LocalTime?) : DueLabel

    /** Within the coming week: just the weekday. */
    data class Weekday(val day: DayOfWeek, override val time: LocalTime?) : DueLabel

    /** Further away, or already in the past and finished: the date. */
    data class OnDate(val date: LocalDate, override val time: LocalTime?) : DueLabel

    /** An open task whose day has passed, in the last week: "Waiting since Mon". Never red. */
    data class WaitingWeekday(val day: DayOfWeek) : DueLabel {
        override val time: LocalTime? get() = null
    }

    /** An open task whose day passed more than a week ago. */
    data class WaitingDate(val date: LocalDate) : DueLabel {
        override val time: LocalTime? get() = null
    }

    /** Waiting tasks are shown quietly, in neutral colors. */
    val isWaiting: Boolean get() = this is WaitingWeekday || this is WaitingDate

    /** Today's tasks are shown with a warmer chip. */
    val isToday: Boolean get() = this is Today || this is Tonight
}

private const val EVENING_HOUR = 18
private const val WEEK_DAYS = 6L

fun dueLabelFor(date: LocalDate, time: LocalTime?, today: LocalDate, completed: Boolean): DueLabel {
    val days = ChronoUnit.DAYS.between(today, date)
    return when {
        days < 0 && !completed ->
            if (-days <= WEEK_DAYS) DueLabel.WaitingWeekday(date.dayOfWeek) else DueLabel.WaitingDate(date)

        days < 0 -> DueLabel.OnDate(date, time)
        days == 0L ->
            if (time != null && time.hour >= EVENING_HOUR) DueLabel.Tonight(time) else DueLabel.Today(time)

        days == 1L -> DueLabel.Tomorrow(time)
        days <= WEEK_DAYS -> DueLabel.Weekday(date.dayOfWeek, time)
        else -> DueLabel.OnDate(date, time)
    }
}
