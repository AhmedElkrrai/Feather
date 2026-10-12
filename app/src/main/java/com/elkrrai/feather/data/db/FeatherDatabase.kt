package com.elkrrai.feather.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TaskEntity::class, ListEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class FeatherDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun listDao(): ListDao

    companion object {
        const val NAME = "feather.db"
    }
}
