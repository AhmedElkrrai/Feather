package com.elkrrai.feather.feature.tasklist

import androidx.activity.compose.BackHandler
import com.elkrrai.feather.ui.format.formatDoneHeader
import kotlin.math.abs
import kotlinx.coroutines.CoroutineStart
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.animation.core.Animatable
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
import com.elkrrai.feather.domain.model.Task
import com.elkrrai.feather.domain.support.TipResult
import com.elkrrai.feather.domain.view.TagFilter
import com.elkrrai.feather.domain.view.TaskScope
import com.elkrrai.feather.feature.tip.TipSheet
import com.elkrrai.feather.feature.tip.TipViewModel
import com.elkrrai.feather.feature.tip.findActivity
import com.elkrrai.feather.domain.view.TaskSort
import com.elkrrai.feather.ui.brand.BirdMood
import com.elkrrai.feather.ui.brand.CompletionBird
import com.elkrrai.feather.ui.components.EmptyState
import com.elkrrai.feather.ui.rememberHaptics
import com.elkrrai.feather.ui.components.MenuAction
import com.elkrrai.feather.ui.components.OverflowButton
import com.elkrrai.feather.ui.format.formatDayHeader
import com.elkrrai.feather.ui.format.title
import com.elkrrai.feather.ui.rememberReduceMotion
import com.elkrrai.feather.ui.theme.Dur
import com.elkrrai.feather.ui.theme.Ease
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.ui.input.nestedscroll.nestedScroll
import java.time.LocalTime
import androidx.compose.runtime.LaunchedEffect
import androidx.annotation.StringRes
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
    onOpenNest: () -> Unit = {},
    viewModel: TaskListViewModel = hiltViewModel(),
    dayCards: DayCardsViewModel = hiltViewModel(),
    tips: TipViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val tipOffers by tips.offers.collectAsStateWithLifecycle()
    var showTips by rememberSaveable { mutableStateOf(false) }
    var tipStatus by remember { mutableStateOf<TipResult?>(null) }
    // Counts the tips that went through; the bird in the sheet flies a lap for each.
    var thanks by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        tips.results.collect { result ->
            tipStatus = result
            if (result == TipResult.THANKS) thanks++
        }
    }
    val dayCard by dayCards.card.collectAsStateWithLifecycle()
    val nestNews by dayCards.nestNews.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var showQuickAdd by rememberSaveable { mutableStateOf(false) }
    val deletedMessage = stringResource(R.string.task_deleted)
    val undoLabel = stringResource(R.string.action_undo)
    val context = LocalContext.current
    val resources = context.resources
    var selectionDialog by remember { mutableStateOf<SelectionDialog?>(null) }
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
        onOpenNest = {
            closeDrawer()
            onOpenNest()
        },
        onOpenTips = {
            closeDrawer()
            showTips = true
        },
        onClose = ::closeDrawer,
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                TaskDrawerContent(state = state, actions = drawerActions, nestDot = nestNews, tipsVisible = tipOffers.isNotEmpty())
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
            onCounterAdd = viewModel::addToCounter,
            onCounterSet = viewModel::setCounter,
            onFinishedByCount = { celebration++ },
            onTagFilterChange = viewModel::selectTagFilter,
        )
        FeatherConfettiOverlay(confetti)
        CompletionBird(
            trigger = celebration,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp),
        )
        }
    }

    if (showTips && tipOffers.isNotEmpty()) {
        TipSheet(
            offers = tipOffers,
            status = tipStatus,
            thanks = thanks,
            onTip = { offerId ->
                tipStatus = null
                context.findActivity()?.let { tips.tip(it, offerId) }
            },
            onDismiss = {
                showTips = false
                tipStatus = null
            },
        )
    }

    when (selectionDialog) {
        null -> Unit

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
    onCounterAdd: (taskId: String, amount: Int) -> Unit = { _, _ -> },
    onCounterSet: (taskId: String, done: Int) -> Unit = { _, _ -> },
    /** A counter task reached its target by steps: the bird hops, as for a ring tick. */
    onFinishedByCount: () -> Unit = {},
    onClearCompleted: () -> Unit = {},
    onFly: (from: Offset, to: Offset?) -> Unit = { _, _ -> },
    onWarmFeathers: (Offset) -> Unit = {},
    dayCard: DayCard? = null,
    onDismissDayCard: (DayCardKind) -> Unit = {},
    onMoveToTomorrow: () -> Unit = {},
    onTagFilterChange: (TagFilter) -> Unit = {},
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

    // Rows can be dragged into any order in a plain list (not Upcoming, whose rows sit under day headings,
    // and not Completed): while a row is dragged the list shows a local copy of the saved order, and
    // dropping it switches the sort to Manual so the list keeps the order that was made.
    val listState = rememberLazyListState()
    val reorderable = state.scope != TaskScope.Completed && state.sections.size == 1 &&
        state.sections.single().date == null && !selecting
    val savedOrder = state.sections.flatMap { it.tasks }
    val order = remember(savedOrder) { mutableStateListOf<Task>().apply { addAll(savedOrder) } }
    val currentOnReorder by rememberUpdatedState(onReorder)
    val currentOnSortChange by rememberUpdatedState(onSortChange)
    val currentSort by rememberUpdatedState(state.sort)
    val reorder = remember(listState, order, haptics) {
        ReorderState(listState, order, haptics) { ids ->
            currentOnReorder(ids)
            if (currentSort != TaskSort.MANUAL) currentOnSortChange(TaskSort.MANUAL)
        }
    }
    // The local copy of the order is what is shown in Manual sort and while a row is being dragged; otherwise
    // the list shows the saved order, with finished rows lingering for a moment.
    val usesOrderCopy = reorderable && (state.sort == TaskSort.MANUAL || reorder.draggingId != null)

    // Back leaves selection mode before it leaves the screen.
    BackHandler(enabled = selecting) { selectionActions.onClear() }

    val sortActions = TaskSort.entries.map { sort ->
        MenuAction(
            label = stringResource(sort.labelRes()),
            selected = state.sort == sort,
            onClick = { onSortChange(sort) },
        )
    }

    // The Today view has its own header (with the buttons), so it has no top bar.
    val todayView = state.scope == TaskScope.Today && !selecting && !state.isLoading
    // There is nothing to search for on a screen with no tasks.
    val canSearch = !state.isLoading && !state.isEmpty
    val collapse = rememberHeaderCollapse()
    // Each time the Today view is shown, its header starts open.
    LaunchedEffect(state.scope) { collapse.state.offset = 0f }
    // The finished task that was tapped in the Completed view, waiting for "mark as not done?".
    var reopenAsk by remember { mutableStateOf<Task?>(null) }
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

    // A task that was just added is brought into view: the list scrolls to wherever the sort put it
    // (the top for most sorts). Its row is looked up in the same order the list is built in.
    val addedId = state.justAddedId
    val addedIndex = if (addedId == null) {
        -1
    } else {
        var index = 0
        if (todayView && dayCard != null) index++
        var found = -1
        for (section in state.sections) {
            if (section.date != null) index++
            val at = section.tasks.indexOfFirst { it.id == addedId }
            if (at >= 0) {
                found = index + at
                break
            }
            index += section.tasks.size
        }
        found
    }
    // Run again when the row shows up (the database may be a moment behind the add).
    LaunchedEffect(addedId, addedIndex) {
        if (addedIndex >= 0) {
            if (reduceMotion) listState.scrollToItem(addedIndex) else listState.animateScrollToItem(addedIndex)
        }
    }

    // The tag tabs sit right under the header (the top bar, or the Today header).
    val tagTabs: @Composable () -> Unit = {
        if (!selecting && !state.isLoading) {
            TagTabs(state.tags, state.tagFilter, onTagFilterChange, state.tagTaskCounts, state.untaggedCount)
        }
    }

    reopenAsk?.let { task ->
        AlertDialog(
            onDismissRequest = { reopenAsk = null },
            title = { Text(stringResource(R.string.reopen_title)) },
            text = { Text(stringResource(R.string.reopen_message, task.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        reopenAsk = null
                        onToggle(task, false)
                    },
                ) { Text(stringResource(R.string.reopen_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { reopenAsk = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
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
            Column {
            TopAppBar(
                title = {
                    Text(
                        text = state.scope.title(),
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
                    if (canSearch) {
                        IconButton(onClick = onSearchClick) {
                            Icon(
                                painter = painterResource(R.drawable.ic_search),
                                contentDescription = stringResource(R.string.action_search),
                            )
                        }
                    }
                    val clearAction = if (state.scope == TaskScope.Completed && !state.isEmpty) {
                        listOf(MenuAction(stringResource(R.string.action_clear_completed), onClick = onClearCompleted))
                    } else {
                        emptyList()
                    }
                    OverflowButton(actions = sortActions + clearAction)
                },
            )
            tagTabs()
            }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // Nothing is added to a view of late or finished tasks.
            if (state.scope != TaskScope.Completed && state.scope != TaskScope.Overdue && !selecting) {
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
            state.isEmpty && !todayView -> {
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
                state.sections.forEach { section ->
                    if (section.date != null) {
                        item(key = "header-${section.date}") {
                            Text(
                                // Upcoming: the day tasks are due. Completed: the day they were finished.
                                text = if (state.scope == TaskScope.Completed) {
                                    formatDoneHeader(context, section.date, today, locale)
                                } else {
                                    formatDayHeader(context, section.date, today, locale)
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .semantics { heading() },
                            )
                        }
                    }
                    // Manual order keeps its own copy of the list for dragging, so finished rows leave at once there.
                    val rows = if (usesOrderCopy) order else withFinishing(section.tasks, finishing, section.date)
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
                                        // a finished counter shows a full count, not the one before the last step
                                        counter = task.counter?.let { it.copy(done = it.target) },
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
                            // In the Completed view a task is not opened: tapping it (or its ring) asks whether
                            // it should go back to the list, and the title is not struck through.
                            val inCompleted = state.scope == TaskScope.Completed
                            TaskRow(
                                task = task,
                                today = today,
                                onToggle = { completed ->
                                    if (inCompleted && !completed) {
                                        reopenAsk = task
                                    } else {
                                        if (completed && !reduceMotion && !usesOrderCopy) {
                                            lingerFinished()
                                        } else {
                                            finishing.remove(task.id)
                                        }
                                        onToggle(task, completed)
                                    }
                                },
                                onClick = {
                                    when {
                                        selecting -> selectionActions.onToggle(task.id)
                                        inCompleted -> reopenAsk = task
                                        else -> onOpenTask(task.id)
                                    }
                                },
                                strikeTitle = !inCompleted,
                                selected = task.id in state.selection,
                                highlighted = task.id == state.justAddedId,
                                onCelebrate = { origin, scale ->
                                    onCelebrate(origin, scale)
                                    // A repeating task that rests flies to the menu, where Completed is.
                                    if (task.recurrence != null && scale == 1f) onFly(origin, null)
                                },
                                onAddSubtask = { title -> onAddSubtask(task.id, title) },
                                onToggleSubtask = { subtaskId ->
                                    // Ticking the last open subtask finishes the task: let the row linger like a ring tick.
                                    val open = task.subtasks.filter { !it.isCompleted }
                                    if (!task.isCompleted && open.size == 1 && open.single().id == subtaskId &&
                                        !reduceMotion && !usesOrderCopy
                                    ) {
                                        lingerFinished()
                                    }
                                    onToggleSubtask(task.id, subtaskId)
                                },
                                onCounterAdd = { amount ->
                                    // The step that reaches the target finishes the task like a ring tick: the bird hops
                                    // and the row lingers.
                                    val counter = task.counter
                                    if (counter != null && !task.isCompleted && counter.done + amount >= counter.target) {
                                        onFinishedByCount()
                                        if (!reduceMotion && !usesOrderCopy) lingerFinished()
                                    }
                                    onCounterAdd(task.id, amount)
                                },
                                onCounterSet = { done ->
                                    val counter = task.counter
                                    if (counter != null && !task.isCompleted && done >= counter.target) {
                                        onFinishedByCount()
                                        if (!reduceMotion && !usesOrderCopy) lingerFinished()
                                    }
                                    onCounterSet(task.id, done)
                                },
                                // No drag grip for now: it felt odd (see TODO.md). TalkBack can still move rows up and down.
                                dragHandle = null,
                                onMoveUp = if (reorderable) ({ reorder.moveBy(task.id, -1) }) else null,
                                onMoveDown = if (reorderable) ({ reorder.moveBy(task.id, 1) }) else null,
                                onLongClick = {
                                    haptics.longPress()
                                    selectionActions.onToggle(task.id)
                                },
                                onDelete = { onDelete(task) },
                                onPinChange = { pinned -> onPinChange(task, pinned) },
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
                    title = state.scope.title(),
                    onMenuClick = onMenuClick,
                    onSearchClick = onSearchClick,
                    showSearch = canSearch,
                    sortActions = sortActions,
                    celebration = allDoneCelebration,
                    onWarmFeathers = onWarmFeathers,
                )
                tagTabs()
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
 * What an empty view says. Overdue is a happy place to be; Goals invite a first one; Upcoming
 * says nothing is due; Completed says nothing is finished yet.
 */
private fun TaskScope.emptyCopy(): EmptyCopy = when (this) {
    TaskScope.Overdue -> EmptyCopy(SceneKind.ClearSkies, R.string.scene_clear_skies, null)
    TaskScope.NoDate -> EmptyCopy(SceneKind.EmptyList, R.string.scene_empty_list, R.string.task_list_empty_body)
    TaskScope.Upcoming -> EmptyCopy(SceneKind.HungryChick, R.string.scene_hungry_chick, null)
    TaskScope.Today -> EmptyCopy(SceneKind.NothingDue, R.string.scene_nothing_due, null)
    TaskScope.Completed -> EmptyCopy(SceneKind.NothingDue, R.string.empty_view_title, R.string.empty_view_body)
}

private fun TaskSort.labelRes(): Int = when (this) {
    TaskSort.CREATED -> R.string.sort_created
    TaskSort.DUE_DATE -> R.string.sort_due_date
    TaskSort.PRIORITY -> R.string.sort_priority
    TaskSort.MANUAL -> R.string.sort_manual
}

/**
 * Swipe a row sideways to delete it, in either direction (it mirrors correctly in RTL). Deleting needs
 * a deliberate swipe: the row has to be dragged past [SWIPE_DELETE_FRACTION] of its width and let go,
 * and how fast the finger moved does not count, so a quick flick never deletes by accident. A short
 * tick tells the finger it has gone far enough; letting go before that brings the row back.
 */
@Composable
private fun SwipeToDeleteRow(
    onDelete: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    var widthPx by remember { mutableFloatStateOf(0f) }
    val ltr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val currentOnDelete by rememberUpdatedState(onDelete)

    val progress = if (widthPx > 0f) (abs(offset.value) / (widthPx * SWIPE_DELETE_FRACTION)).coerceIn(0f, 1f) else 0f
    val armed = progress >= 1f
    LaunchedEffect(armed) { if (armed) haptics.tick() }

    Box(
        modifier = modifier
            .onSizeChanged { widthPx = it.width.toFloat() }
            .draggable(
                orientation = Orientation.Horizontal,
                enabled = enabled,
                state = rememberDraggableState { delta ->
                    scope.launch(start = CoroutineStart.UNDISPATCHED) { offset.snapTo(offset.value + delta) }
                },
                onDragStopped = {
                    if (abs(offset.value) >= widthPx * SWIPE_DELETE_FRACTION) {
                        val target = if (offset.value > 0f) widthPx else -widthPx
                        offset.animateTo(target, tween(Dur.Short1))
                        currentOnDelete()
                        offset.snapTo(0f)
                    } else {
                        offset.animateTo(0f, tween(Dur.Short2))
                    }
                },
            ),
    ) {
        if (offset.value != 0f) {
            // The delete background sits behind the row, on the side it was pulled away from.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f + 0.65f * progress),
                        RoundedCornerShape(20.dp),
                    )
                    .padding(horizontal = 24.dp),
                contentAlignment = if ((offset.value > 0f) == ltr) Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = progress),
                )
            }
        }
        Box(Modifier.graphicsLayer { translationX = offset.value }) { content() }
    }
}

/** How far across the row a swipe must go, as a share of its width, before letting go deletes. */
private const val SWIPE_DELETE_FRACTION = 0.55f

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
                    MenuAction(stringResource(R.string.selection_tag)) { actions.onTag() },
                ),
            )
        },
    )
}

private enum class SelectionDialog { Tag }

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
