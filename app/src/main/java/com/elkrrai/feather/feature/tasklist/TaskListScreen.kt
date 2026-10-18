package com.elkrrai.feather.feature.tasklist

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.elkrrai.feather.domain.model.SavedFilter
import com.elkrrai.feather.domain.model.Task
import com.elkrrai.feather.domain.view.TaskScope
import com.elkrrai.feather.domain.view.TaskSort
import com.elkrrai.feather.feature.taskedit.ListPickerDialog
import com.elkrrai.feather.ui.brand.BirdMood
import com.elkrrai.feather.ui.brand.CompletionBird
import com.elkrrai.feather.ui.components.EmptyState
import com.elkrrai.feather.ui.rememberHaptics
import com.elkrrai.feather.ui.components.MenuAction
import com.elkrrai.feather.ui.components.OverflowButton
import com.elkrrai.feather.ui.format.displayName
import com.elkrrai.feather.ui.format.formatDayHeader
import com.elkrrai.feather.ui.format.title
import com.elkrrai.feather.ui.rememberReduceMotion
import com.elkrrai.feather.ui.theme.Dur
import com.elkrrai.feather.ui.theme.Ease
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.elkrrai.feather.ui.theme.listColorIndex
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.ui.input.nestedscroll.nestedScroll
import java.time.LocalTime
import androidx.compose.runtime.LaunchedEffect
import androidx.annotation.StringRes
import com.elkrrai.feather.domain.model.TaskList
import com.elkrrai.feather.ui.brand.scenes.SceneKind
import androidx.compose.ui.geometry.Offset
import com.elkrrai.feather.ui.DisplayPrefs
import com.elkrrai.feather.ui.components.ConfettiBurst
import com.elkrrai.feather.ui.components.FeatherConfettiOverlay
import com.elkrrai.feather.ui.components.rememberConfettiController
import com.elkrrai.feather.ui.today.DayCard
import com.elkrrai.feather.ui.today.DayCardKind
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

