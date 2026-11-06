package com.elkrrai.feather.data.backup

import com.elkrrai.feather.data.db.CompletionEntity
import com.elkrrai.feather.data.db.HabitEntity
import com.elkrrai.feather.data.db.HabitEntryEntity
import com.elkrrai.feather.data.db.HabitGoalEntity
import com.elkrrai.feather.data.db.HabitGoalStageEntity
import com.elkrrai.feather.data.db.HabitStageEntity
import com.elkrrai.feather.data.db.HabitStageLogEntity
import com.elkrrai.feather.data.db.INBOX_LIST_ID
import com.elkrrai.feather.data.db.ReminderEntity
import com.elkrrai.feather.data.db.TagEntity
import com.elkrrai.feather.data.db.TaskEntity
import com.elkrrai.feather.data.db.TaskTagCrossRef

// Database rows to backup records and back. The two shapes are the same today on purpose: the
// records are the stable file format, the entities are free to change with the schema.

fun TagEntity.toRecord() = TagRecord(id, name, createdAt, updatedAt, deletedAt, parentId)

fun TagRecord.toEntity() = TagEntity(id, name, createdAt, updatedAt, deletedAt, parentId)

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
    counterTarget = counterTarget,
    counterStep = counterStep,
    counterDone = counterDone,
)

/** Every task is kept in the Inbox, whatever list an older file says it was in. */
fun TaskRecord.toEntity() = TaskEntity(
    id = id,
    listId = INBOX_LIST_ID,
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
    counterTarget = counterTarget,
    counterStep = counterStep,
    counterDone = counterDone,
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

fun HabitEntity.toRecord() = HabitRecord(
    id, name, kind, unit, sortOrder, createdAt, updatedAt, deletedAt,
    showInToday, timeMode, startMinute, endMinute, stageMode, todayFromStart, remind, leaf,
)

fun HabitRecord.toEntity() = HabitEntity(
    id, name, kind, unit, sortOrder, createdAt, updatedAt, deletedAt,
    showInToday, timeMode, startMinute, endMinute, stageMode, todayFromStart, remind, leaf,
)

fun HabitGoalEntity.toRecord() = HabitGoalRecord(habitId, effectiveFrom, target, dailyTarget, updatedAt)

fun HabitGoalRecord.toEntity() = HabitGoalEntity(habitId, effectiveFrom, target, dailyTarget, updatedAt)

fun HabitEntryEntity.toRecord() = HabitEntryRecord(habitId, date, amount, updatedAt)

fun HabitEntryRecord.toEntity() = HabitEntryEntity(habitId, date, amount, updatedAt)

fun HabitStageEntity.toRecord() = HabitStageRecord(id, habitId, name, timeMode, startMinute, endMinute, updatedAt)

fun HabitStageRecord.toEntity() = HabitStageEntity(id, habitId, name, timeMode, startMinute, endMinute, updatedAt)

fun HabitGoalStageEntity.toRecord() = HabitGoalStageRecord(habitId, effectiveFrom, stageId, position, updatedAt)

fun HabitGoalStageRecord.toEntity() = HabitGoalStageEntity(habitId, effectiveFrom, stageId, position, updatedAt)

fun HabitStageLogEntity.toRecord() = HabitStageLogRecord(habitId, date, stageId, done, updatedAt)

fun HabitStageLogRecord.toEntity() = HabitStageLogEntity(habitId, date, stageId, done, updatedAt)
