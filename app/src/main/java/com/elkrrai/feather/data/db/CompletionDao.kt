package com.elkrrai.feather.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CompletionDao {
    @Insert
    suspend fun insert(completion: CompletionEntity)

    /** Newest first. */
    @Query("SELECT * FROM completion_history WHERE taskId = :taskId ORDER BY completedAt DESC, id")
    suspend fun getForTask(taskId: String): List<CompletionEntity>

    /** Takes back a completion, used when a finished task is reopened. */
    @Query("DELETE FROM completion_history WHERE taskId = :taskId AND completedAt = :completedAt")
    suspend fun deleteForTaskAt(taskId: String, completedAt: Long)
}
