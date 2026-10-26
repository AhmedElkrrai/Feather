package com.elkrrai.feather.data.repository

import com.elkrrai.feather.data.db.TagDao
import com.elkrrai.feather.data.db.TagEntity
import com.elkrrai.feather.data.db.toDomain
import com.elkrrai.feather.domain.model.Tag
import com.elkrrai.feather.domain.model.asTagName
import com.elkrrai.feather.domain.model.wouldLoop
import com.elkrrai.feather.domain.repository.TagRepository
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TagRepositoryImpl @Inject constructor(
    private val tagDao: TagDao,
    private val clock: Clock,
) : TagRepository {

    override fun observeTags(): Flow<List<Tag>> =
        tagDao.observeActive().map { rows -> rows.map { it.toDomain() } }

    override suspend fun createTag(name: String): Tag {
        val clean = normalize(name)
        require(clean.isNotEmpty()) { "Tag name must not be blank" }
        tagDao.findActiveByName(clean)?.let { return it.toDomain() }

        val now = clock.millis()
        val entity = TagEntity(
            id = UUID.randomUUID().toString(),
            name = clean,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
        )
        tagDao.upsert(entity)
        return entity.toDomain()
    }

    override suspend fun renameTag(id: String, name: String): Boolean {
        val existing = tagDao.getById(id)?.takeIf { it.deletedAt == null } ?: return false
        val clean = normalize(name)
        if (clean.isEmpty()) return false
        val sameNameElsewhere = tagDao.findActiveByName(clean)
        if (sameNameElsewhere != null && sameNameElsewhere.id != id) return false
        tagDao.upsert(existing.copy(name = clean, updatedAt = clock.millis()))
        return true
    }

    override suspend fun setParent(id: String, parentId: String?): Boolean {
        val active = tagDao.getActive()
        val tag = active.firstOrNull { it.id == id } ?: return false
        if (parentId != null && active.none { it.id == parentId }) return false
        if (active.map { it.toDomain() }.wouldLoop(id, parentId)) return false
        if (tag.parentId == parentId) return true
        tagDao.upsert(tag.copy(parentId = parentId, updatedAt = clock.millis()))
        return true
    }

    override suspend fun deleteTag(id: String) {
        val now = clock.millis()
        val existing = tagDao.getById(id)
        // The tags under this one move up to where it was.
        tagDao.activeChildren(id).forEach { child ->
            tagDao.upsert(child.copy(parentId = existing?.parentId, updatedAt = now))
        }
        // Links to tasks stay in the table but readers ignore deleted tags.
        tagDao.softDelete(id, deletedAt = now)
    }

    private fun normalize(name: String): String = name.trim().removePrefix("#").trim().asTagName()
}
