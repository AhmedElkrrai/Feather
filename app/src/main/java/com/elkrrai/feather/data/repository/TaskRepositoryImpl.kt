package com.elkrrai.feather.data.repository

import com.elkrrai.feather.data.db.ReminderDao
import com.elkrrai.feather.data.db.TaskDao
import com.elkrrai.feather.data.db.TaskEntity
import com.elkrrai.feather.data.db.TaskTagCrossRef
import com.elkrrai.feather.data.db.TransactionRunner
import com.elkrrai.feather.data.db.toDomain
import com.elkrrai.feather.data.db.toDomainTasks
import com.elkrrai.feather.data.db.toEntity
import com.elkrrai.feather.data.db.toSubtask
import com.elkrrai.feather.domain.model.ReminderKind
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
    private val reminderDao: ReminderDao,
    private val tx: TransactionRunner,
    private val clock: Clock,
) : TaskRepository {

    override fun observeTasks(): Flow<List<Task>> =
        taskDao.observeAllActive().map { it.toDomainTasks() }

    override suspend fun getActiveTasks(): List<Task> = taskDao.getAllActive().toDomainTasks()

    override fun observeTrash(): Flow<List<Task>> =
        taskDao.observeTrash().map { it.toDomainTasks() }

    override suspend fun getTask(id: String): Task? {
        val row = taskDao.getWithTags(id) ?: return null
        if (row.task.deletedAt != null || row.task.parentTaskId != null) return null
        return row.task.toDomain(
            tags = row.tags.filter { it.deletedAt == null }.map { it.toDomain() },
            subtasks = taskDao.getActiveSubtasks(id).map { it.toSubtask() },
            reminders = row.reminders
                .filter { it.deletedAt == null }
                .sortedWith(compareBy({ it.createdAt }, { it.id }))
                .map { it.toDomain() },
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
            val before = taskDao.getById(task.id)
            saveTask(task)
            replaceTags(task)
            replaceSubtasks(task)
            replaceReminders(task, before)
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

    /**
     * Saves the editable reminders (a pending snooze is left alone). A reminder keeps its
     * "already shown" state and age unless its schedule changed, or the task's due moment
     * changed under a "before due" reminder; in those cases `updatedAt` moves to now, which
     * stops it from being replayed as a catch-up for a time in the past.
     */
    private suspend fun replaceReminders(task: Task, before: TaskEntity?) {
        val now = clock.millis()
        val kept = task.reminders.filter { it.kind != ReminderKind.SNOOZE }
        val existing = reminderDao.getActiveForTask(task.id)
            .filter { it.kind != ReminderKind.SNOOZE.code }
            .associateBy { it.id }
        val dueMomentChanged = before == null ||
            before.dueDate != task.dueDate?.toString() ||
            before.dueTime != task.dueTime?.toString()

        val rows = kept.map { reminder ->
            val old = existing[reminder.id]
            val scheduleChanged = old == null || !old.toDomain().sameSchedule(reminder)
            val touched = scheduleChanged || (reminder.kind == ReminderKind.BEFORE_DUE && dueMomentChanged)
            reminder.toEntity(
                taskId = task.id,
                createdAt = old?.createdAt ?: now,
                updatedAt = if (touched || old == null) now else old.updatedAt,
            ).copy(firedForTrigger = old?.firedForTrigger)
        }

        reminderDao.softDeleteRegularExcept(task.id, kept.map { it.id }, deletedAt = now)
        if (rows.isNotEmpty()) reminderDao.upsertAll(rows)
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
