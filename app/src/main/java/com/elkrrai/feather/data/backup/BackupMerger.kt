package com.elkrrai.feather.data.backup

/**
 * What an import has to write: the records of the file that are new or newer than what is on
 * the phone, plus the child rows that belong to them.
 *
 * [rewrittenTaskIds] are the tasks whose own record is taken from the file; for those the
 * phone's tag links are replaced by [tagLinks]. [reminders] are upserted by id.
 */
data class MergePlan(
    val tags: List<TagRecord>,
    val tasks: List<TaskRecord>,
    val tagLinks: List<TaskTagRecord>,
    val reminders: List<ReminderRecord>,
    val completions: List<CompletionRecord>,
    val rewrittenTaskIds: Set<String>,
    val habits: List<HabitRecord> = emptyList(),
    val habitGoals: List<HabitGoalRecord> = emptyList(),
    val habitEntries: List<HabitEntryRecord> = emptyList(),
    val habitStages: List<HabitStageRecord> = emptyList(),
    val habitGoalStages: List<HabitGoalStageRecord> = emptyList(),
    /** The goal weeks (habit id, first day) whose stage list of the phone is replaced whole by [habitGoalStages]; clear them first. */
    val rewrittenGoalStageWeeks: Set<Pair<String, String>> = emptySet(),
    val habitStageLog: List<HabitStageLogRecord> = emptyList(),
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
 * - Tag links and reminders come from the file only for tasks that were taken from it.
 *   Completion history is a union.
 * - Habits are taken like tasks (the later `updatedAt` wins). Their goals and logged days are taken one by
 *   one, each when the phone does not have it or the file's copy is later, for habits the phone ends up with.
 * - The stages of a habit are taken like its goals. The list of stages of one goal week is taken whole, from the side whose copy of it is
 *   newer (a phone's list and the file's list are never mixed, so a stage that was removed does not come back). A done stage is taken
 *   one by one, like a logged day.
 */
object BackupMerger {

    fun plan(local: BackupFile, incoming: BackupFile): MergePlan {
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

        val tasks = newer(local.tasks, incoming.tasks, { it.id }, { it.updatedAt })
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

        val habits = newer(local.habits, incoming.habits, { it.id }, { it.updatedAt })
        val knownHabits = local.habits.map { it.id }.toSet() + habits.map { it.id }
        val habitGoals = newer(
            local.habitGoals,
            incoming.habitGoals.filter { it.habitId in knownHabits },
            { "${it.habitId}|${it.effectiveFrom}" },
            { it.updatedAt },
        )
        val habitEntries = newer(
            local.habitEntries,
            incoming.habitEntries.filter { it.habitId in knownHabits },
            { "${it.habitId}|${it.date}" },
            { it.updatedAt },
        )

        val habitStages = newer(
            local.habitStages,
            incoming.habitStages.filter { it.habitId in knownHabits },
            { it.id },
            { it.updatedAt },
        )
        val knownStages = local.habitStages.map { it.id }.toSet() + habitStages.map { it.id }
        val localWeeks = local.habitGoalStages.groupBy { it.habitId to it.effectiveFrom }
        val takenWeeks = incoming.habitGoalStages
            .filter { it.habitId in knownHabits }
            .groupBy { it.habitId to it.effectiveFrom }
            .filter { (week, rows) ->
                val mine = localWeeks[week]
                mine == null || rows.maxOf { it.updatedAt } > mine.maxOf { it.updatedAt }
            }
        val habitGoalStages = takenWeeks.values.flatten().filter { it.stageId in knownStages }
        val habitStageLog = newer(
            local.habitStageLog,
            incoming.habitStageLog.filter { it.habitId in knownHabits && it.stageId in knownStages },
            { "${it.habitId}|${it.date}|${it.stageId}" },
            { it.updatedAt },
        )

        return MergePlan(
            tags = tags,
            tasks = tasks,
            tagLinks = tagLinks,
            reminders = reminders,
            completions = completions,
            rewrittenTaskIds = rewritten,
            habits = habits,
            habitGoals = habitGoals,
            habitEntries = habitEntries,
            habitStages = habitStages,
            habitGoalStages = habitGoalStages,
            rewrittenGoalStageWeeks = takenWeeks.keys.filter { it in localWeeks }.toSet(),
            habitStageLog = habitStageLog,
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
