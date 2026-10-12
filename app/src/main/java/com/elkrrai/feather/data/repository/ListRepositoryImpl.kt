package com.elkrrai.feather.data.repository

import com.elkrrai.feather.data.db.ListDao
import com.elkrrai.feather.data.db.ListEntity
import com.elkrrai.feather.data.db.toDomain
import com.elkrrai.feather.domain.model.TaskList
import com.elkrrai.feather.domain.repository.ListRepository
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ListRepositoryImpl @Inject constructor(
    private val listDao: ListDao,
    private val clock: Clock,
) : ListRepository {

    override fun observeLists(): Flow<List<TaskList>> =
        listDao.observeActive().map { rows -> rows.map { it.toDomain() } }

    override suspend fun ensureInbox() {
        val now = clock.millis()
        listDao.insertIgnore(
            ListEntity(
                id = TaskList.INBOX_ID,
                name = INBOX_FALLBACK_NAME,
                isInbox = true,
                sortOrder = 0,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
            )
        )
    }

    private companion object {
        // The UI shows a localized string for the Inbox; this is only a fallback.
        const val INBOX_FALLBACK_NAME = "Inbox"
    }
}
