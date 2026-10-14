package com.elkrrai.feather.feature.tasklist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.model.Task
import com.elkrrai.feather.domain.view.TaskScope
import com.elkrrai.feather.domain.view.TaskSort
import com.elkrrai.feather.feature.taskedit.ListPickerDialog
import com.elkrrai.feather.ui.components.EmptyState
import com.elkrrai.feather.ui.rememberHaptics
import com.elkrrai.feather.ui.components.MenuAction
import com.elkrrai.feather.ui.components.MenuAction
import com.elkrrai.feather.ui.components.OverflowButton
import com.elkrrai.feather.ui.format.displayName
import com.elkrrai.feather.ui.format.formatDayHeader
import com.elkrrai.feather.ui.format.title
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun TaskListScreen(
    onOpenTask: (taskId: String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TaskListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var showQuickAdd by rememberSaveable { mutableStateOf(false) }
    var organizeDialog by remember { mutableStateOf<OrganizeDialog?>(null) }
    val deletedMessage = stringResource(R.string.task_deleted)
    val undoLabel = stringResource(R.string.action_undo)
    val resources = LocalContext.current.resources
    var selectionDialog by remember { mutableStateOf<SelectionDialog?>(null) }

    val selectionActions = SelectionActions(
        onToggle = viewModel::toggleSelected,
        onClear = viewModel::clearSelection,
        onSelectAll = viewModel::selectAll,
        onComplete = viewModel::completeSelected,
        onMove = { selectionDialog = SelectionDialog.Move },
        onTag = { selectionDialog = SelectionDialog.Tag },
        onDelete = {
            val ids = state.selection.toList()
            viewModel.deleteTasks(ids)
            coroutineScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val result = snackbarHostState.showSnackbar(
                    message = resources.getQuantityString(R.plurals.tasks_deleted, ids.size, ids.size),
                    actionLabel = undoLabel,
                    duration = SnackbarDuration.Short,
                )
                if (result == SnackbarResult.ActionPerformed) viewModel.restoreTasks(ids)
            }
        },
    )

    fun closeDrawer() {
        coroutineScope.launch { drawerState.close() }
    }

    val drawerActions = DrawerActions(
        onSelect = { scope ->
            viewModel.selectScope(scope)
            closeDrawer()
        },
        onNewList = { organizeDialog = OrganizeDialog.NewList },
        onNewFolder = { organizeDialog = OrganizeDialog.NewFolder },
        onEditList = { organizeDialog = OrganizeDialog.EditList(it) },
        onDeleteList = { organizeDialog = OrganizeDialog.DeleteList(it) },
        onRenameFolder = { organizeDialog = OrganizeDialog.RenameFolder(it) },
        onDeleteFolder = { organizeDialog = OrganizeDialog.DeleteFolder(it) },
        onRenameTag = { organizeDialog = OrganizeDialog.RenameTag(it) },
        onDeleteTag = { organizeDialog = OrganizeDialog.DeleteTag(it) },
        onOpenTrash = {
            closeDrawer()
            onOpenTrash()
        },
        onOpenSettings = {
            closeDrawer()
            onOpenSettings()
        },
    )

    val organizeActions = OrganizeActions(
        createList = viewModel::createList,
        updateList = viewModel::updateList,
        deleteList = viewModel::deleteList,
        createFolder = viewModel::createFolder,
        renameFolder = viewModel::renameFolder,
        deleteFolder = viewModel::deleteFolder,
        renameTag = viewModel::renameTag,
        deleteTag = viewModel::deleteTag,
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                TaskDrawerContent(state = state, actions = drawerActions)
            }
        },
        modifier = modifier,
    ) {
        TaskListContent(
            state = state,
            snackbarHostState = snackbarHostState,
            onMenuClick = { coroutineScope.launch { drawerState.open() } },
            onAddClick = { showQuickAdd = true },
            onToggle = { task, completed -> viewModel.setCompleted(task.id, completed) },
            onDelete = { task ->
                viewModel.deleteTask(task.id)
                coroutineScope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val result = snackbarHostState.showSnackbar(
                        message = deletedMessage,
                        actionLabel = undoLabel,
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.restoreTask(task.id)
                }
            },
            onOpenTask = onOpenTask,
            onSearchClick = onOpenSearch,
            onSortChange = viewModel::setSort,
            onPinChange = { task, pinned -> viewModel.setPinned(task.id, pinned) },
            selectionActions = selectionActions,
        )
    }

    OrganizeDialogHost(
        dialog = organizeDialog,
        folders = state.folders,
        actions = organizeActions,
        onDismiss = { organizeDialog = null },
    )

    when (selectionDialog) {
        null -> Unit

        SelectionDialog.Move -> ListPickerDialog(
            lists = state.lists,
            selectedId = "",
            onSelect = {
                viewModel.moveSelectedTo(it)
                selectionDialog = null
            },
            onDismiss = { selectionDialog = null },
        )

        SelectionDialog.Tag -> TagPickerDialog(
            tags = state.tags,
            onPick = {
                viewModel.addTagToSelected(it)
                selectionDialog = null
            },
            onCreate = {
                viewModel.addNewTagToSelected(it)
                selectionDialog = null
            },
            onDismiss = { selectionDialog = null },
        )
    }

    if (showQuickAdd) {
        QuickAddSheet(
            lists = state.lists,
            onDismiss = { showQuickAdd = false },
            onAdd = viewModel::addParsed,
        )
    }
}

