package com.elkrrai.feather.domain.backup

import com.elkrrai.feather.domain.model.Priority
import com.elkrrai.feather.domain.model.Subtask
import com.elkrrai.feather.domain.model.Tag
import com.elkrrai.feather.domain.model.Task
import com.elkrrai.feather.domain.recurrence.Frequency
import com.elkrrai.feather.domain.recurrence.Recurrence
import com.elkrrai.feather.domain.recurrence.RepeatMode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskCsvTest {

    private fun task(title: String, block: Task.() -> Task = { this }) = Task(
        id = title,
        listId = "l",
        title = title,
        sortOrder = 1,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    ).block()

    private val names = mapOf("l" to "Home")

    private fun lines(csv: String) = csv.removePrefix("﻿").split("\r\n").filter { it.isNotEmpty() }

    @Test
    fun startsWithAByteOrderMarkAndTheHeader() {
        val csv = TaskCsv.build(emptyList(), names)

        assertTrue(csv.startsWith("﻿"))
        assertEquals(listOf(TaskCsv.HEADER.joinToString(",")), lines(csv))
    }

    @Test
    fun aTaskBecomesOneRow() {
        val full = task("Call mom") {
            copy(
                notes = "Ask about the trip",
                dueDate = LocalDate.of(2026, 10, 15),
                dueTime = LocalTime.of(17, 0),
                priority = Priority.HIGH,
                tags = listOf(Tag("t1", "family"), Tag("t2", "calls")),
                subtasks = listOf(Subtask("s1", "Dial", isCompleted = true), Subtask("s2", "Talk")),
                recurrence = Recurrence(Frequency.WEEKLY),
            )
        }

        val row = lines(TaskCsv.build(listOf(full), names))[1]

        assertEquals(
            "Call mom,Ask about the trip,Home,2026-10-15,17:00,high,open,,family; calls,FREQ=WEEKLY,[x] Dial | [ ] Talk",
            row,
        )
    }

    @Test
    fun finishedTasksCarryTheirCompletionTime() {
        val done = task("Done") { copy(completedAt = Instant.parse("2026-10-14T08:30:00Z")) }

        assertTrue(lines(TaskCsv.build(listOf(done), names))[1].contains(",done,2026-10-14T08:30:00Z,"))
    }

    @Test
    fun repeatAfterCompletionIsMarked() {
        val rule = Recurrence(Frequency.DAILY, interval = 2, mode = RepeatMode.AFTER_COMPLETION)

        val row = lines(TaskCsv.build(listOf(task("Water") { copy(recurrence = rule) }), names))[1]

        assertTrue(row.contains("FREQ=DAILY;INTERVAL=2 (after completion)"))
    }

    @Test
    fun cellsWithCommasQuotesAndLineBreaksAreQuoted() {
        val tricky = task("Buy milk, eggs") { copy(notes = "He said \"now\"\nand later") }

        val csv = TaskCsv.build(listOf(tricky), names)

        assertTrue(csv.contains("\"Buy milk, eggs\",\"He said \"\"now\"\"\nand later\",Home"))
    }

    @Test
    fun arabicTextIsKeptAsIs() {
        val csv = TaskCsv.build(listOf(task("اتصل بأمي")), mapOf("l" to "المنزل"))

        assertTrue(csv.contains("اتصل بأمي,,المنزل"))
    }

    @Test
    fun anUnknownListIsLeftBlank() {
        val row = lines(TaskCsv.build(listOf(task("Lost") { copy(listId = "gone") }), names))[1]

        assertTrue(row.startsWith("Lost,,,"))
    }
}
