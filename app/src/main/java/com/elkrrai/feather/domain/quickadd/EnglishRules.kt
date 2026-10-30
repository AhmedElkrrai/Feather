package com.elkrrai.feather.domain.quickadd

import com.elkrrai.feather.domain.recurrence.Frequency
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** English phrases for repeats, times and dates (see [QuickAddParser] for the overview). */
internal object EnglishRules : LanguageRules {

    // --- Vocabulary ---

    private val NUMBER_WORDS = mapOf(
        "a" to 1, "an" to 1, "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5, "six" to 6,
        "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10, "eleven" to 11, "twelve" to 12,
    )
    private const val NUM = "\\d{1,3}|a|an|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve"

    private val FULL_DAYS = mapOf(
        "monday" to DayOfWeek.MONDAY, "tuesday" to DayOfWeek.TUESDAY, "wednesday" to DayOfWeek.WEDNESDAY,
        "thursday" to DayOfWeek.THURSDAY, "friday" to DayOfWeek.FRIDAY, "saturday" to DayOfWeek.SATURDAY,
        "sunday" to DayOfWeek.SUNDAY,
    )
    private val SHORT_DAYS = mapOf(
        "mon" to DayOfWeek.MONDAY, "tues" to DayOfWeek.TUESDAY, "tue" to DayOfWeek.TUESDAY,
        "wed" to DayOfWeek.WEDNESDAY, "thurs" to DayOfWeek.THURSDAY, "thur" to DayOfWeek.THURSDAY,
        "thu" to DayOfWeek.THURSDAY, "fri" to DayOfWeek.FRIDAY, "sat" to DayOfWeek.SATURDAY,
        "sun" to DayOfWeek.SUNDAY,
    )
    private val ALL_DAYS = FULL_DAYS + SHORT_DAYS

    private val MONTHS = mapOf(
        "january" to 1, "february" to 2, "march" to 3, "april" to 4, "may" to 5, "june" to 6,
        "july" to 7, "august" to 8, "september" to 9, "october" to 10, "november" to 11, "december" to 12,
        "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "jun" to 6, "jul" to 7, "aug" to 8,
        "sept" to 9, "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12,
    )

    /** Longest words first, so "monday" is tried before "mon". */
    private fun alternation(words: Collection<String>) = words.sortedByDescending { it.length }.joinToString("|")

    private val ANY_DAY = alternation(ALL_DAYS.keys)
    private val FULL_DAY = alternation(FULL_DAYS.keys)
    private val SHORT_DAY = alternation(SHORT_DAYS.keys)
    private val ANY_MONTH = alternation(MONTHS.keys)

    private fun number(token: String): Int? = token.toIntOrNull() ?: NUMBER_WORDS[token]

    private fun unitOf(word: String): DateUnit? = when (word.removeSuffix("s")) {
        "day" -> DateUnit.DAYS
        "week" -> DateUnit.WEEKS
        "month" -> DateUnit.MONTHS
        "year" -> DateUnit.YEARS
        "minute", "min" -> DateUnit.MINUTES
        "hour", "hr" -> DateUnit.HOURS
        else -> null
    }

    private fun frequencyOf(word: String): Frequency? = unitOf(word)?.let { unitToFrequency(it) }

    private const val ON = "(?:(?:on|by|due|before|until)\\s+)?"

    // --- Repeats ---

    private val EVERY_OTHER = Regex("${WORD_START}every\\s+other\\s+(day|week|month|year)$WORD_END")
    private val EVERY_N = Regex("${WORD_START}every\\s+($NUM)\\s+(days?|weeks?|months?|years?)$WORD_END")
    private val EVERY_WORKDAYS = Regex("${WORD_START}every\\s+(weekday|weekend)s?$WORD_END")
    private val EVERY_DAYS = Regex(
        "${WORD_START}every\\s+((?:$ANY_DAY)s?(?:\\s*(?:,|and|&)?\\s*(?:$ANY_DAY)s?)*)$WORD_END"
    )
    private val EVERY_UNIT = Regex("${WORD_START}every\\s*(day|week|month|year)$WORD_END")
    private val PLURAL_DAY = Regex("$WORD_START($FULL_DAY)s$WORD_END")
    private val ADVERB = Regex("$WORD_START(daily|weekly|monthly|yearly|annually)$WORD_END")
    private val DAY_WORD = Regex(ANY_DAY)

