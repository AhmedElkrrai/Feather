package com.elkrrai.feather.feature.taskedit

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.model.Priority
import com.elkrrai.feather.domain.model.Subtask
import com.elkrrai.feather.domain.model.Tag
import com.elkrrai.feather.domain.model.TaskList
import com.elkrrai.feather.domain.recurrence.Recurrence
import com.elkrrai.feather.feature.tasklist.NameDialog
import com.elkrrai.feather.domain.text.findLinks
import com.elkrrai.feather.reminders.ReminderHealth
import com.elkrrai.feather.reminders.startActivitySafely
import com.elkrrai.feather.ui.components.MenuAction
import com.elkrrai.feather.ui.components.OverflowButton
import com.elkrrai.feather.ui.components.DurationDialog
import com.elkrrai.feather.ui.components.TimeOfDayDialog
import com.elkrrai.feather.ui.components.rememberReminderHealth
import com.elkrrai.feather.ui.format.displayName
import com.elkrrai.feather.ui.format.formatDueDate
import com.elkrrai.feather.ui.format.formatDueTime
import com.elkrrai.feather.ui.format.label
import com.elkrrai.feather.ui.format.offsetLabel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import androidx.compose.ui.geometry.Offset
import com.elkrrai.feather.ui.components.FeatherConfettiOverlay
import com.elkrrai.feather.ui.components.MiniRing
import com.elkrrai.feather.ui.components.rememberConfettiController

/** Everything the editor can ask its ViewModel to do. */
class TaskEditActions(
    val onBack: () -> Unit,
    val onSave: () -> Unit,
    val onTitleChange: (String) -> Unit,
    val onNotesChange: (String) -> Unit,
    val onDueDateChange: (LocalDate?) -> Unit,
    val onDueTimeChange: (LocalTime?) -> Unit,
    val onPriorityChange: (Priority) -> Unit,
    val onListChange: (String) -> Unit,
    val onTagToggle: (Tag) -> Unit,
    val onCreateTag: (String) -> Unit,
    val onPinnedChange: (Boolean) -> Unit,
    val onAddSubtask: (String) -> Unit,
    val onSubtaskToggle: (String) -> Unit,
    val onSubtaskTitleChange: (id: String, title: String) -> Unit,
    val onSubtaskRemove: (String) -> Unit,
    val onAddBeforeDue: (minutes: Int) -> Unit,
    val onAddAtTime: (LocalDateTime) -> Unit,
    val onRemoveReminder: (String) -> Unit,
    val onOpenReliability: () -> Unit,
    val onRecurrenceChange: (Recurrence?) -> Unit = {},
    val onDuplicate: () -> Unit = {},
)

@Composable
fun TaskEditScreen(
    onBack: () -> Unit,
    onOpenReliability: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenTask: (taskId: String) -> Unit = {},
    viewModel: TaskEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val health by rememberReminderHealth()
    val context = LocalContext.current

    // Asked for when the user starts adding a reminder; the answer shows up in [health] on return.
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    val requestNotificationPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(state.finished) {
        if (state.finished) onBack()
    }
    LaunchedEffect(state.duplicatedId) {
        state.duplicatedId?.let(onOpenTask)
    }
    val copyTitle = stringResource(R.string.duplicate_title, state.title)

    TaskEditContent(
        state = state,
        actions = TaskEditActions(
            onBack = onBack,
            onSave = viewModel::save,
            onTitleChange = viewModel::onTitleChange,
            onNotesChange = viewModel::onNotesChange,
            onDueDateChange = viewModel::onDueDateChange,
            onDueTimeChange = viewModel::onDueTimeChange,
            onPriorityChange = viewModel::onPriorityChange,
            onListChange = viewModel::onListChange,
            onTagToggle = viewModel::onTagToggle,
            onCreateTag = viewModel::onCreateTag,
            onPinnedChange = viewModel::onPinnedChange,
            onAddSubtask = viewModel::onAddSubtask,
            onSubtaskToggle = viewModel::onSubtaskToggle,
            onSubtaskTitleChange = viewModel::onSubtaskTitleChange,
            onSubtaskRemove = viewModel::onSubtaskRemove,
            onAddBeforeDue = viewModel::onAddBeforeDue,
            onAddAtTime = viewModel::onAddAtTime,
            onRemoveReminder = viewModel::onRemoveReminder,
            onOpenReliability = onOpenReliability,
            onRecurrenceChange = viewModel::onRecurrenceChange,
            onDuplicate = { viewModel.duplicate(copyTitle) },
        ),
        health = health,
        onRequestNotificationPermission = requestNotificationPermission,
        modifier = modifier,
    )
}

