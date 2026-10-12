package com.elkrrai.feather.domain.repository

import com.elkrrai.feather.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    /** Non-deleted tasks of a list: open tasks first (by sort order), completed tasks last. */
    fun observeTasks(listId: String): Flow<List<Task>>

    fun observeTask(id: String): Flow<Task?>

    suspend fun getTask(id: String): Task?

    /** Creates a task with just a title (quick add). The title must not be blank. */
    suspend fun addTask(listId: String, title: String): Task

    /** Inserts or updates a task; stamps `updatedAt`. Does not resurrect a deleted task. */
    suspend fun saveTask(task: Task)

    suspend fun setCompleted(id: String, completed: Boolean)

    /** Soft delete; [restoreTask] undoes it. */
    suspend fun deleteTask(id: String)

    suspend fun restoreTask(id: String)
}
