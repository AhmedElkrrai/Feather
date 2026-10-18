package com.elkrrai.feather.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.ui.theme.Nest
import com.elkrrai.feather.ui.theme.featherColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.random.Random
import kotlin.math.sin
import androidx.compose.runtime.Stable
import com.elkrrai.feather.ui.DisplayPrefs
import com.elkrrai.feather.ui.rememberReduceMotion
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.elkrrai.feather.ui.theme.Ease

/**
 * One feather in the confetti. The numbers are fixed, not random, so every burst looks the same and
 * the motion can be tested.
 */
internal data class ConfettiPiece(
    /** The direction it is thrown in, in degrees; 0 is toward the end, -90 is straight up. */
    val angleDegrees: Float,
    /** How fast it leaves, in dp per second. */
    val speedDp: Float,
    /** How fast it turns, in degrees per second (negative turns the other way). */
    val spinDegreesPerSecond: Float,
    /** How far it drifts from side to side as it falls, in dp. */
    val swayDp: Float,
    val swayHz: Float,
    /** Its length, in dp. */
    val sizeDp: Float,
    val colorIndex: Int,
    /** The share of the whole burst after which it has faded away, from 0 to 1. */
    val life: Float,
)

/** The whole burst lasts this long. */
internal const val CONFETTI_SECONDS = 1.8f

/** Air slows a feather down quickly; this is how quickly. */
private const val DRAG = 2.2f

/** Gravity in dp per second squared, and the share of it a light feather feels. */
private const val GRAVITY_DP = 620f
private const val FEATHER_FALL = 0.22f

/**
 * [count] feathers thrown out in every direction, no two alike: the full circle is cut into
 * [count] slices and each feather takes a random angle inside its own slice, so the burst is
 * even but never the same twice. Each has its own speed, spin, sway, size and color. The same
 * [seed] gives the same burst, which keeps the motion testable.
 */
internal fun confettiPieces(count: Int = 28, colors: Int = 6, seed: Int = 0): List<ConfettiPiece> {
    val random = Random(seed)
    val slice = 360f / count
    val angles = List(count) { index -> -180f + slice * (index + random.nextFloat() * 0.9f) }.shuffled(random)
    return List(count) { index ->
        ConfettiPiece(
            angleDegrees = angles[index],
            speedDp = 260f + 320f * random.nextFloat(),
            spinDegreesPerSecond = (if (random.nextBoolean()) 1f else -1f) * (140f + 220f * random.nextFloat()),
            swayDp = 6f + 10f * random.nextFloat(),
            swayHz = 1.2f + 1.2f * random.nextFloat(),
            sizeDp = 13f + 8f * random.nextFloat(),
            colorIndex = index % colors,
            life = 0.75f + 0.25f * random.nextFloat(),
        )
    }
}

/**
 * Where a feather is [seconds] after the throw, in dp from where it was thrown: it flies out fast,
 * slows in the air, drifts from side to side and sinks.
 */
internal fun confettiOffset(piece: ConfettiPiece, seconds: Float): Pair<Float, Float> {
    val t = seconds.coerceAtLeast(0f)
    val radians = piece.angleDegrees * (PI / 180.0)
    val vx = (cos(radians) * piece.speedDp).toFloat()
    val vy = (sin(radians) * piece.speedDp).toFloat()
    val travel = (1f - exp(-DRAG * t)) / DRAG
    val sway = piece.swayDp * sin(2.0 * PI * piece.swayHz * t).toFloat() * (1f - exp(-3f * t))
    val x = vx * travel + sway
    val y = vy * travel + 0.5f * GRAVITY_DP * FEATHER_FALL * t * t
    return x to y
}

/** Full strength for the first 70 percent of a feather's life, then a fade to nothing. */
internal fun confettiAlpha(piece: ConfettiPiece, progress: Float): Float {
    val p = progress.coerceIn(0f, 1f)
    return when {
        p >= piece.life -> 0f
        p < piece.life * 0.7f -> 1f
        else -> ((piece.life - p) / (piece.life * 0.3f)).coerceIn(0f, 1f)
    }
}

/**
 * A burst of confetti: its number (a new number starts a new burst), where it starts in window
 * pixels, how big it is (1 for a task, a smaller one for a subtask), how many feathers there are,
 * and whether they are the warm ones of the once-a-day "all done" moment.
 */
class ConfettiBurst(
    val id: Int,
    val origin: Offset,
    val scale: Float = 1f,
    val count: Int = 28,
    val warm: Boolean = false,
)

/**
 * One leaf-feather flying from [from] to [to] (window pixels) along a curve: where a repeating
 * task goes when it is finished and rests until its next day. A null [to] means the corner where
 * the menu is.
 */
class ConfettiFlight(val id: Int, val from: Offset, val to: Offset?)

/** How long the flight takes. */
internal const val FLIGHT_MILLIS = 360

/**
 * A point on the quadratic curve from [from] to [to] that bows up through [control], at [t] from 0
 * to 1. The flight is drawn along it, and the test checks its ends.
 */
internal fun quadraticPoint(from: Offset, control: Offset, to: Offset, t: Float): Offset {
    val u = 1f - t
    return Offset(
        u * u * from.x + 2f * u * t * control.x + t * t * to.x,
        u * u * from.y + 2f * u * t * control.y + t * t * to.y,
    )
}

/**
 * Starts the feather confetti for one screen. Every checkbox on the screen that gets finished calls
 * [celebrate] with where it is; nothing happens when Little celebrations is off or motion is reduced.
 */
