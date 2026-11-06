package com.elkrrai.feather.feature.habits

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.habit.HabitKind
import com.elkrrai.feather.ui.components.FieldSection
import com.elkrrai.feather.ui.components.FormHint

/** The screen that makes a habit or edits one. It replaces the dialog the Habits screen used to open. */
@Composable
fun HabitEditScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HabitEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.finished) { if (state.finished) onBack() }
    HabitEditContent(
        state = state,
        onBack = onBack,
        onSave = viewModel::save,
        onNameChange = viewModel::onNameChange,
        onKindChange = viewModel::onKindChange,
        onTargetChange = viewModel::onTargetChange,
        onDailyChange = viewModel::onDailyChange,
        onUnitChange = viewModel::onUnitChange,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun HabitEditContent(
    state: HabitEditUiState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onNameChange: (String) -> Unit,
    onKindChange: (HabitKind) -> Unit,
    onTargetChange: (String) -> Unit,
    onDailyChange: (String) -> Unit,
    onUnitChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val days = state.kind == HabitKind.DAYS
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isNew) R.string.habit_new else R.string.habit_edit)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                },
                actions = {
                    TextButton(onClick = onSave, enabled = state.canSave) { Text(stringResource(R.string.action_save)) }
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
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.habit_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            FieldSection(label = stringResource(R.string.habit_type_label)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = days,
                        onClick = { onKindChange(HabitKind.DAYS) },
                        enabled = state.isNew,
                        label = { Text(stringResource(R.string.habit_kind_days)) },
                    )
                    FilterChip(
                        selected = !days,
                        onClick = { onKindChange(HabitKind.AMOUNT) },
                        enabled = state.isNew,
                        label = { Text(stringResource(R.string.habit_kind_amount)) },
                    )
                }
                FormHint(stringResource(if (state.isNew) R.string.habit_type_hint else R.string.habit_type_locked_hint))
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = state.target,
                    onValueChange = onTargetChange,
                    label = { Text(stringResource(if (days) R.string.habit_target_days else R.string.habit_target_amount)) },
                    isError = !state.targetValid,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                FormHint(stringResource(if (days) R.string.habit_target_days_hint else R.string.habit_target_amount_hint))
                if (!state.isNew) FormHint(stringResource(R.string.habit_goal_next_week))
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (days) {
                    OutlinedTextField(
                        value = state.daily,
                        onValueChange = onDailyChange,
                        label = { Text(stringResource(R.string.habit_daily_target)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    FormHint(stringResource(R.string.habit_daily_target_hint))
                } else {
                    OutlinedTextField(
                        value = state.unit,
                        onValueChange = onUnitChange,
                        label = { Text(stringResource(R.string.habit_unit)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    FormHint(stringResource(R.string.habit_unit_hint))
                }
            }
        }
    }
}
