package com.elkrrai.feather.data.backup

import com.elkrrai.feather.data.db.CompletionEntity
import com.elkrrai.feather.data.db.FolderEntity
import com.elkrrai.feather.data.db.ListEntity
import com.elkrrai.feather.data.db.ReminderEntity
import com.elkrrai.feather.data.db.SavedFilterEntity
import com.elkrrai.feather.data.db.TagEntity
import com.elkrrai.feather.data.db.TaskEntity
import com.elkrrai.feather.data.db.TaskTagCrossRef

// Database rows to backup records and back. The two shapes are the same today on purpose: the
// records are the stable file format, the entities are free to change with the schema.

fun FolderEntity.toRecord() = FolderRecord(id, name, sortOrder, createdAt, updatedAt, deletedAt)

fun FolderRecord.toEntity() = FolderEntity(id, name, sortOrder, createdAt, updatedAt, deletedAt)

fun ListEntity.toRecord() =
    ListRecord(id, name, isInbox, sortOrder, createdAt, updatedAt, deletedAt, folderId, colorArgb)

fun ListRecord.toEntity() = ListEntity(
    id = id,
    name = name,
    isInbox = isInbox,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    folderId = folderId,
    colorArgb = colorArgb,
)

fun TagEntity.toRecord() = TagRecord(id, name, createdAt, updatedAt, deletedAt)

fun TagRecord.toEntity() = TagEntity(id, name, createdAt, updatedAt, deletedAt)

fun TaskEntity.toRecord() = TaskRecord(
    id = id,
    listId = listId,
    title = title,
    notes = notes,
    dueDate = dueDate,
    dueTime = dueTime,
    priority = priority,
    completedAt = completedAt,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    parentTaskId = parentTaskId,
    isPinned = isPinned,
    rrule = rrule,
    repeatMode = repeatMode,
)

fun TaskRecord.toEntity() = TaskEntity(
    id = id,
    listId = listId,
    title = title,
    notes = notes,
    dueDate = dueDate,
    dueTime = dueTime,
    priority = priority,
    completedAt = completedAt,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    parentTaskId = parentTaskId,
    isPinned = isPinned,
    rrule = rrule,
    repeatMode = repeatMode,
)

fun TaskTagCrossRef.toRecord() = TaskTagRecord(taskId, tagId)

fun TaskTagRecord.toEntity() = TaskTagCrossRef(taskId, tagId)

fun ReminderEntity.toRecord() = ReminderRecord(
    id = id,
    taskId = taskId,
    kind = kind,
    offsetMinutes = offsetMinutes,
    atDate = atDate,
    atTime = atTime,
    firedForTrigger = firedForTrigger,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

fun ReminderRecord.toEntity() = ReminderEntity(
    id = id,
    taskId = taskId,
    kind = kind,
    offsetMinutes = offsetMinutes,
    atDate = atDate,
    atTime = atTime,
    firedForTrigger = firedForTrigger,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

fun CompletionEntity.toRecord() = CompletionRecord(id, taskId, completedAt, dueDate)

fun CompletionRecord.toEntity() = CompletionEntity(id, taskId, completedAt, dueDate)

fun SavedFilterEntity.toRecord() = FilterRecord(
    id = id,
    name = name,
    listId = listId,
    tagId = tagId,
    minPriority = minPriority,
    dueWindow = dueWindow,
    hideCompleted = hideCompleted,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

fun FilterRecord.toEntity() = SavedFilterEntity(
    id = id,
    name = name,
    listId = listId,
    tagId = tagId,
    minPriority = minPriority,
    dueWindow = dueWindow,
    hideCompleted = hideCompleted,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)
