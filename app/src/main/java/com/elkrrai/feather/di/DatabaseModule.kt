package com.elkrrai.feather.di

import android.content.Context
import androidx.room.Room
import com.elkrrai.feather.data.db.BackupDao
import com.elkrrai.feather.data.db.CompletionDao
import com.elkrrai.feather.data.db.FeatherDatabase
import com.elkrrai.feather.data.db.FolderDao
import com.elkrrai.feather.data.db.ListDao
import com.elkrrai.feather.data.db.ReminderDao
import com.elkrrai.feather.data.db.TagDao
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
    fun provideFolderDao(db: FeatherDatabase): FolderDao = db.folderDao()

    @Provides
    fun provideTagDao(db: FeatherDatabase): TagDao = db.tagDao()

    @Provides
    fun provideReminderDao(db: FeatherDatabase): ReminderDao = db.reminderDao()

    @Provides
    fun provideCompletionDao(db: FeatherDatabase): CompletionDao = db.completionDao()

    @Provides
    fun provideBackupDao(db: FeatherDatabase): BackupDao = db.backupDao()

    @Provides
    fun provideClock(): Clock = Clock.systemUTC()
}
