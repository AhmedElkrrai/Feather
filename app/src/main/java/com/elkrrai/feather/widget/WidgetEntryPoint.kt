package com.elkrrai.feather.widget

import com.elkrrai.feather.domain.repository.TaskRepository
import com.elkrrai.feather.reminders.ReminderNotifier
import com.elkrrai.feather.reminders.ReminderScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Lets the widgets and their button callbacks reach the app's data. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun taskRepository(): TaskRepository
    fun notifier(): ReminderNotifier
    fun scheduler(): ReminderScheduler
}
