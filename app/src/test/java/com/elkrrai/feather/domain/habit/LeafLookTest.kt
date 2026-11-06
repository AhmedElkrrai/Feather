package com.elkrrai.feather.domain.habit

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LeafLookTest {

    @Test
    fun threeHabitsGetThreeDifferentLooks() {
        val random = Random(7)
        val dealt = mutableListOf<Int>()
        repeat(3) { dealt += nextLeaf(dealt, random) }

        assertEquals(setOf(0, 1, 2), dealt.toSet())
    }

    @Test
    fun theDealStartsOverAfterEveryLookWasUsed() {
        val random = Random(3)
        val dealt = mutableListOf<Int>()
        repeat(6) { dealt += nextLeaf(dealt, random) }

        // every look twice: no look is ever two ahead of another
        assertEquals(listOf(2, 2, 2), (0 until LEAF_LOOKS).map { look -> dealt.count { it == look } })
    }

    @Test
    fun theLookThatIsLeastUsedIsTheOneDealtNext() {
        // two of look 0, one of look 1: look 2 is the only one used zero times
        assertEquals(2, nextLeaf(listOf(0, 0, 1)))
    }

    @Test
    fun aHabitWithoutALookYetIsNotCounted() {
        assertTrue(nextLeaf(listOf(-1, -1, -1), Random(1)) in 0 until LEAF_LOOKS)
        assertEquals(1, nextLeaf(listOf(0, 2, -1, -1)))
    }
}
