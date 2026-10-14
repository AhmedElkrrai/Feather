package com.elkrrai.feather.domain.recurrence

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 14 Oct 2026 is a Wednesday, 13 Oct a Tuesday (the 2nd of the month) and 30 Oct a Friday (the
 * last one); the dates below rely on that.
 */
class RecurrenceTest {

    private fun date(text: String) = LocalDate.parse(text)

    private fun Recurrence.next(due: String, completed: String = due): LocalDate? =
        nextOccurrence(date(due), date(completed))

    // --- Daily ---

    @Test
    fun dailyMovesOneDay() {
        assertEquals(date("2026-10-15"), Recurrence(Frequency.DAILY).next("2026-10-14"))
    }

    @Test
    fun dailyWithAnIntervalKeepsToTheGrid() {
        val everyThree = Recurrence(Frequency.DAILY, interval = 3)
        assertEquals(date("2026-10-17"), everyThree.next("2026-10-14"))
        // Finished late on the 20th: 14, 17, 20, 23, so the next future one is the 23rd.
        assertEquals(date("2026-10-23"), everyThree.next("2026-10-14", completed = "2026-10-20"))
    }

    @Test
    fun finishingEarlyDoesNotSkipAnOccurrence() {
        assertEquals(date("2026-10-15"), Recurrence(Frequency.DAILY).next("2026-10-14", completed = "2026-10-10"))
    }

    @Test
    fun finishingVeryLateJumpsToTheNextFutureOccurrence() {
        assertEquals(date("2026-10-21"), Recurrence(Frequency.DAILY).next("2026-10-01", completed = "2026-10-20"))
    }

    // --- Weekly ---

    @Test
    fun weeklyWithoutDaysUsesTheDueWeekday() {
        assertEquals(date("2026-10-21"), Recurrence(Frequency.WEEKLY).next("2026-10-14"))
    }

    @Test
    fun weeklyWalksThroughTheChosenDays() {
        val monAndWed = Recurrence(Frequency.WEEKLY, weekdays = setOf(DayOfWeek.WEDNESDAY, DayOfWeek.MONDAY))
        assertEquals(date("2026-10-19"), monAndWed.next("2026-10-14")) // Wed -> Mon
        assertEquals(date("2026-10-21"), monAndWed.next("2026-10-19")) // Mon -> Wed
        assertEquals(date("2026-10-19"), monAndWed.next("2026-10-14", completed = "2026-10-16"))
    }

    @Test
    fun weeklyWithAnIntervalSkipsWeeks() {
        val everyOtherWeek = Recurrence(
            Frequency.WEEKLY,
            interval = 2,
            weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
        )
        assertEquals(date("2026-10-14"), everyOtherWeek.next("2026-10-12")) // Mon -> Wed, same week
        assertEquals(date("2026-10-26"), everyOtherWeek.next("2026-10-14")) // Wed -> Mon, two weeks on
    }

    @Test
    fun weeklyCrossesTheEndOfAYear() {
        val friday = Recurrence(Frequency.WEEKLY, weekdays = setOf(DayOfWeek.FRIDAY))
        assertEquals(date("2027-01-01"), friday.next("2026-12-25"))
    }

    // --- Monthly ---

    @Test
    fun monthlyWithoutADayUsesTheDueDay() {
        assertEquals(date("2026-11-14"), Recurrence(Frequency.MONTHLY).next("2026-10-14"))
        assertEquals(date("2027-01-14"), Recurrence(Frequency.MONTHLY, interval = 3).next("2026-10-14"))
    }

    @Test
    fun monthlyOnTheThirtyFirstUsesTheLastDayOfShortMonths() {
        val lastDay = Recurrence(Frequency.MONTHLY, monthDay = 31)
        assertEquals(date("2026-02-28"), lastDay.next("2026-01-31"))
        assertEquals(date("2026-03-31"), lastDay.next("2026-02-28"))
        assertEquals(date("2026-04-30"), lastDay.next("2026-03-31"))
        assertEquals(date("2028-02-29"), lastDay.next("2028-01-31"))
    }

