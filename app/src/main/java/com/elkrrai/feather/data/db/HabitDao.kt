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

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabit(id: String): HabitEntity?

    @Upsert
    suspend fun upsertHabit(habit: HabitEntity)

    @Upsert
    suspend fun upsertGoal(goal: HabitGoalEntity)

    @Upsert
    suspend fun upsertEntry(entry: HabitEntryEntity)

    @Query("UPDATE habits SET deletedAt = :at, updatedAt = :at WHERE id = :id AND deletedAt IS NULL")
    suspend fun softDelete(id: String, at: Long)
}
