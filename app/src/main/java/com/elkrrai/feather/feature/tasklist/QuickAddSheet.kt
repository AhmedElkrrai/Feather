package com.elkrrai.feather.feature.tasklist

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.model.TaskList
import com.elkrrai.feather.domain.quickadd.ParsedQuickAdd
import com.elkrrai.feather.domain.quickadd.QuickAddEdits
import com.elkrrai.feather.domain.quickadd.QuickAddField
import com.elkrrai.feather.domain.quickadd.QuickAddHint
import com.elkrrai.feather.domain.quickadd.QuickAddParser
import com.elkrrai.feather.domain.quickadd.TokenSpan
import com.elkrrai.feather.ui.format.displayName
import com.elkrrai.feather.ui.format.formatDueDate
import com.elkrrai.feather.ui.format.formatDueTime
import com.elkrrai.feather.ui.format.label
import com.elkrrai.feather.ui.rememberHaptics
import com.elkrrai.feather.ui.theme.FabShape
import com.elkrrai.feather.ui.theme.SheetShape
import com.elkrrai.feather.ui.theme.Space
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.launch

/** How many tasks have been added from the sheet, to know when the example hint can go. */
private class QuickAddHintStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("feather_ui_hints", Context.MODE_PRIVATE)

    fun count(): Int = prefs.getInt(KEY, 0)

    fun recordAdd() {
        prefs.edit().putInt(KEY, count() + 1).apply()
    }

    private companion object {
        const val KEY = "quick_add_adds"
    }
}

/** Shows the words that were understood in their tint, right in the text field. */
private class TokenTintTransformation(
    private val spans: List<TokenSpan>,
    private val tints: Map<QuickAddField, FieldTint>,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val styled = buildAnnotatedString {
            append(text)
            for (span in spans) {
                val tint = tints[span.field] ?: continue
                val start = span.start.coerceIn(0, text.length)
                val end = span.end.coerceIn(start, text.length)
                if (end > start) addStyle(SpanStyle(color = tint.content, background = tint.container), start, end)
            }
        }
        return TransformedText(styled, OffsetMapping.Identity)
    }
}

private val priorityTokens = listOf("!high", "!medium", "!low")

/**
 * Quick add with understanding: the text is parsed as it is typed ("Call mom tomorrow 5pm
 * !high #family"), the words that were understood are tinted in place, and each shows as a chip
 * that can be tapped away. A row of tools (date, priority, tag, repeat, voice) writes into the text.
 * [onAdd] gets the parsed task and the id of the list named with `@`, if any.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    val haptics = rememberHaptics()
    var text by rememberSaveable { mutableStateOf(initialText) }
    var ignored by remember { mutableStateOf(emptySet<QuickAddField>()) }
    val focusRequester = remember { FocusRequester() }
    val hintStore = remember { QuickAddHintStore(context) }
    var addsSoFar by remember { mutableIntStateOf(hintStore.count()) }

    // Dictation goes through the phone's own speech recognizer (no microphone permission here);
    // what it hears is added to the text and understood like anything typed.
    val voiceUnavailable = stringResource(R.string.voice_unavailable)
    val voicePrompt = stringResource(R.string.voice_prompt)
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
            if (!spoken.isNullOrEmpty()) {
                text = if (text.isBlank()) spoken else text.trimEnd() + " " + spoken
            }
        }
    }

    fun startVoice() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
            .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            .putExtra(RecognizerIntent.EXTRA_PROMPT, voicePrompt)
        try {
            voiceLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, voiceUnavailable, Toast.LENGTH_SHORT).show()
        }
    }

    val listNames = lists.map { it.displayName() }
    val parsed = remember(text, ignored, listNames) {
        QuickAddParser.parse(text, LocalDateTime.now(), listNames, ignored)
    }
    val today = LocalDate.now()

    fun submit() {
        if (parsed.title.isBlank()) return
        val listId = parsed.listName?.let { name -> lists.getOrNull(listNames.indexOf(name))?.id }
        haptics.thud()
        hintStore.recordAdd()
        addsSoFar = hintStore.count()
        onAdd(parsed, listId)
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    /** A tool wrote into the text: the part it wrote must not stay switched off from an earlier tap. */
    fun edited(newText: String, field: QuickAddField) {
        text = newText
        ignored = ignored - field
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val scheme = MaterialTheme.colorScheme
    val tints = remember(scheme) { QuickAddField.entries.associateWith { fieldTint(it, scheme) } }
    val tintTransformation = remember(parsed.spans, tints) { TokenTintTransformation(parsed.spans, tints) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, shape = SheetShape) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(start = Space.gutter, end = Space.gutter, bottom = Space.x4),
            verticalArrangement = Arrangement.spacedBy(Space.x3),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.x3),
                verticalAlignment = Alignment.CenterVertically,
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
                    shape = RoundedCornerShape(20.dp),
                    label = { Text(stringResource(R.string.quick_add_hint)) },
                    supportingText = if (text.isBlank() && QuickAddHint.isVisible(addsSoFar)) {
                        { Text(stringResource(R.string.quick_add_example)) }
                    } else {
                        null
                    },
                    visualTransformation = tintTransformation,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                )
                FilledIconButton(
                    onClick = ::submit,
                    enabled = parsed.title.isNotBlank(),
                    shape = FabShape,
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_send),
                        contentDescription = stringResource(R.string.action_add_task),
                    )
                }
            }

            QuickAddTools(
                spans = parsed.spans,
                onText = ::edited,
                currentText = text,
                onVoice = ::startVoice,
                onTag = {
                    edited(QuickAddEdits.append(text, "#"), QuickAddField.TAGS)
                    focusRequester.requestFocus()
                },
            )

            val chips = buildList {
                val date = parsed.dueDate
                if (date != null && !parsed.dateIsImplied) {
                    add(QuickAddChip(QuickAddField.DATE, formatDueDate(context, date, today, locale)))
                }
                parsed.dueTime?.let { time ->
                    val clock = formatDueTime(time, locale)
                    val shown = if (date != null && parsed.dateIsImplied) {
                        "${formatDueDate(context, date, today, locale)}, $clock"
                    } else {
                        clock
                    }
                    add(QuickAddChip(QuickAddField.TIME, shown))
                }
                parsed.recurrence?.let { add(QuickAddChip(QuickAddField.REPEAT, it.label(parsed.dueDate))) }
                if (parsed.priority.level > 0) add(QuickAddChip(QuickAddField.PRIORITY, parsed.priority.label()))
                if (parsed.tags.isNotEmpty()) {
                    add(QuickAddChip(QuickAddField.TAGS, parsed.tags.joinToString(" ") { "#$it" }))
                }
                parsed.listName?.let { add(QuickAddChip(QuickAddField.LIST, "@$it")) }
            }
            QuickAddChips(chips = chips, onIgnore = { field -> ignored = ignored + field })
        }
    }
}

