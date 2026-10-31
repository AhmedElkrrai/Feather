package com.elkrrai.feather.domain.habit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TreeShapeTest {

    @Test
    fun a_tree_has_room_for_as_many_leaves_as_the_limit() {
        assertEquals(TREE_MAX_LEAVES, treeShape(1).spots.size)
    }

    @Test
    fun the_same_seed_grows_the_same_tree() {
        val a = treeShape(42)
        val b = treeShape(42)
        assertEquals(a.branches.map { it.x1 }, b.branches.map { it.x1 })
        assertEquals(a.spots.map { it.branch }, b.spots.map { it.branch })
    }

    @Test
    fun the_tree_is_one_tall_and_stands_at_the_origin() {
        val tree = treeShape(7)
        assertEquals(1f, tree.branches.maxOf { maxOf(it.y0, it.y1) }, 0.001f)
        assertEquals(0f, tree.branches[0].y0, 0.001f)
    }

    @Test
    fun leaves_come_out_on_the_lower_forks_first() {
        val tree = treeShape(3)
        val depths = tree.spots.map { tree.branches[it.branch].depth }
        assertEquals(depths.sorted(), depths)
    }

    @Test
    fun a_leaf_sits_on_a_branch_that_has_finished_growing() {
        val tree = treeShape(5)
        tree.spots.forEachIndexed { index, spot ->
            val depth = tree.branches[spot.branch].depth
            // the leaf has just started to come out; its branch is whole (the trunk grows with its first leaves)
            val weeks = index + 0.01f
            if (depth > 0) assertEquals(1f, treeBranchGrowth(depth, weeks), 0.0001f)
        }
    }

    @Test
    fun the_tree_only_grows_with_met_weeks() {
        var before = 0f
        for (weeks in 0..80) {
            val total = (0..TREE_DEPTH).sumOf { treeBranchGrowth(it, weeks.toFloat()).toDouble() }.toFloat()
            assertTrue(total >= before)
            before = total
        }
    }

    @Test
    fun the_trunk_is_a_sprout_before_the_first_week() {
        assertTrue(treeBranchGrowth(0, 0f) > 0f)
        assertEquals(0f, treeBranchGrowth(1, 0f), 0f)
    }
}
