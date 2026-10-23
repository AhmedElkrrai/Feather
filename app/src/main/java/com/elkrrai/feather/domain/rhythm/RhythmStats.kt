package com.elkrrai.feather.domain.rhythm

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** One finished task: which, what it was called, and when. */
data class CompletionRecord(
    val taskId: String,
    val title: String,
    val completedAt: Instant,
)

/** The part of the day a task was finished in. */
enum class TimeOfDay { MORNING, AFTERNOON, EVENING, NIGHT }

/** Morning from 05:00, afternoon from 12:00, evening from 17:00, night from 21:00. */
fun timeOfDayFor(hour: Int): TimeOfDay = when (hour) {
    in 5..11 -> TimeOfDay.MORNING
    in 12..16 -> TimeOfDay.AFTERNOON
    in 17..20 -> TimeOfDay.EVENING
    else -> TimeOfDay.NIGHT
}

data class DayCount(val date: LocalDate, val count: Int)

/** One of the fourteen dots: did the person show up that day (finish at least one task)? */
data class RhythmDot(val date: LocalDate, val showedUp: Boolean)

/**
 * One day of the month grid. [level] is 0 for an empty day and 1 to 4 for how busy it was (the
 * four straw tones); [isFuture] days are drawn dashed.
 */
data class MonthCell(
    val date: LocalDate,
    val count: Int,
    val level: Int,
    val isFuture: Boolean,
    val isToday: Boolean,
)

/** A month as a grid of weeks: [leadingBlanks] empty cells come before the first of the month. */
data class MonthGrid(val month: YearMonth, val leadingBlanks: Int, val cells: List<MonthCell>)

/** A repeating task that is going well, with a dot for each of the last seven days. */
data class SteadyTask(val taskId: String, val title: String, val lastSeven: List<Boolean>)

/**
 * Everything the "Your rhythm" screen shows, worked out from the finished tasks alone. There are no
 * streaks, no comparisons with other weeks and nothing that can be lost.
 */
data class RhythmStats(
    /** Every task ever finished (that is still on record). */
    val total: Int,
    /** Finished in the last seven days, today included. */
    val weekTotal: Int,
    /** The last seven days, oldest first, ending today. */
    val lastSeven: List<DayCount>,
    /** The last [RHYTHM_DAYS] days that were not paused, oldest first. */
    val dots: List<RhythmDot>,
    val month: MonthGrid,
    /** The weekday that is busiest over the last eight weeks, or null when there is not enough to say. */
    val busiestWeekday: DayOfWeek?,
    /** How many were finished in each part of the day over the last 30 days. */
    val timeOfDay: Map<TimeOfDay, Int>,
    /** The part of the day with the most, or null when nothing was finished. */
    val bestTime: TimeOfDay?,
    val steady: List<SteadyTask>,
    /** Everything finished, by day, for the month sheet. */
    val byDay: Map<LocalDate, List<CompletionRecord>>,
) {
    val dotsShowedUp: Int get() = dots.count { it.showedUp }

    /** Before three finished tasks there is no chart to show, just a welcome. */
    val isNew: Boolean get() = total < NEW_USER_BELOW
}

const val RHYTHM_DAYS = 14
const val NEW_USER_BELOW = 3

/** How many tones a busy day can have; 0 is an empty day. */
fun strawLevel(count: Int): Int = when {
    count <= 0 -> 0
    count == 1 -> 1
    count == 2 -> 2
    count <= 4 -> 3
    else -> 4
}

/**
 * Works out [RhythmStats] for [today]. Days are the person's own (in [zone]); the month grid starts
 * its weeks on [firstDay]. [pausedDays] are left out of the fourteen dots, so a break does not look
 * like a gap.
 */
