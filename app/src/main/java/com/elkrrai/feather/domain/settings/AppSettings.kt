package com.elkrrai.feather.domain.settings

import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** User preferences that affect reminders. Language and theme are handled elsewhere. */
data class AppSettings(
    /** The due moment of a task that has a date but no time, for "before due" reminders. */
    val defaultReminderTime: LocalTime = LocalTime.of(9, 0),
    /** Length of the one-tap snooze on a notification. */
    val snoozeMinutes: Int = 10,
    val dailySummaryEnabled: Boolean = false,
    val dailySummaryTime: LocalTime = LocalTime.of(8, 0),
    /** Write a backup file into [backupFolder] once a day. */
    val autoBackupEnabled: Boolean = false,
    /** The folder (a document tree URI) chosen for automatic backups, or null. */
    val backupFolder: String? = null,
    /** When the last automatic backup finished, epoch millis, or null if there was none. */
    val lastBackupAt: Long? = null,
)

/** Snooze lengths offered in settings, in minutes. */
val SNOOZE_MINUTE_OPTIONS: List<Int> = listOf(5, 10, 15, 30, 60)

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun current(): AppSettings = settings.first()

    suspend fun setDefaultReminderTime(time: LocalTime)

    suspend fun setSnoozeMinutes(minutes: Int)

    suspend fun setDailySummaryEnabled(enabled: Boolean)

    suspend fun setDailySummaryTime(time: LocalTime)

    suspend fun setAutoBackupEnabled(enabled: Boolean)

    suspend fun setBackupFolder(uri: String?)

    suspend fun setLastBackupAt(epochMillis: Long?)
}
