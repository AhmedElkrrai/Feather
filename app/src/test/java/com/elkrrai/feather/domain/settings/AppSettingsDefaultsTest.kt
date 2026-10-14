package com.elkrrai.feather.domain.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsDefaultsTest {

    @Test
    fun theAppStartsInItsOwnColorsWithTheBirdMoving() {
        val defaults = AppSettings()

        // The brand palette is the default; Material You is one switch away.
        assertFalse(defaults.dynamicColor)
        assertTrue(defaults.birdAnimations)
        assertEquals(ThemeMode.SYSTEM, defaults.themeMode)
        assertFalse(defaults.pureBlack)
        assertTrue(defaults.haptics)
    }

    @Test
    fun celebrationsAreOnAndMotionIsNotReducedUntilAskedFor() {
        val defaults = AppSettings()

        assertTrue(defaults.celebrations)
        assertFalse(defaults.reduceMotion)
    }
}
