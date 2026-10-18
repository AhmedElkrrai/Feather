package com.elkrrai.feather.feature.rhythm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elkrrai.feather.domain.model.TaskList
import com.elkrrai.feather.domain.repository.ListRepository
import com.elkrrai.feather.domain.rhythm.CompletionRecord
import com.elkrrai.feather.domain.rhythm.RhythmRepository
import com.elkrrai.feather.domain.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the Your rhythm screen is built from; the statistics are worked out in the screen, with its locale. */
data class RhythmUiState(
    val isLoading: Boolean = true,
    val records: List<CompletionRecord> = emptyList(),
    val lists: List<TaskList> = emptyList(),
    val pausedDays: Set<LocalDate> = emptySet(),
)

@HiltViewModel
class RhythmViewModel @Inject constructor(
    rhythm: RhythmRepository,
    listRepository: ListRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
) : ViewModel() {

    val uiState: StateFlow<RhythmUiState> = combine(
        rhythm.observeCompletions(),
        listRepository.observeLists(),
        settingsRepository.settings,
    ) { records, lists, settings ->
        RhythmUiState(isLoading = false, records = records, lists = lists, pausedDays = settings.rhythmPausedDays)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RhythmUiState())

    private fun today(): LocalDate = LocalDate.now(clock.withZone(ZoneId.systemDefault()))

    /** Leaves today and the next [days] - 1 days out of the rhythm. */
    fun pause(days: Int) {
        val start = today()
        viewModelScope.launch {
            val current = settingsRepository.current().rhythmPausedDays
            settingsRepository.setRhythmPausedDays(current + (0 until days).map { start.plusDays(it.toLong()) })
        }
    }

    /** Counts today and the days ahead again; days already paused in the past stay paused. */
    fun resume() {
        val start = today()
        viewModelScope.launch {
            val current = settingsRepository.current().rhythmPausedDays
            settingsRepository.setRhythmPausedDays(current.filter { it.isBefore(start) }.toSet())
        }
    }
}
