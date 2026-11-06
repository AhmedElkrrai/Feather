package com.elkrrai.feather.feature.habits

import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.habit.HabitStage

/** The stages of one day of a habit as a checklist: each is ticked or unticked with a tap, in any order. */
@Composable
internal fun StageChecklistDialog(
    title: String,
    stages: List<HabitStage>,
    done: Set<String>,
    onToggle: (stage: HabitStage, done: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                stages.forEach { stage ->
                    val checked = stage.id in done
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle(stage, it) })
                            .padding(vertical = 8.dp),
                    ) {
                        Checkbox(checked = checked, onCheckedChange = null)
                        Column {
                            Text(stage.name, style = MaterialTheme.typography.bodyLarge)
                            if (stage.time.isSet) {
                                Text(
                                    text = describeTime(stage.time),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_ok)) } },
    )
}

/**
 * The parts of one past day, read-only: a check mark for each part that was done, a dash for each that was not. Nothing here can be changed:
 * a day in the history stays as it was.
 */
@Composable
internal fun StageHistoryDialog(
    title: String,
    stages: List<HabitStage>,
    done: Set<String>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                stages.forEach { stage ->
                    val wasDone = stage.id in done
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    ) {
                        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                            if (wasDone) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_check),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            } else {
                                Text("\u2013", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text(
                            text = stage.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (wasDone) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_ok)) } },
    )
}
