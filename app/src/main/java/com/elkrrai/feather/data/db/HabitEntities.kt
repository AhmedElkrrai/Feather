package com.elkrrai.feather.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A habit. [kind] is a `HabitKind` code. Timestamps are epoch millis; `deletedAt != null` means deleted. */
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val kind: Int,
    val unit: String,
    val sortOrder: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)

/** The goal of a habit from the week that starts on [effectiveFrom] (ISO date) on. */
@Entity(
    tableName = "habit_goals",
    primaryKeys = ["habitId", "effectiveFrom"],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("habitId")],
)
data class HabitGoalEntity(
    val habitId: String,
    val effectiveFrom: String,
    val target: Int,
    val dailyTarget: Int,
    val updatedAt: Long,
)

/** How much of a habit was logged on [date] (ISO date). A day with nothing logged keeps its row at 0. */
@Entity(
    tableName = "habit_entries",
    primaryKeys = ["habitId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("habitId")],
)
data class HabitEntryEntity(
    val habitId: String,
    val date: String,
    val amount: Int,
    val updatedAt: Long,
)
