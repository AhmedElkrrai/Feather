package com.elkrrai.feather.data.backup

/**
 * What an import has to write: the records of the file that are new or newer than what is on
 * the phone, plus the child rows that belong to them.
 *
 * [rewrittenTaskIds] are the tasks whose own record is taken from the file; for those the
 * phone's tag links are replaced by [tagLinks]. [reminders] are upserted by id.
 */
data class MergePlan(
    val folders: List<FolderRecord>,
    val lists: List<ListRecord>,
    val tags: List<TagRecord>,
    val tasks: List<TaskRecord>,
    val tagLinks: List<TaskTagRecord>,
    val reminders: List<ReminderRecord>,
    val completions: List<CompletionRecord>,
    val rewrittenTaskIds: Set<String>,
)

/**
 * Decides, record by record, what an import changes. Pure functions over [BackupFile]s, so the
 * rules are tested without a database. Importing into an empty phone (after "replace") is the
 * same thing with an empty [local].
 *
 * - A record from the file is taken when the phone does not have it or the file's copy has a
 *   later `updatedAt`; on a tie the phone's copy stays.
 * - A tag in the file that has the same name as a live tag on the phone (ignoring case) is
 *   joined to the phone's tag instead of being added a second time.
 * - A task whose list is unknown goes to the Inbox ([inboxId]).
 * - Tag links and reminders come from the file only for tasks that were taken from it.
 *   Completion history is a union.
 */
object BackupMerger {

    fun plan(local: BackupFile, incoming: BackupFile, inboxId: String): MergePlan {
        val folders = newer(local.folders, incoming.folders, { it.id }, { it.updatedAt })
        val lists = newer(local.lists, incoming.lists, { it.id }, { it.updatedAt })

        // Same-named tags are one tag.
        val liveLocalTags = local.tags.filter { it.deletedAt == null }.associateBy { normalizedName(it.name) }
        val tagRemap = HashMap<String, String>()
        val candidateTags = incoming.tags.filter { tag ->
            val twin = if (tag.deletedAt == null) liveLocalTags[normalizedName(tag.name)] else null
            val duplicate = twin != null && twin.id != tag.id
            if (duplicate) tagRemap[tag.id] = twin!!.id
            !duplicate
        }
        val tags = newer(local.tags, candidateTags, { it.id }, { it.updatedAt })

        val knownLists = local.lists.map { it.id }.toSet() + lists.map { it.id } + inboxId
        val tasks = newer(local.tasks, incoming.tasks, { it.id }, { it.updatedAt })
            .map { if (it.listId in knownLists) it else it.copy(listId = inboxId) }
        val rewritten = tasks.map { it.id }.toSet()

        val knownTags = local.tags.map { it.id }.toSet() + tags.map { it.id }
        val tagLinks = incoming.taskTags
            .filter { it.taskId in rewritten }
            .map { it.copy(tagId = tagRemap[it.tagId] ?: it.tagId) }
            .filter { it.tagId in knownTags }
            .distinct()

        val reminders = incoming.reminders.filter { it.taskId in rewritten }

        val knownTasks = local.tasks.map { it.id }.toSet() + rewritten
        val knownCompletions = local.completions.map { it.id }.toSet()
        val completions = incoming.completions
            .filter { it.taskId in knownTasks && it.id !in knownCompletions }
            .distinctBy { it.id }

        return MergePlan(
            folders = folders,
            lists = lists,
            tags = tags,
            tasks = tasks,
            tagLinks = tagLinks,
            reminders = reminders,
            completions = completions,
            rewrittenTaskIds = rewritten,
        )
    }

    private fun normalizedName(name: String) = name.trim().lowercase()

    /** The records of [incoming] that are missing from [local] or have a later `updatedAt`. */
    private fun <T> newer(
        local: List<T>,
        incoming: List<T>,
        id: (T) -> String,
        updatedAt: (T) -> Long,
    ): List<T> {
        val localById = local.associateBy(id)
        // If the file lists a record twice, only its latest copy counts.
        val latest = incoming.groupBy(id).values.map { copies -> copies.maxBy(updatedAt) }
        return latest.filter { record ->
            val existing = localById[id(record)]
            existing == null || updatedAt(record) > updatedAt(existing)
        }
    }
}
