package com.elkrrai.feather.data.repository

import com.elkrrai.feather.data.db.FolderDao
import com.elkrrai.feather.data.db.FolderEntity
import com.elkrrai.feather.data.db.ListDao
import com.elkrrai.feather.data.db.ListEntity
import com.elkrrai.feather.data.db.TaskDao
import com.elkrrai.feather.data.db.TransactionRunner
import com.elkrrai.feather.data.db.toDomain
import com.elkrrai.feather.domain.model.Folder
import com.elkrrai.feather.domain.model.TaskList
import com.elkrrai.feather.domain.repository.ListRepository
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ListRepositoryImpl @Inject constructor(
    private val listDao: ListDao,
    private val folderDao: FolderDao,
    private val taskDao: TaskDao,
    private val tx: TransactionRunner,
    private val clock: Clock,
) : ListRepository {

    override fun observeLists(): Flow<List<TaskList>> =
        listDao.observeActive().map { rows -> rows.map { it.toDomain() } }

    override fun observeFolders(): Flow<List<Folder>> =
        folderDao.observeActive().map { rows -> rows.map { it.toDomain() } }

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

    override suspend fun createList(name: String, folderId: String?, colorArgb: Int?): TaskList {
        require(name.isNotBlank()) { "List name must not be blank" }
        val now = clock.millis()
        val entity = ListEntity(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            isInbox = false,
            sortOrder = now,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
            folderId = folderId,
            colorArgb = colorArgb,
        )
        listDao.upsert(entity)
        return entity.toDomain()
    }

    override suspend fun updateList(id: String, name: String, colorArgb: Int?, folderId: String?) {
        val existing = listDao.getById(id)?.takeIf { it.deletedAt == null } ?: return
        listDao.upsert(
            existing.copy(
                name = if (existing.isInbox) existing.name else name.trim().ifBlank { existing.name },
                colorArgb = colorArgb,
                folderId = if (existing.isInbox) null else folderId,
                updatedAt = clock.millis(),
            )
        )
    }

    override suspend fun deleteList(id: String) {
        if (id == TaskList.INBOX_ID) return
        val now = clock.millis()
        tx.run {
            taskDao.moveAllToList(fromListId = id, toListId = TaskList.INBOX_ID, updatedAt = now)
            listDao.softDelete(id, deletedAt = now)
        }
    }

    override suspend fun createFolder(name: String): Folder {
        require(name.isNotBlank()) { "Folder name must not be blank" }
        val now = clock.millis()
        val entity = FolderEntity(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            sortOrder = now,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
        )
        folderDao.upsert(entity)
        return entity.toDomain()
    }

    override suspend fun renameFolder(id: String, name: String) {
        val existing = folderDao.getById(id)?.takeIf { it.deletedAt == null } ?: return
        val newName = name.trim()
        if (newName.isEmpty()) return
        folderDao.upsert(existing.copy(name = newName, updatedAt = clock.millis()))
    }

    override suspend fun deleteFolder(id: String) {
        val now = clock.millis()
        tx.run {
            listDao.clearFolder(folderId = id, updatedAt = now)
            folderDao.softDelete(id, deletedAt = now)
        }
    }

    private companion object {
        // The UI shows a localized string for the Inbox; this is only a fallback.
        const val INBOX_FALLBACK_NAME = "Inbox"
    }
}
