package com.elkrrai.feather.domain.quickadd

import com.elkrrai.feather.domain.recurrence.Frequency
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Standard Arabic phrases for repeats, times and dates (no dialect words). Every word here is
 * written in its normalized spelling (see [QuickText]): alef without hamza or madda, ha instead
 * of ta marbuta, ya instead of alef maqsura, and no diacritics. An attached "و" (and) in front of
 * a phrase is accepted.
 */
internal object ArabicRules : LanguageRules {

    // --- Vocabulary ---

    private val DAYS = mapOf(
        "احد" to DayOfWeek.SUNDAY,
        "اثنين" to DayOfWeek.MONDAY,
        "ثلاثاء" to DayOfWeek.TUESDAY,
        "اربعاء" to DayOfWeek.WEDNESDAY,
        "خميس" to DayOfWeek.THURSDAY,
        "جمعه" to DayOfWeek.FRIDAY,
        "سبت" to DayOfWeek.SATURDAY,
    )
    private val ANY_DAY = DAYS.keys.joinToString("|")

    private val MONTHS = mapOf(
        "يناير" to 1, "فبراير" to 2, "مارس" to 3, "ابريل" to 4, "مايو" to 5, "يونيو" to 6, "يونيه" to 6,
        "يوليو" to 7, "يوليه" to 7, "اغسطس" to 8, "سبتمبر" to 9, "اكتوبر" to 10, "نوفمبر" to 11, "ديسمبر" to 12,
    )
    private val ANY_MONTH = MONTHS.keys.joinToString("|")

    private val SINGULAR = setOf("يوم", "اسبوع", "شهر", "سنه", "عام", "ساعه", "دقيقه")
    private val DUAL = setOf("يومين", "اسبوعين", "شهرين", "سنتين", "ساعتين", "دقيقتين")

    private fun unitOf(word: String): DateUnit? = when (word) {
        "يوم", "ايام", "يومين" -> DateUnit.DAYS
        "اسبوع", "اسابيع", "اسبوعين" -> DateUnit.WEEKS
        "شهر", "اشهر", "شهور", "شهرين" -> DateUnit.MONTHS
        "سنه", "سنوات", "سنتين", "عام", "اعوام" -> DateUnit.YEARS
        "ساعه", "ساعات", "ساعتين" -> DateUnit.HOURS
        "دقيقه", "دقائق", "دقايق", "دقيقتين" -> DateUnit.MINUTES
        else -> null
    }

    /** "3" -> 3; no number: a singular noun means 1 and a dual means 2 ("بعد يومين"); a plural alone means nothing. */
    private fun amountOf(digits: String, word: String): Int? = digits.toIntOrNull() ?: when (word) {
        in DUAL -> 2
        in SINGULAR -> 1
        else -> null
    }

    private fun frequencyOf(word: String): Frequency? = unitOf(word)?.let { unitToFrequency(it) }

    private const val UNIT_WORDS = "يومين|اسبوعين|شهرين|سنتين|يوم|ايام|اسبوع|اسابيع|شهر|اشهر|شهور|سنه|سنوات|عام|اعوام"
    private const val NEXT_WORD = "القادم|المقبل|القادمه|المقبله"
    private const val S = "$WORD_START(?:و)?"
    private const val PREP = "(?:(?:في|الي|حتي|قبل|بحلول)\\s+)?"
    private val DAY_REF = "(?:ال)?(?:$ANY_DAY)"

    // --- Repeats ---

    private val EVERY_DAYS = Regex("${S}كل\\s+(?:يوم\\s+)?($DAY_REF(?:\\s*(?:,|و)?\\s*$DAY_REF)*)$WORD_END")
    private val EVERY_DUAL = Regex("${S}كل\\s+(يومين|اسبوعين|شهرين|سنتين)$WORD_END")
    private val EVERY_N = Regex("${S}كل\\s+(\\d{1,3})\\s*($UNIT_WORDS)$WORD_END")
    private val EVERY_UNIT = Regex("${S}كل\\s+(يوم|اسبوع|شهر|سنه|عام)$WORD_END")
    private val ADVERB = Regex("$S(يوميا|اسبوعيا|شهريا|سنويا)$WORD_END")
    private val DAY_WORD = Regex(ANY_DAY)

