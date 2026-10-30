package com.elkrrai.feather.data.backup

import kotlinx.serialization.Serializable

/**
 * The backup file: everything in the database, as plain records. The format is deliberately
 * separate from the Room classes so the database can change without breaking old backups; every
 * field that was added later has a default, and unknown fields in a newer file are ignored.
 *
 * Soft-deleted rows are included (they carry `deletedAt`), so a deletion made on one phone is
 * not undone by importing an older copy of the task.
 *
 * Older files also carry lists, folders and saved filters; they are ignored (lists, folders and
 * filters left the app), and every task from them goes into the Inbox.
 */
@Serializable
data class BackupFile(
    val format: String = FORMAT,
    val version: Int = VERSION,
    /** When the backup was made, epoch millis. */
    val exportedAt: Long = 0,
    val tags: List<TagRecord> = emptyList(),
    val tasks: List<TaskRecord> = emptyList(),
    val taskTags: List<TaskTagRecord> = emptyList(),
    val reminders: List<ReminderRecord> = emptyList(),
    val completions: List<CompletionRecord> = emptyList(),
    /** Habits with their weekly goals and logged days; absent in older backup files. */
    val habits: List<HabitRecord> = emptyList(),
    val habitGoals: List<HabitGoalRecord> = emptyList(),
    val habitEntries: List<HabitEntryRecord> = emptyList(),
) {
    companion object {
        const val FORMAT = "feather-backup"

        /** The newest file version this app can read. */
        const val VERSION = 1
    }
}

@Serializable
data class TagRecord(
    val id: String,
    val name: String,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val deletedAt: Long? = null,
    /** The tag this one extends; absent in older backup files, which means a top-level tag. */
    val parentId: String? = null,
)

/** A task, or (when [parentTaskId] is set) a checklist item of one. Dates are ISO text. */
@Serializable
data class TaskRecord(
    val id: String,
    /** Always the Inbox now; older files name the list the task was in. */
    val listId: String = "",
    val title: String,
    val notes: String = "",
    val dueDate: String? = null,
    val dueTime: String? = null,
    val priority: Int = 0,
    val completedAt: Long? = null,
    val sortOrder: Long = 0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val deletedAt: Long? = null,
    val parentTaskId: String? = null,
    val isPinned: Boolean = false,
    val rrule: String? = null,
    val repeatMode: Int = 0,
    /** A counter task's target, step and count; absent in older backup files. */
    val counterTarget: Int? = null,
    val counterStep: Int? = null,
    val counterDone: Int = 0,
)

@Serializable
data class TaskTagRecord(
    val taskId: String,
    val tagId: String,
)

@Serializable
data class ReminderRecord(
    val id: String,
    val taskId: String,
    val kind: Int,
    val offsetMinutes: Int = 0,
    val atDate: String? = null,
    val atTime: String? = null,
    val firedForTrigger: Long? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val deletedAt: Long? = null,
)

@Serializable
data class CompletionRecord(
    val id: String,
    val taskId: String,
    val completedAt: Long,
    val dueDate: String? = null,
)

@Serializable
data class HabitRecord(
    val id: String,
    val name: String,
    val kind: Int = 0,
    val unit: String = "",
    val sortOrder: Long = 0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val deletedAt: Long? = null,
)

@Serializable
data class HabitGoalRecord(
    val habitId: String,
    val effectiveFrom: String,
    val target: Int,
    val dailyTarget: Int = 1,
    val updatedAt: Long = 0,
)

@Serializable
data class HabitEntryRecord(
    val habitId: String,
    val date: String,
    val amount: Int,
    val updatedAt: Long = 0,
)
