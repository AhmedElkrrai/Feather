package com.elkrrai.feather.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.elkrrai.feather.MainActivity
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.view.TaskSort
import com.elkrrai.feather.domain.view.comparator
import com.elkrrai.feather.reminders.withAppLocale
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate

/**
 * One task on the home screen: the most urgent open one (pinned first, then the earliest due
 * date, undated last), with a checkbox to finish it. Tapping it opens the task.
 */
class NextTaskWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entry = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val today = LocalDate.now()
        val localized = context.withAppLocale()
        val locale = localized.resources.configuration.locales[0]

        val next = entry.taskRepository().getActiveTasks()
            .filter { !it.isCompleted }
            .sortedWith(TaskSort.DUE_DATE.comparator())
            .firstOrNull()
        val title = next?.title
        val due = next?.let { dueLabel(localized, it, today, locale) }.orEmpty()
        val taskId = next?.id
        val heading = localized.getString(R.string.widget_next_heading)
        val empty = localized.getString(R.string.widget_next_empty)
        val intent = taskId?.let { openTaskIntent(localized, it) }

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.widgetBackground)
                        .cornerRadius(24.dp)
                        .padding(12.dp),
                ) {
                    Text(
                        text = heading,
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                    Spacer(GlanceModifier.height(4.dp))
                    if (title == null || taskId == null || intent == null) {
                        Column(
                            modifier = GlanceModifier
                                .fillMaxSize()
                                .clickable(actionStartActivity(Intent(localized, MainActivity::class.java))),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_bird_mark),
                                contentDescription = null,
                                modifier = GlanceModifier.size(48.dp),
                            )
                            Spacer(GlanceModifier.height(4.dp))
                            Text(
                                text = empty,
                                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 14.sp),
                            )
                        }
                    } else {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CheckBox(
                                checked = false,
                                onCheckedChange = actionRunCallback<CompleteTaskAction>(
                                    actionParametersOf(CompleteTaskAction.TaskIdKey to taskId)
                                ),
                            )
                            Spacer(GlanceModifier.width(4.dp))
                            Column(
                                modifier = GlanceModifier
                                    .defaultWeight()
                                    .clickable(actionStartActivity(intent)),
                            ) {
                                Text(
                                    text = title,
                                    maxLines = 3,
                                    style = TextStyle(
                                        color = GlanceTheme.colors.onSurface,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                    ),
                                )
                                if (due.isNotEmpty()) {
                                    Text(
                                        text = due,
                                        style = TextStyle(
                                            color = GlanceTheme.colors.onSurfaceVariant,
                                            fontSize = 12.sp,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

class NextTaskWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextTaskWidget()
}
