package com.elkrrai.feather.domain.repository

import com.elkrrai.feather.domain.habit.Habit
import com.elkrrai.feather.domain.habit.HabitKind
import com.elkrrai.feather.domain.habit.HabitState
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/** Habits, their weekly goals and what was logged. Separate from tasks: nothing here shows in Today, Overdue or Goals. */
interface HabitRepository {
    /** Every habit that was not deleted, in the order they were made, each with its goals and logged days. */
    fun observeHabits(): Flow<List<HabitState>>

    /**
     * Makes a habit whose first goal is in force from [thisWeekStart], so it counts from this week.
     * The name must not be blank.
     */
    suspend fun addHabit(
        name: String,
        kind: HabitKind,
        unit: String,
        target: Int,
        dailyTarget: Int,
        thisWeekStart: LocalDate,
    ): Habit

    /** The name and the unit change at once; they do not touch any week's goal. */
    suspend fun rename(id: String, name: String, unit: String)

    /**
     * An edit of the goal, made during a week: it takes effect on [nextWeekStart] and replaces an
     * edit that was already waiting. The goal of the week that has begun does not change.
     */
    suspend fun queueGoal(id: String, target: Int, dailyTarget: Int, nextWeekStart: LocalDate)

    /** Sets how much was logged on [date] (0 clears the day). */
    suspend fun setAmount(id: String, date: LocalDate, amount: Int)

    /** A soft delete, like a task: the rows stay and the habit is no longer listed. */
    suspend fun delete(id: String)
}
