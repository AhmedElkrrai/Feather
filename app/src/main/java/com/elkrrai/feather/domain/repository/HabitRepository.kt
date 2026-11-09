package com.elkrrai.feather.domain.repository

import com.elkrrai.feather.domain.habit.Habit
import com.elkrrai.feather.domain.habit.HabitDraft
import com.elkrrai.feather.domain.habit.HabitState
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * Habits, their weekly goals, their stages and what was logged. Separate from tasks: nothing here is a task, and a habit
 * reaches Today only through its own row, never as a task.
 */
interface HabitRepository {
    /** Every habit that was not deleted, in the order they were made, each with its goals, stages and logged days. */
    fun observeHabits(): Flow<List<HabitState>>

    /**
     * Makes a habit ([id] is null) or edits one. A new habit's first goal is in force from [thisWeekStart], so it counts from this week.
     * For an existing habit the name, the unit, the times and the Today settings change at once; a changed goal (the target, the times
     * a day or the list of stages) is queued for the start of the next week and replaces one that was already waiting.
     * The name must not be blank. Stages only count for a habit that counts days; a stage without a name is dropped.
     */
    suspend fun saveHabit(id: String?, draft: HabitDraft, thisWeekStart: LocalDate): Habit

    /**
     * Sets how much was logged on [date] (0 clears the day). For a habit that counts an amount the difference becomes a new log, or the newest
     * logs are cut down to fit; for any other habit the day's count is replaced.
     */
    suspend fun setAmount(id: String, date: LocalDate, amount: Int)

    /** Adds a log of [amount] (1 or more) to [date] of a habit that counts an amount; the day's total goes up by it. */
    suspend fun addLog(habitId: String, date: LocalDate, amount: Int)

    /** Changes the amount of one log; 0 deletes it. The day's total follows. */
    suspend fun changeLog(habitId: String, logId: String, amount: Int)

    /** Deletes one log; the day's total follows. */
    suspend fun deleteLog(habitId: String, logId: String)

    /**
     * Marks a stage of a habit done, or not done, on [date]. The day's amount becomes the number of stages done that day, so the
     * week, the tree and the rhythm see it like any other logged day.
     */
    suspend fun setStageDone(habitId: String, date: LocalDate, stageId: String, done: Boolean)

    /**
     * Makes exactly [stageIds] the stages done on [date] (others of the habit are undone): finishing a whole day, or putting it back as it was.
     * One change, so the row of Today does not flicker through every stage.
     */
    suspend fun setStagesDone(habitId: String, date: LocalDate, stageIds: Set<String>)

    /**
     * Puts the habits in the order of [orderedIds]. The order lives in the habits' own sort values, and each habit that moved counts as
     * edited, so a backup merge keeps the order that was made last.
     */
    suspend fun reorderHabits(orderedIds: List<String>)

    /** A soft delete, like a task: the rows stay and the habit is no longer listed. */
    suspend fun delete(id: String)
}
