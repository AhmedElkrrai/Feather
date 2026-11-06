package com.elkrrai.feather.feature.habits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.elkrrai.feather.domain.habit.DAYS_IN_WEEK
import com.elkrrai.feather.domain.habit.DayTime
import com.elkrrai.feather.domain.habit.HabitDraft
import com.elkrrai.feather.domain.habit.HabitKind
import com.elkrrai.feather.domain.habit.HabitState
import com.elkrrai.feather.domain.habit.MAX_DAILY_TARGET
import com.elkrrai.feather.domain.habit.MAX_HABIT_TARGET
import com.elkrrai.feather.domain.habit.MAX_STAGES
import com.elkrrai.feather.domain.habit.StageDraft
import com.elkrrai.feather.domain.habit.StageMode
import com.elkrrai.feather.domain.habit.toDraft
import com.elkrrai.feather.domain.repository.HabitRepository
import com.elkrrai.feather.domain.time.FIRST_DAY_OF_WEEK
import com.elkrrai.feather.ui.navigation.HabitEditRoute
import com.elkrrai.feather.domain.habit.weekStartOf
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A stage as it is in the form. [key] tells the rows apart while editing; [id] is the stage's own id, null for one that is not saved yet. */
data class StageUi(val key: Long, val id: String?, val name: String, val time: DayTime = DayTime())

/** What the habit form shows. The numbers are kept as typed. */
data class HabitEditUiState(
    val isLoading: Boolean = true,
    val isNew: Boolean = true,
    val name: String = "",
    val kind: HabitKind = HabitKind.DAYS,
    val unit: String = "",
    val target: String = "3",
    val daily: String = "1",
    /** "Name each time": the day is a list of named times (only for a habit that counts days). */
    val named: Boolean = false,
    val stages: List<StageUi> = emptyList(),
    /** How many stages a day needs, as typed; null means all of them. */
    val needed: String? = null,
    val showInToday: Boolean = true,
    val todayFromStart: Boolean = false,
    val stageMode: StageMode = StageMode.ORDERED,
    /** The habit's own time, for a habit without stages. */
    val time: DayTime = DayTime(),
    val remind: Boolean = false,
    /** Set once the habit was saved (or turned out not to exist); the screen then goes back. */
    val finished: Boolean = false,
) {
    val targetValue: Int? get() = target.toIntOrNull()
    val dailyValue: Int get() = daily.toIntOrNull() ?: 1
    val maxTarget: Int get() = if (kind == HabitKind.DAYS) DAYS_IN_WEEK else MAX_HABIT_TARGET
    val targetValid: Boolean get() = targetValue?.let { it in 1..maxTarget } == true

    /** The habit has named times. A stage without a name is not one. */
    val staged: Boolean get() = kind == HabitKind.DAYS && named
    val namedStages: List<StageUi> get() = stages.filter { it.name.isNotBlank() }
    /** The habit has a time of day to go by: a stage with a time, or the habit's own. */
    val hasTime: Boolean get() = if (staged) namedStages.any { it.time.isSet } else time.isSet
    /** A reminder comes at the start of a time, so there has to be one. */
    val canRemind: Boolean get() = kind == HabitKind.DAYS && hasTime
    val neededValid: Boolean get() = needed?.toIntOrNull()?.let { it in 1..namedStages.size } ?: (needed == null)

    /** How many times a day counts: for a habit with stages, how many of them (all by default); else the typed number. */
    val dailyTarget: Int get() = if (staged) needed?.toIntOrNull() ?: namedStages.size else dailyValue

    val canSave: Boolean
        get() = !isLoading && name.isNotBlank() && targetValid &&
            if (staged) namedStages.isNotEmpty() && neededValid else dailyValue in 1..MAX_DAILY_TARGET
}

/**
 * Behind the screen that makes a habit or edits one. When editing, the name, the unit, the times and the Today settings change
 * at once and a changed goal (the target, the times a day, the stages) is queued for the start of next week.
 */
