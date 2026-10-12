package com.elkrrai.feather.data.db

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/**
 * A task row plus its tags and reminders. May include soft-deleted tags and reminders; filter
 * on `deletedAt` when mapping.
 */
data class TaskWithTags(
    @Embedded val task: TaskEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = TaskTagCrossRef::class,
            parentColumn = "taskId",
            entityColumn = "tagId",
        ),
    )
    val tags: List<TagEntity>,
    @Relation(parentColumn = "id", entityColumn = "taskId")
    val reminders: List<ReminderEntity> = emptyList(),
)
