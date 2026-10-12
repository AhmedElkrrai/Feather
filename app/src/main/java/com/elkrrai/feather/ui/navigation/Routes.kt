package com.elkrrai.feather.ui.navigation

import kotlinx.serialization.Serializable

/** Type-safe navigation destinations. */
@Serializable
data object TaskListRoute

/** [taskId] is null when creating a new task. */
@Serializable
data class TaskEditRoute(val taskId: String? = null)

@Serializable
data object SearchRoute

@Serializable
data object TrashRoute
