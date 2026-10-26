package com.elkrrai.feather.feature.quickadd

import com.elkrrai.feather.domain.model.Priority
import com.elkrrai.feather.domain.model.Tag
import com.elkrrai.feather.domain.model.Task
import com.elkrrai.feather.domain.quickadd.ParsedQuickAdd
import com.elkrrai.feather.domain.repository.TagRepository
import com.elkrrai.feather.domain.repository.TaskRepository
import java.time.LocalDate

/**
 * Saves what the quick-add parser understood as a task: the title and date first, then the time,
 * priority, tags (created when new) and repeat rule in one go. Shared by the task list and the
 * quick-add screen that the share sheet, the Quick Settings tile and the launcher shortcut open.
 */
class QuickAddTask(
    private val tasks: TaskRepository,
    private val tags: TagRepository,
) {
    /**
     * @param fallbackDate the due date when the text did not give one (for example "today" while
     *   looking at the Today view)
     * @param extraTags tags to add besides the ones in the text (for example the tag being viewed)
     */
    suspend operator fun invoke(
        parsed: ParsedQuickAdd,
        fallbackDate: LocalDate? = null,
        extraTags: List<Tag> = emptyList(),
    ): Task {
        val created = tasks.addTask(parsed.title, parsed.dueDate ?: fallbackDate)

        val fromText = parsed.tags.mapNotNull { name ->
            try {
                tags.createTagPath(name)
            } catch (e: IllegalArgumentException) {
                null // a blank name
            }
        }
        val allTags = (extraTags + fromText).distinctBy { it.id }

        // A time or a repeat rule only makes sense with a due date.
        val hasDate = created.dueDate != null
        val time = parsed.dueTime.takeIf { hasDate }
        val recurrence = parsed.recurrence.takeIf { hasDate }

        if (time == null && recurrence == null && parsed.priority == Priority.NONE && allTags.isEmpty() &&
            parsed.counter == null
        ) {
            return created
        }
        val detailed = created.copy(
            dueTime = time,
            priority = parsed.priority,
            tags = allTags,
            recurrence = recurrence,
            counter = parsed.counter,
        )
        tasks.saveTaskDetails(detailed)
        return detailed
    }
}
