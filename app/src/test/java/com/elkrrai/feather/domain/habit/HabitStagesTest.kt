package com.elkrrai.feather.domain.habit

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitStagesTest {

    private val week1 = LocalDate.of(2026, 10, 31)
    private val week2 = week1.plusWeeks(1)
    private val week3 = week1.plusWeeks(2)

    private val fajr = HabitStage("f", "Fajr", DayTime(TimeMode.WINDOW, 270, 360))
    private val dhuhr = HabitStage("d", "Dhuhr", DayTime(TimeMode.AT, 720))
    private val asr = HabitStage("a", "Asr")

    private fun habit(kind: HabitKind = HabitKind.DAYS) =
        Habit("h", "Pray", kind, sortOrder = 0, createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH)

    private fun state(goals: List<HabitGoal>, stageLog: Map<LocalDate, Set<String>> = emptyMap()) = HabitState(
        habit = habit(),
        goals = goals,
        entries = emptyMap(),
        stages = listOf(fajr, dhuhr, asr).associateBy { it.id },
        stageLog = stageLog,
    )

    @Test
    fun aWeekHasTheStagesOfTheGoalInForceThatWeek() {
        val s = state(listOf(HabitGoal(week1, 7, 2, listOf("f", "d")), HabitGoal(week3, 7, 3, listOf("f", "d", "a"))))

        assertEquals(listOf(fajr, dhuhr), s.stagesFor(week1))
        assertEquals(listOf(fajr, dhuhr), s.stagesFor(week2))
        assertEquals(listOf(fajr, dhuhr, asr), s.stagesFor(week3))
    }

    @Test
    fun aListThatWaitsForNextWeekIsKnownBeforeItStarts() {
        val s = state(listOf(HabitGoal(week1, 7, 1, listOf("f")), HabitGoal(week2, 7, 2, listOf("d", "f"))))

        assertEquals(listOf(fajr), s.stagesFor(week1))
        assertEquals(listOf(dhuhr, fajr), s.queuedStagesAfter(week1))
        assertNull(s.queuedStagesAfter(week2))
    }

    @Test
    fun aHabitWithoutStagesHasNone() {
        assertTrue(state(listOf(HabitGoal(week1, 3))).stagesFor(week1).isEmpty())
        assertTrue(state(emptyList()).stagesFor(week1).isEmpty())
    }

    @Test
    fun theDoneStagesOfADayAreWhatWasLoggedThatDay() {
        val s = state(listOf(HabitGoal(week1, 7, 2, listOf("f", "d"))), mapOf(week1 to setOf("f")))

        assertEquals(setOf("f"), s.stagesDoneOn(week1))
        assertTrue(s.stagesDoneOn(week2).isEmpty())
    }

    @Test
    fun aTimeIsMadeSenseOf() {
        assertEquals(DayTime(), DayTime(TimeMode.NONE, 100, 200).normalized())
        assertEquals(DayTime(), DayTime(TimeMode.AT, null).normalized())
        assertEquals(DayTime(TimeMode.AT, 720), DayTime(TimeMode.AT, 720, 800).normalized())
        assertEquals(DayTime(TimeMode.WINDOW, 270, 360), DayTime(TimeMode.WINDOW, 270, 360).normalized())
        // a window that does not end after it starts is one moment
        assertEquals(DayTime(TimeMode.AT, 360), DayTime(TimeMode.WINDOW, 360, 360).normalized())
        assertEquals(DayTime(TimeMode.AT, 360), DayTime(TimeMode.WINDOW, 360, null).normalized())
        // minutes stay inside the day
        assertEquals(DayTime(TimeMode.AT, 1439), DayTime(TimeMode.AT, 5000).normalized())
    }

    @Test
    fun theFormStartsFromTheGoalThatWaitsIfThereIsOne() {
        val s = HabitState(
            habit = habit().copy(showInToday = true, stageMode = StageMode.ORDERED),
            goals = listOf(HabitGoal(week1, 7, 1, listOf("f")), HabitGoal(week2, 5, 2, listOf("f", "d"))),
            entries = emptyMap(),
            stages = listOf(fajr, dhuhr).associateBy { it.id },
        )

        val draft = s.toDraft(week1)

        assertEquals(5, draft.target)
        assertEquals(2, draft.dailyTarget)
        assertEquals(listOf("f", "d"), draft.stages.map { it.id })
        assertTrue(draft.showInToday)
        assertEquals(StageMode.ORDERED, draft.stageMode)
    }
}
