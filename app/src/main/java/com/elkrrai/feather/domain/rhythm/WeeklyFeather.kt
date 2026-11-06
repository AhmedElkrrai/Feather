package com.elkrrai.feather.domain.rhythm

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/**
 * The week that just ended, and the feather it earned: one of eight, by how many of its days had a finished task ([activeDays], 0 to 7).
 * A week with none is a quiet week and gets the quiet feather.
 */
data class WeeklyFeather(
    /** The first day of the week the feather is for. */
    val weekStart: LocalDate,
    /** Finished tasks on each of the seven days, from [weekStart]. */
    val days: List<Int>,
) {
    val total: Int get() = days.sum()
    val restDays: Int get() = days.count { it == 0 }

    /** On how many of the seven days something was finished: which of the eight feathers the week earned. */
    val activeDays: Int get() = days.count { it > 0 }
}

/**
 * The feather for the week before the one that [today] is in. A week with nothing finished still earns the quiet feather, as long as
 * something was finished before this week: someone who has only just started has no last week to show. Weeks start on [firstDay].
 */
fun weeklyFeather(
    records: List<CompletionRecord>,
    today: LocalDate,
    zone: ZoneId,
    firstDay: DayOfWeek,
): WeeklyFeather? {
    val thisWeek = today.minusDays(Math.floorMod(today.dayOfWeek.value - firstDay.value, 7).toLong())
    val lastWeek = thisWeek.minusWeeks(1)
    val inWeek = records.filter {
        val day = it.completedAt.atZone(zone).toLocalDate()
        !day.isBefore(lastWeek) && day.isBefore(thisWeek)
    }
    if (inWeek.isEmpty() && records.none { it.completedAt.atZone(zone).toLocalDate().isBefore(thisWeek) }) return null
    val days = (0L..6L).map { offset ->
        val day = lastWeek.plusDays(offset)
        inWeek.count { it.completedAt.atZone(zone).toLocalDate() == day }
    }
    return WeeklyFeather(lastWeek, days)
}

/** Whether today is the first day of the week, which is when the feather is shown. */
fun isFeatherDay(today: LocalDate, firstDay: DayOfWeek): Boolean = today.dayOfWeek == firstDay

/**
 * Whether some repeating task (one finished more than once) was finished in each of the last four
 * weeks, this one included: the bunting.
 */
fun keptRepeaterFourWeeks(
    records: List<CompletionRecord>,
    today: LocalDate,
    zone: ZoneId,
    firstDay: DayOfWeek,
): Boolean {
    val thisWeek = today.minusDays(Math.floorMod(today.dayOfWeek.value - firstDay.value, 7).toLong())
    return records.groupBy { it.taskId }
        .filterValues { it.size > 1 }
        .any { (_, list) ->
            val days = list.map { it.completedAt.atZone(zone).toLocalDate() }
            (0L..3L).all { weeksBack ->
                val start = thisWeek.minusWeeks(weeksBack)
                days.any { !it.isBefore(start) && !it.isAfter(start.plusDays(6)) }
            }
        }
}
