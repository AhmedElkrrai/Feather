package com.elkrrai.feather.testutil

import com.elkrrai.feather.domain.model.Task
import com.elkrrai.feather.domain.repository.TaskRepository
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory [TaskRepository] with the same soft-delete rules as the real one. */
class FakeTaskRepository : TaskRepository {

    private data class Row(val task: Task, val deletedAt: Long? = null)

    private val rows = MutableStateFlow<List<Row>>(emptyList())
    private var counter = 0L

    override fun observeTasks(): Flow<List<Task>> = rows.map { all ->
        all.filter { it.deletedAt == null }.map { it.task }.sortedBy { it.sortOrder }
    }

    override suspend fun getActiveTasks(): List<Task> =
        rows.value.filter { it.deletedAt == null }.map { it.task }.sortedBy { it.sortOrder }

    override fun observeTrash(): Flow<List<Task>> = rows.map { all ->
        all.filter { it.deletedAt != null }.sortedByDescending { it.deletedAt }.map { it.task }
    }

    override suspend fun getTask(id: String): Task? =
        rows.value.firstOrNull { it.task.id == id && it.deletedAt == null }?.task

    override suspend fun addTask(title: String, dueDate: LocalDate?): Task {
        require(title.isNotBlank())
        counter++
        val now = Instant.ofEpochSecond(counter)
        val task = Task(
            id = "task-$counter",
            title = title.trim(),
            dueDate = dueDate,
            sortOrder = counter,
            createdAt = now,
            updatedAt = now,
        )
        rows.update { it + Row(task) }
        return task
    }

    override suspend fun saveTask(task: Task) = upsert(task)

    override suspend fun saveTaskDetails(task: Task) = upsert(task)

    private fun upsert(task: Task) {
        rows.update { all ->
            val index = all.indexOfFirst { it.task.id == task.id }
            if (index == -1) {
                all + Row(task)
            } else {
                all.toMutableList().also { it[index] = it[index].copy(task = task) }
            }
        }
    }

    override suspend fun setCompleted(id: String, completed: Boolean) = change(id) { row ->
        row.copy(task = row.task.copy(completedAt = if (completed) Instant.ofEpochSecond(1_000) else null))
    }

    override suspend fun addToCounter(id: String, delta: Int) = change(id) { row ->
        val counter = row.task.counter ?: return@change row
        setCount(row, counter.plus(delta))
    }

    override suspend fun setCounterDone(id: String, done: Int) = change(id) { row ->
        val counter = row.task.counter ?: return@change row
        setCount(row, counter.copy(done = done.coerceIn(0, counter.target)))
    }

    private fun setCount(row: Row, counter: com.elkrrai.feather.domain.model.Counter): Row = row.copy(
        task = row.task.copy(
            counter = counter,
            completedAt = if (counter.isFull) row.task.completedAt ?: Instant.ofEpochSecond(1_000) else null,
        ),
    )

    override suspend fun duplicateTask(id: String, title: String): Task? {
        val source = getTask(id) ?: return null
        val copy = addTask(title, source.dueDate)
        val detailed = copy.copy(
            notes = source.notes,
            dueTime = source.dueTime,
            priority = source.priority,
            isPinned = source.isPinned,
            tags = source.tags,
            subtasks = source.subtasks.map { it.copy(id = "copy-${it.id}", isCompleted = false) },
            recurrence = source.recurrence,
        )
        upsert(detailed)
        return detailed
    }

    override suspend fun reorderTasks(orderedIds: List<String>) {
        val slots = orderedIds.mapNotNull { id -> rows.value.firstOrNull { it.task.id == id } }
            .map { it.task.sortOrder }
            .sorted()
        orderedIds.forEachIndexed { index, id ->
            if (index < slots.size) change(id) { row -> row.copy(task = row.task.copy(sortOrder = slots[index])) }
        }
    }

    override suspend fun setPinned(id: String, pinned: Boolean) = change(id) { row ->
        row.copy(task = row.task.copy(isPinned = pinned))
    }

    override suspend fun deleteTask(id: String) = change(id) { it.copy(deletedAt = 1L) }

    override suspend fun restoreTask(id: String) = change(id) { it.copy(deletedAt = null) }

    override suspend fun deleteForever(id: String) {
        rows.update { all -> all.filterNot { it.task.id == id && it.deletedAt != null } }
    }

    override suspend fun emptyTrash() {
        rows.update { all -> all.filter { it.deletedAt == null } }
    }

    override suspend fun purgeTrash(retention: Duration) = Unit

    private fun change(id: String, transform: (Row) -> Row) {
        rows.update { all -> all.map { if (it.task.id == id) transform(it) else it } }
    }
}
