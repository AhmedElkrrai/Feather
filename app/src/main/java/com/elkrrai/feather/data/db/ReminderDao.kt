package com.elkrrai.feather.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: String): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE taskId = :taskId AND deletedAt IS NULL")
    suspend fun getActiveForTask(taskId: String): List<ReminderEntity>

    @Upsert
    suspend fun upsert(reminder: ReminderEntity)

    @Upsert
    suspend fun upsertAll(reminders: List<ReminderEntity>)

    /** Soft-deletes the editable reminders (kinds 0 and 1) of a task that are not in [keepIds]. */
    @Query(
        "UPDATE reminders SET deletedAt = :deletedAt " +
            "WHERE taskId = :taskId AND kind != 2 AND deletedAt IS NULL AND id NOT IN (:keepIds)"
    )
    suspend fun softDeleteRegularExcept(taskId: String, keepIds: List<String>, deletedAt: Long)

    /** Soft-deletes the pending snoozes (kind 2) of a task. */
    @Query("UPDATE reminders SET deletedAt = :deletedAt WHERE taskId = :taskId AND kind = 2 AND deletedAt IS NULL")
    suspend fun softDeleteSnoozes(taskId: String, deletedAt: Long)

    @Query("UPDATE reminders SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long)

    /** Records which trigger was shown. Deliberately leaves `updatedAt` alone. */
    @Query("UPDATE reminders SET firedForTrigger = :trigger WHERE id = :id")
    suspend fun markFired(id: String, trigger: Long)
}
