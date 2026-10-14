package com.elkrrai.feather.ui.format

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FriendlyDueTest {

    // A Wednesday.
    private val today = LocalDate.of(2026, 10, 14)

    private fun label(date: LocalDate, time: LocalTime? = null, completed: Boolean = false) =
        dueLabelFor(date, time, today, completed)

    @Test
    fun todayIsTodayWithItsTime() {
        assertEquals(DueLabel.Today(null), label(today))
        assertEquals(DueLabel.Today(LocalTime.of(9, 30)), label(today, LocalTime.of(9, 30)))
    }

    @Test
    fun fromSixInTheEveningTodayBecomesTonight() {
        assertEquals(DueLabel.Today(LocalTime.of(17, 59)), label(today, LocalTime.of(17, 59)))
        assertEquals(DueLabel.Tonight(LocalTime.of(18, 0)), label(today, LocalTime.of(18, 0)))
        assertEquals(DueLabel.Tonight(LocalTime.of(23, 30)), label(today, LocalTime.of(23, 30)))
    }

    @Test
    fun tomorrowStaysTomorrowEvenInTheEvening() {
        assertEquals(DueLabel.Tomorrow(null), label(today.plusDays(1)))
        assertEquals(DueLabel.Tomorrow(LocalTime.of(20, 0)), label(today.plusDays(1), LocalTime.of(20, 0)))
    }

    @Test
    fun theComingWeekIsJustTheWeekday() {
        assertEquals(DueLabel.Weekday(DayOfWeek.FRIDAY, null), label(today.plusDays(2)))
        assertEquals(DueLabel.Weekday(DayOfWeek.TUESDAY, null), label(today.plusDays(6)))
    }

    @Test
    fun furtherAwayIsTheDate() {
        val date = today.plusDays(7)

        assertEquals(DueLabel.OnDate(date, null), label(date))
    }

    @Test
    fun anOpenTaskAfterItsDayIsWaiting() {
        assertEquals(DueLabel.WaitingWeekday(DayOfWeek.TUESDAY), label(today.minusDays(1)))
        assertEquals(DueLabel.WaitingWeekday(DayOfWeek.THURSDAY), label(today.minusDays(6)))
    }

    @Test
    fun aTaskWaitingForMoreThanAWeekShowsTheDate() {
        val date = today.minusDays(7)

        assertEquals(DueLabel.WaitingDate(date), label(date))
    }

    @Test
    fun aFinishedTaskFromThePastIsJustItsDate() {
        val date = today.minusDays(2)

        assertEquals(DueLabel.OnDate(date, null), label(date, completed = true))
    }

    @Test
    fun waitingLabelsAreMarkedAsWaitingAndTodayLabelsAsToday() {
        assertTrue(label(today.minusDays(1)).isWaiting)
        assertTrue(label(today.minusDays(30)).isWaiting)
        assertFalse(label(today).isWaiting)
        assertTrue(label(today).isToday)
        assertTrue(label(today, LocalTime.of(20, 0)).isToday)
        assertFalse(label(today.plusDays(1)).isToday)
    }

    @Test
    fun waitingLabelsCarryNoTime() {
        assertEquals(null, label(today.minusDays(1), LocalTime.of(9, 0)).time)
    }
}
