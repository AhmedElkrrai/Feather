package com.elkrrai.feather.ui.brand

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.elkrrai.feather.R
import com.elkrrai.feather.ui.DisplayPrefs
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

/**
 * What the owl wears, on its resting pose: the number of each garment (the first is 0), or -1 for
 * none. Each is a layer drawn on the owl, so it moves with the owl.
 */
data class OwlDress(val scarf: Int = -1, val glasses: Int = -1, val hat: Int = -1) {
    companion object {
        val None = OwlDress()
    }
}

private val Scarves = intArrayOf(
    R.drawable.owl_dress_scarf_0, R.drawable.owl_dress_scarf_1, R.drawable.owl_dress_scarf_2,
    R.drawable.owl_dress_scarf_3, R.drawable.owl_dress_scarf_4, R.drawable.owl_dress_scarf_5,
)
private val Spectacles = intArrayOf(R.drawable.owl_dress_glasses_0, R.drawable.owl_dress_glasses_1)
private val Hats = intArrayOf(
    R.drawable.owl_dress_hat_0, R.drawable.owl_dress_hat_1, R.drawable.owl_dress_hat_2, R.drawable.owl_dress_hat_3,
)

/**
 * The owl, Feather's one character, in [mood]. The art is layered PNGs rendered from the Recraft
 * SVGs in `art/owl` (see its README): every layer of a pose shares one square artboard, so a pose is
 * its layers stacked, and the movement is code.
 *
 * - [BirdMood.IDLE]: the upright owl, three quarters on: breathing, blinking, a curious head tilt
 *   and now and then a preen.
 * - [BirdMood.SLEEPING]: the hunched owl with its eyes shut: slow breathing, a pulsing glow and
 *   floating z letters.
 * - [BirdMood.HAPPY], [BirdMood.FLYING]: the front-facing owl with spread wings, driven by [birdPose].
 *
 * [flightTrigger] counts up each time the owl should fly a lap and land again ([birdFlightPose]);
 * nothing happens when movement is not allowed or Little celebrations is off. Without movement the
 * owl keeps its calm pose. What it [dress] is worn only by the resting owl, which does not preen
 * when [preens] is false (the preen pose is drawn without clothes).
 */
@Composable
fun OwlIllustration(
    modifier: Modifier = Modifier,
    mood: BirdMood = BirdMood.IDLE,
    contentDescription: String? = null,
    flightTrigger: Int = 0,
    dress: OwlDress = OwlDress.None,
    preens: Boolean = true,
) {
    val allowed = rememberBirdMotionAllowed()
    val flight = remember { Animatable(0f) }
    var flying by remember { mutableStateOf(false) }
    LaunchedEffect(flightTrigger) {
        if (flightTrigger > 0 && allowed && DisplayPrefs.celebrations) {
            flying = true
            try {
                flight.snapTo(0f)
                flight.animateTo(1f, tween(durationMillis = FLIGHT_MILLIS, easing = LinearEasing))
            } finally {
                flying = false
            }
        }
    }

    Box(modifier.describedAs(contentDescription)) {
        // The owl at rest gives way to the flying owl and comes back when it has landed.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = if (flying) 1f - flightBlend(flight.value) else 1f },
        ) {
            when (mood) {
                BirdMood.IDLE -> IdleOwl(dress, preens)
                BirdMood.SLEEPING -> SleepingOwl(allowed)
                BirdMood.HAPPY, BirdMood.FLYING -> {
                    val phase = rememberAmbientPhase(periodMillis(mood), enabled = allowed)
                    OwlFlight(pose = { birdPose(mood, phase.value) }, still = !allowed)
                }
            }
        }
        if (flying) {
            OwlFlight(
                pose = { birdFlightPose(flight.value) },
                modifier = Modifier.graphicsLayer { alpha = flightBlend(flight.value) },
            )
        }
    }
}

internal fun Modifier.describedAs(description: String?): Modifier =
    if (description == null) clearAndSetSemantics { } else semantics { contentDescription = description }

