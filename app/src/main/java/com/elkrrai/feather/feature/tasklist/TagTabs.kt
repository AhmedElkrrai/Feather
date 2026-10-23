package com.elkrrai.feather.feature.tasklist

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.model.Tag
import com.elkrrai.feather.domain.model.childrenOf
import com.elkrrai.feather.domain.view.TagFilter

/**
 * The tag tabs above the task list: "All" (every task of the view), one tab for each top-level tag
 * (its tasks and the tasks of every tag under it) and "No tag" (tasks without any tag). Picking a tag
 * that has tags under it shows one more row of tabs for those, headed by "All #tag", and so on down.
 * Nothing is drawn while there are no tags.
 */
@Composable
fun TagTabs(
    tags: List<Tag>,
    filter: TagFilter,
    onPick: (TagFilter) -> Unit,
    taskCounts: Map<String, Int>,
    untaggedCount: Int,
    modifier: Modifier = Modifier,
) {
    val path = remember(tags, filter) { tagPath(filter, tags) }
    // Tabs are available only while they contain tasks in the current view.
    fun hasTasks(tag: Tag) = (taskCounts[tag.id] ?: 0) > 0
    val topLevel = tags.childrenOf(null).filter(::hasTasks)
    val showNoTag = untaggedCount > 0
    // Without any tag to pick, "All" alone is not worth a row.
    if (topLevel.isEmpty() && filter == TagFilter.All) return
    Column(modifier) {
        val firstSelected = when (filter) {
            TagFilter.All -> 0
            TagFilter.Untagged -> topLevel.size + 1
            is TagFilter.OfTag -> topLevel.indexOfFirst { it.id == path.firstOrNull()?.id } + 1
        }
        TabRow(
            selectedIndex = firstSelected,
            labels = listOf(stringResource(R.string.tab_all)) + topLevel.map { "#${it.name}" } +
                listOfNotNull(if (showNoTag) stringResource(R.string.tab_no_tag) else null),
            onPick = { index ->
                when {
                    index == 0 -> onPick(TagFilter.All)
                    index == topLevel.size + 1 -> onPick(TagFilter.Untagged)
                    else -> onPick(TagFilter.OfTag(topLevel[index - 1].id))
                }
            },
        )
        // One more row under each picked tag that has tags under it.
        path.forEachIndexed { depth, tag ->
            val children = tags.childrenOf(tag.id).filter(::hasTasks)
            if (children.isNotEmpty()) {
                val next = path.getOrNull(depth + 1)
                TabRow(
                    selectedIndex = if (next == null) 0 else children.indexOfFirst { it.id == next.id } + 1,
                    labels = listOf(stringResource(R.string.tab_all_in, tag.name)) + children.map { "#${it.name}" },
                    onPick = { index ->
                        onPick(TagFilter.OfTag(if (index == 0) tag.id else children[index - 1].id))
                    },
                )
            }
        }
    }
}

@Composable
private fun TabRow(selectedIndex: Int, labels: List<String>, onPick: (index: Int) -> Unit) {
    ScrollableTabRow(
        selectedTabIndex = selectedIndex.coerceIn(0, labels.lastIndex),
        edgePadding = 16.dp,
        containerColor = MaterialTheme.colorScheme.background,
        divider = {},
    ) {
        labels.forEachIndexed { index, label ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onPick(index) },
                text = { Text(label, maxLines = 1) },
            )
        }
    }
}

/** The picked tag and the tags above it, from the top-level one down; empty for All and No tag. */
internal fun tagPath(filter: TagFilter, tags: List<Tag>): List<Tag> {
    val picked = (filter as? TagFilter.OfTag) ?: return emptyList()
    val byId = tags.associateBy { it.id }
    val path = ArrayList<Tag>()
    var current = byId[picked.tagId]
    // The size check only guards against bad data with a loop in it.
    while (current != null && path.size <= byId.size) {
        path.add(0, current)
        current = current.parentId?.let { byId[it] }
    }
    return path
}
