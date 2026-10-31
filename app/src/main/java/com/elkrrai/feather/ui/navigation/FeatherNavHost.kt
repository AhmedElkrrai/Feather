package com.elkrrai.feather.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elkrrai.feather.feature.tasklist.TaskListViewModel
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.elkrrai.feather.feature.focus.FocusScreen
import com.elkrrai.feather.feature.habits.HabitDetailScreen
import com.elkrrai.feather.feature.habits.HabitOverviewScreen
import com.elkrrai.feather.feature.habits.HabitTreeScreen
import com.elkrrai.feather.feature.habits.HabitsScreen
import com.elkrrai.feather.feature.search.SearchScreen
import com.elkrrai.feather.feature.settings.ReliabilityScreen
import com.elkrrai.feather.feature.settings.SettingsScreen
import com.elkrrai.feather.feature.taskedit.TaskEditScreen
import com.elkrrai.feather.feature.tasklist.TaskListScreen
import com.elkrrai.feather.feature.trash.TrashScreen
import com.elkrrai.feather.feature.rhythm.RhythmScreen
import com.elkrrai.feather.feature.nest.NestScreen
import com.elkrrai.feather.feature.tags.TagsScreen
import com.elkrrai.feather.ui.rememberReduceMotion

/**
 * @param openTaskId a task to open straight in the editor (from a reminder notification), or null
 * @param onOpenTaskHandled called once that request has been acted on
 */
@Composable
fun FeatherNavHost(
    modifier: Modifier = Modifier,
    openTaskId: String? = null,
    onOpenTaskHandled: () -> Unit = {},
    /** A task whose focus timer to open (from a focus notification), or null. */
    openFocusTaskId: String? = null,
    onOpenFocusHandled: () -> Unit = {},
) {
    val navController = rememberNavController()

    LaunchedEffect(openTaskId) {
        if (openTaskId != null) {
            navController.navigate(TaskEditRoute(openTaskId))
            onOpenTaskHandled()
        }
    }

    LaunchedEffect(openFocusTaskId) {
        if (openFocusTaskId != null) {
            navController.navigate(FocusRoute(openFocusTaskId))
            onOpenFocusHandled()
        }
    }

    val reduceMotion = rememberReduceMotion()
    NavHost(
        navController = navController,
        startDestination = TaskListRoute,
        modifier = modifier,
        enterTransition = featherEnter(reduceMotion),
        exitTransition = featherExit(reduceMotion),
        popEnterTransition = featherPopEnter(reduceMotion),
        popExitTransition = featherPopExit(reduceMotion),
    ) {
        composable<TaskListRoute> {
            TaskListScreen(
                onOpenTask = { taskId -> navController.navigate(TaskEditRoute(taskId)) },
                onOpenSearch = { navController.navigate(SearchRoute) },
                onOpenTrash = { navController.navigate(TrashRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onOpenRhythm = { navController.navigate(RhythmRoute) },
                onOpenHabits = { navController.navigate(HabitsRoute) },
                onOpenNest = { navController.navigate(NestRoute) },
            )
        }
        // The editor's ViewModel reads the task id from the route itself.
        composable<TaskEditRoute> {
            TaskEditScreen(
                onBack = { navController.goBack() },
                onOpenReliability = { navController.navigate(ReliabilityRoute) },
                onOpenFocus = { taskId -> navController.navigate(FocusRoute(taskId)) },
                // Replace the editor with the copy so Back returns to the list, not to the original.
                onOpenTask = { taskId ->
                    navController.navigate(TaskEditRoute(taskId)) {
                        popUpTo<TaskEditRoute> { inclusive = true }
                    }
                },
            )
        }
        composable<FocusRoute> {
            FocusScreen(onBack = { navController.goBack() })
        }
        composable<SearchRoute> {
            SearchScreen(
                onOpenTask = { taskId -> navController.navigate(TaskEditRoute(taskId)) },
                onBack = { navController.goBack() },
            )
        }
        composable<TrashRoute> {
            TrashScreen(onBack = { navController.goBack() })
        }
        composable<SettingsRoute> {
            SettingsScreen(
                onBack = { navController.goBack() },
                onOpenReliability = { navController.navigate(ReliabilityRoute) },
                onOpenTags = { navController.navigate(TagsRoute) },
            )
        }
        composable<TagsRoute> {
            TagsScreen(onBack = { navController.goBack() })
        }
        composable<NestRoute> {
            NestScreen(onBack = { navController.goBack() })
        }
        composable<HabitOverviewRoute> {
            HabitOverviewScreen(
                onBack = { navController.goBack() },
                // a leaf in the garden opens that week on the weekly page
                onOpenWeek = { habitId, week -> navController.navigate(HabitDetailRoute(habitId, week.toString())) },
            )
        }
        composable<HabitTreeRoute> {
            HabitTreeScreen(onBack = { navController.goBack() })
        }
        composable<HabitDetailRoute> {
            HabitDetailScreen(onBack = { navController.goBack() })
        }
        composable<HabitsRoute> { entry ->
            // The menu on this screen is the task list's: its counts, and its choice of view.
            val listEntry = remember(entry) { navController.getBackStackEntry<TaskListRoute>() }
            val listViewModel: TaskListViewModel = hiltViewModel(listEntry)
            val listState by listViewModel.uiState.collectAsStateWithLifecycle()
            HabitsScreen(
                listState = listState,
                onSelectScope = { scope ->
                    listViewModel.selectScope(scope)
                    navController.goBack()
                },
                onOpenTrash = { navController.navigate(TrashRoute) { popUpTo<TaskListRoute>() } },
                onOpenRhythm = { navController.navigate(RhythmRoute) { popUpTo<TaskListRoute>() } },
                onOpenNest = { navController.navigate(NestRoute) { popUpTo<TaskListRoute>() } },
                onOpenSettings = { navController.navigate(SettingsRoute) { popUpTo<TaskListRoute>() } },
            )
        }
        composable<RhythmRoute> {
            RhythmScreen(
                onBack = { navController.goBack() },
                onOpenHabit = { habitId -> navController.navigate(HabitDetailRoute(habitId)) },
                onOpenHabitOverview = { habitId -> navController.navigate(HabitOverviewRoute(habitId)) },
                onOpenHabitTree = { habitId -> navController.navigate(HabitTreeRoute(habitId)) },
            )
        }
        composable<ReliabilityRoute> {
            ReliabilityScreen(onBack = { navController.goBack() })
        }
    }
}

/**
 * Goes back one screen, unless there is none to go back to. A second tap on a back arrow during the
 * fade of the first would otherwise pop the task list itself (the arrow sits where the menu button
 * is) and leave nothing on screen.
 */
private fun NavController.goBack() {
    if (previousBackStackEntry != null) popBackStack()
}
