package com.elkrrai.feather.di

import com.elkrrai.feather.data.repository.ListRepositoryImpl
import com.elkrrai.feather.data.repository.TaskRepositoryImpl
import com.elkrrai.feather.domain.repository.ListRepository
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
}