@HiltViewModel
class HabitEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: HabitRepository,
) : ViewModel() {

    private val habitId: String? = savedStateHandle.toRoute<HabitEditRoute>().habitId

    private val _uiState = MutableStateFlow(HabitEditUiState(isLoading = habitId != null, isNew = habitId == null))
    val uiState: StateFlow<HabitEditUiState> = _uiState.asStateFlow()

    private var nextKey = 1L

    /** The form as it was when it opened, to tell whether anything was changed. */
    private var initial: HabitEditUiState? = if (habitId == null) HabitEditUiState(isLoading = false, isNew = true) else null

    /** True once the form differs from how it opened, so leaving it asks first. */
    val hasChanges: StateFlow<Boolean> = _uiState
        .map { state -> initial?.let { state.copy(finished = false) != it } ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** The habit as it was when the form opened. */
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
                val draft = habit.toDraft(thisWeek())
                val opened = HabitEditUiState(
                    isLoading = false,
                    isNew = false,
                    name = draft.name,
                    kind = draft.kind,
                    unit = draft.unit,
                    target = draft.target.toString(),
                    daily = draft.dailyTarget.toString(),
                    named = draft.stages.isNotEmpty(),
                    stages = draft.stages.map { StageUi(nextKey++, it.id, it.name, it.time) },
                    needed = draft.dailyTarget.takeIf { draft.stages.isNotEmpty() && it != draft.stages.size }?.toString(),
                    showInToday = draft.showInToday,
                    todayFromStart = draft.todayFromStart,
                    stageMode = draft.stageMode.takeIf { it != StageMode.NONE } ?: StageMode.ORDERED,
                    time = draft.time,
                    remind = draft.remind,
                )
                initial = opened
                _uiState.value = opened
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

    /** Turning the names on starts a list of as many blank times as the habit counts a day (at least two). */
    fun onNamedChange(on: Boolean) = _uiState.update { state ->
        if (on && state.stages.isEmpty()) {
            val count = state.dailyValue.coerceIn(2, MAX_STAGES)
            state.copy(named = true, stages = List(count) { StageUi(nextKey++, null, "") })
        } else {
            state.copy(named = on)
        }
    }

    fun onStageNameChange(key: Long, name: String) = changeStage(key) { it.copy(name = name.take(30)) }

    fun onStageTimeChange(key: Long, time: DayTime) = changeStage(key) { it.copy(time = time) }

    /** Adds a blank time and returns its key, so the screen can open it for typing; null when there are already as many as there can be. */
    fun addStage(): Long? {
        if (_uiState.value.stages.size >= MAX_STAGES) return null
        val key = nextKey++
        _uiState.update { it.copy(stages = it.stages + StageUi(key, null, "")) }
        return key
    }

    /** Moves a stage one place up (-1) or down (1). */
    fun moveStage(key: Long, by: Int) = _uiState.update { state ->
        val from = state.stages.indexOfFirst { it.key == key }
        val to = from + by
        if (from < 0 || to !in state.stages.indices) {
            state
        } else {
            state.copy(stages = state.stages.toMutableList().also { list -> list.add(to, list.removeAt(from)) })
        }
    }

    fun removeStage(key: Long) = _uiState.update { it.copy(stages = it.stages.filterNot { stage -> stage.key == key }) }

    fun onNeededChange(text: String) = _uiState.update {
        it.copy(needed = text.filter(Char::isDigit).trimStart('0').take(2).ifEmpty { null })
    }

    fun onShowInTodayChange(on: Boolean) = _uiState.update { it.copy(showInToday = on) }

    fun onTodayFromStartChange(fromStart: Boolean) = _uiState.update { it.copy(todayFromStart = fromStart) }

    fun onStageModeChange(mode: StageMode) = _uiState.update { it.copy(stageMode = mode) }

    fun onTimeChange(time: DayTime) = _uiState.update { it.copy(time = time) }

    fun onRemindChange(on: Boolean) = _uiState.update { it.copy(remind = on) }

    fun save() {
        val state = _uiState.value
        val target = state.targetValue
        if (!state.canSave || target == null) return
        viewModelScope.launch {
            val draft = HabitDraft(
                name = state.name,
                kind = state.kind,
                unit = state.unit,
                target = target,
                dailyTarget = state.dailyTarget,
                showInToday = state.showInToday,
                time = if (state.staged) DayTime() else state.time,
                stageMode = if (state.staged) state.stageMode else StageMode.NONE,
                todayFromStart = state.todayFromStart,
                remind = state.remind,
                stages = if (state.staged) state.namedStages.map { StageDraft(it.id, it.name, it.time) } else emptyList(),
            )
            repository.saveHabit(loaded?.habit?.id, draft, thisWeek())
            _uiState.update { it.copy(finished = true) }
        }
    }

    private fun changeStage(key: Long, change: (StageUi) -> StageUi) = _uiState.update { state ->
        state.copy(stages = state.stages.map { if (it.key == key) change(it) else it })
    }

    private fun thisWeek(): LocalDate = weekStartOf(LocalDate.now(), FIRST_DAY_OF_WEEK)
}
