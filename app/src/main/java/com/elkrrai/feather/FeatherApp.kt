package com.elkrrai.feather

import android.app.Application
import com.elkrrai.feather.data.backup.BackupWorker
import com.elkrrai.feather.domain.repository.ListRepository
import com.elkrrai.feather.domain.repository.TaskRepository
import com.elkrrai.feather.domain.settings.SettingsRepository
import com.elkrrai.feather.reminders.ReminderChannels
import com.elkrrai.feather.reminders.ReminderSyncer
import com.elkrrai.feather.reminders.ReminderWorker
import com.elkrrai.feather.reminders.withAppLocale
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltAndroidApp
class FeatherApp : Application() {

    @Inject lateinit var listRepository: ListRepository
    @Inject lateinit var taskRepository: TaskRepository
    @Inject lateinit var reminderSyncer: ReminderSyncer
    @Inject lateinit var settingsRepository: SettingsRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
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
        // The daily backup job runs only while automatic backup is on and a folder is chosen.
        appScope.launch {
            settingsRepository.settings
                .map { it.autoBackupEnabled && it.backupFolder != null }
                .distinctUntilChanged()
                .collect { BackupWorker.schedule(this@FeatherApp, it) }
        }
    }
}
