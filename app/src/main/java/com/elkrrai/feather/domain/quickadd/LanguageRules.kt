package com.elkrrai.feather.domain.quickadd

import com.elkrrai.feather.domain.recurrence.Frequency
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import com.elkrrai.feather.domain.time.startOfNextWeek
import java.time.temporal.TemporalAdjusters

/** "every 2 weeks on monday and friday": the pieces of a repeat phrase. */
internal data class RepeatSpec(
    val frequency: Frequency,
    val interval: Int = 1,
    val weekdays: Set<DayOfWeek> = emptySet(),
)

/** A time of day; [date] is set when the phrase also fixes the day ("tonight", "in 2 hours"). */
internal data class TimeSpec(val time: LocalTime, val date: LocalDate? = null)

/**
 * The phrases of one language. Each function finds the first phrase it knows in [QuickText],
 * consumes it and returns what it means, or returns null and leaves the text alone.
 */
internal interface LanguageRules {
    fun repeat(text: QuickText): RepeatSpec?

    fun time(text: QuickText, now: LocalDateTime): TimeSpec?

    fun date(text: QuickText, today: LocalDate): LocalDate?
}

/** Start and end of a word, for any script (`\b` does not behave well with Arabic). */
internal const val WORD_START = "(?<![\\p{L}\\p{N}])"
internal const val WORD_END = "(?![\\p{L}\\p{N}])"

/**
 * Tries every match of [regex] in order and consumes the first one that [build] accepts
 * (returns non-null for), so a pattern that matches something invalid, like "feb 30", does not
 * hide a later valid one.
 */
internal inline fun <T : Any> QuickText.takeFirst(regex: Regex, build: (MatchResult) -> T?): T? {
    for (match in regex.findAll(text)) {
        val value = build(match) ?: continue
        consume(match.range)
        return value
    }
    return null
}

internal enum class DateUnit { MINUTES, HOURS, DAYS, WEEKS, MONTHS, YEARS }

internal fun LocalDate.plusUnit(amount: Long, unit: DateUnit): LocalDate? = when (unit) {
    DateUnit.DAYS -> plusDays(amount)
    DateUnit.WEEKS -> plusWeeks(amount)
    DateUnit.MONTHS -> plusMonths(amount)
    DateUnit.YEARS -> plusYears(amount)
    DateUnit.MINUTES, DateUnit.HOURS -> null
}

internal fun LocalDateTime.plusUnit(amount: Long, unit: DateUnit): LocalDateTime? = when (unit) {
    DateUnit.MINUTES -> plusMinutes(amount)
    DateUnit.HOURS -> plusHours(amount)
    else -> null
}

/**
 * The next [day] after [today] (never today itself). With [following], "next friday": if that
 * day is still in the current week (weeks start on Saturday), the one a week later.
 */
internal fun nextWeekday(today: LocalDate, day: DayOfWeek, following: Boolean): LocalDate {
    var date = today.plusDays(1)
    while (date.dayOfWeek != day) date = date.plusDays(1)
    val nextWeekStart = today.startOfNextWeek()
    return if (following && date.isBefore(nextWeekStart)) date.plusDays(DAYS_PER_WEEK) else date
}

/** "Next week": the first day of the week that comes after this one. */
internal fun startOfNextWeek(today: LocalDate): LocalDate = today.startOfNextWeek()

internal fun firstOfNextMonth(today: LocalDate): LocalDate = today.with(TemporalAdjusters.firstDayOfNextMonth())

/** The coming Saturday, or today when it already is the weekend. */
internal fun upcomingWeekend(today: LocalDate): LocalDate =
    if (today.dayOfWeek == DayOfWeek.SUNDAY) today else today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))

/**
 * [day] of [month]; with no [year], the next time that date comes round (this year, or next
 * year when it has passed). Null if the date does not exist ("30 February").
 */
internal fun monthAndDay(today: LocalDate, month: Int, day: Int, year: Int?): LocalDate? {
    if (month !in 1..12 || day < 1) return null
    val yearMonth = YearMonth.of(year ?: today.year, month)
    if (day > yearMonth.lengthOfMonth()) return null
    val date = yearMonth.atDay(day)
    if (year != null || !date.isBefore(today)) return date
    val nextYear = YearMonth.of(today.year + 1, month)
    return if (day > nextYear.lengthOfMonth()) null else nextYear.atDay(day)
}

/** "the 15th": this month if the 15th is still ahead, otherwise next month (short months clamp). */
internal fun nextDayOfMonth(today: LocalDate, day: Int): LocalDate? {
    if (day !in 1..31) return null
    val thisMonth = YearMonth.from(today)
    val inThisMonth = thisMonth.atDay(minOf(day, thisMonth.lengthOfMonth()))
    if (inThisMonth.isAfter(today)) return inThisMonth
    val nextMonth = thisMonth.plusMonths(1)
    return nextMonth.atDay(minOf(day, nextMonth.lengthOfMonth()))
}

/** A clock time from a 12-hour reading ([hour] 1..12), or null if it is not a real time. */
internal fun twelveHourTime(hour: Int, minute: Int, pm: Boolean): LocalTime? {
    if (hour !in 1..12 || minute !in 0..59) return null
    return LocalTime.of(hour % 12 + if (pm) 12 else 0, minute)
}

/**
 * "at 5" with no am or pm: 1 to 6 is taken as afternoon, 7 to 11 as morning, 12 as noon, and 0
 * or 13 to 23 as they are.
 */
internal fun guessedHour(hour: Int): LocalTime? = when (hour) {
    in 1..6 -> LocalTime.of(hour + 12, 0)
    in 7..12 -> LocalTime.of(hour, 0)
    0, in 13..23 -> LocalTime.of(hour, 0)
    else -> null
}

internal fun unitToFrequency(unit: DateUnit): Frequency? = when (unit) {
    DateUnit.DAYS -> Frequency.DAILY
    DateUnit.WEEKS -> Frequency.WEEKLY
    DateUnit.MONTHS -> Frequency.MONTHLY
    DateUnit.YEARS -> Frequency.YEARLY
    DateUnit.MINUTES, DateUnit.HOURS -> null
}

private const val DAYS_PER_WEEK = 7L
