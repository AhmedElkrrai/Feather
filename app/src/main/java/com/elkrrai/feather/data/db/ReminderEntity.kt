package com.elkrrai.feather.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One reminder of a task. [kind] is a `ReminderKind` code: 0 = before the due moment by
 * [offsetMinutes]; 1 = at the local moment [atDate] [atTime]; 2 = a snooze (one-shot, same
 * fields as 1, hidden from the editor).
 *
 * [firedForTrigger] is the trigger instant (epoch millis) that was last shown, so a reminder
 * fires once per trigger time. Timestamps are epoch millis; `deletedAt != null` means
 * soft-deleted.
 */
@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("taskId")],
)
data class ReminderEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val kind: Int,
    val offsetMinutes: Int,
    val atDate: String?,
    val atTime: String?,
    val firedForTrigger: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)