/** Stateless editor, so it can be previewed and screenshot-tested. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditContent(
    state: TaskEditUiState,
    actions: TaskEditActions,
    modifier: Modifier = Modifier,
    health: ReminderHealth = ReminderHealth(),
    onRequestNotificationPermission: () -> Unit = {},
    today: LocalDate = LocalDate.now(),
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showListPicker by rememberSaveable { mutableStateOf(false) }
    var showRecurrence by rememberSaveable { mutableStateOf(false) }
    var showNewTag by rememberSaveable { mutableStateOf(false) }
    var reminderStep by remember { mutableStateOf<ReminderStep>(ReminderStep.None) }
    val confetti = rememberConfettiController()

    Box(modifier = modifier) {
    Scaffold(
        modifier = Modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.isNew) R.string.edit_title_new else R.string.edit_title_existing
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        // The drawable is autoMirrored, so the arrow flips in RTL languages.
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    if (!state.isNew) {
                        OverflowButton(
                            actions = listOf(
                                MenuAction(stringResource(R.string.action_duplicate)) { actions.onDuplicate() },
                            ),
                        )
                    }
                    TextButton(onClick = actions.onSave, enabled = state.canSave) {
                        Text(stringResource(R.string.action_save))
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = actions.onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.field_title)) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
            )

            OutlinedTextField(
                value = state.notes,
                onValueChange = actions.onNotesChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.field_notes)) },
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )

            // Web addresses in the notes can be opened from here; the field itself stays plain text.
            val links = remember(state.notes) { findLinks(state.notes) }
            if (links.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    links.forEach { link ->
                        AssistChip(
                            onClick = {
                                context.startActivitySafely(Intent(Intent.ACTION_VIEW, Uri.parse(link.url)))
                            },
                            label = { Text(link.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_link),
                                    contentDescription = stringResource(R.string.open_link, link.label),
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                        )
                    }
                }
            }

            FieldSection(label = stringResource(R.string.field_list)) {
                OutlinedButton(onClick = { showListPicker = true }) {
                    Text(
                        state.availableLists.firstOrNull { it.id == state.listId }?.displayName()
                            ?: stringResource(R.string.inbox)
                    )
                }
            }

            FieldSection(label = stringResource(R.string.field_due_date)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(onClick = { showDatePicker = true }) {
                        Text(
                            state.dueDate?.let { formatDueDate(context, it, today, locale) }
                                ?: stringResource(R.string.set_date)
                        )
                    }
                    if (state.dueDate != null) {
                        TextButton(onClick = { actions.onDueDateChange(null) }) {
                            Text(stringResource(R.string.action_clear))
                        }
                    }
                }
            }

            FieldSection(label = stringResource(R.string.field_due_time)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // A time needs a date to attach to.
                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        enabled = state.dueDate != null,
                    ) {
                        Text(
                            state.dueTime?.let { formatDueTime(it, locale) }
                                ?: stringResource(R.string.set_time)
                        )
                    }
                    if (state.dueTime != null) {
                        TextButton(onClick = { actions.onDueTimeChange(null) }) {
                            Text(stringResource(R.string.action_clear))
                        }
                    }
                }
            }

            FieldSection(label = stringResource(R.string.field_repeat)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(onClick = { showRecurrence = true }) {
                        Text(
                            state.recurrence?.label(state.dueDate ?: today)
                                ?: stringResource(R.string.repeat_none)
                        )
                    }
                    if (state.recurrence != null) {
                        TextButton(onClick = { actions.onRecurrenceChange(null) }) {
                            Text(stringResource(R.string.action_clear))
                        }
                    }
                }
            }

            FieldSection(label = stringResource(R.string.field_reminders)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (state.reminders.isNotEmpty() && !health.canDeliverOnTime) {
                        ReminderWarning(onFix = actions.onOpenReliability)
                    }
                    state.reminders.forEach { reminder ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(reminder.label(today), style = MaterialTheme.typography.bodyLarge)
                            IconButton(onClick = { actions.onRemoveReminder(reminder.id) }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_close),
                                    contentDescription = stringResource(R.string.remove_reminder),
                                )
                            }
                        }
                    }
                    AssistChip(
                        onClick = {
                            onRequestNotificationPermission()
                            reminderStep = ReminderStep.Picker
                        },
                        label = { Text(stringResource(R.string.add_reminder)) },
                    )
                }
            }

            FieldSection(label = stringResource(R.string.field_priority)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Priority.entries.forEach { priority ->
                        FilterChip(
                            selected = state.priority == priority,
                            onClick = { actions.onPriorityChange(priority) },
                            label = { Text(priority.label()) },
                        )
                    }
                }
            }

            FieldSection(label = stringResource(R.string.field_tags)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.availableTags.forEach { tag ->
                        FilterChip(
                            selected = state.tags.any { it.id == tag.id },
                            onClick = { actions.onTagToggle(tag) },
                            label = { Text("#${tag.name}") },
                        )
                    }
                    AssistChip(
                        onClick = { showNewTag = true },
                        label = { Text(stringResource(R.string.new_tag)) },
                    )
                }
            }

            FieldSection(label = stringResource(R.string.field_subtasks)) {
                SubtasksEditor(state.subtasks, actions, onCelebrate = confetti::celebrate)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.pin_to_top), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = state.isPinned, onCheckedChange = actions.onPinnedChange)
            }
        }
    }
    FeatherConfettiOverlay(confetti)
    }

    if (showDatePicker) {
        DueDatePickerDialog(
            initial = state.dueDate ?: today,
            onConfirm = {
                actions.onDueDateChange(it)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }

    if (showTimePicker) {
        TimeOfDayDialog(
            initial = state.dueTime ?: LocalTime.of(9, 0),
            onConfirm = {
                actions.onDueTimeChange(it)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        )
    }

    if (showRecurrence) {
        RecurrenceDialog(
            initial = state.recurrence,
            reference = state.dueDate ?: today,
            onConfirm = {
                actions.onRecurrenceChange(it)
                showRecurrence = false
            },
            onDismiss = { showRecurrence = false },
        )
    }

    ReminderDialogs(
        step = reminderStep,
        hasDueDate = state.dueDate != null,
        today = today,
        onStepChange = { reminderStep = it },
        onAddBeforeDue = actions.onAddBeforeDue,
        onAddAtTime = actions.onAddAtTime,
    )

    if (showListPicker) {
        ListPickerDialog(
            lists = state.availableLists,
            selectedId = state.listId,
            onSelect = {
                actions.onListChange(it)
                showListPicker = false
            },
            onDismiss = { showListPicker = false },
        )
    }

    if (showNewTag) {
        NameDialog(
            title = stringResource(R.string.new_tag),
            label = stringResource(R.string.tag_name),
            initial = "",
            confirmLabel = stringResource(R.string.action_create),
            onConfirm = {
                actions.onCreateTag(it)
                showNewTag = false
            },
            onDismiss = { showNewTag = false },
        )
    }
}

@Composable
private fun FieldSection(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}

/** The checklist: existing items (editable, removable) plus a field to add another. */
@Composable
private fun SubtasksEditor(
    subtasks: List<Subtask>,
    actions: TaskEditActions,
    onCelebrate: (Offset, Float) -> Unit,
) {
    var newTitle by rememberSaveable { mutableStateOf("") }

    fun addNew() {
        if (newTitle.isBlank()) return
        actions.onAddSubtask(newTitle)
        newTitle = ""
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        subtasks.forEach { subtask ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                MiniRing(
                    checked = subtask.isCompleted,
                    onCheckedChange = { actions.onSubtaskToggle(subtask.id) },
                    contentDescription = subtask.title,
                    onCelebrate = { origin -> onCelebrate(origin, 0.6f) },
                )
                OutlinedTextField(
                    value = subtask.title,
                    onValueChange = { actions.onSubtaskTitleChange(subtask.id, it) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                IconButton(onClick = { actions.onSubtaskRemove(subtask.id) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.remove_subtask),
                    )
                }
            }
        }
        OutlinedTextField(
            value = newTitle,
            onValueChange = { newTitle = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.add_subtask)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { addNew() }),
            trailingIcon = {
                if (newTitle.isNotBlank()) {
                    IconButton(onClick = ::addNew) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = stringResource(R.string.add_subtask),
                        )
                    }
                }
            },
        )
    }
}

