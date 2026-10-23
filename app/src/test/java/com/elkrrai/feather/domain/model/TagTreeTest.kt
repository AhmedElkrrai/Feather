package com.elkrrai.feather.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TagTreeTest {
    // life > work > bugfix, and a separate top-level "home"
    private val life = Tag("life", "life")
    private val work = Tag("work", "work", parentId = "life")
    private val bugfix = Tag("bugfix", "bugfix", parentId = "work")
    private val home = Tag("home", "home")
    private val all = listOf(bugfix, home, work, life)

    @Test
    fun aBranchHoldsTheTagAndEverythingUnderItAtAnyDepth() {
        assertEquals(setOf("life", "work", "bugfix"), all.branchIds("life"))
        assertEquals(setOf("work", "bugfix"), all.branchIds("work"))
        assertEquals(setOf("bugfix"), all.branchIds("bugfix"))
    }

    @Test
    fun aTagCannotExtendItselfOrAnythingBelowIt() {
        assertTrue(all.wouldLoop("life", "life"))
        assertTrue(all.wouldLoop("life", "bugfix")) // the example from the brief: life must not extend bugfix
        assertTrue(all.wouldLoop("life", "work"))
        assertTrue(all.wouldLoop("work", "bugfix"))
    }

    @Test
    fun movesThatDoNotLoopAreAllowed() {
        assertFalse(all.wouldLoop("bugfix", "life")) // a shortcut up the chain is fine
        assertFalse(all.wouldLoop("home", "work"))
        assertFalse(all.wouldLoop("work", null)) // back to the top level
        assertFalse(all.wouldLoop("bugfix", "home"))
    }

    @Test
    fun theTagsOfferedAsAParentLeaveOutTheTagAndItsBranch() {
        assertEquals(listOf("home"), all.possibleParentsOf("life").map { it.id })
        assertEquals(listOf("home", "life"), all.possibleParentsOf("work").map { it.id })
        assertEquals(listOf("home", "life", "work"), all.possibleParentsOf("bugfix").map { it.id })
    }

    @Test
    fun theTreeIsListedParentFirstWithItsDepth() {
        val rows = all.inTreeOrder()

        assertEquals(listOf("home", "life", "work", "bugfix"), rows.map { it.tag.id })
        assertEquals(listOf(0, 0, 1, 2), rows.map { it.depth })
    }

    @Test
    fun aTagWhoseParentIsGoneIsShownAtTheTopLevel() {
        val orphan = Tag("orphan", "orphan", parentId = "deleted")

        val rows = listOf(orphan, home).inTreeOrder()

        assertEquals(listOf("home", "orphan"), rows.map { it.tag.id })
        assertEquals(listOf(0, 0), rows.map { it.depth })
    }

    @Test
    fun badDataWithALoopStillTerminates() {
        val a = Tag("a", "a", parentId = "b")
        val b = Tag("b", "b", parentId = "a")

        // neither has a top-level ancestor, so nothing is listed, but nothing hangs either
        assertEquals(setOf("a", "b"), listOf(a, b).branchIds("a"))
        assertEquals(emptyList<TagRow>(), listOf(a, b).inTreeOrder())
    }
}
