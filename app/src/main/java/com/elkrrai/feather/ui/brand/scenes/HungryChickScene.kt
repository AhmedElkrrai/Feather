package com.elkrrai.feather.ui.brand.scenes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import com.elkrrai.feather.R
import com.elkrrai.feather.ui.DisplayPrefs
import com.elkrrai.feather.ui.brand.OwlLayer
import com.elkrrai.feather.ui.brand.rememberAmbientPhase
import com.elkrrai.feather.ui.brand.rememberBirdMotionAllowed
import kotlin.math.PI
import kotlin.math.sin

/**
 * Four hungry owlets in the nest the person chose, side by side, each rocking a little on its own
 * beat so they are never in step: one with its beak shut, one with it open, one wailing, one just
 * staring up with big eyes. With movement off they sit still.
 */
@Composable
internal fun HungryChickScene(size: Dp, modifier: Modifier) = SceneFrame(size, modifier) {
    val p = palette
    val moving = rememberBirdMotionAllowed()
    val art = DisplayPrefs.nestStyle.art()
    // a big nest, nearly the width of the picture
    val nestHeight = BIG_NEST_WIDTH * art.heightPerWidth
    val nestTop = GROUND_LINE - nestHeight
    val nestLeft = (SCENE_BOARD - BIG_NEST_WIDTH) / 2f
    // the chicks sit with their feet at the floor of the nest, a little down into it
    val feet = nestTop + art.feet * nestHeight + CHICK_SINK
    Layer { drawGround(p, y = 178f, halfWidth = 90f) }
    NestLayer(art.back, nestTop, nestHeight, left = nestLeft, width = BIG_NEST_WIDTH)
    // the two in the middle are a little further back, so they sit higher and are drawn first
    for (index in DRAW_ORDER) {
        val chick = Chicks[index]
        Chick(
            picture = chick.picture,
            left = SCENE_BOARD / 2f + chick.centre - CHICK_WIDTH / 2f,
            top = feet + chick.lift - CHICK_HEIGHT,
            wobbleMillis = chick.wobbleMillis,
            degrees = chick.degrees,
            startAt = chick.startAt,
            moving = moving,
        )
    }
    NestLayer(art.front, nestTop, nestHeight, left = nestLeft, width = BIG_NEST_WIDTH)
}

private class ChickSpot(
    val picture: Int,
    /** Where its middle is, from the middle of the nest, in board units. */
    val centre: Float,
    /** How far down (positive) or up it sits from the others' feet. */
    val lift: Float,
    /** How long one rock takes, how far it tips, and where in the rock it begins (0 to 1). */
    val wobbleMillis: Int,
    val degrees: Float,
    val startAt: Float,
)

private val Chicks = listOf(
    ChickSpot(R.drawable.chick_0, centre = -68f, lift = 3f, wobbleMillis = 2_300, degrees = 5f, startAt = 0.00f),
    ChickSpot(R.drawable.chick_1, centre = -23f, lift = -3f, wobbleMillis = 1_900, degrees = 4.5f, startAt = 0.35f),
    ChickSpot(R.drawable.chick_2, centre = 23f, lift = -3f, wobbleMillis = 2_600, degrees = 5.5f, startAt = 0.70f),
    ChickSpot(R.drawable.chick_3, centre = 68f, lift = 3f, wobbleMillis = 2_100, degrees = 4f, startAt = 0.15f),
)

private val DRAW_ORDER = listOf(1, 2, 0, 3)

/** One chick, rocking about its feet. */
@Composable
private fun SceneScope.Chick(
    picture: Int,
    left: Float,
    top: Float,
    wobbleMillis: Int,
    degrees: Float,
    startAt: Float,
    moving: Boolean,
) {
    val clock = rememberAmbientPhase(wobbleMillis, enabled = moving)
    Box(
        Modifier
            .offset(x = unit * left, y = unit * top)
            .size(unit * CHICK_WIDTH, unit * CHICK_HEIGHT)
            .graphicsLayer {
                transformOrigin = TransformOrigin(0.5f, 1f)
                val turn = 2f * PI.toFloat() * (clock.value + startAt)
                rotationZ = degrees * sin(turn)
                // it rises a touch as it rocks past upright
                val rise = sin(2f * turn)
                scaleY = 1f + WOBBLE_STRETCH * rise
                scaleX = 1f - WOBBLE_STRETCH * 0.5f * rise
            },
    ) {
        OwlLayer(picture)
    }
}

// Each chick's picture is about 0.8 as wide as it is tall.
private const val CHICK_HEIGHT = 60f
private const val CHICK_WIDTH = CHICK_HEIGHT * 0.797f
private const val CHICK_SINK = 3f
private const val WOBBLE_STRETCH = 0.012f

/** The nest here is nearly as wide as the picture. */
private const val BIG_NEST_WIDTH = 190f
