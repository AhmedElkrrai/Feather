package com.elkrrai.feather.feature.tasklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elkrrai.feather.domain.model.Tag
import com.elkrrai.feather.domain.model.Task
import com.elkrrai.feather.domain.model.branchIds
import com.elkrrai.feather.domain.quickadd.ParsedQuickAdd
import com.elkrrai.feather.domain.repository.TagRepository
import com.elkrrai.feather.domain.repository.TaskRepository
import com.elkrrai.feather.domain.view.TagFilter
import com.elkrrai.feather.domain.view.TaskScope
import com.elkrrai.feather.domain.view.TaskSort
import com.elkrrai.feather.domain.view.comparator
import com.elkrrai.feather.domain.view.groupedByDay
import com.elkrrai.feather.domain.view.groupedByDoneDay
import com.elkrrai.feather.domain.view.inScope
import com.elkrrai.feather.domain.view.openCountIn
import com.elkrrai.feather.domain.view.withTagFilter
import com.elkrrai.feather.feature.quickadd.QuickAddTask
import com.elkrrai.feather.ui.today.TodayStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.elkrrai.feather.domain.model.Subtask
import java.util.UUID

/** How long a task that was just added keeps its tint before it starts to fade. */
private const val JUST_ADDED_MILLIS = 1_000L

/** Tasks of one day (Upcoming view: the day they are due; Completed view: the day they were finished) or, with a null [date], the whole view in one block. */
data class TaskSection(val date: LocalDate?, val tasks: List<Task>)

