package com.elkrrai.feather.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query(
        """
        SELECT * FROM tasks
        WHERE listId = :listId AND deletedAt IS NULL
        ORDER BY (completedAt IS NOT NULL), sortOrder, createdAt
        """
    )
    fun observeByList(listId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id AND deletedAt IS NULL")
    fun observeById(id: String): Flow<TaskEntity?>

    /** Includes soft-deleted rows so callers can preserve `deletedAt` on update. */
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: String): TaskEntity?

    @Upsert
    suspend fun upsert(task: TaskEntity)

    @Query("UPDATE tasks SET completedAt = :completedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setCompletedAt(id: String, completedAt: Long?, updatedAt: Long)

    @Query("UPDATE tasks SET deletedAt = :deletedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setDeletedAt(id: String, deletedAt: Long?, updatedAt: Long)
}
