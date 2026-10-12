package com.elkrrai.feather.feature.tasklist

import com.elkrrai.feather.domain.model.TaskList
import com.elkrrai.feather.domain.view.TaskScope
import com.elkrrai.feather.domain.view.TaskSort
import com.elkrrai.feather.testutil.FakeListRepository
import com.elkrrai.feather.testutil.FakeTagRepository
import com.elkrrai.feather.testutil.FakeTaskRepository
import com.elkrrai.feather.testutil.MainDispatcherRule
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

    private val tasks = FakeTaskRepository()
    private val lists = FakeListRepository()
    private val tags = FakeTagRepository()
    private val clock: Clock = Clock.fixed(Instant.parse("2026-10-12T12:00:00Z"), ZoneId.of("UTC"))

    /** Same rule the ViewModel uses for "today". */
    private val today: LocalDate = LocalDate.now(clock.withZone(ZoneId.systemDefault()))

    private val inbox = TaskList.INBOX_ID

    /**
     * `uiState` is `WhileSubscribed`, so something has to collect it for the test. The collector
     * must run on an unconfined dispatcher: on `runTest`'s default one it would never start.
     */
    private fun TestScope.newViewModel(): TaskListViewModel {
        val viewModel = TaskListViewModel(tasks, lists, tags, clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        return viewModel
    }

    private fun TaskListViewModel.titles(): List<String> =
        uiState.value.sections.flatMap { section -> section.tasks.map { it.title } }

    @Test
    fun startsOnTheInboxAndStopsLoading() = runTest {
        val viewModel = newViewModel()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(TaskScope.InList(inbox), state.scope)
        assertTrue(state.isEmpty)
    }

    @Test
    fun addTaskAppearsInTheInboxWithTrimmedTitle() = runTest {
        val viewModel = newViewModel()

        viewModel.addTask("  Buy milk ")

        assertEquals(listOf("Buy milk"), viewModel.titles())
    }

    @Test
    fun blankTitlesAreIgnored() = runTest {
        val viewModel = newViewModel()

        viewModel.addTask("   ")
        viewModel.addTask("")

        assertTrue(viewModel.uiState.value.isEmpty)
    }

    @Test
    fun aListViewShowsOnlyThatListAndQuickAddTargetsIt() = runTest {
        val work = lists.createList("Work")
        tasks.addTask(inbox, "In inbox")
        tasks.addTask(work.id, "At work")
        val viewModel = newViewModel()

        viewModel.selectScope(TaskScope.InList(work.id))
        assertEquals(listOf("At work"), viewModel.titles())

        viewModel.addTask("Another work task")
        assertEquals(listOf("At work", "Another work task"), viewModel.titles())
    }

    @Test
    fun todayViewHoldsDueAndOverdueTasksAndQuickAddDatesTheNewTaskToday() = runTest {
        tasks.addTask(inbox, "Overdue", today.minusDays(2))
        tasks.addTask(inbox, "Due today", today)
        tasks.addTask(inbox, "Later", today.plusDays(3))
        tasks.addTask(inbox, "No date")
        val viewModel = newViewModel()

        viewModel.selectScope(TaskScope.Today)
        assertEquals(setOf("Overdue", "Due today"), viewModel.titles().toSet())

        viewModel.addTask("New for today")
        assertTrue("New for today" in viewModel.titles())
        assertEquals(
            today,
            viewModel.uiState.value.sections.flatMap { it.tasks }.first { it.title == "New for today" }.dueDate,
        )
    }

    @Test
    fun upcomingViewGroupsTasksByDay() = runTest {
        tasks.addTask(inbox, "Tomorrow A", today.plusDays(1))
        tasks.addTask(inbox, "Tomorrow B", today.plusDays(1))
        tasks.addTask(inbox, "In three days", today.plusDays(3))
        tasks.addTask(inbox, "Too far", today.plusDays(30))
        val viewModel = newViewModel()

        viewModel.selectScope(TaskScope.Upcoming)

        val sections = viewModel.uiState.value.sections
        assertEquals(listOf(today.plusDays(1), today.plusDays(3)), sections.map { it.date })
        assertEquals(listOf("Tomorrow A", "Tomorrow B"), sections.first().tasks.map { it.title })
    }

    @Test
    fun completedViewListsCompletedTasks() = runTest {
        val done = tasks.addTask(inbox, "Done")
        tasks.addTask(inbox, "Open")
        tasks.setCompleted(done.id, true)
        val viewModel = newViewModel()

        viewModel.selectScope(TaskScope.Completed)

        assertEquals(listOf("Done"), viewModel.titles())
    }

    @Test
    fun countsReflectOpenTasks() = runTest {
        tasks.addTask(inbox, "Due today", today)
        tasks.addTask(inbox, "Overdue", today.minusDays(1))
        val done = tasks.addTask(inbox, "Done", today)
        tasks.setCompleted(done.id, true)
        val viewModel = newViewModel()

        val counts = viewModel.uiState.value.counts
        assertEquals(2, counts[TaskScope.Today])
        assertEquals(1, counts[TaskScope.Overdue])
        assertEquals(2, counts[TaskScope.InList(inbox)])
    }

    @Test
    fun deletingTheSelectedListFallsBackToTheInbox() = runTest {
        val work = lists.createList("Work")
        val viewModel = newViewModel()
        viewModel.selectScope(TaskScope.InList(work.id))
        assertEquals(TaskScope.InList(work.id), viewModel.uiState.value.scope)

        viewModel.deleteList(work)

        assertEquals(TaskScope.InList(inbox), viewModel.uiState.value.scope)
    }

    @Test
    fun theInboxCannotBeDeleted() = runTest {
        val viewModel = newViewModel()
        val inboxList = viewModel.uiState.value.lists.single { it.isInbox }

        viewModel.deleteList(inboxList)

        assertTrue(viewModel.uiState.value.lists.any { it.isInbox })
    }

    @Test
    fun aTagViewShowsTaggedTasksAndQuickAddTagsTheNewTask() = runTest {
        val urgent = tags.createTag("urgent")
        val tagged = tasks.addTask(inbox, "Tagged")
        tasks.saveTaskDetails(tagged.copy(tags = listOf(urgent)))
        tasks.addTask(inbox, "Untagged")
        val viewModel = newViewModel()

        viewModel.selectScope(TaskScope.WithTag(urgent.id))
        assertEquals(listOf("Tagged"), viewModel.titles())

        viewModel.addTask("Also urgent")
        val added = viewModel.uiState.value.sections.flatMap { it.tasks }.first { it.title == "Also urgent" }
        assertEquals(listOf(urgent), added.tags)
    }

    @Test
    fun sortByDueDateOrdersTasksAndKeepsUndatedLast() = runTest {
        tasks.addTask(inbox, "No date")
        tasks.addTask(inbox, "Later", today.plusDays(5))
        tasks.addTask(inbox, "Soon", today.plusDays(1))
        val viewModel = newViewModel()

        viewModel.setSort(TaskSort.DUE_DATE)

        assertEquals(listOf("Soon", "Later", "No date"), viewModel.titles())
    }

    @Test
    fun completingMovesTheTaskBelowOpenOnes() = runTest {
        val first = tasks.addTask(inbox, "First")
        tasks.addTask(inbox, "Second")
        val viewModel = newViewModel()

        viewModel.setCompleted(first.id, true)

        assertEquals(listOf("Second", "First"), viewModel.titles())
    }

    @Test
    fun deleteHidesTheTaskAndRestoreBringsItBack() = runTest {
        val task = tasks.addTask(inbox, "Task")
        val viewModel = newViewModel()

        viewModel.deleteTask(task.id)
        assertTrue(viewModel.uiState.value.isEmpty)

        viewModel.restoreTask(task.id)
        assertEquals(listOf("Task"), viewModel.titles())
    }
}
