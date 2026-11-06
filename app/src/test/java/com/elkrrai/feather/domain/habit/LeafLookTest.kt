package com.elkrrai.feather.domain.habit

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LeafLookTest {

    @Test
    fun everyLookIsDealtOnceBeforeAnyIsDealtTwice() {
        val random = Random(7)
        val dealt = mutableListOf<Int>()
        repeat(LEAF_LOOKS) { dealt += nextLeaf(dealt, random) }

        assertEquals((0 until LEAF_LOOKS).toSet(), dealt.toSet())
    }

    @Test
    fun theDealStartsOverAfterEveryLookWasUsed() {
        val random = Random(3)
        val dealt = mutableListOf<Int>()
        repeat(LEAF_LOOKS * 2) { dealt += nextLeaf(dealt, random) }

        // every look twice: no look is ever two ahead of another
        assertEquals(List(LEAF_LOOKS) { 2 }, (0 until LEAF_LOOKS).map { look -> dealt.count { it == look } })
    }

    @Test
    fun theLookThatIsLeastUsedIsTheOneDealtNext() {
        // every look but the last is used: the last is the only one used zero times
        assertEquals(LEAF_LOOKS - 1, nextLeaf((0 until LEAF_LOOKS - 1).toList() + listOf(0)))
    }

    @Test
    fun aHabitWithoutALookYetIsNotCounted() {
        assertTrue(nextLeaf(listOf(-1, -1, -1), Random(1)) in 0 until LEAF_LOOKS)
        assertEquals(LEAF_LOOKS - 1, nextLeaf((0 until LEAF_LOOKS - 1).toList() + listOf(-1, -1)))
    }
}
