package com.elkrrai.feather.ui.format

import android.content.Context
import android.text.format.DateFormat
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.recurrence.Frequency
import com.elkrrai.feather.domain.recurrence.NthWeekday
import com.elkrrai.feather.domain.recurrence.Recurrence
import com.elkrrai.feather.domain.recurrence.RepeatMode
import com.elkrrai.feather.ui.firstDayOfWeek
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/** Day 31 in a monthly rule means "the last day of the month" (see [Recurrence]). */
const val LAST_DAY_OF_MONTH = 31

/**
 * A one-line summary in the app language, for example "Every 2 weeks · Mon, Wed · Until
 * Dec 31, 2026". [reference] (normally the due date) fills in what the rule leaves open, such as
 * the month and day of a yearly rule.
 */
@Composable
fun Recurrence.label(reference: LocalDate? = null): String {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    return remember(this, reference, locale) { describe(context, locale, reference) }
}

fun Recurrence.describe(context: Context, locale: Locale, reference: LocalDate? = null): String {
    val parts = mutableListOf(context.resources.getQuantityString(frequency.everyPlural(), interval, interval))

    if (mode == RepeatMode.AFTER_COMPLETION) {
        parts += context.getString(R.string.repeat_after_completion_summary)
    } else {
        detail(context, locale, reference)?.let { parts += it }
    }
    if (until != null) {
        val date = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(until)
        parts += context.getString(R.string.repeat_until, date)
    }
    if (count != null) {
        parts += context.resources.getQuantityString(R.plurals.repeat_times_left, count, count)
    }
    return parts.joinToString(" · ")
}

private fun Recurrence.detail(context: Context, locale: Locale, reference: LocalDate?): String? =
    when (frequency) {
        Frequency.DAILY -> null
        Frequency.WEEKLY -> if (weekdays.isEmpty()) null else weekdayList(context, locale, weekdays)
        Frequency.MONTHLY -> when {
            nthWeekday != null -> nthWeekday.label(context, locale)
            monthDay == LAST_DAY_OF_MONTH -> context.getString(R.string.repeat_monthly_last_day)
            monthDay != null -> context.getString(R.string.repeat_monthly_on_day, monthDay)
            else -> null
        }
        Frequency.YEARLY -> {
            val targetMonth = month ?: reference?.monthValue
            val targetDay = monthDay ?: reference?.dayOfMonth
            if (targetMonth == null || targetDay == null) {
                null
            } else {
                context.getString(R.string.repeat_yearly_on, monthAndDay(locale, targetMonth, targetDay))
            }
        }
    }

/** "Mon, Wed", starting the week on the chosen first day. */
fun weekdayList(context: Context, locale: Locale, days: Set<DayOfWeek>): String {
    val separator = context.getString(R.string.repeat_list_separator)
    return days
        .sortedBy { orderInWeek(it, locale) }
        .joinToString(separator) { it.getDisplayName(TextStyle.SHORT, locale) }
}

/** Position of [day] in a week that starts on the chosen first day of the week (0 to 6). */
fun orderInWeek(day: DayOfWeek, locale: Locale): Int =
    Math.floorMod(day.value - firstDayOfWeek(locale).value, 7)

/** "the 2nd Tuesday" or "the last Friday", in the word order of the language. */
fun NthWeekday.label(context: Context, locale: Locale): String =
    context.getString(
        R.string.repeat_monthly_nth,
        context.getString(ordinal.ordinalRes()),
        day.getDisplayName(TextStyle.FULL, locale),
    )

/** "Oct 14": the month and day without a year, ordered the way the locale writes dates. */
fun monthAndDay(locale: Locale, month: Int, day: Int): String {
    val yearMonth = YearMonth.of(LEAP_YEAR, month)
    val date = yearMonth.atDay(minOf(day, yearMonth.lengthOfMonth()))
    val pattern = DateFormat.getBestDateTimePattern(locale, "MMMd")
    return DateTimeFormatter.ofPattern(pattern, locale).format(date)
}

/** Any leap year, so that 29 February can be shown. */
private const val LEAP_YEAR = 2024

@StringRes
private fun Int.ordinalRes(): Int = when (this) {
    1 -> R.string.ordinal_first
    2 -> R.string.ordinal_second
    3 -> R.string.ordinal_third
    4 -> R.string.ordinal_fourth
    else -> R.string.ordinal_last
}

@PluralsRes
internal fun Frequency.everyPlural(): Int = when (this) {
    Frequency.DAILY -> R.plurals.repeat_every_days
    Frequency.WEEKLY -> R.plurals.repeat_every_weeks
    Frequency.MONTHLY -> R.plurals.repeat_every_months
    Frequency.YEARLY -> R.plurals.repeat_every_years
}

/** The bare unit ("day", "days") for the interval field of the repeat dialog. */
@PluralsRes
internal fun Frequency.unitPlural(): Int = when (this) {
    Frequency.DAILY -> R.plurals.repeat_unit_days
    Frequency.WEEKLY -> R.plurals.repeat_unit_weeks
    Frequency.MONTHLY -> R.plurals.repeat_unit_months
    Frequency.YEARLY -> R.plurals.repeat_unit_years
}

@StringRes
internal fun Frequency.chipLabel(): Int = when (this) {
    Frequency.DAILY -> R.string.repeat_freq_daily
    Frequency.WEEKLY -> R.string.repeat_freq_weekly
    Frequency.MONTHLY -> R.string.repeat_freq_monthly
    Frequency.YEARLY -> R.string.repeat_freq_yearly
}
