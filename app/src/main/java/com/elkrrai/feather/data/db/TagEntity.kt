package com.elkrrai.feather.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Timestamps are epoch millis; `deletedAt != null` means soft-deleted. [parentId] is the tag this
 * one extends (null for a top-level tag).
 */
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val parentId: String? = null,
)
