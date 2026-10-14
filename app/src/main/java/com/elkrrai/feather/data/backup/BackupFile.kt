package com.elkrrai.feather.data.backup

import kotlinx.serialization.Serializable

/**
 * The backup file: everything in the database, as plain records. The format is deliberately
 * separate from the Room classes so the database can change without breaking old backups; every
 * field that was added later has a default, and unknown fields in a newer file are ignored.
 *
 * Soft-deleted rows are included (they carry `deletedAt`), so a deletion made on one phone is
 * not undone by importing an older copy of the task.
 */
@Serializable
data class BackupFile(
    val format: String = FORMAT,
    val version: Int = VERSION,
    /** When the backup was made, epoch millis. */
    val exportedAt: Long = 0,
    val folders: List<FolderRecord> = emptyList(),
    val lists: List<ListRecord> = emptyList(),
    val tags: List<TagRecord> = emptyList(),
    val tasks: List<TaskRecord> = emptyList(),
    val taskTags: List<TaskTagRecord> = emptyList(),
    val reminders: List<ReminderRecord> = emptyList(),
    val completions: List<CompletionRecord> = emptyList(),
) {
    companion object {
        const val FORMAT = "feather-backup"

        /** The newest file version this app can read. */
        const val VERSION = 1
    }
}

@Serializable
data class FolderRecord(
    val id: String,
    val name: String,
    val sortOrder: Long = 0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val deletedAt: Long? = null,
)

@Serializable
data class ListRecord(
    val id: String,
    val name: String,
    val isInbox: Boolean = false,
    val sortOrder: Long = 0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val deletedAt: Long? = null,
    val folderId: String? = null,
    val colorArgb: Int? = null,
)

@Serializable
data class TagRecord(
    val id: String,
    val name: String,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val deletedAt: Long? = null,
)

/** A task, or (when [parentTaskId] is set) a checklist item of one. Dates are ISO text. */
@Serializable
data class TaskRecord(
    val id: String,
    val listId: String,
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
