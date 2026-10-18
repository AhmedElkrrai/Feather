package com.elkrrai.feather.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.elkrrai.feather.MainActivity
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.view.TaskSort
import com.elkrrai.feather.domain.view.comparator
import com.elkrrai.feather.reminders.withAppLocale
import com.elkrrai.feather.ui.brand.BirdMood
import com.elkrrai.feather.ui.theme.DayPhase
import com.elkrrai.feather.ui.today.dayPhaseFor
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import java.time.LocalTime

/**
 * The 4 by 2 widget: the most urgent open task (pinned first, then the earliest due date,
 * undated last) on a sky that follows the time of day, with a ring to finish it. The bird sits at
 * the end, asleep late in the evening and happy when nothing is left. Tapping the task opens it.
 */
class NextTaskWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entry = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val today = LocalDate.now()
        val now = LocalTime.now()
        val localized = context.withAppLocale()
        val locale = localized.resources.configuration.locales[0]

        val next = entry.taskRepository().getActiveTasks()
            .filter { !it.isCompleted }
            .sortedWith(TaskSort.DUE_DATE.comparator())
            .firstOrNull()
        val phase = dayPhaseFor(now)
        val mood = when {
            next == null -> BirdMood.HAPPY
            phase == DayPhase.Night -> BirdMood.SLEEPING
            else -> BirdMood.IDLE
        }
        val due = next?.let { dueLabel(localized, it, today, locale) }.orEmpty()
        val heading = localized.getString(R.string.widget_next_heading)
        val empty = localized.getString(R.string.widget_next_empty)
        val intent = next?.let { openTaskIntent(localized, it.id) }
        val onSky = WidgetColors.onSky(phase)

        provideContent {
            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ImageProvider(skyDrawable(phase)))
                    .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = heading,
                        style = TextStyle(color = onSky, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                    )
                    Spacer(GlanceModifier.height(2.dp))
                    if (next == null || intent == null) {
                        Text(
                            text = empty,
                            maxLines = 2,
                            style = TextStyle(color = onSky, fontSize = 18.sp, fontWeight = FontWeight.Bold),
                            modifier = GlanceModifier.clickable(actionStartActivity(Intent(localized, MainActivity::class.java))),
                        )
                    } else {
                        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            CompleteRing(localized, next.id, next.title, next.priority)
                            Column(
                                modifier = GlanceModifier
                                    .defaultWeight()
                                    .clickable(actionStartActivity(intent)),
                            ) {
                                Text(
                                    text = next.title,
                                    maxLines = 3,
                                    style = TextStyle(color = onSky, fontSize = 18.sp, fontWeight = FontWeight.Bold),
                                )
                                if (due.isNotEmpty()) {
                                    Text(
                                        text = due,
                                        maxLines = 1,
                                        style = TextStyle(color = onSky, fontSize = 13.sp),
                                    )
                                }
                            }
                        }
                    }
                }
                Image(
                    provider = ImageProvider(birdDrawable(mood)),
                    contentDescription = null,
                    modifier = GlanceModifier.size(64.dp),
                )
            }
        }
    }
}

class NextTaskWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextTaskWidget()
}