data class TaskListUiState(
    val isLoading: Boolean = true,
    val scope: TaskScope = TaskScope.Today,
    val sort: TaskSort = TaskSort.CREATED,
    /** The tag tab picked above the list (All, a tag, or No tag). */
    val tagFilter: TagFilter = TagFilter.All,
    /**
     * For the tag tabs: how many tasks of this view carry each tag or any tag under it, and how many carry
     * none. A tab whose count is 0 is hidden.
     */
    val tagTaskCounts: Map<String, Int> = emptyMap(),
    val untaggedCount: Int = 0,
    val sections: List<TaskSection> = emptyList(),
    val tags: List<Tag> = emptyList(),
    /** Open-task counts for the views, for the drawer badges. */
    val counts: Map<TaskScope, Int> = emptyMap(),
    /** Ids of the tasks picked with a long press; always a subset of what is on screen. */
    val selection: Set<String> = emptySet(),
    /** How today is going, for the Today header: what is left and what was finished. */
    val today: TodayStatus = TodayStatus(),
    /** The task that was just added from quick add; its card glows for a moment. */
    val justAddedId: String? = null,
) {
    val isEmpty: Boolean get() = sections.all { it.tasks.isEmpty() }
}

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val tagRepository: TagRepository,
    private val clock: Clock,
) : ViewModel() {

    private val selectedScope = MutableStateFlow<TaskScope>(TaskScope.Today)
    private val selectedSort = MutableStateFlow(TaskSort.CREATED)
    private val selectedTagFilter = MutableStateFlow<TagFilter>(TagFilter.All)
    private val pickedIds = MutableStateFlow<Set<String>>(emptySet())
    private val justAdded = MutableStateFlow<String?>(null)

    private data class Data(
        val tasks: List<Task>,
        val tags: List<Tag>,
    )

    private val data = combine(
        taskRepository.observeTasks(),
        tagRepository.observeTags(),
    ) { tasks, tags -> Data(tasks, tags) }

    /** What the person is looking at: the view, its sort and the tag tab. */
    private data class Choice(val scope: TaskScope, val sort: TaskSort, val tagFilter: TagFilter)

    private val choice = combine(selectedScope, selectedSort, selectedTagFilter) { scope, sort, tag -> Choice(scope, sort, tag) }

    val uiState: StateFlow<TaskListUiState> = combine(data, choice, pickedIds, justAdded) { d, c, picked, added ->
        buildState(d, c.scope, c.sort, c.tagFilter, picked).copy(justAddedId = added)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskListUiState())

    /** The Clock is UTC; "today" must follow the user's own time zone. */
    private fun today(): LocalDate = LocalDate.now(clock.withZone(ZoneId.systemDefault()))

    private fun buildState(
        d: Data,
        selected: TaskScope,
        sort: TaskSort,
        pickedFilter: TagFilter,
        picked: Set<String>,
    ): TaskListUiState {
        val today = today()
        val zone = ZoneId.systemDefault()
        // A repeating task finished today waits for its next date: it counts as done, not as open.
        val resting = d.tasks.filter { it.isRestingUntilNext(today, zone) }
        val restingIds = resting.map { it.id }.toSet()
        val allTasks = d.tasks.filter { it.id !in restingIds }
        val restingDone = resting.map { it.asFinishedToday() }
        val scope = selected
        // What the tag tabs can offer in this view, before any tag tab narrows it.
        val inView = if (scope == TaskScope.Completed) {
            allTasks.inScope(scope, today) + restingDone
        } else {
            allTasks.inScope(scope, today)
        }
        // A picked tab must always have something to show. Fall back to All as soon as its last
        // matching task leaves this view, so the tab can disappear instead of remaining selected
        // over an empty list. This also handles a tag being deleted and an empty No tag tab.
        val tagFilter = when (pickedFilter) {
            is TagFilter.OfTag -> {
                val branch = d.tags.branchIds(pickedFilter.tagId)
                if (branch.isEmpty() || inView.none { task -> task.tags.any { it.id in branch } }) {
                    TagFilter.All
                } else pickedFilter
            }
            TagFilter.Untagged -> if (inView.none { it.tags.isEmpty() }) TagFilter.All else pickedFilter
            TagFilter.All -> TagFilter.All
        }
        // The tag tab narrows what the list shows; the counts in the menu and the Today header still count everything.
        val shown = allTasks.withTagFilter(tagFilter, d.tags)
        val tagTaskCounts = d.tags.associate { tag ->
            val branch = d.tags.branchIds(tag.id)
            tag.id to inView.count { task -> task.tags.any { it.id in branch } }
        }

        // The Completed view also lists the repeating tasks that were finished today and are resting until
        // their next date, and groups everything by the day it was finished.
        val visible = if (scope == TaskScope.Completed) {
            shown.inScope(scope, today) + restingDone.withTagFilter(tagFilter, d.tags)
        } else {
            shown.inScope(scope, today).sortedWith(sort.comparator())
        }
        val sections = when (scope) {
            TaskScope.Upcoming -> visible.groupedByDay().map { TaskSection(it.date, it.tasks) }
            TaskScope.Completed -> visible.groupedByDoneDay(zone, sort).map { TaskSection(it.date, it.tasks) }
            else -> listOf(TaskSection(null, visible))
        }

        val countedScopes: List<TaskScope> = listOf(
            TaskScope.Today,
            TaskScope.Upcoming,
            TaskScope.Overdue,
            TaskScope.NoDate,
        )

        val finishedToday = (
            allTasks.filter { task -> task.completedAt?.atZone(zone)?.toLocalDate() == today } + restingDone
        ).sortedByDescending { it.completedAt }

        return TaskListUiState(
            isLoading = false,
            scope = scope,
            sort = sort,
            tagFilter = tagFilter,
            tagTaskCounts = tagTaskCounts,
            untaggedCount = inView.count { it.tags.isEmpty() },
            sections = sections,
            tags = d.tags,
            counts = countedScopes.associateWith { allTasks.openCountIn(it, today) },
            selection = picked.intersect(visible.map { it.id }.toSet()),
            today = TodayStatus(left = allTasks.openCountIn(TaskScope.Today, today), done = finishedToday.size),
        )
    }

    fun selectScope(scope: TaskScope) {
        selectedScope.value = scope
        pickedIds.value = emptySet()
    }

    /** Picks a tag tab: All, a tag (with every tag under it) or No tag. */
    fun selectTagFilter(filter: TagFilter) {
        selectedTagFilter.value = filter
        pickedIds.value = emptySet()
    }

    fun setSort(sort: TaskSort) {
        selectedSort.value = sort
    }

    /**
     * Quick add follows the current view: Today and Overdue add a task due today, Upcoming one due
     * tomorrow, Goals one with no date. Things the text itself says (a date, tags) win over what
     * the view suggests. Every task lives in the Inbox.
     */
    fun addTask(title: String) = addParsed(ParsedQuickAdd(title = title.trim()))

    fun addParsed(parsed: ParsedQuickAdd) {
        if (parsed.title.isBlank()) return
        val current = uiState.value
        viewModelScope.launch {
            val today = today()
            val viewDate = when (current.scope) {
                TaskScope.Today, TaskScope.Overdue -> today
                TaskScope.Upcoming -> today.plusDays(1)
                else -> null
            }
            val added = QuickAddTask(taskRepository, tagRepository)(
                parsed = parsed,
                fallbackDate = viewDate,
                // A task added while a tag tab is open gets that tag too.
                extraTags = listOfNotNull(
                    (current.tagFilter as? TagFilter.OfTag)?.let { picked -> current.tags.firstOrNull { it.id == picked.tagId } },
                ),
            )
            // The new card is tinted for a moment, then fades back (the screen animates the fade).
            justAdded.value = added.id
            delay(JUST_ADDED_MILLIS)
            if (justAdded.value == added.id) justAdded.value = null
        }
    }

    // --- Subtasks, straight from the list ---

    /** Adds a subtask to the task [taskId] without opening the editor. */
    fun addSubtask(taskId: String, title: String) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            val task = taskRepository.getTask(taskId) ?: return@launch
            val added = Subtask(id = UUID.randomUUID().toString(), title = clean)
            taskRepository.saveTaskDetails(task.copy(subtasks = task.subtasks + added))
        }
    }

    /**
     * Ticks or unticks a subtask of the task [taskId]. Ticking the last open one finishes the
     * task itself.
     */
    fun toggleSubtask(taskId: String, subtaskId: String) {
        viewModelScope.launch {
            val task = taskRepository.getTask(taskId) ?: return@launch
            val updated = task.subtasks.map { if (it.id == subtaskId) it.copy(isCompleted = !it.isCompleted) else it }
            taskRepository.saveTaskDetails(task.copy(subtasks = updated))
            if (!task.isCompleted && updated.isNotEmpty() && updated.all { it.isCompleted }) {
                taskRepository.setCompleted(taskId, true)
            }
        }
    }

    // --- Selecting several tasks ---

    fun toggleSelected(taskId: String) {
        pickedIds.update { if (taskId in it) it - taskId else it + taskId }
    }

    fun clearSelection() {
        pickedIds.value = emptySet()
    }

    fun selectAll() {
        pickedIds.value = uiState.value.sections.flatMap { it.tasks }.map { it.id }.toSet()
    }

    fun completeSelected() {
        val ids = uiState.value.selection.toList()
        clearSelection()
        viewModelScope.launch { ids.forEach { taskRepository.setCompleted(it, true) } }
    }

    fun addTagToSelected(tag: Tag) {
        val ids = uiState.value.selection.toList()
        clearSelection()
        viewModelScope.launch { ids.forEach { addTag(it, tag) } }
    }

    /** Creates the tag (or finds the one with that name) and adds it to the selected tasks. */
    fun addNewTagToSelected(name: String) {
        val ids = uiState.value.selection.toList()
        clearSelection()
        viewModelScope.launch {
            val tag = try {
                tagRepository.createTag(name)
            } catch (e: IllegalArgumentException) {
                return@launch // blank name
            }
            ids.forEach { addTag(it, tag) }
        }
    }

    private suspend fun addTag(taskId: String, tag: Tag) {
        val task = taskRepository.getTask(taskId) ?: return
        if (task.tags.any { it.id == tag.id }) return
        taskRepository.saveTaskDetails(task.copy(tags = task.tags + tag))
    }

    fun deleteTasks(taskIds: List<String>) {
        clearSelection()
        viewModelScope.launch { taskIds.forEach { taskRepository.deleteTask(it) } }
    }

    fun restoreTasks(taskIds: List<String>) {
        viewModelScope.launch { taskIds.forEach { taskRepository.restoreTask(it) } }
    }

    /**
     * Finishes or reopens a task. [onResult] gets the task as it is afterwards, which tells the screen
     * where a repeating task went (it is still open, on its next date).
     */
    fun setCompleted(taskId: String, completed: Boolean, onResult: (Task?) -> Unit = {}) {
        viewModelScope.launch {
            taskRepository.setCompleted(taskId, completed)
            onResult(taskRepository.getTask(taskId))
        }
    }

    fun reorderTasks(orderedIds: List<String>) {
        viewModelScope.launch { taskRepository.reorderTasks(orderedIds) }
    }

    fun setPinned(taskId: String, pinned: Boolean) {
        viewModelScope.launch { taskRepository.setPinned(taskId, pinned) }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch { taskRepository.deleteTask(taskId) }
    }

    fun restoreTask(taskId: String) {
        viewModelScope.launch { taskRepository.restoreTask(taskId) }
    }

}
