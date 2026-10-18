package com.elkrrai.feather.feature.tasklist

import androidx.compose.ui.geometry.Offset
import com.elkrrai.feather.ui.components.confettiPieces
import com.elkrrai.feather.ui.components.quadraticPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LastTaskMotionTest {
    private val tolerance = 0.001f

    @Test
    fun theBirdHopsTwiceThenRests() {
        assertEquals(0f, doubleHop(0f), tolerance)
        assertEquals(14f, doubleHop(0.15f), tolerance) // top of the first hop
        assertEquals(0f, doubleHop(0.3f), tolerance) // landed
        assertEquals(14f, doubleHop(0.45f), tolerance) // top of the second hop
        assertEquals(0f, doubleHop(0.6f), tolerance)
        assertEquals(0f, doubleHop(0.9f), tolerance)
    }

    @Test
    fun theFlightStartsAndEndsWhereItShould() {
        val from = Offset(10f, 400f)
        val to = Offset(200f, 50f)
        val control = Offset(105f, 100f)

        assertEquals(from, quadraticPoint(from, control, to, 0f))
        assertEquals(to, quadraticPoint(from, control, to, 1f))
        // It bows up toward the control point: the middle is higher on the screen (smaller y) than the straight line.
        assertTrue(quadraticPoint(from, control, to, 0.5f).y < (from.y + to.y) / 2f)
    }

    @Test
    fun theWarmBurstHasTwelveFeathers() {
        assertEquals(12, confettiPieces(count = 12, seed = 5).size)
    }
}
