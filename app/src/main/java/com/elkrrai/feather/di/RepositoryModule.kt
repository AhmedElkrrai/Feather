package com.elkrrai.feather.di

import com.elkrrai.feather.data.db.RoomTransactionRunner
import com.elkrrai.feather.data.db.TransactionRunner
import com.elkrrai.feather.data.repository.BackupRepositoryImpl
import com.elkrrai.feather.data.repository.ListRepositoryImpl
import com.elkrrai.feather.data.repository.ReminderRepositoryImpl
import com.elkrrai.feather.data.repository.SavedFilterRepositoryImpl
import com.elkrrai.feather.data.repository.TagRepositoryImpl
import com.elkrrai.feather.data.repository.TaskRepositoryImpl
import com.elkrrai.feather.domain.backup.BackupRepository
import com.elkrrai.feather.domain.repository.ListRepository
import com.elkrrai.feather.domain.repository.ReminderRepository
import com.elkrrai.feather.domain.repository.SavedFilterRepository
import com.elkrrai.feather.domain.repository.TagRepository
import com.elkrrai.feather.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds
    abstract fun bindListRepository(impl: ListRepositoryImpl): ListRepository

    @Binds
    abstract fun bindTagRepository(impl: TagRepositoryImpl): TagRepository

    @Binds
    abstract fun bindReminderRepository(impl: ReminderRepositoryImpl): ReminderRepository

    @Binds
    abstract fun bindSavedFilterRepository(impl: SavedFilterRepositoryImpl): SavedFilterRepository

    @Binds
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository

    @Binds
    abstract fun bindTransactionRunner(impl: RoomTransactionRunner): TransactionRunner
}
