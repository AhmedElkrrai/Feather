package com.elkrrai.feather.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Timestamps are epoch millis. [dueDate] is ISO-8601 (`2026-10-12`) and [dueTime] is `HH:mm`;
 * both are floating local values, never instants. `deletedAt != null` means soft-deleted.
 *
 * A row with a [parentTaskId] is a subtask (checklist item) of that task. Subtasks use only
 * title, completion and ordering; their other fields keep defaults.
 */
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = ListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("listId"), Index("parentTaskId")],
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val listId: String,
    val title: String,
    val notes: String,
    val dueDate: String?,
    val dueTime: String?,
    val priority: Int,
    val completedAt: Long?,
    val sortOrder: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    // Added in schema v2.
    val parentTaskId: String? = null,
    @ColumnInfo(defaultValue = "0") val isPinned: Boolean = false,
)
