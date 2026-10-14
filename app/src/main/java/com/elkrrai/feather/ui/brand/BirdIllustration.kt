package com.elkrrai.feather.ui.brand

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.ui.DisplayPrefs
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.min

/** The bird's shapes, parsed once from the path data. */
private class BirdPaths {
    val tail: Path = parse(BirdShapes.TAIL)
    val body: Path = parse(BirdShapes.BODY)
    val head: Path = parse(BirdShapes.HEAD)
    val belly: Path = parse(BirdShapes.BELLY)
    val wing: Path = parse(BirdShapes.WING)
    val beak: Path = parse(BirdShapes.BEAK)
    val crest: Path = parse(BirdShapes.CREST)

    private fun parse(data: String): Path = PathParser().parsePathString(data).toPath()
}

/**
 * Whether the bird may move: not when the person turned bird animations off in Settings, and not
 * when the system's animation scale is zero ("remove animations" / reduce motion).
 */
@Composable
fun rememberBirdMotionAllowed(): Boolean {
    val context = LocalContext.current
    val systemAnimationsOff = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    return DisplayPrefs.birdAnimations && !systemAnimationsOff
}

/**
 * The Feather bird. It moves on its own in a loop while movement is allowed
 * ([rememberBirdMotionAllowed]) and is a still picture otherwise. Pass a [contentDescription]
 * when the picture carries meaning; without one it is hidden from screen readers.
 */
@Composable
fun BirdIllustration(
    modifier: Modifier = Modifier.size(120.dp),
    mood: BirdMood = BirdMood.IDLE,
    contentDescription: String? = null,
) {
    val animatedPhase: State<Float>? = if (rememberBirdMotionAllowed()) {
        rememberInfiniteTransition(label = "bird").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = periodMillis(mood), easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "phase",
        )
    } else {
        null
    }
    BirdCanvas(
        mood = mood,
        // Read inside the drawing, so each frame redraws the picture and recomposes nothing.
        phase = { animatedPhase?.value ?: 0f },
        modifier = modifier.describedAs(contentDescription),
    )
}

/** A short hop of the bird over the screen each time [trigger] changes (a task was finished). */
@Composable
fun CompletionBird(trigger: Int, modifier: Modifier = Modifier) {
    if (trigger == 0 || !rememberBirdMotionAllowed()) return
    val progress = remember { Animatable(0f) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(trigger) {
        visible = true
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = HOP_MILLIS, easing = LinearEasing))
        visible = false
    }
    if (visible) {
        BirdCanvas(
            mood = BirdMood.HAPPY,
            phase = { progress.value },
            modifier = modifier
                .size(72.dp)
                .alpha(fade(progress.value))
                .clearAndSetSemantics { },
        )
    }
}

/** Fades in over the first tenth and out over the last quarter. */
internal fun fade(progress: Float): Float =
    min(1f, min(progress / FADE_IN, (1f - progress) / FADE_OUT)).coerceIn(0f, 1f)

@Composable
internal fun BirdCanvas(mood: BirdMood, phase: () -> Float, modifier: Modifier = Modifier) {
    val paths = remember { BirdPaths() }
    Canvas(modifier = modifier) {
        val unit = min(size.width, size.height) / BirdShapes.SIZE
        val left = (size.width - unit * BirdShapes.SIZE) / 2f
        val top = (size.height - unit * BirdShapes.SIZE) / 2f
        val currentPhase = phase()
        val pose = birdPose(mood, currentPhase)
        val bodyPivot = Offset(BirdShapes.BODY_PIVOT_X, BirdShapes.BODY_PIVOT_Y)

        withTransform({
            translate(left = left, top = top + pose.bodyOffsetY * unit)
            scale(unit, unit, pivot = Offset.Zero)
            rotate(pose.tilt, pivot = bodyPivot)
            scale(pose.bodyScale, pose.bodyScale, pivot = bodyPivot)
        }) {
            drawBird(paths, pose)
            if (mood == BirdMood.SLEEPING) drawSleepingZs(currentPhase)
        }
    }
}

