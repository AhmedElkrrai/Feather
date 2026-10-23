package com.elkrrai.feather.ui.brand

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.elkrrai.feather.R
import kotlinx.coroutines.delay

/**
 * The layered owl supplied as separate Recraft SVG assets. All layers share the same 1024 square
 * artboard, so they can be stacked without per-layer positioning. The complete preen pose is used
 * as a matching key pose while the resting pose keeps its separate head, wing, and eye layers.
 */
@Composable
fun RecraftOwlIllustration(
    modifier: Modifier = Modifier,
) {
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

    LaunchedEffect(motionAllowed) {
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
            eyesOpen = false
            preenPose.animateTo(1f, tween(PREEN_TRANSITION_MILLIS, easing = FastOutSlowInEasing))
            delay(PREEN_HOLD_MILLIS)
            preenPose.animateTo(0f, tween(PREEN_TRANSITION_MILLIS, easing = LinearOutSlowInEasing))
            eyesOpen = true
            delay(REST_AFTER_PREEN_MILLIS)
        }
    }

    Box(modifier.clearAndSetSemantics { }) {
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
                }
                Canvas(Modifier.fillMaxSize()) {
                    val center = Offset(size.width * CHEST_GLOW_X, size.height * CHEST_GLOW_Y)
                    val glowRadius = size.minDimension * (
                        CHEST_GLOW_RADIUS + CHEST_GLOW_BREATH_RADIUS * breathing.value
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xE050E5FF),
                                Color(0xA050E5FF),
                                Color.Transparent,
                            ),
                            center = center,
                            radius = glowRadius,
                        ),
                        center = center,
                        radius = glowRadius,
                        alpha = CHEST_GLOW_BASE_ALPHA + CHEST_GLOW_BREATH_ALPHA * breathing.value,
                    )
                    val halfWidth = size.minDimension * CHEST_EMBLEM_HALF_WIDTH
                    val halfHeight = size.minDimension * CHEST_EMBLEM_HALF_HEIGHT
                    val emblem = Path().apply {
                        moveTo(center.x, center.y - halfHeight)
                        lineTo(center.x + halfWidth, center.y)
                        lineTo(center.x, center.y + halfHeight)
                        lineTo(center.x - halfWidth, center.y)
                        close()
                    }
                    drawPath(
                        path = emblem,
                        color = Color(0xFF70F1FF),
                        style = Stroke(width = size.minDimension * CHEST_EMBLEM_STROKE),
                    )
                    drawCircle(
                        color = Color(0xFFB8FBFF),
                        radius = size.minDimension * CHEST_EMBLEM_CORE_RADIUS,
                        center = center,
                    )
                }
            }
            OwlLayer(R.drawable.owl_preen, Modifier.graphicsLayer { alpha = preenPose.value })
        }
    }
}

@Composable
private fun OwlLayer(drawable: Int, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(drawable),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = modifier.fillMaxSize(),
    )
}

private const val BREATH_HALF_CYCLE_MILLIS = 2_100
private const val BREATH_SCALE_X = 0.004f
private const val BREATH_SCALE_Y = 0.009f
private const val BODY_PIVOT_X = 0.5f
private const val BODY_PIVOT_Y = 0.86f
private const val CHEST_GLOW_X = 0.665f
private const val CHEST_GLOW_Y = 0.55f
private const val CHEST_GLOW_RADIUS = 0.12f
private const val CHEST_GLOW_BREATH_RADIUS = 0.02f
private const val CHEST_GLOW_BASE_ALPHA = 0.62f
private const val CHEST_GLOW_BREATH_ALPHA = 0.38f
private const val CHEST_EMBLEM_HALF_WIDTH = 0.027f
private const val CHEST_EMBLEM_HALF_HEIGHT = 0.043f
private const val CHEST_EMBLEM_STROKE = 0.008f
private const val CHEST_EMBLEM_CORE_RADIUS = 0.009f
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
