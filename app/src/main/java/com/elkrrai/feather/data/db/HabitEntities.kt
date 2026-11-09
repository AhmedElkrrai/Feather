package com.elkrrai.feather.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A habit. [kind] is a `HabitKind` code, [timeMode] a `TimeMode` code and [stageMode] a `StageMode` code. Times are minutes since
 * midnight. Timestamps are epoch millis; `deletedAt != null` means deleted. The columns after `deletedAt` came with version 2 and
 * have defaults, so the habits of version 1 are habits without stages that do not show in Today.
 */
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
    @ColumnInfo(defaultValue = "0") val showInToday: Boolean = false,
    @ColumnInfo(defaultValue = "0") val timeMode: Int = 0,
    val startMinute: Int? = null,
    val endMinute: Int? = null,
    @ColumnInfo(defaultValue = "0") val stageMode: Int = 0,
    @ColumnInfo(defaultValue = "0") val todayFromStart: Boolean = false,
    @ColumnInfo(defaultValue = "0") val remind: Boolean = false,
    /** From version 3: the look of the weekly leaf (see `nextLeaf`); negative until it is dealt, which the habits of version 2 are. */
    @ColumnInfo(defaultValue = "-1") val leaf: Int = -1,
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

/** A named time of the day of a habit (Fajr of "Pray"). The row stays for life, so what was logged for it keeps its meaning. */
@Entity(
    tableName = "habit_stages",
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
data class HabitStageEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val name: String,
    @ColumnInfo(defaultValue = "0") val timeMode: Int = 0,
    val startMinute: Int? = null,
    val endMinute: Int? = null,
    val updatedAt: Long,
)

/**
 * Which stages belong to the goal that takes effect on [effectiveFrom] (ISO date), and in what order. A change of the list is a new set of rows for
 * the next week, like a new goal; the weeks before keep the list they had.
 */
@Entity(
    tableName = "habit_goal_stages",
    primaryKeys = ["habitId", "effectiveFrom", "stageId"],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = HabitStageEntity::class,
            parentColumns = ["id"],
            childColumns = ["stageId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("habitId"), Index("stageId")],
)
data class HabitGoalStageEntity(
    val habitId: String,
    val effectiveFrom: String,
    val stageId: String,
    val position: Int,
    val updatedAt: Long,
)

/** Whether a stage was done on [date] (ISO date). Undoing keeps the row with `done = false`, so a backup can tell it from "never logged". */
@Entity(
    tableName = "habit_stage_log",
    primaryKeys = ["habitId", "date", "stageId"],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = HabitStageEntity::class,
            parentColumns = ["id"],
            childColumns = ["stageId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("habitId"), Index("stageId")],
)
data class HabitStageLogEntity(
    val habitId: String,
    val date: String,
    val stageId: String,
    val done: Boolean,
    val updatedAt: Long,
)

/**
 * One thing logged for a habit that counts an amount: "10 pull ups at 9:41". The day's total, which the week and the tree go by, is still
 * `habit_entries.amount`; it is kept equal to the sum of the day's logs. [loggedAt] is null for an amount from before logs existed and for a log
 * added to an earlier day.
 */
@Entity(
    tableName = "habit_logs",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("habitId", "date")],
)
data class HabitLogEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val date: String,
    val amount: Int,
    val loggedAt: Long?,
    val updatedAt: Long,
)

/** The sum of the logs of one day. */
data class HabitDayTotal(val habitId: String, val date: String, val total: Int)
