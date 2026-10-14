package com.elkrrai.feather.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert

/**
 * Whole-table reads and writes for export and import. Writes are upserts (an update in place,
 * never delete-and-insert), so rows that point at the one being written are not removed by a
 * cascade.
 */
@Dao
interface BackupDao {
    @Query("SELECT * FROM folders")
    suspend fun folders(): List<FolderEntity>

    @Query("SELECT * FROM task_lists")
    suspend fun lists(): List<ListEntity>

    @Query("SELECT * FROM tags")
    suspend fun tags(): List<TagEntity>

    @Query("SELECT * FROM tasks")
    suspend fun tasks(): List<TaskEntity>

    @Query("SELECT * FROM task_tags")
    suspend fun tagLinks(): List<TaskTagCrossRef>

    @Query("SELECT * FROM reminders")
    suspend fun reminders(): List<ReminderEntity>

    @Query("SELECT * FROM completion_history")
    suspend fun completions(): List<CompletionEntity>

    @Query("SELECT * FROM saved_filters")
    suspend fun filters(): List<SavedFilterEntity>

    @Upsert
    suspend fun upsertFilters(rows: List<SavedFilterEntity>)

    @Query("DELETE FROM saved_filters")
    suspend fun deleteAllFilters()

    @Upsert
    suspend fun upsertFolders(rows: List<FolderEntity>)

    @Upsert
    suspend fun upsertLists(rows: List<ListEntity>)

    @Upsert
    suspend fun upsertTags(rows: List<TagEntity>)

    @Upsert
    suspend fun upsertTasks(rows: List<TaskEntity>)

    @Upsert
    suspend fun upsertReminders(rows: List<ReminderEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTagLinks(rows: List<TaskTagCrossRef>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCompletions(rows: List<CompletionEntity>)

    @Query("DELETE FROM task_tags WHERE taskId IN (:taskIds)")
    suspend fun clearTagLinks(taskIds: List<String>)

    /** Removes every task with its tag links, reminders and history (they cascade). */
    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    @Query("DELETE FROM task_lists")
    suspend fun deleteAllLists()

    @Query("DELETE FROM folders")
    suspend fun deleteAllFolders()

    @Query("DELETE FROM tags")
    suspend fun deleteAllTags()
}
