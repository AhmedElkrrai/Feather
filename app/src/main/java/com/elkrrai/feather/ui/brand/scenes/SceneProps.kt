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

internal fun DrawScope.drawCloud(p: ScenePalette, center: Offset, color: Color = p.cloud) {
    drawCircle(color, radius = 11f, center = Offset(center.x - 12f, center.y))
    drawCircle(color, radius = 14f, center = Offset(center.x + 2f, center.y - 6f))
    drawCircle(color, radius = 10f, center = Offset(center.x + 16f, center.y))
    drawRoundRect(color, topLeft = Offset(center.x - 24f, center.y - 2f), size = Size(52f, 12f), cornerRadius = CornerRadius(6f, 6f))
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
