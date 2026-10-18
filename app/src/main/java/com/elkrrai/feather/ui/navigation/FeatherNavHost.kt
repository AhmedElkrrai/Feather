package com.elkrrai.feather.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.elkrrai.feather.feature.search.SearchScreen
import com.elkrrai.feather.feature.settings.ReliabilityScreen
import com.elkrrai.feather.feature.settings.SettingsScreen
import com.elkrrai.feather.feature.taskedit.TaskEditScreen
import com.elkrrai.feather.feature.tasklist.TaskListScreen
import com.elkrrai.feather.feature.trash.TrashScreen
import com.elkrrai.feather.feature.rhythm.RhythmScreen
import com.elkrrai.feather.feature.nest.NestScreen

/**
 * @param openTaskId a task to open straight in the editor (from a reminder notification), or null
 * @param onOpenTaskHandled called once that request has been acted on
 */
@Composable
fun FeatherNavHost(
    modifier: Modifier = Modifier,
    openTaskId: String? = null,
    onOpenTaskHandled: () -> Unit = {},
) {
    val navController = rememberNavController()

    LaunchedEffect(openTaskId) {
        if (openTaskId != null) {
            navController.navigate(TaskEditRoute(openTaskId))
            onOpenTaskHandled()
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
            )
        }
        // The editor's ViewModel reads the task id from the route itself.
        composable<TaskEditRoute> {
            TaskEditScreen(
                onBack = { navController.popBackStack() },
                onOpenReliability = { navController.navigate(ReliabilityRoute) },
                // Replace the editor with the copy so Back returns to the list, not to the original.
                onOpenTask = { taskId ->
                    navController.navigate(TaskEditRoute(taskId)) {
                        popUpTo<TaskEditRoute> { inclusive = true }
                    }
                },
            )
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
                onOpenNest = { navController.navigate(NestRoute) },
            )
        }
        composable<NestRoute> {
            NestScreen(onBack = { navController.popBackStack() })
        }
        composable<RhythmRoute> {
            RhythmScreen(onBack = { navController.popBackStack() })
        }
        composable<ReliabilityRoute> {
            ReliabilityScreen(onBack = { navController.popBackStack() })
        }
    }
}
