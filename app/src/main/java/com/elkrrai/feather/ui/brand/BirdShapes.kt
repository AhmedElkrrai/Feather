package com.elkrrai.feather.ui.brand

import androidx.compose.ui.graphics.Color

/**
 * The bird, as path data in a 96 by 96 box, facing right. These strings are the single source of
 * the artwork: the Compose drawing ([BirdIllustration]) parses them, and the vector drawables
 * (`ic_bird_mark`, the launcher icon, the launch screen, the notification icon) and the SVGs in
 * `store/graphics` repeat them. `BirdArtworkTest` fails if a drawable drifts from these.
 *
 * To change the bird, change the paths here and in those files; no screen needs to change.
 */
object BirdShapes {
    const val SIZE = 96f

    const val TAIL = "M28,56 L5,47 L9,60 L6,70 L30,66 Z"
    const val BODY =
        "M30,54 C32,40 44,32 56,32 C68,32 78,38 78,48 C78,52 76,56 72,60 C66,70 52,78 40,76 C32,74 28,64 30,54 Z"
    const val HEAD = "M78,38 A16,16 0 1,1 46,38 A16,16 0 1,1 78,38 Z"
    const val BELLY = "M44,60 C50,54 62,54 70,58 C66,68 54,74 44,70 C42,66 42,63 44,60 Z"
    const val WING = "M40,50 C30,48 18,54 12,70 C26,70 38,64 46,56 Z"
    const val BEAK = "M76,38 L90,43 L76,48 Z"
    const val CREST = "M60,22 C57,14 61,9 66,7 C66,13 68,18 66,23 Z"

    // Circles and lines are drawn directly; the drawables spell them as paths.
    const val EYE_X = 66f
    const val EYE_Y = 37f
    const val EYE_RADIUS = 3.2f
    const val CHEEK_X = 70f
    const val CHEEK_Y = 46f
    const val CHEEK_RADIUS = 3f

    /** Where the wing turns when it flaps. */
    const val WING_PIVOT_X = 44f
    const val WING_PIVOT_Y = 52f

    /** Where the whole bird tilts and breathes from: just above its feet. */
    const val BODY_PIVOT_X = 47f
    const val BODY_PIVOT_Y = 76f

    val FEET: List<Pair<Pair<Float, Float>, Pair<Float, Float>>> = listOf(
        (50f to 76f) to (50f to 86f),
        (60f to 74f) to (60f to 84f),
    )
}

/** The brand colors. The same values are in the drawables and in the app's color schemes. */
object BirdColors {
    val Plumage = Color(0xFF2A7F9E)
    val Deep = Color(0xFF17485E)
    val Cream = Color(0xFFFFF3DC)
    val Amber = Color(0xFFF2A93B)
    val Coral = Color(0xFFE8735F)
    val Ink = Color(0xFF16262E)
}
