package com.elkrrai.feather.feature.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elkrrai.feather.domain.habit.HabitKind
import com.elkrrai.feather.domain.habit.HabitState
import com.elkrrai.feather.domain.repository.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Behind the Habits screen and the Habits tab of Your rhythm. The dates (which week it is, when the
 * next one starts) come from the screen, which knows the locale and the first day of the week.
 */
@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val repository: HabitRepository,
) : ViewModel() {

    /** The habits, or null while they load. */
    val habits: StateFlow<List<HabitState>?> = repository.observeHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun add(name: String, kind: HabitKind, unit: String, target: Int, dailyTarget: Int, thisWeekStart: LocalDate) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.addHabit(name, kind, unit, target, dailyTarget, thisWeekStart) }
    }

    /**
     * The name and the unit change at once. A changed goal is queued for the start of the next week;
     * [goalChanged] says whether the numbers differ from what was shown.
     */
    fun edit(
        id: String,
        name: String,
        unit: String,
        target: Int,
        dailyTarget: Int,
        goalChanged: Boolean,
        nextWeekStart: LocalDate,
    ) {
        viewModelScope.launch {
            repository.rename(id, name, unit)
            if (goalChanged) repository.queueGoal(id, target, dailyTarget, nextWeekStart)
        }
    }

    fun setAmount(id: String, date: LocalDate, amount: Int) {
        viewModelScope.launch { repository.setAmount(id, date, amount) }
    }

    /** Debug builds only: sample habits with months of history (see [addSampleHabits]). */
    fun addSamples(today: LocalDate, firstDay: DayOfWeek) {
        viewModelScope.launch { repository.addSampleHabits(today, firstDay) }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }
}
