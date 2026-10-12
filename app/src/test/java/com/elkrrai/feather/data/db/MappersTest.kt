package com.elkrrai.feather.data.db

import com.elkrrai.feather.domain.model.Priority
import com.elkrrai.feather.domain.model.Task
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MappersTest {

    private val task = Task(
        id = "t1",
        listId = "l1",
        title = "Buy milk",
        notes = "2 liters",
        dueDate = LocalDate.of(2026, 10, 12),
        dueTime = LocalTime.of(17, 5),
        priority = Priority.HIGH,
        completedAt = Instant.ofEpochMilli(5_000),
        sortOrder = 10,
        createdAt = Instant.ofEpochMilli(1_000),
        updatedAt = Instant.ofEpochMilli(2_000),
    )

    @Test
    fun taskRoundTripsThroughEntity() {
        assertEquals(task, task.toEntity().toDomain())
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
    fun deletedAtIsOnlyOnTheEntity() {
        assertEquals(42L, task.toEntity(deletedAt = 42L).deletedAt)
    }
}
