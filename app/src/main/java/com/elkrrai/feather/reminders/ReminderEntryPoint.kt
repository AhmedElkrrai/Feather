package com.elkrrai.feather.reminders

import com.elkrrai.feather.domain.repository.ReminderRepository
import com.elkrrai.feather.domain.repository.TaskRepository
import com.elkrrai.feather.domain.settings.SettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Lets the broadcast receivers and the worker reach the app's dependencies. Using an entry point
 * (instead of `@AndroidEntryPoint`) avoids a Kotlin limitation with Hilt-injected receivers.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderEntryPoint {
    fun scheduler(): ReminderScheduler
    fun notifier(): ReminderNotifier
    fun taskRepository(): TaskRepository
    fun reminderRepository(): ReminderRepository
    fun settingsRepository(): SettingsRepository
}
