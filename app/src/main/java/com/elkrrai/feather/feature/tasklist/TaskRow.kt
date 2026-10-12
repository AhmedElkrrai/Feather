package com.elkrrai.feather.feature.tasklist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.model.Task
import com.elkrrai.feather.ui.format.formatDueDate
import com.elkrrai.feather.ui.format.formatDueTime
import com.elkrrai.feather.ui.format.label
import com.elkrrai.feather.ui.format.markerColor
import java.time.LocalDate

/**
 * One task. [listName] is shown in front of the due date when the view mixes several lists
 * (smart views and tag views); pass null inside a single list.
 */
@Composable
fun TaskRow(
    task: Task,
    today: LocalDate,
    onToggle: (completed: Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    listName: String? = null,
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val dueText = remember(task.dueDate, task.dueTime, today, locale) {
        task.dueDate?.let { date ->
            val day = formatDueDate(context, date, today, locale)
            task.dueTime?.let { "$day, ${formatDueTime(it, locale)}" } ?: day
        }
    }
    val isOverdue = !task.isCompleted && task.dueDate?.isBefore(today) == true
    val markerColor = task.priority.markerColor()
    val priorityLabel = task.priority.label()
    val pinnedLabel = stringResource(R.string.pinned)

    val metaLine = listOfNotNull(listName, dueText).joinToString(" · ").ifEmpty { null }
    val subtaskTotal = task.subtasks.size
    val subtasksDone = task.subtasks.count { it.isCompleted }
    val hasSupporting = metaLine != null || task.tags.isNotEmpty() || subtaskTotal > 0

    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        leadingContent = {
            Checkbox(checked = task.isCompleted, onCheckedChange = onToggle)
        },
        headlineContent = {
            Text(
                text = task.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                color = if (task.isCompleted) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        },
        supportingContent = if (hasSupporting) {
            {
                Column {
                    if (metaLine != null) {
                        Text(
                            text = metaLine,
                            color = if (isOverdue) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    if (task.tags.isNotEmpty()) {
                        Text(
                            text = task.tags.joinToString("  ") { "#${it.name}" },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (subtaskTotal > 0) {
                        Text(
                            text = stringResource(R.string.subtask_progress, subtasksDone, subtaskTotal),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            null
        },
        trailingContent = if (task.isPinned || markerColor != null) {
            {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (task.isPinned) {
                        Icon(
                            painter = painterResource(R.drawable.ic_pin),
                            contentDescription = pinnedLabel,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (markerColor != null) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .background(markerColor, CircleShape)
                                .semantics { contentDescription = priorityLabel }
                        )
                    }
                }
            }
        } else {
            null
        },
    )
}
