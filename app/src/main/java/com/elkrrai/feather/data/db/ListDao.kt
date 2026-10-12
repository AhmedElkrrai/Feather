package com.elkrrai.feather.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ListDao {
    @Query("SELECT * FROM task_lists WHERE deletedAt IS NULL ORDER BY sortOrder, createdAt")
    fun observeActive(): Flow<List<ListEntity>>

    @Query("SELECT * FROM task_lists WHERE id = :id")
    suspend fun getById(id: String): ListEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(list: ListEntity)

    @Upsert
    suspend fun upsert(list: ListEntity)

    @Query("UPDATE task_lists SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long)

    /** Takes every list out of a folder, used when the folder is deleted. */
    @Query("UPDATE task_lists SET folderId = NULL, updatedAt = :updatedAt WHERE folderId = :folderId")
    suspend fun clearFolder(folderId: String, updatedAt: Long)
}
