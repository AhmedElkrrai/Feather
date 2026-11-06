package com.elkrrai.feather.feature.habits

import com.elkrrai.feather.domain.habit.LEAF_LOOKS
import org.junit.Assert.assertEquals
import org.junit.Test

class LeafStyleTest {

    @Test
    fun theDealCountsTheSameLooksAsThereAreStyles() {
        // the saved code of a habit's leaf is a place in this list: the two must agree
        assertEquals(LEAF_LOOKS, LeafStyle.entries.size)
    }
}
