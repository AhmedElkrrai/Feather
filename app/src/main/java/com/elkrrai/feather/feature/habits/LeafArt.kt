package com.elkrrai.feather.feature.habits

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/*
 * The three leaves that are drawn as pictures (a frond, a sprig and a maple), not as a width profile with veins. Each is made on a board
 * of 328 by 470 units, upright with the stem at the bottom; the screen scales the board to the room it has. A day sits in a part of the
 * shape (a leaflet, a lobe), so each leaf says where its seven circles go.
 */

internal const val BOARD_W = 328f
internal const val BOARD_H = 470f
private const val STEM = 26f
private const val BASE = BOARD_H - STEM - 16f
private const val MID = BOARD_W / 2f
private const val HEIGHT = BASE - 10f
private const val DEG = (PI / 180.0).toFloat()

/** A day's circle on the board: its middle and its radius. */
internal class DaySpot(val x: Float, val y: Float, val radius: Float)

internal class LeafArt(
    /** Every blade of the leaf, to fill, to outline and to glow round when the week's goal is met. */
    val outline: Path,
    /** The stem, veins and marks, drawn over the fill. Lines are in board units. */
    val details: DrawScope.(leaf: Color) -> Unit,
    /** The seven circles, Saturday to Friday (the first day of the week to the last), each wholly inside its part of the leaf. */
    val spots: List<DaySpot>,
    /** The stem as a line, from its free end up to the leaf: the small leaves of the overview draw only this and the outline. */
    val stem: Pair<Offset, Offset>,
)

/** The art for a look drawn as a picture, or null for a look drawn as a width profile (smooth, serrated, botanical). */
internal fun leafArtFor(style: LeafStyle): LeafArt? = when (style) {
    LeafStyle.Frond -> frondArt()
    LeafStyle.Sprig -> sprigArt()
    LeafStyle.Maple -> mapleArt()
    else -> null
}

