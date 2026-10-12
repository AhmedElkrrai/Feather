package com.elkrrai.feather.feature.tasklist

import com.elkrrai.feather.domain.model.TaskList
import com.elkrrai.feather.testutil.FakeTaskRepository
import com.elkrrai.feather.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repo = FakeTaskRepository()
    private val inbox = TaskList.INBOX_ID

    /** `uiState` is `WhileSubscribed`, so something has to collect it for the test. */
    private fun TestScope.newViewModel(): TaskListViewModel {
        val viewModel = TaskListViewModel(repo)
        backgroundScope.launch { viewModel.uiState.collect {} }
        return viewModel
    }

    @Test
    fun startsEmptyAndNotLoadingOnceTheRepositoryAnswers() = runTest {
        val viewModel = newViewModel()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.tasks.isEmpty())
    }

    @Test
    fun addTaskAppearsInTheListWithTrimmedTitle() = runTest {
        val viewModel = newViewModel()

        viewModel.addTask("  Buy milk ")

        assertEquals(listOf("Buy milk"), viewModel.uiState.value.tasks.map { it.title })
    }

    @Test
    fun blankTitlesAreIgnored() = runTest {
        val viewModel = newViewModel()

        viewModel.addTask("   ")
        viewModel.addTask("")

        assertTrue(viewModel.uiState.value.tasks.isEmpty())
    }

    @Test
    fun completingMovesTheTaskBelowOpenOnes() = runTest {
        val first = repo.addTask(inbox, "First")
        repo.addTask(inbox, "Second")
        val viewModel = newViewModel()

        viewModel.setCompleted(first.id, true)

        val tasks = viewModel.uiState.value.tasks
        assertEquals(listOf("Second", "First"), tasks.map { it.title })
        assertTrue(tasks.last().isCompleted)
    }

    @Test
    fun reopeningACompletedTaskClearsItsCompletion() = runTest {
        val task = repo.addTask(inbox, "Task")
        val viewModel = newViewModel()
        viewModel.setCompleted(task.id, true)

        viewModel.setCompleted(task.id, false)

        assertFalse(viewModel.uiState.value.tasks.single().isCompleted)
    }

    @Test
    fun deleteHidesTheTaskAndRestoreBringsItBack() = runTest {
        val task = repo.addTask(inbox, "Task")
        val viewModel = newViewModel()

        viewModel.deleteTask(task.id)
        assertTrue(viewModel.uiState.value.tasks.isEmpty())

        viewModel.restoreTask(task.id)
        assertEquals(listOf(task.id), viewModel.uiState.value.tasks.map { it.id })
    }
}
