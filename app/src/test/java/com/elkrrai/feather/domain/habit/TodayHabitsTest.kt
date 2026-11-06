package com.elkrrai.feather.domain.habit

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayHabitsTest {

    // 2026-10-31 is a Saturday, the first day of the app's week.
    private val week = LocalDate.of(2026, 10, 31)
    private val today = week.plusDays(2)

    private val fajr = HabitStage("f", "Fajr", DayTime(TimeMode.WINDOW, 270, 360))
    private val dhuhr = HabitStage("d", "Dhuhr", DayTime(TimeMode.WINDOW, 720, 840))
    private val asr = HabitStage("a", "Asr", DayTime(TimeMode.WINDOW, 915, 990))
    private val maghrib = HabitStage("m", "Maghrib", DayTime(TimeMode.AT, 1090))
    private val isha = HabitStage("i", "Isha")
    private val all = listOf(fajr, dhuhr, asr, maghrib, isha)

    private fun habit(
        id: String = "h",
        kind: HabitKind = HabitKind.DAYS,
        showInToday: Boolean = true,
        fromStart: Boolean = false,
        time: DayTime = DayTime(),
        mode: StageMode = StageMode.NONE,
    ) = Habit(id, id, kind, sortOrder = 0, createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH, showInToday = showInToday, time = time, todayFromStart = fromStart, stageMode = mode)

    private fun pray(done: Set<String> = emptySet(), target: Int = 7, needed: Int = 5, fromStart: Boolean = false) = HabitState(
        habit = habit("pray", fromStart = fromStart, mode = StageMode.ORDERED),
        goals = listOf(HabitGoal(week, target, needed, all.map { it.id })),
        entries = emptyMap(),
        stages = all.associateBy { it.id },
        stageLog = mapOf(today to done),
    )

    private fun rows(vararg habits: HabitState, hour: Int, minute: Int = 0) =
        todayHabits(habits.toList(), today, LocalTime.of(hour, minute), week)

    private fun states(row: TodayHabit) = row.stages.map { it.state }

    @Test
    fun aHabitSwitchedOffForTodayDoesNotShow() {
        val off = HabitState(habit("x", showInToday = false), listOf(HabitGoal(week, 3)), emptyMap())
        assertTrue(rows(off, hour = 9).isEmpty())
    }

    @Test
    fun aPlainHabitShowsUntilTheWeeksGoalIsMet() {
        val open = HabitState(habit("r"), listOf(HabitGoal(week, 2)), mapOf(week to 1))
        val met = HabitState(habit("r"), listOf(HabitGoal(week, 2)), mapOf(week to 1, week.plusDays(1) to 1))
        val metToday = HabitState(habit("r"), listOf(HabitGoal(week, 2)), mapOf(week to 1, today to 1))

        assertEquals(listOf("r"), rows(open, hour = 9).map { it.id })
        // met by earlier days: it rests
        assertTrue(rows(met, hour = 9).isEmpty())
        // met by today's own finish: still listed, as done, so the row can say so before it leaves
        assertTrue(rows(metToday, hour = 9).single().doneToday)
    }

    @Test
    fun aHabitWithoutAGoalThisWeekDoesNotShow() {
        val later = HabitState(habit("r"), listOf(HabitGoal(week.plusWeeks(1), 3)), emptyMap())
        assertTrue(rows(later, hour = 9).isEmpty())
    }

    @Test
    fun aCountedHabitIsDoneWhenTheDaysCountIsReached() {
        val partway = HabitState(habit("w"), listOf(HabitGoal(week, 7, 3)), mapOf(today to 2))
        val reached = HabitState(habit("w"), listOf(HabitGoal(week, 7, 3)), mapOf(today to 3))

        assertFalse(rows(partway, hour = 9).single().doneToday)
        assertEquals(2, rows(partway, hour = 9).single().doneCount)
        assertTrue(rows(reached, hour = 9).single().doneToday)
    }

    @Test
    fun theStagesTakeTheirStateFromTheTimeOfDay() {
        // 3:40 PM: Fajr and Dhuhr are over, Asr's window is open, Maghrib is later, Isha has no time
        val row = rows(pray(done = setOf("f")), hour = 15, minute = 40).single()

        assertEquals(
            listOf(StageState.DONE, StageState.MISSED, StageState.NOW, StageState.NEXT, StageState.NEXT),
            states(row),
        )
        assertEquals("a", row.current?.stage?.id)
        assertEquals(1, row.doneCount)
        assertEquals(5, row.totalCount)
    }

    @Test
    fun onlyTheLatestStageThatIsDueIsTheOneItIsTimeFor() {
        // 7 PM: Maghrib (a moment, 6:10 PM) is due; Asr's window has ended
        val row = rows(pray(), hour = 19).single()

        assertEquals(StageState.MISSED, row.stages[2].state)
        assertEquals(StageState.NOW, row.stages[3].state)
        assertEquals("m", row.current?.stage?.id)
    }

    @Test
    fun beforeAnyTimeTheFirstStageIsNext() {
        val row = rows(pray(), hour = 3).single()

        assertTrue(states(row).all { it == StageState.NEXT })
        assertEquals("f", row.current?.stage?.id)
        assertEquals(270, row.sortMinute)
    }

    @Test
    fun aStageCanBeTickedLateAndInAnyOrder() {
        // Fajr and Dhuhr were missed; Asr and Maghrib are done: the day still lists what is left
        val row = rows(pray(done = setOf("a", "m")), hour = 21).single()

        assertEquals(listOf(StageState.MISSED, StageState.MISSED, StageState.DONE, StageState.DONE, StageState.NEXT), states(row))
        assertFalse(row.doneToday)
        assertEquals("i", row.current?.stage?.id)
    }

    @Test
    fun theDayIsDoneWhenEnoughStagesAreDone() {
        val strict = pray(done = setOf("f", "d", "a", "m"))
        val relaxed = pray(done = setOf("f", "d", "a", "m"), needed = 4)

        assertFalse(rows(strict, hour = 21).single().doneToday)
        assertTrue(rows(relaxed, hour = 21).single().doneToday)
    }

    @Test
    fun aHabitSetToShowFromItsTimeStaysOutOfTheWayUntilThen() {
        val early = pray(fromStart = true)

        assertTrue(rows(early, hour = 3).isEmpty())
        assertEquals(1, rows(early, hour = 5).size)
        // a finished day is never hidden
        val finished = pray(done = all.map { it.id }.toSet(), fromStart = true)
        assertEquals(1, rows(finished, hour = 3).size)
    }

    @Test
    fun rowsAreInTheOrderOfTheirTimesAndAHabitWithoutATimeComesLast() {
        val none = HabitState(habit("none"), listOf(HabitGoal(week, 3)), emptyMap())
        val late = HabitState(habit("late", time = DayTime(TimeMode.AT, 1200)), listOf(HabitGoal(week, 3)), emptyMap())
        val early = HabitState(habit("early", time = DayTime(TimeMode.WINDOW, 420, 480)), listOf(HabitGoal(week, 3)), emptyMap())

        assertEquals(listOf("early", "late", "none"), rows(none, late, early, hour = 6).map { it.id })
    }

    @Test
    fun aHabitFinishedTodayComesAfterTheOthers() {
        val early = HabitState(habit("early", time = DayTime(TimeMode.AT, 420)), listOf(HabitGoal(week, 3)), mapOf(today to 1))
        val open = HabitState(habit("open", time = DayTime(TimeMode.AT, 900)), listOf(HabitGoal(week, 3)), emptyMap())

        assertEquals(listOf("open", "early"), rows(early, open, hour = 9).map { it.id })
    }

    @Test
    fun anAmountHabitShowsWithItsWeeklyProgressUntilTheGoalIsMet() {
        val reading = HabitState(habit("read", HabitKind.AMOUNT), listOf(HabitGoal(week, 20)), mapOf(week to 12))
        val done = HabitState(habit("read", HabitKind.AMOUNT), listOf(HabitGoal(week, 20)), mapOf(week to 20))

        val row = rows(reading, hour = 9).single()
        assertEquals(12, row.weekTotal)
        assertEquals(20, row.weekTarget)
        assertNull(row.current)
        assertTrue(rows(done, hour = 9).isEmpty())
    }

    @Test
    fun anAmountThatReachesTheGoalTodayIsListedAsDoneSoTheRowCanCelebrate() {
        val reached = HabitState(habit("read", HabitKind.AMOUNT), listOf(HabitGoal(week, 20)), mapOf(today to 30))

        val row = rows(reached, hour = 9).single()
        assertTrue(row.doneToday)
        assertEquals(30, row.weekTotal)
    }
}