private fun polygon(points: List<Offset>): Path = Path().apply {
    points.forEachIndexed { index, p -> if (index == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
    close()
}

private fun distanceToSegment(p: Offset, a: Offset, b: Offset): Float {
    val abx = b.x - a.x
    val aby = b.y - a.y
    val lengthSquared = abx * abx + aby * aby
    val t = if (lengthSquared == 0f) 0f else (((p.x - a.x) * abx + (p.y - a.y) * aby) / lengthSquared).coerceIn(0f, 1f)
    val dx = p.x - (a.x + abx * t)
    val dy = p.y - (a.y + aby * t)
    return sqrt(dx * dx + dy * dy)
}

/**
 * Moves a circle towards [anchor] until the whole of it, and [margin] more, is clear of every edge of [outline]. A circle that is clear
 * stays where it is. The circle starts inside the shape, and moving towards a point inside keeps it there.
 */
private fun clearOfEdges(spot: DaySpot, outline: List<Offset>, anchor: Offset, margin: Float): DaySpot {
    var x = spot.x
    var y = spot.y
    repeat(120) {
        val here = Offset(x, y)
        var nearest = Float.MAX_VALUE
        for (i in outline.indices) nearest = min(nearest, distanceToSegment(here, outline[i], outline[(i + 1) % outline.size]))
        if (nearest >= spot.radius + margin) return DaySpot(x, y, spot.radius)
        val dx = anchor.x - x
        val dy = anchor.y - y
        val distance = sqrt(dx * dx + dy * dy)
        if (distance < 1f) return DaySpot(x, y, spot.radius)
        x += dx / distance * 1.5f
        y += dy / distance * 1.5f
    }
    return DaySpot(x, y, spot.radius)
}

/** A maple: seven toothed lobes fanning from where the stem joins, the two small ones at the bottom starting and ending at the stem. */
internal fun mapleArt(): LeafArt {
    val radius = 184f
    val stretch = 1.3f
    val centerY = BASE - 150f
    val angles = listOf(-112f, -72f, -36f, 0f, 36f, 72f, 112f).map { it * DEG }
    val lengths = listOf(0.62f, 0.88f, 0.80f, 1.0f, 0.80f, 0.88f, 0.62f)
    fun at(angle: Float, r: Float) = Offset(MID + radius * r * sin(angle), centerY - radius * stretch * r * cos(angle))
    val valley = 0.42f
    val teeth = 2
    val atStem = 0.02f

    val points = ArrayList<Offset>()
    for (i in 0 until 7) {
        val from = if (i == 0) angles[0] - 0.42f else (angles[i - 1] + angles[i]) / 2f
        val to = if (i == 6) angles[6] + 0.42f else (angles[i] + angles[i + 1]) / 2f
        val length = lengths[i]
        val start = if (i == 0) atStem else valley
        val end = if (i == 6) atStem else valley
        points += at(from, start)
        for (j in 1..teeth) {
            val f = j / (teeth + 1f)
            points += at(from + (angles[i] - from) * (f * 0.9f), (start + (length - start) * f) * (if (j % 2 == 1) 1.07f else 0.96f))
        }
        points += at(angles[i], length)
        for (j in teeth downTo 1) {
            val f = j / (teeth + 1f)
            points += at(to + (angles[i] - to) * (f * 0.9f), (end + (length - end) * f) * (if (j % 2 == 1) 1.07f else 0.96f))
        }
        if (i == 6) points += at(to, end)
    }

    // The two small lower lobes are too narrow for a circle at their middle: theirs sit towards the centre.
    val raw = (0 until 7).map { i ->
        when (i) {
            0 -> DaySpot(at(-100f * DEG, 0.34f).x, at(-100f * DEG, 0.34f).y, 15f)
            6 -> DaySpot(at(100f * DEG, 0.34f).x, at(100f * DEG, 0.34f).y, 15f)
            else -> at(angles[i], lengths[i] * 0.58f).let { DaySpot(it.x, it.y, 19f) }
        }
    }
    val anchor = Offset(MID, centerY)
    val spots = raw.map { clearOfEdges(it, points, anchor, 3f) }

    return LeafArt(
        outline = polygon(points),
        spots = spots,
        stem = Offset(MID + 2f, BASE + STEM) to Offset(MID, centerY),
        details = { leaf ->
            val line = leaf.copy(alpha = 0.75f)
            val light = lerp(leaf, Color.White, 0.5f)
            drawLine(line, Offset(MID, centerY), Offset(MID + 2f, BASE + STEM), 4f, StrokeCap.Round)
            for (i in 0 until 7) {
                val tip = at(angles[i], lengths[i] * 0.93f)
                drawLine(line, Offset(MID, centerY), tip, 2.6f, StrokeCap.Round)
                drawLine(light.copy(alpha = 0.7f), Offset(MID - 1f, centerY), Offset(tip.x - 1f, tip.y), 1.2f, StrokeCap.Round)
            }
            // fine side veins in the five bigger lobes, kept inside them: a lobe narrows towards its tip, and so does the angle a vein may spread
            for (i in 1..5) {
                for (f in listOf(0.45f, 0.65f)) {
                    val from = at(angles[i], lengths[i] * f)
                    val reach = lengths[i] * (f + 0.17f)
                    val halfWidth = max(0f, 0.36f * (1f - (reach - valley) / (lengths[i] - valley)))
                    for (side in listOf(-1f, 1f)) {
                        drawLine(light.copy(alpha = 0.5f), from, at(angles[i] + side * 0.55f * halfWidth, reach), 1.1f)
                    }
                }
            }
        },
    )
}

private const val SPRIG_TOP = 0.74f

/** Where along its length a sprig leaflet's blade starts: before that it is only a short stalk, so the blade never reaches across the stem. */
private const val BLADE_FROM = 0.12f

/** A sprig: a thin stem with six pointed leaflets alternating up it and a seventh at its tip, each marked with a midline and short dashes. */
internal fun sprigArt(): LeafArt {
    fun stemX(t: Float) = MID + 8f * sin(PI.toFloat() * t * 1.4f)
    val random = Random(7)
    val blades = ArrayList<List<Offset>>()
    val midlines = ArrayList<Pair<Offset, Offset>>()
    val dashes = ArrayList<Pair<Offset, Offset>>()
    val spots = ArrayList<DaySpot>()

    for (i in 0 until 7) {
        // Six leaflets climb the stem and the seventh ends it. They are spaced so that no leaflet reaches the next one up on its own side
        // (a leaflet is about 70 units tall, and two on one side are 80 apart), and the last one stops short of the top of the board.
        val top = i == 6
        val t = if (top) SPRIG_TOP else 0.12f + 0.10f * i
        val side = if (i % 2 == 0) -1f else 1f
        val length = 104f
        val width = if (top) 50f else 46f
        val baseX = stemX(t)
        val baseY = BASE - HEIGHT * t
        val dx: Float
        val dy: Float
        val nx: Float
        val ny: Float
        if (top) {
            dx = 0f; dy = -1f; nx = 1f; ny = 0f
        } else {
            val a = 40f * DEG
            dx = side * cos(a); dy = -sin(a); nx = -dy * side; ny = dx * side
        }
        val left = ArrayList<Offset>()
        val right = ArrayList<Offset>()
        for (k in 0..18) {
            val u = k / 18f
            val half = width / 2f * sin(PI.toFloat() * u.pow(0.8f)).coerceAtLeast(0f).pow(0.85f)
            // the blade, from where its stalk ends to the tip
            val along = BLADE_FROM + (1f - BLADE_FROM) * u
            val x = baseX + dx * length * along
            val y = baseY + dy * length * along
            left += Offset(x + nx * half, y + ny * half)
            right += Offset(x - nx * half, y - ny * half)
        }
        val blade = left + right.reversed()
        blades += blade
        midlines += Offset(baseX, baseY) to Offset(baseX + dx * length * 0.96f, baseY + dy * length * 0.96f)
        repeat(46) {
            val u = 0.08f + 0.84f * random.nextFloat()
            val half = width / 2f * sin(PI.toFloat() * u.pow(0.8f)).coerceAtLeast(0f).pow(0.85f) * 0.82f
            val v = (random.nextFloat() * 2f - 1f) * half
            if (abs(v) >= 3f) {
                val along = BLADE_FROM + (1f - BLADE_FROM) * u
                val q = Offset(baseX + dx * length * along + nx * v, baseY + dy * length * along + ny * v)
                dashes += q to Offset(q.x + dx * 5f, q.y + dy * 5f)
            }
        }
        val middle = DaySpot(baseX + dx * length * 0.56f, baseY + dy * length * 0.56f, 17.5f)
        spots += clearOfEdges(middle, blade, Offset(baseX + dx * length * 0.4f, baseY + dy * length * 0.4f), 3f)
    }

    val outline = Path().apply { blades.forEach { addPath(polygon(it)) } }
    return LeafArt(
        outline = outline,
        spots = spots,
        stem = Offset(stemX(0f), BASE + STEM) to Offset(stemX(SPRIG_TOP), BASE - HEIGHT * SPRIG_TOP),
        details = { leaf ->
            val line = leaf.copy(alpha = 0.75f)
            // The stem is drawn by the same height as the leaflets' bases (t), from below the board's base up to where the last leaflet
            // begins, so it ends exactly there.
            val stem = Path().apply {
                val bottom = -STEM / HEIGHT
                for (k in 0..40) {
                    val tt = bottom + (SPRIG_TOP - bottom) * k / 40f
                    if (k == 0) moveTo(stemX(tt), BASE - HEIGHT * tt) else lineTo(stemX(tt), BASE - HEIGHT * tt)
                }
            }
            drawPath(stem, line, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f, cap = StrokeCap.Round))
            midlines.forEach { (a, b) -> drawLine(Color.Black.copy(alpha = 0.55f), a, b, 2.4f, StrokeCap.Round) }
            dashes.forEach { (a, b) -> drawLine(Color.Black.copy(alpha = 0.45f), a, b, 1.2f, StrokeCap.Round) }
        },
    )
}

/** A palm-style frond: a curved stem with narrow leaflets angled up along both sides, longest in the middle. */
internal fun frondArt(): LeafArt {
    fun stemX(t: Float) = MID + 22f * sin(PI.toFloat() * t * 0.9f) - 6f
    fun leafletLength(t: Float) = max(26f, 150f * sin(PI.toFloat() * t.pow(0.75f)).coerceAtLeast(0f).pow(0.9f))
    val angle = 52f * DEG
    val outline = Path()
    val ribs = ArrayList<Pair<Offset, Offset>>()
    val count = 13
    for (k in count downTo 0) {
        val t = 0.08f + 0.88f * k / count
        val length = leafletLength(t)
        val width = 9f
        for (side in listOf(-1f, 1f)) {
            val base = Offset(stemX(t), BASE - HEIGHT * t)
            val tip = Offset(base.x + side * length * cos(angle), base.y - length * sin(angle))
            val halfway = Offset(base.x + side * length * 0.5f * cos(angle), base.y - length * 0.5f * sin(angle))
            outline.moveTo(base.x, base.y)
            outline.quadraticTo(halfway.x - side * width * 0.5f, halfway.y - width, tip.x, tip.y)
            outline.quadraticTo(halfway.x + side * width * 0.5f, halfway.y + width * 0.2f, base.x, base.y + 5f)
            outline.close()
            ribs += base to Offset(tip.x - side * 3f, tip.y + 3f)
        }
    }
    val spots = (0 until 7).map { k ->
        val t = 0.20f + 0.56f * k / 6f
        val side = if (k % 2 == 0) -1f else 1f
        val length = leafletLength(t)
        // on the leaflet, where it leaves the stem: wider than the leaflet, so the circle overlaps it, which is meant
        DaySpot(stemX(t) + side * length * 0.42f * cos(angle), BASE - HEIGHT * t - length * 0.42f * sin(angle), 19.5f)
    }
    return LeafArt(
        outline = outline,
        spots = spots,
        stem = Offset(stemX(0f) + 8f, BASE + STEM) to Offset(stemX(0f), BASE),
        details = { leaf ->
            val line = leaf.copy(alpha = 0.75f)
            val light = lerp(leaf, Color.White, 0.6f)
            ribs.forEach { (a, b) -> drawLine(light.copy(alpha = 0.45f), a, b, 1f) }
            drawLine(line, Offset(stemX(0f), BASE), Offset(stemX(0f) + 8f, BASE + STEM), 4f, StrokeCap.Round)
            val rachis = Path().apply {
                // from where the stem leaves the leaf, so the two meet with no gap
                for (k in 0..30) {
                    val t = 0.98f * k / 30f
                    if (k == 0) moveTo(stemX(t), BASE - HEIGHT * t) else lineTo(stemX(t), BASE - HEIGHT * t)
                }
            }
            drawPath(rachis, line, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5.5f, cap = StrokeCap.Round))
            drawPath(rachis, light.copy(alpha = 0.7f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.8f, cap = StrokeCap.Round))
        },
    )
}
