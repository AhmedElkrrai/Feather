package com.elkrrai.feather.data.db

import com.elkrrai.feather.domain.model.Folder
import com.elkrrai.feather.domain.model.Priority
import com.elkrrai.feather.domain.model.Subtask
import com.elkrrai.feather.domain.model.Tag
import com.elkrrai.feather.domain.model.Task
import com.elkrrai.feather.domain.model.TaskList
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

fun TaskEntity.toDomain(
    tags: List<Tag> = emptyList(),
    subtasks: List<Subtask> = emptyList(),
): Task = Task(
    id = id,
    listId = listId,
    title = title,
    notes = notes,
    dueDate = dueDate?.let(LocalDate::parse),
    dueTime = dueTime?.let(LocalTime::parse),
    priority = Priority.fromLevel(priority),
    completedAt = completedAt?.let(Instant::ofEpochMilli),
    sortOrder = sortOrder,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
    isPinned = isPinned,
    tags = tags,
    subtasks = subtasks,
    deletedAt = deletedAt?.let(Instant::ofEpochMilli),
)

/** Tags and subtasks live in their own rows and are written separately. */
fun Task.toEntity(deletedAt: Long? = null): TaskEntity = TaskEntity(
    id = id,
    listId = listId,
    title = title,
    notes = notes,
    dueDate = dueDate?.toString(),
    dueTime = dueTime?.toString(),
    priority = priority.level,
    completedAt = completedAt?.toEpochMilli(),
    sortOrder = sortOrder,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
    deletedAt = deletedAt,
    parentTaskId = null,
    isPinned = isPinned,
)

fun TaskEntity.toSubtask(): Subtask = Subtask(
    id = id,
    title = title,
    isCompleted = completedAt != null,
)

fun TagEntity.toDomain(): Tag = Tag(id = id, name = name)

fun FolderEntity.toDomain(): Folder = Folder(id = id, name = name, sortOrder = sortOrder)

fun ListEntity.toDomain(): TaskList = TaskList(
    id = id,
    name = name,
    isInbox = isInbox,
    sortOrder = sortOrder,
    folderId = folderId,
    colorArgb = colorArgb,
)

/**
 * Turns the flat rows (top-level tasks and subtasks together) into tasks that carry their tags
 * and subtasks. Keeps the order of the top-level rows; soft-deleted tags are dropped.
 */
fun List<TaskWithTags>.toDomainTasks(): List<Task> {
    val subtasksByParent = this
        .filter { it.task.parentTaskId != null }
        .groupBy({ it.task.parentTaskId!! }, { it.task })
    return this
        .filter { it.task.parentTaskId == null }
        .map { row ->
            val subtasks = subtasksByParent[row.task.id].orEmpty()
                .sortedWith(compareBy({ it.sortOrder }, { it.createdAt }))
                .map(TaskEntity::toSubtask)
            val tags = row.tags
                .filter { it.deletedAt == null }
                .sortedBy { it.name.lowercase() }
                .map(TagEntity::toDomain)
            row.task.toDomain(tags = tags, subtasks = subtasks)
        }
}