fun rhythmStats(
    records: List<CompletionRecord>,
    today: LocalDate,
    zone: ZoneId,
    firstDay: DayOfWeek,
    pausedDays: Set<LocalDate> = emptySet(),
): RhythmStats {
    val dated = records.map { it to it.completedAt.atZone(zone).toLocalDate() }
    val byDay = dated.groupBy({ it.second }, { it.first }).mapValues { (_, list) -> list.sortedBy { it.completedAt } }
    fun countOn(date: LocalDate) = byDay[date]?.size ?: 0

    val lastSeven = (6 downTo 0).map { back -> today.minusDays(back.toLong()).let { DayCount(it, countOn(it)) } }

    // Walk back from today, skipping paused days, until there are fourteen (or the history is long enough to stop).
    val dayDots = ArrayList<RhythmDot>()
    var cursor = today
    var steps = 0
    while (dayDots.size < RHYTHM_DAYS && steps < 200) {
        if (cursor !in pausedDays) dayDots.add(RhythmDot(cursor, countOn(cursor) > 0))
        cursor = cursor.minusDays(1)
        steps++
    }
    val dots = dayDots.reversed()

    val month = YearMonth.from(today)
    val first = month.atDay(1)
    val cells = (1..month.lengthOfMonth()).map { day ->
        val date = month.atDay(day)
        val count = countOn(date)
        MonthCell(date, count, strawLevel(count), isFuture = date.isAfter(today), isToday = date == today)
    }
    val blanks = Math.floorMod(first.dayOfWeek.value - firstDay.value, 7)

    val eightWeeksAgo = today.minusDays(56)
    val weekdayCounts = dated.filter { it.second > eightWeeksAgo && !it.second.isAfter(today) }
        .groupingBy { it.second.dayOfWeek }.eachCount()
    val busiest = weekdayCounts.maxByOrNull { it.value }?.takeIf { it.value >= 2 }?.key

    val thirtyDaysAgo = today.minusDays(30)
    val recent = dated.filter { it.second > thirtyDaysAgo && !it.second.isAfter(today) }
    val times = TimeOfDay.entries.associateWith { slot ->
        recent.count { timeOfDayFor(it.first.completedAt.atZone(zone).hour) == slot }
    }
    val bestTime = times.maxByOrNull { it.value }?.takeIf { it.value > 0 }?.key

    return RhythmStats(
        total = records.size,
        weekTotal = lastSeven.sumOf { it.count },
        lastSeven = lastSeven,
        dots = dots,
        month = MonthGrid(month, blanks, cells),
        busiestWeekday = busiest,
        timeOfDay = times,
        bestTime = bestTime,
        steady = steadyTasks(dated, today, firstDay),
        byDay = byDay,
    )
}

/**
 * Repeating tasks that are doing well: finished on at least four of the last seven days, or in each
 * of the last four weeks. A task is repeating if it has been finished more than once. Tasks that are
 * struggling are never shown.
 */
internal fun steadyTasks(
    dated: List<Pair<CompletionRecord, LocalDate>>,
    today: LocalDate,
    firstDay: DayOfWeek,
): List<SteadyTask> {
    val weekStart = today.minusDays(Math.floorMod(today.dayOfWeek.value - firstDay.value, 7).toLong())
    return dated.groupBy { it.first.taskId }
        .filterValues { it.size > 1 }
        .mapNotNull { (taskId, list) ->
            val days = list.map { it.second }.toSet()
            val inWeek = (0..6).map { back -> today.minusDays(6L - back) in days }
            val daysInWeek = inWeek.count { it }
            val weeksRunning = (0..3).all { weeksBack ->
                val start = weekStart.minusWeeks(weeksBack.toLong())
                val end = start.plusDays(6)
                days.any { !it.isBefore(start) && !it.isAfter(end) }
            }
            if (daysInWeek >= 4 || weeksRunning) {
                SteadyTask(taskId, list.maxBy { it.first.completedAt }.first.title, inWeek)
            } else {
                null
            }
        }
        .sortedByDescending { task -> task.lastSeven.count { it } }
        .take(3)
}
