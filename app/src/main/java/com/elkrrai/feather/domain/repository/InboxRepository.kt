package com.elkrrai.feather.domain.repository

/**
 * Tasks have no lists any more, but the database still keeps every task in one hidden list, the
 * Inbox (see the note on `ListEntity`), and that list has to exist before a task can be saved.
 */
interface InboxRepository {
    /** Creates the Inbox if it does not exist yet. Safe to call any number of times. */
    suspend fun ensureInbox()
}
