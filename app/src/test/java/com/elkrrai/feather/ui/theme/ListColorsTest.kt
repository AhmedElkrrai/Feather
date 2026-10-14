package com.elkrrai.feather.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ListColorsTest {

    @Test
    fun everyColorOfThePaletteKeepsItsPosition() {
        ListColorPalette.forEachIndexed { index, argb ->
            assertEquals(index, listColorIndex(argb))
        }
    }

    @Test
    fun aListWithoutAColorHasNoIndex() {
        assertNull(listColorIndex(null))
    }

    @Test
    fun aColorNobodyKnowsHasNoIndex() {
        assertNull(listColorIndex(0xFF123456.toInt()))
    }

    @Test
    fun colorsSavedByEarlierVersionsMapToTheClosestNestColor() {
        assertEquals(2, listColorIndex(0xFFE57373.toInt())) // red -> coral
        assertEquals(3, listColorIndex(0xFF81C784.toInt())) // green -> moss
        assertEquals(0, listColorIndex(0xFF4DB6AC.toInt())) // teal -> plumage
        assertEquals(6, listColorIndex(0xFF64B5F6.toInt())) // blue -> slate blue
    }

    @Test
    fun everyEarlierColorStillResolvesToAValidIndex() {
        val earlier = listOf(
            0xFFE57373, 0xFFFFB74D, 0xFFFFD54F, 0xFF81C784,
            0xFF4DB6AC, 0xFF64B5F6, 0xFFBA68C8, 0xFFF06292,
        )
        for (color in earlier) {
            val index = listColorIndex(color.toInt())
            assertEquals("color $color", true, index != null && index in ListColorPalette.indices)
        }
    }

    @Test
    fun theEightColorsHaveEightDifferentShapes() {
        val shapes = ListColorPalette.indices.map { listShapeFor(it) }

        assertEquals(ListColorPalette.size, shapes.toSet().size)
    }

    @Test
    fun aListWithoutAColorGetsTheCircle() {
        assertEquals(ListShape.Circle, listShapeFor(null))
    }
}
