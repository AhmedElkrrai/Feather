package com.elkrrai.feather.feature.taskedit

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.elkrrai.feather.data.TestClock
import com.elkrrai.feather.data.db.FeatherDatabase
import com.elkrrai.feather.data.db.RoomTransactionRunner
import com.elkrrai.feather.data.repository.ListRepositoryImpl
import com.elkrrai.feather.data.repository.ReminderRepositoryImpl
import com.elkrrai.feather.data.repository.TagRepositoryImpl
import com.elkrrai.feather.data.repository.TaskRepositoryImpl
import com.elkrrai.feather.domain.model.Priority
import com.elkrrai.feather.domain.model.ReminderKind
import com.elkrrai.feather.domain.model.Subtask
import com.elkrrai.feather.domain.model.TaskList
import com.elkrrai.feather.testutil.MainDispatcherRule
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs on a device because `SavedStateHandle.toRoute` needs the Android `Bundle`.
 * Uses a real in-memory Room database instead of fake repositories.
 */
@RunWith(AndroidJUnit4::class)
class TaskEditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var db: FeatherDatabase
    private lateinit var tasks: TaskRepositoryImpl
    private lateinit var lists: ListRepositoryImpl
    private lateinit var tags: TagRepositoryImpl
    private lateinit var reminderRepo: ReminderRepositoryImpl
    private val inbox = TaskList.INBOX_ID

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FeatherDatabase::class.java).build()
        val clock = TestClock()
        val tx = RoomTransactionRunner(db)
        tasks = TaskRepositoryImpl(db.taskDao(), db.reminderDao(), tx, clock)
        reminderRepo = ReminderRepositoryImpl(db.reminderDao(), tx, clock)
        lists = ListRepositoryImpl(db.listDao(), db.folderDao(), db.taskDao(), tx, clock)
        tags = TagRepositoryImpl(db.tagDao(), clock)
        lists.ensureInbox()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun newTaskViewModel() =
        TaskEditViewModel(SavedStateHandle(mapOf<String, Any?>("taskId" to null)), tasks, lists, tags)

    private fun editViewModel(taskId: String) =
        TaskEditViewModel(SavedStateHandle(mapOf<String, Any?>("taskId" to taskId)), tasks, lists, tags)

    @Test
    fun newTaskStartsEmptyAndCannotBeSaved() {
        val state = newTaskViewModel().uiState.value

        assertTrue(state.isNew)
        assertFalse(state.isLoading)
        assertFalse(state.canSave)
    }

    @Test
    fun savingANewTaskCreatesItInTheInboxWithAllFields() = runTest {
        val viewModel = newTaskViewModel()
        viewModel.onTitleChange("Dentist")
        viewModel.onNotesChange("Bring insurance card")
        viewModel.onDueDateChange(LocalDate.of(2026, 10, 20))
        viewModel.onDueTimeChange(LocalTime.of(14, 30))
        viewModel.onPriorityChange(Priority.HIGH)

        viewModel.save()

        assertTrue(viewModel.uiState.value.finished)
        val saved = tasks.observeTasks().first().single()
        assertEquals(inbox, saved.listId)
        assertEquals("Dentist", saved.title)
        assertEquals("Bring insurance card", saved.notes)
        assertEquals(LocalDate.of(2026, 10, 20), saved.dueDate)
        assertEquals(LocalTime.of(14, 30), saved.dueTime)
        assertEquals(Priority.HIGH, saved.priority)
    }

    @Test
    fun blankTitleIsNotSaved() = runTest {
        val viewModel = newTaskViewModel()
        viewModel.onTitleChange("   ")

        viewModel.save()

        assertFalse(viewModel.uiState.value.finished)
        assertTrue(tasks.observeTasks().first().isEmpty())
    }

    @Test
    fun editingLoadsTheExistingTaskWithItsDetails() = runTest {
        val work = tags.createTag("work")
        val existing = tasks.addTask(inbox, "Existing")
        tasks.saveTaskDetails(
            existing.copy(
                notes = "n",
                priority = Priority.LOW,
                isPinned = true,
                tags = listOf(work),
                subtasks = listOf(Subtask("s1", "Step one")),
            )
        )

        val state = editViewModel(existing.id).uiState.value

        assertFalse(state.isNew)
        assertFalse(state.isLoading)
        assertEquals("Existing", state.title)
        assertEquals("n", state.notes)
        assertEquals(Priority.LOW, state.priority)
        assertTrue(state.isPinned)
        assertEquals(listOf("work"), state.tags.map { it.name })
        assertEquals(listOf("Step one"), state.subtasks.map { it.title })
    }

    @Test
    fun savingAnEditUpdatesTheSameTaskInsteadOfCreatingAnother() = runTest {
        val existing = tasks.addTask(inbox, "Old title")
        val viewModel = editViewModel(existing.id)

        viewModel.onTitleChange("New title")
        viewModel.save()

        val all = tasks.observeTasks().first()
        assertEquals(1, all.size)
        assertEquals(existing.id, all.single().id)
        assertEquals("New title", all.single().title)
    }

    @Test
    fun subtasksCanBeAddedToggledEditedAndRemovedThenSaved() = runTest {
        val viewModel = newTaskViewModel()
        viewModel.onTitleChange("Trip")
        viewModel.onAddSubtask("Passport")
        viewModel.onAddSubtask("  ")
        viewModel.onAddSubtask("Tickets")
        val (passport, tickets) = viewModel.uiState.value.subtasks

        viewModel.onSubtaskToggle(passport.id)
        viewModel.onSubtaskTitleChange(tickets.id, "Train tickets")
        viewModel.onAddSubtask("Remove me")
        viewModel.onSubtaskRemove(viewModel.uiState.value.subtasks.last().id)
        viewModel.save()

        val saved = tasks.observeTasks().first().single()
        assertEquals(listOf("Passport", "Train tickets"), saved.subtasks.map { it.title })
        assertEquals(listOf(true, false), saved.subtasks.map { it.isCompleted })
    }

    @Test
    fun tagsCanBeToggledAndCreatedInline() = runTest {
        val work = tags.createTag("work")
        val viewModel = newTaskViewModel()
        viewModel.onTitleChange("Task")

        viewModel.onTagToggle(work)
        viewModel.onCreateTag("#errands")
        viewModel.onCreateTag("Errands") // same tag, must not be attached twice
        viewModel.save()

        val saved = tasks.observeTasks().first().single()
        assertEquals(setOf("work", "errands"), saved.tags.map { it.name }.toSet())
        assertEquals(2, saved.tags.size)
    }

    @Test
    fun theTaskCanBeMovedToAnotherListAndPinned() = runTest {
        val work = lists.createList("Work")
        val existing = tasks.addTask(inbox, "Task")
        val viewModel = editViewModel(existing.id)

        viewModel.onListChange(work.id)
        viewModel.onPinnedChange(true)
        viewModel.save()

        val saved = tasks.getTask(existing.id)!!
        assertEquals(work.id, saved.listId)
        assertTrue(saved.isPinned)
    }

    @Test
    fun clearingTheDateAlsoClearsTheTime() {
        val viewModel = newTaskViewModel()
        viewModel.onDueDateChange(LocalDate.of(2026, 10, 20))
        viewModel.onDueTimeChange(LocalTime.of(9, 0))

        viewModel.onDueDateChange(null)

        assertNull(viewModel.uiState.value.dueDate)
        assertNull(viewModel.uiState.value.dueTime)
    }

    @Test
    fun aMissingTaskFinishesImmediately() = runTest {
        val viewModel = editViewModel("does-not-exist")

        assertTrue(viewModel.uiState.value.finished)
    }

    @Test
    fun remindersCanBeAddedRemovedAndSaved() = runTest {
        val viewModel = newTaskViewModel()
        viewModel.onTitleChange("Dentist")
        viewModel.onDueDateChange(LocalDate.of(2026, 10, 20))

        viewModel.onAddBeforeDue(30)
        viewModel.onAddBeforeDue(30) // the same schedule is not added twice
        viewModel.onAddBeforeDue(0)
        viewModel.onAddAtTime(LocalDateTime.of(2026, 10, 19, 20, 0))
        assertEquals(3, viewModel.uiState.value.reminders.size)

        viewModel.onRemoveReminder(viewModel.uiState.value.reminders.first { it.offsetMinutes == 0 && it.kind == ReminderKind.BEFORE_DUE }.id)
        viewModel.save()

        val saved = tasks.observeTasks().first().single().reminders
        assertEquals(2, saved.size)
        assertEquals(setOf(ReminderKind.BEFORE_DUE, ReminderKind.AT_TIME), saved.map { it.kind }.toSet())
    }

    @Test
    fun clearingTheDueDateDropsBeforeDueRemindersButKeepsSpecificOnes() {
        val viewModel = newTaskViewModel()
        viewModel.onDueDateChange(LocalDate.of(2026, 10, 20))
        viewModel.onAddBeforeDue(10)
        viewModel.onAddAtTime(LocalDateTime.of(2026, 10, 19, 20, 0))

        viewModel.onDueDateChange(null)

        assertEquals(listOf(ReminderKind.AT_TIME), viewModel.uiState.value.reminders.map { it.kind })
    }

    @Test
    fun aPendingSnoozeIsNotShownInTheEditorAndSurvivesSaving() = runTest {
        val existing = tasks.addTask(inbox, "Task")
        reminderRepo.snooze(existing.id, LocalDateTime.of(2026, 10, 20, 9, 0))
        val viewModel = editViewModel(existing.id)
        assertTrue(viewModel.uiState.value.reminders.isEmpty())

        viewModel.onTitleChange("Edited")
        viewModel.save()

        assertEquals(1, tasks.getTask(existing.id)!!.reminders.size)
    }
}
