package com.elkrrai.feather.feature.taskedit

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.elkrrai.feather.data.TestClock
import com.elkrrai.feather.data.db.FeatherDatabase
import com.elkrrai.feather.data.repository.ListRepositoryImpl
import com.elkrrai.feather.data.repository.TaskRepositoryImpl
import com.elkrrai.feather.domain.model.Priority
import com.elkrrai.feather.domain.model.TaskList
import com.elkrrai.feather.testutil.MainDispatcherRule
import java.time.LocalDate
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
 * Uses a real in-memory Room database instead of a fake repository.
 */
@RunWith(AndroidJUnit4::class)
class TaskEditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var db: FeatherDatabase
    private lateinit var tasks: TaskRepositoryImpl
    private val inbox = TaskList.INBOX_ID

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FeatherDatabase::class.java).build()
        val clock = TestClock()
        tasks = TaskRepositoryImpl(db.taskDao(), clock)
        ListRepositoryImpl(db.listDao(), clock).ensureInbox()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun newTaskViewModel() =
        TaskEditViewModel(SavedStateHandle(mapOf<String, Any?>("taskId" to null)), tasks)

    private fun editViewModel(taskId: String) =
        TaskEditViewModel(SavedStateHandle(mapOf<String, Any?>("taskId" to taskId)), tasks)

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
        val saved = tasks.observeTasks(inbox).first().single()
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
        assertTrue(tasks.observeTasks(inbox).first().isEmpty())
    }

    @Test
    fun editingLoadsTheExistingTask() = runTest {
        val existing = tasks.addTask(inbox, "Existing")
        tasks.saveTask(existing.copy(notes = "n", priority = Priority.LOW))

        val state = editViewModel(existing.id).uiState.value

        assertFalse(state.isNew)
        assertFalse(state.isLoading)
        assertEquals("Existing", state.title)
        assertEquals("n", state.notes)
        assertEquals(Priority.LOW, state.priority)
    }

    @Test
    fun savingAnEditUpdatesTheSameTaskInsteadOfCreatingAnother() = runTest {
        val existing = tasks.addTask(inbox, "Old title")
        val viewModel = editViewModel(existing.id)

        viewModel.onTitleChange("New title")
        viewModel.save()

        val all = tasks.observeTasks(inbox).first()
        assertEquals(1, all.size)
        assertEquals(existing.id, all.single().id)
        assertEquals("New title", all.single().title)
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
}
