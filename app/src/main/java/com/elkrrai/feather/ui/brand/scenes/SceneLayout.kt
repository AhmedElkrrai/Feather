package com.elkrrai.feather.ui.brand.scenes

import kotlin.math.PI
import kotlin.math.sin

/** Every scene is drawn on a 200 by 200 board; the board is scaled to whatever size is shown. */
const val SCENE_BOARD = 200f

private const val NORMAL_SIZE_DP = 190f
private const val LARGE_FONT_SIZE_DP = 140f
private const val MIN_FREE_HEIGHT_DP = 320f
private const val LARGE_FONT_SCALE = 2f

/**
 * How big a scene is shown, in dp, or null when it should be left out: 190dp normally, 140dp when
 * the font is set to 200 percent, and nothing when less than 320dp of height is free (a short
 * landscape screen), so the words always fit.
 */
fun sceneSizeDp(freeHeightDp: Float, fontScale: Float): Float? = when {
    freeHeightDp < MIN_FREE_HEIGHT_DP -> null
    fontScale >= LARGE_FONT_SCALE -> LARGE_FONT_SIZE_DP
    else -> NORMAL_SIZE_DP
}

/** A leaf or feather that sways a little: up to 3 degrees each way, once per [phase] of 1. */
fun swayDegrees(phase: Float): Float = 3f * sin(2.0 * PI * phase).toFloat()

/** How far the first-launch sun climbs, in board units, when [progress] runs from 0 to 1. */
const val SUN_RISE_BOARD_UNITS = 24f

fun sunRise(progress: Float): Float = SUN_RISE_BOARD_UNITS * progress.coerceIn(0f, 1f)

/**
 * Where the bird's 96 by 96 box goes on the board: its top-left corner, how much it is scaled, and
 * how far it is tilted (degrees, around its middle). One transform places it in every scene.
 */
data class BirdPlacement(val x: Float, val y: Float, val scale: Float, val tilt: Float = 0f) {
    /** The side of the bird's box on the board. */
    val side: Float get() = 96f * scale

    /** Whether the whole box lies on the board. */
    val isOnBoard: Boolean
        get() = x >= 0f && y >= 0f && x + side <= SCENE_BOARD && y + side <= SCENE_BOARD
}
