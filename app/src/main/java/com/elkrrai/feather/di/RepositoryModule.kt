package com.elkrrai.feather.di

import com.elkrrai.feather.data.db.RoomTransactionRunner
import com.elkrrai.feather.data.db.TransactionRunner
import com.elkrrai.feather.data.repository.BackupRepositoryImpl
import com.elkrrai.feather.data.repository.HabitRepositoryImpl
import com.elkrrai.feather.data.repository.InboxRepositoryImpl
import com.elkrrai.feather.data.repository.ReminderRepositoryImpl
import com.elkrrai.feather.data.repository.TagRepositoryImpl
import com.elkrrai.feather.data.repository.TaskRepositoryImpl
import com.elkrrai.feather.domain.backup.BackupRepository
import com.elkrrai.feather.domain.repository.HabitRepository
import com.elkrrai.feather.domain.repository.InboxRepository
import com.elkrrai.feather.domain.repository.ReminderRepository
import com.elkrrai.feather.domain.repository.TagRepository
import com.elkrrai.feather.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.elkrrai.feather.data.repository.RhythmRepositoryImpl
import com.elkrrai.feather.domain.rhythm.RhythmRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds
    abstract fun bindInboxRepository(impl: InboxRepositoryImpl): InboxRepository

    @Binds
    abstract fun bindTagRepository(impl: TagRepositoryImpl): TagRepository

    @Binds
    abstract fun bindReminderRepository(impl: ReminderRepositoryImpl): ReminderRepository

    @Binds
    abstract fun bindRhythmRepository(impl: RhythmRepositoryImpl): RhythmRepository

    @Binds
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository

    @Binds
    abstract fun bindHabitRepository(impl: HabitRepositoryImpl): HabitRepository

    @Binds
    abstract fun bindTransactionRunner(impl: RoomTransactionRunner): TransactionRunner
}
