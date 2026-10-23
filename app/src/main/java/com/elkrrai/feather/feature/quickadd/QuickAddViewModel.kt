package com.elkrrai.feather.feature.quickadd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elkrrai.feather.domain.quickadd.ParsedQuickAdd
import com.elkrrai.feather.domain.repository.TagRepository
import com.elkrrai.feather.domain.repository.TaskRepository
import com.elkrrai.feather.widget.WidgetUpdater
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
import java.time.LocalDate

/** Behind [QuickAddActivity]: the tags to offer, and saving the task. */
@HiltViewModel
class QuickAddViewModel @Inject constructor(
    private val tasks: TaskRepository,
    private val tags: TagRepository,
    private val settings: SettingsRepository,
    private val widgetUpdater: WidgetUpdater,
) : ViewModel() {

    val existingTags: StateFlow<List<Tag>> = tags.observeTags()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** A task was dictated: a milestone for the nest. */
    fun voiceUsed() {
        viewModelScope.launch { settings.setVoiceUsed(true) }
    }

    /**
     * Saves the task and then calls [onDone]. The write is
     * protected from cancellation, so it completes even if the screen is closing.
     */
    fun add(parsed: ParsedQuickAdd, fallbackDate: LocalDate? = null, onDone: () -> Unit) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                QuickAddTask(tasks, tags)(parsed, fallbackDate)
            }
            widgetUpdater.refreshNow()
            onDone()
        }
    }
}