    @Test
    fun monthlyOnAFixedDayIgnoresWhereTheDueDateIs() {
        val fifteenth = Recurrence(Frequency.MONTHLY, monthDay = 15)
        assertEquals(date("2026-10-15"), fifteenth.next("2026-10-14"))
        assertEquals(date("2026-11-15"), fifteenth.next("2026-10-20"))
    }

    @Test
    fun monthlyOnTheNthWeekday() {
        val secondTuesday = Recurrence(Frequency.MONTHLY, nthWeekday = NthWeekday(2, DayOfWeek.TUESDAY))
        assertEquals(date("2026-11-10"), secondTuesday.next("2026-10-13"))
        assertEquals(date("2026-12-08"), secondTuesday.next("2026-11-10"))
    }

    @Test
    fun monthlyOnTheLastWeekday() {
        val lastFriday = Recurrence(Frequency.MONTHLY, nthWeekday = NthWeekday(-1, DayOfWeek.FRIDAY))
        assertEquals(date("2026-11-27"), lastFriday.next("2026-10-30"))
    }

    @Test
    fun nthWeekdayOfADate() {
        assertEquals(NthWeekday(2, DayOfWeek.TUESDAY), NthWeekday.of(date("2026-10-13")))
        assertEquals(NthWeekday(-1, DayOfWeek.FRIDAY), NthWeekday.of(date("2026-10-30")))
        assertEquals(NthWeekday(1, DayOfWeek.THURSDAY), NthWeekday.of(date("2026-10-01")))
    }

    // --- Yearly ---

    @Test
    fun yearlyWithoutAMonthUsesTheDueDate() {
        assertEquals(date("2027-10-14"), Recurrence(Frequency.YEARLY).next("2026-10-14"))
        assertEquals(date("2028-10-14"), Recurrence(Frequency.YEARLY, interval = 2).next("2026-10-14"))
    }

    @Test
    fun yearlyOnTheLeapDayFallsBackToTheTwentyEighth() {
        val leapDay = Recurrence(Frequency.YEARLY, month = 2, monthDay = 29)
        assertEquals(date("2029-02-28"), leapDay.next("2028-02-29"))
        assertEquals(date("2032-02-29"), leapDay.next("2031-02-28"))
    }

    // --- Repeat after completion ---

    @Test
    fun afterCompletionCountsFromTheDayItWasFinished() {
        val rule = { frequency: Frequency, interval: Int ->
            Recurrence(frequency, interval = interval, mode = RepeatMode.AFTER_COMPLETION)
        }
        assertEquals(date("2026-10-22"), rule(Frequency.DAILY, 2).next("2026-10-01", completed = "2026-10-20"))
        assertEquals(date("2026-10-27"), rule(Frequency.WEEKLY, 1).next("2026-10-01", completed = "2026-10-20"))
        assertEquals(date("2026-02-28"), rule(Frequency.MONTHLY, 1).next("2026-01-01", completed = "2026-01-31"))
        assertEquals(date("2027-10-20"), rule(Frequency.YEARLY, 1).next("2026-10-01", completed = "2026-10-20"))
    }

    @Test
    fun afterCompletionIgnoresTheWeekdayAndMonthPattern() {
        val rule = Recurrence(
            Frequency.WEEKLY,
            weekdays = setOf(DayOfWeek.MONDAY),
            mode = RepeatMode.AFTER_COMPLETION,
        )
        assertEquals(date("2026-10-23"), rule.next("2026-10-14", completed = "2026-10-16"))
    }

    // --- Ending ---

    @Test
    fun theLastDayIsInclusive() {
        assertEquals(date("2026-10-21"), Recurrence(Frequency.DAILY, until = date("2026-10-21")).next("2026-10-20"))
        assertNull(Recurrence(Frequency.DAILY, until = date("2026-10-20")).next("2026-10-20"))
    }

    @Test
    fun theSeriesEndsAfterTheCountedOccurrences() {
        assertNull(Recurrence(Frequency.DAILY, count = 1).next("2026-10-14"))
        val twoLeft = Recurrence(Frequency.DAILY, count = 2)
        assertEquals(date("2026-10-15"), twoLeft.next("2026-10-14"))
        assertEquals(Recurrence(Frequency.DAILY, count = 1), twoLeft.afterAdvance())
    }

