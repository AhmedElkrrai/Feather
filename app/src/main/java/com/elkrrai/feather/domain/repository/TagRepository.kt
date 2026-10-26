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

    /**
     * Creates the tags of a path like "work/bugfix" and returns the last one: "Work" (or the one that
     * exists), then "Bugfix" under it. A tag that already sits under another tag stays where it is; only
     * a tag that was at the top level is put under the one before it. Blank parts are ignored; a path
     * with nothing in it is refused like a blank name.
     */
    suspend fun createTagPath(path: String): Tag {
        val names = path.split('/').map { it.trim().removePrefix("#").trim() }.filter { it.isNotEmpty() }
        require(names.isNotEmpty()) { "Tag name must not be blank" }
        var before: Tag? = null
        for (name in names) {
            var tag = createTag(name)
            val parent = before
            if (parent != null && tag.id != parent.id && tag.parentId == null && setParent(tag.id, parent.id)) {
                tag = tag.copy(parentId = parent.id)
            }
            before = tag
        }
        return before!!
    }

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
