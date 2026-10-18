package com.elkrrai.feather.domain.rhythm

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/**
 * The week that just ended, as a feather made on the phone: the color is the list that had the
 * most finished tasks, each day has as many barbs as it had finished tasks (up to [MAX_BARBS]), and
 * a day of rest is a small notch.
 */
data class WeeklyFeather(
    /** The first day of the week the feather is for. */
    val weekStart: LocalDate,
    /** Finished tasks on each of the seven days, from [weekStart]. */
    val days: List<Int>,
    /** The list with the most finished tasks that week, or null when there was a tie at nothing. */
    val topListId: String?,
) {
    val total: Int get() = days.sum()
    val restDays: Int get() = days.count { it == 0 }
}

const val MAX_BARBS = 4

/** How many barbs a day of [count] finished tasks gets on the feather. */
fun barbsFor(count: Int): Int = count.coerceIn(0, MAX_BARBS)

/**
 * The feather for the week before the one that [today] is in, or null if nothing was finished that
 * week. Weeks start on [firstDay].
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
    if (inWeek.isEmpty()) return null
    val days = (0L..6L).map { offset ->
        val day = lastWeek.plusDays(offset)
        inWeek.count { it.completedAt.atZone(zone).toLocalDate() == day }
    }
    val top = inWeek.groupingBy { it.listId }.eachCount()
        .entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .firstOrNull()?.key
    return WeeklyFeather(lastWeek, days, top)
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
