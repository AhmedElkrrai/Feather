package com.elkrrai.feather.feature.habits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.elkrrai.feather.domain.habit.HabitState
import com.elkrrai.feather.domain.repository.HabitRepository
import com.elkrrai.feather.ui.navigation.HabitOverviewRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Behind the page that shows every week of one habit at once. */
@HiltViewModel
class HabitOverviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: HabitRepository,
) : ViewModel() {

    private val habitId: String = savedStateHandle.toRoute<HabitOverviewRoute>().habitId

    /** The habit, or null while it loads or after it was deleted. */
    val habit: StateFlow<HabitState?> = repository.observeHabits()
        .map { all -> all.firstOrNull { it.habit.id == habitId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
