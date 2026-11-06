package com.elkrrai.feather.feature.habits

import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.platform.LocalDensity
import com.elkrrai.feather.domain.habit.Habit
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
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

/**
 * The looks of the weekly leaf: a smooth edge, a serrated one, a botanical one (a finely toothed blade with parallel veins), and three drawn as
 * pictures: a palm-style frond, a sprig of seven leaflets and a maple of seven lobes. The order is the code saved with a habit: only add at the end.
 */
enum class LeafStyle { Smooth, Serrated, Botanical, Frond, Sprig, Maple }

/**
 * The look a habit's leaf has: the one it was dealt (see `nextLeaf`), kept for good. The order of [LeafStyle] is the code that is saved, so it
 * must not change. Until a habit is dealt one, a look picked from its id stands in.
 */
fun leafStyleFor(habit: Habit): LeafStyle =
    LeafStyle.entries.getOrNull(habit.leaf) ?: LeafStyle.entries[Math.floorMod(habit.id.hashCode(), LeafStyle.entries.size)]

/** The sawtooth along a toothed edge: from 1 - [depth] to 1 of the smooth width, [count] teeth up the leaf, shifted by [seed] so the two edges differ. */
private fun tooth(t: Float, count: Int, seed: Float, depth: Float): Float {
    val p = (t * count + seed) % 1f
    val ramp = if (p < 0.62f) p / 0.62f else (1f - p) / 0.38f
    return 1f - depth + depth * ramp
}

