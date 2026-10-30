package com.elkrrai.feather.domain.recurrence

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import com.elkrrai.feather.domain.time.FIRST_DAY_OF_WEEK
import com.elkrrai.feather.domain.time.indexInWeek
import com.elkrrai.feather.domain.time.startOfWeek
import java.time.temporal.TemporalAdjusters

/**
 * The date a repeating task moves to when it is finished on [completedOn], or null if the
 * series is over (no occurrences left, or the next one would fall after [Recurrence.until]).
 *
 * - [RepeatMode.FIXED]: the first occurrence of the rule strictly after the later of [due] and
 *   [completedOn]. Finishing early therefore does not skip an occurrence, and finishing late
 *   jumps to the next future one instead of replaying the missed ones.
 * - [RepeatMode.AFTER_COMPLETION]: [completedOn] plus the interval.
 */
fun Recurrence.nextOccurrence(due: LocalDate, completedOn: LocalDate): LocalDate? {
    if (count != null && count <= 1) return null
    val next = when (mode) {
        RepeatMode.FIXED -> nextOnSchedule(due, after = if (completedOn.isAfter(due)) completedOn else due)
        RepeatMode.AFTER_COMPLETION -> nextAfterCompletion(completedOn)
    }
    return if (until != null && next.isAfter(until)) null else next
}

/** The rule for the occurrence that follows: the same, with one fewer occurrence to come. */
fun Recurrence.afterAdvance(): Recurrence = if (count != null) copy(count = count - 1) else this

private fun Recurrence.nextAfterCompletion(completedOn: LocalDate): LocalDate = when (frequency) {
    Frequency.DAILY -> completedOn.plusDays(interval.toLong())
    Frequency.WEEKLY -> completedOn.plusWeeks(interval.toLong())
    Frequency.MONTHLY -> completedOn.plusMonths(interval.toLong())
    Frequency.YEARLY -> completedOn.plusYears(interval.toLong())
}

private fun Recurrence.nextOnSchedule(due: LocalDate, after: LocalDate): LocalDate = when (frequency) {
    Frequency.DAILY -> nextDaily(due, after)
    Frequency.WEEKLY -> nextWeekly(due, after)
    Frequency.MONTHLY -> nextMonthly(due, after)
    Frequency.YEARLY -> nextYearly(due, after)
}

private fun Recurrence.nextDaily(due: LocalDate, after: LocalDate): LocalDate {
    val elapsed = ChronoUnit.DAYS.between(due, after)
    val steps = Math.floorDiv(elapsed, interval.toLong()) + 1
    return due.plusDays(steps * interval)
}

/** Weeks start on Saturday (see [FIRST_DAY_OF_WEEK]); with an interval above 1 they are counted from the week of [due]. */
private fun Recurrence.nextWeekly(due: LocalDate, after: LocalDate): LocalDate {
    val days = weekdays.ifEmpty { setOf(due.dayOfWeek) }.sortedBy { it.indexInWeek() }
    val firstWeek = due.startOfWeek()
    val afterWeek = after.startOfWeek()
    var step = Math.floorDiv(ChronoUnit.WEEKS.between(firstWeek, afterWeek), interval.toLong())
    while (true) {
        val week = firstWeek.plusWeeks(step * interval)
        for (day in days) {
            val candidate = week.plusDays(day.indexInWeek().toLong())
            if (candidate.isAfter(after)) return candidate
        }
        step++
    }
}

private fun Recurrence.nextMonthly(due: LocalDate, after: LocalDate): LocalDate {
    val firstMonth = YearMonth.from(due)
    var step = Math.floorDiv(ChronoUnit.MONTHS.between(firstMonth, YearMonth.from(after)), interval.toLong())
    while (true) {
        val candidate = monthlyDate(firstMonth.plusMonths(step * interval), due)
        if (candidate.isAfter(after)) return candidate
        step++
    }
}

private fun Recurrence.monthlyDate(month: YearMonth, due: LocalDate): LocalDate {
    if (nthWeekday != null) {
        return month.atDay(1).with(TemporalAdjusters.dayOfWeekInMonth(nthWeekday.ordinal, nthWeekday.day))
    }
    return month.atDay(minOf(monthDay ?: due.dayOfMonth, month.lengthOfMonth()))
}

private fun Recurrence.nextYearly(due: LocalDate, after: LocalDate): LocalDate {
    val targetMonth = month ?: due.monthValue
    val targetDay = monthDay ?: due.dayOfMonth
    var step = Math.floorDiv((after.year - due.year).toLong(), interval.toLong())
    while (true) {
        val year = YearMonth.of((due.year + step * interval).toInt(), targetMonth)
        val candidate = year.atDay(minOf(targetDay, year.lengthOfMonth()))
        if (candidate.isAfter(after)) return candidate
        step++
    }
}
