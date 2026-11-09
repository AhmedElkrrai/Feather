package com.elkrrai.feather.feature.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elkrrai.feather.domain.habit.StageState
import com.elkrrai.feather.domain.habit.TodayHabit
import com.elkrrai.feather.domain.habit.todayHabits
import com.elkrrai.feather.domain.habit.weekStartOf
import com.elkrrai.feather.domain.repository.HabitRepository
import com.elkrrai.feather.domain.settings.SettingsRepository
import com.elkrrai.feather.domain.time.FIRST_DAY_OF_WEEK
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The habits of Today's list, and whether the line that tells how to finish one is to be shown. */
data class TodayHabitsUi(
    val rows: List<TodayHabit> = emptyList(),
    val showHoldHint: Boolean = false,
)

/** How many times the "hold the card" line is shown before it is left out. */
private const val HOLD_HINT_TIMES = 3

/** Behind the habit rows on Today: which habits show now, and what holding, ticking and counting do. */
@HiltViewModel
class TodayHabitsViewModel @Inject constructor(
    private val habits: HabitRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
) : ViewModel() {

    private fun zone(): ZoneId = ZoneId.systemDefault()

    private fun today(): LocalDate = LocalDate.now(clock.withZone(zone()))

    /** The time of day, once a minute, so a stage turns from next to now by itself. */
    private val minutes = flow {
        while (true) {
            emit(LocalDateTime.now(clock.withZone(zone())))
            delay(60_000L)
        }
    }

    /** Whether the hint line was decided for this visit: it is settled when the first rows come, so it does not vanish in the middle. */
    private var hintForThisVisit: Boolean? = null

    val ui: StateFlow<TodayHabitsUi> = combine(habits.observeHabits(), settings.settings, minutes) { all, saved, at ->
        if (!saved.habitsInToday) return@combine TodayHabitsUi()
        val day = at.toLocalDate()
        val rows = todayHabits(all, day, at.toLocalTime(), weekStartOf(day, FIRST_DAY_OF_WEEK))
        if (hintForThisVisit == null && rows.isNotEmpty()) {
            hintForThisVisit = !saved.habitHoldLearned && saved.habitHoldHintShown < HOLD_HINT_TIMES
            if (hintForThisVisit == true) viewModelScope.launch { settings.setHabitHoldHintShown(saved.habitHoldHintShown + 1) }
        }
        TodayHabitsUi(rows, showHoldHint = hintForThisVisit == true && !saved.habitHoldLearned && rows.any { !it.doneToday })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayHabitsUi())

    /**
     * Finishes the day of [row]: every stage done, or the day's count reached. [onDone] gets what puts it back as it was, for an Undo.
     * Finishing by holding means the gesture is known, so the hint line is not needed any more.
     */
    fun finishDay(row: TodayHabit, onDone: (undo: () -> Unit) -> Unit) {
        viewModelScope.launch {
            val day = today()
            if (row.stages.isNotEmpty()) {
                val before = row.stages.filter { it.state == StageState.DONE }.map { it.stage.id }.toSet()
                habits.setStagesDone(row.id, day, row.stages.map { it.stage.id }.toSet())
                onDone { viewModelScope.launch { habits.setStagesDone(row.id, day, before) } }
            } else {
                val before = row.todayAmount
                habits.setAmount(row.id, day, row.totalCount)
                onDone { viewModelScope.launch { habits.setAmount(row.id, day, before) } }
            }
            settings.setHabitHoldLearned(true)
        }
    }

    /** Ticks or unticks one stage of today, in any order. */
    fun toggleStage(row: TodayHabit, stageId: String) {
        val done = row.stages.firstOrNull { it.stage.id == stageId }?.state == StageState.DONE
        viewModelScope.launch { habits.setStageDone(row.id, today(), stageId, !done) }
    }

    fun addLog(row: TodayHabit, amount: Int) {
        viewModelScope.launch { habits.addLog(row.id, today(), amount) }
    }

    fun changeLog(row: TodayHabit, logId: String, amount: Int) {
        viewModelScope.launch { habits.changeLog(row.id, logId, amount) }
    }

    fun deleteLog(row: TodayHabit, logId: String) {
        viewModelScope.launch { habits.deleteLog(row.id, logId) }
    }
}
