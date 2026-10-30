package com.elkrrai.feather.ui.brand.scenes

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.elkrrai.feather.R
import com.elkrrai.feather.ui.brand.SideOwlFlight
import com.elkrrai.feather.ui.brand.flapAt
import com.elkrrai.feather.ui.brand.foldAt
import com.elkrrai.feather.ui.brand.smoothStep
import com.elkrrai.feather.ui.brand.rememberBirdMotionAllowed
import com.elkrrai.feather.ui.theme.DayPhase
import com.elkrrai.feather.ui.today.dayPhaseFor
import com.elkrrai.feather.ui.today.showsMoon
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * The owl flying, seen from the side, always in view, in a rhythm of two parts: a glide, wings held
 * up while it floats on a gentle wave and the clouds drift past; then a burst, where it beats its
 * wings fast, surges forward and nose up, rides a bigger wave, and the clouds and the hills rush
 * past; then it settles back into the glide. The scene is one rounded picture of sky, [width] by
 * [height] (it can fill a screen: the board is 200 units wide and as tall as the shape needs), so
 * it follows the time of day: a blue sky and the sun by day, a dark one and the moon at night. With movement off, everything holds still with the owl in
 * the middle, wings down.
 */
@Composable
internal fun ClearSkiesScene(width: Dp, height: Dp, modifier: Modifier = Modifier, fullBleed: Boolean = false) =
    SceneFrame(width, modifier, height = height) {
        val p = palette
        val moving = rememberBirdMotionAllowed()
        // filling a whole screen, the picture has square corners
        val radius = if (fullBleed) 0f else PANEL_RADIUS
        // the board is 200 units wide and this many tall
        val boardHeight = height / unit
        // the sky is the one of the time of day, as on Today: dawn, day, dusk or night, with the moon at night
        val phase = remember { dayPhaseFor(LocalTime.now()) }
        val (skyTop, skyBottom) = skyOf(phase)
        val night = showsMoon(phase)
        val hazy = phase == DayPhase.Dawn || phase == DayPhase.Dusk
        // the sun or the moon sits up in the corner, and lower at the edges of the day
        val skyBodyY = (if (hazy) 0.34f else 0.26f) * boardHeight
        // clouds are white against a sunlit sky and a dim blue-grey against the night one
        val cloudColor = if (night) Color(0xFF3B5668) else Color.White.copy(alpha = 0.96f)
        val flight = rememberFlight(moving)
        val restY = REST_HEIGHT * boardHeight
        val cloudSpan = SCENE_BOARD + 2f * CLOUD_MARGIN

        val panel = Path().apply {
            addRoundRect(RoundRect(0f, 0f, SCENE_BOARD, boardHeight, CornerRadius(radius)))
        }
        Layer {
            clipPath(panel) {
                drawRect(Brush.verticalGradient(listOf(skyTop, skyBottom), startY = 0f, endY = boardHeight))
                if (night) drawMoon(p, Offset(146f, skyBodyY - 14f), 28f)
            }
        }
        // the sun turns slowly behind the clouds: the bright one by day and dawn, the dark one at dusk
        if (!night) {
            Image(
                painter = painterResource(if (phase == DayPhase.Dusk) R.drawable.sun_dusk else R.drawable.sun_day),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .offset(x = unit * (SUN_X - SUN_SIZE / 2f), y = unit * (skyBodyY - SUN_SIZE / 2f))
                    .size(unit * SUN_SIZE)
                    .graphicsLayer { rotationZ = flight.sunTurn % 360f },
            )
        }
        Layer {
            clipPath(panel) {
                // the far clouds, small and slow
                val farPhase = flight.distance * FAR_CLOUD_SPEED / cloudSpan
                drawDriftingCloud(p, cloudColor, Offset(150f, 0.38f * boardHeight), 0.7f, farPhase)
                drawDriftingCloud(p, cloudColor, Offset(36f, 0.56f * boardHeight), 0.7f, farPhase)
            }
        }
        // two rows of hills, the far one higher up and slower; on the light sky they are paler
        HillStrip(boardHeight, radius, R.drawable.hills_far, { flight.distance * FAR_HILLS_SPEED / (2f * SCENE_BOARD) }, FAR_HILLS_LIFT, if (night) 0.8f else if (hazy) 0.6f else 0.4f)
        HillStrip(boardHeight, radius, R.drawable.hills_near, { flight.distance * NEAR_HILLS_SPEED / (2f * SCENE_BOARD) }, 0f, if (night) 1f else if (hazy) 0.8f else 0.55f)
        // The owl is boxed where it rests in the middle of the sky and moved from there by the
        // flight: forward in a burst, up and down on its wave, tipped with it.
        Box(Modifier.size(unit * SCENE_BOARD, unit * boardHeight).clip(RoundedCornerShape(unit * radius))) {
            SideOwlFlight(
                flap = { flight.flap },
                fold = { flight.fold },
                modifier = Modifier
                    .offset(
                        x = unit * (SCENE_BOARD / 2f - OWL_ART_CENTRE_X * OWL_SIDE),
                        y = unit * (restY - OWL_ART_CENTRE_Y * OWL_SIDE),
                    )
                    .size(unit * OWL_SIDE)
                    .graphicsLayer {
                        val unitPx = size.width / OWL_SIDE
                        translationX = flight.surge * unitPx
                        translationY = flight.height * unitPx
                        rotationZ = flight.pitch
                    },
            )
        }
        Layer {
            clipPath(panel) {
                // the near clouds, larger and quicker, pass in front of the sky and the hills
                val nearPhase = flight.distance * NEAR_CLOUD_SPEED / cloudSpan
                drawDriftingCloud(p, cloudColor, Offset(70f, 0.74f * boardHeight), 1.1f, nearPhase)
                drawDriftingCloud(p, cloudColor, Offset(170f, 0.14f * boardHeight), 1f, nearPhase)
            }
            drawRoundRect(
                p.ink.copy(alpha = 0.12f),
                size = Size(SCENE_BOARD, boardHeight),
                cornerRadius = CornerRadius(radius),
                style = Stroke(1.2f),
            )
        }
    }

