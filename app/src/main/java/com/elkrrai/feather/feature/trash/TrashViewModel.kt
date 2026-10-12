package com.elkrrai.feather.feature.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elkrrai.feather.domain.model.Task
import com.elkrrai.feather.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TrashUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val tasks: TaskRepository,
) : ViewModel() {

    val uiState: StateFlow<TrashUiState> = tasks.observeTrash()
        .map { TrashUiState(tasks = it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrashUiState())

    fun restore(taskId: String) {
        viewModelScope.launch { tasks.restoreTask(taskId) }
    }

    fun deleteForever(taskId: String) {
        viewModelScope.launch { tasks.deleteForever(taskId) }
    }

    fun emptyTrash() {
        viewModelScope.launch { tasks.emptyTrash() }
    }
}