    override fun repeat(text: QuickText): RepeatSpec? =
        text.takeFirst(EVERY_DAYS) { m ->
            val days = DAY_WORD.findAll(m.groupValues[1]).mapNotNull { DAYS[it.value] }.toSet()
            if (days.isEmpty()) null else RepeatSpec(Frequency.WEEKLY, weekdays = days)
        }
            ?: text.takeFirst(EVERY_DUAL) { m -> frequencyOf(m.groupValues[1])?.let { RepeatSpec(it, interval = 2) } }
            ?: text.takeFirst(EVERY_N) { m ->
                val count = m.groupValues[1].toIntOrNull()
                val frequency = frequencyOf(m.groupValues[2])
                if (count == null || frequency == null) null else RepeatSpec(frequency, count.coerceAtLeast(1))
            }
            ?: text.takeFirst(EVERY_UNIT) { m -> frequencyOf(m.groupValues[1])?.let { RepeatSpec(it) } }
            ?: text.takeFirst(ADVERB) { m ->
                when (m.groupValues[1]) {
                    "يوميا" -> RepeatSpec(Frequency.DAILY)
                    "اسبوعيا" -> RepeatSpec(Frequency.WEEKLY)
                    "شهريا" -> RepeatSpec(Frequency.MONTHLY)
                    else -> RepeatSpec(Frequency.YEARLY)
                }
            }

    // --- Times ---

    private const val PERIOD = "صباحا|صباح|مساء|مساءا|ظهرا|ظهر|عصرا|عصر|ليلا|ليل|فجرا|فجر"

    private val IN_DURATION = Regex("${S}بعد\\s+(?:(\\d{1,3})\\s*)?(ساعتين|دقيقتين|ساعه|ساعات|دقيقه|دقائق|دقايق)$WORD_END")
    private val IN_HALF_HOUR = Regex("${S}بعد\\s+نصف\\s+ساعه$WORD_END")
    private val CLOCK_WORD = Regex(
        "$S(?:في\\s+)?(?:ال)?ساعه\\s+(\\d{1,2})(?::(\\d{2}))?" +
            "(?:\\s*(?:و\\s*(نصف|النصف|ربع|الربع)|الا\\s+(ربع|الربع)))?" +
            "(?:\\s*(?:في\\s+)?(?:ال)?($PERIOD))?$WORD_END"
    )
    private val CLOCK_PERIOD = Regex("$S(\\d{1,2})(?::(\\d{2}))?\\s*(?:في\\s+)?(?:ال)?($PERIOD)$WORD_END")
    private val CLOCK_24 = Regex("$S(?:(?:ال)?ساعه\\s+)?([01]?\\d|2[0-3]):([0-5]\\d)$WORD_END")
    private val MIDNIGHT = Regex("${S}منتصف\\s+الليل$WORD_END")
    private val TONIGHT = Regex("${S}الليله$WORD_END")
    private val MORNING = Regex("$S(?:صباحا|الصباح)$WORD_END")
    private val NOON = Regex("$S(?:ظهرا|الظهر)$WORD_END")
    private val AFTERNOON = Regex("$S(?:عصرا|العصر)$WORD_END")
    private val EVENING = Regex("$S(?:مساء|مساءا|المساء)$WORD_END")
    private val NIGHT = Regex("${S}ليلا$WORD_END")

    private val MORNING_TIME = LocalTime.of(9, 0)
    private val AFTERNOON_TIME = LocalTime.of(16, 0)
    private val EVENING_TIME = LocalTime.of(18, 0)
    private val NIGHT_TIME = LocalTime.of(21, 0)
    private val TONIGHT_TIME = LocalTime.of(20, 0)

    /** The 24-hour hour for [hour] read with a day [period] ("5 مساء" is 17), or null if impossible. */
    private fun hourWithPeriod(hour: Int, period: String?): Int? {
        if (hour !in 0..23) return null
        return when {
            period == null -> guessedHour(hour)?.hour
            period.startsWith("صباح") || period.startsWith("فجر") -> if (hour == 12) 0 else hour
            period.startsWith("ظهر") -> if (hour in 1..5) hour + 12 else hour
            period.startsWith("عصر") || period.startsWith("مساء") -> if (hour in 1..11) hour + 12 else hour
            period.startsWith("ليل") -> if (hour in 6..11) hour + 12 else if (hour == 12) 0 else hour
            else -> null
        }
    }

    private fun clock(hour: Int, minute: Int, period: String?, quarterTo: Boolean = false): LocalTime? {
        if (minute !in 0..59) return null
        val base = hourWithPeriod(hour, period) ?: return null
        return if (quarterTo) LocalTime.of((base + 23) % 24, 45) else LocalTime.of(base, minute)
    }

    private fun relative(now: LocalDateTime, amount: Int?, unit: DateUnit?): TimeSpec? {
        if (amount == null || unit == null) return null
        val target = now.plusUnit(amount.toLong(), unit) ?: return null
        return TimeSpec(target.toLocalTime().withSecond(0).withNano(0), target.toLocalDate())
    }