/**
 * How the owl is flying at this moment, and how far the world has gone past it. A round lasts
 * [ROUND_SECONDS]: a glide, a burst, then back to the glide. Every number here is read while
 * drawing, so the scene moves without recomposing.
 */
@Stable
private class Flight {
    /** Whether movement is off: then the owl hangs its wings in the middle and nothing moves. */
    var still by mutableStateOf(true)

    /** 0 in the glide, up to 1 in the middle of the burst. */
    var boost by mutableFloatStateOf(0f)

    /** Where in a wing beat the owl is (0 to 1), and where in a wave. */
    var beat by mutableFloatStateOf(0f)
    var wave by mutableFloatStateOf(0f)

    /** How far the clouds' base speed has carried the world past the owl, in board units. */
    var distance by mutableFloatStateOf(0f)

    /** How far the sun has turned, in degrees: at its own steady pace, whether the owl glides or bursts. */
    var sunTurn by mutableFloatStateOf(0f)

    /** The wings hang when still, are held up in the glide and beat in the burst. */
    val flap: Float get() = if (still) 0f else 1f - boost * (1f - flapAt(beat))
    val fold: Float get() = if (still) 0f else boost * foldAt(beat)

    /** How far the owl has surged ahead of its middle, in board units. */
    val surge: Float get() = SURGE * boost

    private val waveHeight: Float get() = GLIDE_WAVE + BURST_WAVE * boost

    /** How far below its middle height the owl is: a wave that gets bigger and quicker in the burst. */
    val height: Float get() = waveHeight * sin(2f * PI.toFloat() * wave)

    /** The owl's tilt in degrees: with the wave, and nose up in the burst. */
    val pitch: Float
        get() {
            if (still) return 0f
            val rise = waveHeight * 2f * PI.toFloat() * cos(2f * PI.toFloat() * wave)
            return atan2(rise, FORWARD_REFERENCE) * (180f / PI.toFloat()) * PITCH_FOLLOW - NOSE_UP * boost
        }

    fun reset(still: Boolean) {
        this.still = still
        boost = 0f
        beat = 0f
        wave = 0f
        distance = 0f
        sunTurn = 0f
    }
}

@Composable
private fun rememberFlight(moving: Boolean): Flight {
    val flight = remember { Flight() }
    LaunchedEffect(moving) {
        flight.reset(still = !moving)
        if (!moving) return@LaunchedEffect
        var last = withFrameNanos { it }
        var t = 0f
        while (true) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
            last = now
            t = (t + dt) % ROUND_SECONDS
            val boost = boostAt(t)
            flight.boost = boost
            flight.beat = (flight.beat + dt * BEAT_HZ * boost) % 1f
            flight.wave = (flight.wave + dt * (WAVE_HZ + WAVE_BURST_HZ * boost)) % 1f
            flight.distance += dt * BASE_SPEED * (1f + SPEED_BURST * boost)
            flight.sunTurn += dt * SUN_TURN_DEGREES_PER_SECOND
        }
    }
    return flight
}

/** How strong the burst is [t] seconds into a round: none in the glide, rising, full, falling. */
private fun boostAt(t: Float): Float = when {
    t < BURST_START -> 0f
    t < BURST_FULL -> smoothStep(BURST_START, BURST_FULL, t)
    t < BURST_HOLD_END -> 1f
    t < BURST_END -> 1f - smoothStep(BURST_HOLD_END, BURST_END, t)
    else -> 0f
}

