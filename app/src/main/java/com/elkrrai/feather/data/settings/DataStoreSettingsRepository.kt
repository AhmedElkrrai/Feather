package com.elkrrai.feather.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.elkrrai.feather.domain.settings.AppSettings
import com.elkrrai.feather.domain.settings.SettingsRepository
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Times are stored as minutes since midnight, which is all the precision the UI offers. */
class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    private object Keys {
        val defaultReminderMinute = intPreferencesKey("default_reminder_minute_of_day")
        val snoozeMinutes = intPreferencesKey("snooze_minutes")
        val dailySummaryEnabled = booleanPreferencesKey("daily_summary_enabled")
        val dailySummaryMinute = intPreferencesKey("daily_summary_minute_of_day")
        val autoBackupEnabled = booleanPreferencesKey("auto_backup_enabled")
        val backupFolder = stringPreferencesKey("backup_folder")
        val lastBackupAt = longPreferencesKey("last_backup_at")
    }

    override val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        val defaults = AppSettings()
        AppSettings(
            defaultReminderTime = prefs[Keys.defaultReminderMinute]?.toTime() ?: defaults.defaultReminderTime,
            snoozeMinutes = prefs[Keys.snoozeMinutes] ?: defaults.snoozeMinutes,
            dailySummaryEnabled = prefs[Keys.dailySummaryEnabled] ?: defaults.dailySummaryEnabled,
            dailySummaryTime = prefs[Keys.dailySummaryMinute]?.toTime() ?: defaults.dailySummaryTime,
            autoBackupEnabled = prefs[Keys.autoBackupEnabled] ?: defaults.autoBackupEnabled,
            backupFolder = prefs[Keys.backupFolder],
            lastBackupAt = prefs[Keys.lastBackupAt],
        )
    }

    override suspend fun setDefaultReminderTime(time: LocalTime) {
        dataStore.edit { it[Keys.defaultReminderMinute] = time.toMinuteOfDay() }
    }

    override suspend fun setSnoozeMinutes(minutes: Int) {
        dataStore.edit { it[Keys.snoozeMinutes] = minutes.coerceAtLeast(1) }
    }

    override suspend fun setDailySummaryEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.dailySummaryEnabled] = enabled }
    }

    override suspend fun setDailySummaryTime(time: LocalTime) {
        dataStore.edit { it[Keys.dailySummaryMinute] = time.toMinuteOfDay() }
    }

    override suspend fun setAutoBackupEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.autoBackupEnabled] = enabled }
    }

    override suspend fun setBackupFolder(uri: String?) {
        dataStore.edit { if (uri == null) it.remove(Keys.backupFolder) else it[Keys.backupFolder] = uri }
    }

    override suspend fun setLastBackupAt(epochMillis: Long?) {
        dataStore.edit { if (epochMillis == null) it.remove(Keys.lastBackupAt) else it[Keys.lastBackupAt] = epochMillis }
    }

    private fun LocalTime.toMinuteOfDay(): Int = hour * 60 + minute

    private fun Int.toTime(): LocalTime =
        LocalTime.of((this / 60).coerceIn(0, 23), (this % 60).coerceIn(0, 59))
}
