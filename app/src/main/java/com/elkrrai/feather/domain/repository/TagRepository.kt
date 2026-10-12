package com.elkrrai.feather.domain.repository

import com.elkrrai.feather.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    /** Non-deleted tags, alphabetical. */
    fun observeTags(): Flow<List<Tag>>

    /**
     * Returns the tag with this name (case-insensitive), creating it if needed. The name is
     * trimmed and a leading `#` is dropped; it must not be blank afterwards.
     */
    suspend fun createTag(name: String): Tag

    /** Returns false (and changes nothing) if the name is blank or used by another tag. */
    suspend fun renameTag(id: String, name: String): Boolean

    /** Removes the tag from every task and deletes it. */
    suspend fun deleteTag(id: String)
}
