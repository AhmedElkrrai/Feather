package com.elkrrai.feather.domain.model

/**
 * A list of tasks. The Inbox is a system list: [name] is only a fallback, the UI shows a
 * localized string for it when [isInbox] is true.
 */
data class TaskList(
    val id: String,
    val name: String,
    val isInbox: Boolean,
    val sortOrder: Long,
) {
    companion object {
        /** Well-known id so the Inbox can be created idempotently and merged safely by a future sync. */
        const val INBOX_ID = "00000000-0000-0000-0000-000000000001"
    }
}
