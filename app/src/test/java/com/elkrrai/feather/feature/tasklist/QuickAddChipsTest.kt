package com.elkrrai.feather.feature.tasklist

import com.elkrrai.feather.domain.quickadd.QuickAddField
import com.elkrrai.feather.ui.theme.NestLightScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickAddChipsTest {

    @Test
    fun aNewChipEarnsATick() {
        assertTrue(chipTickDue(previousCount = 0, count = 1, now = 1_000L, lastTick = 0L))
        assertTrue(chipTickDue(previousCount = 2, count = 3, now = 5_000L, lastTick = 1_000L))
    }

    @Test
    fun fewerOrTheSameNumberOfChipsDoNot() {
        assertFalse(chipTickDue(previousCount = 2, count = 2, now = 5_000L, lastTick = 0L))
        assertFalse(chipTickDue(previousCount = 3, count = 1, now = 5_000L, lastTick = 0L))
    }

    @Test
    fun ticksCloserThan400MillisApartAreSkipped() {
        assertFalse(chipTickDue(previousCount = 0, count = 1, now = 1_399L, lastTick = 1_000L))
        assertTrue(chipTickDue(previousCount = 0, count = 1, now = 1_400L, lastTick = 1_000L))
    }

    @Test
    fun chipsArrivingTogetherComeOneAfterTheOtherFortyMillisApart() {
        assertEquals(0, chipDelayMillis(0))
        assertEquals(40, chipDelayMillis(1))
        assertEquals(120, chipDelayMillis(3))
        assertEquals(0, chipDelayMillis(-1))
    }

    @Test
    fun datesTimesAndRepeatsShareAnAmberTint() {
        val date = fieldTint(QuickAddField.DATE, NestLightScheme)

        assertEquals(NestLightScheme.secondaryContainer, date.container)
        assertEquals(date.container, fieldTint(QuickAddField.TIME, NestLightScheme).container)
        assertEquals(date.container, fieldTint(QuickAddField.REPEAT, NestLightScheme).container)
    }

    @Test
    fun priorityIsCoralAndTagsAndListsAreTeal() {
        assertEquals(NestLightScheme.tertiaryContainer, fieldTint(QuickAddField.PRIORITY, NestLightScheme).container)
        assertEquals(NestLightScheme.primaryContainer, fieldTint(QuickAddField.TAGS, NestLightScheme).container)
        assertEquals(NestLightScheme.primaryContainer, fieldTint(QuickAddField.LIST, NestLightScheme).container)
    }

    @Test
    fun everyPartHasATint() {
        for (field in QuickAddField.entries) {
            assertTrue(fieldTint(field, NestLightScheme).content != fieldTint(field, NestLightScheme).container)
        }
    }
}
