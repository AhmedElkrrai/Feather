package com.elkrrai.feather.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** When a task was last finished, epoch millis. */
data class LatestCompletion(val taskId: String, val completedAt: Long)

@Dao
interface CompletionDao {
    @Insert
    suspend fun insert(completion: CompletionEntity)

    /** Newest first. */
    @Query("SELECT * FROM completion_history WHERE taskId = :taskId ORDER BY completedAt DESC, id")
    suspend fun getForTask(taskId: String): List<CompletionEntity>

    /** The most recent completion of every task that has one. */
    @Query("SELECT taskId, MAX(completedAt) AS completedAt FROM completion_history GROUP BY taskId")
    fun observeLatest(): Flow<List<LatestCompletion>>

    /** Takes back a completion, used when a finished task is reopened. */
    @Query("DELETE FROM completion_history WHERE taskId = :taskId AND completedAt = :completedAt")
    suspend fun deleteForTaskAt(taskId: String, completedAt: Long)
}
