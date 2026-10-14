package com.elkrrai.feather.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.ui.rememberReduceMotion
import com.elkrrai.feather.ui.theme.Ease
import com.elkrrai.feather.ui.theme.featherColors

/** How much of the bar is filled: the share of subtasks that are done, from 0 to 1. */
internal fun subtaskFraction(done: Int, total: Int): Float =
    if (total <= 0) 0f else (done.coerceIn(0, total).toFloat() / total)

private const val GLINT_MILLIS = 700

/**
 * The bar on a task that has subtasks: it fills as they are finished. The fill springs to its new
 * length with a little overshoot, a soft light sweeps along it each time it grows, and when every
 * subtask is done it turns green. With reduced motion it simply changes. Decoration: the text next
 * to it ("2 of 4") says the same.
 */
@Composable
fun SubtaskProgressBar(done: Int, total: Int, modifier: Modifier = Modifier) {
    val reduceMotion = rememberReduceMotion()
    val scheme = MaterialTheme.colorScheme
    val finished = MaterialTheme.featherColors.listColors[3]

    val target = subtaskFraction(done, total)
    val fraction by animateFloatAsState(
        targetValue = target,
        animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.62f, stiffness = 260f),
        label = "subtaskFill",
    )
    val complete = total > 0 && done >= total
    val fill by animateColorAsState(
        targetValue = if (complete) finished else scheme.primary,
        animationSpec = if (reduceMotion) snap() else tween(360),
        label = "subtaskFillColor",
    )

    // A light that sweeps along the fill each time one more subtask is done.
    val glint = remember { Animatable(1f) }
    var doneBefore by remember { mutableIntStateOf(done) }
    LaunchedEffect(done) {
        if (!reduceMotion && done > doneBefore) {
            glint.snapTo(0f)
            glint.animateTo(1f, tween(GLINT_MILLIS, easing = Ease.Settle))
        }
        doneBefore = done
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clearAndSetSemantics { },
    ) {
        val radius = CornerRadius(size.height / 2f, size.height / 2f)
        drawRoundRect(scheme.surfaceVariant, cornerRadius = radius)

        val shown = fraction.coerceIn(0f, 1f)
        if (shown <= 0f) return@Canvas
        // Never thinner than the bar is tall, so the first bit of progress is a round dot.
        val width = maxOf(size.width * shown, size.height)
        drawRoundRect(fill, size = Size(width, size.height), cornerRadius = radius)

        val sweep = glint.value
        if (sweep < 1f) {
            val band = size.height * 5f
            val x = width * sweep
            val clip = Path().apply { addRoundRect(RoundRect(0f, 0f, width, size.height, radius)) }
            clipPath(clip) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.5f), Color.Transparent),
                        startX = x - band,
                        endX = x + band,
                    ),
                    size = Size(width, size.height),
                )
            }
        }
    }
}
