package com.elkrrai.feather.feature.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elkrrai.feather.domain.model.Tag
import com.elkrrai.feather.domain.model.TagRow
import com.elkrrai.feather.domain.model.inTreeOrder
import com.elkrrai.feather.domain.repository.TagRepository
import com.elkrrai.feather.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TagsUiState(
    val isLoading: Boolean = true,
    /** Every tag as an indented tree: each tag is followed by the tags under it. */
    val rows: List<TagRow> = emptyList(),
    /** How many tasks carry each tag directly (tasks of the tags under it are not counted). */
    val taskCounts: Map<String, Int> = emptyMap(),
) {
    val tags: List<Tag> get() = rows.map { it.tag }

    fun nameOf(tagId: String?): String? = rows.firstOrNull { it.tag.id == tagId }?.tag?.name
}

/** Behind the Tags screen: creating, renaming, moving and deleting tags. */
@HiltViewModel
class TagsViewModel @Inject constructor(
    private val tagRepository: TagRepository,
    tasks: TaskRepository,
) : ViewModel() {

    val uiState: StateFlow<TagsUiState> = combine(
        tagRepository.observeTags(),
        tasks.observeTasks(),
    ) { tags, all ->
        val counts = HashMap<String, Int>()
        all.forEach { task -> task.tags.forEach { counts.merge(it.id, 1, Int::plus) } }
        TagsUiState(isLoading = false, rows = tags.inTreeOrder(), taskCounts = counts)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TagsUiState())

    /** Creates the tag, under [parentId] when it is given (and the name can be a path like "work/bugfix"). */
    fun create(name: String, parentId: String? = null) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val tag = try {
                tagRepository.createTagPath(name)
            } catch (e: IllegalArgumentException) {
                return@launch // only slashes
            }
            // a parent picked in the dialog wins over a slash in the name only when the tag is at the top
            if (parentId != null && tag.parentId == null) tagRepository.setParent(tag.id, parentId)
        }
    }

    /** [onResult] gets false when the name is blank or another tag already has it. */
    fun rename(tagId: String, name: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch { onResult(tagRepository.renameTag(tagId, name)) }
    }

    /** Puts the tag under [parentId], or at the top level when null. Moves that would loop are refused. */
    fun setParent(tagId: String, parentId: String?) {
        viewModelScope.launch { tagRepository.setParent(tagId, parentId) }
    }

    fun delete(tagId: String) {
        viewModelScope.launch { tagRepository.deleteTag(tagId) }
    }
}
