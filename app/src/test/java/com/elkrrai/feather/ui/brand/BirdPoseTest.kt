package com.elkrrai.feather.ui.brand

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BirdPoseTest {

    private val tolerance = 0.001f

    @Test
    fun everyMoodStartsInACalmStillPose() {
        for (mood in BirdMood.entries) {
            val pose = birdPose(mood, 0f)
            assertEquals("wing of $mood", 0f, pose.wingAngle, tolerance)
            assertEquals("offset of $mood", 0f, pose.bodyOffsetY, tolerance)
            assertEquals("scale of $mood", 1f, pose.bodyScale, tolerance)
        }
    }

    @Test
    fun onlyTheFractionOfThePhaseCounts() {
        for (mood in BirdMood.entries) {
            val a = birdPose(mood, 0.25f)
            val b = birdPose(mood, 1.25f)
            val c = birdPose(mood, -0.75f)
            assertEquals(a.wingAngle, b.wingAngle, tolerance)
            assertEquals(a.bodyOffsetY, c.bodyOffsetY, tolerance)
        }
    }

    @Test
    fun theIdleBirdBlinksOnceAtTheEndOfTheCycle() {
        assertEquals(1f, birdPose(BirdMood.IDLE, 0.5f).eyeOpen, tolerance)
        assertTrue(birdPose(BirdMood.IDLE, 0.93f).eyeOpen < 0.15f)
        assertEquals(1f, birdPose(BirdMood.IDLE, 0.99f).eyeOpen, tolerance)
    }

    @Test
    fun theSleepingBirdKeepsItsEyesClosedAndBreathes() {
        val samples = (0..9).map { birdPose(BirdMood.SLEEPING, it / 10f) }

        assertTrue(samples.all { it.eyeOpen == 0f })
        assertTrue(samples.maxOf { it.bodyScale } > samples.minOf { it.bodyScale })
        assertTrue(samples.all { it.bodyScale in 0.97f..1.03f })
    }

    @Test
    fun theHappyBirdHopsUpAndComesBackDown() {
        assertEquals(0f, birdPose(BirdMood.HAPPY, 0f).bodyOffsetY, tolerance)
        assertEquals(-14f, birdPose(BirdMood.HAPPY, 0.5f).bodyOffsetY, tolerance)
        assertEquals(0f, birdPose(BirdMood.HAPPY, 1f).bodyOffsetY, tolerance)
        // It never sinks below where it started.
        assertTrue((0..20).all { birdPose(BirdMood.HAPPY, it / 20f).bodyOffsetY <= tolerance })
    }

    @Test
    fun theWingsStayWithinAReasonableSwing() {
        for (mood in BirdMood.entries) {
            for (step in 0..40) {
                val angle = birdPose(mood, step / 40f).wingAngle
                assertTrue("wing of $mood at $step", angle in -40f..40f)
            }
        }
    }

    @Test
    fun theFlyingBirdFlapsBothWays() {
        val angles = (0..20).map { birdPose(BirdMood.FLYING, it / 20f).wingAngle }

        assertTrue(angles.any { it > 10f })
        assertTrue(angles.any { it < -10f })
    }

    @Test
    fun eyeOpeningIsAlwaysAFraction() {
        for (mood in BirdMood.entries) {
            for (step in 0..100) {
                assertTrue(birdPose(mood, step / 100f).eyeOpen in 0f..1f)
            }
        }
    }

    @Test
    fun theCompletionHopFadesInAndOut() {
        assertEquals(0f, fade(0f), tolerance)
        assertEquals(0.5f, fade(0.05f), tolerance)
        assertEquals(1f, fade(0.5f), tolerance)
        assertEquals(0.4f, fade(0.9f), tolerance)
        assertEquals(0f, fade(1f), tolerance)
    }

    // --- ambient movement stops after ten seconds ---

    @Test
    fun ambientMovementStartsAnotherCycleOnlyInsideTheFirstTenSeconds() {
        assertTrue(ambientShouldContinue(startMillis = 1_000L, nowMillis = 1_000L))
        assertTrue(ambientShouldContinue(startMillis = 1_000L, nowMillis = 10_999L))
        assertEquals(false, ambientShouldContinue(startMillis = 1_000L, nowMillis = 11_000L))
        assertEquals(false, ambientShouldContinue(startMillis = 1_000L, nowMillis = 60_000L))
    }

    @Test
    fun theLimitIsTenSeconds() {
        assertEquals(10_000L, AMBIENT_MAX_MILLIS)
    }
}
