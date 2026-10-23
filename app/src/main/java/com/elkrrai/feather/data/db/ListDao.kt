package com.elkrrai.feather.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy

@Dao
interface ListDao {
    /** Adds the Inbox the first time; does nothing when it is already there. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(list: ListEntity)
}
