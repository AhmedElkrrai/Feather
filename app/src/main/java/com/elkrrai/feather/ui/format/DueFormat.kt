package com.elkrrai.feather.ui.format

import android.content.Context
import com.elkrrai.feather.R
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

fun formatDueTime(time: LocalTime, locale: Locale): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).format(time)
