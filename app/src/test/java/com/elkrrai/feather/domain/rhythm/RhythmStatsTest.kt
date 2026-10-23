package com.elkrrai.feather.domain.rhythm

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RhythmStatsTest {
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 10, 15) // a Thursday

    private fun at(date: LocalDate, hour: Int = 10) = date.atTime(hour, 0).toInstant(zone)

    private fun done(date: LocalDate, hour: Int = 10, taskId: String = "t${date}${hour}") =
        CompletionRecord(taskId, "Task $taskId", at(date, hour))

    private fun stats(records: List<CompletionRecord>, paused: Set<LocalDate> = emptySet()) =
        rhythmStats(records, today, zone, DayOfWeek.MONDAY, paused)

    @Test
    fun theLastSevenDaysEndToday() {
        val s = stats(listOf(done(today), done(today, 14), done(today.minusDays(6))))

        assertEquals(7, s.lastSeven.size)
        assertEquals(today.minusDays(6), s.lastSeven.first().date)
        assertEquals(today, s.lastSeven.last().date)
        assertEquals(2, s.lastSeven.last().count)
        assertEquals(1, s.lastSeven.first().count)
        assertEquals(3, s.weekTotal)
    }

    @Test
    fun aTaskFromEightDaysAgoIsNotInTheWeek() {
        assertEquals(0, stats(listOf(done(today.minusDays(7)))).weekTotal)
    }

    @Test
    fun aNewPersonHasNothingToChartBeforeThreeFinishes() {
        assertTrue(stats(listOf(done(today), done(today))).isNew)
        assertFalse(stats(listOf(done(today), done(today), done(today))).isNew)
    }

    @Test
    fun fourteenDotsShowWhoShowedUp() {
        val s = stats(listOf(done(today), done(today.minusDays(2))))

        assertEquals(14, s.dots.size)
        assertEquals(today, s.dots.last().date)
        assertEquals(2, s.dotsShowedUp)
        assertTrue(s.dots.last().showedUp)
        assertFalse(s.dots[s.dots.lastIndex - 1].showedUp)
    }

    @Test
    fun pausedDaysAreLeftOutOfTheDotsNotCountedAsGaps() {
        val paused = (1L..3L).map { today.minusDays(it) }.toSet()
        val s = stats(listOf(done(today)), paused)

        assertEquals(14, s.dots.size)
        assertTrue(s.dots.none { it.date in paused })
        assertEquals(today.minusDays(16), s.dots.first().date)
    }

    @Test
    fun strawTonesGrowWithTheDay() {
        assertEquals(listOf(0, 1, 2, 3, 3, 4, 4), listOf(0, 1, 2, 3, 4, 5, 9).map(::strawLevel))
    }

    @Test
    fun theMonthStartsOnTheFirstDayOfTheWeek() {
        val s = stats(emptyList())

        // 1 October 2026 is a Thursday: with weeks from Monday there are three empty cells before it.
        assertEquals(3, s.month.leadingBlanks)
        assertEquals(31, s.month.cells.size)
        assertTrue(s.month.cells[14].isToday)
        assertTrue(s.month.cells[15].isFuture)
        // With weeks from Sunday there are four.
        assertEquals(4, rhythmStats(emptyList(), today, zone, DayOfWeek.SUNDAY).month.leadingBlanks)
    }

    @Test
    fun theBusiestWeekdayNeedsAtLeastTwoFinishes() {
        assertNull(stats(listOf(done(today))).busiestWeekday)
        val thursdays = listOf(done(today), done(today.minusDays(7)), done(today.minusDays(14)), done(today.minusDays(1)))

        assertEquals(DayOfWeek.THURSDAY, stats(thursdays).busiestWeekday)
    }

    @Test
    fun theBestTimeIsThePartOfTheDayWithTheMost() {
        val s = stats(listOf(done(today, 8), done(today, 9), done(today.minusDays(1), 19)))

        assertEquals(TimeOfDay.MORNING, s.bestTime)
        assertEquals(2, s.timeOfDay[TimeOfDay.MORNING])
        assertEquals(1, s.timeOfDay[TimeOfDay.EVENING])
    }

    @Test
    fun partsOfTheDayFollowTheClock() {
        assertEquals(TimeOfDay.NIGHT, timeOfDayFor(3))
        assertEquals(TimeOfDay.MORNING, timeOfDayFor(5))
        assertEquals(TimeOfDay.AFTERNOON, timeOfDayFor(12))
        assertEquals(TimeOfDay.EVENING, timeOfDayFor(17))
        assertEquals(TimeOfDay.NIGHT, timeOfDayFor(21))
    }

    @Test
    fun aRepeatingTaskDoneOnFourOfTheLastSevenDaysIsGoingWell() {
        val daily = (0L..3L).map { done(today.minusDays(it), taskId = "water") }
        val once = done(today, taskId = "once")
        val s = stats(daily + once)

        assertEquals(listOf("water"), s.steady.map { it.taskId })
        assertEquals(4, s.steady.single().lastSeven.count { it })
        assertTrue(s.steady.single().lastSeven.last())
    }

    @Test
    fun aTaskDoneWeeklyForFourWeeksIsGoingWell() {
        val weekly = (0L..3L).map { done(today.minusWeeks(it), taskId = "plants") }

        assertEquals(listOf("plants"), stats(weekly).steady.map { it.taskId })
    }
}
