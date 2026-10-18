package com.elkrrai.feather.ui.brand.scenes

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.elkrrai.feather.ui.components.leafShape
import com.elkrrai.feather.ui.components.moonShape
import com.elkrrai.feather.ui.theme.DayPhase
import com.elkrrai.feather.ui.theme.Nest
import com.elkrrai.feather.ui.theme.featherColors
import com.elkrrai.feather.ui.theme.nest
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import com.elkrrai.feather.ui.theme.leafColor
import com.elkrrai.feather.ui.theme.rememberSeason

/** The colors the scenes use, resolved for the current theme. */
internal class ScenePalette(
    val ground: Color,
    val bowl: Color,
    val weave: Color,
    val cream: Color,
    val amber: Color,
    val coral: Color,
    val sky: Color,
    val moss: Color,
    /** The leaves of the scenes: green, amber in autumn, blossom in spring. */
    val leaf: Color,
    /** Outlines and the loupe's ring: dark in the light theme, light in the dark ones. */
    val ink: Color,
    val cloud: Color,
    val dawnGround: Color,
)

@Composable
internal fun rememberScenePalette(): ScenePalette {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val feather = MaterialTheme.featherColors
    val season = rememberSeason()
    return remember(dark, feather, season) {
        ScenePalette(
            ground = if (dark) nest(0x0B1114) else nest(0xF1E2C2),
            bowl = nest(0xB08A5A),
            weave = nest(0x8A6A43),
            cream = Nest.Cream,
            amber = Nest.Amber,
            coral = Nest.Coral,
            sky = Nest.SkyWing,
            moss = feather.listColors[3],
            leaf = feather.leafColor(season),
            ink = if (dark) Nest.SkyWing else Nest.DeepTeal,
            cloud = if (dark) lerp(nest(0x121E24), Nest.SkyWing, 0.32f) else Color.White,
            dawnGround = feather.sky.getValue(DayPhase.Dawn).second,
        )
    }
}

// Everything below draws in board units (see SCENE_BOARD); the frame scales the board to the screen.

private val LeafPath = leafShape()
private val MoonPath = moonShape()

/** A soft oval on the ground under a scene. */
internal fun DrawScope.drawGround(p: ScenePalette, cx: Float = 100f, y: Float = 172f, halfWidth: Float = 70f) {
    drawOval(p.ground, topLeft = Offset(cx - halfWidth, y - 8f), size = Size(halfWidth * 2f, 16f))
}

/** The inside of a nest, seen over the rim: the dark back of the bowl. Draw before the bird. */
internal fun DrawScope.drawNestBack(p: ScenePalette, cx: Float, cy: Float, halfWidth: Float) {
    drawOval(p.weave, topLeft = Offset(cx - halfWidth, cy - 7f), size = Size(halfWidth * 2f, 14f))
}

/** The front of a nest: the woven bowl and its rim. Draw after the bird and the eggs. */
internal fun DrawScope.drawNestFront(p: ScenePalette, cx: Float, cy: Float, halfWidth: Float) {
    val depth = halfWidth * 0.6f
    drawArc(
        color = p.bowl,
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(cx - halfWidth, cy - depth),
        size = Size(halfWidth * 2f, depth * 2f),
    )
    for (k in -2..2) {
        val x = cx + k * halfWidth * 0.36f
        val bottom = cy + depth * (0.86f - abs(k) * 0.16f)
        drawLine(p.weave, Offset(x - 3f, cy + depth * 0.18f), Offset(x + 3f, bottom), 2.4f, StrokeCap.Round)
    }
    drawLine(p.weave, Offset(cx - halfWidth, cy), Offset(cx + halfWidth, cy), 3.2f, StrokeCap.Round)
}

/** An egg, 12 by 15 on the board's scale of the spec, enlarged by [size], with a speckle or two. */
internal fun DrawScope.drawEgg(p: ScenePalette, center: Offset, size: Float = 1.5f) {
    drawOval(p.cream, topLeft = Offset(center.x - 6f * size, center.y - 7.5f * size), size = Size(12f * size, 15f * size))
    drawCircle(p.amber.copy(alpha = 0.55f), radius = 0.9f * size, center = Offset(center.x - 1.6f * size, center.y - 2f * size))
    drawCircle(p.coral.copy(alpha = 0.5f), radius = 0.8f * size, center = Offset(center.x + 2f * size, center.y + 2.4f * size))
}

internal fun DrawScope.drawSun(p: ScenePalette, center: Offset, radius: Float) {
    drawCircle(p.amber, radius = radius, center = center)
    for (ray in 0 until 8) {
        val angle = ray * PI / 4
        drawLine(
            color = p.amber,
            start = Offset(center.x + (cos(angle) * (radius + 4f)).toFloat(), center.y + (sin(angle) * (radius + 4f)).toFloat()),
            end = Offset(center.x + (cos(angle) * (radius + 9f)).toFloat(), center.y + (sin(angle) * (radius + 9f)).toFloat()),
            strokeWidth = 2.6f,
            cap = StrokeCap.Round,
        )
    }
}

/** A crescent moon whose box is [size] wide, with its top-left corner at [topLeft]. */
internal fun DrawScope.drawMoon(p: ScenePalette, topLeft: Offset, size: Float) {
    translate(topLeft.x, topLeft.y) { scale(size, pivot = Offset.Zero) { drawPath(MoonPath, p.cream) } }
}

