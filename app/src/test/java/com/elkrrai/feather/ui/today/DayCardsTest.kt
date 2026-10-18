package com.elkrrai.feather.ui.today

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DayCardsTest {
    private val busy = TodayStatus(left = 3, done = 1)
    private val on = DayCardSwitches()

    private fun card(hour: Int, status: TodayStatus = busy, switches: DayCardSwitches = on) =
        dayCardFor(LocalTime.of(hour, 0), status, waiting = 1, firstTitle = "Call mom", switches = switches)

    @Test
    fun theMorningCardShowsFiveToElevenWhenSomethingIsPlanned() {
        assertNull(card(4))
        assertEquals(DayCardKind.Morning, card(5)?.kind)
        assertEquals(DayCardKind.Morning, card(11)?.kind)
        assertNull(card(12))
        assertNull(card(9, status = TodayStatus(left = 0, done = 2)))
    }

    @Test
    fun theEveningCardShowsAfterSevenWhenTheDayHadSomethingInIt() {
        assertNull(card(18))
        assertEquals(DayCardKind.Evening, card(19)?.kind)
        assertEquals(DayCardKind.Evening, card(23)?.kind)
        assertEquals(DayCardKind.Evening, card(21, status = TodayStatus(left = 0, done = 4))?.kind)
        assertNull(card(21, status = TodayStatus()))
    }

    @Test
    fun aCardPutAwayStaysAwayForTheDay() {
        assertNull(card(9, switches = on.copy(morningDismissed = true)))
        assertNull(card(20, switches = on.copy(eveningDismissed = true)))
        // The other card is not affected.
        assertEquals(DayCardKind.Evening, card(20, switches = on.copy(morningDismissed = true))?.kind)
    }

    @Test
    fun eachCardHasItsOwnSwitch() {
        assertNull(card(9, switches = on.copy(morning = false)))
        assertNull(card(20, switches = on.copy(evening = false)))
    }

    @Test
    fun theCardCarriesTheNumbersAndTheFirstTask() {
        val card = card(9)!!

        assertEquals(3, card.left)
        assertEquals(1, card.done)
        assertEquals(1, card.waiting)
        assertEquals("Call mom", card.firstTitle)
    }
}
