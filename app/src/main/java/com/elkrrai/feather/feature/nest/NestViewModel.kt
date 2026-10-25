package com.elkrrai.feather.feature.nest

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elkrrai.feather.data.nest.NestTracker
import com.elkrrai.feather.domain.nest.NestFacts
import com.elkrrai.feather.domain.nest.NestItem
import com.elkrrai.feather.domain.nest.NestOutfit
import com.elkrrai.feather.domain.nest.NestStyle
import com.elkrrai.feather.domain.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    /** The nest the bird sits in. */
    val style: NestStyle = NestStyle.BRAIDED,
    /** What the bird wears, of what is earned. */
    val outfit: NestOutfit = NestOutfit(),
)

@HiltViewModel
class NestViewModel @Inject constructor(
    tracker: NestTracker,
    private val settings: SettingsRepository,
    @ApplicationContext context: Context,
) : ViewModel() {

    // A debug build shows every item as earned, to look at all of them; a release build never does.
    private val showEverything = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    val uiState: StateFlow<NestUiState> = combine(tracker.facts(), settings.settings) { facts, current ->
        val earned = if (showEverything) NestItem.entries.toSet() else current.nestUnlocked.mapNotNull(NestItem::fromId).toSet()
        val seen = current.nestSeen.mapNotNull(NestItem::fromId).toSet()
        NestUiState(facts = facts, earned = earned, news = earned - seen, style = current.nestStyle, outfit = current.nestOutfit)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NestUiState())

    /** Sits the bird in the nest the person picked. */
    fun pickStyle(style: NestStyle) {
        viewModelScope.launch { settings.setNestStyle(style) }
    }

    /** Dresses the bird in the garments the person picked. */
    fun wear(outfit: NestOutfit) {
        viewModelScope.launch { settings.setNestOutfit(outfit) }
    }

    /** Opening the nest is looking at what is in it; the quiet dot on Settings goes away. */
    fun markSeen() {
        viewModelScope.launch {
            val current = settings.current()
            if (!current.nestSeen.containsAll(current.nestUnlocked)) settings.setNestSeen(current.nestUnlocked)
        }
    }
}
