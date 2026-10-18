package com.elkrrai.feather.domain.view

import com.elkrrai.feather.domain.model.Priority
import com.elkrrai.feather.domain.model.Subtask
import com.elkrrai.feather.domain.model.Tag
import com.elkrrai.feather.domain.model.Task
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskSelectionTest {

    private val today = LocalDate.of(2026, 10, 12)
    private val work = Tag("tag-work", "Work")

    private var counter = 0L

    private fun task(
        title: String,
        dueDate: LocalDate? = null,
        dueTime: LocalTime? = null,
        completed: Boolean = false,
        listId: String = "inbox",
        priority: Priority = Priority.NONE,
        pinned: Boolean = false,
        tags: List<Tag> = emptyList(),
        subtasks: List<Subtask> = emptyList(),
        notes: String = "",
    ): Task {
        counter++
        return Task(
            id = "id-$title",
            listId = listId,
            title = title,
            notes = notes,
            dueDate = dueDate,
            dueTime = dueTime,
            priority = priority,
            completedAt = if (completed) Instant.ofEpochSecond(1) else null,
            sortOrder = counter,
            createdAt = Instant.ofEpochSecond(counter),
            updatedAt = Instant.ofEpochSecond(counter),
            isPinned = pinned,
            tags = tags,
            subtasks = subtasks,
        )
    }

    private fun List<Task>.titles(scope: TaskScope) = inScope(scope, today).map { it.title }

    private val overdue = task("overdue", today.minusDays(3))
    private val dueToday = task("today", today)
    private val tomorrow = task("tomorrow", today.plusDays(1))
    private val inAWeek = task("week", today.plusDays(7))
    private val later = task("later", today.plusDays(8))
    private val undated = task("undated")
    private val done = task("done", today, completed = true)
    private val all = listOf(overdue, dueToday, tomorrow, inAWeek, later, undated, done)

    @Test
    fun todayHoldsOpenTasksDueTodayOrEarlier() {
        assertEquals(listOf("overdue", "today"), all.titles(TaskScope.Today))
    }

    @Test
    fun upcomingStartsTomorrowAndEndsAWeekOut() {
        assertEquals(listOf("tomorrow", "week"), all.titles(TaskScope.Upcoming))
    }

    @Test
    fun overdueIsStrictlyBeforeToday() {
        assertEquals(listOf("overdue"), all.titles(TaskScope.Overdue))
    }

    @Test
    fun noDateHoldsOpenUndatedTasksOnly() {
        assertEquals(listOf("undated"), all.titles(TaskScope.NoDate))
    }

    @Test
    fun completedHoldsOnlyFinishedTasks() {
        assertEquals(listOf("done"), all.titles(TaskScope.Completed))
    }

    @Test
    fun openCountIgnoresCompletedTasks() {
        assertEquals(2, all.openCountIn(TaskScope.Today, today))
        assertEquals(1, all.openCountIn(TaskScope.NoDate, today))
    }

    @Test
    fun pinnedFirstThenOpenBeforeCompleted() {
        val a = task("a")
        val b = task("b", pinned = true)
        val c = task("c", completed = true, pinned = true)

        val sorted = listOf(a, c, b).sortedWith(TaskSort.CREATED.comparator())

        assertEquals(listOf("b", "c", "a"), sorted.map { it.title })
    }

    @Test
    fun dueDateSortPutsEarliestFirstAndUndatedLast() {
        val noDate = task("none")
        val late = task("late", today.plusDays(5))
        val earlyWithTime = task("early-timed", today, LocalTime.of(8, 0))
        val earlyNoTime = task("early", today)

        val sorted = listOf(noDate, late, earlyNoTime, earlyWithTime)
            .sortedWith(TaskSort.DUE_DATE.comparator())

        assertEquals(listOf("early-timed", "early", "late", "none"), sorted.map { it.title })
    }

    @Test
    fun prioritySortPutsHighestFirst() {
        val low = task("low", priority = Priority.LOW)
        val high = task("high", priority = Priority.HIGH)
        val none = task("none")

        val sorted = listOf(low, none, high).sortedWith(TaskSort.PRIORITY.comparator())

        assertEquals(listOf("high", "low", "none"), sorted.map { it.title })
    }

    @Test
    fun groupedByDayOrdersDaysAndSkipsUndated() {
        val groups = listOf(tomorrow, undated, dueToday, task("tomorrow-2", today.plusDays(1)))
            .groupedByDay()

        assertEquals(listOf(today, today.plusDays(1)), groups.map { it.date })
        assertEquals(listOf("tomorrow", "tomorrow-2"), groups.last().tasks.map { it.title })
    }

    @Test
    fun searchMatchesTitleNotesTagsAndSubtasksIgnoringCase() {
        val tasks = listOf(
            task("Buy MILK"),
            task("Call", notes = "ask about milk"),
            task("Plan", tags = listOf(Tag("t", "Milkrun"))),
            task("Pack", subtasks = listOf(Subtask("s", "oat milk"))),
            task("Unrelated"),
        )

        val found = tasks.search("milk").map { it.title }

        assertEquals(listOf("Buy MILK", "Call", "Plan", "Pack"), found)
    }

    @Test
    fun blankSearchMatchesNothing() {
        assertTrue(all.search("   ").isEmpty())
    }
}
