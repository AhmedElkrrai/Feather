package com.elkrrai.feather

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.elkrrai.feather.data.backup.BackupWorker
import com.elkrrai.feather.domain.repository.ListRepository
import com.elkrrai.feather.domain.settings.AppSettings
import com.elkrrai.feather.domain.settings.ThemeMode
import com.elkrrai.feather.ui.DisplayPrefs
import com.elkrrai.feather.domain.repository.TaskRepository
import com.elkrrai.feather.domain.settings.SettingsRepository
import com.elkrrai.feather.reminders.ReminderChannels
import com.elkrrai.feather.reminders.ReminderSyncer
import com.elkrrai.feather.reminders.ReminderWorker
import com.elkrrai.feather.reminders.withAppLocale
import com.elkrrai.feather.widget.WidgetUpdater
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.elkrrai.feather.data.nest.NestTracker

@HiltAndroidApp
class FeatherApp : Application() {

    @Inject lateinit var listRepository: ListRepository
    @Inject lateinit var taskRepository: TaskRepository
    @Inject lateinit var reminderSyncer: ReminderSyncer
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var widgetUpdater: WidgetUpdater
    @Inject lateinit var nestTracker: NestTracker

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // The saved theme and formats are applied before the first screen so nothing flashes in
        // the wrong colors. One small read of a cached settings file, on the main thread.
        applyAppearance(runBlocking { settingsRepository.current() })
        appScope.launch {
            settingsRepository.settings.collect { settings ->
                withContext(Dispatchers.Main) { applyAppearance(settings) }
            }
        }
        // Channel names follow the app language, so refresh them on every start.
        ReminderChannels.ensure(withAppLocale())
        appScope.launch {
            listRepository.ensureInbox()
            // Housekeeping: tasks that stayed in the trash past the retention period go for good.
            taskRepository.purgeTrash()
        }
        // Re-arms the reminder alarm now and whenever tasks or settings change.
        reminderSyncer.start(appScope)
        ReminderWorker.schedule(this)
        // Keeps the home-screen widgets in step with the tasks.
        widgetUpdater.start(appScope)
        // Keeps score for the bird's nest: what is earned stays earned.
        nestTracker.start(appScope)
        // The daily backup job runs only while automatic backup is on and a folder is chosen.
        appScope.launch {
            settingsRepository.settings
                .map { it.autoBackupEnabled && it.backupFolder != null }
                .distinctUntilChanged()
                .collect { BackupWorker.schedule(this@FeatherApp, it) }
        }
    }

    /** Light or dark through AppCompat (so system bars and dialogs follow too), plus the display choices. */
    private fun applyAppearance(settings: AppSettings) {
        DisplayPrefs.update(settings)
        AppCompatDelegate.setDefaultNightMode(
            when (settings.themeMode) {
                ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            }
        )
    }
}
