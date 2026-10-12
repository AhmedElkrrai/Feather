package com.elkrrai.feather.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.elkrrai.feather.feature.taskedit.TaskEditScreen
import com.elkrrai.feather.feature.tasklist.TaskListScreen

@Composable
fun FeatherNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = TaskListRoute,
        modifier = modifier,
    ) {
        composable<TaskListRoute> {
            TaskListScreen(
                onOpenTask = { taskId -> navController.navigate(TaskEditRoute(taskId)) },
            )
        }
        // The editor's ViewModel reads the task id from the route itself.
        composable<TaskEditRoute> {
            TaskEditScreen(onBack = { navController.popBackStack() })
        }
    }
}
