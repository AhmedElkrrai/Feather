package com.elkrrai.feather.data.repository

import com.elkrrai.feather.data.db.TaskDao
import com.elkrrai.feather.data.db.toDomain
import com.elkrrai.feather.data.db.toDomainTasks
import com.elkrrai.feather.data.db.toEntity
import com.elkrrai.feather.data.db.toSubtask
import com.elkrrai.feather.domain.model.Task
import com.elkrrai.feather.domain.repository.TaskRepository
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val clock: Clock,
) : TaskRepository {

    override fun observeTasks(listId: String): Flow<List<Task>> =
        taskDao.observeAllActive().map { rows ->
            rows.toDomainTasks()
                .filter { it.listId == listId }
                .sortedWith(compareBy({ it.isCompleted }, { it.sortOrder }, { it.createdAt }))
        }

    override fun observeTask(id: String): Flow<Task?> =
        taskDao.observeAllActive().map { rows -> rows.toDomainTasks().firstOrNull { it.id == id } }

    override suspend fun getTask(id: String): Task? {
        val row = taskDao.getWithTags(id) ?: return null
        if (row.task.deletedAt != null || row.task.parentTaskId != null) return null
        val subtasks = taskDao.getActiveSubtasks(id).map { it.toSubtask() }
        return row.task.toDomain(
            tags = row.tags.filter { it.deletedAt == null }.map { it.toDomain() },
            subtasks = subtasks,
        )
    }

    override suspend fun addTask(listId: String, title: String): Task {
        require(title.isNotBlank()) { "Task title must not be blank" }
        val now = Instant.now(clock)
        val task = Task(
            id = UUID.randomUUID().toString(),
            listId = listId,
            title = title.trim(),
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

    override suspend fun setCompleted(id: String, completed: Boolean) {
        val now = clock.millis()
        taskDao.setCompletedAt(id, completedAt = if (completed) now else null, updatedAt = now)
    }

    override suspend fun deleteTask(id: String) {
        taskDao.softDelete(id, deletedAt = clock.millis())
    }

    override suspend fun restoreTask(id: String) {
        taskDao.restore(id, updatedAt = clock.millis())
    }
}
