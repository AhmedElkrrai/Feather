package com.elkrrai.feather.ui.navigation

import kotlinx.serialization.Serializable

/** Type-safe navigation destinations. */
@Serializable
data object TaskListRoute

/** [taskId] is null when creating a new task. */
@Serializable
data class TaskEditRoute(val taskId: String? = null)

/** The focus timer of one task. */
@Serializable
data class FocusRoute(val taskId: String)

@Serializable
data object SearchRoute

@Serializable
data object TrashRoute

@Serializable
data object SettingsRoute

@Serializable
data object ReliabilityRoute

@Serializable
data object RhythmRoute

@Serializable
data object NestRoute

@Serializable
data object TagsRoute
