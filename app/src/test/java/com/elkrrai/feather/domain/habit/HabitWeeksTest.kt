package com.elkrrai.feather.domain.habit

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitWeeksTest {

    // 2026-10-26 is a Monday
    private val week1 = LocalDate.of(2026, 10, 26)
    private val week2 = week1.plusWeeks(1)
    private val week3 = week1.plusWeeks(2)
    private val now = Instant.ofEpochSecond(1_000)

    private fun habit(kind: HabitKind) = Habit("h", "Habit", kind, "pages", sortOrder = 1, createdAt = now, updatedAt = now)

    private fun state(
        kind: HabitKind,
        goals: List<HabitGoal>,
        entries: Map<LocalDate, Int> = emptyMap(),
    ) = HabitState(habit(kind), goals, entries)

    @Test
    fun the_week_starts_on_the_first_day_chosen() {
        assertEquals(week1, weekStartOf(week1.plusDays(4), DayOfWeek.MONDAY))
        assertEquals(week1, weekStartOf(week1, DayOfWeek.MONDAY))
        assertEquals(LocalDate.of(2026, 10, 24), weekStartOf(week1.plusDays(4), DayOfWeek.SATURDAY))
    }

    @Test
    fun the_goal_in_force_is_the_latest_one_that_has_begun() {
        val goals = listOf(HabitGoal(week1, 20), HabitGoal(week3, 25))
        assertNull(goals.forWeek(week1.minusWeeks(1)))
        assertEquals(20, goals.forWeek(week1)?.target)
        assertEquals(20, goals.forWeek(week2)?.target)
        assertEquals(25, goals.forWeek(week3)?.target)
        assertEquals(25, goals.queuedAfter(week2)?.target)
        assertNull(goals.queuedAfter(week3))
    }

    @Test
    fun amounts_add_up_over_the_week() {
        val s = state(
            HabitKind.AMOUNT,
            listOf(HabitGoal(week1, 20)),
            mapOf(week1 to 10, week1.plusDays(3) to 4, week1.plusDays(4) to 3),
        )
        val progress = s.week(week1)
        assertEquals(17, progress.total)
        assertFalse(progress.met)
        assertEquals(0.85f, progress.fraction, 0.0001f)
    }

    @Test
    fun going_over_the_goal_still_counts_as_met_and_shows_full() {
        val s = state(HabitKind.AMOUNT, listOf(HabitGoal(week1, 20)), mapOf(week1 to 25))
        assertTrue(s.week(week1).met)
        assertEquals(1f, s.week(week1).fraction, 0.0001f)
    }

    @Test
    fun a_day_counts_when_it_reaches_the_daily_target() {
        val s = state(
            HabitKind.DAYS,
            listOf(HabitGoal(week1, target = 3, dailyTarget = 5)),
            mapOf(week1 to 5, week1.plusDays(1) to 4, week1.plusDays(2) to 6),
        )
        val progress = s.week(week1)
        assertEquals(2, progress.total)
        assertFalse(progress.met)
    }

    @Test
    fun a_goal_edited_later_does_not_change_an_earlier_week() {
        val s = state(
            HabitKind.AMOUNT,
            listOf(HabitGoal(week1, 20), HabitGoal(week2, 30)),
            mapOf(week1 to 20, week2 to 20),
        )
        assertTrue(s.week(week1).met)
        assertFalse(s.week(week2).met)
    }

    @Test
    fun weeks_in_a_row_count_met_weeks_up_to_the_last_finished_one() {
        val s = state(
            HabitKind.AMOUNT,
            listOf(HabitGoal(week1, 20)),
            mapOf(week1 to 20, week2 to 22),
        )
        // week3 has started but is not met yet: the two finished weeks count
        assertEquals(2, s.weeksMetInARow(week3))
        // a met current week joins the run
        assertEquals(3, s.copy(entries = s.entries + (week3 to 20)).weeksMetInARow(week3))
    }

    @Test
    fun a_missed_week_ends_the_run_and_so_does_the_start_of_the_habit() {
        val missed = state(
            HabitKind.AMOUNT,
            listOf(HabitGoal(week1, 20)),
            mapOf(week1 to 20, week2 to 5),
        )
        assertEquals(0, missed.weeksMetInARow(week3))
        val onlyBeforeStart = state(HabitKind.AMOUNT, listOf(HabitGoal(week3, 20)))
        assertEquals(0, onlyBeforeStart.weeksMetInARow(week3))
    }

    @Test
    fun only_days_of_the_current_week_up_to_today_can_be_logged() {
        val today = week1.plusDays(3)
        assertTrue(canLog(week1, week1, today))
        assertTrue(canLog(today, week1, today))
        assertFalse(canLog(today.plusDays(1), week1, today))
        assertFalse(canLog(week1.minusDays(1), week1, today))
    }

    @Test
    fun theOverviewCountsThisWeekOnlyOnceItIsMet() {
        val goals = listOf(HabitGoal(week1, 20))
        val partial = state(HabitKind.AMOUNT, goals, mapOf(week1 to 20, week2 to 25, week3 to 5))
        val during = partial.overview(week3, DayOfWeek.MONDAY)
        // the two finished weeks were met; the third is still going and not yet met, so it is not counted
        assertEquals(3, during.weeks.size)
        assertEquals(2, during.weeksMet)
        assertEquals(2, during.weeksSoFar)
        assertEquals(50, during.total)
        assertEquals(week2, during.best?.weekStart)

        val met = state(HabitKind.AMOUNT, goals, mapOf(week1 to 20, week2 to 25, week3 to 20)).overview(week3, DayOfWeek.MONDAY)
        assertEquals(3, met.weeksMet)
        assertEquals(3, met.weeksSoFar)
        assertEquals(3, met.inARow)
    }

    @Test
    fun theOverviewStartsAtTheFirstWeekWithAnythingInIt() {
        val s = state(HabitKind.DAYS, listOf(HabitGoal(week2, 3)), mapOf(week2.plusDays(1) to 1))
        val all = s.overview(week3, DayOfWeek.MONDAY)
        assertEquals(listOf(week2, week3), all.weeks.map { it.weekStart })
        assertNull(state(HabitKind.AMOUNT, listOf(HabitGoal(week1, 20))).overview(week1, DayOfWeek.MONDAY).best)
    }
}
