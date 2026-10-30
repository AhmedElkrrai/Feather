package com.elkrrai.feather.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteTable
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec

/**
 * Schema history:
 * - v1: tasks, task_lists
 * - v2: folders, tags, task_tags; tasks.parentTaskId and isPinned; task_lists.folderId and colorArgb
 * - v3: reminders
 * - v4: tasks.rrule and repeatMode; completion_history
 * - v5: saved_filters
 * - v6: tags.parentId (a tag can sit under another tag)
 * - v7: folders and saved_filters are dropped (lists, folders and filters left the app); task_lists stays,
 *   see [ListEntity]
 * - v8: tasks.counterTarget, counterStep and counterDone (a task that counts toward a target)
 * - v9: habits, habit_goals and habit_entries (habits are not tasks)
 *
 * Migrations are never destructive for what the person made: users have real data. Exported schemas
 * live in `app/schemas`.
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
    version = 9,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7, spec = FeatherDatabase.DropFoldersAndFilters::class),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9),
    ],
)
abstract class FeatherDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun listDao(): ListDao
    abstract fun tagDao(): TagDao
    abstract fun reminderDao(): ReminderDao
    abstract fun completionDao(): CompletionDao
    abstract fun backupDao(): BackupDao
    abstract fun habitDao(): HabitDao

    /** Nothing refers to these two tables, so dropping them cannot touch any task. */
    @DeleteTable.Entries(
        DeleteTable(tableName = "folders"),
        DeleteTable(tableName = "saved_filters"),
    )
    class DropFoldersAndFilters : AutoMigrationSpec

    companion object {
        const val NAME = "feather.db"
    }
}
