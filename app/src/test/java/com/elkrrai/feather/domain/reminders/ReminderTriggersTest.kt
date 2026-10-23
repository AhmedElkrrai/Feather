package com.elkrrai.feather.domain.reminders

import com.elkrrai.feather.domain.model.Reminder
import com.elkrrai.feather.domain.model.ReminderKind
import com.elkrrai.feather.domain.model.Task
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderTriggersTest {

    private val cairo = ZoneId.of("Africa/Cairo") // UTC+3 in October 2026 (summer time)
    private val utc = ZoneId.of("UTC")
    private val defaultTime = LocalTime.of(9, 0)
    private val day = LocalDate.of(2026, 10, 14)

    private fun instant(date: LocalDate, time: LocalTime, zone: ZoneId): Instant =
        LocalDateTime.of(date, time).atZone(zone).toInstant()

    private fun reminder(
        id: String = "r",
        kind: ReminderKind = ReminderKind.BEFORE_DUE,
        offset: Int = 0,
        at: LocalDateTime? = null,
        fired: Instant? = null,
        updatedAt: Instant = Instant.EPOCH,
    ) = Reminder(id, kind, offset, at, fired, updatedAt)

    private fun task(
        id: String = "t",
        dueDate: LocalDate? = day,
        dueTime: LocalTime? = LocalTime.of(17, 0),
        completed: Boolean = false,
        reminders: List<Reminder> = emptyList(),
    ) = Task(
        id = id,
        title = "Task $id",
        dueDate = dueDate,
        dueTime = dueTime,
        completedAt = if (completed) Instant.ofEpochSecond(1) else null,
        sortOrder = 0,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
        reminders = reminders,
    )

    // --- triggerAt ---

    @Test
    fun beforeDueAtZeroFiresAtTheDueMoment() {
        val trigger = reminder(offset = 0).triggerAt(task(), defaultTime, cairo)

        assertEquals(instant(day, LocalTime.of(17, 0), cairo), trigger)
    }

    @Test
    fun beforeDueSubtractsTheOffset() {
        val trigger = reminder(offset = 90).triggerAt(task(), defaultTime, cairo)

        assertEquals(instant(day, LocalTime.of(15, 30), cairo), trigger)
    }

    @Test
    fun anOffsetCanCrossMidnight() {
        val trigger = reminder(offset = 24 * 60).triggerAt(task(), defaultTime, cairo)

        assertEquals(instant(day.minusDays(1), LocalTime.of(17, 0), cairo), trigger)
    }

    @Test
    fun aDateOnlyTaskUsesTheDefaultReminderTime() {
        val trigger = reminder(offset = 0).triggerAt(task(dueTime = null), defaultTime, cairo)

        assertEquals(instant(day, defaultTime, cairo), trigger)
    }

    @Test
    fun beforeDueWithoutADueDateHasNoTrigger() {
        assertNull(reminder().triggerAt(task(dueDate = null, dueTime = null), defaultTime, cairo))
    }

    @Test
    fun absoluteAndSnoozeRemindersUseTheirOwnMoment() {
        val moment = LocalDateTime.of(day, LocalTime.of(8, 15))
        val noDueDate = task(dueDate = null, dueTime = null)

        val absolute = reminder(kind = ReminderKind.AT_TIME, at = moment).triggerAt(noDueDate, defaultTime, cairo)
        val snooze = reminder(kind = ReminderKind.SNOOZE, at = moment).triggerAt(noDueDate, defaultTime, cairo)

        assertEquals(instant(day, LocalTime.of(8, 15), cairo), absolute)
        assertEquals(absolute, snooze)
    }

    @Test
    fun localTimesFollowTheTimeZone() {
        val inCairo = reminder().triggerAt(task(), defaultTime, cairo)
        val inUtc = reminder().triggerAt(task(), defaultTime, utc)

        // 17:00 in Cairo (UTC+3) happens three hours before 17:00 in UTC.
        assertEquals(Duration.ofHours(3), Duration.between(inCairo, inUtc))
    }

    // --- planReminders ---

    private val now = instant(day, LocalTime.of(12, 0), cairo)

    private fun plan(vararg tasks: Task, at: Instant = now) =
        planReminders(tasks.toList(), at, defaultTime, cairo)

    @Test
    fun upcomingRemindersGiveTheEarliestNextTrigger() {
        val soon = task("soon", reminders = listOf(reminder(offset = 60))) // 16:00
        val later = task("later", reminders = listOf(reminder(offset = 0))) // 17:00

        val plan = plan(later, soon)

        assertTrue(plan.due.isEmpty())
        assertEquals(instant(day, LocalTime.of(16, 0), cairo), plan.next)
    }

    @Test
    fun noRemindersMeansNothingToDo() {
        val plan = plan(task(reminders = emptyList()))

        assertTrue(plan.due.isEmpty())
        assertNull(plan.next)
    }

    @Test
    fun aTriggerThatJustPassedIsDueAndKeepsItsTime() {
        val t = task(reminders = listOf(reminder(offset = 6 * 60))) // 11:00, one hour ago

        val plan = plan(t)

        assertEquals(listOf(instant(day, LocalTime.of(11, 0), cairo)), plan.due.map { it.trigger })
        assertEquals(t.id, plan.due.single().task.id)
        assertNull(plan.next)
    }

    @Test
    fun aTriggerOneSecondAheadCountsAsDue() {
        val trigger = now.plusSeconds(1)
        val t = task(
            dueDate = null,
            dueTime = null,
            reminders = listOf(
                reminder(kind = ReminderKind.AT_TIME, at = LocalDateTime.ofInstant(trigger, cairo)),
            ),
        )

        assertEquals(1, plan(t).due.size)
    }

    @Test
    fun missedRemindersAreCaughtUpOnlyWithinTheWindow() {
        val twentyHoursAgo = LocalDateTime.ofInstant(now.minus(Duration.ofHours(20)), cairo)
        val thirtyHoursAgo = LocalDateTime.ofInstant(now.minus(Duration.ofHours(30)), cairo)
        val recent = task("recent", dueDate = null, dueTime = null, reminders = listOf(reminder("a", ReminderKind.AT_TIME, at = twentyHoursAgo)))
        val old = task("old", dueDate = null, dueTime = null, reminders = listOf(reminder("b", ReminderKind.AT_TIME, at = thirtyHoursAgo)))

        val plan = plan(recent, old)

        assertEquals(listOf("recent"), plan.due.map { it.task.id })
    }

    @Test
    fun aReminderAlreadyShownForThisTriggerIsSkipped() {
        val trigger = instant(day, LocalTime.of(11, 0), cairo)
        val shown = task(reminders = listOf(reminder(offset = 6 * 60, fired = trigger)))

        val plan = plan(shown)

        assertTrue(plan.due.isEmpty())
        assertNull(plan.next)
    }

    @Test
    fun movingTheDueTimeMakesTheReminderFireAgain() {
        val oldTrigger = instant(day, LocalTime.of(11, 0), cairo)
        // The task is now due at 18:00, so the reminder 6 hours before moves to 12:00 -> later today.
        val moved = task(
            dueTime = LocalTime.of(18, 30),
            reminders = listOf(reminder(offset = 6 * 60, fired = oldTrigger)),
        )

        val plan = plan(moved)

        assertEquals(instant(day, LocalTime.of(12, 30), cairo), plan.next)
    }

    @Test
    fun aReminderEditedIntoThePastStaysQuiet() {
        val justNow = now.minusSeconds(30)
        val edited = task(
            dueDate = null,
            dueTime = null,
            reminders = listOf(
                reminder(
                    kind = ReminderKind.AT_TIME,
                    at = LocalDateTime.ofInstant(now.minus(Duration.ofHours(2)), cairo),
                    updatedAt = justNow, // saved after its trigger time
                ),
            ),
        )

        val plan = plan(edited)

        assertTrue(plan.due.isEmpty())
        assertNull(plan.next)
    }

    @Test
    fun completedTasksAreSkipped() {
        val done = task(completed = true, reminders = listOf(reminder(offset = 0)))

        val plan = plan(done)

        assertTrue(plan.due.isEmpty())
        assertNull(plan.next)
    }

    @Test
    fun dueRemindersAreOrderedByTriggerTime() {
        val later = task("later", reminders = listOf(reminder("a", offset = 5 * 60 + 30))) // 11:30
        val earlier = task("earlier", reminders = listOf(reminder("b", offset = 6 * 60))) // 11:00

        val plan = plan(later, earlier)

        assertEquals(listOf("earlier", "later"), plan.due.map { it.task.id })
    }

    @Test
    fun sameScheduleIgnoresIdsAndBookkeeping() {
        val a = reminder("a", offset = 30, fired = Instant.ofEpochSecond(5), updatedAt = Instant.ofEpochSecond(9))
        val b = reminder("b", offset = 30)

        assertTrue(a.sameSchedule(b))
        assertTrue(!a.sameSchedule(b.copy(offsetMinutes = 31)))
    }

    // --- nextDailySummary ---

    @Test
    fun theSummaryIsTodayWhenItsTimeHasNotPassed() {
        val morning = instant(day, LocalTime.of(6, 30), cairo)

        val next = nextDailySummary(morning, LocalTime.of(8, 0), cairo)

        assertEquals(instant(day, LocalTime.of(8, 0), cairo), next)
    }

    @Test
    fun theSummaryIsTomorrowOnceItsTimeHasPassed() {
        val afternoon = instant(day, LocalTime.of(15, 0), cairo)

        val next = nextDailySummary(afternoon, LocalTime.of(8, 0), cairo)

        assertEquals(instant(day.plusDays(1), LocalTime.of(8, 0), cairo), next)
    }

    @Test
    fun theSummaryExactlyAtItsTimeWaitsForTomorrow() {
        val exactly = instant(day, LocalTime.of(8, 0), cairo)

        val next = nextDailySummary(exactly, LocalTime.of(8, 0), cairo)

        assertEquals(instant(day.plusDays(1), LocalTime.of(8, 0), cairo), next)
    }

    @Test
    fun theSummaryFollowsTheTimeZone() {
        val now = instant(day, LocalTime.of(6, 30), cairo)

        val inCairo = nextDailySummary(now, LocalTime.of(8, 0), cairo)
        val inUtc = nextDailySummary(now, LocalTime.of(8, 0), utc)

        // 08:00 in Cairo (UTC+3) is 05:00 UTC, which already passed, so UTC waits for 08:00 tomorrow.
        assertTrue(inUtc.isAfter(inCairo))
    }
}
