package com.elkrrai.feather.feature.habits

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.habit.DAYS_IN_WEEK
import com.elkrrai.feather.domain.habit.DayProgress
import com.elkrrai.feather.domain.habit.HabitKind
import com.elkrrai.feather.domain.habit.HabitState
import com.elkrrai.feather.domain.habit.MAX_DAILY_TARGET
import com.elkrrai.feather.domain.habit.MAX_HABIT_TARGET
import com.elkrrai.feather.domain.habit.canLog
import com.elkrrai.feather.domain.habit.forWeek
import com.elkrrai.feather.domain.habit.queuedAfter
import com.elkrrai.feather.domain.habit.week
import com.elkrrai.feather.domain.habit.weekStartOf
import com.elkrrai.feather.domain.habit.weeksMetInARow
import com.elkrrai.feather.domain.view.TaskScope
import com.elkrrai.feather.feature.tasklist.ConfirmDialog
import com.elkrrai.feather.feature.tasklist.DrawerActions
import com.elkrrai.feather.feature.tasklist.TaskDrawerContent
import com.elkrrai.feather.feature.tasklist.TaskListUiState
import com.elkrrai.feather.ui.brand.BirdMood
import com.elkrrai.feather.ui.components.EmptyState
import com.elkrrai.feather.ui.components.MenuAction
import com.elkrrai.feather.ui.components.OverflowButton
import com.elkrrai.feather.ui.components.SubtaskProgressBar
import com.elkrrai.feather.ui.firstDayOfWeek
import com.elkrrai.feather.ui.rememberHaptics
import com.elkrrai.feather.ui.theme.Space
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * The Habits screen: a card for each habit with its week, where days are logged. It is one of the
 * places the menu leads to, like Today and Goals, so it has the same menu (the burger) and not a back
 * arrow. [listState] is the task list's state, for the counts in the menu.
 */
@Composable
fun HabitsScreen(
    listState: TaskListUiState,
    onSelectScope: (TaskScope) -> Unit,
    onOpenTrash: () -> Unit,
    onOpenRhythm: () -> Unit,
    onOpenNest: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HabitsViewModel = hiltViewModel(),
) {
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    fun closeDrawer() {
        coroutineScope.launch { drawerState.close() }
    }

    // Back closes the menu first, before it leaves the screen.
    BackHandler(enabled = drawerState.currentValue == DrawerValue.Open) { closeDrawer() }

    val drawerActions = DrawerActions(
        onSelect = onSelectScope,
        onOpenTrash = onOpenTrash,
        onOpenHabits = ::closeDrawer,
        onOpenSettings = onOpenSettings,
        onOpenRhythm = onOpenRhythm,
        onOpenNest = onOpenNest,
        onClose = ::closeDrawer,
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                TaskDrawerContent(
                    state = listState,
                    actions = drawerActions,
                    highlightScope = false,
                    habitsSelected = true,
                )
            }
        },
        modifier = modifier,
    ) {
        HabitsContent(
            habits = habits,
            onMenuClick = { coroutineScope.launch { drawerState.open() } },
            onAdd = viewModel::add,
            onEdit = viewModel::edit,
            onSetAmount = viewModel::setAmount,
            onDelete = viewModel::delete,
        )
    }
}

