package com.elkrrai.feather.feature.tasklist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.model.Tag
import com.elkrrai.feather.domain.model.childrenOf
import com.elkrrai.feather.domain.view.TagFilter

/**
 * The tag filters above the task list, as pills: "All" (every task of the view), one pill for each top-level tag (its tasks and the
 * tasks of every tag under it) and "No tag" (tasks without any tag). Never more than two rows: the second, smaller one holds what sits
 * under the deepest picked tag ("All in Work" first), or, when that tag has nothing under it, its brothers and sisters; a dashed pill
 * at its start goes one level up. Nothing is drawn while there are no tags.
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
    // Pills are available only while they contain tasks in the current view.
    fun hasTasks(tag: Tag) = (taskCounts[tag.id] ?: 0) > 0
    val topLevel = tags.childrenOf(null).filter(::hasTasks)
    val showNoTag = untaggedCount > 0
    // Without any tag to pick, "All" alone is not worth a row.
    if (topLevel.isEmpty() && filter == TagFilter.All) return

    val deepest = path.lastOrNull()
    val context: Tag? = when {
        deepest == null -> null
        tags.childrenOf(deepest.id).any(::hasTasks) -> deepest
        else -> deepest.parentId?.let { id -> tags.firstOrNull { it.id == id } }
    }
    val children = context?.let { tags.childrenOf(it.id).filter(::hasTasks) }.orEmpty()
    val above = context?.parentId?.let { id -> tags.firstOrNull { it.id == id } }

    Column(modifier) {
        PillRow {
            TabPill(stringResource(R.string.tab_all), selected = filter == TagFilter.All, onClick = { onPick(TagFilter.All) })
            topLevel.forEach { tag ->
                TabPill(tag.name, selected = path.firstOrNull()?.id == tag.id, onClick = { onPick(TagFilter.OfTag(tag.id)) })
            }
            if (showNoTag) {
                TabPill(stringResource(R.string.tab_no_tag), selected = filter == TagFilter.Untagged, onClick = { onPick(TagFilter.Untagged) })
            }
        }
        if (context != null && children.isNotEmpty()) {
            PillRow {
                if (above != null) {
                    TabPill(
                        label = stringResource(R.string.tab_back_to, above.name),
                        selected = false,
                        small = true,
                        dashed = true,
                        onClick = { onPick(TagFilter.OfTag(above.id)) },
                    )
                }
                TabPill(
                    label = stringResource(R.string.tab_all_in, context.name),
                    selected = deepest?.id == context.id,
                    small = true,
                    onClick = { onPick(TagFilter.OfTag(context.id)) },
                )
                children.forEach { child ->
                    TabPill(child.name, selected = path.any { it.id == child.id }, small = true, onClick = { onPick(TagFilter.OfTag(child.id)) })
                }
            }
        }
    }
}

@Composable
private fun PillRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** One filter: filled blue when picked. The [small] kind is for the second row; [dashed] marks the one that goes up a level. */
@Composable
private fun TabPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    small: Boolean = false,
    dashed: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val background = when {
        selected && small -> scheme.primary.copy(alpha = 0.16f)
        selected -> scheme.primary
        small -> Color.Transparent
        else -> scheme.surfaceContainer
    }
    val content = when {
        selected && small -> scheme.primary
        selected -> scheme.onPrimary
        small -> scheme.onSurfaceVariant
        else -> scheme.onSurface
    }
    val line = when {
        selected && small -> scheme.primary.copy(alpha = 0.5f)
        selected -> scheme.primary
        small -> scheme.outlineVariant
        else -> scheme.outlineVariant.copy(alpha = 0.6f)
    }
    Text(
        text = label,
        maxLines = 1,
        style = if (small) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        color = content,
        modifier = Modifier
            .clip(CircleShape)
            .background(background)
            .then(
                if (dashed) {
                    Modifier.drawBehind {
                        drawRoundRect(
                            color = line,
                            cornerRadius = CornerRadius(size.height / 2f),
                            style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
                        )
                    }
                } else {
                    Modifier.border(1.dp, line, CircleShape)
                },
            )
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = if (small) 12.dp else 14.dp, vertical = if (small) 6.dp else 8.dp),
    )
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
