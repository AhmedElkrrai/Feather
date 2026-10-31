package com.elkrrai.feather.domain.habit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TreeShapeTest {

    @Test
    fun a_stage_is_four_met_weeks_and_the_first_week_starts_it() {
        assertEquals(0, treeStage(0))
        assertEquals(1, treeStage(1))
        assertEquals(1, treeStage(4))
        assertEquals(2, treeStage(5))
        assertEquals(12, treeStage(48))
        assertEquals(13, treeStage(49))
        assertEquals(13, treeStage(500))
    }

    @Test
    fun the_weeks_to_the_next_stage_count_down_and_end_at_the_last() {
        assertEquals(1, weeksToNextTreeStage(0))
        assertEquals(4, weeksToNextTreeStage(1))
        assertEquals(1, weeksToNextTreeStage(4))
        assertEquals(4, weeksToNextTreeStage(5))
        assertNull(weeksToNextTreeStage(49))
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
        val top = tree.branches.maxOf { b -> (0..6).maxOf { b.y(it / 6f) } }
        assertEquals(1f, top, 0.001f)
        assertEquals(0f, tree.branches[0].y0, 0.001f)
    }

    @Test
    fun the_trunk_forks_into_six_limbs_and_there_are_plenty_of_leaves() {
        val tree = treeShape(3)
        assertEquals(6, tree.branches.count { it.depth == 1 })
        assertTrue(tree.spots.size > 800)
        assertTrue(tree.branches.filter { it.depth == 1 }.all { it.attach in 0.8f..1f })
    }

    @Test
    fun the_random_numbers_are_those_of_the_preview_script() {
        // reference values from art/tree_preview/tree_preview.py (the class Rng)
        val random = TreeRandom(1)
        assertEquals(listOf(2693262067L, 11749833L, 2265367787L, 4213581821L), List(4) { random.nextUnsigned() })
        assertEquals(listOf(1860010037L, 1397564179L), TreeRandom(-7).let { r -> List(2) { r.nextUnsigned() } })
        assertEquals(740640171, TreeRandom(5).nextBits30())
        assertEquals(2, TreeRandom(5).pick(intArrayOf(1, 2, 2)))
    }

    @Test
    fun a_seed_grows_the_tree_that_the_preview_script_draws() {
        // the number of branches and of leaf places for these seeds, from art/tree_preview/tree_preview.py
        for ((seed, counts) in mapOf(5 to (363 to 1624), 8 to (291 to 1302), 31 to (375 to 1684), 42 to (307 to 1370))) {
            val tree = treeShape(seed)
            assertEquals("branches of seed $seed", counts.first, tree.branches.size)
            assertEquals("leaf places of seed $seed", counts.second, tree.spots.size)
        }
    }

    @Test
    fun every_leaf_is_on_a_branch_that_is_not_the_trunk() {
        val tree = treeShape(5)
        assertTrue(tree.spots.all { tree.branches[it.branch].depth > 0 })
    }

    @Test
    fun stages_eight_nine_and_ten_take_the_trunk_to_its_final_form() {
        assertEquals(9f, TreeLook.trunkStage(8f), 0.001f)
        assertEquals(11f, TreeLook.trunkStage(9f), 0.001f)
        assertEquals(13f, TreeLook.trunkStage(10f), 0.001f)
        assertEquals(13f, TreeLook.trunkStage(13f), 0.001f)
    }

    @Test
    fun after_stage_ten_the_trunk_stays_and_the_crown_grows_taller() {
        val ten = TreeLook(10f)
        val thirteen = TreeLook(13f)
        assertEquals(ten.lift, thirteen.lift, 0f)
        assertEquals(ten.girth(0), thirteen.girth(0), 0f)
        assertEquals(0f, ten.stretch, 0f)
        assertTrue(thirteen.stretch > 0f)
    }

    @Test
    fun the_tree_only_grows_stage_by_stage() {
        var branches = 0f
        var leaves = 0f
        var girth = 0f
        for (tenth in 0..130) {
            val look = TreeLook(tenth / 10f)
            val b = (0..5).sumOf { look.branchGrowth(it).toDouble() }.toFloat()
            assertTrue(b >= branches)
            assertTrue(look.leafShare >= leaves)
            assertTrue(look.girth(0) >= girth)
            branches = b
            leaves = look.leafShare
            girth = look.girth(0)
        }
    }

    @Test
    fun the_seed_has_a_sprout_and_no_leaves() {
        val look = TreeLook(0f)
        assertTrue(look.branchGrowth(0) > 0f)
        assertEquals(0f, look.branchGrowth(1), 0f)
        assertEquals(0f, look.leafShare, 0f)
    }

    @Test
    fun there_are_leaves_by_stage_four() {
        assertTrue(TreeLook(4f).leafShare > 0.5f)
    }
}
