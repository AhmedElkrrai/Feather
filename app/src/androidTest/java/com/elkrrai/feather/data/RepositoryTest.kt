package com.elkrrai.feather.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.elkrrai.feather.data.db.FeatherDatabase
import com.elkrrai.feather.data.db.RoomTransactionRunner
import com.elkrrai.feather.data.repository.ListRepositoryImpl
import com.elkrrai.feather.data.repository.ReminderRepositoryImpl
import com.elkrrai.feather.data.repository.TagRepositoryImpl
import com.elkrrai.feather.data.repository.TaskRepositoryImpl
import com.elkrrai.feather.domain.model.Priority
import com.elkrrai.feather.domain.model.Reminder
import com.elkrrai.feather.domain.model.ReminderKind
import com.elkrrai.feather.domain.model.Subtask
import com.elkrrai.feather.domain.model.TaskList
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
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
    private lateinit var tags: TagRepositoryImpl
    private lateinit var reminderRepo: ReminderRepositoryImpl

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FeatherDatabase::class.java).build()
        clock = TestClock()
        val tx = RoomTransactionRunner(db)
        tasks = TaskRepositoryImpl(db.taskDao(), db.reminderDao(), db.completionDao(), tx, clock)
        reminderRepo = ReminderRepositoryImpl(db.reminderDao(), tx, clock)
        lists = ListRepositoryImpl(db.listDao(), db.folderDao(), db.taskDao(), tx, clock)
        tags = TagRepositoryImpl(db.tagDao(), clock)
        lists.ensureInbox()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private val inbox = TaskList.INBOX_ID

    private suspend fun allTasks() = tasks.observeTasks().first()

    // --- Tasks ---

    @Test
    fun addTaskPersistsTrimmedTitleAndDueDate() = runTest {
        val added = tasks.addTask(inbox, "  Buy milk  ", LocalDate.of(2026, 10, 20))

        val stored = allTasks().single()
        assertEquals(added.id, stored.id)
        assertEquals("Buy milk", stored.title)
        assertEquals(LocalDate.of(2026, 10, 20), stored.dueDate)
        assertFalse(stored.isCompleted)
        assertEquals(Priority.NONE, stored.priority)
    }

    @Test(expected = IllegalArgumentException::class)
    fun addTaskRejectsBlankTitle() = runTest {
        tasks.addTask(inbox, "   ")
    }

    @Test
    fun completingAndReopeningStampsAndClearsCompletedAt() = runTest {
        val t = tasks.addTask(inbox, "Task")
        clock.advance()

        tasks.setCompleted(t.id, true)
        assertEquals(clock.millis, tasks.getTask(t.id)!!.completedAt!!.toEpochMilli())

        tasks.setCompleted(t.id, false)
        assertNull(tasks.getTask(t.id)!!.completedAt)
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
    fun tasksKnowTheirList() = runTest {
        val work = lists.createList("Work")
        tasks.addTask(inbox, "In inbox")
        tasks.addTask(work.id, "At work")

        val byList = allTasks().groupBy({ it.listId }, { it.title })

        assertEquals(listOf("In inbox"), byList[inbox])
        assertEquals(listOf("At work"), byList[work.id])
    }

    @Test
    fun pinningIsStored() = runTest {
        val t = tasks.addTask(inbox, "Task")

        tasks.setPinned(t.id, true)

        assertTrue(tasks.getTask(t.id)!!.isPinned)
    }

    // --- Subtasks and tags ---

    @Test
    fun saveTaskDetailsStoresSubtasksInOrderAndDropsBlankOnes() = runTest {
        val t = tasks.addTask(inbox, "Trip")

        tasks.saveTaskDetails(
            t.copy(
                subtasks = listOf(
                    Subtask("s1", "Passport"),
                    Subtask("s2", "   "),
                    Subtask("s3", "Tickets", isCompleted = true),
                )
            )
        )

        val stored = tasks.getTask(t.id)!!
        assertEquals(listOf("Passport", "Tickets"), stored.subtasks.map { it.title })
        assertEquals(listOf(false, true), stored.subtasks.map { it.isCompleted })
        assertEquals(1, allTasks().size) // subtasks are not separate top-level tasks
    }

    @Test
    fun removedSubtasksAreDeletedAndEditsAreKept() = runTest {
        val t = tasks.addTask(inbox, "Trip")
        tasks.saveTaskDetails(t.copy(subtasks = listOf(Subtask("s1", "A"), Subtask("s2", "B"))))

        tasks.saveTaskDetails(t.copy(subtasks = listOf(Subtask("s2", "B edited", isCompleted = true))))

        val stored = tasks.getTask(t.id)!!
        assertEquals(listOf("s2"), stored.subtasks.map { it.id })
        assertEquals("B edited", stored.subtasks.single().title)
        assertTrue(stored.subtasks.single().isCompleted)
    }

    @Test
    fun deletingATaskHidesItsSubtasksAndRestoreBringsThemBack() = runTest {
        val t = tasks.addTask(inbox, "Trip")
        tasks.saveTaskDetails(t.copy(subtasks = listOf(Subtask("s1", "A"), Subtask("s2", "B"))))
        // "B" was removed on its own earlier, so it must stay deleted after the restore.
        tasks.saveTaskDetails(t.copy(subtasks = listOf(Subtask("s1", "A"))))
        clock.advance()

        tasks.deleteTask(t.id)
        assertTrue(allTasks().isEmpty())
        assertEquals(listOf(t.id), tasks.observeTrash().first().map { it.id })

        clock.advance()
        tasks.restoreTask(t.id)

        assertEquals(listOf("A"), tasks.getTask(t.id)!!.subtasks.map { it.title })
        assertTrue(tasks.observeTrash().first().isEmpty())
    }

    @Test
    fun saveTaskDetailsReplacesTags() = runTest {
        val work = tags.createTag("work")
        val home = tags.createTag("home")
        val t = tasks.addTask(inbox, "Task")

        tasks.saveTaskDetails(t.copy(tags = listOf(work, home)))
        assertEquals(setOf("work", "home"), tasks.getTask(t.id)!!.tags.map { it.name }.toSet())

        tasks.saveTaskDetails(t.copy(tags = listOf(home)))
        assertEquals(listOf("home"), tasks.getTask(t.id)!!.tags.map { it.name })
    }

    @Test
    fun saveTaskDetailsDoesNotResurrectADeletedTask() = runTest {
        val t = tasks.addTask(inbox, "Task")
        tasks.deleteTask(t.id)

        tasks.saveTaskDetails(t.copy(title = "Edited while deleted", subtasks = listOf(Subtask("s1", "A"))))

        assertNull(tasks.getTask(t.id))
        assertTrue(allTasks().isEmpty())
    }

    // --- Trash ---

    @Test
    fun deletedTasksAreHiddenAndRestoreBringsThemBack() = runTest {
        val t = tasks.addTask(inbox, "Task")

        tasks.deleteTask(t.id)
        assertTrue(allTasks().isEmpty())
        assertNull(tasks.getTask(t.id))
        assertEquals(clock.millis, tasks.observeTrash().first().single().deletedAt!!.toEpochMilli())

        tasks.restoreTask(t.id)
        assertEquals(listOf(t.id), allTasks().map { it.id })
    }

    @Test
    fun deleteForeverOnlyRemovesTasksAlreadyInTheTrash() = runTest {
        val live = tasks.addTask(inbox, "Live")
        val trashed = tasks.addTask(inbox, "Trashed")
        tasks.deleteTask(trashed.id)

        tasks.deleteForever(live.id) // not in the trash: ignored
        tasks.deleteForever(trashed.id)

        assertEquals(listOf(live.id), allTasks().map { it.id })
        assertTrue(tasks.observeTrash().first().isEmpty())
    }

    @Test
    fun emptyTrashRemovesEveryDeletedTask() = runTest {
        val a = tasks.addTask(inbox, "A")
        val b = tasks.addTask(inbox, "B")
        tasks.addTask(inbox, "Keep")
        tasks.deleteTask(a.id)
        tasks.deleteTask(b.id)

        tasks.emptyTrash()

        assertTrue(tasks.observeTrash().first().isEmpty())
        assertEquals(listOf("Keep"), allTasks().map { it.title })
    }

    @Test
    fun purgeRemovesOnlyTasksDeletedLongAgo() = runTest {
        val old = tasks.addTask(inbox, "Old")
        tasks.deleteTask(old.id)
        clock.advance(Duration.ofDays(31).toMillis())
        val recent = tasks.addTask(inbox, "Recent")
        tasks.deleteTask(recent.id)

        tasks.purgeTrash(Duration.ofDays(30))

        assertEquals(listOf(recent.id), tasks.observeTrash().first().map { it.id })
    }

    // --- Lists and folders ---

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
    fun listsCanBeCreatedUpdatedAndMovedIntoFolders() = runTest {
        val folder = lists.createFolder("Areas")
        val list = lists.createList("Work", colorArgb = 0xFF64B5F6.toInt())

        lists.updateList(list.id, "Work stuff", colorArgb = null, folderId = folder.id)

        val stored = lists.observeLists().first().single { it.id == list.id }
        assertEquals("Work stuff", stored.name)
        assertNull(stored.colorArgb)
        assertEquals(folder.id, stored.folderId)
    }

    @Test
    fun theInboxKeepsItsNameAndStaysOutOfFolders() = runTest {
        val folder = lists.createFolder("Areas")

        lists.updateList(inbox, "Renamed", colorArgb = null, folderId = folder.id)

        val stored = lists.observeLists().first().single { it.id == inbox }
        assertEquals("Inbox", stored.name)
        assertNull(stored.folderId)
    }

    @Test
    fun deletingAListMovesItsTasksToTheInbox() = runTest {
        val work = lists.createList("Work")
        val task = tasks.addTask(work.id, "At work")

        lists.deleteList(work.id)

        assertTrue(lists.observeLists().first().none { it.id == work.id })
        assertEquals(inbox, tasks.getTask(task.id)!!.listId)
    }

    @Test
    fun theInboxCannotBeDeleted() = runTest {
        lists.deleteList(inbox)

        assertTrue(lists.observeLists().first().any { it.id == inbox })
    }

    @Test
    fun deletingAFolderKeepsItsLists() = runTest {
        val folder = lists.createFolder("Areas")
        val list = lists.createList("Work", folderId = folder.id)

        lists.deleteFolder(folder.id)

        assertTrue(lists.observeFolders().first().isEmpty())
        assertNull(lists.observeLists().first().single { it.id == list.id }.folderId)
    }

    // --- Tags ---

    @Test
    fun createTagReturnsTheExistingTagForTheSameNameIgnoringCaseAndHash() = runTest {
        val first = tags.createTag("Work")
        val again = tags.createTag("#work")

        assertEquals(first.id, again.id)
        assertEquals(1, tags.observeTags().first().size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun createTagRejectsBlankNames() = runTest {
        tags.createTag("  #  ")
    }

    @Test
    fun renameTagRefusesNamesUsedByAnotherTag() = runTest {
        val work = tags.createTag("work")
        tags.createTag("home")

        assertFalse(tags.renameTag(work.id, "HOME"))
        assertTrue(tags.renameTag(work.id, "job"))
        assertEquals(setOf("job", "home"), tags.observeTags().first().map { it.name }.toSet())
    }

    @Test
    fun deletingATagRemovesItFromTasks() = runTest {
        val work = tags.createTag("work")
        val task = tasks.addTask(inbox, "Task")
        tasks.saveTaskDetails(task.copy(tags = listOf(work)))

        tags.deleteTag(work.id)

        assertTrue(tags.observeTags().first().isEmpty())
        assertTrue(tasks.getTask(task.id)!!.tags.isEmpty())
        assertNotNull(allTasks().single())
    }

    // --- Reminders ---

    private fun beforeDue(id: String, minutes: Int = 0) =
        Reminder(id = id, kind = ReminderKind.BEFORE_DUE, offsetMinutes = minutes)

    private suspend fun taskDueOn20th() = tasks.addTask(inbox, "Task", LocalDate.of(2026, 10, 20))

    @Test
    fun saveTaskDetailsStoresRemindersAndRemovesMissingOnes() = runTest {
        val t = taskDueOn20th()

        tasks.saveTaskDetails(t.copy(reminders = listOf(beforeDue("r1", 0), beforeDue("r2", 30))))
        assertEquals(setOf(0, 30), tasks.getTask(t.id)!!.reminders.map { it.offsetMinutes }.toSet())

        tasks.saveTaskDetails(t.copy(reminders = listOf(beforeDue("r2", 30))))
        assertEquals(listOf("r2"), tasks.getTask(t.id)!!.reminders.map { it.id })
    }

    @Test
    fun specificTimeRemindersKeepTheirMoment() = runTest {
        val t = tasks.addTask(inbox, "Task")
        val moment = LocalDateTime.of(2026, 10, 21, 8, 15)

        tasks.saveTaskDetails(t.copy(reminders = listOf(Reminder("a", ReminderKind.AT_TIME, at = moment))))

        val stored = tasks.getTask(t.id)!!.reminders.single()
        assertEquals(ReminderKind.AT_TIME, stored.kind)
        assertEquals(moment, stored.at)
    }

    @Test
    fun remindersAlsoComeWithTheFullTaskList() = runTest {
        val t = taskDueOn20th()
        tasks.saveTaskDetails(t.copy(reminders = listOf(beforeDue("r1", 10))))

        assertEquals(listOf("r1"), allTasks().single().reminders.map { it.id })
        assertEquals(listOf("r1"), tasks.getActiveTasks().single().reminders.map { it.id })
    }

    @Test
    fun aShownReminderKeepsItsStateAcrossAnUnrelatedEdit() = runTest {
        val t = taskDueOn20th()
        tasks.saveTaskDetails(t.copy(reminders = listOf(beforeDue("r1", 10))))
        val before = tasks.getTask(t.id)!!.reminders.single()
        reminderRepo.markFired("r1", Instant.ofEpochMilli(5_000))
        clock.advance()

        tasks.saveTaskDetails(t.copy(title = "Renamed", reminders = listOf(beforeDue("r1", 10))))

        val after = tasks.getTask(t.id)!!.reminders.single()
        assertEquals(Instant.ofEpochMilli(5_000), after.firedForTrigger)
        assertEquals(before.updatedAt, after.updatedAt) // nothing about its schedule changed
    }

    @Test
    fun markingAReminderShownLeavesItsAgeAlone() = runTest {
        val t = taskDueOn20th()
        tasks.saveTaskDetails(t.copy(reminders = listOf(beforeDue("r1", 10))))
        val before = tasks.getTask(t.id)!!.reminders.single()
        clock.advance()

        reminderRepo.markFired("r1", Instant.ofEpochMilli(7_000))

        assertEquals(before.updatedAt, tasks.getTask(t.id)!!.reminders.single().updatedAt)
    }

    @Test
    fun movingTheDueTimeRefreshesOnlyBeforeDueReminders() = runTest {
        val t = taskDueOn20th()
        val moment = LocalDateTime.of(2026, 10, 21, 8, 0)
        val reminders = listOf(beforeDue("r1", 10), Reminder("r2", ReminderKind.AT_TIME, at = moment))
        tasks.saveTaskDetails(t.copy(reminders = reminders))
        val created = tasks.getTask(t.id)!!.reminders.associateBy { it.id }
        clock.advance()

        tasks.saveTaskDetails(t.copy(dueTime = LocalTime.of(18, 0), reminders = reminders))

        val after = tasks.getTask(t.id)!!.reminders.associateBy { it.id }
        assertEquals(clock.millis, after.getValue("r1").updatedAt.toEpochMilli())
        assertEquals(created.getValue("r2").updatedAt, after.getValue("r2").updatedAt)
    }

    @Test
    fun aNewSnoozeReplacesTheEarlierOneAndSurvivesEditing() = runTest {
        val t = taskDueOn20th()
        reminderRepo.snooze(t.id, LocalDateTime.of(2026, 10, 20, 9, 0))
        reminderRepo.snooze(t.id, LocalDateTime.of(2026, 10, 20, 9, 30, 15))

        val snoozes = tasks.getTask(t.id)!!.reminders
        assertEquals(1, snoozes.size)
        assertEquals(ReminderKind.SNOOZE, snoozes.single().kind)
        assertEquals(LocalDateTime.of(2026, 10, 20, 9, 30, 15), snoozes.single().at)

        // The editor never sees the snooze, so saving without reminders must not remove it.
        tasks.saveTaskDetails(t.copy(reminders = emptyList()))
        assertEquals(1, tasks.getTask(t.id)!!.reminders.size)
    }

    @Test
    fun deletingASnoozeRemovesIt() = runTest {
        val t = taskDueOn20th()
        reminderRepo.snooze(t.id, LocalDateTime.of(2026, 10, 20, 9, 0))
        val id = tasks.getTask(t.id)!!.reminders.single().id

        reminderRepo.delete(id)

        assertTrue(tasks.getTask(t.id)!!.reminders.isEmpty())
    }

    @Test
    fun remindersGoWhenTheirTaskIsDeletedForever() = runTest {
        val t = taskDueOn20th()
        tasks.saveTaskDetails(t.copy(reminders = listOf(beforeDue("r1", 10))))
        tasks.deleteTask(t.id)

        tasks.deleteForever(t.id)

        assertTrue(tasks.getActiveTasks().isEmpty())
        assertTrue(tasks.observeTrash().first().isEmpty())
    }
}
