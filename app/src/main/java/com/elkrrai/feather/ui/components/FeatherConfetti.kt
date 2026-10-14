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
import kotlin.math.sin
import androidx.compose.runtime.Stable
import com.elkrrai.feather.ui.DisplayPrefs
import com.elkrrai.feather.ui.rememberReduceMotion

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
 * [count] feathers thrown up and out in a fan, no two alike: spread over the upper half-circle,
 * each with its own speed, spin, sway, size and color.
 */
internal fun confettiPieces(count: Int = 28, colors: Int = 6): List<ConfettiPiece> =
    List(count) { index ->
        val a = (index * 0.6180339f) % 1f
        val b = (index * 0.381966f + 0.17f) % 1f
        ConfettiPiece(
            angleDegrees = -165f + 150f * a,
            speedDp = 300f + 280f * b,
            spinDegreesPerSecond = (if (index % 2 == 0) 1f else -1f) * (140f + 220f * a),
            swayDp = 6f + 10f * b,
            swayHz = 1.2f + 1.2f * a,
            sizeDp = 13f + 8f * b,
            colorIndex = index % colors,
            life = 0.75f + 0.25f * ((index * 0.7f) % 1f),
        )
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

/** Below this share of the screen's height a burst is thrown up; above it, down, so it stays on screen. */
internal const val THROW_DOWN_ABOVE_FRACTION = 0.4f

/** The same feathers thrown the other way: a mirror image of the fan across the horizontal. */
internal fun mirrored(pieces: List<ConfettiPiece>): List<ConfettiPiece> =
    pieces.map { it.copy(angleDegrees = -it.angleDegrees) }

/**
 * A burst of confetti: its number (a new number starts a new burst), where it starts in window
 * pixels, and how big it is: 1 for a task, a smaller one for a subtask.
 */
class ConfettiBurst(val id: Int, val origin: Offset, val scale: Float = 1f)

/**
 * Starts the feather confetti for one screen. Every checkbox on the screen that gets finished calls
 * [celebrate] with where it is; nothing happens when Little celebrations is off or motion is reduced.
 */
@Stable
class ConfettiController internal constructor() {
    var burst by mutableStateOf<ConfettiBurst?>(null)
        private set
    internal var reduceMotion: Boolean = false
    private var count = 0

    fun celebrate(origin: Offset, scale: Float = 1f) {
        if (!DisplayPrefs.celebrations || reduceMotion) return
        burst = ConfettiBurst(++count, origin, scale)
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
    FeatherConfettiOverlay(controller.burst, modifier)
}

/**
 * Feather confetti over the whole screen: about 28 small leaf-feathers thrown up from [burst]'s
 * origin that flutter, spin, fall and fade over 1.8 seconds. It draws above everything, takes no
 * touches, and keeps playing after the row it started from has left the list.
 */
@Composable
fun FeatherConfettiOverlay(burst: ConfettiBurst?, modifier: Modifier = Modifier) {
    val upward = remember { confettiPieces() }
    val downward = remember { mirrored(upward) }
    val feather = MaterialTheme.featherColors
    val colors = remember(feather) {
        listOf(Nest.SkyWing, Nest.Amber, Nest.Coral, feather.listColors[3], Nest.Cream, Nest.Plumage)
    }
    val leaf = remember { leafShape() }
    val progress = remember { Animatable(0f) }
    var topLeft by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(burst?.id) {
        if (burst != null) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween((CONFETTI_SECONDS * 1000).toInt(), easing = LinearEasing))
            progress.snapTo(0f)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { topLeft = it.positionInRoot() },
    ) {
        val current = burst ?: return@Canvas
        val p = progress.value
        if (p <= 0f || p >= 1f) return@Canvas
        val seconds = p * CONFETTI_SECONDS
        // Near the top of the screen there is no room above: spray down instead of up.
        val fromTop = (current.origin.y - topLeft.y) < size.height * THROW_DOWN_ABOVE_FRACTION
        for (piece in if (fromTop) downward else upward) {
            val alpha = confettiAlpha(piece, p)
            if (alpha <= 0f) continue
            val (dx, dy) = confettiOffset(piece, seconds)
            val cx = current.origin.x - topLeft.x + dx.dp.toPx() * current.scale
            val cy = current.origin.y - topLeft.y + dy.dp.toPx() * current.scale
            val length = piece.sizeDp.dp.toPx() * (0.5f + 0.5f * current.scale)
            val color = colors[piece.colorIndex % colors.size].copy(alpha = alpha)
            withTransform({
                translate(left = cx, top = cy)
                rotate(piece.angleDegrees + 90f + piece.spinDegreesPerSecond * seconds, pivot = Offset.Zero)
                scale(length, length, pivot = Offset.Zero)
                translate(left = -0.51f, top = -0.49f)
            }) {
                drawPath(leaf, color)
                drawLine(
                    color = Nest.Cream.copy(alpha = alpha * 0.6f),
                    start = Offset(0.12f, 0.88f),
                    end = Offset(0.9f, 0.1f),
                    strokeWidth = 0.05f,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}