/** Stateless task list, so it can be previewed and screenshot-tested. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListContent(
    state: TaskListUiState,
    snackbarHostState: SnackbarHostState,
    onMenuClick: () -> Unit,
    onAddClick: () -> Unit,
    onToggle: (Task, Boolean) -> Unit,
    onDelete: (Task) -> Unit,
    onOpenTask: (taskId: String) -> Unit,
    onSearchClick: () -> Unit,
    onSortChange: (TaskSort) -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
    onPinChange: (Task, Boolean) -> Unit = { _, _ -> },
    selectionActions: SelectionActions = SelectionActions(),
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val selecting = state.selection.isNotEmpty()
    val haptics = rememberHaptics()

    // Back leaves selection mode before it leaves the screen.
    BackHandler(enabled = selecting) { selectionActions.onClear() }

    // Only mixed views (smart views, tags) show which list a task belongs to.
    val listNames: Map<String, String> = if (state.scope is TaskScope.InList) {
        emptyMap()
    } else {
        state.lists.associate { it.id to it.displayName() }
    }

    val sortActions = TaskSort.entries.map { sort ->
        MenuAction(
            label = stringResource(sort.labelRes()),
            selected = state.sort == sort,
            onClick = { onSortChange(sort) },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            if (selecting) {
                SelectionTopBar(count = state.selection.size, actions = selectionActions)
            } else {
            TopAppBar(
                title = { Text(state.scope.title(state.lists, state.tags), maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_menu),
                            contentDescription = stringResource(R.string.action_open_menu),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_search),
                            contentDescription = stringResource(R.string.action_search),
                        )
                    }
                    OverflowButton(actions = sortActions)
                },
            )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (state.scope != TaskScope.Completed && !selecting) {
                FloatingActionButton(onClick = onAddClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = stringResource(R.string.action_add_task),
                    )
                }
            }
        },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding))

            state.isEmpty -> {
                val isList = state.scope is TaskScope.InList
                EmptyState(
                    title = stringResource(
                        if (isList) R.string.task_list_empty_title else R.string.empty_view_title
                    ),
                    body = stringResource(
                        if (isList) R.string.task_list_empty_body else R.string.empty_view_body
                    ),
                    modifier = Modifier.padding(padding),
                )
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // Extra bottom space so the last row can scroll clear of the FAB.
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 88.dp,
                ),
            ) {
                state.sections.forEach { section ->
                    if (section.date != null) {
                        item(key = "header-${section.date}") {
                            Text(
                                text = formatDayHeader(context, section.date, today, locale),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .semantics { heading() },
                            )
                        }
                    }
                    items(section.tasks, key = { it.id }) { task ->
                        SwipeToDeleteRow(enabled = !selecting, onDelete = { onDelete(task) }) {
                            TaskRow(
                                task = task,
                                today = today,
                                onToggle = { onToggle(task, it) },
                                onClick = {
                                    if (selecting) selectionActions.onToggle(task.id) else onOpenTask(task.id)
                                },
                                selected = task.id in state.selection,
                                onLongClick = {
                                    haptics.longPress()
                                    selectionActions.onToggle(task.id)
                                },
                                listName = listNames[task.listId],
                                onDelete = { onDelete(task) },
                                onPinChange = { pinned -> onPinChange(task, pinned) },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun TaskSort.labelRes(): Int = when (this) {
    TaskSort.CREATED -> R.string.sort_created
    TaskSort.DUE_DATE -> R.string.sort_due_date
    TaskSort.PRIORITY -> R.string.sort_priority
}

/** Swipe in either direction deletes. Direction-aware, so it mirrors correctly in RTL. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteRow(
    onDelete: () -> Unit,
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { target ->
            if (target == SwipeToDismissBoxValue.Settled) {
                false
            } else {
                onDelete()
                true
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = enabled,
        enableDismissFromEndToStart = enabled,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (direction == SwipeToDismissBoxValue.Settled) {
                            Color.Transparent
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        }
                    )
                    .padding(horizontal = 24.dp),
                contentAlignment = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    SwipeToDismissBoxValue.Settled -> Alignment.Center
                },
            ) {
                if (direction != SwipeToDismissBoxValue.Settled) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.action_delete),
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        },
        content = { content() },
    )
}

/** Replaces the top bar while tasks are selected: how many, and what can be done with them. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(count: Int, actions: SelectionActions) {
    val resources = LocalContext.current.resources
    TopAppBar(
        title = { Text(resources.getQuantityString(R.plurals.selected_count, count, count), maxLines = 1) },
        navigationIcon = {
            IconButton(onClick = actions.onClear) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.selection_clear),
                )
            }
        },
        actions = {
            IconButton(onClick = actions.onComplete) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = stringResource(R.string.action_mark_done),
                )
            }
            IconButton(onClick = actions.onDelete) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.action_delete),
                )
            }
            OverflowButton(
                actions = listOf(
                    MenuAction(stringResource(R.string.selection_select_all)) { actions.onSelectAll() },
                    MenuAction(stringResource(R.string.selection_move)) { actions.onMove() },
                    MenuAction(stringResource(R.string.selection_tag)) { actions.onTag() },
                ),
            )
        },
    )
}

private enum class SelectionDialog { Move, Tag }
