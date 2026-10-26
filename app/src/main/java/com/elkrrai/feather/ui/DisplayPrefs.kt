package com.elkrrai.feather.ui

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.elkrrai.feather.domain.nest.NestStyle
import com.elkrrai.feather.domain.settings.AppSettings
import com.elkrrai.feather.domain.settings.TimeFormat
import com.elkrrai.feather.domain.settings.WeekStart
import java.time.DayOfWeek
import java.util.Locale
import java.time.temporal.WeekFields

/**
 * The display choices from Settings that formatting code needs everywhere: theme colors, the
 * clock format, the first weekday and haptics. They are read where they are used instead of
 * being passed through every screen, notification and widget; the app writes them from the
 * saved settings at start and whenever they change. The values are snapshot state, so screens
 * that read them redraw when they change.
 */
object DisplayPrefs {
    var timeFormat by mutableStateOf(TimeFormat.SYSTEM)
        private set
    var weekStart by mutableStateOf(WeekStart.SYSTEM)
        private set
    var dynamicColor by mutableStateOf(false)
        private set
    var pureBlack by mutableStateOf(false)
        private set
    var haptics by mutableStateOf(true)
        private set
    var birdAnimations by mutableStateOf(true)
        private set
    var reduceMotion by mutableStateOf(false)
        private set
    var celebrations by mutableStateOf(true)
        private set
    /** The nest the person chose, for the scenes that show it. */
    var nestStyle by mutableStateOf(NestStyle.BRAIDED)
        private set

    fun update(settings: AppSettings) {
        timeFormat = settings.timeFormat
        weekStart = settings.weekStart
        dynamicColor = settings.dynamicColor
        pureBlack = settings.pureBlack
        haptics = settings.haptics
        birdAnimations = settings.birdAnimations
        reduceMotion = settings.reduceMotion
        nestStyle = settings.nestStyle
        celebrations = settings.celebrations
    }
}

/** Whether clocks show 24 hours: the user's choice, or the phone's own setting. */
fun use24HourClock(context: Context): Boolean = when (DisplayPrefs.timeFormat) {
    TimeFormat.SYSTEM -> DateFormat.is24HourFormat(context)
    TimeFormat.H12 -> false
    TimeFormat.H24 -> true
}

/** The first day of the week: the user's choice, or the locale's. */
fun firstDayOfWeek(locale: Locale): DayOfWeek = when (DisplayPrefs.weekStart) {
    WeekStart.SYSTEM -> WeekFields.of(locale).firstDayOfWeek
    WeekStart.MONDAY -> DayOfWeek.MONDAY
    WeekStart.SATURDAY -> DayOfWeek.SATURDAY
    WeekStart.SUNDAY -> DayOfWeek.SUNDAY
}
