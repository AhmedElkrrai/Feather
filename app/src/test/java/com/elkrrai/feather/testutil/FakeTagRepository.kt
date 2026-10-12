package com.elkrrai.feather.testutil

import com.elkrrai.feather.domain.model.Tag
import com.elkrrai.feather.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory [TagRepository] with case-insensitive names, like the real one. */
class FakeTagRepository : TagRepository {

    private val tags = MutableStateFlow<List<Tag>>(emptyList())
    private var counter = 0L

    override fun observeTags(): Flow<List<Tag>> = tags.map { all -> all.sortedBy { it.name.lowercase() } }

    override suspend fun createTag(name: String): Tag {
        val clean = name.trim().removePrefix("#").trim()
        require(clean.isNotEmpty())
        tags.value.firstOrNull { it.name.equals(clean, ignoreCase = true) }?.let { return it }
        counter++
        val tag = Tag("tag-$counter", clean)
        tags.update { it + tag }
        return tag
    }

    override suspend fun renameTag(id: String, name: String): Boolean {
        val clean = name.trim().removePrefix("#").trim()
        if (clean.isEmpty()) return false
        if (tags.value.any { it.id != id && it.name.equals(clean, ignoreCase = true) }) return false
        tags.update { all -> all.map { if (it.id == id) it.copy(name = clean) else it } }
        return true
    }

    override suspend fun deleteTag(id: String) {
        tags.update { all -> all.filterNot { it.id == id } }
    }
}
