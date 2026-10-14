package com.elkrrai.feather.feature.trash

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.model.Task
import com.elkrrai.feather.domain.repository.TRASH_RETENTION_DAYS
import com.elkrrai.feather.feature.tasklist.ConfirmDialog
import com.elkrrai.feather.ui.brand.BirdMood
import com.elkrrai.feather.ui.components.EmptyState
import com.elkrrai.feather.ui.components.MenuAction
import com.elkrrai.feather.ui.components.OverflowButton

@Composable
fun TrashScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrashViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TrashContent(
        state = state,
        onBack = onBack,
        onRestore = viewModel::restore,
        onDeleteForever = viewModel::deleteForever,
        onEmptyTrash = viewModel::emptyTrash,
        modifier = modifier,
    )
}

/** Stateless trash screen, so it can be previewed and tested. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashContent(
    state: TrashUiState,
    onBack: () -> Unit,
    onRestore: (taskId: String) -> Unit,
    onDeleteForever: (taskId: String) -> Unit,
    onEmptyTrash: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmEmpty by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<Task?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.trash)) },
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
                    if (state.tasks.isNotEmpty()) {
                        OverflowButton(
                            actions = listOf(
                                MenuAction(stringResource(R.string.action_empty_trash)) {
                                    confirmEmpty = true
                                },
                            ),
                        )
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding))

            state.tasks.isEmpty() -> EmptyState(
                mood = BirdMood.SLEEPING,
                title = stringResource(R.string.trash_empty_title),
                body = stringResource(R.string.trash_empty_body, TRASH_RETENTION_DAYS.toInt()),
                modifier = Modifier.padding(padding),
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                items(state.tasks, key = { it.id }) { task ->
                    TrashRow(
                        task = task,
                        onRestore = { onRestore(task.id) },
                        onDeleteForever = { confirmDelete = task },
                    )
                }
            }
        }
    }

    if (confirmEmpty) {
        ConfirmDialog(
            title = stringResource(R.string.empty_trash_title),
            message = stringResource(R.string.empty_trash_message),
            onConfirm = {
                onEmptyTrash()
                confirmEmpty = false
            },
            onDismiss = { confirmEmpty = false },
        )
    }

    confirmDelete?.let { task ->
        ConfirmDialog(
            title = stringResource(R.string.delete_forever_title),
            message = stringResource(R.string.delete_forever_message, task.title),
            onConfirm = {
                onDeleteForever(task.id)
                confirmDelete = null
            },
            onDismiss = { confirmDelete = null },
        )
    }
}

@Composable
private fun TrashRow(
    task: Task,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit,
) {
    val deletedAgo = remember(task.deletedAt) {
        task.deletedAt?.let {
            DateUtils.getRelativeTimeSpanString(
                it.toEpochMilli(),
                System.currentTimeMillis(),
                DateUtils.DAY_IN_MILLIS,
            ).toString()
        }
    }

    ListItem(
        headlineContent = {
            Text(text = task.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = deletedAgo?.let { ago ->
            { Text(stringResource(R.string.trash_deleted_ago, ago)) }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onRestore) { Text(stringResource(R.string.action_restore)) }
                IconButton(onClick = onDeleteForever) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.action_delete_forever),
                    )
                }
            }
        },
    )
}
