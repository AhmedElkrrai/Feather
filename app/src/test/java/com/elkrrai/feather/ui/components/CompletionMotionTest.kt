package com.elkrrai.feather.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompletionMotionTest {

    private val tolerance = 0.0001f

    // --- the strike-through ---

    @Test
    fun aSingleLineIsStruckInProportionToTheProgress() {
        assertEquals(listOf(0f), strikeFractions(listOf(120f), 0f))
        assertEquals(listOf(0.5f), strikeFractions(listOf(120f), 0.5f))
        assertEquals(listOf(1f), strikeFractions(listOf(120f), 1f))
    }

    @Test
    fun linesAreStruckOneAfterTheOther() {
        val half = strikeFractions(listOf(100f, 100f), 0.5f)
        assertEquals(1f, half[0], tolerance)
        assertEquals(0f, half[1], tolerance)

        val threeQuarters = strikeFractions(listOf(100f, 100f), 0.75f)
        assertEquals(1f, threeQuarters[0], tolerance)
        assertEquals(0.5f, threeQuarters[1], tolerance)
    }

    @Test
    fun aLongerLineTakesLongerToStrike() {
        val fractions = strikeFractions(listOf(300f, 100f), 0.5f)

        assertEquals(200f / 300f, fractions[0], tolerance)
        assertEquals(0f, fractions[1], tolerance)
    }

    @Test
    fun progressOutsideZeroToOneIsClamped() {
        assertEquals(listOf(0f), strikeFractions(listOf(50f), -1f))
        assertEquals(listOf(1f), strikeFractions(listOf(50f), 3f))
    }

    @Test
    fun emptyOrZeroWidthTextIsNeverStruck() {
        assertEquals(emptyList<Float>(), strikeFractions(emptyList(), 1f))
        assertEquals(listOf(0f, 0f), strikeFractions(listOf(0f, 0f), 1f))
    }

    // --- the feather burst ---

    @Test
    fun theBurstHasTheRequestedNumberOfFeathersAndTheFirstFliesUp() {
        val particles = burstParticles(7)

        assertEquals(7, particles.size)
        assertEquals(-90f, particles.first().angleDegrees, tolerance)
    }

    @Test
    fun feathersTakeTheBurstColorsInTurn() {
        val particles = burstParticles(7, colors = 4)

        assertEquals(listOf(0, 1, 2, 3, 0, 1, 2), particles.map { it.colorIndex })
    }

    @Test
    fun feathersFlyBetween28And52DpAndSpinNoMoreThan40Degrees() {
        for (particle in burstParticles(7)) {
            assertTrue(particle.distanceDp in 28f..52f)
            assertTrue(particle.spinDegrees in -40f..40f)
            assertTrue(particle.sizeDp > 0f)
        }
    }

    @Test
    fun theDirectionsAreAllDifferent() {
        val angles = burstParticles(7).map { it.angleDegrees }

        assertEquals(angles.size, angles.toSet().size)
    }

    @Test
    fun aSmallerBurstForSlowPhonesStillWorks() {
        assertEquals(3, burstParticles(3).size)
    }

    @Test
    fun theBurstIsFullStrengthThenFadesOverTheLast40Percent() {
        assertEquals(1f, burstAlpha(0f), tolerance)
        assertEquals(1f, burstAlpha(0.6f), tolerance)
        assertEquals(0.5f, burstAlpha(0.8f), tolerance)
        assertEquals(0f, burstAlpha(1f), tolerance)
        assertEquals(1f, burstAlpha(-1f), tolerance)
        assertEquals(0f, burstAlpha(2f), tolerance)
    }
}
