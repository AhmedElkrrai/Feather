package com.elkrrai.feather.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.elkrrai.feather.data.db.FeatherDatabase
import com.elkrrai.feather.data.db.ListEntity
import com.elkrrai.feather.data.repository.ListRepositoryImpl
import com.elkrrai.feather.data.repository.TaskRepositoryImpl
import com.elkrrai.feather.domain.model.Priority
import com.elkrrai.feather.domain.model.TaskList
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositoryTest {

    private lateinit var db: FeatherDatabase
    private lateinit var clock: TestClock
    private lateinit var tasks: TaskRepositoryImpl
    private lateinit var lists: ListRepositoryImpl

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FeatherDatabase::class.java).build()
        clock = TestClock()
        tasks = TaskRepositoryImpl(db.taskDao(), clock)
        lists = ListRepositoryImpl(db.listDao(), clock)
        lists.ensureInbox()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private val inbox = TaskList.INBOX_ID

    @Test
    fun ensureInboxIsIdempotent() = runTest {
        lists.ensureInbox()
        lists.ensureInbox()

        val all = lists.observeLists().first()
        assertEquals(1, all.size)
        assertEquals(TaskList.INBOX_ID, all.single().id)
        assertTrue(all.single().isInbox)
    }

    @Test
    fun addTaskPersistsTrimmedTitleInTheList() = runTest {
        val added = tasks.addTask(inbox, "  Buy milk  ")

        val stored = tasks.observeTasks(inbox).first().single()
        assertEquals(added.id, stored.id)
        assertEquals("Buy milk", stored.title)
        assertFalse(stored.isCompleted)
        assertEquals(Priority.NONE, stored.priority)
    }

    @Test(expected = IllegalArgumentException::class)
    fun addTaskRejectsBlankTitle() = runTest {
        tasks.addTask(inbox, "   ")
    }

    @Test
    fun openTasksComeBeforeCompletedOnes() = runTest {
        val a = tasks.addTask(inbox, "A")
        clock.advance()
        val b = tasks.addTask(inbox, "B")
        clock.advance()

        assertEquals(listOf(a.id, b.id), tasks.observeTasks(inbox).first().map { it.id })

        tasks.setCompleted(a.id, true)

        val ordered = tasks.observeTasks(inbox).first()
        assertEquals(listOf(b.id, a.id), ordered.map { it.id })
        assertTrue(ordered.last().isCompleted)
    }

    @Test
    fun completingAndReopeningATaskStampsAndClearsCompletedAt() = runTest {
        val t = tasks.addTask(inbox, "Task")
        clock.advance()

        tasks.setCompleted(t.id, true)
        assertEquals(clock.millis, tasks.getTask(t.id)!!.completedAt!!.toEpochMilli())

        tasks.setCompleted(t.id, false)
        assertNull(tasks.getTask(t.id)!!.completedAt)
    }

    @Test
    fun deletedTasksAreHiddenAndRestoreBringsThemBack() = runTest {
        val t = tasks.addTask(inbox, "Task")

        tasks.deleteTask(t.id)
        assertTrue(tasks.observeTasks(inbox).first().isEmpty())
        assertNull(tasks.getTask(t.id))
        assertNull(tasks.observeTask(t.id).first())

        tasks.restoreTask(t.id)
        assertEquals(listOf(t.id), tasks.observeTasks(inbox).first().map { it.id })
    }

    @Test
    fun saveTaskStoresEditsAndBumpsUpdatedAt() = runTest {
        val created = tasks.addTask(inbox, "Draft")
        clock.advance()

        tasks.saveTask(
            created.copy(
                title = "Final",
                notes = "details",
                dueDate = LocalDate.of(2026, 10, 12),
                dueTime = LocalTime.of(17, 0),
                priority = Priority.HIGH,
            )
        )

        val stored = tasks.getTask(created.id)!!
        assertEquals("Final", stored.title)
        assertEquals("details", stored.notes)
        assertEquals(LocalDate.of(2026, 10, 12), stored.dueDate)
        assertEquals(LocalTime.of(17, 0), stored.dueTime)
        assertEquals(Priority.HIGH, stored.priority)
        assertEquals(clock.millis, stored.updatedAt.toEpochMilli())
        assertEquals(created.createdAt, stored.createdAt)
    }

    @Test
    fun saveTaskDoesNotResurrectADeletedTask() = runTest {
        val created = tasks.addTask(inbox, "Task")
        tasks.deleteTask(created.id)

        tasks.saveTask(created.copy(title = "Edited while deleted"))

        assertNull(tasks.getTask(created.id))
        tasks.restoreTask(created.id)
        assertEquals("Edited while deleted", tasks.getTask(created.id)!!.title)
    }

    @Test
    fun tasksAreScopedToTheirList() = runTest {
        db.listDao().insertIgnore(
            ListEntity(
                id = "other",
                name = "Other",
                isInbox = false,
                sortOrder = 1,
                createdAt = clock.millis,
                updatedAt = clock.millis,
                deletedAt = null,
            )
        )
        tasks.addTask(inbox, "In inbox")
        tasks.addTask("other", "In other")

        assertEquals(listOf("In inbox"), tasks.observeTasks(inbox).first().map { it.title })
        assertEquals(listOf("In other"), tasks.observeTasks("other").first().map { it.title })
    }

    @Test
    fun observeTaskEmitsNullForUnknownId() = runTest {
        assertNull(tasks.observeTask("missing").first())
        assertNotNull(tasks.addTask(inbox, "x").id)
    }
}
