package com.elkrrai.feather.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedFilterDao {
    @Query("SELECT * FROM saved_filters WHERE deletedAt IS NULL ORDER BY sortOrder, createdAt")
    fun observeActive(): Flow<List<SavedFilterEntity>>

    @Query("SELECT * FROM saved_filters WHERE id = :id")
    suspend fun getById(id: String): SavedFilterEntity?

    @Upsert
    suspend fun upsert(filter: SavedFilterEntity)

    @Query("UPDATE saved_filters SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long)
}
