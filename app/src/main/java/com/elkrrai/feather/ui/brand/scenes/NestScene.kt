package com.elkrrai.feather.ui.brand.scenes

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.domain.nest.NestItem
import com.elkrrai.feather.ui.brand.BirdMood

/** The bird's place in its nest, the same as on the all-done scene. */
private val NestBird = AllDoneBird

/** A point of the bird's 96 by 96 box on the 200 by 200 board. */
private fun bird(x: Float, y: Float) = Offset(NestBird.x + x * NestBird.scale, NestBird.y + y * NestBird.scale)

private val Terracotta = Color(0xFFC77D4F)
private val TerracottaDark = Color(0xFFA6613B)

/**
 * The bird's nest with everything it has been given: a scarf, fairy lights, bunting, a lantern, moss,
 * glasses, a flower pot and a party hat. Only what has been earned is drawn. It is the same scene
 * kit as the empty states, so the bird is alive and the whole thing stays quiet.
 */
@Composable
fun NestScene(items: Set<NestItem>, modifier: Modifier = Modifier, size: Dp = 280.dp) =
    SceneFrame(size, modifier) {
        val p = palette
        Layer {
            if (items.any { it == NestItem.FAIRY_LIGHTS || it == NestItem.BUNTING || it == NestItem.LANTERN }) {
                drawBranch(p, Offset(14f, 26f), Offset(186f, 22f), width = 7f)
            }
            if (NestItem.BUNTING in items) drawBunting(p.coral, p.amber, p.sky, p.weave)
            if (NestItem.FAIRY_LIGHTS in items) drawFairyLights(p.weave, listOf(p.amber, p.coral, p.sky, p.cream))
            if (NestItem.LANTERN in items) drawLantern(p.weave, p.amber, p.bowl)
            drawGround(p, y = 176f, halfWidth = 70f)
            if (NestItem.MOSS in items) drawMoss(p.moss, p.leaf)
            drawNestBack(p, 100f, 152f, 54f)
        }
        SceneBird(BirdMood.IDLE, NestBird)
        Layer {
            drawNestFront(p, 100f, 152f, 54f)
            if (NestItem.FLOWER_POT in items) drawFlowerPot(p.coral, p.amber, p.moss)
            if (NestItem.SCARF in items) drawScarf(p.coral)
            if (NestItem.GLASSES in items) drawGlasses(p.ink, p.sky)
            if (NestItem.PARTY_HAT in items) drawPartyHat(p.coral, p.amber, p.cream)
        }
    }

/** A swag of bulbs on a thin wire between the two ends of the branch. */
private fun DrawScope.drawFairyLights(wire: Color, colors: List<Color>) {
    val from = Offset(20f, 28f)
    val to = Offset(180f, 26f)
    val sag = 22f
    val path = Path().apply {
        moveTo(from.x, from.y)
        quadraticTo((from.x + to.x) / 2f, (from.y + to.y) / 2f + 2f * sag, to.x, to.y)
    }
    drawPath(path, wire, style = Stroke(1.4f, cap = StrokeCap.Round))
    for (i in 0..8) {
        val t = (i + 0.5f) / 9f
        val u = 1f - t
        val x = u * u * from.x + 2f * u * t * ((from.x + to.x) / 2f) + t * t * to.x
        val y = u * u * from.y + 2f * u * t * ((from.y + to.y) / 2f + 2f * sag) + t * t * to.y
        val color = colors[i % colors.size]
        drawCircle(color.copy(alpha = 0.28f), radius = 6f, center = Offset(x, y + 4f))
        drawCircle(color, radius = 3f, center = Offset(x, y + 4f))
    }
}

/** Triangular flags on a string, in three colors. */
private fun DrawScope.drawBunting(a: Color, b: Color, c: Color, string: Color) {
    val from = Offset(20f, 30f)
    val to = Offset(180f, 28f)
    val sag = 20f
    val control = Offset((from.x + to.x) / 2f, (from.y + to.y) / 2f + 2f * sag)
    val path = Path().apply {
        moveTo(from.x, from.y)
        quadraticTo(control.x, control.y, to.x, to.y)
    }
    drawPath(path, string, style = Stroke(1.2f, cap = StrokeCap.Round))
    val colors = listOf(a, b, c)
    for (i in 0..7) {
        val t = (i + 0.5f) / 8f
        val u = 1f - t
        val x = u * u * from.x + 2f * u * t * control.x + t * t * to.x
        val y = u * u * from.y + 2f * u * t * control.y + t * t * to.y
        val flag = Path().apply {
            moveTo(x - 6f, y)
            lineTo(x + 6f, y)
            lineTo(x, y + 14f)
            close()
        }
        drawPath(flag, colors[i % colors.size])
    }
}

