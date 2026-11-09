package com.elkrrai.feather.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE deletedAt IS NULL ORDER BY sortOrder, createdAt")
    fun observeHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habit_goals ORDER BY effectiveFrom")
    fun observeGoals(): Flow<List<HabitGoalEntity>>

    @Query("SELECT * FROM habit_entries WHERE amount > 0")
    fun observeEntries(): Flow<List<HabitEntryEntity>>

    @Query("SELECT * FROM habit_stages")
    fun observeStages(): Flow<List<HabitStageEntity>>

    @Query("SELECT * FROM habit_goal_stages ORDER BY habitId, effectiveFrom, position")
    fun observeGoalStages(): Flow<List<HabitGoalStageEntity>>

    @Query("SELECT * FROM habit_stage_log WHERE done = 1")
    fun observeStageLog(): Flow<List<HabitStageLogEntity>>

    @Query("SELECT * FROM habit_logs ORDER BY loggedAt, id")
    fun observeLogs(): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabit(id: String): HabitEntity?

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND date = :date ORDER BY loggedAt, id")
    suspend fun logsOn(habitId: String, date: String): List<HabitLogEntity>

    @Query("SELECT * FROM habit_logs WHERE id = :id AND habitId = :habitId")
    suspend fun getLog(habitId: String, id: String): HabitLogEntity?

    @Query("SELECT COALESCE(SUM(amount), 0) FROM habit_logs WHERE habitId = :habitId AND date = :date")
    suspend fun logTotal(habitId: String, date: String): Int

    @Query("SELECT habitId, date, SUM(amount) AS total FROM habit_logs GROUP BY habitId, date")
    suspend fun logTotals(): List<HabitDayTotal>

    @Query("SELECT * FROM habit_entries WHERE habitId = :habitId AND date = :date")
    suspend fun getEntry(habitId: String, date: String): HabitEntryEntity?

    /** The days of habits that count an amount (kind 1) that have an amount but no log: what was logged before logs existed. */
    @Query(
        "SELECT e.* FROM habit_entries e JOIN habits h ON h.id = e.habitId WHERE h.kind = 1 AND e.amount > 0 AND NOT EXISTS " +
            "(SELECT 1 FROM habit_logs l WHERE l.habitId = e.habitId AND l.date = e.date)",
    )
    suspend fun amountsWithoutLogs(): List<HabitEntryEntity>

    @Query("SELECT * FROM habit_goals WHERE habitId = :habitId ORDER BY effectiveFrom")
    suspend fun goalsOf(habitId: String): List<HabitGoalEntity>

    @Query("SELECT * FROM habit_stages WHERE habitId = :habitId")
    suspend fun stagesOf(habitId: String): List<HabitStageEntity>

    @Query("SELECT * FROM habit_stages WHERE id = :id")
    suspend fun getStage(id: String): HabitStageEntity?

    /** The stages of the goal that takes effect on [effectiveFrom], in order. */
    @Query("SELECT stageId FROM habit_goal_stages WHERE habitId = :habitId AND effectiveFrom = :effectiveFrom ORDER BY position")
    suspend fun goalStageIds(habitId: String, effectiveFrom: String): List<String>

    @Query("SELECT stageId FROM habit_stage_log WHERE habitId = :habitId AND date = :date AND done = 1")
    suspend fun doneStageIds(habitId: String, date: String): List<String>

    @Query("SELECT COUNT(*) FROM habit_stage_log WHERE habitId = :habitId AND date = :date AND done = 1")
    suspend fun doneStageCount(habitId: String, date: String): Int

    @Upsert
    suspend fun upsertHabit(habit: HabitEntity)

    /** The leaf looks the listed habits have, for dealing the next one. */
    @Query("SELECT leaf FROM habits WHERE deletedAt IS NULL AND leaf >= 0")
    suspend fun assignedLeaves(): List<Int>

    @Query("SELECT * FROM habits WHERE deletedAt IS NULL AND leaf < 0 ORDER BY createdAt, id")
    suspend fun habitsWithoutLeaf(): List<HabitEntity>

    /** Only the look changes: a deal is not an edit of the habit, so it does not touch `updatedAt`. */
    @Query("UPDATE habits SET leaf = :leaf WHERE id = :id")
    suspend fun setLeaf(id: String, leaf: Int)

    @Query("UPDATE habits SET sortOrder = :sortOrder, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setSortOrder(id: String, sortOrder: Long, updatedAt: Long)

    @Upsert
    suspend fun upsertGoal(goal: HabitGoalEntity)

    @Upsert
    suspend fun upsertEntry(entry: HabitEntryEntity)

    @Upsert
    suspend fun upsertLog(row: HabitLogEntity)

    @Query("DELETE FROM habit_logs WHERE id = :id")
    suspend fun deleteLog(id: String)

    @Upsert
    suspend fun upsertStages(rows: List<HabitStageEntity>)

    @Query("DELETE FROM habit_goal_stages WHERE habitId = :habitId AND effectiveFrom = :effectiveFrom")
    suspend fun deleteGoalStages(habitId: String, effectiveFrom: String)

    @Upsert
    suspend fun upsertGoalStages(rows: List<HabitGoalStageEntity>)

    @Upsert
    suspend fun upsertStageLog(row: HabitStageLogEntity)

    @Query("UPDATE habits SET deletedAt = :at, updatedAt = :at WHERE id = :id AND deletedAt IS NULL")
    suspend fun softDelete(id: String, at: Long)
}
