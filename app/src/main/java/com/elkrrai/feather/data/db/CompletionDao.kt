package com.elkrrai.feather.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** When a task was last finished, epoch millis. */
data class LatestCompletion(val taskId: String, val completedAt: Long)

/** One finish with the task's title, for the Your rhythm screen. */
data class CompletionRow(val taskId: String, val title: String, val completedAt: Long)

@Dao
interface CompletionDao {
    @Insert
    suspend fun insert(completion: CompletionEntity)

    /** Newest first. */
    @Query("SELECT * FROM completion_history WHERE taskId = :taskId ORDER BY completedAt DESC, id")
    suspend fun getForTask(taskId: String): List<CompletionEntity>

    /** Every finish of a top-level task (a deleted one included: it still happened), newest first. */
    @Query(
        "SELECT c.taskId AS taskId, t.title AS title, c.completedAt AS completedAt " +
            "FROM completion_history c JOIN tasks t ON t.id = c.taskId " +
            "WHERE t.parentTaskId IS NULL ORDER BY c.completedAt DESC"
    )
    fun observeRows(): Flow<List<CompletionRow>>

    /** The most recent completion of every task that has one. */
    @Query("SELECT taskId, MAX(completedAt) AS completedAt FROM completion_history GROUP BY taskId")
    fun observeLatest(): Flow<List<LatestCompletion>>

    /** Takes back a completion, used when a finished task is reopened. */
    @Query("DELETE FROM completion_history WHERE taskId = :taskId AND completedAt = :completedAt")
    suspend fun deleteForTaskAt(taskId: String, completedAt: Long)
}
