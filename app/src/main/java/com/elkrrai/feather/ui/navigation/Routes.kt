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
data object HabitsRoute

/** One habit looked back on, week by week. */
@Serializable
data class HabitDetailRoute(val habitId: String, val weekStart: String? = null)

/** One habit, every week of it at once. */
@Serializable
data class HabitOverviewRoute(val habitId: String)

/** One habit as a tree, a leaf for each week that met the goal. */
@Serializable
data class HabitTreeRoute(val habitId: String)

@Serializable
data object RhythmRoute

@Serializable
data object NestRoute

@Serializable
data object TagsRoute
