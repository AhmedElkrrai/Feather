package com.elkrrai.feather.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Timestamps are epoch millis. `deletedAt != null` means soft-deleted.
 * [colorArgb] is an optional list color; [folderId] is the optional containing folder.
 */
@Entity(tableName = "task_lists")
data class ListEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isInbox: Boolean,
    val sortOrder: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    // Added in schema v2.
    val folderId: String? = null,
    val colorArgb: Int? = null,
)
