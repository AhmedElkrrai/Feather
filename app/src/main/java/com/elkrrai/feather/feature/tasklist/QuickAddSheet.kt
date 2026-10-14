package com.elkrrai.feather.feature.tasklist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.model.TaskList
import com.elkrrai.feather.domain.quickadd.ParsedQuickAdd
import com.elkrrai.feather.domain.quickadd.QuickAddField
import com.elkrrai.feather.domain.quickadd.QuickAddParser
import com.elkrrai.feather.ui.format.displayName
import com.elkrrai.feather.ui.format.formatDueDate
import com.elkrrai.feather.ui.format.formatDueTime
import com.elkrrai.feather.ui.format.label
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.launch

/**
 * Quick add with understanding: the text is parsed as it is typed ("Call mom tomorrow 5pm
 * !high #family") and what was recognized shows as chips; tapping a chip leaves those words in
 * the title instead. [onAdd] gets the parsed task and the id of the list named with `@`, if any.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickAddSheet(
    lists: List<TaskList>,
    onDismiss: () -> Unit,
    onAdd: (parsed: ParsedQuickAdd, listId: String?) -> Unit,
    initialText: String = "",
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    var text by rememberSaveable { mutableStateOf(initialText) }
    var ignored by remember { mutableStateOf(emptySet<QuickAddField>()) }
    val focusRequester = remember { FocusRequester() }

    val listNames = lists.map { it.displayName() }
    val parsed = remember(text, ignored, listNames) {
        QuickAddParser.parse(text, LocalDateTime.now(), listNames, ignored)
    }
    val today = LocalDate.now()

    fun submit() {
        if (parsed.title.isBlank()) return
        val listId = parsed.listName?.let { name -> lists.getOrNull(listNames.indexOf(name))?.id }
        onAdd(parsed, listId)
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        if (it.isBlank()) ignored = emptySet()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    label = { Text(stringResource(R.string.quick_add_hint)) },
                    supportingText = if (text.isBlank()) {
                        { Text(stringResource(R.string.quick_add_example)) }
                    } else {
                        null
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                )
                Button(
                    onClick = ::submit,
                    enabled = parsed.title.isNotBlank(),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text(stringResource(R.string.action_add))
                }
            }

            val repeatText = parsed.recurrence?.label(parsed.dueDate)
            val priorityText = parsed.priority.label()
            val chips = buildList {
                val date = parsed.dueDate
                if (date != null && !parsed.dateIsImplied) {
                    add(QuickAddField.DATE to formatDueDate(context, date, today, locale))
                }
                parsed.dueTime?.let { time ->
                    val clock = formatDueTime(time, locale)
                    val shown = if (date != null && parsed.dateIsImplied) {
                        "${formatDueDate(context, date, today, locale)}, $clock"
                    } else {
                        clock
                    }
                    add(QuickAddField.TIME to shown)
                }
                if (repeatText != null) add(QuickAddField.REPEAT to repeatText)
                if (parsed.priority.level > 0) add(QuickAddField.PRIORITY to priorityText)
                if (parsed.tags.isNotEmpty()) {
                    add(QuickAddField.TAGS to parsed.tags.joinToString(" ") { "#$it" })
                }
                parsed.listName?.let { add(QuickAddField.LIST to "@$it") }
            }

            if (chips.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    chips.forEach { (field, label) ->
                        val ignore = stringResource(R.string.quick_add_ignore, label)
                        InputChip(
                            selected = true,
                            onClick = { ignored = ignored + field },
                            label = { Text(label) },
                            trailingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_close),
                                    contentDescription = ignore,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}
