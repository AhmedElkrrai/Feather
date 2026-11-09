package com.elkrrai.feather.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.elkrrai.feather.domain.nest.NestOutfit
import com.elkrrai.feather.domain.nest.NestStyle
import com.elkrrai.feather.domain.settings.AppSettings
import com.elkrrai.feather.domain.settings.BackupStyle
import com.elkrrai.feather.domain.settings.SettingsRepository
import com.elkrrai.feather.domain.settings.ThemeMode
import com.elkrrai.feather.domain.settings.TimeFormat
import com.elkrrai.feather.domain.settings.VoiceLanguage
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.stringSetPreferencesKey
import java.time.LocalDate

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
        val themeMode = stringPreferencesKey("theme_mode")
        val pureBlack = booleanPreferencesKey("pure_black")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val timeFormat = stringPreferencesKey("time_format")
        val voiceLanguage = stringPreferencesKey("voice_language")
        val voiceRegion = stringPreferencesKey("voice_region")
        val backupStyle = stringPreferencesKey("backup_style")
        val haptics = booleanPreferencesKey("haptics")
        val menuHints = booleanPreferencesKey("menu_hints")
        val welcomeSeen = booleanPreferencesKey("welcome_seen")
        val habitSwipeHintsShown = intPreferencesKey("habit_swipe_hints_shown")
        val habitSwipeHintWeek = stringPreferencesKey("habit_swipe_hint_week")
        val habitTreesSeen = stringSetPreferencesKey("habit_trees_seen")
        val appLock = booleanPreferencesKey("app_lock")
        val birdAnimations = booleanPreferencesKey("bird_animations")
        val reduceMotion = booleanPreferencesKey("reduce_motion")
        val celebrations = booleanPreferencesKey("celebrations")
        val rhythmPausedDays = stringSetPreferencesKey("rhythm_paused_days")
        val weeklyFeather = booleanPreferencesKey("weekly_feather")
        val nestUnlocked = stringSetPreferencesKey("nest_unlocked")
        val nestSeen = stringSetPreferencesKey("nest_seen")
        val nestStyle = stringPreferencesKey("nest_style")
        val nestScarf = intPreferencesKey("nest_scarf")
        val nestGlasses = intPreferencesKey("nest_glasses")
        val nestHat = intPreferencesKey("nest_hat")
        val nestPot = intPreferencesKey("nest_pot")
        val nestMoss = intPreferencesKey("nest_moss")
        val voiceUsed = booleanPreferencesKey("voice_used")
        val backedUp = booleanPreferencesKey("backed_up")
        val morningCard = booleanPreferencesKey("morning_card")
        val eveningCard = booleanPreferencesKey("evening_card")
        val habitsInToday = booleanPreferencesKey("habits_in_today")
        val habitHoldHintShown = intPreferencesKey("habit_hold_hint_shown")
        val habitHoldLearned = booleanPreferencesKey("habit_hold_learned")
        val habitRemindersFired = stringSetPreferencesKey("habit_reminders_fired")
        val morningCardDismissedOn = stringPreferencesKey("morning_card_dismissed_on")
        val eveningCardDismissedOn = stringPreferencesKey("evening_card_dismissed_on")
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
            themeMode = prefs.enumOf(Keys.themeMode, defaults.themeMode),
            pureBlack = prefs[Keys.pureBlack] ?: defaults.pureBlack,
            dynamicColor = prefs[Keys.dynamicColor] ?: defaults.dynamicColor,
            timeFormat = prefs.enumOf(Keys.timeFormat, defaults.timeFormat),
            voiceLanguage = prefs.enumOf(Keys.voiceLanguage, defaults.voiceLanguage),
            voiceRegion = prefs[Keys.voiceRegion] ?: defaults.voiceRegion,
            backupStyle = prefs.enumOf(Keys.backupStyle, defaults.backupStyle),
            haptics = prefs[Keys.haptics] ?: defaults.haptics,
            menuHints = prefs[Keys.menuHints] ?: defaults.menuHints,
            welcomeSeen = prefs[Keys.welcomeSeen] ?: defaults.welcomeSeen,
            habitSwipeHintsShown = prefs[Keys.habitSwipeHintsShown] ?: defaults.habitSwipeHintsShown,
            habitSwipeHintWeek = prefs[Keys.habitSwipeHintWeek]?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            habitTreesSeen = prefs[Keys.habitTreesSeen].orEmpty().mapNotNull { entry ->
                val at = entry.lastIndexOf('|')
                entry.substring(at + 1).toIntOrNull()?.takeIf { at > 0 }?.let { entry.substring(0, at) to it }
            }.toMap(),
            appLock = prefs[Keys.appLock] ?: defaults.appLock,
            birdAnimations = prefs[Keys.birdAnimations] ?: defaults.birdAnimations,
            reduceMotion = prefs[Keys.reduceMotion] ?: defaults.reduceMotion,
            celebrations = prefs[Keys.celebrations] ?: defaults.celebrations,
            weeklyFeather = prefs[Keys.weeklyFeather] ?: defaults.weeklyFeather,
            nestUnlocked = prefs[Keys.nestUnlocked].orEmpty(),
            nestSeen = prefs[Keys.nestSeen].orEmpty(),
            nestStyle = prefs.enumOf(Keys.nestStyle, defaults.nestStyle),
            nestOutfit = NestOutfit(
                scarf = (prefs[Keys.nestScarf] ?: 0).coerceIn(NestOutfit.NONE, NestOutfit.SCARVES - 1),
                glasses = (prefs[Keys.nestGlasses] ?: 0).coerceIn(NestOutfit.NONE, NestOutfit.GLASSES - 1),
                hat = (prefs[Keys.nestHat] ?: 0).coerceIn(NestOutfit.NONE, NestOutfit.HATS - 1),
                pot = (prefs[Keys.nestPot] ?: 0).coerceIn(NestOutfit.NONE, NestOutfit.POTS - 1),
                moss = (prefs[Keys.nestMoss] ?: 0).coerceIn(NestOutfit.NONE, NestOutfit.MOSSES - 1),
            ),
            voiceUsed = prefs[Keys.voiceUsed] ?: defaults.voiceUsed,
            backedUp = prefs[Keys.backedUp] ?: defaults.backedUp,
            morningCard = prefs[Keys.morningCard] ?: defaults.morningCard,
            eveningCard = prefs[Keys.eveningCard] ?: defaults.eveningCard,
            habitsInToday = prefs[Keys.habitsInToday] ?: defaults.habitsInToday,
            habitHoldHintShown = prefs[Keys.habitHoldHintShown] ?: defaults.habitHoldHintShown,
            habitHoldLearned = prefs[Keys.habitHoldLearned] ?: defaults.habitHoldLearned,
            habitRemindersFired = prefs[Keys.habitRemindersFired].orEmpty(),
            morningCardDismissedOn = prefs[Keys.morningCardDismissedOn]?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            eveningCardDismissedOn = prefs[Keys.eveningCardDismissedOn]?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            rhythmPausedDays = prefs[Keys.rhythmPausedDays].orEmpty().mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet(),
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

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.themeMode] = mode.name }
    }

    override suspend fun setPureBlack(enabled: Boolean) {
        dataStore.edit { it[Keys.pureBlack] = enabled }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[Keys.dynamicColor] = enabled }
    }

    override suspend fun setTimeFormat(format: TimeFormat) {
        dataStore.edit { it[Keys.timeFormat] = format.name }
    }

    override suspend fun setVoiceLanguage(language: VoiceLanguage) {
        dataStore.edit { it[Keys.voiceLanguage] = language.name }
    }

    override suspend fun setVoiceRegion(region: String) {
        dataStore.edit { it[Keys.voiceRegion] = region }
    }

    override suspend fun setBackupStyle(style: BackupStyle) {
        dataStore.edit { it[Keys.backupStyle] = style.name }
    }

    override suspend fun setHaptics(enabled: Boolean) {
        dataStore.edit { it[Keys.haptics] = enabled }
    }

    override suspend fun setBirdAnimations(enabled: Boolean) {
        dataStore.edit { it[Keys.birdAnimations] = enabled }
    }

    override suspend fun setReduceMotion(enabled: Boolean) {
        dataStore.edit { it[Keys.reduceMotion] = enabled }
    }

    override suspend fun setAppLock(enabled: Boolean) {
        dataStore.edit { it[Keys.appLock] = enabled }
    }

    override suspend fun setMenuHints(enabled: Boolean) {
        dataStore.edit { it[Keys.menuHints] = enabled }
    }

    override suspend fun setWelcomeSeen(seen: Boolean) {
        dataStore.edit { it[Keys.welcomeSeen] = seen }
    }

    override suspend fun setHabitSwipeHint(shown: Int, week: LocalDate) {
        dataStore.edit {
            it[Keys.habitSwipeHintsShown] = shown
            it[Keys.habitSwipeHintWeek] = week.toString()
        }
    }

    override suspend fun setHabitTreeSeen(habitId: String, metWeeks: Int) {
        dataStore.edit { prefs ->
            val others = prefs[Keys.habitTreesSeen].orEmpty().filterNot { it.startsWith("$habitId|") }
            prefs[Keys.habitTreesSeen] = others.toSet() + "$habitId|$metWeeks"
        }
    }

    override suspend fun setCelebrations(enabled: Boolean) {
        dataStore.edit { it[Keys.celebrations] = enabled }
    }

    override suspend fun setWeeklyFeather(enabled: Boolean) {
        dataStore.edit { it[Keys.weeklyFeather] = enabled }
    }

    override suspend fun setNestUnlocked(ids: Set<String>) {
        dataStore.edit { it[Keys.nestUnlocked] = ids }
    }

    override suspend fun setNestSeen(ids: Set<String>) {
        dataStore.edit { it[Keys.nestSeen] = ids }
    }

    override suspend fun setNestStyle(style: NestStyle) {
        dataStore.edit { it[Keys.nestStyle] = style.name }
    }

    override suspend fun setNestOutfit(outfit: NestOutfit) {
        dataStore.edit {
            it[Keys.nestScarf] = outfit.scarf
            it[Keys.nestGlasses] = outfit.glasses
            it[Keys.nestHat] = outfit.hat
            it[Keys.nestPot] = outfit.pot
            it[Keys.nestMoss] = outfit.moss
        }
    }

    override suspend fun setVoiceUsed(used: Boolean) {
        dataStore.edit { it[Keys.voiceUsed] = used }
    }

    override suspend fun setBackedUp(done: Boolean) {
        dataStore.edit { it[Keys.backedUp] = done }
    }

    override suspend fun setMorningCard(enabled: Boolean) {
        dataStore.edit { it[Keys.morningCard] = enabled }
    }

    override suspend fun setEveningCard(enabled: Boolean) {
        dataStore.edit { it[Keys.eveningCard] = enabled }
    }

    override suspend fun setHabitsInToday(enabled: Boolean) {
        dataStore.edit { it[Keys.habitsInToday] = enabled }
    }

    override suspend fun setHabitHoldHintShown(count: Int) {
        dataStore.edit { it[Keys.habitHoldHintShown] = count }
    }

    override suspend fun setHabitHoldLearned(learned: Boolean) {
        dataStore.edit { it[Keys.habitHoldLearned] = learned }
    }

    override suspend fun markHabitReminderFired(key: String, today: LocalDate) {
        dataStore.edit { prefs ->
            val oldest = today.minusDays(3).toString()
            // the day is the last part of a key; ISO dates sort as text
            val kept = prefs[Keys.habitRemindersFired].orEmpty().filter { it.substringAfterLast('|') >= oldest }
            prefs[Keys.habitRemindersFired] = (kept + key).toSet()
        }
    }

    override suspend fun setMorningCardDismissedOn(date: LocalDate?) {
        dataStore.edit { if (date == null) it.remove(Keys.morningCardDismissedOn) else it[Keys.morningCardDismissedOn] = date.toString() }
    }

    override suspend fun setEveningCardDismissedOn(date: LocalDate?) {
        dataStore.edit { if (date == null) it.remove(Keys.eveningCardDismissedOn) else it[Keys.eveningCardDismissedOn] = date.toString() }
    }

    override suspend fun setRhythmPausedDays(days: Set<LocalDate>) {
        dataStore.edit { it[Keys.rhythmPausedDays] = days.map { day -> day.toString() }.toSet() }
    }

    override suspend fun backedUpPreferences(): Map<String, String> {
        val prefs = dataStore.data.first()
        val out = LinkedHashMap<String, String>()
        fun <T : Any> add(keys: List<Preferences.Key<T>>) = keys.forEach { key ->
            prefs[key]?.let { value -> PrefCodec.encode(value)?.let { out[key.name] = it } }
        }
        add(booleanKeys)
        add(intKeys)
        add(stringKeys)
        add(setKeys)
        return out
    }

    override suspend fun restoreBackedUpPreferences(values: Map<String, String>) {
        dataStore.edit { prefs ->
            // A value is taken only for a key this app knows and only when its type is the key's own: a file from another
            // version must never put the wrong kind of value under a name, which would break every later read.
            fun <T : Any> restore(keys: List<Preferences.Key<T>>, cast: (Any) -> T?) = keys.forEach { key ->
                val incoming = values[key.name]?.let { PrefCodec.decode(it) }?.let(cast) ?: return@forEach
                val merged = cast(mergedPreference(key.name, prefs[key], incoming)) ?: return@forEach
                prefs[key] = merged
            }
            restore(booleanKeys) { it as? Boolean }
            restore(intKeys) { it as? Int }
            restore(stringKeys) { it as? String }
            restore(setKeys) { (it as? Set<*>)?.filterIsInstance<String>()?.toSet() }
        }
    }

    /** A stored enum by name; a missing or unknown name (say, from a newer version) gives [default]. */
    private inline fun <reified E : Enum<E>> Preferences.enumOf(key: Preferences.Key<String>, default: E): E =
        this[key]?.let { name -> enumValues<E>().firstOrNull { it.name == name } } ?: default

    private fun LocalTime.toMinuteOfDay(): Int = hour * 60 + minute

    private fun Int.toTime(): LocalTime =
        LocalTime.of((this / 60).coerceIn(0, 23), (this % 60).coerceIn(0, 59))

    companion object {
        // Every preference is in one of these lists, by the type of its value, or in [deviceOnly]. A test fails when a new key is in neither.
        private val booleanKeys = listOf(
            Keys.dailySummaryEnabled, Keys.pureBlack, Keys.dynamicColor, Keys.haptics, Keys.menuHints, Keys.welcomeSeen,
            Keys.birdAnimations, Keys.reduceMotion, Keys.celebrations, Keys.weeklyFeather, Keys.voiceUsed, Keys.backedUp,
            Keys.morningCard, Keys.eveningCard, Keys.habitsInToday, Keys.habitHoldLearned,
        )
        private val intKeys = listOf(
            Keys.defaultReminderMinute, Keys.snoozeMinutes, Keys.dailySummaryMinute, Keys.habitSwipeHintsShown,
            Keys.nestScarf, Keys.nestGlasses, Keys.nestHat, Keys.nestPot, Keys.nestMoss, Keys.habitHoldHintShown,
        )
        private val stringKeys = listOf(Keys.themeMode, Keys.timeFormat, Keys.voiceLanguage, Keys.voiceRegion, Keys.backupStyle, Keys.habitSwipeHintWeek, Keys.nestStyle)
        private val setKeys = listOf(Keys.habitTreesSeen, Keys.rhythmPausedDays, Keys.nestUnlocked, Keys.nestSeen)

        /** Left out of a backup on purpose: they only make sense on this phone (a folder permission, the lock, today's dismissed cards). */
        private val deviceOnly = listOf<Preferences.Key<*>>(
            Keys.autoBackupEnabled, Keys.backupFolder, Keys.lastBackupAt, Keys.appLock,
            Keys.morningCardDismissedOn, Keys.eveningCardDismissedOn, Keys.habitRemindersFired,
        )

        internal fun backedUpNames(): Set<String> =
            (booleanKeys + intKeys + stringKeys + setKeys).map { it.name }.toSet()

        internal fun deviceOnlyNames(): Set<String> = deviceOnly.map { it.name }.toSet()
    }
}