/** The upright owl, three quarters on, with separate head, wing and eye layers. */
@Composable
private fun IdleOwl(dress: OwlDress, preens: Boolean) {
    val motionAllowed = rememberBirdMotionAllowed()
    val breathing = remember { Animatable(0f) }
    val headTilt = remember { Animatable(0f) }
    val preenPose = remember { Animatable(0f) }
    var eyesOpen by remember { mutableStateOf(true) }

    LaunchedEffect(motionAllowed) {
        breathing.snapTo(0f)
        if (!motionAllowed) return@LaunchedEffect
        while (true) {
            breathing.animateTo(1f, tween(BREATH_HALF_CYCLE_MILLIS, easing = FastOutSlowInEasing))
            breathing.animateTo(0f, tween(BREATH_HALF_CYCLE_MILLIS, easing = LinearOutSlowInEasing))
        }
    }

    LaunchedEffect(motionAllowed, preens) {
        headTilt.snapTo(0f)
        preenPose.snapTo(0f)
        eyesOpen = true
        if (!motionAllowed) return@LaunchedEffect

        while (true) {
            // The owl rests upright for a beat, then makes one crisp blink.
            delay(REST_BEFORE_BLINK_MILLIS)
            eyesOpen = false
            delay(BLINK_MILLIS)
            eyesOpen = true

            // A short, curious look before the preen.
            delay(BEFORE_TILT_MILLIS)
            headTilt.animateTo(CURIOUS_TILT_DEGREES, tween(HEAD_TILT_MILLIS, easing = FastOutSlowInEasing))
            delay(CURIOUS_HOLD_MILLIS)
            headTilt.animateTo(0f, tween(HEAD_RETURN_MILLIS, easing = LinearOutSlowInEasing))

            delay(BEFORE_PREEN_MILLIS)
            if (preens) {
                eyesOpen = false
                preenPose.animateTo(1f, tween(PREEN_TRANSITION_MILLIS, easing = FastOutSlowInEasing))
                delay(PREEN_HOLD_MILLIS)
                preenPose.animateTo(0f, tween(PREEN_TRANSITION_MILLIS, easing = LinearOutSlowInEasing))
                eyesOpen = true
            }
            delay(REST_AFTER_PREEN_MILLIS)
        }
    }

    // The complete preen pose is a matching key pose; the resting pose keeps its separate layers.
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                transformOrigin = TransformOrigin(BODY_PIVOT_X, BODY_PIVOT_Y)
                scaleX = 1f + BREATH_SCALE_X * breathing.value
                scaleY = 1f + BREATH_SCALE_Y * breathing.value
            },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 1f - preenPose.value },
        ) {
            OwlLayer(R.drawable.owl_body_back)
            OwlLayer(R.drawable.owl_body)
            OwlLayer(R.drawable.owl_wing)
            OwlLayer(R.drawable.owl_chest)
            Scarves.getOrNull(dress.scarf)?.let { OwlLayer(it) }
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(HEAD_PIVOT_X, HEAD_PIVOT_Y)
                        rotationZ = headTilt.value
                    },
            ) {
                OwlLayer(R.drawable.owl_head)
                OwlLayer(if (eyesOpen) R.drawable.owl_eyes_open else R.drawable.owl_eyes_closed)
                Spectacles.getOrNull(dress.glasses)?.let { OwlLayer(it) }
                Hats.getOrNull(dress.hat)?.let { OwlLayer(it, Modifier.graphicsLayer { translationY = -size.height * HAT_LIFT }) }
            }
            // the glowing rune on the chest is covered by a scarf
            if (Scarves.getOrNull(dress.scarf) == null) {
                Canvas(Modifier.fillMaxSize()) {
                    drawEmblem(
                        center = Offset(size.width * CHEST_GLOW_X, size.height * CHEST_GLOW_Y),
                        halfWidth = CHEST_EMBLEM_HALF_WIDTH,
                        halfHeight = CHEST_EMBLEM_HALF_HEIGHT,
                        glowRadius = CHEST_GLOW_RADIUS + CHEST_GLOW_BREATH_RADIUS * breathing.value,
                        glowAlpha = CHEST_GLOW_BASE_ALPHA + CHEST_GLOW_BREATH_ALPHA * breathing.value,
                    )
                }
            }
        }
        OwlLayer(R.drawable.owl_preen, Modifier.graphicsLayer { alpha = preenPose.value })
    }
}

