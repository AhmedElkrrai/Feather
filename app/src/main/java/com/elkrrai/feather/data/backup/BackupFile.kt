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
    /** The named times of habits, which of them belong to a week's goal, and which were done on a day; absent before version 2. */
    val habitStages: List<HabitStageRecord> = emptyList(),
    val habitGoalStages: List<HabitGoalStageRecord> = emptyList(),
    val habitStageLog: List<HabitStageLogRecord> = emptyList(),
    /** What was logged for habits that count an amount, one record per log; absent before version 3 (the day's amount in [habitEntries] then stands for one log). */
    val habitLogs: List<HabitLogRecord> = emptyList(),
    /** The preferences, as text by name (see `BackupPreferences`); absent in older files. */
    val settings: Map<String, String> = emptyMap(),
    /** The app language as a language tag, empty for the phone's own; null in older files. */
    val language: String? = null,
) {
    companion object {
        const val FORMAT = "feather-backup"

        /**
         * The newest file version this app can read. Version 2 added the stages of habits and version 3 the logs of amount habits: an app that
         * knows only an older one refuses such a file rather than drop them.
         */
        const val VERSION = 3
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
    /** From version 2: the times and the Today settings. */
    val showInToday: Boolean = false,
    val timeMode: Int = 0,
    val startMinute: Int? = null,
    val endMinute: Int? = null,
    val stageMode: Int = 0,
    val todayFromStart: Boolean = false,
    val remind: Boolean = false,
    /** The look of the weekly leaf; negative when the backup has none (an older one), and the habit is dealt one when listed. */
    val leaf: Int = -1,
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

@Serializable
data class HabitLogRecord(
    val id: String,
    val habitId: String,
    val date: String,
    val amount: Int,
    val loggedAt: Long? = null,
    val updatedAt: Long = 0,
)

@Serializable
data class HabitStageRecord(
    val id: String,
    val habitId: String,
    val name: String,
    val timeMode: Int = 0,
    val startMinute: Int? = null,
    val endMinute: Int? = null,
    val updatedAt: Long = 0,
)

@Serializable
data class HabitGoalStageRecord(
    val habitId: String,
    val effectiveFrom: String,
    val stageId: String,
    val position: Int = 0,
    val updatedAt: Long = 0,
)

@Serializable
data class HabitStageLogRecord(
    val habitId: String,
    val date: String,
    val stageId: String,
    val done: Boolean = true,
    val updatedAt: Long = 0,
)
