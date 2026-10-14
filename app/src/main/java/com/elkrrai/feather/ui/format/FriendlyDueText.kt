package com.elkrrai.feather.ui.format

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.elkrrai.feather.R
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/**
 * The words on a task's date chip: "Tonight · 7:00 PM", "Today", "Tomorrow · 9:00 AM", "Sat",
 * "Oct 31, 2026", or, for a task that is still open after its day, "Waiting since Mon".
 */
fun dueChipText(context: Context, label: DueLabel, locale: Locale): String {
    fun withTime(day: String, time: LocalTime?): String =
        if (time != null) "$day · ${formatDueTime(time, locale)}" else day

    fun mediumDate(label: java.time.LocalDate): String =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(label)

    return when (label) {
        is DueLabel.Tonight -> withTime(context.getString(R.string.due_tonight), label.time)
        is DueLabel.Today -> withTime(context.getString(R.string.due_today), label.time)
        is DueLabel.Tomorrow -> withTime(context.getString(R.string.due_tomorrow), label.time)
        is DueLabel.Weekday -> withTime(label.day.getDisplayName(TextStyle.SHORT, locale), label.time)
        is DueLabel.OnDate -> withTime(mediumDate(label.date), label.time)
        is DueLabel.WaitingWeekday ->
            context.getString(R.string.due_waiting_since, label.day.getDisplayName(TextStyle.SHORT, locale))

        is DueLabel.WaitingDate -> context.getString(R.string.due_waiting_since, mediumDate(label.date))
    }
}

@Composable
fun DueLabel.chipText(): String {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    return remember(this, locale) { dueChipText(context, this, locale) }
}
