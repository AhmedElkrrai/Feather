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
        // dark mode is true black from the start, and the app lock is off until it is turned on
        assertTrue(defaults.pureBlack)
        assertFalse(defaults.appLock)
        assertTrue(defaults.haptics)
    }

    @Test
    fun celebrationsAreOnAndMotionIsNotReducedUntilAskedFor() {
        val defaults = AppSettings()

        assertTrue(defaults.celebrations)
        assertFalse(defaults.reduceMotion)
    }
}
