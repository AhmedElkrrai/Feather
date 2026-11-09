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

    @Query("SELECT * FROM habits")
    suspend fun habits(): List<HabitEntity>

    @Query("SELECT * FROM habit_goals")
    suspend fun habitGoals(): List<HabitGoalEntity>

    @Query("SELECT * FROM habit_entries")
    suspend fun habitEntries(): List<HabitEntryEntity>

    @Query("SELECT * FROM habit_stages")
    suspend fun habitStages(): List<HabitStageEntity>

    @Query("SELECT * FROM habit_goal_stages")
    suspend fun habitGoalStages(): List<HabitGoalStageEntity>

    @Query("SELECT * FROM habit_stage_log")
    suspend fun habitStageLog(): List<HabitStageLogEntity>

    @Query("SELECT * FROM habit_logs")
    suspend fun habitLogs(): List<HabitLogEntity>

    @Upsert
    suspend fun upsertHabitLogs(rows: List<HabitLogEntity>)

    @Upsert
    suspend fun upsertHabits(rows: List<HabitEntity>)

    @Upsert
    suspend fun upsertHabitStages(rows: List<HabitStageEntity>)

    @Upsert
    suspend fun upsertHabitGoalStages(rows: List<HabitGoalStageEntity>)

    @Upsert
    suspend fun upsertHabitStageLog(rows: List<HabitStageLogEntity>)

    @Query("DELETE FROM habit_goal_stages WHERE habitId = :habitId AND effectiveFrom = :effectiveFrom")
    suspend fun deleteGoalStages(habitId: String, effectiveFrom: String)

    @Upsert
    suspend fun upsertHabitGoals(rows: List<HabitGoalEntity>)

    @Upsert
    suspend fun upsertHabitEntries(rows: List<HabitEntryEntity>)

    /** Removes every habit with its goals, stages and logged days (they cascade). */
    @Query("DELETE FROM habits")
    suspend fun deleteAllHabits()

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

    @Query("DELETE FROM tags")
    suspend fun deleteAllTags()
}
