package com.elkrrai.feather.feature.rhythm

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.domain.rhythm.DayCount
import com.elkrrai.feather.domain.rhythm.TimeOfDay
import com.elkrrai.feather.ui.components.leafShape
import com.elkrrai.feather.ui.rememberReduceMotion
import com.elkrrai.feather.ui.theme.Ease
import com.elkrrai.feather.ui.theme.featherColors
import java.text.NumberFormat
import kotlin.math.max
import kotlin.math.min

// Every chart on the Your rhythm screen is one Canvas, and they all play from one clock: the
// entry. It runs once when the screen first appears, and each chart takes its own slice of it
// (the bars first, then the dots, the month, the lists), so the whole thing is over in 1.1 seconds.

/** The whole entrance, in milliseconds. */
internal const val ENTRY_MS = 1100f

/** One 0 to 1 clock for the entrance. With reduced motion it starts finished. */
@Composable
internal fun rememberEntry(): Animatable<Float, AnimationVector1D> {
    val reduce = rememberReduceMotion()
    val entry = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(reduce) {
        if (reduce) {
            entry.snapTo(1f)
        } else if (entry.value < 1f) {
            entry.animateTo(1f, tween(ENTRY_MS.toInt(), easing = LinearEasing))
        }
    }
    return entry
}

/** Where a part that starts [startMs] into the entrance and lasts [durMs] has got to, from 0 to 1. */
internal fun segment(entry: Float, startMs: Float, durMs: Float): Float =
    ((entry * ENTRY_MS - startMs) / durMs).coerceIn(0f, 1f)

/** A little overshoot at the end, so a dot pops into place. */
private val PopEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

private val BarsHeight = 120.dp

/**
 * The last seven days as bars, oldest first in the reading direction. A day with nothing is a
 * small curled leaf, not an empty bar; today's bar has a dashed outline. The tallest bar is
 * at least five finished tasks high, so a quiet week does not look like a big one.
 */
@Composable
internal fun WeekBars(
    days: List<DayCount>,
    entry: Animatable<Float, AnimationVector1D>,
    modifier: Modifier = Modifier,
) {
    val feather = MaterialTheme.featherColors
    val scheme = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val valueStyle = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum", color = scheme.onSurface)
    val leaf = remember { leafShape() }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val locale = LocalConfiguration.current.locales[0]
    val numbers = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    val maxCount = max(5, days.maxOfOrNull { it.count } ?: 0)

    Canvas(modifier.fillMaxWidth().height(BarsHeight).clearAndSetSemantics { }) {
        val slot = size.width / 7f
        val barWidth = min(28.dp.toPx(), slot - 6.dp.toPx())
        val maxHeight = 92.dp.toPx()
        val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
        days.forEachIndexed { index, day ->
            val column = if (rtl) days.lastIndex - index else index
            val centerX = slot * column + slot / 2f
            val grow = Ease.Settle.transform(segment(entry.value, index * 40f, 360f))
            val isToday = index == days.lastIndex
            if (day.count == 0) {
                val side = 16.dp.toPx()
                val top = size.height - side - 4.dp.toPx()
                withTransform({
                    translate(left = centerX - side / 2f, top = top)
                    scale(side, side, pivot = Offset.Zero)
                }) {
                    drawPath(leaf, feather.listColors[3].copy(alpha = 0.55f * grow))
                }
                if (isToday) {
                    drawRoundRect(
                        color = scheme.primary.copy(alpha = grow),
                        topLeft = Offset(centerX - barWidth / 2f - 3.dp.toPx(), top - 6.dp.toPx()),
                        size = Size(barWidth + 6.dp.toPx(), side + 12.dp.toPx()),
                        cornerRadius = CornerRadius(12.dp.toPx()),
                        style = Stroke(1.5.dp.toPx(), pathEffect = dash),
                    )
                }
            } else {
                val height = maxHeight * day.count / maxCount * grow
                val top = size.height - height
                val bar = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = centerX - barWidth / 2f, top = top, right = centerX + barWidth / 2f, bottom = size.height,
                            topLeftCornerRadius = CornerRadius(10.dp.toPx()),
                            topRightCornerRadius = CornerRadius(10.dp.toPx()),
                            bottomRightCornerRadius = CornerRadius(5.dp.toPx()),
                            bottomLeftCornerRadius = CornerRadius(5.dp.toPx()),
                        )
                    )
                }
                drawPath(bar, feather.amberFill)
                val value = measurer.measure(numbers.format(day.count), valueStyle)
                drawText(
                    value,
                    topLeft = Offset(centerX - value.size.width / 2f, top - value.size.height - 4.dp.toPx()),
                    alpha = grow,
                )
                if (isToday) {
                    drawRoundRect(
                        color = scheme.primary.copy(alpha = grow),
                        topLeft = Offset(centerX - barWidth / 2f - 3.dp.toPx(), top - 3.dp.toPx()),
                        size = Size(barWidth + 6.dp.toPx(), height + 6.dp.toPx()),
                        cornerRadius = CornerRadius(12.dp.toPx()),
                        style = Stroke(1.5.dp.toPx(), pathEffect = dash),
                    )
                }
            }
        }
    }
}

