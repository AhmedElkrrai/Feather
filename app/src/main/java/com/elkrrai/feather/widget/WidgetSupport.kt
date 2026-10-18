package com.elkrrai.feather.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.glance.appwidget.updateAll
import com.elkrrai.feather.MainActivity
import com.elkrrai.feather.domain.model.Task
import com.elkrrai.feather.ui.format.dueChipText
import com.elkrrai.feather.ui.format.dueLabelFor
import com.elkrrai.feather.ui.format.formatDueTime
import java.time.LocalDate
import java.util.Locale

/**
 * The time for a task due today; for any other day the friendly words ("Tomorrow", "Sat",
 * "Waiting since Mon"), never a red alarm.
 */
internal fun dueLabel(context: Context, task: Task, today: LocalDate, locale: Locale): String {
    val date = task.dueDate ?: return ""
    if (date == today) return task.dueTime?.let { formatDueTime(it, locale) }.orEmpty()
    return dueChipText(context, dueLabelFor(date, task.dueTime, today, task.isCompleted), locale)
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
