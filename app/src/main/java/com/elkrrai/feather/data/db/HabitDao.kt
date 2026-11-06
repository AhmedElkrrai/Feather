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

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabit(id: String): HabitEntity?

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

    @Upsert
    suspend fun upsertGoal(goal: HabitGoalEntity)

    @Upsert
    suspend fun upsertEntry(entry: HabitEntryEntity)

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
