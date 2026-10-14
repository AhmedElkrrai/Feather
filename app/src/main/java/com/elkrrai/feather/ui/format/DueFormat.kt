package com.elkrrai.feather.ui.format

import android.content.Context
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.settings.TimeFormat
import com.elkrrai.feather.ui.DisplayPrefs
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Locale-aware label for a due date: "Today", "Tomorrow", or a medium localized date. */
fun formatDueDate(context: Context, date: LocalDate, today: LocalDate, locale: Locale): String =
    when (date) {
        today -> context.getString(R.string.due_today)
        today.plusDays(1) -> context.getString(R.string.due_tomorrow)
        else -> DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(date)
    }

/** Header for a day group (Upcoming view): "Today", "Tomorrow", or a full date with weekday. */
fun formatDayHeader(context: Context, date: LocalDate, today: LocalDate, locale: Locale): String =
    when (date) {
        today -> context.getString(R.string.due_today)
        today.plusDays(1) -> context.getString(R.string.due_tomorrow)
        else -> DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale).format(date)
    }

fun formatDueTime(time: LocalTime, locale: Locale): String {
    val formatter = when (DisplayPrefs.timeFormat) {
        TimeFormat.SYSTEM -> DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
        TimeFormat.H12 -> DateTimeFormatter.ofPattern("h:mm a", locale)
        TimeFormat.H24 -> DateTimeFormatter.ofPattern("HH:mm", locale)
    }
    return formatter.format(time)
}
