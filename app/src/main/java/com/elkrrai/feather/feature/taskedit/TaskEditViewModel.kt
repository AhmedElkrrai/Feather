package com.elkrrai.feather.feature.taskedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.elkrrai.feather.domain.model.Priority
import com.elkrrai.feather.domain.model.Task
import com.elkrrai.feather.domain.model.TaskList
import com.elkrrai.feather.domain.repository.TaskRepository
import com.elkrrai.feather.ui.navigation.TaskEditRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalTime
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
) : ViewModel() {

    private val taskId: String? = savedStateHandle.toRoute<TaskEditRoute>().taskId

    private val _uiState = MutableStateFlow(
        TaskEditUiState(isLoading = taskId != null, isNew = taskId == null)
    )
    val uiState: StateFlow<TaskEditUiState> = _uiState.asStateFlow()

    /** The task as loaded, kept so saving preserves fields the editor does not show. */
    private var original: Task? = null

    init {
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

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val base = original ?: tasks.addTask(TaskList.INBOX_ID, state.title)
            tasks.saveTask(
                base.copy(
                    title = state.title.trim(),
                    notes = state.notes,
                    dueDate = state.dueDate,
                    dueTime = state.dueTime,
                    priority = state.priority,
                )
            )
            _uiState.update { it.copy(isSaving = false, finished = true) }
        }
    }
}
