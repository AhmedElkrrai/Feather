package com.elkrrai.feather.feature.rhythm

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.domain.rhythm.WeeklyFeather
import com.elkrrai.feather.domain.rhythm.barbsFor
import com.elkrrai.feather.ui.theme.featherColors
import kotlin.math.PI
import kotlin.math.sin

/** The feather is drawn on a 100 by 160 board. */
private const val BOARD_W = 100f
private const val BOARD_H = 160f
private const val QUILL_X = 50f
private const val QUILL_TOP = 12f
private const val QUILL_BOTTOM = 150f

/** Half the width of the vane at [y] on the board: widest in the middle, pointed at both ends. */
internal fun vaneHalfWidth(y: Float): Float {
    val t = ((QUILL_BOTTOM - y) / (QUILL_BOTTOM - QUILL_TOP)).coerceIn(0f, 1f)
    return 34f * sin(PI * t).toFloat().coerceAtLeast(0f)
}

/**
 * The weekly feather: a quill with a vane in the color of the busiest list, seven stretches from the
 * bottom (the first day of the week) to the tip, each with a barb for every task finished that day
 * (up to four) and, for a day of rest, a small notch in the vane. Made on the phone from last week's
 * numbers, so every week's is different. Decoration; the words next to it carry the meaning.
 */
@Composable
internal fun WeeklyFeatherView(
    feather: WeeklyFeather,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val paper = MaterialTheme.featherColors.card
    Canvas(modifier.width(100.dp).height(160.dp).clearAndSetSemantics { }) {
        scale(size.width / BOARD_W, pivot = Offset.Zero) {
            val vane = Path().apply {
                moveTo(QUILL_X, QUILL_BOTTOM)
                cubicTo(QUILL_X - 44f, 112f, QUILL_X - 44f, 44f, QUILL_X, QUILL_TOP)
                cubicTo(QUILL_X + 44f, 44f, QUILL_X + 44f, 112f, QUILL_X, QUILL_BOTTOM)
                close()
            }
            drawPath(vane, color.copy(alpha = 0.16f))
            drawPath(vane, color.copy(alpha = 0.55f), style = Stroke(1.2f))
            // the quill, with its bare end below the vane
            drawLine(color, Offset(QUILL_X, QUILL_BOTTOM + 8f), Offset(QUILL_X, QUILL_TOP + 2f), 2.4f, StrokeCap.Round)

            val stretch = (QUILL_BOTTOM - 12f - (QUILL_TOP + 14f)) / 7f
            feather.days.forEachIndexed { index, count ->
                val centerY = QUILL_BOTTOM - 12f - (index + 0.5f) * stretch
                val barbs = barbsFor(count)
                if (barbs == 0) {
                    // a day of rest: a small notch bitten out of each edge of the vane
                    val half = vaneHalfWidth(centerY)
                    for (side in listOf(-1f, 1f)) {
                        val edge = QUILL_X + side * half
                        val notch = Path().apply {
                            moveTo(edge, centerY - 4.5f)
                            lineTo(edge - side * 7f, centerY)
                            lineTo(edge, centerY + 4.5f)
                            close()
                        }
                        drawPath(notch, paper)
                    }
                } else {
                    for (j in 0 until barbs) {
                        val y = centerY + (j - (barbs - 1) / 2f) * 4.6f
                        val reach = vaneHalfWidth(y) * 0.92f
                        for (side in listOf(-1f, 1f)) {
                            drawLine(
                                color = color,
                                start = Offset(QUILL_X, y + 3f),
                                end = Offset(QUILL_X + side * reach, y - 5f),
                                strokeWidth = 1.9f,
                                cap = StrokeCap.Round,
                            )
                        }
                    }
                }
            }
        }
    }
}
