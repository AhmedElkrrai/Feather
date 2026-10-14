package com.elkrrai.feather.domain.model

import com.elkrrai.feather.domain.recurrence.Frequency
import com.elkrrai.feather.domain.recurrence.Recurrence
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskRestingTest {
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 10, 15)
    private val noon = today.atTime(12, 0).toInstant(zone)

    private fun task(
        due: LocalDate? = today.plusDays(1),
        recurrence: Recurrence? = Recurrence(Frequency.DAILY),
        lastCompletedAt: Instant? = noon,
        completedAt: Instant? = null,
    ) = Task(
        id = "t", listId = "l", title = "Get a life", dueDate = due, recurrence = recurrence,
        lastCompletedAt = lastCompletedAt, completedAt = completedAt, sortOrder = 0,
        createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH,
        subtasks = listOf(Subtask("a", "One"), Subtask("b", "Two")),
    )

    @Test
    fun aRepeatingTaskFinishedTodayAndDueLaterRests() {
        assertTrue(task().isRestingUntilNext(today, zone))
    }

    @Test
    fun itWakesWhenItsDateArrives() {
        assertFalse(task(due = today).isRestingUntilNext(today, zone))
    }

    @Test
    fun aTaskFinishedOnAnEarlierDayDoesNotRest() {
        assertFalse(task(lastCompletedAt = noon.minusSeconds(86_400)).isRestingUntilNext(today, zone))
    }

    @Test
    fun aOneOffOrNeverFinishedTaskDoesNotRest() {
        assertFalse(task(recurrence = null).isRestingUntilNext(today, zone))
        assertFalse(task(lastCompletedAt = null).isRestingUntilNext(today, zone))
    }

    @Test
    fun aRestingTaskIsShownFinishedWithItsChecklistTicked() {
        val shown = task().asFinishedToday()

        assertEquals(noon, shown.completedAt)
        assertTrue(shown.subtasks.all { it.isCompleted })
    }
}