@Stable
class ConfettiController internal constructor() {
    var burst by mutableStateOf<ConfettiBurst?>(null)
        private set
    var flight by mutableStateOf<ConfettiFlight?>(null)
        private set
    internal var reduceMotion: Boolean = false
    private var count = 0

    fun celebrate(origin: Offset, scale: Float = 1f, count: Int = 28, warm: Boolean = false) {
        if (!DisplayPrefs.celebrations || reduceMotion) return
        burst = ConfettiBurst(++this.count, origin, scale, count, warm)
    }

    /** A leaf-feather flies from [from] to [to]; with reduced motion nothing flies. */
    fun fly(from: Offset, to: Offset?) {
        if (!DisplayPrefs.celebrations || reduceMotion) return
        flight = ConfettiFlight(++count, from, to)
    }
}

@Composable
fun rememberConfettiController(): ConfettiController {
    val controller = remember { ConfettiController() }
    controller.reduceMotion = rememberReduceMotion()
    return controller
}

/** The overlay for a screen's [controller]; put it last, over everything else. */
@Composable
fun FeatherConfettiOverlay(controller: ConfettiController, modifier: Modifier = Modifier) {
    FeatherConfettiOverlay(controller.burst, modifier, controller.flight)
}

/**
 * Feather confetti over the whole screen: small leaf-feathers thrown in random directions from
 * [burst]'s origin that flutter, spin, fall and fade over 1.8 seconds, and a single feather
 * flying along [flight]. It draws above everything, takes no touches, and keeps playing after the
 * row it started from has left the list.
 */
@Composable
fun FeatherConfettiOverlay(burst: ConfettiBurst?, modifier: Modifier = Modifier, flight: ConfettiFlight? = null) {
    // A new burst throws its feathers in new directions.
    val pieces = remember(burst?.id, burst?.count) { confettiPieces(count = burst?.count ?: 28, seed = burst?.id ?: 0) }
    val feather = MaterialTheme.featherColors
    val colors = remember(feather) {
        listOf(Nest.SkyWing, Nest.Amber, Nest.Coral, feather.listColors[3], Nest.Cream, Nest.Plumage)
    }
    val warmColors = remember { listOf(Nest.Amber, Nest.Coral, Nest.Cream, Nest.Amber, Nest.Coral, Nest.Cream) }
    val leaf = remember { leafShape() }
    val progress = remember { Animatable(0f) }
    val flying = remember { Animatable(0f) }
    var topLeft by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(burst?.id) {
        if (burst != null) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween((CONFETTI_SECONDS * 1000).toInt(), easing = LinearEasing))
            progress.snapTo(0f)
        }
    }
    LaunchedEffect(flight?.id) {
        if (flight != null) {
            flying.snapTo(0f)
            flying.animateTo(1f, tween(FLIGHT_MILLIS, easing = Ease.Soft))
            flying.snapTo(0f)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { topLeft = it.positionInRoot() },
    ) {
        val current = burst
        val p = progress.value
        if (current != null && p > 0f && p < 1f) {
            val seconds = p * CONFETTI_SECONDS
            val palette = if (current.warm) warmColors else colors
            for (piece in pieces) {
                val alpha = confettiAlpha(piece, p)
                if (alpha <= 0f) continue
                val (dx, dy) = confettiOffset(piece, seconds)
                val cx = current.origin.x - topLeft.x + dx.dp.toPx() * current.scale
                val cy = current.origin.y - topLeft.y + dy.dp.toPx() * current.scale
                val length = piece.sizeDp.dp.toPx() * (0.5f + 0.5f * current.scale)
                drawFeather(leaf, Offset(cx, cy), length, piece.angleDegrees + 90f + piece.spinDegreesPerSecond * seconds, palette[piece.colorIndex % palette.size].copy(alpha = alpha))
            }
        }

        val trip = flight
        val t = flying.value
        if (trip != null && t > 0f && t < 1f) {
            val start = Offset(trip.from.x - topLeft.x, trip.from.y - topLeft.y)
            // Without a target the feather heads for the menu corner, where Upcoming lives.
            val end = trip.to?.let { Offset(it.x - topLeft.x, it.y - topLeft.y) } ?: Offset(size.width * 0.12f, size.height * 0.06f)
            val control = Offset((start.x + end.x) / 2f, minOf(start.y, end.y) - 120.dp.toPx())
            val at = quadraticPoint(start, control, end, t)
            val ahead = quadraticPoint(start, control, end, (t + 0.02f).coerceAtMost(1f))
            val heading = Math.toDegrees(Math.atan2((ahead.y - at.y).toDouble(), (ahead.x - at.x).toDouble())).toFloat()
            val fade = if (t > 0.8f) (1f - t) / 0.2f else 1f
            drawFeather(leaf, at, 26.dp.toPx() * (1f - 0.35f * t), heading + 45f + 200f * t, Nest.SkyWing.copy(alpha = fade))
        }
    }
}

/** One leaf-feather of [length] centered on [center], turned [degrees], with its pale spine. */
private fun DrawScope.drawFeather(leaf: Path, center: Offset, length: Float, degrees: Float, color: Color) {
    withTransform({
        translate(left = center.x, top = center.y)
        rotate(degrees, pivot = Offset.Zero)
        scale(length, length, pivot = Offset.Zero)
        translate(left = -0.51f, top = -0.49f)
    }) {
        drawPath(leaf, color)
        drawLine(
            color = Nest.Cream.copy(alpha = color.alpha * 0.6f),
            start = Offset(0.12f, 0.88f),
            end = Offset(0.9f, 0.1f),
            strokeWidth = 0.05f,
            cap = StrokeCap.Round,
        )
    }
}
