package com.elkrrai.feather.data.db

import com.elkrrai.feather.domain.model.Priority
import com.elkrrai.feather.domain.model.Task
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MappersTest {

    private val task = Task(
        id = "t1",
        title = "Buy milk",
        notes = "2 liters",
        dueDate = LocalDate.of(2026, 10, 12),
        dueTime = LocalTime.of(17, 5),
        priority = Priority.HIGH,
        completedAt = Instant.ofEpochMilli(5_000),
        sortOrder = 10,
        createdAt = Instant.ofEpochMilli(1_000),
        updatedAt = Instant.ofEpochMilli(2_000),
        isPinned = true,
    )

    private fun entity(
        id: String,
        parentTaskId: String? = null,
        title: String = id,
        sortOrder: Long = 0,
        completedAt: Long? = null,
    ) = TaskEntity(
        id = id,
        listId = "l1",
        title = title,
        notes = "",
        dueDate = null,
        dueTime = null,
        priority = 0,
        completedAt = completedAt,
        sortOrder = sortOrder,
        createdAt = 0,
        updatedAt = 0,
        deletedAt = null,
        parentTaskId = parentTaskId,
    )

    private fun tag(id: String, name: String, deletedAt: Long? = null) =
        TagEntity(id = id, name = name, createdAt = 0, updatedAt = 0, deletedAt = deletedAt)

    @Test
    fun taskRoundTripsThroughEntity() {
        assertEquals(task, task.toEntity().toDomain())
    }

    @Test
    fun everyTaskIsKeptInTheInbox() {
        assertEquals(INBOX_LIST_ID, task.toEntity().listId)
    }

    @Test
    fun dueDateAndTimeAreStoredAsFloatingLocalText() {
        val entity = task.toEntity()
        assertEquals("2026-10-12", entity.dueDate)
        assertEquals("17:05", entity.dueTime)
    }

    @Test
    fun missingDueDateAndTimeStayNull() {
        val entity = task.copy(dueDate = null, dueTime = null, completedAt = null).toEntity()
        assertNull(entity.dueDate)
        assertNull(entity.dueTime)
        assertNull(entity.completedAt)
        assertNull(entity.toDomain().dueDate)
    }

    @Test
    fun unknownPriorityLevelFallsBackToNone() {
        assertEquals(Priority.NONE, Priority.fromLevel(99))
        assertEquals(Priority.NONE, task.toEntity().copy(priority = -1).toDomain().priority)
    }

    @Test
    fun deletedAtIsWrittenToTheEntityAndReadBackAsAnInstant() {
        val entity = task.toEntity(deletedAt = 42L)

        assertEquals(42L, entity.deletedAt)
        assertEquals(Instant.ofEpochMilli(42L), entity.toDomain().deletedAt)
    }

    @Test
    fun taskEntitiesAreTopLevelAndKeepThePinFlag() {
        val entity = task.toEntity()

        assertNull(entity.parentTaskId)
        assertTrue(entity.isPinned)
    }

    @Test
    fun subtaskRowsGroupUnderTheirParentInOrder() {
        val rows = listOf(
            TaskWithTags(entity("parent"), emptyList()),
            TaskWithTags(entity("s2", parentTaskId = "parent", title = "Second", sortOrder = 2), emptyList()),
            TaskWithTags(entity("s1", parentTaskId = "parent", title = "First", sortOrder = 1, completedAt = 9), emptyList()),
            TaskWithTags(entity("other"), emptyList()),
        )

        val tasks = rows.toDomainTasks()

        assertEquals(listOf("parent", "other"), tasks.map { it.id })
        val subtasks = tasks.first().subtasks
        assertEquals(listOf("First", "Second"), subtasks.map { it.title })
        assertEquals(listOf(true, false), subtasks.map { it.isCompleted })
        assertTrue(tasks.last().subtasks.isEmpty())
    }

    @Test
    fun deletedTagsAreDroppedAndTagsAreSortedByName() {
        val rows = listOf(
            TaskWithTags(
                entity("t"),
                listOf(tag("b", "Zebra"), tag("a", "apple"), tag("c", "gone", deletedAt = 5)),
            ),
        )

        val tags = rows.toDomainTasks().single().tags

        assertEquals(listOf("apple", "Zebra"), tags.map { it.name })
    }
}
