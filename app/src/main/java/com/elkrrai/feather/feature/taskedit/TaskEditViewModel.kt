package com.elkrrai.feather.feature.taskedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.elkrrai.feather.domain.model.Priority
import com.elkrrai.feather.domain.model.Subtask
import com.elkrrai.feather.domain.model.Tag
import com.elkrrai.feather.domain.model.Task
import com.elkrrai.feather.domain.model.TaskList
import com.elkrrai.feather.domain.repository.ListRepository
import com.elkrrai.feather.domain.repository.TagRepository
import com.elkrrai.feather.domain.repository.TaskRepository
import com.elkrrai.feather.ui.navigation.TaskEditRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskEditUiState(
    val isLoading: Boolean = true,
    val isNew: Boolean = true,
    val title: String = "",
    val notes: String = "",
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    val priority: Priority = Priority.NONE,
    val listId: String = TaskList.INBOX_ID,
    val isPinned: Boolean = false,
    /** The tags currently attached to the task. */
    val tags: List<Tag> = emptyList(),
    /** The checklist, in order. Blank items are dropped when saving. */
    val subtasks: List<Subtask> = emptyList(),
    val availableLists: List<TaskList> = emptyList(),
    val availableTags: List<Tag> = emptyList(),
    val isSaving: Boolean = false,
    /** Set once the task was saved (or turned out not to exist); the screen then navigates back. */
    val finished: Boolean = false,
) {
    val canSave: Boolean get() = title.isNotBlank() && !isSaving
}

@HiltViewModel
class TaskEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tasks: TaskRepository,
    private val lists: ListRepository,
    private val tagRepository: TagRepository,
) : ViewModel() {

    private val taskId: String? = savedStateHandle.toRoute<TaskEditRoute>().taskId

    private val _uiState = MutableStateFlow(
        TaskEditUiState(isLoading = taskId != null, isNew = taskId == null)
    )
    val uiState: StateFlow<TaskEditUiState> = _uiState.asStateFlow()

    /** The task as loaded, kept so saving preserves fields the editor does not show. */
    private var original: Task? = null

    init {
        viewModelScope.launch {
            lists.observeLists().collect { all -> _uiState.update { it.copy(availableLists = all) } }
        }
        viewModelScope.launch {
            tagRepository.observeTags().collect { all -> _uiState.update { it.copy(availableTags = all) } }
        }
        if (taskId != null) load(taskId)
    }

    private fun load(id: String) {
        viewModelScope.launch {
            val task = tasks.getTask(id)
            if (task == null) {
                _uiState.update { it.copy(isLoading = false, finished = true) }
                return@launch
            }
            original = task
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isNew = false,
                    title = task.title,
                    notes = task.notes,
                    dueDate = task.dueDate,
                    dueTime = task.dueTime,
                    priority = task.priority,
                    listId = task.listId,
                    isPinned = task.isPinned,
                    tags = task.tags,
                    subtasks = task.subtasks,
                )
            }
        }
    }

    fun onTitleChange(title: String) = _uiState.update { it.copy(title = title) }

    fun onNotesChange(notes: String) = _uiState.update { it.copy(notes = notes) }

    /** Clearing the date also clears the time, since a time without a date is meaningless. */
    fun onDueDateChange(date: LocalDate?) = _uiState.update {
        it.copy(dueDate = date, dueTime = if (date == null) null else it.dueTime)
    }

    fun onDueTimeChange(time: LocalTime?) = _uiState.update { it.copy(dueTime = time) }

    fun onPriorityChange(priority: Priority) = _uiState.update { it.copy(priority = priority) }

    fun onListChange(listId: String) = _uiState.update { it.copy(listId = listId) }

    fun onPinnedChange(pinned: Boolean) = _uiState.update { it.copy(isPinned = pinned) }

    fun onTagToggle(tag: Tag) = _uiState.update { state ->
        val attached = state.tags.any { it.id == tag.id }
        state.copy(tags = if (attached) state.tags.filterNot { it.id == tag.id } else state.tags + tag)
    }

    /** Creates the tag (or finds the existing one with that name) and attaches it. */
    fun onCreateTag(name: String) {
        viewModelScope.launch {
            val tag = try {
                tagRepository.createTag(name)
            } catch (e: IllegalArgumentException) {
                return@launch // blank name
            }
            _uiState.update { state ->
                if (state.tags.any { it.id == tag.id }) state else state.copy(tags = state.tags + tag)
            }
        }
    }

    fun onAddSubtask(title: String) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        _uiState.update {
            it.copy(subtasks = it.subtasks + Subtask(id = UUID.randomUUID().toString(), title = clean))
        }
    }

    fun onSubtaskToggle(id: String) = _uiState.update { state ->
        state.copy(
            subtasks = state.subtasks.map { if (it.id == id) it.copy(isCompleted = !it.isCompleted) else it }
        )
    }

    fun onSubtaskTitleChange(id: String, title: String) = _uiState.update { state ->
        state.copy(subtasks = state.subtasks.map { if (it.id == id) it.copy(title = title) else it })
    }

    fun onSubtaskRemove(id: String) = _uiState.update { state ->
        state.copy(subtasks = state.subtasks.filterNot { it.id == id })
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val base = original ?: tasks.addTask(state.listId, state.title)
            tasks.saveTaskDetails(
                base.copy(
                    title = state.title.trim(),
                    notes = state.notes,
                    dueDate = state.dueDate,
                    dueTime = state.dueTime,
                    priority = state.priority,
                    listId = state.listId,
                    isPinned = state.isPinned,
                    tags = state.tags,
                    subtasks = state.subtasks,
                )
            )
            _uiState.update { it.copy(isSaving = false, finished = true) }
        }
    }
}