    override fun repeat(text: QuickText): RepeatSpec? =
        text.takeFirst(EVERY_OTHER) { m -> frequencyOf(m.groupValues[1])?.let { RepeatSpec(it, interval = 2) } }
            ?: text.takeFirst(EVERY_N) { m ->
                val count = number(m.groupValues[1])
                val frequency = frequencyOf(m.groupValues[2])
                if (count == null || frequency == null) null else RepeatSpec(frequency, count.coerceAtLeast(1))
            }
            ?: text.takeFirst(EVERY_WORKDAYS) { m ->
                val days = if (m.groupValues[1] == "weekday") QuickAddParser.WORKDAYS else QuickAddParser.WEEKEND
                RepeatSpec(Frequency.WEEKLY, weekdays = days)
            }
            ?: text.takeFirst(EVERY_DAYS) { m ->
                val days = DAY_WORD.findAll(m.groupValues[1]).mapNotNull { ALL_DAYS[it.value] }.toSet()
                if (days.isEmpty()) null else RepeatSpec(Frequency.WEEKLY, weekdays = days)
            }
            ?: text.takeFirst(EVERY_UNIT) { m -> frequencyOf(m.groupValues[1])?.let { RepeatSpec(it) } }
            ?: text.takeFirst(PLURAL_DAY) { m ->
                FULL_DAYS[m.groupValues[1]]?.let { RepeatSpec(Frequency.WEEKLY, weekdays = setOf(it)) }
            }
            ?: text.takeFirst(ADVERB) { m ->
                when (m.groupValues[1]) {
                    "daily" -> RepeatSpec(Frequency.DAILY)
                    "weekly" -> RepeatSpec(Frequency.WEEKLY)
                    "monthly" -> RepeatSpec(Frequency.MONTHLY)
                    else -> RepeatSpec(Frequency.YEARLY)
                }
            }

    // --- Times ---

    private val IN_DURATION = Regex("${WORD_START}in\\s+($NUM)\\s*(minutes?|mins?|hours?|hrs?)$WORD_END")
    private val IN_HALF_HOUR = Regex("${WORD_START}in\\s+half\\s+an?\\s+hour$WORD_END")
    private val CLOCK_AMPM = Regex("$WORD_START(?:at\\s+)?(\\d{1,2})(?::(\\d{2}))?\\s*([ap])\\.?m\\.?$WORD_END")
    private val CLOCK_24 = Regex("$WORD_START(?:at\\s+)?([01]?\\d|2[0-3]):([0-5]\\d)$WORD_END")
    private val AT_HOUR = Regex("${WORD_START}at\\s+(\\d{1,2})$WORD_END")
    private val MIDNIGHT = Regex("$WORD_START(?:at\\s+)?midnight$WORD_END")
    private val NOON = Regex("$WORD_START(?:at\\s+)?(?:noon|midday)$WORD_END")
    private val TONIGHT = Regex("${WORD_START}tonight$WORD_END")
    private val PART_OF_DAY_THIS = Regex("$WORD_START(this|in\\s+the)\\s+(morning|afternoon|evening)$WORD_END")
    private val PART_OF_DAY_AFTER_DATE = Regex(
        "(?<=(?:tomorrow|today|$ANY_DAY)\\s{1,3})(morning|afternoon|evening)$WORD_END"
    )

    private val MORNING = LocalTime.of(9, 0)
    private val AFTERNOON = LocalTime.of(15, 0)
    private val EVENING = LocalTime.of(18, 0)
    private val TONIGHT_TIME = LocalTime.of(20, 0)

    private fun partOfDay(word: String): LocalTime = when (word) {
        "morning" -> MORNING
        "afternoon" -> AFTERNOON
        else -> EVENING
    }

    private fun relative(now: LocalDateTime, amount: Int?, unit: DateUnit?): TimeSpec? {
        if (amount == null || unit == null) return null
        val target = now.plusUnit(amount.toLong(), unit) ?: return null
        return TimeSpec(target.toLocalTime().withSecond(0).withNano(0), target.toLocalDate())
    }

    override fun time(text: QuickText, now: LocalDateTime): TimeSpec? =
        text.takeFirst(IN_DURATION) { m -> relative(now, number(m.groupValues[1]), unitOf(m.groupValues[2])) }
            ?: text.takeFirst(IN_HALF_HOUR) { relative(now, 30, DateUnit.MINUTES) }
            ?: text.takeFirst(CLOCK_AMPM) { m ->
                val hour = m.groupValues[1].toInt()
                val minute = m.groupValues[2].ifEmpty { "0" }.toInt()
                twelveHourTime(hour, minute, pm = m.groupValues[3] == "p")?.let { TimeSpec(it) }
            }
            ?: text.takeFirst(CLOCK_24) { m ->
                TimeSpec(LocalTime.of(m.groupValues[1].toInt(), m.groupValues[2].toInt()))
            }
            ?: text.takeFirst(AT_HOUR) { m -> guessedHour(m.groupValues[1].toInt())?.let { TimeSpec(it) } }
            ?: text.takeFirst(MIDNIGHT) { TimeSpec(LocalTime.MIDNIGHT) }
            ?: text.takeFirst(NOON) { TimeSpec(LocalTime.NOON) }
            ?: text.takeFirst(TONIGHT) { TimeSpec(TONIGHT_TIME, now.toLocalDate()) }
            ?: text.takeFirst(PART_OF_DAY_THIS) { m ->
                val date = if (m.groupValues[1] == "this") now.toLocalDate() else null
                TimeSpec(partOfDay(m.groupValues[2]), date)
            }
            ?: text.takeFirst(PART_OF_DAY_AFTER_DATE) { m -> TimeSpec(partOfDay(m.groupValues[1])) }

