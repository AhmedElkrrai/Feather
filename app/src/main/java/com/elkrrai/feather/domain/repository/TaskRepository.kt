package com.elkrrai.feather.domain.repository

import com.elkrrai.feather.domain.model.Task
import java.time.Duration
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/** Days a deleted task stays in the trash before it is removed for good. */
const val TRASH_RETENTION_DAYS = 30L

interface TaskRepository {
    /**
     * Every non-deleted top-level task of every list, each with its tags and subtasks, in
     * creation order. Views, sorting and search are applied on top by `domain/view`.
     */
    fun observeTasks(): Flow<List<Task>>

    /** One-shot snapshot of [observeTasks], for background work such as scheduling reminders. */
    suspend fun getActiveTasks(): List<Task>

    /** Deleted top-level tasks, most recently deleted first. */
    fun observeTrash(): Flow<List<Task>>

    /** A non-deleted top-level task with its tags and subtasks, or null. */
    suspend fun getTask(id: String): Task?

    /** Creates a task with just a title (quick add). The title must not be blank. */
    suspend fun addTask(listId: String, title: String, dueDate: LocalDate? = null): Task

    /** Inserts or updates the task's own fields; stamps `updatedAt`. Does not touch tags or subtasks. */
    suspend fun saveTask(task: Task)

    /**
     * Saves the task's own fields together with its tags, subtasks and reminders in one
     * transaction. Tags, subtasks and editable reminders missing from [task] are removed; a
     * pending snooze is left alone. Does not resurrect a deleted task.
     */
    suspend fun saveTaskDetails(task: Task)

    suspend fun setCompleted(id: String, completed: Boolean)

    suspend fun setPinned(id: String, pinned: Boolean)

    /** Soft delete (with the task's subtasks); [restoreTask] undoes it. */
    suspend fun deleteTask(id: String)

    suspend fun restoreTask(id: String)

    /** Permanently removes a deleted task and its subtasks. */
    suspend fun deleteForever(id: String)

    suspend fun emptyTrash()

    /** Permanently removes tasks that have been in the trash longer than [retention]. */
    suspend fun purgeTrash(retention: Duration = Duration.ofDays(TRASH_RETENTION_DAYS))
}
