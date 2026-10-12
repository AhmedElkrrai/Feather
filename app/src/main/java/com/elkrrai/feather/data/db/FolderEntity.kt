package com.elkrrai.feather.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A one-level container for lists. Timestamps are epoch millis; `deletedAt != null` means soft-deleted. */
@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sortOrder: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)
