package com.elkrrai.feather.domain.recurrence

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class Frequency(val token: String) {
    DAILY("DAILY"),
    WEEKLY("WEEKLY"),
    MONTHLY("MONTHLY"),
    YEARLY("YEARLY");

    companion object {
        fun fromToken(token: String): Frequency? = entries.firstOrNull { it.token == token }
    }
}

/** How the next date of a repeating task is worked out when it is finished. */
enum class RepeatMode(val code: Int) {
    /** Keep to the calendar: the next occurrence of the rule after the due date. */
    FIXED(0),

    /** Count from the day the task was finished: that day plus the interval. */
    AFTER_COMPLETION(1);

    companion object {
        fun fromCode(code: Int): RepeatMode = entries.firstOrNull { it.code == code } ?: FIXED
    }
}

/**
 * "The [ordinal]th [day] of the month": 1 to 4, or -1 for the last one. (A fifth weekday does
 * not exist in every month, so the last one is offered instead.)
 */
data class NthWeekday(val ordinal: Int, val day: DayOfWeek) {
    init {
        require(ordinal in 1..4 || ordinal == -1) { "Ordinal must be 1..4 or -1" }
    }

    companion object {
        /** The pattern that [date] itself follows, e.g. 13 Oct 2026 is the 2nd Tuesday. */
        fun of(date: LocalDate): NthWeekday {
            val ordinal = (date.dayOfMonth - 1) / 7 + 1
            return NthWeekday(if (ordinal > 4) -1 else ordinal, date.dayOfWeek)
        }
    }
}

/**
 * How a task repeats. Pure data: [nextOccurrence] and [afterAdvance] do the arithmetic and
 * [toRule] / [Recurrence.parse] convert to and from the RFC 5545 style string kept in the
 * database.
 *
 * Which fields matter depends on [frequency] (and are ignored when [mode] is
 * [RepeatMode.AFTER_COMPLETION], which only uses [frequency] and [interval]):
 * - WEEKLY: [weekdays]; empty means "the weekday of the due date".
 * - MONTHLY: either [nthWeekday], or [monthDay] (1..31; days past the end of a short month use
 *   its last day, so 31 means "the last day"); neither set means "the day of the due date".
 * - YEARLY: [month] and [monthDay]; unset means "the due date's".
 *
 * [until] is the last day an occurrence may fall on. [count] is the number of occurrences still
 * to come, including the current one; it goes down each time the task is finished.
 */
data class Recurrence(
    val frequency: Frequency,
    val interval: Int = 1,
    val weekdays: Set<DayOfWeek> = emptySet(),
    val monthDay: Int? = null,
    val nthWeekday: NthWeekday? = null,
    val month: Int? = null,
    val until: LocalDate? = null,
    val count: Int? = null,
    val mode: RepeatMode = RepeatMode.FIXED,
) {
    init {
        require(interval >= 1) { "Interval must be at least 1" }
        require(count == null || count >= 1) { "Count must be at least 1" }
        require(monthDay == null || monthDay in 1..31) { "Month day must be 1..31" }
        require(month == null || month in 1..12) { "Month must be 1..12" }
    }

    /** The RFC 5545 style rule (without the mode, which is stored separately). */
    fun toRule(): String {
        val parts = mutableListOf("FREQ=${frequency.token}")
        if (interval != 1) parts += "INTERVAL=$interval"
        when (frequency) {
            Frequency.DAILY -> Unit
            Frequency.WEEKLY -> if (weekdays.isNotEmpty()) {
                parts += "BYDAY=" + weekdays.sortedBy { it.value }.joinToString(",") { it.token() }
            }
            Frequency.MONTHLY -> if (nthWeekday != null) {
                parts += "BYDAY=${nthWeekday.ordinal}${nthWeekday.day.token()}"
            } else if (monthDay != null) {
                parts += "BYMONTHDAY=$monthDay"
            }
            Frequency.YEARLY -> {
                if (month != null) parts += "BYMONTH=$month"
                if (monthDay != null) parts += "BYMONTHDAY=$monthDay"
            }
        }
        if (until != null) parts += "UNTIL=${until.format(DateTimeFormatter.BASIC_ISO_DATE)}"
        if (count != null) parts += "COUNT=$count"
        return parts.joinToString(";")
    }

    companion object {
        /**
         * A rule for [frequency] that repeats the way [reference] (normally the due date)
         * falls: its weekday, day of the month, or month and day.
         */
        fun starting(frequency: Frequency, reference: LocalDate, mode: RepeatMode = RepeatMode.FIXED): Recurrence =
            when (frequency) {
                Frequency.DAILY -> Recurrence(frequency, mode = mode)
                Frequency.WEEKLY -> Recurrence(frequency, weekdays = setOf(reference.dayOfWeek), mode = mode)
                Frequency.MONTHLY -> Recurrence(frequency, monthDay = reference.dayOfMonth, mode = mode)
                Frequency.YEARLY -> Recurrence(
                    frequency,
                    month = reference.monthValue,
                    monthDay = reference.dayOfMonth,
                    mode = mode,
                )
            }

        /** Reads a stored rule; null if it is missing or not one this app understands. */
        fun parse(rule: String?, mode: RepeatMode = RepeatMode.FIXED): Recurrence? {
            if (rule.isNullOrBlank()) return null
            return try {
                parseOrThrow(rule, mode)
            } catch (e: RuntimeException) {
                null
            }
        }

        private fun parseOrThrow(rule: String, mode: RepeatMode): Recurrence {
            val fields = rule.split(';').associate { part ->
                val pair = part.split('=', limit = 2)
                require(pair.size == 2) { "Bad part: $part" }
                pair[0].trim().uppercase() to pair[1].trim()
            }
            val frequency = requireNotNull(Frequency.fromToken(fields["FREQ"].orEmpty().uppercase())) { "No FREQ" }
            val byDay = fields["BYDAY"]

            var weekdays = emptySet<DayOfWeek>()
            var nth: NthWeekday? = null
            when (frequency) {
                Frequency.WEEKLY -> if (byDay != null) {
                    weekdays = byDay.split(',').map { dayFromToken(it.trim().uppercase()) }.toSet()
                }
                Frequency.MONTHLY -> if (byDay != null) {
                    val match = requireNotNull(NTH_DAY.matchEntire(byDay.trim().uppercase())) { "Bad BYDAY" }
                    nth = NthWeekday(match.groupValues[1].toInt(), dayFromToken(match.groupValues[2]))
                }
                else -> Unit
            }

            return Recurrence(
                frequency = frequency,
                interval = fields["INTERVAL"]?.toInt() ?: 1,
                weekdays = weekdays,
                monthDay = fields["BYMONTHDAY"]?.toInt(),
                nthWeekday = nth,
                month = fields["BYMONTH"]?.toInt(),
                until = fields["UNTIL"]?.let { LocalDate.parse(it.take(8), DateTimeFormatter.BASIC_ISO_DATE) },
                count = fields["COUNT"]?.toInt(),
                mode = mode,
            )
        }

        private val NTH_DAY = Regex("^(-?[1-9])([A-Z]{2})$")
    }
}

private val DAY_TOKENS = listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU")

private fun DayOfWeek.token(): String = DAY_TOKENS[value - 1]

private fun dayFromToken(token: String): DayOfWeek {
    val index = DAY_TOKENS.indexOf(token)
    require(index >= 0) { "Bad day: $token" }
    return DayOfWeek.of(index + 1)
}
