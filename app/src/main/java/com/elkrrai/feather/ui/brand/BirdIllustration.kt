package com.elkrrai.feather.ui.brand

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.ui.DisplayPrefs
import com.elkrrai.feather.ui.rememberLowEndMode
import com.elkrrai.feather.ui.rememberReduceMotion
import kotlin.math.min

/**
 * Whether the bird may move: not when the person turned bird animations off in Settings, not
 * when motion is reduced (the phone's animation scale is zero, or Reduce motion is on), and not on a
 * low-memory phone or in battery saver.
 */
@Composable
fun rememberBirdMotionAllowed(): Boolean = DisplayPrefs.birdAnimations && !rememberReduceMotion() && !rememberLowEndMode()

/**
 * A phase that runs from 0 to 1 over [periodMillis], again and again, for as long as the screen is
 * shown and [enabled]; otherwise it stays at 0, the calm pose. Read its value while drawing so a
 * running loop redraws without recomposing.
 */
@Composable
internal fun rememberAmbientPhase(periodMillis: Int, enabled: Boolean): Animatable<Float, AnimationVector1D> {
    val phase = remember { Animatable(0f) }
    LaunchedEffect(periodMillis, enabled) {
        phase.snapTo(0f)
        if (!enabled) return@LaunchedEffect
        while (true) {
            phase.snapTo(0f)
            phase.animateTo(1f, tween(durationMillis = periodMillis, easing = LinearEasing))
        }
    }
    return phase
}

/**
 * The character every screen asks for, in a [mood]. It is the owl ([OwlIllustration]); this is the
 * one door to it, so replacing the art or the animation updates every call site at once.
 *
 * [flightTrigger] counts up each time the owl should fly a lap over its perch and land again (see
 * [birdFlightPose]). Nothing happens when movement is not allowed or Little celebrations is off.
 */
@Composable
fun BirdIllustration(
    modifier: Modifier = Modifier.size(120.dp),
    mood: BirdMood = BirdMood.IDLE,
    contentDescription: String? = null,
    flightTrigger: Int = 0,
    dress: OwlDress = OwlDress.None,
) = OwlIllustration(modifier, mood, contentDescription, flightTrigger, dress)

/** A short hop of the owl over the screen each time [trigger] changes (a task was finished). */
@Composable
fun CompletionBird(trigger: Int, modifier: Modifier = Modifier) {
    if (trigger == 0 || !DisplayPrefs.celebrations || !rememberBirdMotionAllowed()) return
    val progress = remember { Animatable(0f) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(trigger) {
        visible = true
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = HOP_MILLIS, easing = LinearEasing))
        visible = false
    }
    if (visible) {
        OwlFlight(
            pose = { birdPose(BirdMood.HAPPY, progress.value) },
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

internal fun periodMillis(mood: BirdMood): Int = when (mood) {
    BirdMood.IDLE -> 8000
    BirdMood.SLEEPING -> 3600
    BirdMood.HAPPY -> 1100
    BirdMood.FLYING -> 700
}

private const val HOP_MILLIS = 1100
private const val FADE_IN = 0.10f
private const val FADE_OUT = 0.25f
