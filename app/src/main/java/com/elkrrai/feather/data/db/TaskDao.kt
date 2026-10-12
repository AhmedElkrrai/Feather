package com.elkrrai.feather.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    /** Every non-deleted row, top-level tasks and subtasks together; the repository groups them. */
    @Transaction
    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL ORDER BY sortOrder, createdAt")
    fun observeAllActive(): Flow<List<TaskWithTags>>

    /** One-shot version of [observeAllActive], for background work such as scheduling reminders. */
    @Transaction
    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL ORDER BY sortOrder, createdAt")
    suspend fun getAllActive(): List<TaskWithTags>

    /** Deleted top-level tasks, newest deletion first. Subtasks are shown through their parent. */
    @Transaction
    @Query(
        "SELECT * FROM tasks WHERE deletedAt IS NOT NULL AND parentTaskId IS NULL " +
            "ORDER BY deletedAt DESC"
    )
    fun observeTrash(): Flow<List<TaskWithTags>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getWithTags(id: String): TaskWithTags?

    /** Includes soft-deleted rows so callers can preserve `deletedAt` on update. */
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: String): TaskEntity?

    @Query(
        "SELECT * FROM tasks WHERE parentTaskId = :parentId AND deletedAt IS NULL " +
            "ORDER BY sortOrder, createdAt"
    )
    suspend fun getActiveSubtasks(parentId: String): List<TaskEntity>

    @Upsert
    suspend fun upsert(task: TaskEntity)

    @Upsert
    suspend fun upsertAll(tasks: List<TaskEntity>)

    @Query("UPDATE tasks SET completedAt = :completedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setCompletedAt(id: String, completedAt: Long?, updatedAt: Long)

    @Query("UPDATE tasks SET isPinned = :pinned, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean, updatedAt: Long)

    /** Soft-deletes a task together with its subtasks, all with the same timestamp. */
    @Query(
        "UPDATE tasks SET deletedAt = :deletedAt, updatedAt = :deletedAt " +
            "WHERE (id = :id OR parentTaskId = :id) AND deletedAt IS NULL"
    )
    suspend fun softDelete(id: String, deletedAt: Long)

    /**
     * Restores a task and the subtasks that were deleted with it (same `deletedAt`); a subtask
     * removed earlier on its own stays deleted.
     */
    @Query(
        "UPDATE tasks SET deletedAt = NULL, updatedAt = :updatedAt " +
            "WHERE (id = :id OR parentTaskId = :id) " +
            "AND deletedAt = (SELECT deletedAt FROM tasks WHERE id = :id)"
    )
    suspend fun restore(id: String, updatedAt: Long)

    @Query(
        "UPDATE tasks SET deletedAt = :deletedAt, updatedAt = :deletedAt " +
            "WHERE id IN (:ids) AND deletedAt IS NULL"
    )
    suspend fun softDeleteByIds(ids: List<String>, deletedAt: Long)

    /** Moves every task (and subtask) of a list, used when a list is deleted. */
    @Query("UPDATE tasks SET listId = :toListId, updatedAt = :updatedAt WHERE listId = :fromListId")
    suspend fun moveAllToList(fromListId: String, toListId: String, updatedAt: Long)

    /** Permanently removes a task and its subtasks (cross-refs go with them by cascade). */
    @Query("DELETE FROM tasks WHERE id = :id OR parentTaskId = :id")
    suspend fun hardDelete(id: String)

    @Query("DELETE FROM tasks WHERE deletedAt IS NOT NULL")
    suspend fun hardDeleteAllDeleted()

    @Query("DELETE FROM tasks WHERE deletedAt IS NOT NULL AND deletedAt < :cutoff")
    suspend fun purgeDeletedBefore(cutoff: Long)

    @Query("DELETE FROM task_tags WHERE taskId = :taskId")
    suspend fun clearTags(taskId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTaskTags(refs: List<TaskTagCrossRef>)
}
