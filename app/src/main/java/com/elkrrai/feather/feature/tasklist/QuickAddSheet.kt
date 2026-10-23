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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.quickadd.ParsedQuickAdd
import com.elkrrai.feather.domain.quickadd.QuickAddEdits
import com.elkrrai.feather.domain.quickadd.QuickAddField
import com.elkrrai.feather.domain.quickadd.QuickAddHint
import com.elkrrai.feather.domain.quickadd.QuickAddParser
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
import androidx.compose.foundation.layout.heightIn
import com.elkrrai.feather.domain.model.Priority
import com.elkrrai.feather.domain.recurrence.Frequency
import com.elkrrai.feather.domain.recurrence.Recurrence
import androidx.compose.material3.AlertDialog
import com.elkrrai.feather.domain.model.Tag

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

/**
 * Quick add with understanding: the text is parsed as it is typed ("Call mom tomorrow 5pm
 * !high #family") and each part that was understood shows as a badge under the text field, which
 * can be tapped away. The tools (date, priority, repeat) do not write into the text: what they
 * choose appears as a badge too. [onAdd] gets the parsed task.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    tags: List<Tag>,
    onDismiss: () -> Unit,
    onAdd: (parsed: ParsedQuickAdd) -> Unit,
    initialText: String = "",
    /** Called when something was dictated into the sheet. */
    onVoiceUsed: () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val haptics = rememberHaptics()
    var text by rememberSaveable { mutableStateOf(initialText) }
    var ignored by remember { mutableStateOf(emptySet<QuickAddField>()) }
    // What the tools chose; it shows as a badge and wins over what the text says.
    var toolDate by remember { mutableStateOf<LocalDate?>(null) }
    var toolPriority by remember { mutableStateOf<Priority?>(null) }
    var toolRepeat by remember { mutableStateOf<Frequency?>(null) }
    var toolTags by remember { mutableStateOf(emptyList<String>()) }
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
                onVoiceUsed()
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

    // There are no lists to name with `@`, so the parser is given none.
    val fromText = remember(text, ignored) {
        QuickAddParser.parse(text, LocalDateTime.now(), emptyList(), ignored)
    }
    val parsed = remember(fromText, toolDate, toolPriority, toolRepeat, toolTags) {
        fromText.withTools(toolDate, toolPriority, toolRepeat, toolTags)
    }
    val today = LocalDate.now()

    fun submit() {
        if (parsed.title.isBlank()) return
        haptics.thud()
        hintStore.recordAdd()
        addsSoFar = hintStore.count()
        onAdd(parsed)
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    /** A tool chose a value: that part must not stay switched off from an earlier tap on its badge. */
    fun chosen(field: QuickAddField) {
        ignored = ignored - field
    }

    /** A badge was tapped away: the tool's choice goes, and so does what the text said about it. */
    fun dismissed(field: QuickAddField) {
        when (field) {
            QuickAddField.DATE -> toolDate = null
            QuickAddField.PRIORITY -> toolPriority = null
            QuickAddField.REPEAT -> toolRepeat = null
            QuickAddField.TAGS -> toolTags = emptyList()
            else -> Unit
        }
        ignored = ignored + field
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, shape = SheetShape) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(start = Space.gutter, end = Space.gutter, bottom = Space.x2),
            verticalArrangement = Arrangement.spacedBy(Space.x2),
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
            }
            QuickAddChips(
                chips = chips,
                onIgnore = ::dismissed,
            )

            QuickAddTools(
                onDate = { date ->
                    toolDate = date
                    chosen(QuickAddField.DATE)
                },
                onPriority = { priority ->
                    toolPriority = priority
                    chosen(QuickAddField.PRIORITY)
                },
                onRepeat = { frequency ->
                    toolRepeat = frequency
                    chosen(QuickAddField.REPEAT)
                },
                onVoice = ::startVoice,
                tags = tags,
                chosenTags = toolTags,
                onTagToggle = { name ->
                    toolTags = if (toolTags.any { it.equals(name, ignoreCase = true) }) {
                        toolTags.filterNot { it.equals(name, ignoreCase = true) }
                    } else {
                        toolTags + name
                    }
                    chosen(QuickAddField.TAGS)
                },
            )
        }
    }
}

