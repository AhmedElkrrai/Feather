package com.elkrrai.feather.feature.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.model.Task
import com.elkrrai.feather.feature.tasklist.TaskRow
import com.elkrrai.feather.ui.brand.scenes.SceneKind
import com.elkrrai.feather.ui.components.EmptyState
import com.elkrrai.feather.ui.components.FeatherConfettiOverlay
import com.elkrrai.feather.ui.components.rememberConfettiController
import java.time.LocalDate

@Composable
fun SearchScreen(
    onOpenTask: (taskId: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SearchContent(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onToggle = { task, completed -> viewModel.setCompleted(task.id, completed) },
        onOpenTask = onOpenTask,
        onBack = onBack,
        onTagPicked = viewModel::onTagPicked,
        modifier = modifier,
    )
}

/** Stateless search screen, so it can be previewed and tested. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchContent(
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onToggle: (Task, Boolean) -> Unit,
    onOpenTask: (taskId: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
    onTagPicked: (tagId: String) -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val confetti = rememberConfettiController()

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(modifier = modifier) {
        Scaffold(
            modifier = Modifier,
            topBar = {
                TopAppBar(
                    title = {
                        TextField(
                            value = state.query,
                            onValueChange = onQueryChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            placeholder = { Text(stringResource(R.string.search_hint)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            // The drawable is autoMirrored, so the arrow flips in RTL languages.
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = stringResource(R.string.action_back),
                            )
                        }
                    },
                    actions = {
                        if (state.query.isNotBlank()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_close),
                                    contentDescription = stringResource(R.string.action_clear),
                                )
                            }
                        }
                    },
                )
            },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                when {
                    !state.isSearching -> EmptyState(
                        title = stringResource(R.string.search_prompt_title),
                        body = stringResource(R.string.search_prompt_body),
                        modifier = Modifier.imePadding(),
                    )

                    state.results.isEmpty() -> EmptyState(
                        scene = SceneKind.NoResults,
                        title = stringResource(
                            R.string.scene_no_results,
                            state.query.trim().ifBlank { state.selectedTag?.name.orEmpty() },
                        ),
                        body = stringResource(R.string.search_no_results_body),
                        modifier = Modifier.imePadding(),
                    )

                    else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.results, key = { it.id }) { task ->
                            TaskRow(
                                task = task,
                                today = today,
                                onToggle = { onToggle(task, it) },
                                onClick = { onOpenTask(task.id) },
                                onCelebrate = confetti::celebrate,
                            )
                        }
                    }
                }
            }
        }
        FeatherConfettiOverlay(confetti)
    }
}
