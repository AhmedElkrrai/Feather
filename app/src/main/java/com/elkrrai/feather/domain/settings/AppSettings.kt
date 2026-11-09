package com.elkrrai.feather.domain.settings

import com.elkrrai.feather.domain.nest.NestOutfit
import com.elkrrai.feather.domain.nest.NestStyle
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import com.elkrrai.feather.domain.backup.BackupPreferences
import java.time.LocalDate

/** The app's own light or dark choice, applied through AppCompat's night mode. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class TimeFormat { SYSTEM, H12, H24 }

/** The language the microphone in quick add listens for: English (US) or Arabic (Egypt), [tag] being what the speech recognizer is asked for. */
enum class VoiceLanguage(val tag: String) { EN_US("en-US"), AR_EG("ar-EG") }

/** Which region of the voice language the recognizer is asked for: the language's usual one, or the region of the phone. */
enum class VoiceRegion { DEFAULT, DEVICE }

/**
 * The tag the speech recognizer is asked for. [deviceCountry] is the phone's own region ("TN"), empty when it has none; with
 * [VoiceRegion.DEVICE] it replaces the region of [language] ("en-TN"), and without one the language's usual region is used.
 */
fun speechTag(language: VoiceLanguage, region: VoiceRegion, deviceCountry: String): String =
    if (region == VoiceRegion.DEVICE && deviceCountry.isNotEmpty()) "${language.tag.substringBefore('-')}-$deviceCountry" else language.tag

/** How the automatic backup names its files: one file replaced each time, a file for each of the last 7 days, or one file plus an older copy. */
enum class BackupStyle { ONE_FILE, SEVEN_DAYS, ONE_PLUS_OLDER }

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
    /** How many files the automatic backup keeps in [backupFolder]. One file by default. */
    val backupStyle: BackupStyle = BackupStyle.ONE_FILE,
    /** When the last automatic backup finished, epoch millis, or null if there was none. */
    val lastBackupAt: Long? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** True black backgrounds in dark mode, for OLED screens. */
    val pureBlack: Boolean = true,
    /** Material You colors from the wallpaper (Android 12 and up); off by default, the app has its own colors. */
    val dynamicColor: Boolean = false,
    val timeFormat: TimeFormat = TimeFormat.SYSTEM,
    val voiceLanguage: VoiceLanguage = VoiceLanguage.EN_US,
    val voiceRegion: VoiceRegion = VoiceRegion.DEFAULT,
    val haptics: Boolean = true,
    /** The grey line under each item of the menu that says what is in it. */
    val menuHints: Boolean = true,
    /** The first-run welcome card was shown (and closed), so it does not come again. */
    val welcomeSeen: Boolean = false,
    /** How many times the "swipe to see earlier weeks" hint on a habit's weekly page was shown, and the week of the last time. */
    val habitSwipeHintsShown: Int = 0,
    val habitSwipeHintWeek: LocalDate? = null,
    /** For each habit (by id): how many met weeks its tree had when the tree was last opened, so the card can say it grew. */
    val habitTreesSeen: Map<String, Int> = emptyMap(),
    /** Asks for a fingerprint, a face or the screen lock when the app is opened. Off until the person turns it on, and only takes effect on a phone that has one. */
    val appLock: Boolean = false,
    /** Lets the bird move; off keeps it still. */
    val birdAnimations: Boolean = true,
    /** Fewer animations: things appear and leave without moving. The phone's own setting counts too. */
    val reduceMotion: Boolean = false,
    /** The feathers and the bird's hop when a task is finished. Independent of [reduceMotion]. */
    val celebrations: Boolean = true,
    /** The "your day" card on Today in the morning, and the wind-down card in the evening. */
    val morningCard: Boolean = true,
    val eveningCard: Boolean = true,
    /** Habits that are switched on for Today show as rows there. */
    val habitsInToday: Boolean = true,
    /** How many times the "hold the card to finish" line was shown, and whether a habit was finished by holding (then the line is not needed). */
    val habitHoldHintShown: Int = 0,
    val habitHoldLearned: Boolean = false,
    /** Keys of the habit reminders already shown (habit|stage|day), so each is shown once; only the last few days are kept. Stays on this phone. */
    val habitRemindersFired: Set<String> = emptySet(),
    /** The day each card was last put away, so it shows once a day. */
    val morningCardDismissedOn: LocalDate? = null,
    val eveningCardDismissedOn: LocalDate? = null,
    /** The feather for last week, shown on Your rhythm on the first day of the week. */
    val weeklyFeather: Boolean = true,
    /** Ids of the nest items earned so far (permanent), and the ones whose news was looked at. */
    val nestUnlocked: Set<String> = emptySet(),
    val nestSeen: Set<String> = emptySet(),
    /** The nest the bird sits in. */
    val nestStyle: NestStyle = NestStyle.BRAIDED,
    /** What the bird wears, of what has been earned. */
    val nestOutfit: NestOutfit = NestOutfit(),
    /** A task was added by voice at least once; a backup was made at least once. Both are milestones for the nest. */
    val voiceUsed: Boolean = false,
    val backedUp: Boolean = false,
    /** Days that do not count against the rhythm (a break, a trip); never shown as gaps. */
    val rhythmPausedDays: Set<LocalDate> = emptySet(),
)

/** Snooze lengths offered in settings, in minutes. */
val SNOOZE_MINUTE_OPTIONS: List<Int> = listOf(5, 10, 15, 30, 60)

interface SettingsRepository : BackupPreferences {
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

    suspend fun setVoiceLanguage(language: VoiceLanguage)

    suspend fun setVoiceRegion(region: VoiceRegion)

    suspend fun setBackupStyle(style: BackupStyle)

    suspend fun setHaptics(enabled: Boolean)

    suspend fun setMenuHints(enabled: Boolean)

    suspend fun setWelcomeSeen(seen: Boolean)

    suspend fun setHabitSwipeHint(shown: Int, week: LocalDate)

    suspend fun setHabitTreeSeen(habitId: String, metWeeks: Int)

    suspend fun setAppLock(enabled: Boolean)

    suspend fun setBirdAnimations(enabled: Boolean)

    suspend fun setReduceMotion(enabled: Boolean)

    suspend fun setCelebrations(enabled: Boolean)

    suspend fun setRhythmPausedDays(days: Set<LocalDate>)

    suspend fun setWeeklyFeather(enabled: Boolean)

    suspend fun setNestUnlocked(ids: Set<String>)

    suspend fun setNestSeen(ids: Set<String>)

    suspend fun setNestStyle(style: NestStyle)

    suspend fun setNestOutfit(outfit: NestOutfit)

    suspend fun setVoiceUsed(used: Boolean)

    suspend fun setBackedUp(done: Boolean)

    suspend fun setMorningCard(enabled: Boolean)

    suspend fun setEveningCard(enabled: Boolean)

    suspend fun setHabitsInToday(enabled: Boolean)

    suspend fun setHabitHoldHintShown(count: Int)

    suspend fun setHabitHoldLearned(learned: Boolean)

    /** Remembers that the habit reminder with [key] was shown, and forgets the ones of days before [today] minus a few. */
    suspend fun markHabitReminderFired(key: String, today: LocalDate)

    suspend fun setMorningCardDismissedOn(date: LocalDate?)

    suspend fun setEveningCardDismissedOn(date: LocalDate?)
}
