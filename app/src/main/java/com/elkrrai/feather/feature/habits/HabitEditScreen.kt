package com.elkrrai.feather.feature.habits

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.habit.DayTime
import com.elkrrai.feather.domain.habit.HabitKind
import com.elkrrai.feather.domain.habit.StageMode
import com.elkrrai.feather.ui.components.FieldSection
import com.elkrrai.feather.ui.components.FormHint
import com.elkrrai.feather.ui.components.ReminderWarning
import com.elkrrai.feather.ui.components.SwitchRow
import com.elkrrai.feather.ui.components.rememberLeaveGuard
import com.elkrrai.feather.ui.components.rememberReminderHealth

/** What the habit form can change; the stateful screen supplies them. */
internal class HabitEditActions(
    val onBack: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onNameChange: (String) -> Unit = {},
    val onKindChange: (HabitKind) -> Unit = {},
    val onTargetChange: (String) -> Unit = {},
    val onDailyChange: (String) -> Unit = {},
    val onUnitChange: (String) -> Unit = {},
    val onNamedChange: (Boolean) -> Unit = {},
    val onStageName: (key: Long, name: String) -> Unit = { _, _ -> },
    val onStageTime: (key: Long, time: DayTime) -> Unit = { _, _ -> },
    val onStageMove: (key: Long, by: Int) -> Unit = { _, _ -> },
    val onStageRemove: (key: Long) -> Unit = {},
    val onStageAdd: () -> Long? = { null },
    val onNeededChange: (String) -> Unit = {},
    val onShowInTodayChange: (Boolean) -> Unit = {},
    val onTodayFromStartChange: (Boolean) -> Unit = {},
    val onStageModeChange: (StageMode) -> Unit = {},
    val onTimeChange: (DayTime) -> Unit = {},
    val onRemindChange: (Boolean) -> Unit = {},
    val onRequestNotificationPermission: () -> Unit = {},
    val onOpenReliability: () -> Unit = {},
)

