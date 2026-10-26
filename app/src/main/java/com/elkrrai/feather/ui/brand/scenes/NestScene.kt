package com.elkrrai.feather.ui.brand.scenes

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.nest.NestItem
import com.elkrrai.feather.domain.nest.NestOutfit
import com.elkrrai.feather.domain.nest.NestStyle
import com.elkrrai.feather.ui.brand.BirdMood
import com.elkrrai.feather.ui.brand.OwlDress
import com.elkrrai.feather.ui.brand.rememberBirdMotionAllowed


/**
 * The bird's nest with everything it has been given: a scarf, fairy lights, bunting, a lantern, moss,
 * glasses, a flower pot and a party hat (the scarf, the glasses and the hat are worn by the bird itself). Only what has been earned is drawn. It is the same scene
 * kit as the empty states, so the bird is alive and the whole thing stays quiet. The nest itself is
 * the person's choice ([style]); the bird sits in it whichever it is.
 */
@Composable
fun NestScene(
    items: Set<NestItem>,
    modifier: Modifier = Modifier,
    size: Dp = 280.dp,
    style: NestStyle = NestStyle.BRAIDED,
    outfit: NestOutfit = NestOutfit(),
) =
    SceneFrame(size, modifier) {
        val p = palette
        val art = style.art()
        val nestHeight = NEST_WIDTH * art.heightPerWidth
        val nestTop = GROUND_LINE - nestHeight
        // the bird stands with its feet at the floor of this nest
        val bird = BirdPlacement(x = BIRD_X, y = nestTop + art.feet * nestHeight - BIRD_FEET * 96f * BIRD_SCALE, scale = BIRD_SCALE)
        Layer {
            if (items.any { it == NestItem.FAIRY_LIGHTS || it == NestItem.BUNTING || it == NestItem.LANTERN }) {
                // What hangs is hung on two ropes from the top of the board.
                drawLine(p.weave, Offset(20f, 0f), Offset(20f, 29f), 1.6f, StrokeCap.Round)
                drawLine(p.weave, Offset(180f, 0f), Offset(180f, 27f), 1.6f, StrokeCap.Round)
            }
            if (NestItem.BUNTING in items) drawBunting(p.coral, p.amber, p.sky, p.weave)
            if (NestItem.FAIRY_LIGHTS in items) drawFairyLights(p.weave, listOf(p.amber, p.coral, p.sky, p.cream))
            if (NestItem.LANTERN in items) drawLantern(p.weave, p.amber, p.bowl)
            drawGround(p, y = 176f, halfWidth = 70f)
            if (NestItem.MOSS in items) drawMoss(p.moss, p.leaf)
        }
        NestLayer(art.back, nestTop, nestHeight)
        // what the bird wears: each garment shows once earned, in the choice made for it
        val dress = OwlDress(
            scarf = if (NestItem.SCARF in items) outfit.scarf else NestOutfit.NONE,
            glasses = if (NestItem.GLASSES in items) outfit.glasses else NestOutfit.NONE,
            hat = if (NestItem.PARTY_HAT in items) outfit.hat else NestOutfit.NONE,
        )
        SceneBird(BirdMood.IDLE, bird, dress, preens = false)
        // a little owlet beside it, rocking now and then, its feet down in the nest a little
        val chickHeight = NESTLING_HEIGHT
        Chick(
            picture = R.drawable.chick_3,
            left = NESTLING_CENTRE - chickHeight * 0.797f / 2f,
            top = nestTop + art.nestlingFeet * nestHeight - chickHeight,
            wobbleMillis = 2_100,
            degrees = 5f,
            startAt = 0.3f,
            moving = rememberBirdMotionAllowed(),
            height = chickHeight,
        )
        // the front of the nest, over the bird's feet
        NestLayer(art.front, nestTop, nestHeight)
        if (NestItem.FLOWER_POT in items) PotPicture(outfit.pot)
    }

/** One picture of the nest: the whole of it for the back layer, only its front for the front layer. */
@Composable
internal fun SceneScope.NestLayer(
    @DrawableRes drawable: Int,
    top: Float,
    height: Float,
    left: Float = NEST_X,
    width: Float = NEST_WIDTH,
) {
    Image(
        painter = painterResource(drawable),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = Modifier
            .offset(x = unit * left, y = unit * top)
            .size(unit * width, unit * height),
    )
}

/** The flower pot beside the nest: the choice made for it, standing on the ground. */
@Composable
private fun SceneScope.PotPicture(choice: Int) {
    val picture = Pots.getOrNull(choice) ?: return
    Image(
        painter = painterResource(picture),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = Modifier
            .offset(x = unit * POT_X, y = unit * (POT_BOTTOM - POT_HEIGHT))
            .size(unit * POT_HEIGHT * POT_ASPECT, unit * POT_HEIGHT),
    )
}

private val Pots = intArrayOf(R.drawable.nest_pot_0, R.drawable.nest_pot_1, R.drawable.nest_pot_2, R.drawable.nest_pot_3)

// Each pot picture is 320 by 500, the pot standing on its bottom edge.
private const val POT_ASPECT = 320f / 500f
private const val POT_HEIGHT = 46f
private const val POT_X = 162f
private const val POT_BOTTOM = 174f

/** The nest's pictures, and where in them the bird's feet land (a share of the picture's height). */
internal class NestArt(
    @DrawableRes val back: Int,
    @DrawableRes val front: Int,
    /** The picture's height for each unit of its width. */
    val heightPerWidth: Float,
    val feet: Float,
    /**
     * Where the owlet's feet land (a share of the picture's height): a little below the top edge of the
     * front rim where the owlet sits, so the rim covers its feet and it is down in the nest.
     */
    val nestlingFeet: Float,
)

internal fun NestStyle.art() = when (this) {
    NestStyle.BRAIDED -> NestArt(R.drawable.nest_braided_back, R.drawable.nest_braided_front, 701f / 920f, 0.64f, 0.72f)
    NestStyle.TWIGS -> NestArt(R.drawable.nest_twigs_back, R.drawable.nest_twigs_front, 577f / 908f, 0.46f, 0.55f)
    NestStyle.STRAW -> NestArt(R.drawable.nest_straw_back, R.drawable.nest_straw_front, 568f / 864f, 0.60f, 0.49f)
    NestStyle.ROPE -> NestArt(R.drawable.nest_rope_back, R.drawable.nest_rope_front, 639f / 934f, 0.55f, 0.64f)
}

/** The whole nest of [this] style, to show it small, for choosing. */
@DrawableRes
fun NestStyle.pictureRes(): Int = art().back

internal const val NEST_WIDTH = 152f
internal const val NEST_X = 24f
internal const val GROUND_LINE = 172f
private const val BIRD_X = 40f
private const val BIRD_SCALE = 0.95f

/** The owlet in the nest: how tall it is and where its middle is on the board. */
private const val NESTLING_HEIGHT = 42f
private const val NESTLING_CENTRE = 130f

/** The bird's feet are this far down its box. */
private const val BIRD_FEET = 0.945f

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
    drawLine(string, Offset(170f, 0f), Offset(170f, 46f), 1.4f, StrokeCap.Round)
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

