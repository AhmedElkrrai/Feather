package com.elkrrai.feather.feature.tasklist

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.model.Task
import com.elkrrai.feather.domain.view.TaskScope
import com.elkrrai.feather.domain.view.TaskSort
import com.elkrrai.feather.feature.settings.LanguageDialog
import com.elkrrai.feather.ui.components.EmptyState
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
    modifier: Modifier = Modifier,
    viewModel: TaskListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var showQuickAdd by rememberSaveable { mutableStateOf(false) }
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }
    var organizeDialog by remember { mutableStateOf<OrganizeDialog?>(null) }
    val deletedMessage = stringResource(R.string.task_deleted)
    val undoLabel = stringResource(R.string.action_undo)

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
        onOpenLanguage = {
            closeDrawer()
            showLanguageDialog = true
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
        )
    }

    OrganizeDialogHost(
        dialog = organizeDialog,
        folders = state.folders,
        actions = organizeActions,
        onDismiss = { organizeDialog = null },
    )

    if (showLanguageDialog) {
        LanguageDialog(onDismiss = { showLanguageDialog = false })
    }

    if (showQuickAdd) {
        QuickAddSheet(
            onDismiss = { showQuickAdd = false },
            onAdd = viewModel::addTask,
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
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]

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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (state.scope != TaskScope.Completed) {
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
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                    }
                    items(section.tasks, key = { it.id }) { task ->
                        SwipeToDeleteRow(onDelete = { onDelete(task) }) {
                            TaskRow(
                                task = task,
                                today = today,
                                onToggle = { onToggle(task, it) },
                                onClick = { onOpenTask(task.id) },
                                listName = listNames[task.listId],
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
