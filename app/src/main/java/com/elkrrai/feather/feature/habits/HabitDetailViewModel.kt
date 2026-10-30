package com.elkrrai.feather.feature.habits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.elkrrai.feather.domain.habit.HabitState
import com.elkrrai.feather.domain.repository.HabitRepository
import com.elkrrai.feather.ui.navigation.HabitDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Behind the page that looks back at one habit, week by week. */
@HiltViewModel
class HabitDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: HabitRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<HabitDetailRoute>()
    private val habitId: String = route.habitId

    /** The week the page should open on (the first day of it), or null for this week. */
    val initialWeek: LocalDate? = route.weekStart?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    /** The habit, or null while it loads or after it was deleted. */
    val habit: StateFlow<HabitState?> = repository.observeHabits()
        .map { all -> all.firstOrNull { it.habit.id == habitId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
