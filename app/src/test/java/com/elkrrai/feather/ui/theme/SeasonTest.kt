package com.elkrrai.feather.ui.theme

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SeasonTest {
    @Test
    fun octoberAndNovemberAreAutumn() {
        assertEquals(Season.Autumn, seasonFor(LocalDate.of(2026, 10, 1)))
        assertEquals(Season.Autumn, seasonFor(LocalDate.of(2026, 11, 30)))
    }

    @Test
    fun marchToMayIsSpring() {
        assertEquals(Season.Spring, seasonFor(LocalDate.of(2026, 3, 1)))
        assertEquals(Season.Spring, seasonFor(LocalDate.of(2026, 5, 31)))
    }

    @Test
    fun theRestOfTheYearIsPlain() {
        for (month in listOf(1, 2, 6, 7, 8, 9, 12)) {
            assertEquals(Season.Other, seasonFor(LocalDate.of(2026, month, 15)))
        }
    }

    @Test
    fun eachSeasonHasItsOwnLeaf() {
        val colors = LightFeatherColors
        val leaves = Season.entries.map { colors.leafColor(it) }

        assertEquals(leaves.size, leaves.toSet().size)
        assertEquals(colors.listColors[3], colors.leafColor(Season.Other))
        assertNotEquals(colors.listColors[3], colors.leafColor(Season.Autumn))
    }
}
