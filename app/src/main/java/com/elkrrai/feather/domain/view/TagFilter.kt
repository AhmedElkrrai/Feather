package com.elkrrai.feather.domain.view

import com.elkrrai.feather.domain.model.Tag
import com.elkrrai.feather.domain.model.Task
import com.elkrrai.feather.domain.model.branchIds

/** Which tag tab is picked above the task list. */
sealed interface TagFilter {
    /** The first tab: every task of the view, tagged or not. */
    data object All : TagFilter

    /** The last tab: only tasks that have no tag. */
    data object Untagged : TagFilter

    /** A tag's tab: the tasks of that tag and of every tag under it. */
    data class OfTag(val tagId: String) : TagFilter
}

/** The tasks that belong under [filter]; [allTags] is needed to know which tags sit under a tag. */
fun List<Task>.withTagFilter(filter: TagFilter, allTags: List<Tag>): List<Task> = when (filter) {
    TagFilter.All -> this
    TagFilter.Untagged -> filter { it.tags.isEmpty() }
    is TagFilter.OfTag -> {
        val branch = allTags.branchIds(filter.tagId)
        filter { task -> task.tags.any { it.id in branch } }
    }
}
