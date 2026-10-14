package com.elkrrai.feather.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.ui.rememberReduceMotion
import com.elkrrai.feather.ui.theme.Dur
import com.elkrrai.feather.ui.theme.Ease
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

/**
 * The small round control of a subtask: a 40dp target around a 20dp ring that fills with the
 * primary color and shows a check when it is done. A quieter sibling of [CompletionRing].
 */
@Composable
fun MiniRing(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    /** Called with the ring's middle (window pixels) when it is ticked, to start the confetti. */
    onCelebrate: (Offset) -> Unit = {},
) {
    val reduceMotion = rememberReduceMotion()
    var centerInRoot by remember { mutableStateOf(Offset.Zero) }
    val fill by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = if (reduceMotion) snap() else tween(Dur.Short2, easing = Ease.Soft),
        label = "miniRingFill",
    )
    val scheme = MaterialTheme.colorScheme
    val ring = scheme.outline
    val primary = scheme.primary
    val onPrimary = scheme.onPrimary
    val check = remember {
        Path().apply {
            moveTo(6.2f, 11.2f)
            lineTo(9.2f, 14.2f)
            lineTo(14.6f, 7.4f)
        }
    }

    Box(
        modifier = modifier
            .size(40.dp)
            .onGloballyPositioned { centerInRoot = it.positionInRoot() + Offset(it.size.width / 2f, it.size.height / 2f) }
            .toggleable(
                value = checked,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 20.dp),
                role = Role.Checkbox,
                onValueChange = { value ->
                    if (value) onCelebrate(centerInRoot)
                    onCheckedChange(value)
                },
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        // Drawn on a 20 by 20 board, whatever size it ends up at.
        Canvas(Modifier.size(20.dp)) {
            scale(size.width / 20f, pivot = Offset.Zero) {
                val center = Offset(10f, 10f)
                if (fill > 0f) drawCircle(primary, radius = 9f * fill + 1f, center = center)
                drawCircle(lerp(ring, primary, fill), radius = 9f, center = center, style = Stroke(width = 1.8f))
                if (fill > 0f) {
                    drawPath(
                        path = check,
                        color = onPrimary.copy(alpha = fill),
                        style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
            }
        }
    }
}
