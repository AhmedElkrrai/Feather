package com.elkrrai.feather.data.repository

import com.elkrrai.feather.data.db.SavedFilterDao
import com.elkrrai.feather.data.db.SavedFilterEntity
import com.elkrrai.feather.data.db.toDomain
import com.elkrrai.feather.domain.model.DueWindow
import com.elkrrai.feather.domain.model.Priority
import com.elkrrai.feather.domain.model.SavedFilter
import com.elkrrai.feather.domain.repository.SavedFilterRepository
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SavedFilterRepositoryImpl @Inject constructor(
    private val dao: SavedFilterDao,
    private val clock: Clock,
) : SavedFilterRepository {

    override fun observeFilters(): Flow<List<SavedFilter>> =
        dao.observeActive().map { rows -> rows.map { it.toDomain() } }

    override suspend fun createFilter(
        name: String,
        listId: String?,
        tagId: String?,
        minPriority: Priority,
        due: DueWindow,
        hideCompleted: Boolean,
    ): SavedFilter {
        require(name.isNotBlank()) { "Filter name must not be blank" }
        val now = clock.millis()
        val entity = SavedFilterEntity(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            listId = listId,
            tagId = tagId,
            minPriority = minPriority.level,
            dueWindow = due.code,
            hideCompleted = hideCompleted,
            sortOrder = now,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
        )
        dao.upsert(entity)
        return entity.toDomain()
    }

    override suspend fun updateFilter(filter: SavedFilter) {
        val existing = dao.getById(filter.id) ?: return
        if (existing.deletedAt != null || filter.name.isBlank()) return
        dao.upsert(
            existing.copy(
                name = filter.name.trim(),
                listId = filter.listId,
                tagId = filter.tagId,
                minPriority = filter.minPriority.level,
                dueWindow = filter.due.code,
                hideCompleted = filter.hideCompleted,
                updatedAt = clock.millis(),
            )
        )
    }

    override suspend fun deleteFilter(id: String) {
        dao.softDelete(id, deletedAt = clock.millis())
    }
}
