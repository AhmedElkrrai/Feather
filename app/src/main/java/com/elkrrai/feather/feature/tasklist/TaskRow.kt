package com.elkrrai.feather.feature.tasklist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.domain.model.Task
import com.elkrrai.feather.ui.format.formatDueDate
import com.elkrrai.feather.ui.format.formatDueTime
import com.elkrrai.feather.ui.format.label
import com.elkrrai.feather.ui.format.markerColor
import java.time.LocalDate

@Composable
fun TaskRow(
    task: Task,
    today: LocalDate,
    onToggle: (completed: Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
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
        supportingContent = dueText?.let { text ->
            {
                Text(
                    text = text,
                    color = if (isOverdue) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        },
        trailingContent = markerColor?.let { color ->
            {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(color, CircleShape)
                        .semantics { contentDescription = priorityLabel }
                )
            }
        },
    )
}
