package com.elkrrai.feather.ui

import com.elkrrai.feather.domain.settings.AppSettings
import com.elkrrai.feather.domain.settings.TimeFormat
import com.elkrrai.feather.domain.settings.WeekStart
import com.elkrrai.feather.ui.format.formatDueTime
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayFormatTest {

    @After
    fun backToDefaults() {
        DisplayPrefs.update(AppSettings())
    }

    @Test
    fun theClockFollowsTheChosenFormat() {
        val time = LocalTime.of(17, 5)

        DisplayPrefs.update(AppSettings(timeFormat = TimeFormat.H24))
        assertEquals("17:05", formatDueTime(time, Locale.US))

        DisplayPrefs.update(AppSettings(timeFormat = TimeFormat.H12))
        assertEquals("5:05 PM", formatDueTime(time, Locale.US))
    }

    @Test
    fun theFirstWeekdayFollowsTheChoice() {
        DisplayPrefs.update(AppSettings(weekStart = WeekStart.SATURDAY))
        assertEquals(DayOfWeek.SATURDAY, firstDayOfWeek(Locale.US))

        DisplayPrefs.update(AppSettings(weekStart = WeekStart.MONDAY))
        assertEquals(DayOfWeek.MONDAY, firstDayOfWeek(Locale.US))

        // Follow system: the locale decides.
        DisplayPrefs.update(AppSettings(weekStart = WeekStart.SYSTEM))
        assertEquals(DayOfWeek.SUNDAY, firstDayOfWeek(Locale.US))
        assertEquals(DayOfWeek.MONDAY, firstDayOfWeek(Locale.FRANCE))
    }
}
