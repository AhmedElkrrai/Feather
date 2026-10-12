package com.elkrrai.feather.di

import com.elkrrai.feather.reminders.AlarmReminderScheduler
import com.elkrrai.feather.reminders.ReminderScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RemindersModule {

    @Binds
    abstract fun bindReminderScheduler(impl: AlarmReminderScheduler): ReminderScheduler
}
