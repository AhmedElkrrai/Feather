package com.elkrrai.feather.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ListDao {
    @Query("SELECT * FROM task_lists WHERE deletedAt IS NULL ORDER BY sortOrder, createdAt")
    fun observeActive(): Flow<List<ListEntity>>

    @Query("SELECT * FROM task_lists WHERE id = :id")
    suspend fun getById(id: String): ListEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(list: ListEntity)
}
