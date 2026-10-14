package com.elkrrai.feather.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Text must reach 4.5:1 and graphics 3:1 in all three themes (DESIGN_SPEC.md, "Contrast"). */
class NestContrastTest {

    private fun channel(value: Float): Double {
        val v = value.toDouble()
        return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(color: Color): Double =
        0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)

    private fun ratio(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    private fun assertText(label: String, foreground: Color, background: Color) {
        val value = ratio(foreground, background)
        assertTrue("$label is $value:1, below 4.5:1", value >= 4.5)
    }

    private fun assertGraphic(label: String, foreground: Color, background: Color) {
        val value = ratio(foreground, background)
        assertTrue("$label is $value:1, below 3:1", value >= 3.0)
    }

    private fun checkScheme(name: String, scheme: ColorScheme, colors: FeatherColors) {
        assertText("$name text on surface", scheme.onSurface, scheme.surface)
        assertText("$name text on card", scheme.onSurface, colors.card)
        assertText("$name secondary text on surface", scheme.onSurfaceVariant, scheme.surface)
        assertText("$name secondary text on cards", scheme.onSurfaceVariant, scheme.surfaceContainer)
        assertText("$name primary on surface", scheme.primary, scheme.surface)
        assertText("$name primary on card", scheme.primary, colors.card)
        assertText("$name label on primary", scheme.onPrimary, scheme.primary)
        assertText("$name secondary on surface", scheme.secondary, scheme.surface)
        assertText("$name tertiary on surface", scheme.tertiary, scheme.surface)
        assertText("$name label on primary container", scheme.onPrimaryContainer, scheme.primaryContainer)
        assertText("$name label on secondary container", scheme.onSecondaryContainer, scheme.secondaryContainer)
        assertText("$name label on tertiary container", scheme.onTertiaryContainer, scheme.tertiaryContainer)
        assertGraphic("$name outline on surface", scheme.outline, scheme.surface)

        assertText("$name low priority chip", colors.prioLowOn, colors.prioLowTint)
        assertText("$name medium priority chip", colors.prioMedOn, colors.prioMedTint)
        assertText("$name high priority chip", colors.prioHighOn, colors.prioHighTint)
        assertGraphic("$name low priority ring", colors.prioLow, colors.card)
        assertGraphic("$name medium priority ring", colors.prioMed, colors.card)
        assertGraphic("$name high priority ring", colors.prioHigh, colors.card)
        assertGraphic("$name no-priority ring", colors.prioNone, colors.card)

        colors.heat.forEachIndexed { level, fill ->
            assertText("$name heat level $level", colors.heatText[level], fill)
        }
        colors.listColors.forEachIndexed { index, color ->
            assertGraphic("$name list color $index", color, colors.card)
        }
    }

    @Test
    fun lightThemeIsReadable() = checkScheme("light", NestLightScheme, LightFeatherColors)

    @Test
    fun darkThemeIsReadable() = checkScheme("dark", NestDarkScheme, DarkFeatherColors)

    @Test
    fun blackThemeIsReadable() = checkScheme("black", NestBlackScheme, BlackFeatherColors)

    @Test
    fun theBlackThemeReallyIsBlack() {
        assertEquals(Color.Black.value, NestBlackScheme.background.value)
        assertEquals(Color.Black.value, NestBlackScheme.surface.value)
    }

    @Test
    fun everySkyPhaseHasAWashAndEveryHeatMapHasFourLevels() {
        for (colors in listOf(LightFeatherColors, DarkFeatherColors, BlackFeatherColors)) {
            assertEquals(DayPhase.entries.toSet(), colors.sky.keys)
            assertEquals(4, colors.heat.size)
            assertEquals(4, colors.heatText.size)
            assertEquals(8, colors.listColors.size)
        }
    }
}