/** The screen that makes a habit or edits one. It replaces the dialog the Habits screen used to open. */
@Composable
fun HabitEditScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenReliability: () -> Unit = {},
    viewModel: HabitEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Asked for when a reminder is switched on; the answer shows up in the reminder health on return.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val requestNotificationPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    LaunchedEffect(state.finished) { if (state.finished) onBack() }
    val hasChanges by viewModel.hasChanges.collectAsStateWithLifecycle()
    val leave = rememberLeaveGuard(hasChanges && !state.finished, onBack)
    HabitEditContent(
        state = state,
        actions = HabitEditActions(
            onBack = leave,
            onSave = viewModel::save,
            onNameChange = viewModel::onNameChange,
            onKindChange = viewModel::onKindChange,
            onTargetChange = viewModel::onTargetChange,
            onDailyChange = viewModel::onDailyChange,
            onUnitChange = viewModel::onUnitChange,
            onNamedChange = viewModel::onNamedChange,
            onStageName = viewModel::onStageNameChange,
            onStageTime = viewModel::onStageTimeChange,
            onStageMove = viewModel::moveStage,
            onStageRemove = viewModel::removeStage,
            onStageAdd = viewModel::addStage,
            onNeededChange = viewModel::onNeededChange,
            onShowInTodayChange = viewModel::onShowInTodayChange,
            onTodayFromStartChange = viewModel::onTodayFromStartChange,
            onStageModeChange = viewModel::onStageModeChange,
            onTimeChange = viewModel::onTimeChange,
            onRemindChange = viewModel::onRemindChange,
            onRequestNotificationPermission = requestNotificationPermission,
            onOpenReliability = onOpenReliability,
        ),
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun HabitEditContent(
    state: HabitEditUiState,
    actions: HabitEditActions,
    modifier: Modifier = Modifier,
) {
    val days = state.kind == HabitKind.DAYS
    val health by rememberReminderHealth()
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isNew) R.string.habit_new else R.string.habit_edit)) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                },
                actions = {
                    TextButton(onClick = actions.onSave, enabled = state.canSave) { Text(stringResource(R.string.action_save)) }
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
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = actions.onNameChange,
                label = { Text(stringResource(R.string.habit_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            FieldSection(label = stringResource(R.string.habit_type_label)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = days,
                        onClick = { actions.onKindChange(HabitKind.DAYS) },
                        enabled = state.isNew,
                        label = { Text(stringResource(R.string.habit_kind_days)) },
                    )
                    FilterChip(
                        selected = !days,
                        onClick = { actions.onKindChange(HabitKind.AMOUNT) },
                        enabled = state.isNew,
                        label = { Text(stringResource(R.string.habit_kind_amount)) },
                    )
                }
                FormHint(stringResource(if (state.isNew) R.string.habit_type_hint else R.string.habit_type_locked_hint))
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = state.target,
                    onValueChange = actions.onTargetChange,
                    label = { Text(stringResource(if (days) R.string.habit_target_days else R.string.habit_target_amount)) },
                    isError = !state.targetValid,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                FormHint(stringResource(if (days) R.string.habit_target_days_hint else R.string.habit_target_amount_hint))
                if (!state.isNew) FormHint(stringResource(R.string.habit_goal_next_week))
            }

            if (days) {
                // with named times the count of a day is how many of them, so the plain number is not asked
                if (!state.staged) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = state.daily,
                            onValueChange = actions.onDailyChange,
                            label = { Text(stringResource(R.string.habit_daily_target)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        FormHint(stringResource(R.string.habit_daily_target_hint))
                    }
                }

                SwitchRow(
                    label = stringResource(R.string.habit_named_label),
                    checked = state.named,
                    onCheckedChange = actions.onNamedChange,
                    hint = stringResource(R.string.habit_named_hint),
                )

                if (state.staged) {
                    FieldSection(label = stringResource(R.string.habit_stages_label)) {
                        StageList(
                            stages = state.stages,
                            onName = actions.onStageName,
                            onTime = actions.onStageTime,
                            onMove = actions.onStageMove,
                            onRemove = actions.onStageRemove,
                            onAdd = actions.onStageAdd,
                        )
                        FormHint(stringResource(R.string.habit_stages_hint))
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = state.needed ?: state.namedStages.size.toString(),
                            onValueChange = actions.onNeededChange,
                            label = { Text(stringResource(R.string.habit_needed_label)) },
                            isError = !state.neededValid,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        FormHint(stringResource(R.string.habit_needed_hint, state.namedStages.size))
                    }
                }
            }

            Text(
                text = stringResource(R.string.habit_today_section),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )

            SwitchRow(
                label = stringResource(R.string.habit_today_show),
                checked = state.showInToday,
                onCheckedChange = actions.onShowInTodayChange,
                hint = stringResource(R.string.habit_today_show_hint),
            )

            if (state.showInToday) {
                FieldSection(label = stringResource(R.string.habit_today_when)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !state.todayFromStart,
                            onClick = { actions.onTodayFromStartChange(false) },
                            label = { Text(stringResource(R.string.habit_today_all_day)) },
                        )
                        FilterChip(
                            selected = state.todayFromStart,
                            onClick = { actions.onTodayFromStartChange(true) },
                            // waiting for a time needs a time to wait for
                            enabled = state.hasTime,
                            label = { Text(stringResource(R.string.habit_today_from_time)) },
                        )
                    }
                    FormHint(
                        stringResource(
                            when {
                                !state.hasTime -> R.string.habit_today_from_needs_time
                                state.staged -> R.string.habit_today_when_hint_stages
                                else -> R.string.habit_today_when_hint
                            },
                        ),
                    )
                }

                if (state.staged) {
                    FieldSection(label = stringResource(R.string.habit_row_style)) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = state.stageMode == StageMode.ORDERED,
                                onClick = { actions.onStageModeChange(StageMode.ORDERED) },
                                label = { Text(stringResource(R.string.habit_row_one)) },
                            )
                            FilterChip(
                                selected = state.stageMode == StageMode.ALL,
                                onClick = { actions.onStageModeChange(StageMode.ALL) },
                                label = { Text(stringResource(R.string.habit_row_checklist)) },
                            )
                        }
                        FormHint(stringResource(R.string.habit_row_hint))
                    }
                } else {
                    FieldSection(label = stringResource(R.string.habit_own_time)) {
                        TimeSetting(state.time, actions.onTimeChange)
                        FormHint(stringResource(R.string.habit_own_time_hint))
                    }
                }
            }

            if (days) {
                SwitchRow(
                    label = stringResource(R.string.habit_remind_label),
                    checked = state.remind,
                    onCheckedChange = { on ->
                        actions.onRemindChange(on)
                        if (on) actions.onRequestNotificationPermission()
                    },
                    hint = stringResource(if (state.canRemind) R.string.habit_remind_hint else R.string.habit_remind_needs_time),
                )
                if (state.remind && state.canRemind && !health.canDeliverOnTime) ReminderWarning(onFix = actions.onOpenReliability)
            }
        }
    }
}