    override fun time(text: QuickText, now: LocalDateTime): TimeSpec? =
        text.takeFirst(IN_DURATION) { m ->
            relative(now, amountOf(m.groupValues[1], m.groupValues[2]), unitOf(m.groupValues[2]))
        }
            ?: text.takeFirst(IN_HALF_HOUR) { relative(now, 30, DateUnit.MINUTES) }
            ?: text.takeFirst(CLOCK_WORD) { m ->
                val hour = m.groupValues[1].toInt()
                val typedMinutes = m.groupValues[2].toIntOrNull()
                val fraction = m.groupValues[3]
                val period = m.groupValues[5].ifEmpty { null }
                val minute = when {
                    typedMinutes != null -> typedMinutes
                    fraction.endsWith("نصف") -> 30
                    fraction.endsWith("ربع") -> 15
                    else -> 0
                }
                val time = when {
                    m.groupValues[4].isNotEmpty() -> clock(hour, 0, period, quarterTo = true)
                    // "الساعة 5:30" with no period is read as a 24-hour time.
                    period == null && typedMinutes != null -> validTime(hour, minute)
                    else -> clock(hour, minute, period)
                }
                time?.let { TimeSpec(it) }
            }
            ?: text.takeFirst(CLOCK_PERIOD) { m ->
                val minute = m.groupValues[2].toIntOrNull() ?: 0
                clock(m.groupValues[1].toInt(), minute, m.groupValues[3])?.let { TimeSpec(it) }
            }
            ?: text.takeFirst(CLOCK_24) { m ->
                TimeSpec(LocalTime.of(m.groupValues[1].toInt(), m.groupValues[2].toInt()))
            }
            ?: text.takeFirst(MIDNIGHT) { TimeSpec(LocalTime.MIDNIGHT) }
            ?: text.takeFirst(TONIGHT) { TimeSpec(TONIGHT_TIME, now.toLocalDate()) }
            ?: text.takeFirst(MORNING) { TimeSpec(MORNING_TIME) }
            ?: text.takeFirst(NOON) { TimeSpec(LocalTime.NOON) }
            ?: text.takeFirst(AFTERNOON) { TimeSpec(AFTERNOON_TIME) }
            ?: text.takeFirst(EVENING) { TimeSpec(EVENING_TIME) }
            ?: text.takeFirst(NIGHT) { TimeSpec(NIGHT_TIME) }

    private fun validTime(hour: Int, minute: Int): LocalTime? =
        if (hour in 0..23 && minute in 0..59) LocalTime.of(hour, minute) else null

    // --- Dates ---

    private val AFTER_TOMORROW = Regex("${S}بعد\\s+(?:ال)?غدا?$WORD_END")
    private val TOMORROW = Regex("$S$PREP(?:ال)?غدا?$WORD_END")
    private val TODAY = Regex("$S${PREP}اليوم$WORD_END")
    private val IN_UNITS = Regex("${S}بعد\\s+(?:(\\d{1,3})\\s*)?($UNIT_WORDS)$WORD_END")
    private val NEXT_UNIT = Regex("$S(?:ال)?(اسبوع|شهر)\\s+($NEXT_WORD)$WORD_END")
    private val WEEKDAY = Regex("$S$PREP(?:يوم\\s+(?:ال)?|ال)($ANY_DAY)(?:\\s+($NEXT_WORD))?$WORD_END")
    private val DAY_MONTH = Regex(
        "$S(?:في\\s+)?(?:يوم\\s+)?(\\d{1,2})\\s+(?:من\\s+)?(?:شهر\\s+)?($ANY_MONTH)(?:\\s+(\\d{4}))?$WORD_END"
    )
    private val MONTH_DAY = Regex(
        "$S(?:في\\s+)?(?:شهر\\s+)?($ANY_MONTH)\\s+(\\d{1,2})(?:\\s+(\\d{4}))?$WORD_END"
    )

    override fun date(text: QuickText, today: LocalDate): LocalDate? =
        text.takeFirst(DAY_MONTH) { m ->
            MONTHS[m.groupValues[2]]?.let { monthAndDay(today, it, m.groupValues[1].toInt(), m.groupValues[3].toIntOrNull()) }
        }
            ?: text.takeFirst(MONTH_DAY) { m ->
                MONTHS[m.groupValues[1]]?.let { monthAndDay(today, it, m.groupValues[2].toInt(), m.groupValues[3].toIntOrNull()) }
            }
            ?: text.takeFirst(AFTER_TOMORROW) { today.plusDays(2) }
            ?: text.takeFirst(TOMORROW) { today.plusDays(1) }
            ?: text.takeFirst(TODAY) { today }
            ?: text.takeFirst(NEXT_UNIT) { m ->
                if (m.groupValues[1] == "اسبوع") nextMonday(today) else firstOfNextMonth(today)
            }
            ?: text.takeFirst(IN_UNITS) { m ->
                val amount = amountOf(m.groupValues[1], m.groupValues[2])
                val unit = unitOf(m.groupValues[2])
                if (amount == null || unit == null) null else today.plusUnit(amount.toLong(), unit)
            }
            ?: text.takeFirst(WEEKDAY) { m ->
                DAYS[m.groupValues[1]]?.let { nextWeekday(today, it, following = m.groupValues[2].isNotEmpty()) }
            }
}