    @Test
    fun anOpenEndedRuleKeepsItsShapeWhenAdvancing() {
        val rule = Recurrence(Frequency.WEEKLY, interval = 2)
        assertEquals(rule, rule.afterAdvance())
    }

    // --- Rule strings ---

    @Test
    fun rulesAreWrittenInRfcStyle() {
        assertEquals("FREQ=DAILY", Recurrence(Frequency.DAILY).toRule())
        assertEquals(
            "FREQ=WEEKLY;INTERVAL=2;BYDAY=MO,WE",
            Recurrence(Frequency.WEEKLY, 2, weekdays = setOf(DayOfWeek.WEDNESDAY, DayOfWeek.MONDAY)).toRule(),
        )
        assertEquals("FREQ=MONTHLY;BYMONTHDAY=15", Recurrence(Frequency.MONTHLY, monthDay = 15).toRule())
        assertEquals(
            "FREQ=MONTHLY;BYDAY=-1FR",
            Recurrence(Frequency.MONTHLY, nthWeekday = NthWeekday(-1, DayOfWeek.FRIDAY)).toRule(),
        )
        assertEquals(
            "FREQ=YEARLY;BYMONTH=2;BYMONTHDAY=29;UNTIL=20321231;COUNT=3",
            Recurrence(Frequency.YEARLY, month = 2, monthDay = 29, until = date("2032-12-31"), count = 3).toRule(),
        )
    }

    @Test
    fun rulesSurviveAWriteAndRead() {
        val rules = listOf(
            Recurrence(Frequency.DAILY),
            Recurrence(Frequency.DAILY, interval = 5, until = date("2027-01-01")),
            Recurrence(Frequency.WEEKLY, weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY), count = 4),
            Recurrence(Frequency.MONTHLY, monthDay = 31),
            Recurrence(Frequency.MONTHLY, nthWeekday = NthWeekday(2, DayOfWeek.TUESDAY)),
            Recurrence(Frequency.YEARLY, interval = 2, month = 10, monthDay = 14),
            Recurrence(Frequency.WEEKLY, mode = RepeatMode.AFTER_COMPLETION),
        )
        for (rule in rules) {
            assertEquals(rule, Recurrence.parse(rule.toRule(), rule.mode))
        }
    }

    @Test
    fun aUntilWithATimeIsReadAsADate() {
        val parsed = Recurrence.parse("FREQ=DAILY;UNTIL=20261231T235959Z")
        assertEquals(date("2026-12-31"), parsed?.until)
    }

    @Test
    fun rulesAreReadCaseInsensitively() {
        assertEquals(
            Recurrence(Frequency.WEEKLY, weekdays = setOf(DayOfWeek.MONDAY)),
            Recurrence.parse("freq=weekly;byday=mo"),
        )
    }

    @Test
    fun brokenRulesAreRejected() {
        assertNull(Recurrence.parse(null))
        assertNull(Recurrence.parse(""))
        assertNull(Recurrence.parse("INTERVAL=2"))
        assertNull(Recurrence.parse("FREQ=HOURLY"))
        assertNull(Recurrence.parse("FREQ=DAILY;INTERVAL=0"))
        assertNull(Recurrence.parse("FREQ=DAILY;INTERVAL=abc"))
        assertNull(Recurrence.parse("FREQ=WEEKLY;BYDAY=XX"))
        assertNull(Recurrence.parse("FREQ=MONTHLY;BYDAY=5MO"))
        assertNull(Recurrence.parse("FREQ=MONTHLY;BYMONTHDAY=32"))
        assertNull(Recurrence.parse("FREQ=DAILY;COUNT=0"))
        assertNull(Recurrence.parse("FREQ=DAILY;UNTIL=soon"))
    }

    @Test
    fun startingFromADateUsesItsPattern() {
        val reference = date("2026-10-14")
        assertEquals(setOf(DayOfWeek.WEDNESDAY), Recurrence.starting(Frequency.WEEKLY, reference).weekdays)
        assertEquals(14, Recurrence.starting(Frequency.MONTHLY, reference).monthDay)
        val yearly = Recurrence.starting(Frequency.YEARLY, reference)
        assertEquals(10, yearly.month)
        assertEquals(14, yearly.monthDay)
    }
}