private fun DrawScope.drawBird(paths: BirdPaths, pose: BirdPose) {
    for ((from, to) in BirdShapes.FEET) {
        drawLine(
            color = BirdColors.Amber,
            start = Offset(from.first, from.second),
            end = Offset(to.first, to.second),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round,
        )
    }
    drawPath(paths.tail, BirdColors.Deep)
    drawPath(paths.body, BirdColors.Plumage)
    drawPath(paths.head, BirdColors.Plumage)
    drawPath(paths.belly, BirdColors.Cream)
    withTransform({ rotate(pose.wingAngle, pivot = Offset(BirdShapes.WING_PIVOT_X, BirdShapes.WING_PIVOT_Y)) }) {
        drawPath(paths.wing, BirdColors.Wing)
    }
    drawPath(paths.beak, BirdColors.Amber)
    drawPath(paths.crest, BirdColors.Amber)
    drawCircle(
        color = BirdColors.Coral.copy(alpha = CHEEK_ALPHA),
        radius = BirdShapes.CHEEK_RADIUS,
        center = Offset(BirdShapes.CHEEK_X, BirdShapes.CHEEK_Y),
    )
    drawEye(pose.eyeOpen)
}

/** An open eye with a glint, a squint, or (nearly shut) a curved line. */
private fun DrawScope.drawEye(open: Float) {
    val radius = BirdShapes.EYE_RADIUS
    if (open < CLOSED_EYE) {
        drawLine(
            color = BirdColors.Ink,
            start = Offset(BirdShapes.EYE_X - radius, BirdShapes.EYE_Y),
            end = Offset(BirdShapes.EYE_X + radius, BirdShapes.EYE_Y),
            strokeWidth = 1.6f,
            cap = StrokeCap.Round,
        )
        return
    }
    drawOval(
        color = BirdColors.Ink,
        topLeft = Offset(BirdShapes.EYE_X - radius, BirdShapes.EYE_Y - radius * open),
        size = Size(radius * 2f, radius * 2f * open),
    )
    if (open > GLINT_MIN_OPEN) {
        drawCircle(BirdColors.Cream, radius = 1.1f, center = Offset(BirdShapes.EYE_X + 1f, BirdShapes.EYE_Y - 1f))
    }
}

/** Two "z" letters that float up and fade, offset by half a cycle. */
private fun DrawScope.drawSleepingZs(phase: Float) {
    for (index in 0..1) {
        val t = (phase + index * 0.5f) % 1f
        val x = 78f + 6f * t
        val y = 26f - 14f * t
        val size = 4f + 3f * t
        val color = BirdColors.Deep.copy(alpha = sin((PI * t).toFloat()))
        drawLine(color, Offset(x, y), Offset(x + size, y), strokeWidth = 1.4f, cap = StrokeCap.Round)
        drawLine(color, Offset(x + size, y), Offset(x, y + size), strokeWidth = 1.4f, cap = StrokeCap.Round)
        drawLine(color, Offset(x, y + size), Offset(x + size, y + size), strokeWidth = 1.4f, cap = StrokeCap.Round)
    }
}

private fun Modifier.describedAs(description: String?): Modifier =
    if (description == null) clearAndSetSemantics { } else semantics { contentDescription = description }

private fun periodMillis(mood: BirdMood): Int = when (mood) {
    BirdMood.IDLE -> 4000
    BirdMood.SLEEPING -> 3600
    BirdMood.HAPPY -> 1100
    BirdMood.FLYING -> 700
}

private const val HOP_MILLIS = 1100
private const val FADE_IN = 0.10f
private const val FADE_OUT = 0.25f
private const val CHEEK_ALPHA = 0.6f
private const val CLOSED_EYE = 0.15f
private const val GLINT_MIN_OPEN = 0.6f
