package com.elkrrai.feather.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.elkrrai.feather.MainActivity
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.model.Task
import com.elkrrai.feather.domain.view.TaskScope
import com.elkrrai.feather.domain.view.TaskSort
import com.elkrrai.feather.domain.view.comparator
import com.elkrrai.feather.domain.view.inScope
import com.elkrrai.feather.feature.quickadd.QuickAddActivity
import com.elkrrai.feather.reminders.withAppLocale
import com.elkrrai.feather.ui.format.formatDueDate
import com.elkrrai.feather.ui.format.formatDueTime
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import java.util.Locale

/**
 * Home-screen list of what is due today or overdue: tick a task to finish it, tap its title to
 * open it, tap the plus to add one. It is redrawn by [WidgetUpdater] whenever tasks change.
 */
class TodayWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entry = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val today = LocalDate.now()
        val localized = context.withAppLocale()
        val locale = localized.resources.configuration.locales[0]

        val tasks = entry.taskRepository().getActiveTasks()
            .inScope(TaskScope.Today, today)
            .sortedWith(TaskSort.DUE_DATE.comparator())
        val rows = tasks.take(MAX_ROWS).map { task ->
            TodayRow(task.id, task.title, dueLabel(localized, task, today, locale), overdue = task.dueDate?.isBefore(today) == true)
        }
        val title = localized.getString(R.string.scope_today)
        val empty = localized.getString(R.string.widget_today_empty)
        val addLabel = localized.getString(R.string.action_add_task)

        provideContent {
            GlanceTheme {
                TodayContent(
                    context = localized,
                    title = title,
                    count = tasks.size,
                    rows = rows,
                    empty = empty,
                    addLabel = addLabel,
                )
            }
        }
    }

    private data class TodayRow(val id: String, val title: String, val due: String, val overdue: Boolean)

    /** The time for a task due today, the date for an overdue one. */
    private fun dueLabel(context: Context, task: Task, today: LocalDate, locale: Locale): String {
        val date = task.dueDate ?: return ""
        val time = task.dueTime?.let { formatDueTime(it, locale) }
        return when {
            date.isBefore(today) -> listOfNotNull(formatDueDate(context, date, today, locale), time).joinToString(", ")
            else -> time.orEmpty()
        }
    }

    @Composable
    private fun TodayContent(
        context: Context,
        title: String,
        count: Int,
        rows: List<TodayRow>,
        empty: String,
        addLabel: String,
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(24.dp)
                .padding(12.dp),
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (count > 0) "$title · $count" else title,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    modifier = GlanceModifier
                        .defaultWeight()
                        .clickable(actionStartActivity<MainActivity>()),
                )
                Image(
                    provider = ImageProvider(R.drawable.ic_add),
                    contentDescription = addLabel,
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                    modifier = GlanceModifier
                        .size(32.dp)
                        .padding(4.dp)
                        .clickable(actionStartActivity<QuickAddActivity>()),
                )
            }

            Spacer(GlanceModifier.size(4.dp))

            if (rows.isEmpty()) {
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = empty,
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 14.sp),
                    )
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(rows, itemId = { it.id.hashCode().toLong() }) { row ->
                        TaskLine(context, row)
                    }
                }
            }
        }
    }

    @Composable
    private fun TaskLine(context: Context, row: TodayRow) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CheckBox(
                checked = false,
                onCheckedChange = actionRunCallback<CompleteTaskAction>(
                    actionParametersOf(CompleteTaskAction.TaskIdKey to row.id)
                ),
            )
            Spacer(GlanceModifier.width(4.dp))
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(actionStartActivity(openTaskIntent(context, row.id))),
            ) {
                Text(
                    text = row.title,
                    maxLines = 2,
                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp),
                )
                if (row.due.isNotEmpty()) {
                    Text(
                        text = row.due,
                        style = TextStyle(
                            color = if (row.overdue) GlanceTheme.colors.error else GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 12.sp,
                        ),
                    )
                }
            }
        }
    }

    private fun openTaskIntent(context: Context, taskId: String): Intent =
        Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN_TASK_ID, taskId)
            // A distinct data URI keeps every row's PendingIntent separate.
            .setData(Uri.parse("feather://task/$taskId"))

    private companion object {
        const val MAX_ROWS = 30
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}

/** The checkbox of a row: finishes the task (a repeating one moves on) and redraws the widget. */
class CompleteTaskAction : ActionCallback {

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val taskId = parameters[TaskIdKey] ?: return
        val entry = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        entry.taskRepository().setCompleted(taskId, true)
        entry.notifier().cancel(taskId)
        entry.scheduler().reschedule()
        TodayWidget().updateAll(context)
    }

    companion object {
        val TaskIdKey = ActionParameters.Key<String>("task_id")
    }
}
