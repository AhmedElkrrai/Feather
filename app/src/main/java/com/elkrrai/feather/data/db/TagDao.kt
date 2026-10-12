package com.elkrrai.feather.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags WHERE deletedAt IS NULL ORDER BY name COLLATE NOCASE")
    fun observeActive(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getById(id: String): TagEntity?

    /** Case-insensitive lookup among non-deleted tags, used to avoid duplicate names. */
    @Query("SELECT * FROM tags WHERE name = :name COLLATE NOCASE AND deletedAt IS NULL LIMIT 1")
    suspend fun findActiveByName(name: String): TagEntity?

    @Upsert
    suspend fun upsert(tag: TagEntity)

    @Query("UPDATE tags SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long)
}
