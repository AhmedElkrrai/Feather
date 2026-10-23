package com.elkrrai.feather.feature.trash

import com.elkrrai.feather.domain.view.TaskScope
import com.elkrrai.feather.feature.tasklist.TaskListViewModel
import com.elkrrai.feather.testutil.FakeTagRepository
import com.elkrrai.feather.testutil.FakeTaskRepository
import com.elkrrai.feather.testutil.MainDispatcherRule
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The path a task takes in the report "I created a task, marked it done, deleted it from Completed,
 * and after restoring it from the trash I sometimes could not find it": the task must come back in the
 * Completed view, still finished, and not be lost from the trash list.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TrashRestoreFlowTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val tasks = FakeTaskRepository()
    private val tags = FakeTagRepository()
    private val clock: Clock = Clock.fixed(Instant.parse("2026-10-12T12:00:00Z"), ZoneId.of("UTC"))

    private fun TestScope.newViews(): Pair<TaskListViewModel, TrashViewModel> {
        val list = TaskListViewModel(tasks, tags, clock)
        val trash = TrashViewModel(tasks)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { list.uiState.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { trash.uiState.collect {} }
        return list to trash
    }

    private fun TaskListViewModel.titles(): List<String> =
        uiState.value.sections.flatMap { section -> section.tasks.map { it.title } }

    @Test
    fun aFinishedTaskDeletedFromCompletedComesBackToCompletedWhenRestored() = runTest {
        val task = tasks.addTask("Write report")
        val (list, trash) = newViews()
        list.setCompleted(task.id, true)
        list.selectScope(TaskScope.Completed)
        assertEquals(listOf("Write report"), list.titles())

        list.deleteTask(task.id)
        assertTrue(list.titles().isEmpty())
        assertEquals(listOf("Write report"), trash.uiState.value.tasks.map { it.title })

        trash.restore(task.id)

        assertTrue(trash.uiState.value.tasks.isEmpty())
        assertEquals(listOf("Write report"), list.titles())
        assertTrue(tasks.getTask(task.id)!!.isCompleted)
    }

    @Test
    fun theRestoredTaskIsFoundEvenWhenManyOtherTasksWereFinishedAfterIt() = runTest {
        val first = tasks.addTask("First")
        val (list, trash) = newViews()
        list.setCompleted(first.id, true)
        repeat(12) { index ->
            val other = tasks.addTask("Other $index")
            list.setCompleted(other.id, true)
        }
        list.selectScope(TaskScope.Completed)

        list.deleteTask(first.id)
        trash.restore(first.id)

        // It is in the list, not lost somewhere below the fold of a long one.
        assertTrue("First" in list.titles())
        assertEquals(13, list.titles().size)
    }

    @Test
    fun restoringTwiceOrDeletingTwiceNeverLosesOrDuplicatesTheTask() = runTest {
        val task = tasks.addTask("Once")
        val (list, trash) = newViews()
        list.setCompleted(task.id, true)
        list.selectScope(TaskScope.Completed)

        list.deleteTask(task.id)
        list.deleteTask(task.id)
        trash.restore(task.id)
        trash.restore(task.id)

        assertEquals(listOf("Once"), list.titles())
    }
}
