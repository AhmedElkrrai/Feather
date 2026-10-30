package com.elkrrai.feather.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Schema history: the app's database was started again at version 1 before the first release, with
 * everything the earlier versions had (tasks, tags and their links, reminders, completion history,
 * the counter columns, habits). The old migrations were dropped with them: there is no data from before
 * to carry over. From the first release on, every change is a new version with a migration, and
 * migrations never destroy what the person made. Exported schemas live in `app/schemas`.
 *
 * [ListEntity] stays: every task is kept in the Inbox list, see [INBOX_LIST_ID].
 */
@Database(
    entities = [
        TaskEntity::class,
        ListEntity::class,
        TagEntity::class,
        TaskTagCrossRef::class,
        ReminderEntity::class,
        CompletionEntity::class,
        HabitEntity::class,
        HabitGoalEntity::class,
        HabitEntryEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class FeatherDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun listDao(): ListDao
    abstract fun tagDao(): TagDao
    abstract fun reminderDao(): ReminderDao
    abstract fun completionDao(): CompletionDao
    abstract fun backupDao(): BackupDao
    abstract fun habitDao(): HabitDao

    companion object {
        const val NAME = "feather.db"
    }
}