/**
 * Date, priority, tag, repeat and voice: 48dp tools. Date, priority and repeat hand their choice up
 * (and tags) to be shown as badges; voice adds what it hears to the text.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddTools(
    onDate: (LocalDate) -> Unit,
    onPriority: (Priority) -> Unit,
    onRepeat: (Frequency) -> Unit,
    onVoice: () -> Unit,
    tags: List<Tag>,
    chosenTags: List<String>,
    onTagToggle: (String) -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var priorityMenu by remember { mutableStateOf(false) }
    var repeatMenu by remember { mutableStateOf(false) }
    var tagMenu by remember { mutableStateOf(false) }
    var newTagDialog by remember { mutableStateOf(false) }

    Row(horizontalArrangement = Arrangement.spacedBy(Space.x1), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { showDatePicker = true }) {
            Icon(painterResource(R.drawable.ic_calendar), stringResource(R.string.quick_add_tool_date))
        }

        Box {
            IconButton(onClick = { priorityMenu = true }) {
                Icon(painterResource(R.drawable.ic_flag), stringResource(R.string.quick_add_tool_priority))
            }
            DropdownMenu(expanded = priorityMenu, onDismissRequest = { priorityMenu = false }) {
                val levels = listOf(
                    R.string.priority_high to Priority.HIGH,
                    R.string.priority_medium to Priority.MEDIUM,
                    R.string.priority_low to Priority.LOW,
                    R.string.priority_none to Priority.NONE,
                )
                levels.forEach { (label, priority) ->
                    DropdownMenuItem(
                        text = { Text(stringResource(label)) },
                        onClick = {
                            priorityMenu = false
                            onPriority(priority)
                        },
                    )
                }
            }
        }

        Box {
            IconButton(onClick = { tagMenu = true }) {
                Icon(painterResource(R.drawable.ic_tag), stringResource(R.string.quick_add_tool_tag))
            }
            DropdownMenu(expanded = tagMenu, onDismissRequest = { tagMenu = false }) {
                tags.forEach { tag ->
                    val picked = chosenTags.any { it.equals(tag.name, ignoreCase = true) }
                    DropdownMenuItem(
                        text = { Text("#${tag.name}") },
                        trailingIcon = if (picked) {
                            { Icon(painterResource(R.drawable.ic_check), contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else {
                            null
                        },
                        onClick = {
                            tagMenu = false
                            onTagToggle(tag.name)
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.new_tag)) },
                    onClick = {
                        tagMenu = false
                        newTagDialog = true
                    },
                )
            }
        }

        Box {
            IconButton(onClick = { repeatMenu = true }) {
                Icon(painterResource(R.drawable.ic_repeat), stringResource(R.string.quick_add_tool_repeat))
            }
            DropdownMenu(expanded = repeatMenu, onDismissRequest = { repeatMenu = false }) {
                val options = listOf(
                    R.string.repeat_freq_daily to Frequency.DAILY,
                    R.string.repeat_freq_weekly to Frequency.WEEKLY,
                    R.string.repeat_freq_monthly to Frequency.MONTHLY,
                    R.string.repeat_freq_yearly to Frequency.YEARLY,
                )
                options.forEach { (label, frequency) ->
                    DropdownMenuItem(
                        text = { Text(stringResource(label)) },
                        onClick = {
                            repeatMenu = false
                            onRepeat(frequency)
                        },
                    )
                }
            }
        }

        IconButton(onClick = onVoice) {
            Icon(painterResource(R.drawable.ic_mic), stringResource(R.string.voice_input))
        }
    }

    if (newTagDialog) {
        var name by rememberSaveable { mutableStateOf("") }
        val clean = name.trim().trimStart('#').trim()
        AlertDialog(
            onDismissRequest = { newTagDialog = false },
            title = { Text(stringResource(R.string.new_tag)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.tag_name)) },
                )
            },
            confirmButton = {
                TextButton(
                    enabled = clean.isNotEmpty(),
                    onClick = {
                        onTagToggle(clean)
                        newTagDialog = false
                    },
                ) { Text(stringResource(R.string.action_add)) }
            },
            dismissButton = {
                TextButton(onClick = { newTagDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(selectableDates = com.elkrrai.feather.feature.taskedit.TodayOrLater)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            // The picker works in UTC midnights; the date written is the day that was tapped.
                            val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            onDate(date)
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

/** The tools' choices on top of what the text said; a repeat with no date starts today, tags are added. */
private fun ParsedQuickAdd.withTools(
    date: LocalDate?,
    priority: Priority?,
    repeat: Frequency?,
    extraTags: List<String>,
): ParsedQuickAdd {
    var result = this
    if (extraTags.isNotEmpty()) {
        result = result.copy(tags = (result.tags + extraTags).distinctBy { it.lowercase() })
    }
    if (date != null) result = result.copy(dueDate = date, dateIsImplied = false)
    if (priority != null) result = result.copy(priority = priority)
    if (repeat != null) {
        val due = result.dueDate ?: LocalDate.now()
        result = result.copy(
            dueDate = due,
            dateIsImplied = result.dueDate == null || result.dateIsImplied,
            recurrence = Recurrence(repeat),
        )
    }
    return result
}
