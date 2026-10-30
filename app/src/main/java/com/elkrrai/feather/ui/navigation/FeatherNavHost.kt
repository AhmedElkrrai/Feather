package com.elkrrai.feather.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.elkrrai.feather.feature.focus.FocusScreen
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

    NavHost(
        navController = navController,
        startDestination = TaskListRoute,
        modifier = modifier,
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
                onBack = { navController.popBackStack() },
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
            FocusScreen(onBack = { navController.popBackStack() })
        }
        composable<SearchRoute> {
            SearchScreen(
                onOpenTask = { taskId -> navController.navigate(TaskEditRoute(taskId)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<TrashRoute> {
            TrashScreen(onBack = { navController.popBackStack() })
        }
        composable<SettingsRoute> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenReliability = { navController.navigate(ReliabilityRoute) },
                onOpenTags = { navController.navigate(TagsRoute) },
            )
        }
        composable<TagsRoute> {
            TagsScreen(onBack = { navController.popBackStack() })
        }
        composable<NestRoute> {
            NestScreen(onBack = { navController.popBackStack() })
        }
        composable<HabitsRoute> {
            HabitsScreen(onBack = { navController.popBackStack() })
        }
        composable<RhythmRoute> {
            RhythmScreen(onBack = { navController.popBackStack() })
        }
        composable<ReliabilityRoute> {
            ReliabilityScreen(onBack = { navController.popBackStack() })
        }
    }
}
