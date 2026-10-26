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
 * Three hungry owlets in the nest the person chose, side by side, each rocking a little on its own
 * beat so they are never in step: one with its beak open, one wailing, one just staring up with
 * big eyes. The nest is as wide as the picture and a little flattened. With movement off they sit
 * still.
 */
@Composable
internal fun HungryChickScene(size: Dp, modifier: Modifier) = SceneFrame(size, modifier) {
    val p = palette
    val moving = rememberBirdMotionAllowed()
    val art = DisplayPrefs.nestStyle.art()
    // a nest as wide as the picture, squashed down a little so it is wider than it is tall
    val nestHeight = BIG_NEST_WIDTH * art.heightPerWidth * NEST_FLATTEN
    val nestTop = GROUND_LINE - nestHeight
    val nestLeft = (SCENE_BOARD - BIG_NEST_WIDTH) / 2f
    // the chicks sit with their feet at the floor of the nest, a little down into it
    val feet = nestTop + art.feet * nestHeight + CHICK_SINK
    Layer { drawGround(p, y = 178f, halfWidth = 96f) }
    NestLayer(art.back, nestTop, nestHeight, left = nestLeft, width = BIG_NEST_WIDTH)
    // the one in the middle is a little further back, so it sits higher and is drawn first
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
    ChickSpot(R.drawable.chick_0, centre = -50f, lift = 3f, wobbleMillis = 2_300, degrees = 5f, startAt = 0.00f),
    ChickSpot(R.drawable.chick_1, centre = 0f, lift = -3f, wobbleMillis = 1_900, degrees = 4.5f, startAt = 0.40f),
    ChickSpot(R.drawable.chick_2, centre = 50f, lift = 3f, wobbleMillis = 2_600, degrees = 5.5f, startAt = 0.75f),
)

private val DRAW_ORDER = listOf(1, 0, 2)

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
private const val CHICK_HEIGHT = 70f
private const val CHICK_WIDTH = CHICK_HEIGHT * 0.797f
private const val CHICK_SINK = 3f
private const val WOBBLE_STRETCH = 0.012f

/** The nest here is as wide as the picture, and this much as tall as its drawing would be. */
private const val BIG_NEST_WIDTH = SCENE_BOARD
private const val NEST_FLATTEN = 0.8f
