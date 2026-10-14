package com.elkrrai.feather.domain.view

import com.elkrrai.feather.domain.model.Task
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskSortOrderTest {

    private fun task(title: String, createdAt: Long, sortOrder: Long, pinned: Boolean = false) = Task(
        id = title,
        listId = "inbox",
        title = title,
        sortOrder = sortOrder,
        createdAt = Instant.ofEpochSecond(createdAt),
        updatedAt = Instant.ofEpochSecond(createdAt),
        isPinned = pinned,
    )

    // Created in the order a, b, c; then dragged into the order c, a, b.
    private val a = task("a", createdAt = 1, sortOrder = 3)
    private val b = task("b", createdAt = 2, sortOrder = 4)
    private val c = task("c", createdAt = 3, sortOrder = 2)

    @Test
    fun createdOrderIgnoresDragging() {
        val sorted = listOf(c, b, a).sortedWith(TaskSort.CREATED.comparator())

        assertEquals(listOf("a", "b", "c"), sorted.map { it.title })
    }

    @Test
    fun manualOrderFollowsTheDraggedValues() {
        val sorted = listOf(a, b, c).sortedWith(TaskSort.MANUAL.comparator())

        assertEquals(listOf("c", "a", "b"), sorted.map { it.title })
    }

    @Test
    fun pinnedTasksStillComeFirstInManualOrder() {
        val pinned = a.copy(isPinned = true)

        val sorted = listOf(b, c, pinned).sortedWith(TaskSort.MANUAL.comparator())

        assertEquals(listOf("a", "c", "b"), sorted.map { it.title })
    }
}
