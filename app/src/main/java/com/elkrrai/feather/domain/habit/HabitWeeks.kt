package com.elkrrai.feather.domain.habit

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** The first day of the week that [date] is in. */
fun weekStartOf(date: LocalDate, firstDay: DayOfWeek): LocalDate = date.with(TemporalAdjusters.previousOrSame(firstDay))

/** The goal in force for the week that starts on [weekStart], or null before the habit's first goal. */
fun List<HabitGoal>.forWeek(weekStart: LocalDate): HabitGoal? =
    filter { !it.effectiveFrom.isAfter(weekStart) }.maxByOrNull { it.effectiveFrom }

/** The goal that takes effect after [weekStart] (an edit made during the week), or null. */
fun List<HabitGoal>.queuedAfter(weekStart: LocalDate): HabitGoal? =
    filter { it.effectiveFrom.isAfter(weekStart) }.minByOrNull { it.effectiveFrom }

/** One day of a week: how much was logged and whether it counts as done. */
data class DayProgress(val date: LocalDate, val amount: Int, val done: Boolean)

/** One week of a habit against the goal that was in force then. [goal] is null for a week before the habit existed. */
data class WeekProgress(
    val weekStart: LocalDate,
    val goal: HabitGoal?,
    /** [HabitKind.DAYS]: the days done. [HabitKind.AMOUNT]: the total logged. */
    val total: Int,
    val days: List<DayProgress>,
) {
    val target: Int get() = goal?.target ?: 0

    /** Reaching or passing the goal counts as met. */
    val met: Boolean get() = goal != null && total >= target

    /** How far along, 0 to 1 (more than met is still 1). */
    val fraction: Float get() = if (goal == null || target <= 0) 0f else (total.toFloat() / target).coerceAtMost(1f)
}

/** The week that starts on [weekStart]: each day's amount, and the total against that week's goal. */
fun HabitState.week(weekStart: LocalDate): WeekProgress {
    val goal = goals.forWeek(weekStart)
    val days = (0 until DAYS_IN_WEEK).map { offset ->
        val date = weekStart.plusDays(offset.toLong())
        val amount = entries[date] ?: 0
        val done = when (habit.kind) {
            HabitKind.DAYS -> goal != null && amount >= (goal.dailyTarget).coerceAtLeast(1)
            HabitKind.AMOUNT -> amount > 0
        }
        DayProgress(date, amount, done)
    }
    val total = when (habit.kind) {
        HabitKind.DAYS -> days.count { it.done }
        HabitKind.AMOUNT -> days.sumOf { it.amount }
    }
    return WeekProgress(weekStart, goal, total, days)
}

/** The last [weeks] weeks up to and including the week of [currentWeekStart], oldest first. */
fun HabitState.history(currentWeekStart: LocalDate, weeks: Int): List<WeekProgress> =
    (weeks - 1 downTo 0).map { back -> week(currentWeekStart.minusWeeks(back.toLong())) }

/**
 * How many weeks in a row ended with the goal met: the finished weeks before [currentWeekStart], and
 * this week too once it is met. A week before the habit existed ends the count. It is only ever shown
 * as a quiet line; a missed week just takes it away.
 */
fun HabitState.weeksMetInARow(currentWeekStart: LocalDate): Int {
    var count = if (week(currentWeekStart).met) 1 else 0
    var start = currentWeekStart.minusWeeks(1)
    while (true) {
        val progress = week(start)
        if (!progress.met) return count
        count++
        start = start.minusWeeks(1)
    }
}

/** True when [date] is a day that can be logged: in the week that starts on [weekStart], and not after [today]. */
fun canLog(date: LocalDate, weekStart: LocalDate, today: LocalDate): Boolean =
    !date.isBefore(weekStart) && date.isBefore(weekStart.plusDays(DAYS_IN_WEEK.toLong())) && !date.isAfter(today)
