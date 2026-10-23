package com.elkrrai.feather.data.repository

import com.elkrrai.feather.data.db.INBOX_LIST_ID
import com.elkrrai.feather.data.db.ListDao
import com.elkrrai.feather.data.db.ListEntity
import com.elkrrai.feather.domain.repository.InboxRepository
import java.time.Clock
import javax.inject.Inject

class InboxRepositoryImpl @Inject constructor(
    private val listDao: ListDao,
    private val clock: Clock,
) : InboxRepository {

    override suspend fun ensureInbox() {
        val now = clock.millis()
        listDao.insertIgnore(
            ListEntity(
                id = INBOX_LIST_ID,
                name = "Inbox",
                isInbox = true,
                sortOrder = 0,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
            )
        )
    }
}