    // --- Dates ---

    private val MONTH_DAY = Regex(
        "$WORD_START$ON($ANY_MONTH)\\.?\\s+(\\d{1,2})(?:st|nd|rd|th)?(?:,?\\s+(\\d{4}))?$WORD_END"
    )
    private val DAY_MONTH = Regex(
        "$WORD_START$ON(?:the\\s+)?(\\d{1,2})(?:st|nd|rd|th)?\\s+(?:of\\s+)?($ANY_MONTH)\\.?(?:,?\\s+(\\d{4}))?$WORD_END"
    )
    private val DAY_AFTER_TOMORROW = Regex("$WORD_START${ON}day\\s+after\\s+tomorrow$WORD_END")
    private val TOMORROW = Regex("$WORD_START$ON(?:tomorrow|tmrw|tmr)$WORD_END")
    private val TODAY = Regex("$WORD_START${ON}today$WORD_END")
    private val IN_UNITS = Regex("${WORD_START}in\\s+($NUM)\\s+(days?|weeks?|months?|years?)$WORD_END")
    private val NEXT_UNIT = Regex("${WORD_START}next\\s+(week|month)$WORD_END")
    private val WEEKEND = Regex("$WORD_START(?:(?:on|this)\\s+)?(?:the\\s+)?weekend$WORD_END")
    private val WEEKDAY_FULL = Regex("$WORD_START$ON(?:(next|this)\\s+)?($FULL_DAY)$WORD_END")
    private val WEEKDAY_SHORT = Regex("$WORD_START(on|by|due|before|until|next|this)\\s+($SHORT_DAY)\\.?$WORD_END")
    private val THE_NTH = Regex("$WORD_START${ON}the\\s+(\\d{1,2})(?:st|nd|rd|th)$WORD_END")
    private val ON_NTH = Regex("$WORD_START(?:on|by|due|before)\\s+(\\d{1,2})(?:st|nd|rd|th)$WORD_END")

    override fun date(text: QuickText, today: LocalDate): LocalDate? =
        text.takeFirst(MONTH_DAY) { m ->
            MONTHS[m.groupValues[1]]?.let { monthAndDay(today, it, m.groupValues[2].toInt(), m.groupValues[3].toIntOrNull()) }
        }
            ?: text.takeFirst(DAY_MONTH) { m ->
                MONTHS[m.groupValues[2]]?.let { monthAndDay(today, it, m.groupValues[1].toInt(), m.groupValues[3].toIntOrNull()) }
            }
            ?: text.takeFirst(DAY_AFTER_TOMORROW) { today.plusDays(2) }
            ?: text.takeFirst(TOMORROW) { today.plusDays(1) }
            ?: text.takeFirst(TODAY) { today }
            ?: text.takeFirst(IN_UNITS) { m ->
                val amount = number(m.groupValues[1])
                val unit = unitOf(m.groupValues[2])
                if (amount == null || unit == null) null else today.plusUnit(amount.toLong(), unit)
            }
            ?: text.takeFirst(NEXT_UNIT) { m ->
                if (m.groupValues[1] == "week") startOfNextWeek(today) else firstOfNextMonth(today)
            }
            ?: text.takeFirst(WEEKEND) { upcomingWeekend(today) }
            ?: text.takeFirst(WEEKDAY_FULL) { m ->
                FULL_DAYS[m.groupValues[2]]?.let { nextWeekday(today, it, following = m.groupValues[1] == "next") }
            }
            ?: text.takeFirst(WEEKDAY_SHORT) { m ->
                SHORT_DAYS[m.groupValues[2]]?.let { nextWeekday(today, it, following = m.groupValues[1] == "next") }
            }
            ?: text.takeFirst(THE_NTH) { m -> nextDayOfMonth(today, m.groupValues[1].toInt()) }
            ?: text.takeFirst(ON_NTH) { m -> nextDayOfMonth(today, m.groupValues[1].toInt()) }
}
