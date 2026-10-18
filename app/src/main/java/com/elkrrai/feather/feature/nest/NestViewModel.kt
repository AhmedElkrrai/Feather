package com.elkrrai.feather.feature.nest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elkrrai.feather.data.nest.NestTracker
import com.elkrrai.feather.domain.nest.NestFacts
import com.elkrrai.feather.domain.nest.NestItem
import com.elkrrai.feather.domain.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the nest screen shows: how far along the person is, what is earned, and what is new. */
data class NestUiState(
    val facts: NestFacts = NestFacts(),
    val earned: Set<NestItem> = emptySet(),
    /** Earned items the person has not looked at yet. */
    val news: Set<NestItem> = emptySet(),
)

@HiltViewModel
class NestViewModel @Inject constructor(
    tracker: NestTracker,
    private val settings: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<NestUiState> = combine(tracker.facts(), settings.settings) { facts, current ->
        val earned = current.nestUnlocked.mapNotNull(NestItem::fromId).toSet()
        val seen = current.nestSeen.mapNotNull(NestItem::fromId).toSet()
        NestUiState(facts = facts, earned = earned, news = earned - seen)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NestUiState())

    /** Opening the nest is looking at what is in it; the quiet dot on Settings goes away. */
    fun markSeen() {
        viewModelScope.launch {
            val current = settings.current()
            if (!current.nestSeen.containsAll(current.nestUnlocked)) settings.setNestSeen(current.nestUnlocked)
        }
    }
}
