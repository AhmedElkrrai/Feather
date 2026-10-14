package com.elkrrai.feather.feature.tasklist

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.model.DueWindow
import com.elkrrai.feather.domain.model.Priority
import com.elkrrai.feather.domain.model.SavedFilter
import com.elkrrai.feather.domain.model.Tag
import com.elkrrai.feather.domain.model.TaskList
import com.elkrrai.feather.ui.format.displayName
import com.elkrrai.feather.ui.format.label

/** The conditions picked in [FilterDialog]. */
data class FilterDraft(
    val name: String,
    val listId: String?,
    val tagId: String?,
    val minPriority: Priority,
    val due: DueWindow,
    val hideCompleted: Boolean,
)

/** Make a saved filter, or change one ([initial]): a name and any of list, tag, priority and due date. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterDialog(
    initial: SavedFilter?,
    lists: List<TaskList>,
    tags: List<Tag>,
    onConfirm: (FilterDraft) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initial?.name.orEmpty()) }
    var listId by rememberSaveable { mutableStateOf(initial?.listId) }
    var tagId by rememberSaveable { mutableStateOf(initial?.tagId) }
    var priorityLevel by rememberSaveable { mutableStateOf(initial?.minPriority?.level ?: 0) }
    var dueCode by rememberSaveable { mutableStateOf(initial?.due?.code ?: DueWindow.ANY.code) }
    var hideCompleted by rememberSaveable { mutableStateOf(initial?.hideCompleted ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (initial == null) R.string.new_filter else R.string.edit_filter))
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.filter_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )

                Group(R.string.field_list) {
                    FilterChip(
                        selected = listId == null,
                        onClick = { listId = null },
                        label = { Text(stringResource(R.string.filter_list_any)) },
                    )
                    lists.forEach { list ->
                        FilterChip(
                            selected = listId == list.id,
                            onClick = { listId = list.id },
                            label = { Text(list.displayName()) },
                        )
                    }
                }

                if (tags.isNotEmpty()) {
                    Group(R.string.field_tags) {
                        FilterChip(
                            selected = tagId == null,
                            onClick = { tagId = null },
                            label = { Text(stringResource(R.string.filter_tag_any)) },
                        )
                        tags.forEach { tag ->
                            FilterChip(
                                selected = tagId == tag.id,
                                onClick = { tagId = tag.id },
                                label = { Text("#${tag.name}") },
                            )
                        }
                    }
                }

                Group(R.string.field_priority) {
                    Priority.entries.forEach { priority ->
                        FilterChip(
                            selected = priorityLevel == priority.level,
                            onClick = { priorityLevel = priority.level },
                            label = {
                                Text(
                                    when (priority) {
                                        Priority.NONE -> stringResource(R.string.filter_priority_any)
                                        Priority.HIGH -> priority.label()
                                        else -> stringResource(R.string.filter_priority_at_least, priority.label())
                                    }
                                )
                            },
                        )
                    }
                }

                Group(R.string.field_due_date) {
                    DueWindow.entries.forEach { window ->
                        FilterChip(
                            selected = dueCode == window.code,
                            onClick = { dueCode = window.code },
                            label = { Text(stringResource(window.labelRes())) },
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(stringResource(R.string.filter_hide_completed), modifier = Modifier.weight(1f))
                    Switch(checked = hideCompleted, onCheckedChange = { hideCompleted = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onConfirm(
                        FilterDraft(
                            name = name.trim(),
                            listId = listId,
                            tagId = tagId,
                            minPriority = Priority.fromLevel(priorityLevel),
                            due = DueWindow.fromCode(dueCode),
                            hideCompleted = hideCompleted,
                        )
                    )
                },
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Group(@StringRes title: Int, chips: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { heading() },
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) { chips() }
    }
}

@StringRes
private fun DueWindow.labelRes(): Int = when (this) {
    DueWindow.ANY -> R.string.due_window_any
    DueWindow.OVERDUE -> R.string.due_window_overdue
    DueWindow.TODAY -> R.string.due_window_today
    DueWindow.NEXT_7_DAYS -> R.string.due_window_week
    DueWindow.NO_DATE -> R.string.due_window_none
}
