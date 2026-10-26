package com.elkrrai.feather.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.color.ColorProvider
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider as GlanceColorProvider
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.model.Priority
import com.elkrrai.feather.ui.brand.BirdMood
import com.elkrrai.feather.ui.theme.DarkFeatherColors
import com.elkrrai.feather.ui.theme.DayPhase
import com.elkrrai.feather.ui.theme.LightFeatherColors
import com.elkrrai.feather.ui.theme.Nest
import com.elkrrai.feather.ui.theme.NestDarkScheme
import com.elkrrai.feather.ui.theme.NestLightScheme

/**
 * The widgets' look, from the same Nest palette as the app but with its own day and night values
 * (a widget cannot read the app's theme). Everything here is a drawable or a color: no animation,
 * nothing that needs the app to be running.
 */
internal object WidgetColors {
    private fun pair(day: Color, night: Color) = ColorProvider(day = day, night = night)

    val ink = pair(NestLightScheme.onSurface, NestDarkScheme.onSurface)
    val inkSoft = pair(NestLightScheme.onSurfaceVariant, NestDarkScheme.onSurfaceVariant)
    val primary = pair(NestLightScheme.primary, NestDarkScheme.primary)
    val onPrimary = pair(NestLightScheme.onPrimary, NestDarkScheme.onPrimary)
    val primaryContainer = pair(NestLightScheme.primaryContainer, NestDarkScheme.primaryContainer)
    val onPrimaryContainer = pair(NestLightScheme.onPrimaryContainer, NestDarkScheme.onPrimaryContainer)

    /** Text on a sky: the night sky is dark in both themes, so its text is always cream. */
    fun onSky(phase: DayPhase): GlanceColorProvider =
        if (phase == DayPhase.Night) pair(Nest.Cream, Nest.Cream) else pair(LightFeatherColors.skyText, DarkFeatherColors.skyText)

    /** The ring's color for a task's priority. */
    fun priority(priority: Priority): GlanceColorProvider = when (priority) {
        Priority.NONE -> pair(LightFeatherColors.prioNone, DarkFeatherColors.prioNone)
        Priority.LOW -> pair(LightFeatherColors.prioLow, DarkFeatherColors.prioLow)
        Priority.MEDIUM -> pair(LightFeatherColors.prioMed, DarkFeatherColors.prioMed)
        Priority.HIGH -> pair(LightFeatherColors.prioHigh, DarkFeatherColors.prioHigh)
    }
}

/** A whole-widget sky with rounded corners. */
internal fun skyDrawable(phase: DayPhase): Int = when (phase) {
    DayPhase.Dawn -> R.drawable.widget_sky_dawn
    DayPhase.Day -> R.drawable.widget_sky_day
    DayPhase.Dusk -> R.drawable.widget_sky_dusk
    DayPhase.Night -> R.drawable.widget_sky_night
}

/** A sky for the top band of a widget: rounded on top only. */
internal fun skyTopDrawable(phase: DayPhase): Int = when (phase) {
    DayPhase.Dawn -> R.drawable.widget_sky_top_dawn
    DayPhase.Day -> R.drawable.widget_sky_top_day
    DayPhase.Dusk -> R.drawable.widget_sky_top_dusk
    DayPhase.Night -> R.drawable.widget_sky_top_night
}

/** The owl for a mood, chosen when the widget is drawn: asleep, flying with wings up, or resting. */
internal fun birdDrawable(mood: BirdMood): Int = when (mood) {
    BirdMood.SLEEPING -> R.drawable.widget_owl_sleeping
    BirdMood.HAPPY -> R.drawable.widget_owl_happy
    BirdMood.IDLE, BirdMood.FLYING -> R.drawable.widget_owl_idle
}

/**
 * The completion ring of a widget row: a 26dp ring in the priority color inside a 48dp target. It
 * says "Mark <title> as done" to screen readers; tapping it finishes the task.
 */
@Composable
internal fun CompleteRing(context: Context, taskId: String, title: String, priority: Priority) {
    Image(
        provider = ImageProvider(R.drawable.ic_widget_ring),
        contentDescription = context.getString(R.string.widget_mark_done, title),
        colorFilter = ColorFilter.tint(WidgetColors.priority(priority)),
        modifier = GlanceModifier
            .size(48.dp)
            .padding(11.dp)
            .clickable(actionRunCallback<CompleteTaskAction>(actionParametersOf(CompleteTaskAction.TaskIdKey to taskId))),
    )
}
