package com.elkrrai.feather.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.ui.DisplayPrefs
import com.elkrrai.feather.ui.rememberReduceMotion
import com.elkrrai.feather.ui.theme.Dur
import com.elkrrai.feather.ui.theme.Ease
import com.elkrrai.feather.ui.theme.Nest
import com.elkrrai.feather.ui.theme.featherColors
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The ring and the check are drawn in a 26 by 26 box, whatever size they end up at. */
private const val RING_BOX = 26f
private const val RING_RADIUS = 12f
private const val RING_STROKE = 2f
private const val CHECK_STROKE = 2.4f
private const val BARB_STROKE = 1.8f

/** The check mark as a stroke: a short stroke down, a long one up. */
private val CheckPoints = listOf(Offset(7.4f, 13.6f), Offset(11.4f, 17.6f), Offset(18.8f, 9.4f))

/** Pressed in, then popped out and settled: the scale the ring passes through on its way to filled. */
private const val SQUEEZE_SCALE = 0.9f
private const val POP_SCALE = 1.12f

/** The feathers leave 120 ms after the tap and take 580 ms, so the whole burst ends 700 ms in. */
private const val BURST_DELAY = 120L
private const val BURST_MILLIS = 580
private const val BURST_FEATHERS = 7

/**
 * The round control that finishes a task. 48dp to touch, a 26dp ring to look at, in [ringColor]
 * (the task's priority). Finishing squeezes the ring, fills it with the primary color, pops it and
 * draws the check as a stroke, and a few leaf-feathers fly out (when Little celebrations is on);
 * undoing erases the check. With reduced motion the ring is filled at once, the check fades in over
 * 90ms and a soft halo replaces the feathers.
 *
 * [contentDescription] should be the task's title, so screen readers announce which task the
 * control belongs to.
 */
@Composable
fun CompletionRing(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    ringColor: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
    /** Priority strokes inside the ring: 0 for none up to 3 (see [barbCount]). */
    barbs: Int = 0,
) {
    val reduceMotion = rememberReduceMotion()
    val fill = remember { Animatable(if (checked) 1f else 0f) }
    val check = remember { Animatable(if (checked) 1f else 0f) }
    val pressScale = remember { Animatable(1f) }
    val burst = remember { Animatable(0f) }
    var burstRuns by remember { mutableIntStateOf(0) }
    val colors = MaterialTheme.featherColors
    val burstColors = remember(colors) { listOf(Nest.SkyWing, colors.amberFill, colors.coralFill, colors.listColors[3]) }
    val particles = remember { burstParticles(BURST_FEATHERS, colors = 4) }
    val ticks = remember(barbs) { barbTicks(barbs) }

    LaunchedEffect(checked, reduceMotion) {
        val target = if (checked) 1f else 0f
        if (fill.value == target && check.value == target) return@LaunchedEffect
        if (reduceMotion) {
            pressScale.snapTo(1f)
            fill.snapTo(target)
            check.animateTo(target, tween(Dur.Micro))
            return@LaunchedEffect
        }
        if (checked) {
            pressScale.animateTo(SQUEEZE_SCALE, tween(Dur.Micro, easing = Ease.Leave))
            coroutineScope {
                launch { fill.animateTo(1f, tween(Dur.Micro)) }
                launch { check.animateTo(1f, tween(Dur.Short2, easing = Ease.Draw)) }
                launch {
                    pressScale.animateTo(POP_SCALE, tween(70, easing = Ease.Settle))
                    pressScale.animateTo(1f, Ease.pop())
                }
            }
        } else {
            coroutineScope {
                launch { pressScale.animateTo(1f, tween(Dur.Micro)) }
                launch { check.animateTo(0f, tween(Dur.Medium1, easing = Ease.Settle)) }
                launch { fill.animateTo(0f, tween(Dur.Short2, easing = Ease.Soft)) }
            }
        }
    }

    // Each tap that finishes the task starts one burst (nothing happens when a finished row first appears).
    LaunchedEffect(burstRuns) {
        if (burstRuns == 0 || !DisplayPrefs.celebrations) return@LaunchedEffect
        burst.snapTo(0f)
        if (reduceMotion) {
            burst.animateTo(1f, tween(BURST_DELAY.toInt()))
        } else {
            delay(BURST_DELAY)
            burst.animateTo(1f, tween(BURST_MILLIS, easing = Ease.Settle))
        }
        burst.snapTo(0f)
    }

    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val checkPath = remember { Path() }
    val checkMeasure = remember { PathMeasure() }
    val visible = remember { Path() }
    remember {
        checkPath.moveTo(CheckPoints[0].x, CheckPoints[0].y)
        checkPath.lineTo(CheckPoints[1].x, CheckPoints[1].y)
        checkPath.lineTo(CheckPoints[2].x, CheckPoints[2].y)
        checkMeasure.setPath(checkPath, false)
    }

    Box(
        modifier = modifier
            .size(48.dp)
            .then(
                if (reduceMotion) {
                    Modifier.halo({ burst.value }, primary)
                } else {
                    Modifier.featherBurst({ burst.value }, particles, burstColors)
                }
            )
            .toggleable(
                value = checked,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 24.dp),
                role = Role.Checkbox,
                onValueChange = { value ->
                    if (value) burstRuns++
                    onCheckedChange(value)
                },
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .size(26.dp)
                .graphicsLayer {
                    scaleX = pressScale.value
                    scaleY = pressScale.value
                },
        ) {
            val unit = size.width / RING_BOX
            scale(unit, pivot = Offset.Zero) {
                val center = Offset(RING_BOX / 2f, RING_BOX / 2f)
                val filled = fill.value
                if (filled > 0f) {
                    drawCircle(primary, radius = RING_RADIUS * filled + RING_STROKE / 2f, center = center)
                }
                drawCircle(
                    color = lerp(ringColor, primary, filled),
                    radius = RING_RADIUS,
                    center = center,
                    style = Stroke(width = RING_STROKE),
                )
                // The priority strokes fade out as the ring fills.
                if (ticks.isNotEmpty() && filled < 1f) {
                    for ((from, to) in ticks) {
                        drawLine(
                            color = ringColor.copy(alpha = 1f - filled),
                            start = from,
                            end = to,
                            strokeWidth = BARB_STROKE,
                            cap = StrokeCap.Round,
                        )
                    }
                }
                val drawn = check.value
                if (drawn > 0f) {
                    visible.rewind()
                    checkMeasure.getSegment(0f, checkMeasure.length * drawn, visible, true)
                    drawPath(
                        path = visible,
                        color = onPrimary,
                        style = Stroke(width = CHECK_STROKE, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
            }
        }
    }
}
