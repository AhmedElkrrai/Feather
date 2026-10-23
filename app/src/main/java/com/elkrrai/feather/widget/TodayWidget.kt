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
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
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
import com.elkrrai.feather.domain.model.Priority
import com.elkrrai.feather.domain.view.TaskScope
import com.elkrrai.feather.domain.view.TaskSort
import com.elkrrai.feather.domain.view.comparator
import com.elkrrai.feather.domain.view.inScope
import com.elkrrai.feather.feature.quickadd.QuickAddActivity
import com.elkrrai.feather.reminders.withAppLocale
import com.elkrrai.feather.ui.brand.BirdMood
import com.elkrrai.feather.ui.theme.DayPhase
import com.elkrrai.feather.ui.today.Greeting
import com.elkrrai.feather.ui.today.TodayStatus
import com.elkrrai.feather.ui.today.dayPhaseFor
import com.elkrrai.feather.ui.today.greetingFor
import com.elkrrai.feather.ui.today.todayBirdMood
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * The 4 by 4 widget: a sky header with the greeting, how many things are left and the bird in the
 * mood of the day, then the tasks due today or waiting, each with a ring to finish it. A plus
 * button adds one. It is redrawn by [WidgetUpdater] whenever tasks change, and by the system every
 * half hour (which also moves the sky along).
 */
class TodayWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entry = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val today = LocalDate.now()
        val now = LocalTime.now()
        val localized = context.withAppLocale()
        val locale = localized.resources.configuration.locales[0]

        val all = entry.taskRepository().getActiveTasks()
        val open = all.inScope(TaskScope.Today, today).sortedWith(TaskSort.DUE_DATE.comparator())
        val zone = ZoneId.systemDefault()
        val doneToday = all.count { it.completedAt?.atZone(zone)?.toLocalDate() == today }
        val status = TodayStatus(left = open.size, done = doneToday)
        val phase = dayPhaseFor(now)
        val mood = todayBirdMood(status, phase)

        val rows = open.take(MAX_ROWS).map { task ->
            TodayRow(task.id, task.title, dueLabel(localized, task, today, locale), task.priority)
        }
        val greeting = localized.getString(
            when (greetingFor(now)) {
                Greeting.Morning -> R.string.greeting_morning
                Greeting.Afternoon -> R.string.greeting_afternoon
                Greeting.Evening -> R.string.greeting_evening
                Greeting.Night -> R.string.greeting_night
            }
        )
        val countLine = when {
            status.planned == 0 -> localized.getString(R.string.today_nothing_planned)
            status.left == 0 -> localized.getString(R.string.today_all_done)
            status.done == 0 -> localized.resources.getQuantityString(R.plurals.today_left, status.left, status.left)
            else -> localized.resources.getQuantityString(R.plurals.today_left, status.left, status.left) +
                " · " + localized.getString(R.string.today_done_count, status.done)
        }
        val empty = localized.getString(R.string.widget_today_empty)
        val addLabel = localized.getString(R.string.action_add_task)

        provideContent {
            TodayContent(localized, phase, greeting, countLine, mood, rows, empty, addLabel)
        }
    }

    private data class TodayRow(val id: String, val title: String, val due: String, val priority: Priority)

    @Composable
    private fun TodayContent(
        context: Context,
        phase: DayPhase,
        greeting: String,
        countLine: String,
        mood: BirdMood,
        rows: List<TodayRow>,
        empty: String,
        addLabel: String,
    ) {
        val onSky = WidgetColors.onSky(phase)
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.widget_bg_card)),
        ) {
            // The sky: greeting and count on the start side, the bird and the add button at the end.
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(ImageProvider(skyTopDrawable(phase)))
                    .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
                ) {
                    Text(
                        text = greeting,
                        maxLines = 1,
                        style = TextStyle(color = onSky, fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = countLine,
                        maxLines = 1,
                        style = TextStyle(color = onSky, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                    )
                }
                Image(
                    provider = ImageProvider(birdDrawable(mood)),
                    contentDescription = null,
                    modifier = GlanceModifier.size(52.dp),
                )
                Spacer(GlanceModifier.width(8.dp))
                Box(
                    modifier = GlanceModifier
                        .size(44.dp)
                        .background(WidgetColors.primary)
                        .cornerRadius(16.dp)
                    .clickable(actionStartActivity(QuickAddActivity.intent(context).apply {
                        // Extras do not identify PendingIntents. Give this action a unique data URI
                        // so Android replaces any previously cached quick-add intent without the date.
                        data = Uri.parse("feather://quick-add/today-widget")
                        putExtra(QuickAddActivity.EXTRA_DEFAULT_DUE_TODAY, true)
                    })),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_add),
                        contentDescription = addLabel,
                        colorFilter = ColorFilter.tint(WidgetColors.onPrimary),
                        modifier = GlanceModifier.size(24.dp),
                    )
                }
            }

            if (rows.isEmpty()) {
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = empty,
                        style = TextStyle(color = WidgetColors.inkSoft, fontSize = 14.sp),
                    )
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 4.dp)) {
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
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompleteRing(context, row.id, row.title, row.priority)
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .padding(end = 8.dp)
                    .clickable(actionStartActivity(openTaskIntent(context, row.id))),
            ) {
                Text(
                    text = row.title,
                    maxLines = 2,
                    style = TextStyle(color = WidgetColors.ink, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                )
                if (row.due.isNotEmpty()) {
                    Text(
                        text = row.due,
                        maxLines = 1,
                        style = TextStyle(color = WidgetColors.inkSoft, fontSize = 12.sp),
                    )
                }
            }
        }
    }

    private companion object {
        const val MAX_ROWS = 30
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}

/** The ring of a row: finishes the task (a repeating one moves on) and redraws the widgets. */
class CompleteTaskAction : ActionCallback {

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val taskId = parameters[TaskIdKey] ?: return
        val entry = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        entry.taskRepository().setCompleted(taskId, true)
        // Redraw before reminder rescheduling does additional database work.
        updateTaskWidgets(context)
        entry.notifier().cancel(taskId)
        entry.scheduler().reschedule()
    }

    companion object {
        val TaskIdKey = ActionParameters.Key<String>("task_id")
    }
}
