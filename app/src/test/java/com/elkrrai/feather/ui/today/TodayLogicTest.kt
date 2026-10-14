package com.elkrrai.feather.ui.today

import com.elkrrai.feather.ui.brand.BirdMood
import com.elkrrai.feather.ui.theme.DayPhase
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayLogicTest {

    private fun at(hour: Int, minute: Int = 0) = LocalTime.of(hour, minute)

    @Test
    fun greetingsChangeAt5Then12Then17Then21() {
        assertEquals(Greeting.Night, greetingFor(at(4, 59)))
        assertEquals(Greeting.Morning, greetingFor(at(5)))
        assertEquals(Greeting.Morning, greetingFor(at(11, 59)))
        assertEquals(Greeting.Afternoon, greetingFor(at(12)))
        assertEquals(Greeting.Afternoon, greetingFor(at(16, 59)))
        assertEquals(Greeting.Evening, greetingFor(at(17)))
        assertEquals(Greeting.Evening, greetingFor(at(20, 59)))
        assertEquals(Greeting.Night, greetingFor(at(21)))
        assertEquals(Greeting.Night, greetingFor(at(0)))
    }

    @Test
    fun theSkyChangesAt5Then10Then17Then20() {
        assertEquals(DayPhase.Night, dayPhaseFor(at(4, 59)))
        assertEquals(DayPhase.Dawn, dayPhaseFor(at(5)))
        assertEquals(DayPhase.Dawn, dayPhaseFor(at(9, 59)))
        assertEquals(DayPhase.Day, dayPhaseFor(at(10)))
        assertEquals(DayPhase.Day, dayPhaseFor(at(16, 59)))
        assertEquals(DayPhase.Dusk, dayPhaseFor(at(17)))
        assertEquals(DayPhase.Dusk, dayPhaseFor(at(19, 59)))
        assertEquals(DayPhase.Night, dayPhaseFor(at(20)))
    }

    @Test
    fun onlyTheNightSkyHasAMoon() {
        assertTrue(showsMoon(DayPhase.Night))
        for (phase in listOf(DayPhase.Dawn, DayPhase.Day, DayPhase.Dusk)) assertFalse(showsMoon(phase))
    }

    @Test
    fun plannedIsWhatIsLeftPlusWhatIsDone() {
        val status = TodayStatus(left = 3, done = 2)

        assertEquals(5, status.planned)
        assertFalse(status.allDone)
    }

    @Test
    fun everythingIsDoneOnlyWhenSomethingWasPlannedAndNothingIsLeft() {
        assertTrue(TodayStatus(left = 0, done = 4).allDone)
        assertFalse(TodayStatus(left = 0, done = 0).allDone)
    }

    @Test
    fun theBirdSleepsOnAnEmptyDay() {
        assertEquals(BirdMood.SLEEPING, todayBirdMood(TodayStatus(0, 0), DayPhase.Day))
    }

    @Test
    fun theBirdIsHappyWhenEverythingIsDone() {
        assertEquals(BirdMood.HAPPY, todayBirdMood(TodayStatus(0, 3), DayPhase.Day))
        assertEquals(BirdMood.HAPPY, todayBirdMood(TodayStatus(0, 3), DayPhase.Night))
    }

    @Test
    fun theBirdGetsSleepyLateWithAtMostTwoThingsLeft() {
        assertEquals(BirdMood.SLEEPING, todayBirdMood(TodayStatus(2, 5), DayPhase.Night))
        assertEquals(BirdMood.IDLE, todayBirdMood(TodayStatus(3, 5), DayPhase.Night))
        assertEquals(BirdMood.IDLE, todayBirdMood(TodayStatus(2, 5), DayPhase.Dusk))
    }

    @Test
    fun otherwiseTheBirdJustSitsThere() {
        assertEquals(BirdMood.IDLE, todayBirdMood(TodayStatus(4, 0), DayPhase.Dawn))
    }

    @Test
    fun noLeavesAreFilledForAnEmptyDay() {
        assertEquals(0, leavesFilled(TodayStatus(0, 0)))
    }

    @Test
    fun leavesFollowTheShareThatIsDone() {
        assertEquals(0, leavesFilled(TodayStatus(left = 4, done = 0)))
        assertEquals(3, leavesFilled(TodayStatus(left = 2, done = 3)))
        assertEquals(5, leavesFilled(TodayStatus(left = 0, done = 6)))
    }

    @Test
    fun aLeafIsNeverFilledForNothingNorAllFilledWhileSomethingIsLeft() {
        // 1 of 40 done would round to no leaf at all; show one.
        assertEquals(1, leavesFilled(TodayStatus(left = 39, done = 1)))
        // 39 of 40 done would round to every leaf; keep one open.
        assertEquals(4, leavesFilled(TodayStatus(left = 1, done = 39)))
    }
}
