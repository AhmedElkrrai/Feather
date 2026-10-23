package com.elkrrai.feather.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** The id of the one list every task is kept in. */
const val INBOX_LIST_ID = "00000000-0000-0000-0000-000000000001"

/**
 * Tasks have no lists in the app any more, but the table stays: `tasks.listId` is a foreign key to
 * it, and SQLite can only drop a foreign key by rebuilding the `tasks` table, which would also wipe
 * everything that hangs on tasks (tags, reminders, history) if it went wrong. So every task is kept
 * in one hidden list, the Inbox, and the columns for lists of your own ([folderId], [colorArgb]) are
 * unused. Lists made before the change stay as unused rows. Timestamps are epoch millis.
 */
@Entity(tableName = "task_lists")
data class ListEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isInbox: Boolean,
    val sortOrder: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    // Added in schema v2.
    val folderId: String? = null,
    val colorArgb: Int? = null,
)
