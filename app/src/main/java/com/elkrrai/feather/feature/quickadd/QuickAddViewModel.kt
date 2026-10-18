package com.elkrrai.feather.feature.quickadd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elkrrai.feather.domain.model.TaskList
import com.elkrrai.feather.domain.quickadd.ParsedQuickAdd
import com.elkrrai.feather.domain.repository.ListRepository
import com.elkrrai.feather.domain.repository.TagRepository
import com.elkrrai.feather.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.elkrrai.feather.domain.model.Tag
import com.elkrrai.feather.domain.settings.SettingsRepository

/** Behind [QuickAddActivity]: the lists for `@list`, and saving the task. */
@HiltViewModel
class QuickAddViewModel @Inject constructor(
    private val tasks: TaskRepository,
    private val tags: TagRepository,
    listRepository: ListRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    val lists: StateFlow<List<TaskList>> = listRepository.observeLists()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val existingTags: StateFlow<List<Tag>> = tags.observeTags()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** A task was dictated: a milestone for the nest. */
    fun voiceUsed() {
        viewModelScope.launch { settings.setVoiceUsed(true) }
    }

    /**
     * Saves the task in [listId] (the Inbox when null) and then calls [onDone]. The write is
     * protected from cancellation, so it completes even if the screen is closing.
     */
    fun add(parsed: ParsedQuickAdd, listId: String?, onDone: () -> Unit) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                QuickAddTask(tasks, tags)(parsed, listId = listId ?: TaskList.INBOX_ID)
            }
            onDone()
        }
    }
}
