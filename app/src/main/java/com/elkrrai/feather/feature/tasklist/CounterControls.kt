package com.elkrrai.feather.feature.tasklist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.model.Counter
import com.elkrrai.feather.ui.rememberHaptics

/**
 * The "+25" button of a counter task: a tap adds one step, a long press opens [CounterDialog] (through
 * [onLongClick]) to take a mistake back or set the count. [onCelebrate] gets the button's middle in
 * window pixels, with the size of the burst: a full one when this step reaches the target.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CounterStepButton(
    counter: Counter,
    title: String,
    onAdd: (Int) -> Unit,
    onLongClick: () -> Unit,
    onCelebrate: (origin: Offset, scale: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    var middle by remember { mutableStateOf(Offset.Zero) }
    val addLabel = stringResource(R.string.counter_add_description, counter.step, title)
    val setLabel = stringResource(R.string.counter_set_action)
    Surface(
        modifier = modifier
            .defaultMinSize(minWidth = 56.dp, minHeight = 40.dp)
            .onGloballyPositioned { middle = it.positionInWindow() + Offset(it.size.width / 2f, it.size.height / 2f) },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Box(
            modifier = Modifier
                .combinedClickable(
                    onClickLabel = addLabel,
                    onLongClickLabel = setLabel,
                    role = Role.Button,
                    onLongClick = {
                        haptics.longPress()
                        onLongClick()
                    },
                    onClick = {
                        haptics.tick()
                        onCelebrate(middle, if (counter.done + counter.step >= counter.target) 1f else 0.6f)
                        onAdd(counter.step)
                    },
                )
                .defaultMinSize(minWidth = 56.dp, minHeight = 40.dp)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.counter_step_label, counter.step),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/**
 * The target and the step of a new counter task: "to 100 by 25". The step may be left empty (one at a
 * time). [onRemove] is offered when a counter is already chosen.
 */
@Composable
internal fun CounterSetupDialog(
    current: Counter?,
    onSet: (Counter) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    var target by rememberSaveable { mutableStateOf(current?.target?.toString().orEmpty()) }
    var step by rememberSaveable { mutableStateOf(current?.step?.toString().orEmpty()) }
    val counter = Counter.of(target.toIntOrNull(), step.toIntOrNull(), done = 0)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.counter_setup_title)) },
        text = {
            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it.filter(Char::isDigit).take(5) },
                    label = { Text(stringResource(R.string.counter_target)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = step,
                    onValueChange = { step = it.filter(Char::isDigit).take(5) },
                    label = { Text(stringResource(R.string.counter_step)) },
                    supportingText = { Text(stringResource(R.string.counter_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (current != null) {
                    TextButton(onClick = onRemove) { Text(stringResource(R.string.counter_remove)) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { counter?.let(onSet) }, enabled = counter != null) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/**
 * Sets the count of a counter task to a number, or takes the last step back. Reaching the target
 * finishes the task.
 */
@Composable
internal fun CounterDialog(
    counter: Counter,
    onSet: (Int) -> Unit,
    onTakeBack: () -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(counter.done.toString()) }
    val value = text.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.counter_set_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = countText(it, counter.target) },
                    label = { Text(stringResource(R.string.counter_set_label)) },
                    supportingText = { Text(stringResource(R.string.counter_set_of, counter.target)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (counter.done > 0) {
                    TextButton(onClick = onTakeBack, modifier = Modifier.padding(top = 4.dp)) {
                        Text(stringResource(R.string.counter_take_back, minOf(counter.step, counter.done)))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { value?.let { onSet(it.coerceAtMost(counter.target)) } }, enabled = value != null) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** What was typed as a count: digits only, no leading zeros, and never more than [max]. */
internal fun countText(typed: String, max: Int): String {
    val digits = typed.filter(Char::isDigit).take(5).trimStart('0')
    val value = digits.toIntOrNull() ?: return if (typed.any(Char::isDigit)) "0" else ""
    return minOf(value, max).toString()
}