@Composable
internal fun ListPickerDialog(
    lists: List<TaskList>,
    selectedId: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.choose_list)) },
        text = {
            Column(Modifier.selectableGroup().verticalScroll(rememberScrollState())) {
                lists.forEach { list ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = list.id == selectedId,
                                onClick = { onSelect(list.id) },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = list.id == selectedId, onClick = null)
                        Spacer(Modifier.width(16.dp))
                        Text(list.displayName())
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** The Material date picker works in UTC millis; a due date is a plain local date. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DueDatePickerDialog(
    initial: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onConfirm(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                },
                enabled = pickerState.selectedDateMillis != null,
            ) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    ) {
        DatePicker(state = pickerState)
    }
}

/** Where the "add reminder" flow currently is. */
private sealed interface ReminderStep {
    data object None : ReminderStep
    data object Picker : ReminderStep
    data object CustomOffset : ReminderStep
    data object PickDate : ReminderStep
    data class PickTime(val date: LocalDate) : ReminderStep
}

/** Offsets offered as one-tap choices, in minutes before the due moment. */
private val offsetPresets = listOf(0, 10, 30, 60, 24 * 60)

@Composable
private fun ReminderDialogs(
    step: ReminderStep,
    hasDueDate: Boolean,
    today: LocalDate,
    onStepChange: (ReminderStep) -> Unit,
    onAddBeforeDue: (Int) -> Unit,
    onAddAtTime: (LocalDateTime) -> Unit,
) {
    val close = { onStepChange(ReminderStep.None) }
    when (step) {
        ReminderStep.None -> Unit

        ReminderStep.Picker -> AlertDialog(
            onDismissRequest = close,
            title = { Text(stringResource(R.string.add_reminder)) },
            text = {
                Column {
                    if (!hasDueDate) {
                        Text(
                            text = stringResource(R.string.reminder_needs_due_date),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    offsetPresets.forEach { minutes ->
                        PickerRow(offsetLabel(minutes), enabled = hasDueDate) {
                            onAddBeforeDue(minutes)
                            close()
                        }
                    }
                    PickerRow(stringResource(R.string.reminder_custom_before), enabled = hasDueDate) {
                        onStepChange(ReminderStep.CustomOffset)
                    }
                    PickerRow(stringResource(R.string.reminder_at_specific_time), enabled = true) {
                        onStepChange(ReminderStep.PickDate)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = close) { Text(stringResource(R.string.action_cancel)) }
            },
        )

        ReminderStep.CustomOffset -> DurationDialog(
            title = stringResource(R.string.reminder_custom_title),
            confirmLabel = stringResource(R.string.action_add),
            onConfirm = { minutes ->
                onAddBeforeDue(minutes.toInt())
                close()
            },
            onDismiss = close,
        )

        ReminderStep.PickDate -> DueDatePickerDialog(
            initial = today,
            onConfirm = { onStepChange(ReminderStep.PickTime(it)) },
            onDismiss = close,
        )

        is ReminderStep.PickTime -> TimeOfDayDialog(
            initial = LocalTime.of(9, 0),
            onConfirm = {
                onAddAtTime(LocalDateTime.of(step.date, it))
                close()
            },
            onDismiss = close,
        )
    }
}

@Composable
private fun PickerRow(label: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color = if (enabled) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
    )
}

/** Shown above the reminders when the phone is set up so that they might not arrive on time. */
@Composable
private fun ReminderWarning(onFix: () -> Unit) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.reminder_warning),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onFix) { Text(stringResource(R.string.reminder_warning_fix)) }
        }
    }
}
