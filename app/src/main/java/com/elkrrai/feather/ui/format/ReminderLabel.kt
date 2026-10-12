package com.elkrrai.feather.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.model.Reminder
import com.elkrrai.feather.domain.model.ReminderKind
import java.time.LocalDate

/**
 * "At due time", "30 minutes before", "2 days before" or the date and time of a specific
 * reminder, in the app language. Offsets that are whole days or hours are shown in those units.
 */
@Composable
fun Reminder.label(today: LocalDate = LocalDate.now()): String = when (kind) {
    ReminderKind.BEFORE_DUE -> offsetLabel(offsetMinutes)
    ReminderKind.AT_TIME, ReminderKind.SNOOZE -> {
        val context = LocalContext.current
        val locale = LocalConfiguration.current.locales[0]
        at?.let { moment ->
            val day = formatDueDate(context, moment.toLocalDate(), today, locale)
            "$day, ${formatDueTime(moment.toLocalTime(), locale)}"
        }.orEmpty()
    }
}

/** The label of a "before due" reminder with the given offset in minutes (0 = at the due moment). */
@Composable
fun offsetLabel(offsetMinutes: Int): String {
    val resources = LocalContext.current.resources
    return when {
        offsetMinutes <= 0 -> stringResource(R.string.reminder_at_due_time)
        offsetMinutes % MINUTES_PER_DAY == 0 -> {
            val days = offsetMinutes / MINUTES_PER_DAY
            resources.getQuantityString(R.plurals.reminder_days_before, days, days)
        }
        offsetMinutes % MINUTES_PER_HOUR == 0 -> {
            val hours = offsetMinutes / MINUTES_PER_HOUR
            resources.getQuantityString(R.plurals.reminder_hours_before, hours, hours)
        }
        else -> resources.getQuantityString(R.plurals.reminder_minutes_before, offsetMinutes, offsetMinutes)
    }
}

private const val MINUTES_PER_HOUR = 60
private const val MINUTES_PER_DAY = 24 * 60
