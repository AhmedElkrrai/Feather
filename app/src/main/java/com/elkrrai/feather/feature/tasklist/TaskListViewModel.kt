package com.elkrrai.feather.feature.tasklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elkrrai.feather.domain.model.Folder
import com.elkrrai.feather.domain.model.Tag
import com.elkrrai.feather.domain.model.Task
import com.elkrrai.feather.domain.model.TaskList
import com.elkrrai.feather.domain.repository.ListRepository
import com.elkrrai.feather.domain.repository.TagRepository
import com.elkrrai.feather.domain.repository.TaskRepository
import com.elkrrai.feather.domain.view.TaskScope
import com.elkrrai.feather.domain.view.TaskSort
import com.elkrrai.feather.domain.view.comparator
import com.elkrrai.feather.domain.view.groupedByDay
import com.elkrrai.feather.domain.view.inScope
import com.elkrrai.feather.domain.view.openCountIn
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Tasks of one day (Upcoming view) or, with a null [date], the whole view in one block. */
data class TaskSection(val date: LocalDate?, val tasks: List<Task>)

data class TaskListUiState(
    val isLoading: Boolean = true,
    val scope: TaskScope = TaskScope.InList(TaskList.INBOX_ID),
    val sort: TaskSort = TaskSort.CREATED,
    val sections: List<TaskSection> = emptyList(),
    val lists: List<TaskList> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val tags: List<Tag> = emptyList(),
    /** Open-task counts for every smart view, list and tag, for the drawer badges. */
    val counts: Map<TaskScope, Int> = emptyMap(),
) {
    val isEmpty: Boolean get() = sections.all { it.tasks.isEmpty() }
}

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val listRepository: ListRepository,
    private val tagRepository: TagRepository,
    private val clock: Clock,
) : ViewModel() {

    private val inbox = TaskScope.InList(TaskList.INBOX_ID)
    private val selectedScope = MutableStateFlow<TaskScope>(inbox)
    private val selectedSort = MutableStateFlow(TaskSort.CREATED)

    private data class Data(
        val tasks: List<Task>,
        val lists: List<TaskList>,
        val folders: List<Folder>,
        val tags: List<Tag>,
    )

    private val data = combine(
        taskRepository.observeTasks(),
        listRepository.observeLists(),
        listRepository.observeFolders(),
        tagRepository.observeTags(),
    ) { tasks, lists, folders, tags -> Data(tasks, lists, folders, tags) }

    val uiState: StateFlow<TaskListUiState> = combine(data, selectedScope, selectedSort) { d, scope, sort ->
        buildState(d, scope, sort)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskListUiState())

    /** The Clock is UTC; "today" must follow the user's own time zone. */
    private fun today(): LocalDate = LocalDate.now(clock.withZone(ZoneId.systemDefault()))

    private fun buildState(d: Data, selected: TaskScope, sort: TaskSort): TaskListUiState {
        val today = today()
        // A deleted list or tag falls back to the Inbox.
        val scope = when (selected) {
            is TaskScope.InList -> if (d.lists.any { it.id == selected.listId }) selected else inbox
            is TaskScope.WithTag -> if (d.tags.any { it.id == selected.tagId }) selected else inbox
            else -> selected
        }

        val visible = d.tasks.inScope(scope, today).sortedWith(sort.comparator())
        val sections = if (scope == TaskScope.Upcoming) {
            visible.groupedByDay().map { TaskSection(it.date, it.tasks) }
        } else {
            listOf(TaskSection(null, visible))
        }

        val countedScopes: List<TaskScope> = listOf(
            TaskScope.Today,
            TaskScope.Upcoming,
            TaskScope.Overdue,
            TaskScope.All,
            TaskScope.NoDate,
        ) + d.lists.map { TaskScope.InList(it.id) } + d.tags.map { TaskScope.WithTag(it.id) }

        return TaskListUiState(
            isLoading = false,
            scope = scope,
            sort = sort,
            sections = sections,
            lists = d.lists,
            folders = d.folders,
            tags = d.tags,
            counts = countedScopes.associateWith { d.tasks.openCountIn(it, today) },
        )
    }

    fun selectScope(scope: TaskScope) {
        selectedScope.value = scope
    }

    fun setSort(sort: TaskSort) {
        selectedSort.value = sort
    }

    /**
     * Quick add follows the current view: a list view adds to that list; Today and Overdue add
     * a task due today, Upcoming one due tomorrow; a tag view tags the new task.
     */
    fun addTask(title: String) {
        if (title.isBlank()) return
        val current = uiState.value
        viewModelScope.launch {
            val today = today()
            val scope = current.scope
            val (listId, dueDate) = when (scope) {
                is TaskScope.InList -> scope.listId to null
                TaskScope.Today, TaskScope.Overdue -> TaskList.INBOX_ID to today
                TaskScope.Upcoming -> TaskList.INBOX_ID to today.plusDays(1)
                else -> TaskList.INBOX_ID to null
            }
            val task = taskRepository.addTask(listId, title, dueDate)
            if (scope is TaskScope.WithTag) {
                current.tags.firstOrNull { it.id == scope.tagId }?.let { tag ->
                    taskRepository.saveTaskDetails(task.copy(tags = listOf(tag)))
                }
            }
        }
    }

    fun setCompleted(taskId: String, completed: Boolean) {
        viewModelScope.launch { taskRepository.setCompleted(taskId, completed) }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch { taskRepository.deleteTask(taskId) }
    }

    fun restoreTask(taskId: String) {
        viewModelScope.launch { taskRepository.restoreTask(taskId) }
    }

    // --- Lists, folders and tags ---

    fun createList(name: String, folderId: String?, colorArgb: Int?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val list = listRepository.createList(name, folderId, colorArgb)
            selectedScope.value = TaskScope.InList(list.id)
        }
    }

    fun updateList(list: TaskList, name: String, colorArgb: Int?, folderId: String?) {
        viewModelScope.launch { listRepository.updateList(list.id, name, colorArgb, folderId) }
    }

    fun deleteList(list: TaskList) {
        if (list.isInbox) return
        val selected = selectedScope.value
        if (selected is TaskScope.InList && selected.listId == list.id) selectedScope.value = inbox
        viewModelScope.launch { listRepository.deleteList(list.id) }
    }

    fun createFolder(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { listRepository.createFolder(name) }
    }

    fun renameFolder(folder: Folder, name: String) {
        viewModelScope.launch { listRepository.renameFolder(folder.id, name) }
    }

    fun deleteFolder(folder: Folder) {
        viewModelScope.launch { listRepository.deleteFolder(folder.id) }
    }

    fun renameTag(tag: Tag, name: String) {
        viewModelScope.launch { tagRepository.renameTag(tag.id, name) }
    }

    fun deleteTag(tag: Tag) {
        val selected = selectedScope.value
        if (selected is TaskScope.WithTag && selected.tagId == tag.id) selectedScope.value = inbox
        viewModelScope.launch { tagRepository.deleteTag(tag.id) }
    }
}
