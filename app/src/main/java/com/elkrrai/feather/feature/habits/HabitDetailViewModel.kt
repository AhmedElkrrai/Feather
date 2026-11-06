package com.elkrrai.feather.feature.habits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.elkrrai.feather.domain.habit.HabitState
import com.elkrrai.feather.domain.habit.SwipeHintHistory
import com.elkrrai.feather.domain.habit.shouldShowSwipeHint
import com.elkrrai.feather.domain.repository.HabitRepository
import com.elkrrai.feather.domain.settings.SettingsRepository
import com.elkrrai.feather.ui.navigation.HabitDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Behind the page that looks back at one habit, week by week. */
@HiltViewModel
class HabitDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: HabitRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<HabitDetailRoute>()
    private val habitId: String = route.habitId

    /** The week the page should open on (the first day of it), or null for this week. */
    val initialWeek: LocalDate? = route.weekStart?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    /** The habit, or null while it loads or after it was deleted. */
    val habit: StateFlow<HabitState?> = repository.observeHabits()
        .map { all -> all.firstOrNull { it.habit.id == habitId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _swipeHint = MutableStateFlow(false)

    /** True when this visit shows the "swipe to see earlier weeks" hint. */
    val swipeHint: StateFlow<Boolean> = _swipeHint.asStateFlow()

    private var hintDecided = false

    /**
     * Decides once per visit whether to show the swipe hint (see [shouldShowSwipeHint]) and, when it
     * does, counts it. Turning the phone does not decide again.
     */
    fun decideSwipeHint(hasEarlierWeek: Boolean, thisWeek: LocalDate) {
        if (hintDecided || !hasEarlierWeek) return
        hintDecided = true
        viewModelScope.launch {
            val saved = settings.current()
            val history = SwipeHintHistory(saved.habitSwipeHintsShown, saved.habitSwipeHintWeek)
            if (shouldShowSwipeHint(history, hasEarlierWeek, thisWeek, Random.nextFloat())) {
                settings.setHabitSwipeHint(history.shown + 1, thisWeek)
                _swipeHint.value = true
            }
        }
    }
}
