package com.elkrrai.feather.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// "The Cozy Nest": warm paper, teal and amber from the bird. The values are the contrast-checked
// ones from DESIGN_SPEC.md; NestContrastTest fails if a text pair drops below 4.5:1.

/** An opaque color from 0xRRGGBB. */
internal fun nest(rgb: Long): Color = Color(0xFF000000 or rgb)

/**
 * The brand colors, for the bird, charts and large graphics. Do not set text or buttons in
 * Plumage with cream on it: that pair is only 4.1:1. Buttons and links use `primary`.
 */
object Nest {
    val Plumage = nest(0x2A7F9E)
    val DeepTeal = nest(0x17485E)
    val SkyWing = nest(0x78C0D8)
    val Cream = nest(0xFFF3DC)
    val Amber = nest(0xF2A93B)
    val Coral = nest(0xE8735F)
    val Ink = nest(0x16262E)
}

val NestLightScheme: ColorScheme = lightColorScheme(
    primary = nest(0x1F6C88), onPrimary = nest(0xFFFFFF),
    primaryContainer = nest(0xCFE9F2), onPrimaryContainer = nest(0x0F3547),
    secondary = nest(0x8A5300), onSecondary = nest(0xFFFFFF),
    secondaryContainer = nest(0xFFE9B8), onSecondaryContainer = nest(0x8A5300),
    tertiary = nest(0xB2402A), onTertiary = nest(0xFFFFFF),
    tertiaryContainer = nest(0xFADBD2), onTertiaryContainer = nest(0x8E2F1C),
    background = nest(0xFFF8EC), onBackground = nest(0x16262E),
    surface = nest(0xFFF8EC), onSurface = nest(0x16262E),
    surfaceVariant = nest(0xF5E8CC), onSurfaceVariant = nest(0x4D5A5F),
    surfaceContainerLowest = nest(0xFFFDF8), surfaceContainerLow = nest(0xFBF1DD),
    surfaceContainer = nest(0xF5E8CC), surfaceContainerHigh = nest(0xEDDDB8),
    surfaceContainerHighest = nest(0xE6D4A8),
    outline = nest(0x857763), outlineVariant = nest(0xE4D2AB),
    error = nest(0xB2402A), onError = nest(0xFFFFFF), scrim = nest(0x000000),
)

val NestDarkScheme: ColorScheme = darkColorScheme(
    primary = nest(0x86C8DE), onPrimary = nest(0x0C2B3A),
    primaryContainer = nest(0x1D5670), onPrimaryContainer = nest(0xCFE9F2),
    secondary = nest(0xF5B955), onSecondary = nest(0x0C2B3A),
    secondaryContainer = nest(0x4A3A1C), onSecondaryContainer = nest(0xF5B955),
    tertiary = nest(0xF58F7B), onTertiary = nest(0x0C2B3A),
    tertiaryContainer = nest(0x5A2E26), onTertiaryContainer = nest(0xF9B3A3),
    background = nest(0x121E24), onBackground = nest(0xF6ECD8),
    surface = nest(0x121E24), onSurface = nest(0xF6ECD8),
    surfaceVariant = nest(0x243740), onSurfaceVariant = nest(0xB9C3C4),
    surfaceContainerLowest = nest(0x0E181D), surfaceContainerLow = nest(0x1A2A31),
    surfaceContainer = nest(0x243740), surfaceContainerHigh = nest(0x2E454F),
    surfaceContainerHighest = nest(0x38535F),
    outline = nest(0x7F8E92), outlineVariant = nest(0x34474F),
    error = nest(0xF58F7B), onError = nest(0x0C2B3A),
)

/** The dark scheme on true black for OLED screens; cards stay a step lighter so they read as layers. */
val NestBlackScheme: ColorScheme = NestDarkScheme.copy(
    background = nest(0x000000), surface = nest(0x000000),
    surfaceContainerLowest = nest(0x000000), surfaceContainerLow = nest(0x0B1114),
    surfaceContainer = nest(0x17232A), surfaceContainerHigh = nest(0x1F2E36),
    surfaceContainerHighest = nest(0x26343B),
    onSurfaceVariant = nest(0xB0BBBD), outline = nest(0x74848A), outlineVariant = nest(0x26343B),
)
