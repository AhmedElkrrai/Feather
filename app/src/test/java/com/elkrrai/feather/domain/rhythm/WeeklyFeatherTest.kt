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

class WeeklyFeatherTest {
    private val zone = ZoneOffset.UTC
    // Monday, 19 October 2026: the first day of a week that starts on Monday.
    private val monday = LocalDate.of(2026, 10, 19)
    private val lastMonday = monday.minusWeeks(1)

    private fun at(date: LocalDate): Instant = date.atTime(10, 0).toInstant(zone)

    private fun done(date: LocalDate, list: String = "inbox", task: String = "t$date$list") =
        CompletionRecord(task, "Task", list, at(date))

    @Test
    fun theFeatherIsForTheWeekThatJustEnded() {
        val records = listOf(
            done(lastMonday), done(lastMonday), done(lastMonday.plusDays(2)),
            done(monday), // this week: not in it
            done(lastMonday.minusDays(1)), // the week before: not in it
        )

        val feather = weeklyFeather(records, monday, zone, DayOfWeek.MONDAY)!!

        assertEquals(lastMonday, feather.weekStart)
        assertEquals(listOf(2, 0, 1, 0, 0, 0, 0), feather.days)
        assertEquals(3, feather.total)
        assertEquals(5, feather.restDays)
    }

    @Test
    fun itsColorIsTheBusiestList() {
        val records = listOf(done(lastMonday, "a"), done(lastMonday.plusDays(1), "b"), done(lastMonday.plusDays(2), "b"))

        assertEquals("b", weeklyFeather(records, monday, zone, DayOfWeek.MONDAY)!!.topListId)
    }

    @Test
    fun aWeekOfNothingMakesNoFeather() {
        assertNull(weeklyFeather(listOf(done(monday)), monday, zone, DayOfWeek.MONDAY))
    }

    @Test
    fun aDayGetsOneBarbPerTaskUpToFour() {
        assertEquals(listOf(0, 1, 3, 4, 4), listOf(0, 1, 3, 4, 9).map(::barbsFor))
    }

    @Test
    fun theFeatherDayIsTheFirstDayOfTheWeek() {
        assertTrue(isFeatherDay(monday, DayOfWeek.MONDAY))
        assertFalse(isFeatherDay(monday.plusDays(1), DayOfWeek.MONDAY))
        assertTrue(isFeatherDay(monday.plusDays(5), DayOfWeek.SATURDAY))
    }

    @Test
    fun aTaskKeptForFourWeeksEarnsTheBuntingCheck() {
        val weekly = (0L..3L).map { done(monday.minusWeeks(it), task = "plants") }
        val skipped = listOf(done(monday, task = "x"), done(monday.minusWeeks(2), task = "x"))

        assertTrue(keptRepeaterFourWeeks(weekly, monday, zone, DayOfWeek.MONDAY))
        assertFalse(keptRepeaterFourWeeks(skipped, monday, zone, DayOfWeek.MONDAY))
    }
}