/** A small lantern hanging from the branch, glowing. */
private fun DrawScope.drawLantern(string: Color, glow: Color, frame: Color) {
    val top = Offset(170f, 24f)
    drawLine(string, top, Offset(170f, 46f), 1.4f, StrokeCap.Round)
    drawCircle(
        brush = Brush.radialGradient(listOf(glow.copy(alpha = 0.45f), Color.Transparent), center = Offset(170f, 60f), radius = 26f),
        radius = 26f,
        center = Offset(170f, 60f),
    )
    drawRoundRect(frame, topLeft = Offset(161f, 44f), size = Size(18f, 4f), cornerRadius = CornerRadius(2f))
    drawRoundRect(glow, topLeft = Offset(162f, 48f), size = Size(16f, 20f), cornerRadius = CornerRadius(5f))
    drawRoundRect(frame, topLeft = Offset(161f, 66f), size = Size(18f, 4f), cornerRadius = CornerRadius(2f))
}

/** A few cushions of moss on the ground beside the nest. */
private fun DrawScope.drawMoss(moss: Color, light: Color) {
    drawOval(moss, topLeft = Offset(20f, 168f), size = Size(42f, 14f))
    drawOval(light, topLeft = Offset(28f, 165f), size = Size(24f, 11f))
    drawOval(moss, topLeft = Offset(150f, 170f), size = Size(34f, 12f))
    drawOval(light, topLeft = Offset(158f, 167f), size = Size(18f, 9f))
}

/** A terracotta pot with a flower, beside the nest. */
private fun DrawScope.drawFlowerPot(petal: Color, center: Color, leaf: Color) {
    drawLine(leaf, Offset(168f, 152f), Offset(168f, 130f), 2.4f, StrokeCap.Round)
    drawOval(leaf, topLeft = Offset(168f, 138f), size = Size(12f, 6f))
    for (k in 0 until 5) {
        val angle = k * 2.0 * Math.PI / 5
        drawCircle(petal, radius = 4.2f, center = Offset(168f + (Math.cos(angle) * 5.5).toFloat(), 126f + (Math.sin(angle) * 5.5).toFloat()))
    }
    drawCircle(center, radius = 3.4f, center = Offset(168f, 126f))
    val pot = Path().apply {
        moveTo(157f, 150f)
        lineTo(179f, 150f)
        lineTo(176f, 174f)
        lineTo(160f, 174f)
        close()
    }
    drawPath(pot, Terracotta)
    drawRoundRect(TerracottaDark, topLeft = Offset(155f, 148f), size = Size(26f, 6f), cornerRadius = CornerRadius(2f))
}

/** A scarf round the bird's neck with a tail hanging at the front. */
private fun DrawScope.drawScarf(color: Color) {
    val path = Path().apply {
        val start = bird(46f, 52f)
        val end = bird(73f, 55f)
        moveTo(start.x, start.y)
        quadraticTo(bird(60f, 62f).x, bird(60f, 62f).y, end.x, end.y)
    }
    drawPath(path, color, style = Stroke(5.2f, cap = StrokeCap.Round))
    val tailTop = bird(52f, 57f)
    val tailBottom = bird(49f, 72f)
    drawLine(color, tailTop, tailBottom, 5f, StrokeCap.Round)
    drawLine(Color.White.copy(alpha = 0.35f), bird(58f, 57f), bird(60f, 59f), 1.6f, StrokeCap.Round)
}

/** Tiny round glasses on the bird's eye, with an arm toward its ear. */
private fun DrawScope.drawGlasses(frame: Color, lens: Color) {
    val eye = bird(66f, 37f)
    drawCircle(lens.copy(alpha = 0.28f), radius = 6.4f, center = eye)
    drawCircle(frame, radius = 6.4f, center = eye, style = Stroke(1.5f))
    drawLine(frame, Offset(eye.x - 6.4f, eye.y - 0.5f), bird(52f, 35f), 1.4f, StrokeCap.Round)
}

/** A small party hat tilted on the crest, with a pompom. */
private fun DrawScope.drawPartyHat(hat: Color, stripe: Color, pompom: Color) {
    val left = bird(53f, 25f)
    val right = bird(75f, 21f)
    val apex = bird(64f, -2f)
    val path = Path().apply {
        moveTo(left.x, left.y)
        lineTo(right.x, right.y)
        lineTo(apex.x, apex.y)
        close()
    }
    drawPath(path, hat)
    drawLine(stripe, bird(57.5f, 14f), bird(70f, 12.5f), 2.4f, StrokeCap.Round)
    drawLine(stripe, bird(61f, 7f), bird(67f, 6.2f), 2.2f, StrokeCap.Round)
    drawCircle(pompom, radius = 3.2f, center = apex)
}
