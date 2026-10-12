package com.elkrrai.feather.data.db

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/** A task row plus its tags. May include soft-deleted tags; filter on `deletedAt` when mapping. */
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
)
