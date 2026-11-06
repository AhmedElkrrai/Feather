package com.elkrrai.feather.domain.reminders

import com.elkrrai.feather.domain.habit.DayTime
import com.elkrrai.feather.domain.habit.Habit
import com.elkrrai.feather.domain.habit.HabitGoal
import com.elkrrai.feather.domain.habit.HabitKind
import com.elkrrai.feather.domain.habit.HabitStage
import com.elkrrai.feather.domain.habit.HabitState
import com.elkrrai.feather.domain.habit.TimeMode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitTriggersTest {

    private val zone = ZoneId.of("UTC")

    // 2026-10-31 is a Saturday, the first day of the app's week; the 2nd is a Monday.
    private val week = LocalDate.of(2026, 10, 31)
    private val today = week.plusDays(2)

    private val fajr = HabitStage("f", "Fajr", DayTime(TimeMode.WINDOW, 270, 360))
    private val dhuhr = HabitStage("d", "Dhuhr", DayTime(TimeMode.AT, 720))
    private val isha = HabitStage("i", "Isha")

    private fun at(date: LocalDate, hour: Int, minute: Int = 0): Instant =
        LocalDateTime.of(date, java.time.LocalTime.of(hour, minute)).atZone(zone).toInstant()

    private fun habit(remind: Boolean = true, kind: HabitKind = HabitKind.DAYS, time: DayTime = DayTime()) =
        Habit("h", "Pray", kind, sortOrder = 0, createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH, remind = remind, time = time)

    private fun staged(
        done: Set<String> = emptySet(),
        remind: Boolean = true,
        target: Int = 7,
        entries: Map<LocalDate, Int> = emptyMap(),
    ) = HabitState(
        habit = habit(remind),
        goals = listOf(HabitGoal(week, target, 3, listOf("f", "d", "i"))),
        entries = entries,
        stages = listOf(fajr, dhuhr, isha).associateBy { it.id },
        stageLog = mapOf(today to done),
    )

    private fun plan(state: HabitState, now: Instant, shown: Set<String> = emptySet()) =
        planHabitReminders(listOf(state), now, zone, shown)

    @Test
    fun theNextReminderIsTheStartOfTheNextTime() {
        val plan = plan(staged(), at(today, 7))

        assertTrue(plan.due.isEmpty())
        assertEquals(at(today, 12), plan.next)
    }

    @Test
    fun aTimeThatHasComeIsDue() {
        val plan = plan(staged(), at(today, 12, 1))

        assertEquals(listOf("d"), plan.due.map { it.stage?.id })
        assertEquals(at(today, 12), plan.due.single().trigger)
        // Fajr was hours ago: too late to be worth a nudge
        assertEquals(at(today.plusDays(1), 4, 30), plan.next)
    }

    @Test
    fun aReminderThatIsMoreThanTwoHoursLateIsDropped() {
        val plan = plan(staged(), at(today, 14, 30))

        assertTrue(plan.due.isEmpty())
    }

    @Test
    fun aStageThatIsDoneGetsNoReminder() {
        val plan = plan(staged(done = setOf("d")), at(today, 12, 1))

        assertTrue(plan.due.isEmpty())
    }

    @Test
    fun aReminderThatWasShownIsNotShownAgain() {
        val key = habitReminderKey("h", "d", today)

        val plan = plan(staged(), at(today, 12, 1), shown = setOf(key))

        assertTrue(plan.due.isEmpty())
    }

    @Test
    fun aHabitWithRemindSwitchedOffOrAnAmountHabitGetsNone() {
        assertTrue(plan(staged(remind = false), at(today, 12, 1)).due.isEmpty())
        assertNull(plan(staged(remind = false), at(today, 7)).next)

        val amount = HabitState(habit(kind = HabitKind.AMOUNT, time = DayTime(TimeMode.AT, 720)), listOf(HabitGoal(week, 20)), emptyMap())
        assertNull(plan(amount, at(today, 7)).next)
    }

    @Test
    fun aHabitWithoutStagesRemindsAtItsOwnTime() {
        val plain = HabitState(habit(time = DayTime(TimeMode.WINDOW, 1020, 1140)), listOf(HabitGoal(week, 3)), emptyMap())

        val plan = plan(plain, at(today, 9))

        assertEquals(at(today, 17), plan.next)
        assertNull(plan.due.firstOrNull()?.stage)
    }

    @Test
    fun aFinishedDayHasNoMoreReminders() {
        // all three stages done, though only two are needed: the day is finished
        val done = staged(done = setOf("f", "d"))

        val plan = plan(done, at(today, 7))

        // today is finished, so the next one is tomorrow's first
        assertTrue(plan.due.isEmpty())
        assertEquals(at(today.plusDays(1), 4, 30), plan.next)
    }

    @Test
    fun aHabitWhoseWeekIsMetByOtherDaysRests() {
        // a one-day goal that was met on the first day of the week
        val met = staged(target = 1, entries = mapOf(week to 3))

        val plan = plan(met, at(today, 7))

        // tomorrow is still this week (Monday), so there is nothing until the next week starts
        assertNull(plan.next)
    }

    @Test
    fun theKeyNamesTheHabitTheStageAndTheDay() {
        assertEquals("h|d|2026-11-02", habitReminderKey("h", "d", today))
        assertEquals("h|-|2026-11-02", habitReminderKey("h", null, today))
    }
}
