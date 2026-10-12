package com.elkrrai.feather.data.repository

import com.elkrrai.feather.data.db.TaskDao
import com.elkrrai.feather.data.db.TaskEntity
import com.elkrrai.feather.data.db.TaskTagCrossRef
import com.elkrrai.feather.data.db.TransactionRunner
import com.elkrrai.feather.data.db.toDomain
import com.elkrrai.feather.data.db.toDomainTasks
import com.elkrrai.feather.data.db.toEntity
import com.elkrrai.feather.data.db.toSubtask
import com.elkrrai.feather.domain.model.Task
import com.elkrrai.feather.domain.repository.TaskRepository
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val tx: TransactionRunner,
    private val clock: Clock,
) : TaskRepository {

    override fun observeTasks(): Flow<List<Task>> =
        taskDao.observeAllActive().map { it.toDomainTasks() }

    override fun observeTrash(): Flow<List<Task>> =
        taskDao.observeTrash().map { it.toDomainTasks() }

    override suspend fun getTask(id: String): Task? {
        val row = taskDao.getWithTags(id) ?: return null
        if (row.task.deletedAt != null || row.task.parentTaskId != null) return null
        return row.task.toDomain(
            tags = row.tags.filter { it.deletedAt == null }.map { it.toDomain() },
            subtasks = taskDao.getActiveSubtasks(id).map { it.toSubtask() },
        )
    }

    override suspend fun addTask(listId: String, title: String, dueDate: LocalDate?): Task {
        require(title.isNotBlank()) { "Task title must not be blank" }
        val now = Instant.now(clock)
        val task = Task(
            id = UUID.randomUUID().toString(),
            listId = listId,
            title = title.trim(),
            dueDate = dueDate,
            sortOrder = now.toEpochMilli(),
            createdAt = now,
            updatedAt = now,
        )
        taskDao.upsert(task.toEntity())
        return task
    }

    override suspend fun saveTask(task: Task) {
        val existing = taskDao.getById(task.id)
        val updated = task.copy(updatedAt = Instant.now(clock))
        taskDao.upsert(updated.toEntity(deletedAt = existing?.deletedAt))
    }

    override suspend fun saveTaskDetails(task: Task) {
        tx.run {
            saveTask(task)
            replaceTags(task)
            replaceSubtasks(task)
        }
    }

    private suspend fun replaceTags(task: Task) {
        taskDao.clearTags(task.id)
        taskDao.insertTaskTags(task.tags.map { TaskTagCrossRef(task.id, it.id) }.distinct())
    }

    /** Subtasks missing from [Task.subtasks] are soft-deleted; the rest are upserted in order. */
    private suspend fun replaceSubtasks(task: Task) {
        val now = clock.millis()
        val parentDeletedAt = taskDao.getById(task.id)?.deletedAt
        val existing = taskDao.getActiveSubtasks(task.id).associateBy { it.id }
        val kept = task.subtasks.filter { it.title.isNotBlank() }

        val removedIds = existing.keys - kept.map { it.id }.toSet()
        if (removedIds.isNotEmpty()) taskDao.softDeleteByIds(removedIds.toList(), now)

        val rows = kept.mapIndexed { index, subtask ->
            val old = existing[subtask.id]
            TaskEntity(
                id = subtask.id,
                listId = task.listId,
                title = subtask.title.trim(),
                notes = "",
                dueDate = null,
                dueTime = null,
                priority = 0,
                completedAt = if (subtask.isCompleted) old?.completedAt ?: now else null,
                sortOrder = index.toLong(),
                createdAt = old?.createdAt ?: now,
                updatedAt = now,
                deletedAt = parentDeletedAt,
                parentTaskId = task.id,
                isPinned = false,
            )
        }
        if (rows.isNotEmpty()) taskDao.upsertAll(rows)
    }

    override suspend fun setCompleted(id: String, completed: Boolean) {
        val now = clock.millis()
        taskDao.setCompletedAt(id, completedAt = if (completed) now else null, updatedAt = now)
    }

    override suspend fun setPinned(id: String, pinned: Boolean) {
        taskDao.setPinned(id, pinned, updatedAt = clock.millis())
    }

    override suspend fun deleteTask(id: String) {
        taskDao.softDelete(id, deletedAt = clock.millis())
    }

    override suspend fun restoreTask(id: String) {
        taskDao.restore(id, updatedAt = clock.millis())
    }

    override suspend fun deleteForever(id: String) {
        // Only tasks already in the trash can be removed for good.
        if (taskDao.getById(id)?.deletedAt == null) return
        taskDao.hardDelete(id)
    }

    override suspend fun emptyTrash() {
        taskDao.hardDeleteAllDeleted()
    }

    override suspend fun purgeTrash(retention: Duration) {
        taskDao.purgeDeletedBefore(clock.millis() - retention.toMillis())
    }
}