/**
 * The hunched owl, eyes shut. It breathes slowly (one breath is [periodMillis] for
 * [BirdMood.SLEEPING]); the glow of its closed eyes swells with each breath and two z letters float
 * up beside its head. With movement off it holds the calm pose, one z showing.
 */
@Composable
private fun SleepingOwl(motionAllowed: Boolean) {
    val phase = rememberAmbientPhase(periodMillis(BirdMood.SLEEPING), enabled = motionAllowed)
    val zColor = LocalContentColor.current
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val breath = breath(phase.value)
                    transformOrigin = TransformOrigin(SLEEP_PIVOT_X, SLEEP_PIVOT_Y)
                    scaleX = 1f + SLEEP_BREATH_X * breath
                    scaleY = 1f + SLEEP_BREATH_Y * breath
                },
        ) {
            OwlLayer(R.drawable.owl_sleep_body)
            // The body already shows the closed eyes; this lays their glow over it again, brighter on the in-breath.
            OwlLayer(R.drawable.owl_sleep_eyes, Modifier.graphicsLayer { alpha = SLEEP_GLOW_MAX * breath(phase.value) })
        }
        Canvas(Modifier.fillMaxSize()) {
            for (index in 0..1) {
                val t = (phase.value + index * 0.5f) % 1f
                val unit = size.minDimension
                val x = unit * (SLEEP_Z_X + SLEEP_Z_DRIFT_X * t)
                val y = unit * (SLEEP_Z_Y - SLEEP_Z_RISE * t)
                val side = unit * (SLEEP_Z_SIZE + SLEEP_Z_GROWTH * t)
                val color = zColor.copy(alpha = SLEEP_Z_ALPHA * sin(PI * t).toFloat())
                val width = unit * SLEEP_Z_STROKE
                drawLine(color, Offset(x, y), Offset(x + side, y), width, StrokeCap.Round)
                drawLine(color, Offset(x + side, y), Offset(x, y + side), width, StrokeCap.Round)
                drawLine(color, Offset(x, y + side), Offset(x + side, y + side), width, StrokeCap.Round)
            }
        }
    }
}

/**
 * The front-facing owl with spread wings, posed by [pose] (a [BirdPose] from [birdPose] or
 * [birdFlightPose]): its offsets move it, its wing angle beats the wings through their four
 * drawings (lowered, curled, fanned, raised), and its body scale squashes it. With [still] the wings hang down, calm.
 */
@Composable
internal fun OwlFlight(pose: () -> BirdPose, modifier: Modifier = Modifier, still: Boolean = false) {
    fun flap(): Float = if (still) 0f else flapOf(pose().wingAngle)
    Box(
        modifier
            .fillMaxSize()
            .graphicsLayer {
                val current = pose()
                // The pose's offsets are in the 96 units of the old bird's box.
                val unit = size.minDimension / POSE_BOX_UNITS
                translationX = current.bodyOffsetX * unit
                translationY = current.bodyOffsetY * unit + size.height * FLY_SETTLE_Y
                scaleX = FLY_SCALE * current.bodyScale
                scaleY = FLY_SCALE * current.bodyScale
                transformOrigin = TransformOrigin(0.5f, FLY_PIVOT_Y)
            },
    ) {
        for (wing in LeftWings) {
            OwlLayer(wing.drawable, Modifier.graphicsLayer { alpha = wingPoseAlpha(wing.index, flap()) })
        }
        for (wing in RightWings) {
            OwlLayer(wing.drawable, Modifier.graphicsLayer { alpha = wingPoseAlpha(wing.index, flap()) })
        }
        OwlLayer(R.drawable.owl_fly_body)
        OwlLayer(R.drawable.owl_fly_head)
        OwlLayer(R.drawable.owl_fly_eyes)
        Canvas(Modifier.fillMaxSize()) {
            drawEmblem(
                center = Offset(size.width * FLY_EMBLEM_X, size.height * FLY_EMBLEM_Y),
                halfWidth = FLY_EMBLEM_HALF_WIDTH,
                halfHeight = FLY_EMBLEM_HALF_HEIGHT,
                glowRadius = FLY_GLOW_RADIUS,
                glowAlpha = FLY_GLOW_ALPHA,
            )
        }
    }
}

