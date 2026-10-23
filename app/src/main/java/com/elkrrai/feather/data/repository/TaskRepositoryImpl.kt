package com.elkrrai.feather.data.repository

import com.elkrrai.feather.data.db.CompletionDao
import com.elkrrai.feather.data.db.CompletionEntity
import com.elkrrai.feather.data.db.INBOX_LIST_ID
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
import com.elkrrai.feather.domain.model.Subtask
import com.elkrrai.feather.domain.model.Task
import com.elkrrai.feather.domain.recurrence.Recurrence
import com.elkrrai.feather.domain.recurrence.RepeatMode
import com.elkrrai.feather.domain.recurrence.afterAdvance
import com.elkrrai.feather.domain.recurrence.nextOccurrence
import com.elkrrai.feather.domain.repository.TaskRepository
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val reminderDao: ReminderDao,
    private val completionDao: CompletionDao,
    private val tx: TransactionRunner,
    private val clock: Clock,
) : TaskRepository {

    override fun observeTasks(): Flow<List<Task>> =
        combine(taskDao.observeAllActive(), completionDao.observeLatest()) { rows, latest ->
            val lastDone = latest.associate { it.taskId to Instant.ofEpochMilli(it.completedAt) }
            rows.toDomainTasks().map { task -> lastDone[task.id]?.let { task.copy(lastCompletedAt = it) } ?: task }
        }

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

    override suspend fun addTask(title: String, dueDate: LocalDate?): Task {
        require(title.isNotBlank()) { "Task title must not be blank" }
        val now = Instant.now(clock)
        val task = Task(
            id = UUID.randomUUID().toString(),
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
                listId = INBOX_LIST_ID,
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

    /**
     * Finishing a task records it in the completion history. A repeating task does not stay
     * finished: it moves to its next date (see [advance]), unless that was its last occurrence.
     * Reopening a finished task takes its history row back.
     */
    override suspend fun setCompleted(id: String, completed: Boolean) {
        tx.run {
            taskDao.getById(id)?.let { task ->
                if (completed) complete(task) else reopen(task)
            }
        }
    }

    private suspend fun reopen(task: TaskEntity) {
        val finishedAt = task.completedAt
        if (finishedAt == null) {
            undoAdvance(task)
            return
        }
        completionDao.deleteForTaskAt(task.id, finishedAt)
        taskDao.setCompletedAt(task.id, completedAt = null, updatedAt = clock.millis())
    }

    /**
     * Takes back the last finish of a repeating task that moved on to its next date: the due
     * date goes back to the one it was finished for, and the rule gets its occurrence back.
     */
    private suspend fun undoAdvance(task: TaskEntity) {
        val recurrence = Recurrence.parse(task.rrule, RepeatMode.fromCode(task.repeatMode)) ?: return
        val last = completionDao.getForTask(task.id).firstOrNull() ?: return
        val dueBefore = last.dueDate ?: return
        // Only today's finish can be taken back.
        val today = LocalDate.now(clock.withZone(ZoneId.systemDefault()))
        if (Instant.ofEpochMilli(last.completedAt).atZone(ZoneId.systemDefault()).toLocalDate() != today) return
        val restored = recurrence.copy(count = recurrence.count?.plus(1))
        taskDao.upsert(task.copy(dueDate = dueBefore, rrule = restored.toRule(), updatedAt = clock.millis()))
        completionDao.deleteForTaskAt(task.id, last.completedAt)
    }

    private suspend fun complete(task: TaskEntity) {
        if (task.completedAt != null) return
        val now = clock.millis()
        completionDao.insert(
            CompletionEntity(
                id = UUID.randomUUID().toString(),
                taskId = task.id,
                completedAt = now,
                dueDate = task.dueDate,
            )
        )

        val due = task.dueDate?.let(LocalDate::parse)
        val recurrence = Recurrence.parse(task.rrule, RepeatMode.fromCode(task.repeatMode))
        // The Clock is UTC; the day a task was finished on is the user's own day.
        val completedOn = LocalDate.now(clock.withZone(ZoneId.systemDefault()))
        val next = if (due != null && recurrence != null) recurrence.nextOccurrence(due, completedOn) else null

        if (due == null || recurrence == null || next == null) {
            taskDao.setCompletedAt(task.id, completedAt = now, updatedAt = now)
            // A finished task has a finished checklist.
            taskDao.completeSubtasks(task.id, completedAt = now)
        } else {
            advance(task, recurrence, due, next, now)
        }
    }

    /**
     * Moves a repeating task to [next]: the rule loses one occurrence, the checklist is
     * unchecked, and the reminders move with the date and may fire again.
     */
    private suspend fun advance(task: TaskEntity, recurrence: Recurrence, due: LocalDate, next: LocalDate, now: Long) {
        taskDao.upsert(
            task.copy(dueDate = next.toString(), rrule = recurrence.afterAdvance().toRule(), updatedAt = now)
        )
        taskDao.reopenSubtasks(task.id, updatedAt = now)

        // A pending snooze belonged to the occurrence that was just finished.
        reminderDao.softDeleteSnoozes(task.id, deletedAt = now)
        val days = ChronoUnit.DAYS.between(due, next)
        val moved = reminderDao.getActiveForTask(task.id)
            .filter { it.kind != ReminderKind.SNOOZE.code }
            .map { row ->
                val atDate = if (row.kind == ReminderKind.AT_TIME.code) {
                    row.atDate?.let { LocalDate.parse(it).plusDays(days).toString() }
                } else {
                    row.atDate
                }
                // A new updatedAt keeps a reminder whose time is already past from being replayed.
                row.copy(atDate = atDate, firedForTrigger = null, updatedAt = now)
            }
        if (moved.isNotEmpty()) reminderDao.upsertAll(moved)
    }

    override suspend fun duplicateTask(id: String, title: String): Task? {
        val source = getTask(id) ?: return null
        val copyId = tx.run {
            val created = addTask(title, source.dueDate)
            saveTaskDetails(
                created.copy(
                    notes = source.notes,
                    dueTime = source.dueTime,
                    priority = source.priority,
                    isPinned = source.isPinned,
                    tags = source.tags,
                    subtasks = source.subtasks.map { Subtask(UUID.randomUUID().toString(), it.title) },
                    reminders = source.reminders
                        .filter { it.kind != ReminderKind.SNOOZE }
                        .map { it.copy(id = UUID.randomUUID().toString(), firedForTrigger = null) },
                    recurrence = source.recurrence,
                )
            )
            created.id
        }
        return getTask(copyId)
    }

    override suspend fun reorderTasks(orderedIds: List<String>) {
        tx.run {
            val rows = orderedIds.distinct().mapNotNull { taskDao.getById(it) }
            // The slots are the tasks' own sort values, strictly increasing so two tasks never tie.
            val slots = rows.map { it.sortOrder }.sorted().toMutableList()
            for (i in 1 until slots.size) if (slots[i] <= slots[i - 1]) slots[i] = slots[i - 1] + 1
            val now = clock.millis()
            rows.forEachIndexed { index, row ->
                if (row.sortOrder != slots[index]) taskDao.setSortOrder(row.id, slots[index], now)
            }
        }
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
