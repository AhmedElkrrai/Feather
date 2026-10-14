package com.elkrrai.feather.feature.tasklist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.model.Subtask
import com.elkrrai.feather.ui.components.MiniRing
import com.elkrrai.feather.ui.rememberHaptics
import androidx.compose.ui.geometry.Offset

/**
 * What opens under a task card: its subtasks, each with a small ring, and a field to add another.
 * Ticking one fills the bar on the card; adding one grows it.
 */
@Composable
internal fun SubtaskPanel(
    subtasks: List<Subtask>,
    onToggle: (subtaskId: String) -> Unit,
    onAdd: (title: String) -> Unit,
    modifier: Modifier = Modifier,
    onCelebrate: (Offset) -> Unit = {},
) {
    val haptics = rememberHaptics()
    Column(modifier = modifier.fillMaxWidth()) {
        subtasks.forEach { subtask ->
            key(subtask.id) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MiniRing(
                        checked = subtask.isCompleted,
                        onCheckedChange = {
                            haptics.tick()
                            onToggle(subtask.id)
                        },
                        contentDescription = subtask.title,
                        onCelebrate = onCelebrate,
                    )
                    Text(
                        text = subtask.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (subtask.isCompleted) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else null,
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                    )
                }
            }
        }
        AddSubtaskField(onAdd = onAdd, modifier = Modifier.padding(top = 4.dp))
    }
}

/** A one-line field: Done (or the plus) adds the subtask and clears the field for the next one. */
@Composable
private fun AddSubtaskField(onAdd: (String) -> Unit, modifier: Modifier = Modifier) {
    var text by rememberSaveable { mutableStateOf("") }

    fun commit() {
        val title = text.trim()
        if (title.isNotEmpty()) {
            onAdd(title)
            text = ""
        }
    }

    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
        placeholder = { Text(stringResource(R.string.add_subtask), style = MaterialTheme.typography.bodyMedium) },
        trailingIcon = {
            IconButton(onClick = ::commit, enabled = text.isNotBlank()) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.add_subtask),
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { commit() }),
    )
}
