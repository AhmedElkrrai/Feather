package com.elkrrai.feather.feature.habits

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.domain.habit.WeekProgress
import com.elkrrai.feather.ui.rememberLowEndMode
import com.elkrrai.feather.ui.rememberReduceMotion
import com.elkrrai.feather.ui.theme.featherColors
import com.elkrrai.feather.ui.theme.leafColor
import com.elkrrai.feather.ui.theme.rememberSeason
import java.time.LocalDate
import java.time.format.TextStyle
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

private val LabelHeight = 16.dp
private const val DAYS = 7

/** How wide the leaf is at [t] (0 at the stem, 1 at the tip), as a share of its widest part: pointed at the tip, round at the base. */
internal fun leafWidth(t: Float): Float = sin(PI.toFloat() * t.coerceIn(0f, 1f).pow(0.75f)).coerceAtLeast(0f).pow(0.9f)

/**
 * One week of a habit as a leaf that stands upright and fills the space it is given: the stem at the
 * bottom, the tip at the top, a midrib, and a vein branching off it to each of the seven days, one
 * side and then the other, climbing from the first day of the week to the last. A circle sits on each
 * vein with that day's number inside and the weekday beneath. A circle glows when the day counts; the
 * whole leaf glows when the week's goal was met. The glow breathes slowly, and stands still when
 * motion is reduced.
 */
@Composable
fun WeekLeaf(
    week: WeekProgress,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val season = rememberSeason()
    val leaf = MaterialTheme.featherColors.leafColor(season)
    val surface = MaterialTheme.colorScheme.surface
    val still = rememberReduceMotion() || rememberLowEndMode()

    // One slow breath shared by the leaf and the circles that glow.
    val transition = rememberInfiniteTransition(label = "leafBreath")
    val breathing by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath",
    )
    val breath = if (still) 0.5f else breathing

    BoxWithConstraints(modifier.fillMaxSize()) {
        val boxWidth = maxWidth
        val boxHeight = maxHeight
        val stem = 26.dp
        val topY = 10.dp
        val baseY = boxHeight - stem - LabelHeight
        val leafHeight = baseY - topY
        val halfMax = minOf(boxWidth * 0.46f, leafHeight * 0.36f)
        val centerX = boxWidth / 2
        val circle: Dp = minOf(46.dp, boxWidth * 0.12f)

        // The circle of each day: climbing the leaf, one side and then the other.
        class Spot(val x: Dp, val y: Dp, val veinFrom: Dp, val index: Int)
        val radius = circle / 2
        // how far from the midrib a circle can sit and still be wholly inside the leaf: the leaf is
        // narrower above and below the circle (and its label) than at its middle
        fun room(t: Float): Dp {
            val below = (t - (radius + LabelHeight) / leafHeight).coerceAtLeast(0f)
            val above = (t + radius / leafHeight).coerceAtMost(1f)
            return halfMax * minOf(leafWidth(below), leafWidth(above)) - radius - 8.dp
        }
        val spots = (0 until DAYS).map { index ->
            val t = 0.22f + 0.62f * (index / (DAYS - 1f))
            val y = baseY - leafHeight * t
            val side = if (index % 2 == 0) -1f else 1f
            // preferably well out on the vein, never past the edge, never on the midrib
            val reach = maxOf(minOf(halfMax * leafWidth(t) * 0.62f, room(t)), radius + 4.dp)
            Spot(centerX + reach * side, y, y + circle * 0.9f, index)
        }

        Canvas(Modifier.fillMaxSize()) {
            val cx = centerX.toPx()
            val top = topY.toPx()
            val base = baseY.toPx()
            val half = halfMax.toPx()
            val height = base - top

            // the outline, drawn up the right side and back down the left
            val outline = Path().apply {
                val steps = 64
                for (step in 0..steps) {
                    val t = step / steps.toFloat()
                    val point = Offset(cx + half * leafWidth(t), base - height * t)
                    if (step == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                }
                for (step in steps downTo 0) {
                    val t = step / steps.toFloat()
                    lineTo(cx - half * leafWidth(t), base - height * t)
                }
                close()
            }

            if (week.met) {
                // a soft light round the leaf, breathing
                for (layer in 1..5) {
                    drawPath(
                        outline,
                        leaf.copy(alpha = (0.05f + 0.10f * breath) * (1f - layer / 6f)),
                        style = Stroke(width = (6 + layer * 7).dp.toPx(), cap = StrokeCap.Round),
                    )
                }
            }
            drawPath(
                outline,
                Brush.verticalGradient(
                    listOf(leaf.copy(alpha = 0.10f), leaf.copy(alpha = if (week.met) 0.30f + 0.12f * breath else 0.20f)),
                    startY = top,
                    endY = base,
                ),
            )
            drawPath(outline, leaf.copy(alpha = 0.75f), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

            // the stem and the midrib
            drawLine(leaf.copy(alpha = 0.8f), Offset(cx, base), Offset(cx + 6.dp.toPx(), base + stem.toPx()), 3.dp.toPx(), StrokeCap.Round)
            drawLine(leaf.copy(alpha = 0.6f), Offset(cx, base), Offset(cx, top + height * 0.04f), 2.dp.toPx(), StrokeCap.Round)

            // a vein from the midrib up to each circle, and a little beyond it
            spots.forEach { spot ->
                val from = Offset(cx, spot.veinFrom.toPx())
                val to = Offset(spot.x.toPx(), spot.y.toPx())
                val path = Path().apply {
                    moveTo(from.x, from.y)
                    quadraticTo(from.x + (to.x - from.x) * 0.15f, to.y + (from.y - to.y) * 0.15f, to.x, to.y)
                }
                drawPath(path, leaf.copy(alpha = 0.55f), style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round))
            }
        }

        week.days.forEachIndexed { index, day ->
            val spot = spots[index]
            DayCircle(
                number = day.amount,
                label = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                done = day.done,
                dimmed = day.date.isAfter(today),
                size = circle,
                leaf = leaf,
                surface = surface,
                breath = breath,
                modifier = Modifier.offset(x = spot.x - circle, y = spot.y - circle / 2).width(circle * 2),
            )
        }
    }
}

/** A circle on a vein: the day's number inside, the weekday beneath, a glow when the day counts. */
@Composable
private fun DayCircle(
    number: Int,
    label: String,
    done: Boolean,
    dimmed: Boolean,
    size: Dp,
    leaf: Color,
    surface: Color,
    breath: Float,
    modifier: Modifier = Modifier,
) {
    val onLeaf = remember(leaf) { if (leaf.luminance() > 0.5f) Color.Black.copy(alpha = 0.8f) else Color.White }
    val description = if (number > 0) "$label, $number" else label
    Column(
        modifier = modifier
            .alpha(if (dimmed) 0.4f else 1f)
            .semantics(mergeDescendants = true) { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(size)
                .drawBehind {
                    // the glow reaches out past the circle
                    if (done) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(leaf.copy(alpha = 0.30f + 0.35f * breath), Color.Transparent),
                                radius = this.size.minDimension,
                            ),
                            radius = this.size.minDimension,
                        )
                    }
                }
                .background(if (done) leaf else surface, CircleShape)
                .border(BorderStroke(1.5.dp, if (done) leaf else leaf.copy(alpha = 0.55f)), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (number > 0) {
                Text(
                    text = number.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (done) onLeaf else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.height(LabelHeight),
        )
    }
}
