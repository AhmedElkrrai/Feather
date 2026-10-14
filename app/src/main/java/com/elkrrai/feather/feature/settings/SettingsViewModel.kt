package com.elkrrai.feather.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elkrrai.feather.domain.settings.AppSettings
import com.elkrrai.feather.domain.settings.SettingsRepository
import com.elkrrai.feather.domain.settings.ThemeMode
import com.elkrrai.feather.domain.settings.TimeFormat
import com.elkrrai.feather.domain.settings.WeekStart
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setDefaultReminderTime(time: LocalTime) {
        viewModelScope.launch { repository.setDefaultReminderTime(time) }
    }

    fun setSnoozeMinutes(minutes: Int) {
        viewModelScope.launch { repository.setSnoozeMinutes(minutes) }
    }

    fun setDailySummaryEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setDailySummaryEnabled(enabled) }
    }

    fun setDailySummaryTime(time: LocalTime) {
        viewModelScope.launch { repository.setDailySummaryTime(time) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setPureBlack(enabled: Boolean) {
        viewModelScope.launch { repository.setPureBlack(enabled) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { repository.setDynamicColor(enabled) }
    }

    fun setTimeFormat(format: TimeFormat) {
        viewModelScope.launch { repository.setTimeFormat(format) }
    }

    fun setWeekStart(start: WeekStart) {
        viewModelScope.launch { repository.setWeekStart(start) }
    }

    fun setHaptics(enabled: Boolean) {
        viewModelScope.launch { repository.setHaptics(enabled) }
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setAutoBackupEnabled(enabled) }
    }

    fun setBackupFolder(uri: String?) {
        viewModelScope.launch { repository.setBackupFolder(uri) }
    }
}
