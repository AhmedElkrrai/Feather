package com.elkrrai.feather.feature.focus

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.elkrrai.feather.domain.focus.BREAK_MINUTES
import com.elkrrai.feather.domain.focus.DEFAULT_FOCUS_MINUTES
import com.elkrrai.feather.domain.focus.FOCUS_STEP_MINUTES
import com.elkrrai.feather.domain.focus.FocusPhase
import com.elkrrai.feather.domain.focus.FocusSession
import com.elkrrai.feather.domain.focus.clampFocusMinutes
import com.elkrrai.feather.domain.repository.TaskRepository
import com.elkrrai.feather.focus.FocusController
import com.elkrrai.feather.ui.navigation.FocusRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FocusUiState(
    val title: String = "",
    /** The length chosen for the next focus stretch. */
    val minutes: Int = DEFAULT_FOCUS_MINUTES,
    /** The session, when it is on this task (running, paused or run out). */
    val session: FocusSession? = null,
    /** The title of the task another session is running on, or null. */
    val otherTitle: String? = null,
    /** The wall clock, ticking while the screen is shown, so the countdown moves. */
    val now: Long = 0L,
    /** The task is gone (deleted): the screen leaves. */
    val missing: Boolean = false,
)

/** Behind the focus screen of one task: reads the one session from the controller and starts, pauses and ends it. */
@HiltViewModel
class FocusViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    tasks: TaskRepository,
    private val controller: FocusController,
) : ViewModel() {

    private val taskId: String = savedStateHandle.toRoute<FocusRoute>().taskId

    private val title = MutableStateFlow("")
    private val missing = MutableStateFlow(false)
    private val minutes = MutableStateFlow(controller.lastMinutes)

    private val ticks = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(TICK_MILLIS)
        }
    }

    val uiState: StateFlow<FocusUiState> = combine(title, minutes, controller.session, ticks, missing) { name, chosen, session, now, gone ->
        val mine = session?.takeIf { it.taskId == taskId }
        FocusUiState(
            title = name,
            minutes = chosen,
            session = mine,
            otherTitle = session?.takeIf { it.taskId != taskId }?.taskTitle,
            now = now,
            missing = gone,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusUiState(now = System.currentTimeMillis()))

    init {
        viewModelScope.launch {
            val task = tasks.getTask(taskId)
            // a deleted or finished task has no focus timer
            if (task == null || task.isCompleted) missing.value = true else title.value = task.title
        }
        // When the screen sees the time run out it says so, as the alarm does if the screen is closed.
        viewModelScope.launch {
            combine(controller.session, ticks) { session, now -> session?.isOver(now) == true }
                .distinctUntilChanged()
                .filter { it }
                .collect { controller.timeIsUp() }
        }
    }

    fun onLonger() = minutes.update { clampFocusMinutes(it + FOCUS_STEP_MINUTES) }

    fun onShorter() = minutes.update { clampFocusMinutes(it - FOCUS_STEP_MINUTES) }

    fun start() = controller.start(taskId, title.value, FocusPhase.FOCUS, minutes.value)

    fun pause() = controller.pause()

    fun resume() = controller.resume()

    fun stop() = controller.stop()

    fun startBreak() = controller.start(taskId, title.value, FocusPhase.BREAK, BREAK_MINUTES)

    private companion object {
        const val TICK_MILLIS = 250L
    }
}