@Composable
fun TaskListScreen(
    onOpenTask: (taskId: String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenRhythm: () -> Unit = {},
    viewModel: TaskListViewModel = hiltViewModel(),
    dayCards: DayCardsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dayCard by dayCards.card.collectAsStateWithLifecycle()
    val nestNews by dayCards.nestNews.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var showQuickAdd by rememberSaveable { mutableStateOf(false) }
    var organizeDialog by remember { mutableStateOf<OrganizeDialog?>(null) }
    val deletedMessage = stringResource(R.string.task_deleted)
    val undoLabel = stringResource(R.string.action_undo)
    val context = LocalContext.current
    val resources = context.resources
    var selectionDialog by remember { mutableStateOf<SelectionDialog?>(null) }
    var filterDialog by remember { mutableStateOf<FilterDialogState?>(null) }
    // Counts up each time a task is finished; the bird hops over the list for each step.
    var celebration by remember { mutableIntStateOf(0) }
    // Feather confetti for every checkbox finished on this screen.
    val confetti = rememberConfettiController()

    /** Moves the tasks to the trash and offers to bring them back. */
    fun deleteWithUndo(ids: List<String>) {
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
    }

    val selectionActions = SelectionActions(
        onToggle = viewModel::toggleSelected,
        onClear = viewModel::clearSelection,
        onSelectAll = viewModel::selectAll,
        onComplete = {
            celebration++
            viewModel.completeSelected()
        },
        onMove = { selectionDialog = SelectionDialog.Move },
        onTag = { selectionDialog = SelectionDialog.Tag },
        onDelete = { deleteWithUndo(state.selection.toList()) },
    )

    fun closeDrawer() {
        coroutineScope.launch { drawerState.close() }
    }

    // Back closes the menu first, before it leaves the screen.
    BackHandler(enabled = drawerState.currentValue == DrawerValue.Open) { closeDrawer() }

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
        onOpenRhythm = {
            closeDrawer()
            onOpenRhythm()
        },
        onClose = ::closeDrawer,
        onNewFilter = { filterDialog = FilterDialogState.New },
        onEditFilter = { filterDialog = FilterDialogState.Edit(it) },
        onDeleteFilter = { filterDialog = FilterDialogState.Delete(it) },
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
                TaskDrawerContent(state = state, actions = drawerActions, settingsDot = nestNews)
            }
        },
        modifier = modifier,
    ) {
        Box {
        TaskListContent(
            state = state,
            snackbarHostState = snackbarHostState,
            onMenuClick = { coroutineScope.launch { drawerState.open() } },
            onAddClick = { showQuickAdd = true },
            onToggle = { task, completed ->
                viewModel.setCompleted(task.id, completed) { after ->
                    // A repeating task is not finished for good: it rests and comes back on its next day.
                    val comesBack = after?.dueDate
                    if (completed && task.recurrence != null && after != null && !after.isCompleted && comesBack != null) {
                        coroutineScope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            val result = snackbarHostState.showSnackbar(
                                message = backMessage(context, comesBack),
                                actionLabel = undoLabel,
                                duration = SnackbarDuration.Short,
                            )
                            if (result == SnackbarResult.ActionPerformed) viewModel.setCompleted(task.id, false)
                        }
                    }
                }
                if (completed) celebration++
            },
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
            onReorder = viewModel::reorderTasks,
            onCelebrate = confetti::celebrate,
            onFly = confetti::fly,
            onWarmFeathers = { origin -> confetti.celebrate(origin, 1f, 12, true) },
            onClearCompleted = { deleteWithUndo(state.sections.flatMap { it.tasks }.map { it.id }) },
            dayCard = dayCard,
            onDismissDayCard = dayCards::dismiss,
            onMoveToTomorrow = {
                dayCards.moveToTomorrow { moved ->
                    coroutineScope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        val result = snackbarHostState.showSnackbar(
                            message = resources.getQuantityString(R.plurals.day_card_moved, moved.size, moved.size),
                            actionLabel = undoLabel,
                            duration = SnackbarDuration.Short,
                        )
                        if (result == SnackbarResult.ActionPerformed) dayCards.undoMove(moved)
                    }
                }
            },
            onAddSubtask = viewModel::addSubtask,
            onToggleSubtask = viewModel::toggleSubtask,
        )
        FeatherConfettiOverlay(confetti)
        CompletionBird(
            trigger = celebration,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp),
        )
        }
    }

    OrganizeDialogHost(
        dialog = organizeDialog,
        folders = state.folders,
        actions = organizeActions,
        onDismiss = { organizeDialog = null },
    )

    when (val dialog = filterDialog) {
        null -> Unit

        FilterDialogState.New -> FilterDialog(
            initial = null,
            lists = state.lists,
            tags = state.tags,
            onConfirm = {
                viewModel.createFilter(it)
                filterDialog = null
                closeDrawer()
            },
            onDismiss = { filterDialog = null },
        )

        is FilterDialogState.Edit -> FilterDialog(
            initial = dialog.filter,
            lists = state.lists,
            tags = state.tags,
            onConfirm = {
                viewModel.updateFilter(dialog.filter, it)
                filterDialog = null
            },
            onDismiss = { filterDialog = null },
        )

        is FilterDialogState.Delete -> AlertDialog(
            onDismissRequest = { filterDialog = null },
            title = { Text(stringResource(R.string.delete_filter_title)) },
            text = { Text(stringResource(R.string.delete_filter_message, dialog.filter.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteFilter(dialog.filter)
                        filterDialog = null
                    },
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { filterDialog = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

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
            tags = state.tags,
            onDismiss = { showQuickAdd = false },
            onAdd = viewModel::addParsed,
            onVoiceUsed = dayCards::voiceUsed,
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
    onReorder: (List<String>) -> Unit = {},
    onCelebrate: (Offset, Float) -> Unit = { _, _ -> },
    onAddSubtask: (taskId: String, title: String) -> Unit = { _, _ -> },
    onToggleSubtask: (taskId: String, subtaskId: String) -> Unit = { _, _ -> },
    onClearCompleted: () -> Unit = {},
    onFly: (from: Offset, to: Offset?) -> Unit = { _, _ -> },
    onWarmFeathers: (Offset) -> Unit = {},
    dayCard: DayCard? = null,
    onDismissDayCard: (DayCardKind) -> Unit = {},
    onMoveToTomorrow: () -> Unit = {},
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val selecting = state.selection.isNotEmpty()
    val haptics = rememberHaptics()
    val reduceMotion = rememberReduceMotion()
    val scope = rememberCoroutineScope()
    // Tasks finished a moment ago, kept on screen while their finishing animation plays.
    val finishing = remember { mutableStateMapOf<String, FinishingRow>() }
    var finishStamp by remember { mutableLongStateOf(0L) }

    // Manual order: while a row is dragged the list shows a local copy of the saved order.
    val listState = rememberLazyListState()
    val reorderable = state.sort == TaskSort.MANUAL && state.sections.size == 1 &&
        state.sections.single().date == null && !selecting
    val savedOrder = state.sections.flatMap { it.tasks }
    val order = remember(savedOrder) { mutableStateListOf<Task>().apply { addAll(savedOrder) } }
    val currentOnReorder by rememberUpdatedState(onReorder)
    val reorder = remember(listState, order, haptics) {
        ReorderState(listState, order, haptics) { ids -> currentOnReorder(ids) }
    }

    // Back leaves selection mode before it leaves the screen.
    BackHandler(enabled = selecting) { selectionActions.onClear() }

    // Only mixed views (smart views, tags) show which list a task belongs to.
    val listLabels: Map<String, ListLabel> = if (state.scope is TaskScope.InList) {
        emptyMap()
    } else {
        state.lists.associate { it.id to ListLabel(it.displayName(), listColorIndex(it.colorArgb)) }
    }

    val sortActions = TaskSort.entries.map { sort ->
        MenuAction(
            label = stringResource(sort.labelRes()),
            selected = state.sort == sort,
            onClick = { onSortChange(sort) },
        )
    }

    // The Today view has its own header (with the buttons), so it has no top bar.
    val todayView = state.scope == TaskScope.Today && !selecting && !state.isLoading
    val collapse = rememberHeaderCollapse()
    // Each time the Today view is shown, its header starts open.
    LaunchedEffect(state.scope) { collapse.state.offset = 0f }
    var doneExpanded by rememberSaveable { mutableStateOf(false) }
    // Where the Done pill is, for the feather a repeating task sends there when it rests.
    var doneTarget by remember { mutableStateOf<Offset?>(null) }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    // The last open task of the day: the header celebrates, once a calendar day. Moving tasks to
    // tomorrow also empties the day, but it is not finishing, so only a rise in finished tasks counts.
    val dayLog = remember { DayCelebrationLog(context) }
    var allDoneCelebration by remember { mutableIntStateOf(0) }
    var previousLeft by remember { mutableIntStateOf(state.today.left) }
    var previousDone by remember { mutableIntStateOf(state.today.done) }
    LaunchedEffect(state.today.left, state.today.done, todayView) {
        val finishedTheLast = previousLeft > 0 && state.today.left == 0 && state.today.done > previousDone
        previousLeft = state.today.left
        previousDone = state.today.done
        if (todayView && finishedTheLast && !dayLog.celebratedOn(today)) {
            dayLog.mark(today)
            allDoneCelebration++
        }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = if (todayView) WindowInsets.navigationBars else ScaffoldDefaults.contentWindowInsets,
        topBar = {
            if (todayView) {
                // nothing: see TodayHeader
            } else if (selecting) {
                SelectionTopBar(count = state.selection.size, actions = selectionActions)
            } else {
            TopAppBar(
                title = {
                    Text(
                        text = state.scope.title(state.lists, state.tags),
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                    )
                },
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
                    val clearAction = if (state.scope == TaskScope.Completed && !state.isEmpty) {
                        listOf(MenuAction(stringResource(R.string.action_clear_completed), onClick = onClearCompleted))
                    } else {
                        emptyList()
                    }
                    OverflowButton(actions = sortActions + clearAction)
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
    ) { scaffoldPadding ->
        val listBody: @Composable (PaddingValues) -> Unit = { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding))

            // Today keeps its header (and its message) instead of an empty-state picture.
            state.isEmpty && !todayView && state.doneTasks.isEmpty() -> {
                val copy = state.scope.emptyCopy()
                EmptyState(
                    scene = copy.scene,
                    title = stringResource(copy.title),
                    body = copy.body?.let { stringResource(it) },
                    modifier = Modifier.padding(padding),
                )
            }

            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                // Extra bottom space so the last row can scroll clear of the FAB.
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 88.dp,
                ),
            ) {
                // The morning and evening cards sit on top of Today, one a day.
                if (todayView && dayCard != null) {
                    item(key = "day-card") {
                        DayCardView(
                            card = dayCard,
                            onDismiss = { onDismissDayCard(dayCard.kind) },
                            onMoveToTomorrow = onMoveToTomorrow,
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
                // Everything here is finished: say so, above the Done section.
                if (state.isEmpty && !todayView && state.doneTasks.isNotEmpty()) {
                    item(key = "all-done-note") { AllDoneNote() }
                }
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
                    // Manual order keeps its own copy of the list for dragging, so finished rows leave at once there.
                    val rows = if (reorderable) order else withFinishing(section.tasks, finishing, section.date)
                    items(rows, key = { it.id }) { task ->
                        val dragging = reorder.draggingId == task.id
                        SwipeToDeleteRow(
                            enabled = !selecting && reorder.draggingId == null,
                            onDelete = { onDelete(task) },
                            modifier = if (dragging) {
                                Modifier.zIndex(1f).graphicsLayer { translationY = reorder.offset }
                            } else {
                                Modifier.animateItem(
                                    fadeInSpec = tween(if (reduceMotion) Dur.Micro else Dur.Short2),
                                    placementSpec = if (reduceMotion) null else Ease.nestle(),
                                    fadeOutSpec = tween(
                                        durationMillis = if (reduceMotion) 120 else Dur.Short1,
                                        easing = Ease.Leave,
                                    ),
                                )
                            },
                        ) {
                            // Keeps a finished row on screen, struck through, for a moment before it leaves.
                            fun lingerFinished() {
                                val stamp = ++finishStamp
                                finishing[task.id] = FinishingRow(
                                    task = task.copy(
                                        completedAt = Instant.now(),
                                        subtasks = task.subtasks.map { it.copy(isCompleted = true) },
                                    ),
                                    index = rows.indexOfFirst { it.id == task.id }.coerceAtLeast(0),
                                    sectionKey = section.date,
                                    stamp = stamp,
                                )
                                scope.launch {
                                    delay(Dur.RowLinger.toLong())
                                    if (finishing[task.id]?.stamp == stamp) finishing.remove(task.id)
                                }
                            }
                            TaskRow(
                                task = task,
                                today = today,
                                onToggle = { completed ->
                                    if (completed && !reduceMotion && !reorderable) {
                                        lingerFinished()
                                    } else {
                                        finishing.remove(task.id)
                                    }
                                    onToggle(task, completed)
                                },
                                onClick = {
                                    if (selecting) selectionActions.onToggle(task.id) else onOpenTask(task.id)
                                },
                                selected = task.id in state.selection,
                                highlighted = task.id == state.justAddedId,
                                onCelebrate = { origin, scale ->
                                    onCelebrate(origin, scale)
                                    if (task.recurrence != null && scale == 1f) onFly(origin, doneTarget)
                                },
                                onAddSubtask = { title -> onAddSubtask(task.id, title) },
                                onToggleSubtask = { subtaskId ->
                                    // Ticking the last open subtask finishes the task: let the row linger like a ring tick.
                                    val open = task.subtasks.filter { !it.isCompleted }
                                    if (!task.isCompleted && open.size == 1 && open.single().id == subtaskId &&
                                        !reduceMotion && !reorderable
                                    ) {
                                        lingerFinished()
                                    }
                                    onToggleSubtask(task.id, subtaskId)
                                },
                                dragHandle = if (reorderable) ({ DragHandle(reorder, task.id) }) else null,
                                onMoveUp = if (reorderable) ({ reorder.moveBy(task.id, -1) }) else null,
                                onMoveDown = if (reorderable) ({ reorder.moveBy(task.id, 1) }) else null,
                                onLongClick = {
                                    haptics.longPress()
                                    selectionActions.onToggle(task.id)
                                },
                                list = listLabels[task.listId],
                                onDelete = { onDelete(task) },
                                onPinChange = { pinned -> onPinChange(task, pinned) },
                            )
                        }
                    }
                }
                if (state.doneTasks.isNotEmpty()) {
                    item(key = "done-today-pill") {
                        DoneTodayPill(
                            label = stringResource(
                                if (todayView) R.string.done_today_pill else R.string.done_pill,
                                state.doneTasks.size,
                            ),
                            expanded = doneExpanded,
                            onClick = { doneExpanded = !doneExpanded },
                            modifier = Modifier.animateItem().onGloballyPositioned {
                                val share = if (rtl) 0.88f else 0.12f
                                doneTarget = it.positionInRoot() + Offset(it.size.width * share, it.size.height / 2f)
                            },
                        )
                    }
                    if (doneExpanded) {
                        items(state.doneTasks, key = { "done-${it.id}" }) { task ->
                            TaskRow(
                                task = task,
                                today = today,
                                onToggle = { onToggle(task, it) },
                                onClick = { onOpenTask(task.id) },
                                list = listLabels[task.listId],
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
        }
        }

        if (todayView) {
            val now = remember(state.today) { LocalTime.now() }
            Column(Modifier.fillMaxSize().nestedScroll(collapse.connection)) {
                TodayHeader(
                    status = state.today,
                    date = today,
                    time = now,
                    collapse = collapse.state,
                    title = state.scope.title(state.lists, state.tags),
                    onMenuClick = onMenuClick,
                    onSearchClick = onSearchClick,
                    sortActions = sortActions,
                    celebration = allDoneCelebration,
                    onWarmFeathers = onWarmFeathers,
                )
                Box(Modifier.weight(1f)) {
                    listBody(PaddingValues(bottom = scaffoldPadding.calculateBottomPadding()))
                }
            }
        } else {
            listBody(scaffoldPadding)
        }
    }
}

/** The picture and words for an empty view. */
private class EmptyCopy(val scene: SceneKind, @StringRes val title: Int, @StringRes val body: Int?)

/**
 * What an empty view says. The Inbox is where a new user starts, so it greets; other lists invite
 * a first task; Waiting for you is a happy place to be; the date views say nothing is due.
 */
private fun TaskScope.emptyCopy(): EmptyCopy = when (this) {
    is TaskScope.InList ->
        if (listId == TaskList.INBOX_ID) {
            EmptyCopy(SceneKind.FirstLaunch, R.string.scene_first_launch, R.string.task_list_empty_body)
        } else {
            EmptyCopy(SceneKind.EmptyList, R.string.scene_empty_list, R.string.task_list_empty_body)
        }

    TaskScope.Overdue -> EmptyCopy(SceneKind.AllDone, R.string.scene_all_done, null)
    TaskScope.Today, TaskScope.Upcoming, TaskScope.NoDate ->
        EmptyCopy(SceneKind.NothingDue, R.string.scene_nothing_due, null)

    else -> EmptyCopy(SceneKind.NothingDue, R.string.empty_view_title, R.string.empty_view_body)
}

private fun TaskSort.labelRes(): Int = when (this) {
    TaskSort.CREATED -> R.string.sort_created
    TaskSort.DUE_DATE -> R.string.sort_due_date
    TaskSort.PRIORITY -> R.string.sort_priority
    TaskSort.MANUAL -> R.string.sort_manual
}

/** Swipe in either direction deletes. Direction-aware, so it mirrors correctly in RTL. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteRow(
    onDelete: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
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
        modifier = modifier,
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

/** Which saved-filter dialog is open. */
private sealed interface FilterDialogState {
    data object New : FilterDialogState
    data class Edit(val filter: SavedFilter) : FilterDialogState
    data class Delete(val filter: SavedFilter) : FilterDialogState
}

/** "Back tomorrow" or "Back Fri" or "Back Nov 16, 2026": when a repeating task that was just finished returns. */
private fun backMessage(context: android.content.Context, date: LocalDate): String {
    val today = LocalDate.now()
    val locale = context.resources.configuration.locales[0]
    return if (date == today.plusDays(1)) {
        context.getString(R.string.repeat_back_tomorrow)
    } else {
        context.getString(
            R.string.repeat_back_on,
            com.elkrrai.feather.ui.format.dueChipText(context, com.elkrrai.feather.ui.format.dueLabelFor(date, null, today, false), locale),
        )
    }
}
