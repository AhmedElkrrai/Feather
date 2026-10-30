package com.elkrrai.feather.domain.habit

import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeHintTest {

    private val week = LocalDate.of(2026, 10, 31)
    private val never = 0.99f
    private val always = 0f

    @Test
    fun the_first_time_it_always_shows_when_there_is_an_earlier_week() {
        assertTrue(shouldShowSwipeHint(SwipeHintHistory(), hasEarlierWeek = true, thisWeek = week, roll = never))
    }

    @Test
    fun it_never_shows_without_an_earlier_week() {
        assertFalse(shouldShowSwipeHint(SwipeHintHistory(), hasEarlierWeek = false, thisWeek = week, roll = always))
    }

    @Test
    fun it_does_not_come_twice_in_the_same_week() {
        val history = SwipeHintHistory(shown = 1, lastWeek = week)
        assertFalse(shouldShowSwipeHint(history, true, week, always))
    }

    @Test
    fun it_waits_a_couple_of_weeks_between_showings() {
        val history = SwipeHintHistory(shown = 1, lastWeek = week)
        assertFalse(shouldShowSwipeHint(history, true, week.plusWeeks(1), always))
        assertTrue(shouldShowSwipeHint(history, true, week.plusWeeks(2), always))
    }

    @Test
    fun later_showings_only_come_on_some_visits() {
        val history = SwipeHintHistory(shown = 1, lastWeek = week)
        assertTrue(shouldShowSwipeHint(history, true, week.plusWeeks(3), roll = SWIPE_HINT_CHANCE - 0.01f))
        assertFalse(shouldShowSwipeHint(history, true, week.plusWeeks(3), roll = SWIPE_HINT_CHANCE))
    }

    @Test
    fun it_stops_after_three_showings() {
        val history = SwipeHintHistory(shown = SWIPE_HINT_MAX, lastWeek = week)
        assertFalse(shouldShowSwipeHint(history, true, week.plusWeeks(20), always))
    }
}
