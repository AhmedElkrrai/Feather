package com.elkrrai.feather.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row each time a task is finished: when ([completedAt], epoch millis) and which due date
 * it was for ([dueDate], ISO-8601, null for a task without one). A repeating task keeps a single
 * row in `tasks` that moves forward, so this table is the only record of its past occurrences
 * (and the base for streaks later). Rows go away with their task.
 */
@Entity(
    tableName = "completion_history",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("taskId")],
)
data class CompletionEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val completedAt: Long,
    val dueDate: String?,
)
