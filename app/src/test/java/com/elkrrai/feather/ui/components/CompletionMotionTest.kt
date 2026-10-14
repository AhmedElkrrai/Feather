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

    // --- the feather confetti ---

    @Test
    fun theConfettiHasTheRequestedNumberOfFeathersAllDifferent() {
        val pieces = confettiPieces(28)

        assertEquals(28, pieces.size)
        assertEquals(28, pieces.map { it.angleDegrees }.toSet().size)
    }

    @Test
    fun feathersAreThrownUpInAFan() {
        for (piece in confettiPieces()) {
            // Between about 165 and 15 degrees above the horizontal, never downward.
            assertTrue(piece.angleDegrees in -166f..-14f)
            assertTrue(piece.speedDp in 300f..580f)
            assertTrue(piece.sizeDp in 13f..21f)
        }
    }

    @Test
    fun feathersTakeTheColorsInTurn() {
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 0), confettiPieces(7, colors = 6).map { it.colorIndex })
    }

    @Test
    fun aFeatherStartsWhereItWasThrown() {
        val (x, y) = confettiOffset(confettiPieces().first(), 0f)

        assertEquals(0f, x, tolerance)
        assertEquals(0f, y, tolerance)
    }

    @Test
    fun aFeatherRisesFirstAndSinksLater() {
        // Thrown mostly upward: early on it is above where it started, and by the end it has fallen below it.
        val piece = confettiPieces().first { it.angleDegrees < -80f && it.angleDegrees > -100f }

        assertTrue(confettiOffset(piece, 0.3f).second < 0f)
        assertTrue(confettiOffset(piece, CONFETTI_SECONDS).second > confettiOffset(piece, 0.3f).second)
    }

    @Test
    fun aFeatherSlowsDownInTheAir() {
        val piece = confettiPieces().first()
        val early = confettiOffset(piece, 0.1f).first - confettiOffset(piece, 0f).first
        val later = confettiOffset(piece, 1.1f).first - confettiOffset(piece, 1.0f).first

        assertTrue(Math.abs(later) < Math.abs(early) + 5f)
    }

    @Test
    fun aFeatherIsFullStrengthThenFadesAndIsGoneAtTheEndOfItsLife() {
        val piece = ConfettiPiece(-90f, 400f, 200f, 8f, 1.5f, 16f, 0, life = 1f)

        assertEquals(1f, confettiAlpha(piece, 0f), tolerance)
        assertEquals(1f, confettiAlpha(piece, 0.7f), tolerance)
        assertEquals(0.5f, confettiAlpha(piece, 0.85f), tolerance)
        assertEquals(0f, confettiAlpha(piece, 1f), tolerance)
    }

    @Test
    fun aShorterLivedFeatherFadesSooner() {
        val short = ConfettiPiece(-90f, 400f, 200f, 8f, 1.5f, 16f, 0, life = 0.75f)

        assertEquals(0f, confettiAlpha(short, 0.8f), tolerance)
        assertEquals(1f, confettiAlpha(short, 0.5f), tolerance)
    }

    @Test
    fun noFeatherLivesPastTheEndOfTheBurst() {
        for (piece in confettiPieces()) {
            assertEquals(0f, confettiAlpha(piece, 1f), tolerance)
        }
    }
}
