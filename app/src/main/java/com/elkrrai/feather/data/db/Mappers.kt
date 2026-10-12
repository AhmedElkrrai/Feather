package com.elkrrai.feather.data.db

import com.elkrrai.feather.domain.model.Priority
import com.elkrrai.feather.domain.model.Task
import com.elkrrai.feather.domain.model.TaskList
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

fun TaskEntity.toDomain(): Task = Task(
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
)

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
)

fun ListEntity.toDomain(): TaskList = TaskList(
    id = id,
    name = name,
    isInbox = isInbox,
    sortOrder = sortOrder,
)
