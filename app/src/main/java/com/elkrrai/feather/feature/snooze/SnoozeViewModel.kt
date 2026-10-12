package com.elkrrai.feather.feature.snooze

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elkrrai.feather.domain.repository.ReminderRepository
import com.elkrrai.feather.domain.settings.AppSettings
import com.elkrrai.feather.domain.settings.SettingsRepository
import com.elkrrai.feather.reminders.ReminderNotifier
import com.elkrrai.feather.reminders.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Snoozes the task whose id arrives as an intent extra (it becomes a SavedStateHandle entry). */
@HiltViewModel
class SnoozeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reminders: ReminderRepository,
    private val notifier: ReminderNotifier,
    private val scheduler: ReminderScheduler,
    settings: SettingsRepository,
) : ViewModel() {

    private val taskId: String = savedStateHandle.get<String>(SnoozeActivity.EXTRA_TASK_ID).orEmpty()

    private val defaultTime: StateFlow<LocalTime> = settings.settings
        .map { it.defaultReminderTime }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings().defaultReminderTime)

    private val _finished = MutableStateFlow(false)

    /** True once the snooze is saved; the activity then closes. */
    val finished: StateFlow<Boolean> = _finished.asStateFlow()

    fun snoozeForMinutes(minutes: Long) {
        snoozeUntil(LocalDateTime.now().plusMinutes(minutes))
    }

    /** Tomorrow at the default reminder time. */
    fun snoozeUntilTomorrowMorning() {
        snoozeUntil(LocalDate.now().plusDays(1).atTime(defaultTime.value))
    }

    private fun snoozeUntil(moment: LocalDateTime) {
        viewModelScope.launch {
            if (taskId.isNotEmpty()) {
                reminders.snooze(taskId, moment)
                notifier.cancel(taskId)
                scheduler.reschedule()
            }
            _finished.value = true
        }
    }
}
