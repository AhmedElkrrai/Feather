package com.elkrrai.feather.feature.tasklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elkrrai.feather.domain.model.Task
import com.elkrrai.feather.domain.model.TaskList
import com.elkrrai.feather.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TaskListUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val tasks: TaskRepository,
) : ViewModel() {

    // Milestone 1 has a single list; M2 adds list selection.
    private val listId = TaskList.INBOX_ID

    val uiState: StateFlow<TaskListUiState> = tasks.observeTasks(listId)
        .map { TaskListUiState(tasks = it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskListUiState())

    fun addTask(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch { tasks.addTask(listId, title) }
    }

    fun setCompleted(taskId: String, completed: Boolean) {
        viewModelScope.launch { tasks.setCompleted(taskId, completed) }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch { tasks.deleteTask(taskId) }
    }

    fun restoreTask(taskId: String) {
        viewModelScope.launch { tasks.restoreTask(taskId) }
    }
}
