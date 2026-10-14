package com.elkrrai.feather.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.glance.appwidget.updateAll
import com.elkrrai.feather.MainActivity
import com.elkrrai.feather.domain.model.Task
import com.elkrrai.feather.ui.format.formatDueDate
import com.elkrrai.feather.ui.format.formatDueTime
import java.time.LocalDate
import java.util.Locale

/** The time for a task due today, the date (and time) for an overdue one, nothing for the rest. */
internal fun dueLabel(context: Context, task: Task, today: LocalDate, locale: Locale): String {
    val date = task.dueDate ?: return ""
    val time = task.dueTime?.let { formatDueTime(it, locale) }
    return when {
        date.isBefore(today) -> listOfNotNull(formatDueDate(context, date, today, locale), time).joinToString(", ")
        date == today -> time.orEmpty()
        else -> listOfNotNull(formatDueDate(context, date, today, locale), time).joinToString(", ")
    }
}

/** Opens the editor on [taskId]. */
internal fun openTaskIntent(context: Context, taskId: String): Intent =
    Intent(context, MainActivity::class.java)
        .putExtra(MainActivity.EXTRA_OPEN_TASK_ID, taskId)
        // A distinct data URI keeps every widget's PendingIntent separate.
        .setData(Uri.parse("feather://task/$taskId"))

/** Redraws every widget that shows tasks. */
internal suspend fun updateTaskWidgets(context: Context) {
    TodayWidget().updateAll(context)
    NextTaskWidget().updateAll(context)
}