/** How wide the botanical leaf is at [t]: fullest low down, tapering to a point. */
private fun botanicalWidth(t: Float): Float = sin(PI.toFloat() * t.coerceIn(0f, 1f).pow(0.62f)).coerceAtLeast(0f).pow(0.85f)

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
    /** Makes the circles of the days up to today tappable; null leaves the leaf to be looked at only. */
    onDayClick: ((LocalDate) -> Unit)? = null,
    style: LeafStyle = LeafStyle.Smooth,
) {
    val serrated = style == LeafStyle.Serrated
    val botanical = style == LeafStyle.Botanical
    // the frond, the sprig and the maple are pictures with a place for each day, not a width profile with veins
    val art = remember(style) {
        when (style) {
            LeafStyle.Frond -> frondArt()
            LeafStyle.Sprig -> sprigArt()
            LeafStyle.Maple -> mapleArt()
            else -> null
        }
    }
    fun width(t: Float) = if (botanical) botanicalWidth(t) else leafWidth(t)
    // the sawtooth factor of one edge at t; the seed tells the two edges apart
    fun edge(t: Float, seed: Float) = when (style) {
        LeafStyle.Serrated -> tooth(t, 9, seed, 0.28f)
        LeafStyle.Botanical -> tooth(t, 26, seed, 0.055f)
        // the smooth leaf; the frond, the sprig and the maple are drawn by ArtLeaf and never get here
        else -> 1f
    }
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

    if (art != null) {
        ArtLeaf(art, style, week, today, onDayClick, leaf, surface, breath, modifier)
        return
    }

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
            // a toothed edge is, between its teeth, 0.72 (serrated) or 0.945 (botanical) of the smooth one
            val deepest = when (style) {
                LeafStyle.Serrated -> 0.72f
                LeafStyle.Botanical -> 0.945f
                else -> 1f
            }
            return halfMax * (minOf(width(below), width(above)) * deepest) - radius - 8.dp
        }
        // the botanical leaf bends a little to one side: how far its midrib is from the middle at t
        fun shift(t: Float): Dp = if (botanical) 9.dp * sin(PI.toFloat() * t) else 0.dp
        val spots = (0 until DAYS).map { index ->
            // the last day stops well short of the tip, where the leaf is narrow and a circle would touch its edge
            val t = 0.20f + 0.56f * (index / (DAYS - 1f))
            val y = baseY - leafHeight * t
            val side = if (index % 2 == 0) -1f else 1f
            // preferably well out on the vein, never past the edge, never on the midrib
            val reach = maxOf(minOf(halfMax * width(t) * 0.62f, room(t)), radius + 4.dp)
            Spot(centerX + shift(t) + reach * side, y, y + circle * 0.9f, index)
        }

        Canvas(Modifier.fillMaxSize()) {
            val cx = centerX.toPx()
            val top = topY.toPx()
            val base = baseY.toPx()
            val half = halfMax.toPx()
            val height = base - top
            val shiftPx = if (botanical) 9.dp.toPx() else 0f
            fun midX(t: Float) = cx + shiftPx * sin(PI.toFloat() * t)

            // the outline, drawn up the right side and back down the left
            val outline = Path().apply {
                val steps = if (style == LeafStyle.Smooth) 64 else 170
                for (step in 0..steps) {
                    val t = step / steps.toFloat()
                    val point = Offset(midX(t) + half * width(t) * edge(t, 0.1f), base - height * t)
                    if (step == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                }
                for (step in steps downTo 0) {
                    val t = step / steps.toFloat()
                    lineTo(midX(t) - half * width(t) * edge(t, if (botanical) 0.55f else 0.45f), base - height * t)
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
            val fill = if (botanical) {
                // the blade is deeper in the middle and lighter towards its edges
                val edges = if (week.met) 0.34f + 0.10f * breath else 0.26f
                val middle = if (week.met) 0.16f + 0.06f * breath else 0.12f
                Brush.horizontalGradient(
                    listOf(leaf.copy(alpha = edges), leaf.copy(alpha = middle), leaf.copy(alpha = edges)),
                    startX = cx - half,
                    endX = cx + half,
                )
            } else {
                Brush.verticalGradient(
                    listOf(leaf.copy(alpha = 0.10f), leaf.copy(alpha = if (week.met) 0.30f + 0.12f * breath else 0.20f)),
                    startY = top,
                    endY = base,
                )
            }
            drawPath(outline, fill)
            drawPath(outline, leaf.copy(alpha = 0.75f), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

            // the stem and the midrib
            drawLine(leaf.copy(alpha = 0.8f), Offset(cx, base), Offset(cx + 6.dp.toPx(), base + stem.toPx()), 3.dp.toPx(), StrokeCap.Round)
            val midrib = Path().apply {
                val steps = 24
                for (step in 0..steps) {
                    val t = 0.96f * step / steps
                    val point = Offset(midX(t), base - height * t)
                    if (step == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                }
            }
            drawPath(midrib, leaf.copy(alpha = 0.6f), style = Stroke(width = (if (botanical) 3.5f else 2f).dp.toPx(), cap = StrokeCap.Round))

            // the botanical leaf: eleven pairs of parallel veins curving up to the edge, with fine cross veins between
            if (botanical) {
                val pairs = 11
                for (side in listOf(-1f, 1f)) {
                    for (k in 1..pairs) {
                        val tk = 0.06f + 0.80f * k / pairs
                        val te = (tk + 0.17f).coerceAtMost(0.97f)
                        val from = Offset(midX(tk), base - height * tk)
                        // ends short of the finely toothed edge (0.945 at its deepest), inside the leaf
                        val to = Offset(midX(te) + side * half * width(te) * 0.90f, base - height * te)
                        val control = Offset(from.x + side * half * width(tk + 0.07f) * 0.62f, from.y - height * 0.035f)
                        val vein = Path().apply {
                            moveTo(from.x, from.y)
                            quadraticTo(control.x, control.y, to.x, to.y)
                        }
                        drawPath(vein, leaf.copy(alpha = 0.5f), style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))
                        for (m in 1..3) {
                            val f = m / 4f
                            val u = 1f - f
                            val ax = u * u * from.x + 2f * u * f * control.x + f * f * to.x
                            val ay = u * u * from.y + 2f * u * f * control.y + f * f * to.y
                            // a short link towards the next vein, kept clear of the edge
                            drawLine(leaf.copy(alpha = 0.22f), Offset(ax, ay), Offset(ax + side * 5.dp.toPx(), ay - 8.dp.toPx()), 1.dp.toPx(), StrokeCap.Round)
                        }
                    }
                }
            }

            // a vein from the midrib up to each circle, rising in a gentle curve (on the botanical leaf, from its bent midrib)
            spots.forEach { spot ->
                val fromT = (base - spot.veinFrom.toPx()) / height
                val from = Offset(midX(fromT), spot.veinFrom.toPx())
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
                onClick = onDayClick?.takeUnless { day.date.isAfter(today) }?.let { click -> { click(day.date) } },
                size = circle,
                leaf = leaf,
                surface = surface,
                breath = breath,
                modifier = Modifier.offset(x = spot.x - circle, y = spot.y - circle / 2).width(circle * 2),
            )
        }
    }
}

/** One of the leaves drawn as a picture: the board scaled to the room, centred, and a circle placed in each part of the shape. */
@Composable
private fun ArtLeaf(
    art: LeafArt,
    style: LeafStyle,
    week: WeekProgress,
    today: LocalDate,
    onDayClick: ((LocalDate) -> Unit)?,
    leaf: Color,
    surface: Color,
    breath: Float,
    modifier: Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val density = LocalDensity.current
    BoxWithConstraints(modifier.fillMaxSize()) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val fit = minOf(widthPx / BOARD_W, heightPx / BOARD_H)
        val originX = (widthPx - BOARD_W * fit) / 2f
        val originY = (heightPx - BOARD_H * fit) / 2f

        Canvas(Modifier.fillMaxSize()) {
            withTransform({
                translate(originX, originY)
                scale(fit, fit, pivot = Offset.Zero)
            }) {
                if (week.met) {
                    // a soft light round the leaf, breathing
                    for (layer in 1..5) {
                        drawPath(
                            art.outline,
                            leaf.copy(alpha = (0.05f + 0.10f * breath) * (1f - layer / 6f)),
                            style = Stroke(width = 6f + layer * 7f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                        )
                    }
                }
                val fill = if (style == LeafStyle.Maple) {
                    // deeper at the top, lighter at the bottom, like the leaf of an autumn tree
                    Brush.verticalGradient(
                        listOf(leaf.copy(alpha = 0.44f), leaf.copy(alpha = 0.34f), lerp(leaf, Color.White, 0.25f).copy(alpha = 0.30f)),
                        startY = 0f,
                        endY = BOARD_H,
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(leaf.copy(alpha = 0.12f), leaf.copy(alpha = if (week.met) 0.30f + 0.12f * breath else 0.22f)),
                        startY = 0f,
                        endY = BOARD_H,
                    )
                }
                drawPath(art.outline, fill)
                drawPath(art.outline, leaf.copy(alpha = 0.75f), style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                art.details(this, leaf)
            }
        }

        week.days.forEachIndexed { index, day ->
            val spot = art.spots[index]
            val circle = with(density) { (spot.radius * 2f * fit).toDp() }
            val centerX = with(density) { (originX + spot.x * fit).toDp() }
            val centerY = with(density) { (originY + spot.y * fit).toDp() }
            DayCircle(
                number = day.amount,
                label = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                done = day.done,
                dimmed = day.date.isAfter(today),
                onClick = onDayClick?.takeUnless { day.date.isAfter(today) }?.let { click -> { click(day.date) } },
                size = circle,
                leaf = leaf,
                surface = surface,
                breath = breath,
                modifier = Modifier.offset(x = centerX - circle, y = centerY - circle / 2).width(circle * 2),
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
    onClick: (() -> Unit)?,
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
                .border(BorderStroke(1.5.dp, if (done) leaf else leaf.copy(alpha = 0.55f)), CircleShape)
                .then(if (onClick != null) Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onClick) else Modifier),
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