/** The weekday letters under the bars: seven equal slots, in the same order as the bars. */
@Composable
internal fun WeekdayLabels(labels: List<String>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().clearAndSetSemantics { }) {
        labels.forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private val TimeBarsHeight = 76.dp

/** How many tasks were finished in each part of the day, as four bars; the most is amber. */
@Composable
internal fun TimeOfDayBars(
    times: Map<TimeOfDay, Int>,
    best: TimeOfDay?,
    labels: List<String>,
    entry: Animatable<Float, AnimationVector1D>,
    modifier: Modifier = Modifier,
) {
    val feather = MaterialTheme.featherColors
    val scheme = MaterialTheme.colorScheme
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val most = max(1, times.values.maxOrNull() ?: 0)
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(TimeBarsHeight).clearAndSetSemantics { }) {
            val slot = size.width / 4f
            val barWidth = min(40.dp.toPx(), slot - 12.dp.toPx())
            TimeOfDay.entries.forEachIndexed { index, part ->
                val column = if (rtl) 3 - index else index
                val centerX = slot * column + slot / 2f
                val grow = Ease.Settle.transform(segment(entry.value, 400f + index * 50f, 360f))
                val count = times[part] ?: 0
                val height = max(6.dp.toPx(), (size.height - 4.dp.toPx()) * count / most) * grow
                drawRoundRect(
                    color = if (part == best) feather.amberFill else scheme.surfaceContainerHigh,
                    topLeft = Offset(centerX - barWidth / 2f, size.height - height),
                    size = Size(barWidth, height),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                )
            }
        }
        WeekdayLabels(labels)
    }
}

/** Seven small leaves for the last seven days: filled for a day the task was finished. */
@Composable
internal fun LeafDays(days: List<Boolean>, modifier: Modifier = Modifier, leafSize: Dp = 14.dp) {
    val feather = MaterialTheme.featherColors
    val leaf = remember { leafShape() }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val green = feather.listColors[3]
    Canvas(modifier.height(leafSize).fillMaxWidth().clearAndSetSemantics { }) {
        val side = leafSize.toPx()
        val gap = 4.dp.toPx()
        days.forEachIndexed { index, done ->
            // The first day sits at the start: the left in English, the right in Arabic.
            val left = if (rtl) size.width - (index + 1) * side - index * gap else index * (side + gap)
            withTransform({
                translate(left = left, top = 0f)
                scale(side, side, pivot = Offset.Zero)
            }) {
                if (done) {
                    drawPath(leaf, green)
                } else {
                    drawPath(leaf, green.copy(alpha = 0.55f), style = Stroke(0.1f))
                }
            }
        }
    }
}