@Composable
internal fun OwlLayer(drawable: Int, modifier: Modifier = Modifier, colorFilter: ColorFilter? = null) {
    Image(
        painter = painterResource(drawable),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        colorFilter = colorFilter,
        modifier = modifier.fillMaxSize(),
    )
}

/**
 * The glowing diamond on the owl's chest. Sizes are shares of the smaller side of the drawing area:
 * the glow behind it is [glowRadius] wide and [glowAlpha] strong.
 */
private fun DrawScope.drawEmblem(center: Offset, halfWidth: Float, halfHeight: Float, glowRadius: Float, glowAlpha: Float) {
    val radius = size.minDimension * glowRadius
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xE050E5FF), Color(0xA050E5FF), Color.Transparent),
            center = center,
            radius = radius,
        ),
        center = center,
        radius = radius,
        alpha = glowAlpha,
    )
    val width = size.minDimension * halfWidth
    val height = size.minDimension * halfHeight
    val diamond = Path().apply {
        moveTo(center.x, center.y - height)
        lineTo(center.x + width, center.y)
        lineTo(center.x, center.y + height)
        lineTo(center.x - width, center.y)
        close()
    }
    drawPath(diamond, Color(0xFF70F1FF), style = Stroke(width = size.minDimension * EMBLEM_STROKE))
    drawCircle(Color(0xFFB8FBFF), radius = size.minDimension * EMBLEM_CORE_RADIUS, center = center)
}

/** One slow breath for a phase of 0 to 1: out at the ends, in half way. */
private fun breath(phase: Float): Float = 0.5f - 0.5f * cos(2f * PI.toFloat() * phase)

/** How far up the wings are, 0 (hanging) to 1 (raised), for a pose's wing angle (negative lifts the wing). */
private fun flapOf(wingAngle: Float): Float = (0.5f - wingAngle / (2f * FLAP_RANGE_DEGREES)).coerceIn(0f, 1f)

/** One drawing of a wing: the [index]th of [WING_POSES], from lowered (0) to raised. */
private class WingPose(val index: Int, val drawable: Int)

// A wing beat is four drawings, lowered, curled, fanned and raised, drawn in that order.
private const val WING_POSES = 4

private val LeftWings = listOf(
    WingPose(0, R.drawable.owl_fly_wing_left_down),
    WingPose(1, R.drawable.owl_fly_wing_left_curl),
    WingPose(2, R.drawable.owl_fly_wing_left_mid),
    WingPose(3, R.drawable.owl_fly_wing_left_up),
)

private val RightWings = listOf(
    WingPose(0, R.drawable.owl_fly_wing_right_down),
    WingPose(1, R.drawable.owl_fly_wing_right_curl),
    WingPose(2, R.drawable.owl_fly_wing_right_mid),
    WingPose(3, R.drawable.owl_fly_wing_right_up),
)

/**
 * How opaque the [index]th wing drawing is at [flap] (0 lowered to 1 raised). Each drawing fades in
 * over the one before it, and that one stays underneath until the new one is nearly solid, so the
 * wing never thins out half way between two drawings.
 */
private fun wingPoseAlpha(index: Int, flap: Float): Float {
    val step = 1f / (WING_POSES - 1)
    val at = index * step
    val rise = if (index == 0) 1f else smoothStep(at - step, at, flap)
    val fall = if (index == WING_POSES - 1) 1f else 1f - smoothStep(at + step - WING_HANDOVER, at + step, flap)
    return rise * fall
}

