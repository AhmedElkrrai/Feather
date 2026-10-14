package com.elkrrai.feather.feature.tasklist

import com.elkrrai.feather.domain.model.DueWindow
import com.elkrrai.feather.domain.model.Priority
import com.elkrrai.feather.domain.model.TaskList
import com.elkrrai.feather.domain.quickadd.ParsedQuickAdd
import com.elkrrai.feather.domain.view.TaskScope
import com.elkrrai.feather.domain.view.TaskSort
import com.elkrrai.feather.testutil.FakeListRepository
import com.elkrrai.feather.testutil.FakeSavedFilterRepository
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
    private val filters = FakeSavedFilterRepository()
    private val clock: Clock = Clock.fixed(Instant.parse("2026-10-12T12:00:00Z"), ZoneId.of("UTC"))

    /** Same rule the ViewModel uses for "today". */
    private val today: LocalDate = LocalDate.now(clock.withZone(ZoneId.systemDefault()))

    private val inbox = TaskList.INBOX_ID

    /**
     * `uiState` is `WhileSubscribed`, so something has to collect it for the test. The collector
     * must run on an unconfined dispatcher: on `runTest`'s default one it would never start.
     */
    private fun TestScope.newViewModel(): TaskListViewModel {
        val viewModel = TaskListViewModel(tasks, lists, tags, filters, clock)
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
    fun completingMovesTheTaskToTheDoneSection() = runTest {
        val first = tasks.addTask(inbox, "First")
        tasks.addTask(inbox, "Second")
        val viewModel = newViewModel()

        viewModel.setCompleted(first.id, true)

        assertEquals(listOf("Second"), viewModel.titles())
        assertEquals(listOf("First"), viewModel.uiState.value.doneTasks.map { it.title })
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
    fun aListNamedInTheTextWinsOverTheCurrentView() = runTest {
        val work = lists.createList("Work")
        val viewModel = newViewModel()

        viewModel.addParsed(ParsedQuickAdd(title = "Draft report"), listId = work.id)

        assertEquals(listOf("Draft report"), tasks.getActiveTasks().filter { it.listId == work.id }.map { it.title })
    }

    @Test
    fun aDateInTheTextWinsOverTheTodayView() = runTest {
        val viewModel = newViewModel()
        viewModel.selectScope(TaskScope.Today)
        val written = today.plusDays(5)

        viewModel.addParsed(ParsedQuickAdd(title = "Later", dueDate = written), listId = null)
        viewModel.addParsed(ParsedQuickAdd(title = "No date"), listId = null)

        val byTitle = tasks.getActiveTasks().associateBy { it.title }
        assertEquals(written, byTitle.getValue("Later").dueDate)
        assertEquals(today, byTitle.getValue("No date").dueDate)
    }

    @Test
    fun aTagViewAddsItsTagBesidesTheOnesInTheText() = runTest {
        val work = tags.createTag("work")
        val viewModel = newViewModel()
        viewModel.selectScope(TaskScope.WithTag(work.id))

        viewModel.addParsed(ParsedQuickAdd(title = "Report", tags = listOf("urgent")), listId = null)

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

        viewModel.selectScope(TaskScope.All)

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
    fun moveSelectedGoesToTheChosenList() = runTest {
        val work = lists.createList("Work")
        val a = tasks.addTask(inbox, "A")
        val viewModel = newViewModel()
        viewModel.toggleSelected(a.id)

        viewModel.moveSelectedTo(work.id)

        assertEquals(work.id, tasks.getTask(a.id)!!.listId)
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

    // --- Saved filters ---

    private fun draft(
        name: String = "Filter",
        listId: String? = null,
        tagId: String? = null,
        minPriority: Priority = Priority.NONE,
        due: DueWindow = DueWindow.ANY,
        hideCompleted: Boolean = true,
    ) = FilterDraft(name, listId, tagId, minPriority, due, hideCompleted)

    @Test
    fun creatingAFilterOpensItAndShowsOnlyMatchingTasks() = runTest {
        val urgent = tasks.addTask(inbox, "Urgent")
        tasks.saveTask(urgent.copy(priority = Priority.HIGH))
        tasks.addTask(inbox, "Whenever")
        val viewModel = newViewModel()

        viewModel.createFilter(draft(name = "Important", minPriority = Priority.HIGH))

        val state = viewModel.uiState.value
        assertTrue(state.scope is TaskScope.Filtered)
        assertEquals(listOf("Urgent"), viewModel.titles())
        assertEquals(1, state.filters.size)
        assertEquals(1, state.counts[state.scope])
    }

    @Test
    fun editingTheOpenFilterUpdatesTheView() = runTest {
        val high = tasks.addTask(inbox, "High")
        tasks.saveTask(high.copy(priority = Priority.HIGH))
        tasks.addTask(inbox, "Plain")
        val viewModel = newViewModel()
        viewModel.createFilter(draft(minPriority = Priority.HIGH))
        val filter = viewModel.uiState.value.filters.single()

        viewModel.updateFilter(filter, draft(minPriority = Priority.NONE))

        assertEquals(setOf("High", "Plain"), viewModel.titles().toSet())
    }

    @Test
    fun deletingTheOpenFilterFallsBackToTheInbox() = runTest {
        val viewModel = newViewModel()
        viewModel.createFilter(draft())
        val filter = viewModel.uiState.value.filters.single()

        viewModel.deleteFilter(filter)

        assertEquals(TaskScope.InList(inbox), viewModel.uiState.value.scope)
        assertTrue(viewModel.uiState.value.filters.isEmpty())
    }

    @Test
    fun quickAddInsideAFilterUsesItsListAndTag() = runTest {
        val work = lists.createList("Work")
        val tag = tags.createTag("urgent")
        val viewModel = newViewModel()
        viewModel.createFilter(draft(listId = work.id, tagId = tag.id))

        viewModel.addTask("From the filter")

        val added = tasks.getActiveTasks().single()
        assertEquals(work.id, added.listId)
        assertEquals(listOf("urgent"), added.tags.map { it.name })
    }

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
    fun aListWithOnlyFinishedTasksHasNothingOpenAndAllOfThemInTheDoneSection() = runTest {
        val done = tasks.addTask(inbox, "Done")
        tasks.setCompleted(done.id, true)
        val viewModel = newViewModel()

        val state = viewModel.uiState.value
        assertTrue(state.isEmpty)
        assertEquals(listOf("Done"), state.doneTasks.map { it.title })
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
