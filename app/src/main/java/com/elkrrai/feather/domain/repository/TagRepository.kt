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

    /**
     * Puts the tag under [parentId], or at the top level when it is null. Returns false (and changes
     * nothing) if either tag does not exist or the move would make a loop: a tag cannot extend itself
     * or a tag that sits under it.
     */
    suspend fun setParent(id: String, parentId: String?): Boolean

    /**
     * Removes the tag from every task and deletes it. The tags that were under it move up to where it
     * was, so nothing is cut off from the tree.
     */
    suspend fun deleteTag(id: String)
}
