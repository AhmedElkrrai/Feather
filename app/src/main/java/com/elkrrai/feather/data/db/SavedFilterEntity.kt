package com.elkrrai.feather.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A saved view (see `SavedFilter`). [listId] and [tagId] are not foreign keys on purpose: a
 * filter outlives a deleted list or tag and then simply matches nothing. [minPriority] is a
 * priority level and [dueWindow] a `DueWindow` code. Timestamps are epoch millis;
 * `deletedAt != null` means soft-deleted.
 */
@Entity(tableName = "saved_filters")
data class SavedFilterEntity(
    @PrimaryKey val id: String,
    val name: String,
    val listId: String?,
    val tagId: String?,
    val minPriority: Int,
    val dueWindow: Int,
    val hideCompleted: Boolean,
    val sortOrder: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)
