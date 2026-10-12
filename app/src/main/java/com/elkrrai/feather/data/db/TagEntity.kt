package com.elkrrai.feather.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Timestamps are epoch millis; `deletedAt != null` means soft-deleted. */
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)
