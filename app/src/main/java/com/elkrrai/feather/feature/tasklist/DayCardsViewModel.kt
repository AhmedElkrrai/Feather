package com.elkrrai.feather.feature.tasklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elkrrai.feather.domain.model.Task
import com.elkrrai.feather.domain.repository.TaskRepository
import com.elkrrai.feather.domain.settings.SettingsRepository
import com.elkrrai.feather.domain.view.TaskScope
import com.elkrrai.feather.domain.view.TaskSort
import com.elkrrai.feather.domain.view.comparator
import com.elkrrai.feather.domain.view.inScope
import com.elkrrai.feather.domain.view.openCountIn
import com.elkrrai.feather.ui.today.DayCard
import com.elkrrai.feather.ui.today.DayCardKind
import com.elkrrai.feather.ui.today.DayCardSwitches
import com.elkrrai.feather.ui.today.TodayStatus
import com.elkrrai.feather.ui.today.dayCardFor
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map

/** Behind the morning and evening cards on Today: which one to show, and what its buttons do. */
@HiltViewModel
class DayCardsViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
) : ViewModel() {

    private fun zone(): ZoneId = ZoneId.systemDefault()

    private fun today(): LocalDate = LocalDate.now(clock.withZone(zone()))

    /** The time of day, once a minute, so the cards come and go on their own. */
    private val minutes = flow {
        while (true) {
            emit(LocalTime.now(clock.withZone(zone())))
            delay(60_000L)
        }
    }

    val card: StateFlow<DayCard?> = combine(
        taskRepository.observeTasks(),
        settingsRepository.settings,
        minutes,
    ) { tasks, settings, time ->
        val today = today()
        val open = tasks.inScope(TaskScope.Today, today).sortedWith(TaskSort.DUE_DATE.comparator())
        val done = tasks.count { it.completedAt?.atZone(zone())?.toLocalDate() == today || it.isRestingUntilNext(today, zone()) }
        dayCardFor(
            time = time,
            status = TodayStatus(left = open.size, done = done),
            waiting = tasks.openCountIn(TaskScope.Overdue, today),
            firstTitle = open.firstOrNull()?.title,
            switches = DayCardSwitches(
                morning = settings.morningCard,
                evening = settings.eveningCard,
                morningDismissed = settings.morningCardDismissedOn == today,
                eveningDismissed = settings.eveningCardDismissedOn == today,
            ),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Whether the nest has something new, for the quiet dot on Settings. */
    val nestNews: StateFlow<Boolean> = settingsRepository.settings
        .map { (it.nestUnlocked - it.nestSeen).isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** A task was dictated: that is a milestone for the nest (a flower pot). */
    fun voiceUsed() {
        viewModelScope.launch { settingsRepository.setVoiceUsed(true) }
    }

    /** Puts the card away until tomorrow. */
    fun dismiss(kind: DayCardKind) {
        val today = today()
        viewModelScope.launch {
            when (kind) {
                DayCardKind.Morning -> settingsRepository.setMorningCardDismissedOn(today)
                DayCardKind.Evening -> settingsRepository.setEveningCardDismissedOn(today)
            }
        }
    }

    /** Moves everything still open for today to tomorrow, then calls [onMoved] with what moved. */
    fun moveToTomorrow(onMoved: (List<Task>) -> Unit) {
        val today = today()
        viewModelScope.launch {
            val moved = taskRepository.getActiveTasks().inScope(TaskScope.Today, today)
            moved.forEach { taskRepository.saveTaskDetails(it.copy(dueDate = today.plusDays(1))) }
            settingsRepository.setEveningCardDismissedOn(today)
            onMoved(moved)
        }
    }

    /** Puts the moved tasks back on their own days and brings the card back. */
    fun undoMove(tasks: List<Task>) {
        viewModelScope.launch {
            tasks.forEach { taskRepository.saveTaskDetails(it) }
            settingsRepository.setEveningCardDismissedOn(null)
        }
    }
}
