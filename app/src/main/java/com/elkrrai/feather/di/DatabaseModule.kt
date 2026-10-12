package com.elkrrai.feather.di

import android.content.Context
import androidx.room.Room
import com.elkrrai.feather.data.db.FeatherDatabase
import com.elkrrai.feather.data.db.ListDao
import com.elkrrai.feather.data.db.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FeatherDatabase =
        Room.databaseBuilder(context, FeatherDatabase::class.java, FeatherDatabase.NAME).build()

    @Provides
    fun provideTaskDao(db: FeatherDatabase): TaskDao = db.taskDao()

    @Provides
    fun provideListDao(db: FeatherDatabase): ListDao = db.listDao()

    @Provides
    fun provideClock(): Clock = Clock.systemUTC()
}