private sealed interface HabitDialog {
    data object New : HabitDialog
    data class Edit(val id: String) : HabitDialog
    data class Delete(val id: String) : HabitDialog
    data class SetAmount(val id: String, val date: LocalDate) : HabitDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HabitsContent(
    habits: List<HabitState>?,
    onMenuClick: () -> Unit,
    onAdd: (String, HabitKind, String, Int, Int, LocalDate) -> Unit,
    onEdit: (String, String, String, Int, Int, Boolean, LocalDate) -> Unit,
    onSetAmount: (String, LocalDate, Int) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
) {
    val locale = LocalConfiguration.current.locales[0]
    val firstDay = firstDayOfWeek(locale)
    val weekStart = weekStartOf(today, firstDay)
    val nextWeekStart = weekStart.plusWeeks(1)
    var dialog by remember { mutableStateOf<HabitDialog?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.habits_title), style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(painterResource(R.drawable.ic_menu), stringResource(R.string.action_open_menu))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { dialog = HabitDialog.New }) {
                Icon(painterResource(R.drawable.ic_add), stringResource(R.string.habit_new))
            }
        },
    ) { padding ->
        when {
            habits == null -> Box(Modifier.fillMaxSize().padding(padding))

            habits.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding)) {
                EmptyState(
                    title = stringResource(R.string.habits_empty_title),
                    body = stringResource(R.string.habits_empty_body),
                    mood = BirdMood.IDLE,
                )
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Space.gutter,
                    end = Space.gutter,
                    top = padding.calculateTopPadding() + Space.x2,
                    bottom = padding.calculateBottomPadding() + 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(Space.x3),
            ) {
                items(habits, key = { it.habit.id }) { state ->
                    HabitCard(
                        state = state,
                        today = today,
                        weekStart = weekStart,
                        locale = locale,
                        onLog = { date, amount -> onSetAmount(state.habit.id, date, amount) },
                        onPickAmount = { date -> dialog = HabitDialog.SetAmount(state.habit.id, date) },
                        onEdit = { dialog = HabitDialog.Edit(state.habit.id) },
                        onDelete = { dialog = HabitDialog.Delete(state.habit.id) },
                    )
                }
            }
        }
    }

    when (val open = dialog) {
        null -> Unit

        HabitDialog.New -> HabitEditDialog(
            existing = null,
            weekStart = weekStart,
            onSave = { name, kind, unit, target, daily ->
                onAdd(name, kind, unit, target, daily, weekStart)
                dialog = null
            },
            onDismiss = { dialog = null },
        )

        is HabitDialog.Edit -> habits?.firstOrNull { it.habit.id == open.id }?.let { state ->
            HabitEditDialog(
                existing = state,
                weekStart = weekStart,
                onSave = { name, _, unit, target, daily ->
                    val shown = state.goals.queuedAfter(weekStart) ?: state.goals.forWeek(weekStart)
                    val changed = shown == null || shown.target != target || shown.dailyTarget != daily
                    onEdit(state.habit.id, name, unit, target, daily, changed, nextWeekStart)
                    dialog = null
                },
                onDismiss = { dialog = null },
            )
        }

        is HabitDialog.Delete -> habits?.firstOrNull { it.habit.id == open.id }?.let { state ->
            ConfirmDialog(
                title = stringResource(R.string.habit_delete_title),
                message = stringResource(R.string.habit_delete_message, state.habit.name),
                onConfirm = {
                    onDelete(state.habit.id)
                    dialog = null
                },
                onDismiss = { dialog = null },
            )
        }

        is HabitDialog.SetAmount -> habits?.firstOrNull { it.habit.id == open.id }?.let { state ->
            AmountDialog(
                title = stringResource(
                    R.string.habit_amount_title,
                    state.habit.unit.ifBlank { state.habit.name },
                    open.date.dayOfWeek.getDisplayName(TextStyle.FULL, locale),
                ),
                initial = state.entries[open.date] ?: 0,
                onSet = { amount ->
                    onSetAmount(state.habit.id, open.date, amount)
                    dialog = null
                },
                onDismiss = { dialog = null },
            )
        }
    }
}