/**
 * Date, priority, tag, repeat and voice: 48dp tools that write into the text. They never keep a
 * setting of their own: the parser reads the text again and the chips show what it understood.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddTools(
    spans: List<TokenSpan>,
    currentText: String,
    onText: (newText: String, field: QuickAddField) -> Unit,
    onVoice: () -> Unit,
    onTag: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var priorityMenu by remember { mutableStateOf(false) }
    var repeatMenu by remember { mutableStateOf(false) }

    Row(horizontalArrangement = Arrangement.spacedBy(Space.x1), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { showDatePicker = true }) {
            Icon(painterResource(R.drawable.ic_calendar), stringResource(R.string.quick_add_tool_date))
        }

        Box {
            IconButton(onClick = { priorityMenu = true }) {
                Icon(painterResource(R.drawable.ic_flag), stringResource(R.string.quick_add_tool_priority))
            }
            DropdownMenu(expanded = priorityMenu, onDismissRequest = { priorityMenu = false }) {
                val labels = listOf(R.string.priority_high, R.string.priority_medium, R.string.priority_low)
                priorityTokens.forEachIndexed { index, token ->
                    DropdownMenuItem(
                        text = { Text(stringResource(labels[index])) },
                        onClick = {
                            priorityMenu = false
                            onText(QuickAddEdits.replacing(currentText, spans, QuickAddField.PRIORITY, token), QuickAddField.PRIORITY)
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.priority_none)) },
                    onClick = {
                        priorityMenu = false
                        onText(QuickAddEdits.without(currentText, spans, QuickAddField.PRIORITY), QuickAddField.PRIORITY)
                    },
                )
            }
        }

        IconButton(onClick = onTag) {
            Icon(painterResource(R.drawable.ic_tag), stringResource(R.string.quick_add_tool_tag))
        }

        Box {
            IconButton(onClick = { repeatMenu = true }) {
                Icon(painterResource(R.drawable.ic_repeat), stringResource(R.string.quick_add_tool_repeat))
            }
            DropdownMenu(expanded = repeatMenu, onDismissRequest = { repeatMenu = false }) {
                val options = listOf(
                    R.string.repeat_freq_daily to R.string.quick_add_insert_every_day,
                    R.string.repeat_freq_weekly to R.string.quick_add_insert_every_week,
                    R.string.repeat_freq_monthly to R.string.quick_add_insert_every_month,
                    R.string.repeat_freq_yearly to R.string.quick_add_insert_every_year,
                )
                options.forEach { (label, phrase) ->
                    val words = stringResource(phrase)
                    DropdownMenuItem(
                        text = { Text(stringResource(label)) },
                        onClick = {
                            repeatMenu = false
                            onText(QuickAddEdits.replacing(currentText, spans, QuickAddField.REPEAT, words), QuickAddField.REPEAT)
                        },
                    )
                }
            }
        }

        IconButton(onClick = onVoice) {
            Icon(painterResource(R.drawable.ic_mic), stringResource(R.string.voice_input))
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            // The picker works in UTC midnights; the date written is the day that was tapped.
                            val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            onText(
                                QuickAddEdits.replacing(currentText, spans, QuickAddField.DATE, date.toString()),
                                QuickAddField.DATE,
                            )
                        }
                        showDatePicker = false
                    },
                ) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