/** How much of a flight, 0 to 1, is shown as the flying owl: it takes over at take-off and hands back when landed. */
private fun flightBlend(progress: Float): Float =
    smoothStep(0f, FLIGHT_BLEND_END, progress) * (1f - smoothStep(1f - FLIGHT_BLEND_END, 1f, progress))

internal fun smoothStep(from: Float, to: Float, x: Float): Float {
    val t = ((x - from) / (to - from)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private const val EMBLEM_STROKE = 0.008f
private const val EMBLEM_CORE_RADIUS = 0.009f

private const val BREATH_HALF_CYCLE_MILLIS = 2_100
private const val BREATH_SCALE_X = 0.004f
private const val BREATH_SCALE_Y = 0.009f
private const val BODY_PIVOT_X = 0.5f
private const val BODY_PIVOT_Y = 0.86f
private const val CHEST_GLOW_X = 0.665f
private const val CHEST_GLOW_Y = 0.55f
private const val CHEST_GLOW_RADIUS = 0.06f
private const val CHEST_GLOW_BREATH_RADIUS = 0.02f
private const val CHEST_GLOW_BASE_ALPHA = 0.62f
private const val CHEST_GLOW_BREATH_ALPHA = 0.38f
private const val CHEST_EMBLEM_HALF_WIDTH = 0.027f
private const val CHEST_EMBLEM_HALF_HEIGHT = 0.043f
/** The hat's picture is the owl's artboard moved down by this share of its height, so the hat has room above the owl. */
private const val HAT_LIFT = 128f / 512f
private const val HEAD_PIVOT_X = 0.56f
private const val HEAD_PIVOT_Y = 0.5f
private const val CURIOUS_TILT_DEGREES = -8f
private const val REST_BEFORE_BLINK_MILLIS = 1_700L
private const val BLINK_MILLIS = 95L
private const val BEFORE_TILT_MILLIS = 1_300L
private const val HEAD_TILT_MILLIS = 420
private const val CURIOUS_HOLD_MILLIS = 800L
private const val HEAD_RETURN_MILLIS = 450
private const val BEFORE_PREEN_MILLIS = 1_000L
private const val PREEN_TRANSITION_MILLIS = 450
private const val PREEN_HOLD_MILLIS = 1_250L
private const val REST_AFTER_PREEN_MILLIS = 850L

// Sleeping: the owl's feet are at about 95 percent of the artboard's height, a little left of centre.
private const val SLEEP_PIVOT_X = 0.45f
private const val SLEEP_PIVOT_Y = 0.95f
// About 3 dp of rise at the 84 dp size of the Today header: enough to see from arm's length.
private const val SLEEP_BREATH_X = 0.018f
private const val SLEEP_BREATH_Y = 0.04f
private const val SLEEP_GLOW_MAX = 0.8f
private const val SLEEP_Z_X = 0.80f
private const val SLEEP_Z_DRIFT_X = 0.07f
private const val SLEEP_Z_Y = 0.20f
private const val SLEEP_Z_RISE = 0.16f
private const val SLEEP_Z_SIZE = 0.04f
private const val SLEEP_Z_GROWTH = 0.03f
private const val SLEEP_Z_STROKE = 0.012f
private const val SLEEP_Z_ALPHA = 0.8f

// Flying: its art is smaller on the artboard than the resting owl (its feet at about 86 percent of
// the height, the body about half as tall), so it is scaled up about its feet and set down onto the
// resting owl's ground line.
private const val FLY_SCALE = 1.3f
private const val FLY_PIVOT_Y = 0.86f
private const val FLY_SETTLE_Y = 0.085f
private const val FLAP_RANGE_DEGREES = 30f
private const val WING_HANDOVER = 0.1f
private const val FLIGHT_BLEND_END = 0.06f
private const val FLY_EMBLEM_X = 0.4995f
private const val FLY_EMBLEM_Y = 0.6245f
private const val FLY_EMBLEM_HALF_WIDTH = 0.018f
private const val FLY_EMBLEM_HALF_HEIGHT = 0.027f
private const val FLY_GLOW_RADIUS = 0.045f
private const val FLY_GLOW_ALPHA = 0.8f
