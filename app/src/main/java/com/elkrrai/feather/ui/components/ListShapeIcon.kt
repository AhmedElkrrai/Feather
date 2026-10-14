package com.elkrrai.feather.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.ui.theme.ListShape
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** A leaf pointed at both ends, in a box from 0 to 1. Shared with the subtask leaves. */
internal fun leafShape(): Path = Path().apply {
    moveTo(0.12f, 0.88f)
    cubicTo(0.10f, 0.40f, 0.50f, 0.10f, 0.90f, 0.10f)
    cubicTo(0.92f, 0.55f, 0.60f, 0.90f, 0.12f, 0.88f)
    close()
}

private fun dropShape(): Path = Path().apply {
    moveTo(0.50f, 0.06f)
    cubicTo(0.62f, 0.28f, 0.86f, 0.46f, 0.86f, 0.66f)
    cubicTo(0.86f, 0.84f, 0.70f, 0.94f, 0.50f, 0.94f)
    cubicTo(0.30f, 0.94f, 0.14f, 0.84f, 0.14f, 0.66f)
    cubicTo(0.14f, 0.46f, 0.38f, 0.28f, 0.50f, 0.06f)
    close()
}

private fun diamondShape(): Path = Path().apply {
    moveTo(0.50f, 0.06f)
    lineTo(0.94f, 0.50f)
    lineTo(0.50f, 0.94f)
    lineTo(0.06f, 0.50f)
    close()
}

private fun triangleShape(): Path = Path().apply {
    moveTo(0.50f, 0.10f)
    lineTo(0.92f, 0.86f)
    lineTo(0.08f, 0.86f)
    close()
}

internal fun moonShape(): Path = Path().apply {
    moveTo(0.62f, 0.08f)
    cubicTo(0.28f, 0.12f, 0.10f, 0.40f, 0.18f, 0.68f)
    cubicTo(0.28f, 0.90f, 0.58f, 0.98f, 0.84f, 0.80f)
    cubicTo(0.50f, 0.82f, 0.38f, 0.48f, 0.62f, 0.08f)
    close()
}

private fun pathFor(shape: ListShape): Path? = when (shape) {
    ListShape.Drop -> dropShape()
    ListShape.Leaf -> leafShape()
    ListShape.Diamond -> diamondShape()
    ListShape.Triangle -> triangleShape()
    ListShape.Moon -> moonShape()
    ListShape.Circle, ListShape.Sun, ListShape.Square -> null
}

/**
 * The small shape that goes with a list color (circle, sun, drop, leaf, diamond, square, triangle
 * or moon), so a list is never told apart by color alone. Decoration: the list's name is next to
 * it, so it has no description of its own.
 */
@Composable
fun ListShapeIcon(shape: ListShape, color: Color, modifier: Modifier = Modifier, size: Dp = 12.dp) {
    val path = remember(shape) { pathFor(shape) }
    Canvas(modifier = modifier.size(size).clearAndSetSemantics { }) {
        scale(this.size.width, pivot = Offset.Zero) { drawListShape(shape, path, color) }
    }
}

/** Draws [shape] in a box from 0 to 1 (the caller scales the drawing to the real size). */
private fun DrawScope.drawListShape(shape: ListShape, path: Path?, color: Color) {
    when {
        path != null -> drawPath(path, color)
        shape == ListShape.Square -> drawRoundRect(
            color = color,
            topLeft = Offset(0.14f, 0.14f),
            size = Size(0.72f, 0.72f),
            cornerRadius = CornerRadius(0.12f, 0.12f),
        )

        shape == ListShape.Sun -> {
            drawCircle(color, radius = 0.25f, center = Offset(0.5f, 0.5f))
            for (ray in 0 until 8) {
                val angle = ray * PI / 4
                drawLine(
                    color = color,
                    start = Offset(0.5f + (cos(angle) * 0.36).toFloat(), 0.5f + (sin(angle) * 0.36).toFloat()),
                    end = Offset(0.5f + (cos(angle) * 0.48).toFloat(), 0.5f + (sin(angle) * 0.48).toFloat()),
                    strokeWidth = 0.09f,
                    cap = StrokeCap.Round,
                )
            }
        }

        else -> drawCircle(color, radius = 0.42f, center = Offset(0.5f, 0.5f))
    }
}

/** An outline version of a leaf, for the subtasks that are not done yet. */
internal fun DrawScope.drawLeaf(path: Path, color: Color, filled: Boolean) {
    if (filled) {
        drawPath(path, color)
    } else {
        drawPath(path, color, style = Stroke(width = 0.1f))
    }
}
