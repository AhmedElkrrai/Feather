package com.elkrrai.feather.feature.habits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.elkrrai.feather.domain.habit.DAYS_IN_WEEK
import com.elkrrai.feather.domain.habit.HabitKind
import com.elkrrai.feather.domain.habit.HabitState
import com.elkrrai.feather.domain.habit.MAX_DAILY_TARGET
import com.elkrrai.feather.domain.habit.MAX_HABIT_TARGET
import com.elkrrai.feather.domain.habit.forWeek
import com.elkrrai.feather.domain.habit.queuedAfter
import com.elkrrai.feather.domain.habit.weekStartOf
import com.elkrrai.feather.domain.repository.HabitRepository
import com.elkrrai.feather.domain.time.FIRST_DAY_OF_WEEK
import com.elkrrai.feather.ui.navigation.HabitEditRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the habit form shows. The numbers are kept as typed. */
data class HabitEditUiState(
    val isLoading: Boolean = true,
    val isNew: Boolean = true,
    val name: String = "",
    val kind: HabitKind = HabitKind.DAYS,
    val unit: String = "",
    val target: String = "3",
    val daily: String = "1",
    /** Set once the habit was saved (or turned out not to exist); the screen then goes back. */
    val finished: Boolean = false,
) {
    val targetValue: Int? get() = target.toIntOrNull()
    val dailyValue: Int get() = daily.toIntOrNull() ?: 1
    val maxTarget: Int get() = if (kind == HabitKind.DAYS) DAYS_IN_WEEK else MAX_HABIT_TARGET
    val targetValid: Boolean get() = targetValue?.let { it in 1..maxTarget } == true
    val canSave: Boolean get() = !isLoading && name.isNotBlank() && targetValid && dailyValue in 1..MAX_DAILY_TARGET
}

/**
 * Behind the screen that makes a habit or edits one. When editing, the name and the unit change at once and a
 * changed goal is queued for the start of next week, as the dialog this screen replaced did.
 */
@HiltViewModel
class HabitEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: HabitRepository,
) : ViewModel() {

    private val habitId: String? = savedStateHandle.toRoute<HabitEditRoute>().habitId

    private val _uiState = MutableStateFlow(HabitEditUiState(isLoading = habitId != null, isNew = habitId == null))
    val uiState: StateFlow<HabitEditUiState> = _uiState.asStateFlow()

    /** The habit as it was when the form opened, to tell whether the goal changed. */
    private var loaded: HabitState? = null

    init {
        if (habitId != null) {
            viewModelScope.launch {
                val habit = repository.observeHabits().first().firstOrNull { it.habit.id == habitId }
                if (habit == null) {
                    _uiState.update { it.copy(finished = true) }
                    return@launch
                }
                loaded = habit
                val weekStart = thisWeek()
                val shown = habit.goals.queuedAfter(weekStart) ?: habit.goals.forWeek(weekStart)
                _uiState.value = HabitEditUiState(
                    isLoading = false,
                    isNew = false,
                    name = habit.habit.name,
                    kind = habit.habit.kind,
                    unit = habit.habit.unit,
                    target = shown?.target?.toString() ?: if (habit.habit.kind == HabitKind.DAYS) "3" else "20",
                    daily = shown?.dailyTarget?.toString() ?: "1",
                )
            }
        }
    }

    fun onNameChange(name: String) = _uiState.update { it.copy(name = name) }

    fun onKindChange(kind: HabitKind) = _uiState.update {
        // a week has seven days: a bigger number typed for an amount does not carry over
        val target = if (kind == HabitKind.DAYS && (it.target.toIntOrNull() ?: 0) > DAYS_IN_WEEK) "3" else it.target
        it.copy(kind = kind, target = target)
    }

    fun onTargetChange(text: String) = _uiState.update { it.copy(target = text.filter(Char::isDigit).trimStart('0').take(4)) }

    fun onDailyChange(text: String) = _uiState.update { it.copy(daily = text.filter(Char::isDigit).trimStart('0').take(4)) }

    fun onUnitChange(text: String) = _uiState.update { it.copy(unit = text.take(20)) }

    fun save() {
        val state = _uiState.value
        val target = state.targetValue
        if (!state.canSave || target == null) return
        viewModelScope.launch {
            val weekStart = thisWeek()
            val existing = loaded
            if (existing == null) {
                repository.addHabit(state.name, state.kind, state.unit, target, state.dailyValue, weekStart)
            } else {
                val id = existing.habit.id
                repository.rename(id, state.name, state.unit)
                val shown = existing.goals.queuedAfter(weekStart) ?: existing.goals.forWeek(weekStart)
                val changed = shown == null || shown.target != target || shown.dailyTarget != state.dailyValue
                if (changed) repository.queueGoal(id, target, state.dailyValue, weekStart.plusWeeks(1))
            }
            _uiState.update { it.copy(finished = true) }
        }
    }

    private fun thisWeek(): LocalDate = weekStartOf(LocalDate.now(), FIRST_DAY_OF_WEEK)
}
