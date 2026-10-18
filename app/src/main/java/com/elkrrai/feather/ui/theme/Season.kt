package com.elkrrai.feather.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.Month

/**
 * The time of year, for one small touch: the leaves in the scenes and on the Today branch turn amber
 * in October and November and blossom pink in spring. It is a static recolor, with no animation.
 */
enum class Season { Autumn, Spring, Other }

fun seasonFor(date: LocalDate): Season = when (date.month) {
    Month.OCTOBER, Month.NOVEMBER -> Season.Autumn
    Month.MARCH, Month.APRIL, Month.MAY -> Season.Spring
    else -> Season.Other
}

/** The soft pink of spring blossom. */
private val Blossom = nest(0xE8A0B5)

/** The color of a plain leaf in [season]: green most of the year, amber in autumn, blossom in spring. */
fun FeatherColors.leafColor(season: Season): Color = when (season) {
    Season.Autumn -> amberFill
    Season.Spring -> Blossom
    Season.Other -> listColors[3]
}

/** The season of today's date, worked out once while the screen is shown. */
@Composable
fun rememberSeason(): Season = remember { seasonFor(LocalDate.now()) }
