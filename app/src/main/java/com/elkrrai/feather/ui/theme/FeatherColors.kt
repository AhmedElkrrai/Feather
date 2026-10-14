package com.elkrrai.feather.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** The part of the day the Today header shows in its sky: from 05:00, 10:00, 17:00 and 20:00. */
enum class DayPhase { Dawn, Day, Dusk, Night }

/**
 * Colors that Material's scheme has no slot for: the card surface, priority tints, list colors,
 * the heatmap "straw" ramp and the sky washes. Read them with `MaterialTheme.featherColors`.
 */
@Immutable
data class FeatherColors(
    val card: Color,
    val amberFill: Color,
    val coralFill: Color,
    val twig: Color,
    val prioNone: Color,
    val prioLow: Color,
    val prioMed: Color,
    val prioHigh: Color,
    /** Text on the tinted priority chips. */
    val prioLowOn: Color,
    val prioMedOn: Color,
    val prioHighOn: Color,
    val prioLowTint: Color,
    val prioMedTint: Color,
    val prioHighTint: Color,
    /** Eight list colors; each is paired with a shape so color is never the only signal. */
    val listColors: List<Color>,
    /** Heatmap levels, empty to busiest; text on them is [heatText]. */
    val heat: List<Color>,
    val heatText: List<Color>,
    /** Top and bottom of the flat wash behind the Today header. */
    val sky: Map<DayPhase, Pair<Color, Color>>,
    val skyText: Color,
)

private fun list(vararg rgb: Long): List<Color> = rgb.map(::nest)

val LightFeatherColors = FeatherColors(
    card = nest(0xFFFDF8), amberFill = nest(0xF2A93B), coralFill = nest(0xE8735F), twig = nest(0x8A6A43),
    prioNone = nest(0x857763), prioLow = nest(0x2C7A93), prioMed = nest(0xB5700A), prioHigh = nest(0xB2402A),
    prioLowOn = nest(0x1F6C88), prioMedOn = nest(0x9A5B00), prioHighOn = nest(0x8E2F1C),
    prioLowTint = nest(0xDDEFF5), prioMedTint = nest(0xFFE9B8), prioHighTint = nest(0xFADBD2),
    listColors = list(0x2A7F9E, 0xB5700A, 0xD9604A, 0x5E7F3E, 0x9A4F73, 0xA6613B, 0x5A6EA3, 0x17485E),
    heat = list(0xF3E6C9, 0xF8D98F, 0xF2A93B, 0xC77D12),
    heatText = List(4) { nest(0x16262E) },
    sky = mapOf(
        DayPhase.Dawn to (nest(0xFFE3C4) to nest(0xFBD0C2)),
        DayPhase.Day to (nest(0xCFE9F2) to nest(0xFFF3DC)),
        DayPhase.Dusk to (nest(0xF9C9A0) to nest(0xEBA9A0)),
        DayPhase.Night to (nest(0x1E3A4C) to nest(0x17485E)),
    ),
    // The night wash is dark in the light theme too, so its text is cream (see nightSkyText).
    skyText = nest(0x16262E),
)

val DarkFeatherColors = FeatherColors(
    card = nest(0x1A2A31), amberFill = nest(0xF5B955), coralFill = nest(0xF58F7B), twig = nest(0xB08A5A),
    prioNone = nest(0x7F8E92), prioLow = nest(0x6FB6CE), prioMed = nest(0xF5B955), prioHigh = nest(0xF58F7B),
    prioLowOn = nest(0xCFE9F2), prioMedOn = nest(0xF5B955), prioHighOn = nest(0xF9B3A3),
    prioLowTint = nest(0x1D5670), prioMedTint = nest(0x4A3A1C), prioHighTint = nest(0x5A2E26),
    listColors = list(0x6FB6CE, 0xF5B955, 0xF58F7B, 0x9DBF7A, 0xD08AAE, 0xD79A72, 0x9AAEE0, 0x86C8DE),
    heat = list(0x243740, 0x5F4B25, 0xB8862F, 0xF5B955),
    heatText = listOf(nest(0xF6ECD8), nest(0xF6ECD8), nest(0x16262E), nest(0x16262E)),
    sky = mapOf(
        DayPhase.Dawn to (nest(0x3B3140) to nest(0x2A2A36)),
        DayPhase.Day to (nest(0x1D4658) to nest(0x1A2A31)),
        DayPhase.Dusk to (nest(0x4A3340) to nest(0x2D2A36)),
        DayPhase.Night to (nest(0x0F1C25) to nest(0x121E24)),
    ),
    skyText = nest(0xF6ECD8),
)

/** Black is the dark set with a darker card, hairline and first heat level, and a night sky that fades to black. */
val BlackFeatherColors = DarkFeatherColors.copy(
    card = nest(0x0B1114),
    heat = listOf(nest(0x17232A)) + DarkFeatherColors.heat.drop(1),
    sky = DarkFeatherColors.sky + (DayPhase.Night to (nest(0x0F1C25) to nest(0x000000))),
)

/** Text on the night sky in the light theme, where the wash is dark. */
val NightSkyText: Color = Nest.Cream

val LocalFeatherColors = staticCompositionLocalOf { LightFeatherColors }

val MaterialTheme.featherColors: FeatherColors
    @Composable
    @ReadOnlyComposable
    get() = LocalFeatherColors.current
