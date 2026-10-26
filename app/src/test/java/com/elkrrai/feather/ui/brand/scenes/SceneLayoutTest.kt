package com.elkrrai.feather.ui.brand.scenes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneLayoutTest {

    private val tolerance = 0.0001f

    // --- how big a scene is shown ---

    @Test
    fun aScenesIs190DpNormally() {
        assertEquals(190f, sceneSizeDp(freeHeightDp = 600f, fontScale = 1f)!!, tolerance)
        assertEquals(190f, sceneSizeDp(freeHeightDp = 320f, fontScale = 1.3f)!!, tolerance)
    }

    @Test
    fun aScenesIs140DpAtTwiceTheFontSize() {
        assertEquals(140f, sceneSizeDp(freeHeightDp = 600f, fontScale = 2f)!!, tolerance)
        assertEquals(140f, sceneSizeDp(freeHeightDp = 600f, fontScale = 2.4f)!!, tolerance)
    }

    @Test
    fun aScenesIsLeftOutWhenLessThan320DpIsFree() {
        assertNull(sceneSizeDp(freeHeightDp = 319f, fontScale = 1f))
        assertNull(sceneSizeDp(freeHeightDp = 200f, fontScale = 2f))
    }

    // --- the movement ---

    @Test
    fun theSwayStartsStillAndNeverLeansMoreThanThreeDegrees() {
        assertEquals(0f, swayDegrees(0f), tolerance)
        for (step in 0..100) {
            assertTrue(swayDegrees(step / 100f) in -3f..3f)
        }
        assertEquals(3f, swayDegrees(0.25f), tolerance)
        assertEquals(-3f, swayDegrees(0.75f), tolerance)
    }

    @Test
    fun theSwayRepeatsEveryWholePhase() {
        assertEquals(swayDegrees(0.3f), swayDegrees(1.3f), tolerance)
    }

    @Test
    fun theSunClimbs24BoardUnitsAndNoMore() {
        assertEquals(0f, sunRise(0f), tolerance)
        assertEquals(12f, sunRise(0.5f), tolerance)
        assertEquals(24f, sunRise(1f), tolerance)
        assertEquals(0f, sunRise(-1f), tolerance)
        assertEquals(24f, sunRise(3f), tolerance)
    }

    // --- where the bird stands ---

    @Test
    fun theBirdsBoxIs96UnitsScaled() {
        assertEquals(96f, BirdPlacement(0f, 0f, 1f).side, tolerance)
        assertEquals(48f, BirdPlacement(0f, 0f, 0.5f).side, tolerance)
    }

    @Test
    fun aPlacementPastTheBoardIsNotOnIt() {
        assertTrue(BirdPlacement(10f, 10f, 1f).isOnBoard)
        assertTrue(!BirdPlacement(110f, 10f, 1f).isOnBoard)
        assertTrue(!BirdPlacement(10f, 110f, 1f).isOnBoard)
        assertTrue(!BirdPlacement(-1f, 10f, 1f).isOnBoard)
    }

    @Test
    fun everySceneKeepsItsBirdOnTheBoard() {
        val placements = mapOf(
            "empty list" to EmptyListBird,
            "nothing due" to NothingDueBird,
            "no results" to NoResultsBird,
            "reminders blocked" to RemindersBlockedBird,
            "first launch" to FirstLaunchBird,
            "trash empty" to TrashEmptyBird,
        )
        for ((scene, placement) in placements) {
            assertTrue("the bird of $scene is off the board", placement.isOnBoard)
        }
    }

    @Test
    fun thereAreNineScenes() {
        assertEquals(9, SceneKind.entries.size)
    }
}
