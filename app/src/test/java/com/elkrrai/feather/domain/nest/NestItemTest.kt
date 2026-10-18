package com.elkrrai.feather.domain.nest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NestItemTest {
    @Test
    fun nothingIsEarnedAtTheStart() {
        assertTrue(earnedNestItems(NestFacts()).isEmpty())
    }

    @Test
    fun taskCountsEarnTheScarfTheGlassesAndTheHat() {
        assertEquals(setOf(NestItem.SCARF), earnedNestItems(NestFacts(tasksFinished = 10)))
        assertEquals(setOf(NestItem.SCARF, NestItem.GLASSES), earnedNestItems(NestFacts(tasksFinished = 25)))
        assertEquals(
            setOf(NestItem.SCARF, NestItem.GLASSES, NestItem.PARTY_HAT),
            earnedNestItems(NestFacts(tasksFinished = 100)),
        )
        assertFalse(NestItem.SCARF.isEarnedBy(NestFacts(tasksFinished = 9)))
    }

    @Test
    fun eachOtherMilestoneEarnsItsOwnItem() {
        assertEquals(setOf(NestItem.FAIRY_LIGHTS), earnedNestItems(NestFacts(backedUp = true)))
        assertEquals(setOf(NestItem.BUNTING), earnedNestItems(NestFacts(keptRepeaterFourWeeks = true)))
        assertEquals(setOf(NestItem.LANTERN), earnedNestItems(NestFacts(remindersSet = 10)))
        assertEquals(setOf(NestItem.MOSS), earnedNestItems(NestFacts(listsMade = 3)))
        assertEquals(setOf(NestItem.FLOWER_POT), earnedNestItems(NestFacts(voiceUsed = true)))
        assertTrue(earnedNestItems(NestFacts(remindersSet = 9, listsMade = 2)).isEmpty())
    }

    @Test
    fun progressCountsUpToTheGoalAndNoFurther() {
        assertEquals(4 to 10, NestItem.SCARF.progress(NestFacts(tasksFinished = 4)))
        assertEquals(10 to 10, NestItem.SCARF.progress(NestFacts(tasksFinished = 60)))
        assertEquals(2 to 3, NestItem.MOSS.progress(NestFacts(listsMade = 2)))
        assertNull(NestItem.FAIRY_LIGHTS.progress(NestFacts()))
    }

    @Test
    fun itemsAreFoundByTheirSavedId() {
        for (item in NestItem.entries) assertEquals(item, NestItem.fromId(item.id))
        assertNull(NestItem.fromId("unknown"))
    }
}
