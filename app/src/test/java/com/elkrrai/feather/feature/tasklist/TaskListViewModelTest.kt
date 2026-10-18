package com.elkrrai.feather.feature.tasklist

import com.elkrrai.feather.domain.model.TaskList
import com.elkrrai.feather.domain.quickadd.ParsedQuickAdd
import com.elkrrai.feather.domain.view.TaskScope
import com.elkrrai.feather.domain.view.TaskSort
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
    private val tags = FakeTagRepository()
    private val clock: Clock = Clock.fixed(Instant.parse("2026-10-12T12:00:00Z"), ZoneId.of("UTC"))

    /** Same rule the ViewModel uses for "today". */
    private val today: LocalDate = LocalDate.now(clock.withZone(ZoneId.systemDefault()))

    private val inbox = TaskList.INBOX_ID

    /**
     * `uiState` is `WhileSubscribed`, so something has to collect it for the test. The collector
     * must run on an unconfined dispatcher: on `runTest`'s default one it would never start.
     *
     * The app opens on Today, but most of these tests use tasks without a date, which live in the
     * Goals view, so the new ViewModel is moved there unless [startOnToday] is set.
     */
    private fun TestScope.newViewModel(startOnToday: Boolean = false): TaskListViewModel {
        val viewModel = TaskListViewModel(tasks, tags, clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        if (!startOnToday) viewModel.selectScope(TaskScope.NoDate)
        return viewModel
    }

    private fun TaskListViewModel.titles(): List<String> =
        uiState.value.sections.flatMap { section -> section.tasks.map { it.title } }

    @Test
    fun startsOnTodayAndStopsLoading() = runTest {
        val viewModel = newViewModel(startOnToday = true)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(TaskScope.Today, state.scope)
        assertTrue(state.isEmpty)
    }

    @Test
    fun addTaskAppearsWithTrimmedTitle() = runTest {
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
    fun everyNewTaskLivesInTheInbox() = runTest {
        val viewModel = newViewModel()

        viewModel.addTask("Anywhere")
        viewModel.addParsed(ParsedQuickAdd(title = "Parsed"))

        assertTrue(tasks.getActiveTasks().all { it.listId == inbox })
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
    fun overdueViewHoldsOnlyTasksBeforeToday() = runTest {
        tasks.addTask(inbox, "Overdue", today.minusDays(2))
        tasks.addTask(inbox, "Due today", today)
        val viewModel = newViewModel()

        viewModel.selectScope(TaskScope.Overdue)

        assertEquals(listOf("Overdue"), viewModel.titles())
    }

    @Test
    fun goalsViewHoldsOpenTasksWithoutADateAndQuickAddLeavesThemUndated() = runTest {
        tasks.addTask(inbox, "Dated", today)
        tasks.addTask(inbox, "A goal")
        val viewModel = newViewModel()

        viewModel.selectScope(TaskScope.NoDate)
        assertEquals(listOf("A goal"), viewModel.titles())

        viewModel.addTask("Another goal")
        assertEquals(setOf("A goal", "Another goal"), viewModel.titles().toSet())
        assertEquals(null, tasks.getActiveTasks().first { it.title == "Another goal" }.dueDate)
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
    fun countsReflectOpenTasksInEveryView() = runTest {
        tasks.addTask(inbox, "Due today", today)
        tasks.addTask(inbox, "Overdue", today.minusDays(1))
        tasks.addTask(inbox, "Tomorrow", today.plusDays(1))
        tasks.addTask(inbox, "A goal")
        val done = tasks.addTask(inbox, "Done", today)
        tasks.setCompleted(done.id, true)
        val viewModel = newViewModel()

        val counts = viewModel.uiState.value.counts
        assertEquals(2, counts[TaskScope.Today])
        assertEquals(1, counts[TaskScope.Overdue])
        assertEquals(1, counts[TaskScope.Upcoming])
        assertEquals(1, counts[TaskScope.NoDate])
    }

    @Test
    fun sortByDueDateOrdersTasksEarliestFirst() = runTest {
        tasks.addTask(inbox, "Today", today)
        tasks.addTask(inbox, "Long ago", today.minusDays(5))
        tasks.addTask(inbox, "Yesterday", today.minusDays(1))
        val viewModel = newViewModel()
        viewModel.selectScope(TaskScope.Today)

        viewModel.setSort(TaskSort.DUE_DATE)

        assertEquals(listOf("Long ago", "Yesterday", "Today"), viewModel.titles())
    }

    @Test
    fun finishingATaskTakesItOutOfTheOpenList() = runTest {
        val first = tasks.addTask(inbox, "First", today)
        tasks.addTask(inbox, "Second", today)
        val viewModel = newViewModel()
        viewModel.selectScope(TaskScope.Today)

        viewModel.setCompleted(first.id, true)

        assertEquals(listOf("Second"), viewModel.titles())
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

    @Test
    fun aDateInTheTextWinsOverTheTodayView() = runTest {
        val viewModel = newViewModel()
        viewModel.selectScope(TaskScope.Today)
        val written = today.plusDays(5)

        viewModel.addParsed(ParsedQuickAdd(title = "Later", dueDate = written))
        viewModel.addParsed(ParsedQuickAdd(title = "No date"))

        val byTitle = tasks.getActiveTasks().associateBy { it.title }
        assertEquals(written, byTitle.getValue("Later").dueDate)
        assertEquals(today, byTitle.getValue("No date").dueDate)
    }

    @Test
    fun tagsInTheTextAreAddedToTheTask() = runTest {
        val viewModel = newViewModel()

        viewModel.addParsed(ParsedQuickAdd(title = "Report", tags = listOf("urgent", "work")))

        val saved = tasks.getActiveTasks().single()
        assertEquals(setOf("work", "urgent"), saved.tags.map { it.name }.toSet())
    }

    // --- Selecting several tasks ---

    @Test
    fun selectionIsLimitedToTasksOnScreen() = runTest {
        val a = tasks.addTask(inbox, "A")
        val viewModel = newViewModel()

        viewModel.toggleSelected(a.id)
        viewModel.toggleSelected("ghost")
        assertEquals(setOf(a.id), viewModel.uiState.value.selection)

        viewModel.toggleSelected(a.id)
        assertTrue(viewModel.uiState.value.selection.isEmpty())
    }

    @Test
    fun selectAllAndClear() = runTest {
        tasks.addTask(inbox, "A")
        tasks.addTask(inbox, "B")
        val viewModel = newViewModel()

        viewModel.selectAll()
        assertEquals(2, viewModel.uiState.value.selection.size)

        viewModel.clearSelection()
        assertTrue(viewModel.uiState.value.selection.isEmpty())
    }

    @Test
    fun changingTheViewClearsTheSelection() = runTest {
        val a = tasks.addTask(inbox, "A")
        val viewModel = newViewModel()
        viewModel.toggleSelected(a.id)

        viewModel.selectScope(TaskScope.Overdue)

        assertTrue(viewModel.uiState.value.selection.isEmpty())
    }

    @Test
    fun completeSelectedFinishesThemAndEndsSelecting() = runTest {
        val a = tasks.addTask(inbox, "A")
        val b = tasks.addTask(inbox, "B")
        val c = tasks.addTask(inbox, "C")
        val viewModel = newViewModel()
        viewModel.toggleSelected(a.id)
        viewModel.toggleSelected(b.id)

        viewModel.completeSelected()

        assertTrue(tasks.getTask(a.id)!!.isCompleted)
        assertTrue(tasks.getTask(b.id)!!.isCompleted)
        assertFalse(tasks.getTask(c.id)!!.isCompleted)
        assertTrue(viewModel.uiState.value.selection.isEmpty())
    }

    @Test
    fun addTagToSelectedKeepsOtherTagsAndDoesNotRepeat() = runTest {
        val existing = tags.createTag("home")
        val a = tasks.addTask(inbox, "A")
        tasks.saveTaskDetails(a.copy(tags = listOf(existing)))
        val viewModel = newViewModel()

        viewModel.toggleSelected(a.id)
        viewModel.addNewTagToSelected("urgent")
        viewModel.toggleSelected(a.id)
        viewModel.addTagToSelected(existing)

        assertEquals(setOf("home", "urgent"), tasks.getTask(a.id)!!.tags.map { it.name }.toSet())
        assertEquals(2, tasks.getTask(a.id)!!.tags.size)
    }

    @Test
    fun deleteTasksCanBeUndone() = runTest {
        val a = tasks.addTask(inbox, "A")
        val b = tasks.addTask(inbox, "B")
        val viewModel = newViewModel()

        viewModel.deleteTasks(listOf(a.id, b.id))
        assertTrue(viewModel.uiState.value.isEmpty)

        viewModel.restoreTasks(listOf(a.id, b.id))
        assertEquals(listOf("A", "B"), viewModel.titles())
    }

    // --- Manual order and pinning ---

    @Test
    fun manualOrderFollowsTheSavedOrder() = runTest {
        val a = tasks.addTask(inbox, "A")
        val b = tasks.addTask(inbox, "B")
        val c = tasks.addTask(inbox, "C")
        val viewModel = newViewModel()
        viewModel.setSort(TaskSort.MANUAL)

        viewModel.reorderTasks(listOf(c.id, a.id, b.id))

        assertEquals(listOf("C", "A", "B"), viewModel.titles())
    }

    @Test
    fun createdOrderIsNotChangedByDragging() = runTest {
        val a = tasks.addTask(inbox, "A")
        val b = tasks.addTask(inbox, "B")
        val viewModel = newViewModel()

        viewModel.reorderTasks(listOf(b.id, a.id))

        assertEquals(listOf("A", "B"), viewModel.titles())
    }

    @Test
    fun pinningMovesATaskToTheTop() = runTest {
        tasks.addTask(inbox, "A")
        val b = tasks.addTask(inbox, "B")
        val viewModel = newViewModel()

        viewModel.setPinned(b.id, true)

        assertEquals(listOf("B", "A"), viewModel.titles())
    }

    // --- Subtasks from the list ---

    @Test
    fun aSubtaskAddedFromTheListIsSavedOnTheTask() = runTest {
        val task = tasks.addTask(inbox, "Plan trip")
        val viewModel = newViewModel()

        viewModel.addSubtask(task.id, "  Book flights ")
        viewModel.addSubtask(task.id, "Pack")

        val subtasks = tasks.getTask(task.id)!!.subtasks
        assertEquals(listOf("Book flights", "Pack"), subtasks.map { it.title })
        assertTrue(subtasks.none { it.isCompleted })
    }

    @Test
    fun aBlankSubtaskIsIgnored() = runTest {
        val task = tasks.addTask(inbox, "Plan trip")
        val viewModel = newViewModel()

        viewModel.addSubtask(task.id, "   ")

        assertTrue(tasks.getTask(task.id)!!.subtasks.isEmpty())
    }

    @Test
    fun tickingASubtaskFlipsOnlyThatOne() = runTest {
        val task = tasks.addTask(inbox, "Plan trip")
        val viewModel = newViewModel()
        viewModel.addSubtask(task.id, "Book flights")
        viewModel.addSubtask(task.id, "Pack")
        val first = tasks.getTask(task.id)!!.subtasks.first()

        viewModel.toggleSubtask(task.id, first.id)

        val after = tasks.getTask(task.id)!!.subtasks
        assertEquals(listOf(true, false), after.map { it.isCompleted })
        viewModel.toggleSubtask(task.id, first.id)
        assertEquals(listOf(false, false), tasks.getTask(task.id)!!.subtasks.map { it.isCompleted })
    }

    @Test
    fun tickingTheLastOpenSubtaskFinishesTheTask() = runTest {
        val task = tasks.addTask(inbox, "Plan trip")
        val viewModel = newViewModel()
        viewModel.addSubtask(task.id, "Book flights")
        viewModel.addSubtask(task.id, "Pack")
        val (first, second) = tasks.getTask(task.id)!!.subtasks

        viewModel.toggleSubtask(task.id, first.id)
        assertEquals(false, tasks.getTask(task.id)!!.isCompleted)

        viewModel.toggleSubtask(task.id, second.id)
        assertEquals(true, tasks.getTask(task.id)!!.isCompleted)
    }

    @Test
    fun theCompletedViewStillListsFinishedTasksInTheMainList() = runTest {
        val done = tasks.addTask(inbox, "Done")
        tasks.setCompleted(done.id, true)
        val viewModel = newViewModel()

        viewModel.selectScope(TaskScope.Completed)

        assertEquals(listOf("Done"), viewModel.titles())
        assertTrue(viewModel.uiState.value.doneTasks.isEmpty())
    }
}