/** One habit: its name, how far this week is, the seven days to log, and a quiet line about weeks in a row. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HabitCard(
    state: HabitState,
    today: LocalDate,
    weekStart: LocalDate,
    locale: Locale,
    onLog: (LocalDate, Int) -> Unit,
    onPickAmount: (LocalDate) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val progress = state.week(weekStart)
    val goal = progress.goal
    val queued = state.goals.queuedAfter(weekStart)
    val inARow = state.weeksMetInARow(weekStart)
    val daily = goal?.dailyTarget ?: 1

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, scheme.outlineVariant),
    ) {
        Column(Modifier.padding(start = Space.x4, end = Space.x1, top = Space.x2, bottom = Space.x4)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = state.habit.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).padding(vertical = Space.x2),
                )
                OverflowButton(
                    actions = listOf(
                        MenuAction(stringResource(R.string.habit_edit)) { onEdit() },
                        MenuAction(stringResource(R.string.action_delete)) { onDelete() },
                    ),
                )
            }
            Column(Modifier.padding(end = Space.x3)) {
                Text(
                    text = if (state.habit.kind == HabitKind.DAYS) {
                        stringResource(R.string.habit_progress_days, progress.total, progress.target)
                    } else {
                        stringResource(R.string.habit_progress_amount, progress.total, progress.target, state.habit.unit).trim()
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
                if (state.habit.kind == HabitKind.DAYS && daily > 1) {
                    Text(
                        text = stringResource(R.string.habit_daily_needs, daily),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(Space.x2))
                SubtaskProgressBar(done = progress.total.coerceAtMost(progress.target), total = progress.target)
                Spacer(Modifier.height(Space.x3))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    progress.days.forEach { day ->
                        DayCell(
                            day = day,
                            isToday = day.date == today,
                            enabled = canLog(day.date, weekStart, today),
                            dailyTarget = daily,
                            amountHabit = state.habit.kind == HabitKind.AMOUNT,
                            label = day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                            fullName = day.date.dayOfWeek.getDisplayName(TextStyle.FULL, locale),
                            onTap = {
                                when {
                                    state.habit.kind == HabitKind.AMOUNT -> onPickAmount(day.date)
                                    daily <= 1 -> onLog(day.date, if (day.amount > 0) 0 else 1)
                                    else -> onLog(day.date, if (day.amount >= daily) 0 else day.amount + 1)
                                }
                            },
                            onLongTap = { onPickAmount(day.date) },
                        )
                    }
                }
                if (inARow >= 2) {
                    Spacer(Modifier.height(Space.x3))
                    Text(
                        text = stringResource(R.string.habit_weeks_in_a_row, inARow),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                if (queued != null) {
                    Spacer(Modifier.height(Space.x1))
                    Text(
                        text = if (state.habit.kind == HabitKind.DAYS) {
                            stringResource(R.string.habit_next_week_days, queued.target, queued.dailyTarget)
                        } else {
                            stringResource(R.string.habit_next_week_amount, queued.target, state.habit.unit).trim()
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** A day of the week: a round mark that is filled when the day counts, with the weekday letter under it. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayCell(
    day: DayProgress,
    isToday: Boolean,
    enabled: Boolean,
    dailyTarget: Int,
    amountHabit: Boolean,
    label: String,
    fullName: String,
    onTap: () -> Unit,
    onLongTap: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = rememberHaptics()
    val description = if (day.done) {
        stringResource(R.string.habit_day_done, fullName)
    } else {
        stringResource(R.string.habit_day_open, fullName)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .semantics { contentDescription = description }
            .combinedClickable(
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    haptics.tick()
                    onTap()
                },
                onLongClick = {
                    haptics.longPress()
                    onLongTap()
                },
            )
            .padding(vertical = Space.x1),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(if (day.done) scheme.primary else scheme.surface, CircleShape)
                .then(
                    if (day.done) {
                        Modifier
                    } else {
                        Modifier.border(BorderStroke(1.5.dp, if (isToday) scheme.primary else scheme.outlineVariant), CircleShape)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            when {
                day.done && !amountHabit && dailyTarget <= 1 ->
                    Icon(painterResource(R.drawable.ic_check), null, Modifier.size(20.dp), tint = scheme.onPrimary)

                day.amount > 0 -> Text(
                    text = day.amount.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (day.done) scheme.onPrimary else scheme.onSurface,
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (enabled) scheme.onSurfaceVariant else scheme.onSurfaceVariant.copy(alpha = 0.4f),
        )
    }
}

/** The amount logged on a day, as a number to set (0 clears the day). */
@Composable
private fun AmountDialog(title: String, initial: Int, onSet: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(if (initial > 0) initial.toString() else "") }
    val value = text.toIntOrNull() ?: if (text.isEmpty()) 0 else null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter(Char::isDigit).trimStart('0').take(5) },
                label = { Text(stringResource(R.string.habit_amount_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { value?.let(onSet) }, enabled = value != null) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/**
 * Making a habit, or editing one. The kind is chosen once. When editing, the name and the unit
 * change at once; a new goal waits for the start of next week, and the dialog says so.
 */
@Composable
private fun HabitEditDialog(
    existing: HabitState?,
    weekStart: LocalDate,
    onSave: (name: String, kind: HabitKind, unit: String, target: Int, dailyTarget: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val shownGoal = existing?.let { it.goals.queuedAfter(weekStart) ?: it.goals.forWeek(weekStart) }
    var name by rememberSaveable { mutableStateOf(existing?.habit?.name.orEmpty()) }
    var kind by rememberSaveable { mutableStateOf(existing?.habit?.kind ?: HabitKind.DAYS) }
    var unit by rememberSaveable { mutableStateOf(existing?.habit?.unit.orEmpty()) }
    var target by rememberSaveable { mutableStateOf(shownGoal?.target?.toString() ?: if (kind == HabitKind.DAYS) "3" else "20") }
    var daily by rememberSaveable { mutableStateOf(shownGoal?.dailyTarget?.toString() ?: "1") }

    val targetValue = target.toIntOrNull()
    val dailyValue = daily.toIntOrNull() ?: 1
    val maxTarget = if (kind == HabitKind.DAYS) DAYS_IN_WEEK else MAX_HABIT_TARGET
    val valid = name.isNotBlank() && targetValue != null && targetValue in 1..maxTarget && dailyValue in 1..MAX_DAILY_TARGET

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (existing == null) R.string.habit_new else R.string.habit_edit)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Space.x2)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.habit_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (existing == null) {
                    Column(Modifier.selectableGroup()) {
                        KindChoice(R.string.habit_kind_days, kind == HabitKind.DAYS) {
                            kind = HabitKind.DAYS
                            if ((target.toIntOrNull() ?: 0) > DAYS_IN_WEEK) target = "3"
                        }
                        KindChoice(R.string.habit_kind_amount, kind == HabitKind.AMOUNT) { kind = HabitKind.AMOUNT }
                    }
                    // an example of what the chosen kind looks like
                    Text(
                        text = stringResource(if (kind == HabitKind.DAYS) R.string.habit_example_days else R.string.habit_example_amount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it.filter(Char::isDigit).trimStart('0').take(4) },
                    label = {
                        Text(stringResource(if (kind == HabitKind.DAYS) R.string.habit_target_days else R.string.habit_target_amount))
                    },
                    isError = targetValue == null || targetValue !in 1..maxTarget,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (kind == HabitKind.DAYS) {
                    OutlinedTextField(
                        value = daily,
                        onValueChange = { daily = it.filter(Char::isDigit).trimStart('0').take(2) },
                        label = { Text(stringResource(R.string.habit_daily_target)) },
                        supportingText = { Text(stringResource(R.string.habit_daily_target_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it.take(20) },
                        label = { Text(stringResource(R.string.habit_unit)) },
                        supportingText = { Text(stringResource(R.string.habit_unit_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (existing != null) {
                    Text(
                        text = stringResource(R.string.habit_goal_next_week),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (valid) onSave(name, kind, unit, targetValue ?: 1, dailyValue) },
                enabled = valid,
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun KindChoice(label: Int, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = Space.x2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(text = stringResource(label), modifier = Modifier.padding(start = Space.x3), textAlign = TextAlign.Start)
    }
}
