package com.elkrrai.feather.domain.model

/** A tag and how deep it sits in the tree (0 for a top-level tag), for showing the tree as an indented list. */
data class TagRow(val tag: Tag, val depth: Int)

/** The tags directly under [parentId] (the top-level tags when it is null), alphabetical. */
fun List<Tag>.childrenOf(parentId: String?): List<Tag> {
    // A tag whose parent is gone counts as top-level, so nothing is ever lost from view.
    val ids = mapTo(HashSet()) { it.id }
    return filter { tag ->
        if (parentId == null) tag.parentId == null || tag.parentId !in ids else tag.parentId == parentId
    }.sortedBy { it.name.lowercase() }
}

/** [rootId] and every tag under it, however deep. A loop in the data cannot make this run forever. */
fun List<Tag>.branchIds(rootId: String): Set<String> {
    val result = LinkedHashSet<String>()
    val pending = ArrayDeque(listOf(rootId))
    while (pending.isNotEmpty()) {
        val id = pending.removeFirst()
        if (!result.add(id)) continue
        childrenOf(id).forEach { pending.addLast(it.id) }
    }
    return result
}

/**
 * True if putting [tagId] under [newParentId] would make a loop: the tag cannot extend itself, or
 * extend a tag that already sits under it. A null parent (top level) never loops.
 */
fun List<Tag>.wouldLoop(tagId: String, newParentId: String?): Boolean =
    newParentId != null && newParentId in branchIds(tagId)

/** The tags a tag may be put under: everything except itself and the tags below it. */
fun List<Tag>.possibleParentsOf(tagId: String): List<Tag> {
    val blocked = branchIds(tagId)
    return filter { it.id !in blocked }.sortedBy { it.name.lowercase() }
}

/** The tags as an indented tree: each top-level tag followed by the tags under it, alphabetical at every level. */
fun List<Tag>.inTreeOrder(): List<TagRow> {
    val rows = ArrayList<TagRow>(size)
    val seen = HashSet<String>()
    fun walk(parentId: String?, depth: Int) {
        for (tag in childrenOf(parentId)) {
            if (!seen.add(tag.id)) continue
            rows += TagRow(tag, depth)
            walk(tag.id, depth + 1)
        }
    }
    walk(null, 0)
    return rows
}