/** A leaf with its stem end at [base], [length] long, turned [degrees] clockwise from pointing up and right. */
internal fun DrawScope.drawLeaf(base: Offset, length: Float, degrees: Float, color: Color) {
    translate(base.x, base.y) {
        rotate(degrees, pivot = Offset.Zero) {
            scale(length, pivot = Offset.Zero) {
                translate(-0.12f, -0.88f) { drawPath(LeafPath, color) }
            }
        }
    }
}

internal fun DrawScope.drawBranch(p: ScenePalette, from: Offset, to: Offset, width: Float = 8f) {
    drawLine(p.bowl, from, to, width, StrokeCap.Round)
}

/** A four-pointed sparkle. */
internal fun DrawScope.drawSparkle(center: Offset, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        quadraticTo(center.x, center.y, center.x + radius, center.y)
        quadraticTo(center.x, center.y, center.x, center.y + radius)
        quadraticTo(center.x, center.y, center.x - radius, center.y)
        quadraticTo(center.x, center.y, center.x, center.y - radius)
        close()
    }
    drawPath(path, color)
}

internal fun DrawScope.drawCloud(p: ScenePalette, center: Offset) {
    drawCircle(p.cloud, radius = 11f, center = Offset(center.x - 12f, center.y))
    drawCircle(p.cloud, radius = 14f, center = Offset(center.x + 2f, center.y - 6f))
    drawCircle(p.cloud, radius = 10f, center = Offset(center.x + 16f, center.y))
    drawRoundRect(p.cloud, topLeft = Offset(center.x - 24f, center.y - 2f), size = Size(52f, 12f), cornerRadius = CornerRadius(6f, 6f))
}

/** A woven basket or bin: a trapezoid with rows of weave. */
internal fun DrawScope.drawBasket(
    p: ScenePalette,
    cx: Float,
    topY: Float,
    bottomY: Float,
    topHalf: Float,
    bottomHalf: Float,
) {
    val body = Path().apply {
        moveTo(cx - topHalf, topY)
        lineTo(cx + topHalf, topY)
        lineTo(cx + bottomHalf, bottomY)
        lineTo(cx - bottomHalf, bottomY)
        close()
    }
    drawPath(body, p.bowl)
    for (row in 1..3) {
        val t = row / 4f
        val y = topY + (bottomY - topY) * t
        val half = topHalf + (bottomHalf - topHalf) * t
        drawLine(p.weave, Offset(cx - half + 3f, y), Offset(cx + half - 3f, y), 2.6f, StrokeCap.Round)
    }
    drawRoundRect(
        color = p.weave,
        topLeft = Offset(cx - topHalf - 3f, topY - 5f),
        size = Size(topHalf * 2f + 6f, 9f),
        cornerRadius = CornerRadius(4.5f, 4.5f),
    )
}

/** A bell, amber, with its flared lip and clapper, centered on [cx]. */
internal fun DrawScope.drawBell(p: ScenePalette, cx: Float, top: Float) {
    val shade = lerp(p.amber, Color.Black, 0.18f)
    val body = Path().apply {
        moveTo(cx - 34f, top + 90f)
        cubicTo(cx - 34f, top + 52f, cx - 22f, top + 18f, cx, top + 18f)
        cubicTo(cx + 22f, top + 18f, cx + 34f, top + 52f, cx + 34f, top + 90f)
        lineTo(cx + 46f, top + 102f)
        lineTo(cx - 46f, top + 102f)
        close()
    }
    drawCircle(shade, radius = 6f, center = Offset(cx, top + 14f))
    drawPath(body, p.amber)
    drawRoundRect(shade, topLeft = Offset(cx - 50f, top + 100f), size = Size(100f, 10f), cornerRadius = CornerRadius(5f, 5f))
    drawCircle(shade, radius = 8f, center = Offset(cx, top + 120f))
}

/** A magnifying glass: a lens with a soft tint, a ring and a handle. */
internal fun DrawScope.drawLoupe(p: ScenePalette, center: Offset, radius: Float) {
    drawCircle(p.sky.copy(alpha = 0.22f), radius = radius, center = center)
    drawCircle(p.ink, radius = radius, center = center, style = Stroke(width = 6f))
    val angle = PI / 4
    val start = Offset(center.x + (cos(angle) * radius).toFloat(), center.y + (sin(angle) * radius).toFloat())
    drawLine(p.ink, start, Offset(start.x + 20f, start.y + 20f), 8f, StrokeCap.Round)
}

/** A bare twig with a fork, rising from [from] to [to]. */
internal fun DrawScope.drawTwig(p: ScenePalette, from: Offset, to: Offset, width: Float = 4f) {
    drawLine(p.bowl, from, to, width, StrokeCap.Round)
    val mid = Offset(from.x + (to.x - from.x) * 0.55f, from.y + (to.y - from.y) * 0.55f)
    val fork = Offset(mid.x + (to.x - from.x) * 0.35f + 12f, mid.y + (to.y - from.y) * 0.25f - 10f)
    drawLine(p.bowl, mid, fork, width * 0.7f, StrokeCap.Round)
}

/** A check mark in a circle, for the backup badge. */
internal fun DrawScope.drawCheckBadge(p: ScenePalette, center: Offset, radius: Float) {
    drawCircle(p.moss, radius = radius, center = center)
    val path = Path().apply {
        moveTo(center.x - radius * 0.45f, center.y + radius * 0.02f)
        lineTo(center.x - radius * 0.1f, center.y + radius * 0.38f)
        lineTo(center.x + radius * 0.5f, center.y - radius * 0.35f)
    }
    drawPath(path, p.cream, style = Stroke(width = radius * 0.24f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}
