package com.elkrrai.feather.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elkrrai.feather.domain.model.Task
import com.elkrrai.feather.domain.model.TaskList
import com.elkrrai.feather.domain.repository.ListRepository
import com.elkrrai.feather.domain.repository.TaskRepository
import com.elkrrai.feather.domain.view.TaskSort
import com.elkrrai.feather.domain.view.comparator
import com.elkrrai.feather.domain.view.search
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val results: List<Task> = emptyList(),
    val lists: List<TaskList> = emptyList(),
) {
    val hasQuery: Boolean get() = query.isNotBlank()
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val tasks: TaskRepository,
    lists: ListRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")

    val uiState: StateFlow<SearchUiState> = combine(
        tasks.observeTasks(),
        lists.observeLists(),
        query,
    ) { all, allLists, q ->
        SearchUiState(
            query = q,
            results = all.search(q).sortedWith(TaskSort.CREATED.comparator()),
            lists = allLists,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun setCompleted(taskId: String, completed: Boolean) {
        viewModelScope.launch { tasks.setCompleted(taskId, completed) }
    }
}
