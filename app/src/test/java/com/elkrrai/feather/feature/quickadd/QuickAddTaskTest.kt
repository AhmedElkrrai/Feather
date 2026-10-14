package com.elkrrai.feather.feature.quickadd

import com.elkrrai.feather.domain.model.Priority
import com.elkrrai.feather.domain.model.TaskList
import com.elkrrai.feather.domain.quickadd.ParsedQuickAdd
import com.elkrrai.feather.domain.recurrence.Frequency
import com.elkrrai.feather.domain.recurrence.Recurrence
import com.elkrrai.feather.testutil.FakeTagRepository
import com.elkrrai.feather.testutil.FakeTaskRepository
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickAddTaskTest {

    private val tasks = FakeTaskRepository()
    private val tags = FakeTagRepository()
    private val quickAdd = QuickAddTask(tasks, tags)
    private val inbox = TaskList.INBOX_ID
    private val tomorrow = LocalDate.of(2026, 10, 15)

    @Test
    fun aPlainTitleMakesAPlainTask() = runTest {
        val task = quickAdd(ParsedQuickAdd(title = "Buy milk"), inbox)

        val stored = tasks.getTask(task.id)!!
        assertEquals("Buy milk", stored.title)
        assertEquals(inbox, stored.listId)
        assertNull(stored.dueDate)
        assertTrue(stored.tags.isEmpty())
    }

    @Test
    fun everythingTheTextSaidIsSaved() = runTest {
        val rule = Recurrence(Frequency.WEEKLY)
        val parsed = ParsedQuickAdd(
            title = "Call mom",
            dueDate = tomorrow,
            dueTime = LocalTime.of(17, 0),
            priority = Priority.HIGH,
            tags = listOf("family", "calls"),
            recurrence = rule,
        )

        val task = quickAdd(parsed, inbox)

        val stored = tasks.getTask(task.id)!!
        assertEquals(tomorrow, stored.dueDate)
        assertEquals(LocalTime.of(17, 0), stored.dueTime)
        assertEquals(Priority.HIGH, stored.priority)
        assertEquals(setOf("family", "calls"), stored.tags.map { it.name }.toSet())
        assertEquals(rule, stored.recurrence)
        assertEquals(setOf("family", "calls"), tags.observeTags().first().map { it.name }.toSet())
    }

    @Test
    fun anExistingTagIsReusedNotDuplicated() = runTest {
        val existing = tags.createTag("Family")

        val task = quickAdd(ParsedQuickAdd(title = "Call", tags = listOf("family")), inbox)

        assertEquals(listOf(existing.id), tasks.getTask(task.id)!!.tags.map { it.id })
        assertEquals(1, tags.observeTags().first().size)
    }

    @Test
    fun theViewsDateIsUsedOnlyWhenTheTextHasNone() = runTest {
        val noDate = quickAdd(ParsedQuickAdd(title = "A"), inbox, fallbackDate = tomorrow)
        assertEquals(tomorrow, tasks.getTask(noDate.id)!!.dueDate)

        val written = LocalDate.of(2026, 11, 2)
        val withDate = quickAdd(ParsedQuickAdd(title = "B", dueDate = written), inbox, fallbackDate = tomorrow)
        assertEquals(written, tasks.getTask(withDate.id)!!.dueDate)
    }

    @Test
    fun aTimeAndARepeatAreDroppedWhenThereIsNoDueDate() = runTest {
        val parsed = ParsedQuickAdd(
            title = "Odd",
            dueTime = LocalTime.of(9, 0),
            recurrence = Recurrence(Frequency.DAILY),
            priority = Priority.LOW,
        )

        val stored = tasks.getTask(quickAdd(parsed, inbox).id)!!

        assertNull(stored.dueTime)
        assertNull(stored.recurrence)
        assertEquals(Priority.LOW, stored.priority)
    }

    @Test
    fun extraTagsAreAddedAndNotDuplicated() = runTest {
        val viewed = tags.createTag("work")

        val task = quickAdd(ParsedQuickAdd(title = "Report", tags = listOf("Work", "urgent")), inbox, extraTags = listOf(viewed))

        assertEquals(setOf("work", "urgent"), tasks.getTask(task.id)!!.tags.map { it.name }.toSet())
        assertEquals(2, tasks.getTask(task.id)!!.tags.size)
    }
}
