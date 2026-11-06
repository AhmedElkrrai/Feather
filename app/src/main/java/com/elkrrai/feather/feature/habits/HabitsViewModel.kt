package com.elkrrai.feather.feature.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elkrrai.feather.domain.habit.HabitState
import com.elkrrai.feather.domain.repository.HabitRepository
import com.elkrrai.feather.domain.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Behind the Habits screen and the Habits tab of Your rhythm. The dates (which week it is, when the
 * next one starts) come from the screen, which knows the locale and the first day of the week.
 */
@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val repository: HabitRepository,
    settings: SettingsRepository,
) : ViewModel() {

    /** The habits, or null while they load. */
    val habits: StateFlow<List<HabitState>?> = repository.observeHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** For each habit: how many met weeks its tree had when it was last opened. */
    val treesSeen: StateFlow<Map<String, Int>> = settings.settings
        .map { it.habitTreesSeen }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun setAmount(id: String, date: LocalDate, amount: Int) {
        viewModelScope.launch { repository.setAmount(id, date, amount) }
    }

    fun setStageDone(habitId: String, date: LocalDate, stageId: String, done: Boolean) {
        viewModelScope.launch { repository.setStageDone(habitId, date, stageId, done) }
    }

    /** Debug builds only: a habit for every picture of the tree (see [addTreeStageHabits]). */
    fun addTreeStages(today: LocalDate, firstDay: DayOfWeek) {
        viewModelScope.launch { repository.addTreeStageHabits(today, firstDay) }
    }

    /** The habits dragged into a new order. */
    fun reorder(orderedIds: List<String>) {
        viewModelScope.launch { repository.reorderHabits(orderedIds) }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }
}
