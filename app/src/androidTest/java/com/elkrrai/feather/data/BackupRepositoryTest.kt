package com.elkrrai.feather.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.elkrrai.feather.data.backup.BackupCodec
import com.elkrrai.feather.data.backup.BackupFile
import com.elkrrai.feather.data.db.FeatherDatabase
import com.elkrrai.feather.data.db.RoomTransactionRunner
import com.elkrrai.feather.data.repository.BackupRepositoryImpl
import com.elkrrai.feather.data.repository.ListRepositoryImpl
import com.elkrrai.feather.data.repository.TagRepositoryImpl
import com.elkrrai.feather.data.repository.TaskRepositoryImpl
import com.elkrrai.feather.domain.backup.BackupFormatException
import com.elkrrai.feather.domain.backup.ImportMode
import com.elkrrai.feather.domain.model.Priority
import com.elkrrai.feather.domain.model.Reminder
import com.elkrrai.feather.domain.model.ReminderKind
import com.elkrrai.feather.domain.model.Subtask
import com.elkrrai.feather.domain.model.TaskList
import com.elkrrai.feather.domain.recurrence.Frequency
import com.elkrrai.feather.domain.recurrence.Recurrence
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupRepositoryTest {

    private lateinit var db: FeatherDatabase
    private lateinit var clock: TestClock
    private lateinit var tasks: TaskRepositoryImpl
    private lateinit var lists: ListRepositoryImpl
    private lateinit var tags: TagRepositoryImpl
    private lateinit var backup: BackupRepositoryImpl

    private val inbox = TaskList.INBOX_ID

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FeatherDatabase::class.java).build()
        clock = TestClock()
        val tx = RoomTransactionRunner(db)
        tasks = TaskRepositoryImpl(db.taskDao(), db.reminderDao(), db.completionDao(), tx, clock)
        lists = ListRepositoryImpl(db.listDao(), db.folderDao(), db.taskDao(), tx, clock)
        tags = TagRepositoryImpl(db.tagDao(), clock)
        backup = BackupRepositoryImpl(db.backupDao(), tx, clock, tasks, lists, context)
        lists.ensureInbox()
    }

    @After
    fun tearDown() {
        db.close()
    }

    /** A task with a tag, a subtask, a reminder, a repeat rule and one recorded completion. */
    private suspend fun fullTask(): String {
        val work = lists.createList("Work")
        val tag = tags.createTag("urgent")
        val created = tasks.addTask(work.id, "Report", LocalDate.of(2026, 10, 20))
        tasks.saveTaskDetails(
            created.copy(
                priority = Priority.HIGH,
                tags = listOf(tag),
                subtasks = listOf(Subtask("s1", "Outline")),
                reminders = listOf(Reminder(id = "r1", kind = ReminderKind.BEFORE_DUE, offsetMinutes = 30)),
                recurrence = Recurrence(Frequency.WEEKLY),
            )
        )
        clock.advance()
        tasks.setCompleted(created.id, true) // a repeating task: history row, then it moves on
        clock.advance()
        return created.id
    }

    @Test
    fun aReplaceImportBringsBackEverythingInTheFile() = runTest {
        val id = fullTask()
        val json = backup.exportJson()
        clock.advance()
        tasks.saveTask(tasks.getTask(id)!!.copy(title = "Changed"))
        tasks.addTask(inbox, "Added later")

        backup.importJson(json, ImportMode.REPLACE)

        val restored = tasks.getTask(id)!!
        assertEquals("Report", restored.title)
        assertEquals(Priority.HIGH, restored.priority)
        assertEquals(listOf("urgent"), restored.tags.map { it.name })
        assertEquals(listOf("Outline"), restored.subtasks.map { it.title })
        assertEquals(listOf("r1"), restored.reminders.map { it.id })
        assertEquals(Recurrence(Frequency.WEEKLY), restored.recurrence)
        assertEquals(LocalDate.of(2026, 10, 27), restored.dueDate)
        assertEquals(1, db.completionDao().getForTask(id).size)
        assertEquals(listOf("Report"), tasks.getActiveTasks().map { it.title })
    }

    @Test
    fun aReplaceImportWithAnEmptyFileStillLeavesTheInbox() = runTest {
        fullTask()

        backup.importJson(BackupCodec.encode(BackupFile()), ImportMode.REPLACE)

        assertTrue(tasks.getActiveTasks().isEmpty())
        val remaining = lists.observeLists().first().single()
        assertEquals(inbox, remaining.id)
        assertTrue(remaining.isInbox)
    }

    @Test
    fun aMergeKeepsTheNewerEditAndAddsWhatIsMissing() = runTest {
        val id = fullTask()
        val json = backup.exportJson()
        clock.advance()
        tasks.saveTask(tasks.getTask(id)!!.copy(title = "Edited after the backup"))
        val extra = tasks.addTask(inbox, "Added after the backup")

        backup.importJson(json, ImportMode.MERGE)

        assertEquals("Edited after the backup", tasks.getTask(id)!!.title)
        assertEquals("Added after the backup", tasks.getTask(extra.id)!!.title)
    }

    @Test
    fun aMergeBringsBackATaskDeletedForGoodOnThePhone() = runTest {
        val id = fullTask()
        val json = backup.exportJson()
        tasks.deleteTask(id)
        tasks.deleteForever(id)

        backup.importJson(json, ImportMode.MERGE)

        assertEquals("Report", tasks.getTask(id)!!.title)
    }

    @Test
    fun anOlderCopyDoesNotUndoADeletion() = runTest {
        val id = fullTask()
        val json = backup.exportJson()
        clock.advance()
        tasks.deleteTask(id)

        backup.importJson(json, ImportMode.MERGE)

        assertTrue(tasks.getActiveTasks().isEmpty())
    }

    @Test
    fun aBrokenFileChangesNothing() = runTest {
        fullTask()
        val before = backup.exportJson()

        try {
            backup.importJson("{\"format\": \"feather-backup\", \"version\": 1, \"tasks\": [{\"id\": 5}]}", ImportMode.REPLACE)
            fail("Expected the file to be rejected")
        } catch (e: BackupFormatException) {
            assertEquals(BackupFormatException.Reason.NOT_A_BACKUP, e.reason)
        }

        assertEquals(1, tasks.getActiveTasks().size)
        assertEquals(before.length, backup.exportJson().length)
    }

    @Test
    fun theSummaryCountsWhatTheFileHeld() = runTest {
        fullTask()
        val json = backup.exportJson()

        val summary = backup.importJson(json, ImportMode.MERGE)

        assertEquals(1, summary.tasks)
        assertEquals(1, summary.lists)
        assertEquals(1, summary.tags)
    }

    @Test
    fun theCsvListsTheOpenTasks() = runTest {
        fullTask()

        val csv = backup.exportCsv()

        assertTrue(csv.contains("Report"))
        assertTrue(csv.contains("Work"))
        assertTrue(csv.contains("urgent"))
    }
}