/**
 * The sky's top and bottom colours at each time of day. They are the sky's own, whatever the app's
 * theme is: the dark theme does not make the day dark.
 */
private fun skyOf(phase: DayPhase): Pair<Color, Color> = when (phase) {
    DayPhase.Dawn -> Color(0xFFF3B79E) to Color(0xFFFBE4C4)
    DayPhase.Day -> Color(0xFF86CBE8) to Color(0xFFDDF2F9)
    DayPhase.Dusk -> Color(0xFF6C6CA6) to Color(0xFFF2AC7C)
    DayPhase.Night -> Color(0xFF0E1F2A) to Color(0xFF1D3B4A)
}

/**
 * A cloud drifting left once across the picture for each [phase] of 0 to 1, coming back in from the
 * right: [start] is where it is at a phase of 0.
 */
private fun DrawScope.drawDriftingCloud(p: ScenePalette, color: Color, start: Offset, size: Float, phase: Float) {
    val span = SCENE_BOARD + 2f * CLOUD_MARGIN
    val travelled = (start.x + CLOUD_MARGIN - phase * span) % span
    val x = (if (travelled < 0f) travelled + span else travelled) - CLOUD_MARGIN
    val center = Offset(x, start.y)
    scale(size, pivot = center) { drawCloud(p, center, color) }
}

/**
 * A row of hills (a 2000 by 496 picture) as wide as the sky picture, scrolling left for ever: each
 * [phase] of 0 to 1 moves it two picture widths. It is tiled one way up, then flipped, then the
 * first way again, so the joins always match even where the picture's own edges do not. [lift]
 * raises it by that many board units from the bottom of the sky, which is [boardHeight] tall; the
 * sky has corners of [radius] units.
 */
@Composable
private fun SceneScope.HillStrip(boardHeight: Float, radius: Float, drawable: Int, phase: () -> Float, lift: Float, alpha: Float) {
    Box(Modifier.size(unit * SCENE_BOARD, unit * boardHeight).clip(RoundedCornerShape(unit * radius))) {
        for (tile in 0..2) {
            Image(
                painter = painterResource(drawable),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                alpha = alpha,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(y = -(unit * lift))
                    .requiredSize(unit * SCENE_BOARD, unit * SCENE_BOARD * HILLS_HEIGHT_PER_WIDTH)
                    .graphicsLayer {
                        translationX = (tile - (phase() % 1f) * 2f) * size.width
                        scaleX = if (tile % 2 == 1) -1f else 1f
                    },
            )
        }
    }
}

// A round: a glide of 2 seconds, a burst that builds for a second, holds for three, eases for one
// and a half, and a second of glide again, which joins the next round's: the wings are held up for
// 3 seconds at a time.
private const val ROUND_SECONDS = 8.5f
private const val BURST_START = 2f
private const val BURST_FULL = 3f
private const val BURST_HOLD_END = 6f
private const val BURST_END = 7.5f

/** Wing beats a second at the height of the burst. */
private const val BEAT_HZ = 2.2f

/** Waves a second in the glide, and how many more in the burst. */
private const val WAVE_HZ = 0.15f
private const val WAVE_BURST_HZ = 0.35f

// The world's speed in the glide, in board units a second, and how many times more in the burst; each
// row below is a share of it, the nearer the faster.
private const val BASE_SPEED = 22f
private const val SPEED_BURST = 3f
private const val NEAR_CLOUD_SPEED = 1f
private const val FAR_CLOUD_SPEED = 0.5f
private const val NEAR_HILLS_SPEED = 0.18f
private const val FAR_HILLS_SPEED = 0.09f

private const val SURGE = 16f
private const val GLIDE_WAVE = 5f
private const val BURST_WAVE = 9f
private const val NOSE_UP = 7f
private const val PITCH_FOLLOW = 0.5f

/** How far the owl would go forward in a wave if it flew on, used only to work out its tilt. */
private const val FORWARD_REFERENCE = 340f

/** How far down the sky the owl's middle is, as a share of its height. */
private const val REST_HEIGHT = 0.48f

// The owl's picture is boxed 170 board units across; its body is centred a little left of and
// above the middle of that box.
private const val OWL_SIDE = 170f
private const val OWL_ART_CENTRE_X = 0.44f
private const val OWL_ART_CENTRE_Y = 0.5f

private const val SUN_X = 156f
private const val SUN_SIZE = 44f
/** The sun turns this many degrees a second, always: it does not speed up with the owl. */
private const val SUN_TURN_DEGREES_PER_SECOND = 7.7f
private const val FAR_HILLS_LIFT = 24f
private const val HILLS_HEIGHT_PER_WIDTH = 496f / 2000f
private const val PANEL_RADIUS = 12f
private const val CLOUD_MARGIN = 40f
