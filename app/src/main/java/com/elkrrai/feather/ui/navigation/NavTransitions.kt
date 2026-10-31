package com.elkrrai.feather.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute

/*
 * How screens come and go. Two kinds of move, and in both the old screen is gone before the new one
 * shows, so the two are never on screen together:
 * - between the places of the menu (Today, Settings, Progress, ...): fade through, the old screen
 *   fades out and the new one fades in a little larger than it settles.
 * - going deeper (the task editor, a habit's weeks, search, ...): the same, with a short slide toward
 *   the end of the screen, and back again when coming out. The slide follows the layout direction.
 * With reduced motion, a quick plain fade.
 */

private const val FADE_OUT_MILLIS = 90
private const val FADE_IN_MILLIS = 210
private const val SLIDE_MILLIS = 280
private const val REDUCED_MILLIS = 100

/** The slide is this part of the screen's width: about 30dp on a phone. */
private const val SLIDE_DIVISOR = 12
private const val INCOMING_SCALE = 0.92f

private typealias Scope = AnimatedContentTransitionScope<NavBackStackEntry>

/** Screens that open from another screen to go into something, as against the places of the menu. */
private fun NavBackStackEntry.isDrillDown(): Boolean = destination.let {
    it.hasRoute<TaskEditRoute>() ||
        it.hasRoute<FocusRoute>() ||
        it.hasRoute<SearchRoute>() ||
        it.hasRoute<ReliabilityRoute>() ||
        it.hasRoute<TagsRoute>() ||
        it.hasRoute<HabitDetailRoute>() ||
        it.hasRoute<HabitOverviewRoute>() ||
        it.hasRoute<HabitTreeRoute>()
}

private fun fadeIn210() = fadeIn(tween(FADE_IN_MILLIS, delayMillis = FADE_OUT_MILLIS, easing = LinearOutSlowInEasing))

private fun fadeOut90() = fadeOut(tween(FADE_OUT_MILLIS, easing = FastOutLinearInEasing))

private fun fadeThroughIn(): EnterTransition =
    fadeIn210() + scaleIn(
        tween(FADE_IN_MILLIS, delayMillis = FADE_OUT_MILLIS, easing = LinearOutSlowInEasing),
        initialScale = INCOMING_SCALE,
    )

private fun Scope.slideIn(direction: SlideDirection): EnterTransition =
    slideIntoContainer(
        direction,
        tween<IntOffset>(SLIDE_MILLIS, easing = FastOutSlowInEasing),
        initialOffset = { it / SLIDE_DIVISOR },
    ) + fadeIn210()

private fun Scope.slideOut(direction: SlideDirection): ExitTransition =
    slideOutOfContainer(
        direction,
        tween<IntOffset>(SLIDE_MILLIS, easing = FastOutSlowInEasing),
        targetOffset = { it / SLIDE_DIVISOR },
    ) + fadeOut90()

/** A screen opens: the one that opens, going deeper, slides in. */
internal fun featherEnter(reduceMotion: Boolean): Scope.() -> EnterTransition = {
    when {
        reduceMotion -> fadeIn(tween(REDUCED_MILLIS))
        targetState.isDrillDown() -> slideIn(SlideDirection.Start)
        else -> fadeThroughIn()
    }
}

/** The screen that was there when another one opens. */
internal fun featherExit(reduceMotion: Boolean): Scope.() -> ExitTransition = {
    when {
        reduceMotion -> fadeOut(tween(REDUCED_MILLIS))
        targetState.isDrillDown() -> slideOut(SlideDirection.Start)
        else -> fadeOut90()
    }
}

/** Back: the screen that comes back into view. */
internal fun featherPopEnter(reduceMotion: Boolean): Scope.() -> EnterTransition = {
    when {
        reduceMotion -> fadeIn(tween(REDUCED_MILLIS))
        initialState.isDrillDown() -> slideIn(SlideDirection.End)
        else -> fadeThroughIn()
    }
}

/** Back: the screen that is left. */
internal fun featherPopExit(reduceMotion: Boolean): Scope.() -> ExitTransition = {
    when {
        reduceMotion -> fadeOut(tween(REDUCED_MILLIS))
        initialState.isDrillDown() -> slideOut(SlideDirection.End)
        else -> fadeOut90()
    }
}
