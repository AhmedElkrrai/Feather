package com.elkrrai.feather.domain.model

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class SavedFilterTest {

    private val today = LocalDate.of(2026, 10, 14)

    private fun task(
        title: String,
        listId: String = "inbox",
        due: LocalDate? = null,
        priority: Priority = Priority.NONE,
        tags: List<Tag> = emptyList(),
        completed: Boolean = false,
    ) = Task(
        id = title,
        listId = listId,
        title = title,
        dueDate = due,
        priority = priority,
        completedAt = if (completed) Instant.EPOCH else null,
        tags = tags,
        sortOrder = 1,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    private fun filter(
        listId: String? = null,
        tagId: String? = null,
        minPriority: Priority = Priority.NONE,
        due: DueWindow = DueWindow.ANY,
        hideCompleted: Boolean = true,
    ) = SavedFilter("f", "Filter", listId, tagId, minPriority, due, hideCompleted)

    private fun SavedFilter.titles(vararg tasks: Task) = tasks.filter { accepts(it, today) }.map { it.title }

    @Test
    fun anEmptyFilterLetsOpenTasksThrough() {
        val open = task("open")
        val done = task("done", completed = true)

        assertEquals(listOf("open"), filter().titles(open, done))
        assertEquals(listOf("open", "done"), filter(hideCompleted = false).titles(open, done))
    }

    @Test
    fun listAndTagMustMatch() {
        val work = task("work", listId = "w")
        val tagged = task("tagged", tags = listOf(Tag("t1", "urgent")))

        assertEquals(listOf("work"), filter(listId = "w").titles(work, tagged))
        assertEquals(listOf("tagged"), filter(tagId = "t1").titles(work, tagged))
        assertEquals(emptyList<String>(), filter(listId = "w", tagId = "t1").titles(work, tagged))
    }

    @Test
    fun priorityIsAMinimum() {
        val low = task("low", priority = Priority.LOW)
        val medium = task("medium", priority = Priority.MEDIUM)
        val high = task("high", priority = Priority.HIGH)
        val none = task("none")

        assertEquals(listOf("medium", "high"), filter(minPriority = Priority.MEDIUM).titles(low, medium, high, none))
        assertEquals(listOf("low", "medium", "high", "none"), filter().titles(low, medium, high, none))
    }

    @Test
    fun dueWindows() {
        val overdue = task("overdue", due = today.minusDays(1))
        val dueToday = task("today", due = today)
        val soon = task("soon", due = today.plusDays(7))
        val later = task("later", due = today.plusDays(8))
        val undated = task("undated")
        val all = arrayOf(overdue, dueToday, soon, later, undated)

        assertEquals(listOf("overdue"), filter(due = DueWindow.OVERDUE).titles(*all))
        assertEquals(listOf("today"), filter(due = DueWindow.TODAY).titles(*all))
        assertEquals(listOf("today", "soon"), filter(due = DueWindow.NEXT_7_DAYS).titles(*all))
        assertEquals(listOf("undated"), filter(due = DueWindow.NO_DATE).titles(*all))
        assertEquals(5, filter(due = DueWindow.ANY).titles(*all).size)
    }

    @Test
    fun dueWindowCodesSurviveStorage() {
        DueWindow.entries.forEach { assertEquals(it, DueWindow.fromCode(it.code)) }
        assertEquals(DueWindow.ANY, DueWindow.fromCode(99))
    }
}
