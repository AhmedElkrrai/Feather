package com.elkrrai.feather.domain.settings

import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/** The app's own light or dark choice, applied through AppCompat's night mode. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class TimeFormat { SYSTEM, H12, H24 }

enum class WeekStart { SYSTEM, MONDAY, SATURDAY, SUNDAY }

/** User preferences. The language is handled separately, by AppCompat's per-app locales. */
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
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** True black backgrounds in dark mode, for OLED screens. */
    val pureBlack: Boolean = false,
    /** Material You colors from the wallpaper (Android 12 and up); off by default, the app has its own colors. */
    val dynamicColor: Boolean = false,
    val timeFormat: TimeFormat = TimeFormat.SYSTEM,
    val weekStart: WeekStart = WeekStart.SYSTEM,
    val haptics: Boolean = true,
    /** Lets the bird move; off keeps it still. */
    val birdAnimations: Boolean = true,
    /** Fewer animations: things appear and leave without moving. The phone's own setting counts too. */
    val reduceMotion: Boolean = false,
    /** The feathers and the bird's hop when a task is finished. Independent of [reduceMotion]. */
    val celebrations: Boolean = true,
    /** Days that do not count against the rhythm (a break, a trip); never shown as gaps. */
    val rhythmPausedDays: Set<LocalDate> = emptySet(),
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

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setPureBlack(enabled: Boolean)

    suspend fun setDynamicColor(enabled: Boolean)

    suspend fun setTimeFormat(format: TimeFormat)

    suspend fun setWeekStart(start: WeekStart)

    suspend fun setHaptics(enabled: Boolean)

    suspend fun setBirdAnimations(enabled: Boolean)

    suspend fun setReduceMotion(enabled: Boolean)

    suspend fun setCelebrations(enabled: Boolean)

    suspend fun setRhythmPausedDays(days: Set<LocalDate>)
}
