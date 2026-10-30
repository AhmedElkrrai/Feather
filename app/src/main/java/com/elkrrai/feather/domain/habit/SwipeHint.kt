package com.elkrrai.feather.domain.habit

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** The most times the "swipe to see earlier weeks" hint is ever shown. */
const val SWIPE_HINT_MAX = 3

/** After a showing, the weeks that must pass before the hint can come again. */
const val SWIPE_HINT_MIN_WEEKS_APART = 2

/** Once the hint is allowed again, the chance that a visit to the weekly page shows it. */
const val SWIPE_HINT_CHANCE = 0.5f

/** What was shown so far: how many times, and the week (its first day) of the last time. */
data class SwipeHintHistory(val shown: Int = 0, val lastWeek: LocalDate? = null)

/**
 * Whether a visit to the weekly page shows the swipe hint. The first time it always does, as long as
 * there is an earlier week to swipe to. After that it comes back at most [SWIPE_HINT_MAX] times in all,
 * never twice in a week and not before [SWIPE_HINT_MIN_WEEKS_APART] weeks have passed, and then only on
 * some visits: [roll] is a random number from 0 up to 1.
 */
fun shouldShowSwipeHint(
    history: SwipeHintHistory,
    hasEarlierWeek: Boolean,
    thisWeek: LocalDate,
    roll: Float,
): Boolean {
    if (!hasEarlierWeek || history.shown >= SWIPE_HINT_MAX) return false
    val last = history.lastWeek
    if (history.shown == 0 || last == null) return true
    if (ChronoUnit.WEEKS.between(last, thisWeek) < SWIPE_HINT_MIN_WEEKS_APART) return false
    return roll < SWIPE_HINT_CHANCE
}
