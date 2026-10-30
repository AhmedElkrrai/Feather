package com.elkrrai.feather.domain.focus

import com.elkrrai.feather.domain.model.Task
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusEligibilityTest {

    private val today = LocalDate.of(2026, 10, 30)
    private val now = Instant.ofEpochSecond(1_000)

    private fun task(due: LocalDate? = today, done: Boolean = false) = Task(
        id = "t",
        title = "Write",
        dueDate = due,
        completedAt = if (done) now else null,
        sortOrder = 1,
        createdAt = now,
        updatedAt = now,
    )

    @Test
    fun only_open_tasks_due_today_can_be_focused_on() {
        assertTrue(task().canFocusOn(today))
        assertFalse(task(due = today.plusDays(1)).canFocusOn(today))
        assertFalse(task(due = today.minusDays(1)).canFocusOn(today))
        assertFalse(task(due = null).canFocusOn(today))
        assertFalse(task(done = true).canFocusOn(today))
    }

    @Test
    fun a_session_ends_when_its_task_is_gone_finished_or_moved_to_a_later_day() {
        assertTrue(focusSessionEnded(null, today))
        assertTrue(focusSessionEnded(task(done = true), today))
        assertTrue(focusSessionEnded(task(due = today.plusDays(1)), today))
        assertFalse(focusSessionEnded(task(), today))
        // overdue since midnight: the session carries on
        assertFalse(focusSessionEnded(task(due = today.minusDays(1)), today))
    }
}
