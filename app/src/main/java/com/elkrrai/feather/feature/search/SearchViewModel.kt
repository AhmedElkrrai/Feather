package com.elkrrai.feather.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elkrrai.feather.domain.model.Tag
import com.elkrrai.feather.domain.model.Task
import com.elkrrai.feather.domain.model.TagRow
import com.elkrrai.feather.domain.model.inTreeOrder
import com.elkrrai.feather.domain.repository.TagRepository
import com.elkrrai.feather.domain.repository.TaskRepository
import com.elkrrai.feather.domain.view.TaskSort
import com.elkrrai.feather.domain.view.comparator
import com.elkrrai.feather.domain.view.searchTasks
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
    /** Every tag, as an indented tree, for the filter chips. */
    val tags: List<TagRow> = emptyList(),
    /** The tag picked as a filter: its tasks and the tasks of every tag under it are listed. */
    val selectedTagId: String? = null,
) {
    /** Something is being searched for: text, a picked tag, or both. */
    val isSearching: Boolean get() = query.isNotBlank() || selectedTagId != null

    /** The picked tag has tags under it, so the screen can say they are included. */
    val selectedTagHasChildren: Boolean
        get() = selectedTagId != null && tags.any { it.tag.parentId == selectedTagId }

    val selectedTag: Tag? get() = tags.firstOrNull { it.tag.id == selectedTagId }?.tag

    /** The picked tag and the tags above it, from the top-level one down; empty when no tag is picked. */
    val selectedPath: List<Tag>
        get() {
            val byId = tags.associate { it.tag.id to it.tag }
            val path = ArrayList<Tag>()
            var current = selectedTagId?.let { byId[it] }
            while (current != null && path.size <= byId.size) {
                path.add(0, current)
                current = current.parentId?.let { byId[it] }
            }
            return path
        }
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val tasks: TaskRepository,
    tagRepository: TagRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val selectedTag = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SearchUiState> = combine(
        tasks.observeTasks(),
        tagRepository.observeTags(),
        query,
        selectedTag,
    ) { all, allTags, q, picked ->
        // A tag that was deleted while it was picked no longer filters anything.
        val pickedTag = picked?.takeIf { id -> allTags.any { it.id == id } }
        SearchUiState(
            query = q,
            results = all.searchTasks(q, allTags, pickedTag).sortedWith(TaskSort.CREATED.comparator()),
            tags = allTags.inTreeOrder(),
            selectedTagId = pickedTag,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    /** Picks a tag as the filter; null is the "All" tab (no tag filter). */
    fun onTagPicked(tagId: String?) {
        selectedTag.value = tagId
    }

    fun setCompleted(taskId: String, completed: Boolean) {
        viewModelScope.launch { tasks.setCompleted(taskId, completed) }
    }
}
