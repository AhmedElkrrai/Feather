package com.elkrrai.feather.domain.habit

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** The first day of the week that [date] is in. */
fun weekStartOf(date: LocalDate, firstDay: DayOfWeek): LocalDate = date.with(TemporalAdjusters.previousOrSame(firstDay))

/**
 * The goal in force for the week that starts on [weekStart], or null before the habit's first goal. A goal
 * counts for the week that its first day falls in, so a goal made when weeks began on another day still covers
 * the week it was made in.
 */
fun List<HabitGoal>.forWeek(weekStart: LocalDate): HabitGoal? =
    filter { !it.effectiveFrom.isAfter(weekStart.plusDays(6)) }.maxByOrNull { it.effectiveFrom }

/** The goal that takes effect after the week of [weekStart] (an edit made during the week), or null. */
fun List<HabitGoal>.queuedAfter(weekStart: LocalDate): HabitGoal? =
    filter { it.effectiveFrom.isAfter(weekStart.plusDays(6)) }.minByOrNull { it.effectiveFrom }

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

/** The first week the habit has anything in: its first goal or its first logged day, never after the current week. */
fun HabitState.firstWeekStart(currentWeekStart: LocalDate, firstDay: DayOfWeek): LocalDate {
    val earliest = (goals.map { it.effectiveFrom } + entries.keys).minOrNull() ?: currentWeekStart
    return minOf(weekStartOf(earliest, firstDay), currentWeekStart)
}

/**
 * The habit with its first goal moved back to cover [firstStart]. A first week that starts a little before
 * the first goal (the week start changed since it was made) is then still judged by that goal.
 */
fun HabitState.coveringFrom(firstStart: LocalDate): HabitState = copy(
    goals = goals.mapIndexed { index, goal ->
        if (index == 0) goal.copy(effectiveFrom = minOf(goal.effectiveFrom, firstStart)) else goal
    },
)

/** The whole of a habit in one place: each week from the first to this one, and a few quiet numbers. */
data class HabitOverview(
    /** Every week, oldest first, the last being this week. */
    val weeks: List<WeekProgress>,
    /** The weeks that reached the goal, and the weeks so far: this week counts only once it is met. */
    val weeksMet: Int,
    val weeksSoFar: Int,
    /** Days done ([HabitKind.DAYS]) or the amount logged ([HabitKind.AMOUNT]) over all the weeks. */
    val total: Int,
    /** The week with the most, or null when nothing was ever logged. */
    val best: WeekProgress?,
    val inARow: Int,
)

fun HabitState.overview(currentWeekStart: LocalDate, firstDay: DayOfWeek): HabitOverview {
    val first = firstWeekStart(currentWeekStart, firstDay)
    val covered = coveringFrom(first)
    val count = (ChronoUnit.WEEKS.between(first, currentWeekStart) + 1).toInt().coerceAtLeast(1)
    val weeks = (0 until count).map { covered.week(first.plusWeeks(it.toLong())) }
    val finished = weeks.dropLast(1)
    val current = weeks.last()
    val met = finished.count { it.met } + if (current.met) 1 else 0
    val sofar = finished.size + if (current.met) 1 else 0
    val total = weeks.sumOf { it.total }
    return HabitOverview(
        weeks = weeks,
        weeksMet = met,
        weeksSoFar = sofar,
        total = total,
        best = weeks.filter { it.total > 0 }.maxByOrNull { it.total },
        inARow = covered.weeksMetInARow(currentWeekStart),
    )
}
