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

    // --- the idle bird's head tilt and tail flick ---

    @Test
    fun theIdleBirdTiltsItsHeadUpAndBackDownOnceACycle() {
        val tilts = (0..100).map { birdPose(BirdMood.IDLE, it / 100f).headTilt }

        assertEquals(0f, tilts.first(), tolerance)
        assertEquals(0f, tilts.last(), tolerance)
        assertTrue("beak lifts", tilts.min() < -8f)
        assertTrue("never dips below level", tilts.max() <= tolerance)
        assertEquals(0f, birdPose(BirdMood.IDLE, 0.5f).headTilt, tolerance)
    }

    @Test
    fun theIdleBirdFlicksItsTailBothWaysThenSettles() {
        val flicks = (0..200).map { birdPose(BirdMood.IDLE, it / 200f).tailFlick }

        assertTrue(flicks.any { it > 3f })
        assertTrue(flicks.any { it < -3f })
        assertTrue(flicks.all { it in -12f..12f })
        assertEquals(0f, birdPose(BirdMood.IDLE, 0.9f).tailFlick, tolerance)
        assertEquals(0f, birdPose(BirdMood.IDLE, 0.25f).tailFlick, tolerance)
    }

    // --- the flight: a lap over the perch, then landing ---

    @Test
    fun theFlightStartsAndEndsInTheCalmPose() {
        for (progress in listOf(0f, 1f)) {
            val pose = birdFlightPose(progress)
            assertEquals(0f, pose.bodyOffsetX, tolerance)
            assertEquals(0f, pose.bodyOffsetY, tolerance)
            assertEquals(0f, pose.wingAngle, tolerance)
            assertEquals(0f, pose.tilt, tolerance)
            assertEquals(1f, pose.bodyScale, tolerance)
            assertEquals(1f, pose.facing, tolerance)
            assertEquals(1f, pose.eyeOpen, tolerance)
        }
    }

    @Test
    fun theFlightGoesRoundInACircleAndComesBackToThePerch() {
        // halfway round the lap: straight above the perch, two radii up, facing the other way
        val top = birdFlightPose(0.45f)
        assertEquals(0f, top.bodyOffsetX, 0.01f)
        assertEquals(-2f * FLIGHT_RADIUS, top.bodyOffsetY, 0.01f)
        assertEquals(-1f, top.facing, tolerance)

        // the end of the lap is back on the perch
        val landed = birdFlightPose(0.80f)
        assertEquals(0f, landed.bodyOffsetX, 0.01f)
        assertEquals(0f, landed.bodyOffsetY, 0.01f)
    }

    @Test
    fun theFlightStaysInsideItsCircleAndNeverSinksBelowThePerch() {
        for (step in 0..400) {
            val pose = birdFlightPose(step / 400f)
            assertTrue("x at $step", pose.bodyOffsetX in -FLIGHT_RADIUS - tolerance..FLIGHT_RADIUS + tolerance)
            assertTrue("y at $step", pose.bodyOffsetY in -2f * FLIGHT_RADIUS - tolerance..3f)
            assertTrue("facing at $step", pose.facing in -1f..1f)
            assertTrue("wing at $step", pose.wingAngle in -40f..40f)
            assertTrue("eye at $step", pose.eyeOpen in 0f..1f)
        }
    }

    @Test
    fun theBirdFliesNoseFirstAndTurnsRoundOverTheTop() {
        val rising = birdFlightPose(0.25f) // up the right-hand side
        val atTheTop = birdFlightPose(0.45f)
        val descending = birdFlightPose(0.51f) // down the left-hand side

        assertTrue(rising.facing > 0.9f && rising.bodyOffsetX > 0f)
        assertTrue(atTheTop.facing < -0.9f)
        assertTrue(descending.facing < -0.9f && descending.bodyOffsetX < 0f)
    }

    @Test
    fun theFlightLastsTwoPointTwoSeconds() {
        assertEquals(2200, FLIGHT_MILLIS)
    }

    @Test
    fun onlyTheIdleBirdTiltsAndFlicks() {
        for (mood in BirdMood.entries.filter { it != BirdMood.IDLE }) {
            for (step in 0..20) {
                val pose = birdPose(mood, step / 20f)
                assertEquals(0f, pose.headTilt, tolerance)
                assertEquals(0f, pose.tailFlick, tolerance)
            }
        }
    }

    @Test
    fun theIdleBirdBlinksTwiceACycleAndNeverWhileTiltingOrFlicking() {
        val closed = (0..200).filter { birdPose(BirdMood.IDLE, it / 200f).eyeOpen < 0.15f }.map { it / 200f }

        assertTrue(closed.any { it < 0.5f })
        assertTrue(closed.any { it > 0.5f })
        assertTrue(closed.none { it in 0.14f..0.38f || it in 0.60f..0.74f })
    }
}
