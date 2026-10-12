package com.elkrrai.feather.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Schema history:
 * - v1: tasks, task_lists
 * - v2: folders, tags, task_tags; tasks.parentTaskId and isPinned; task_lists.folderId and colorArgb
 *
 * Migrations are never destructive: users have real data. Exported schemas live in `app/schemas`.
 */
@Database(
    entities = [
        TaskEntity::class,
        ListEntity::class,
        FolderEntity::class,
        TagEntity::class,
        TaskTagCrossRef::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class FeatherDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun listDao(): ListDao
    abstract fun folderDao(): FolderDao
    abstract fun tagDao(): TagDao

    companion object {
        const val NAME = "feather.db"
    }
}
