package com.elkrrai.feather.ui.brand.scenes

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.elkrrai.feather.R
import com.elkrrai.feather.ui.brand.SideOwlFlight
import com.elkrrai.feather.ui.brand.rememberAmbientPhase
import com.elkrrai.feather.ui.brand.rememberBirdMotionAllowed
import com.elkrrai.feather.ui.theme.Nest
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * The owl flying across a sky, seen from the side: in at the left, out at the right on a gentle
 * wave, nose up as it climbs, then round again. Clouds and hills far below hardly drift. The scene
 * is one rounded picture of sky, [width] by [height] (it can fill a screen: the board is 200 units
 * wide and as tall as the shape needs), that cuts the owl off at its edge; it reads in the light and
 * in the dark. With movement off, everything holds still with the owl half way across, wings down.
 */
@Composable
internal fun ClearSkiesScene(width: Dp, height: Dp, modifier: Modifier = Modifier) =
    SceneFrame(width, modifier, height = height) {
        val p = palette
        val moving = rememberBirdMotionAllowed()
        // the board is 200 units wide and this many tall
        val boardHeight = height / unit
        val dark = p.ground.luminance() < 0.5f
        val skyTop = if (dark) Color(0xFF0E1F2A) else lerp(Nest.SkyWing, Color.White, 0.3f)
        val skyBottom = if (dark) Color(0xFF1D3B4A) else lerp(Nest.SkyWing, Nest.Cream, 0.8f)
        val farClouds = rememberAmbientPhase(FAR_CLOUD_MILLIS, enabled = moving)
        val nearClouds = rememberAmbientPhase(NEAR_CLOUD_MILLIS, enabled = moving)
        val travel = rememberAmbientPhase(TRAVEL_MILLIS, enabled = moving)
        val farHills = rememberAmbientPhase(FAR_HILLS_MILLIS, enabled = moving)
        val nearHills = rememberAmbientPhase(NEAR_HILLS_MILLIS, enabled = moving)
        val restY = REST_HEIGHT * boardHeight

        val panel = Path().apply {
            addRoundRect(RoundRect(0f, 0f, SCENE_BOARD, boardHeight, CornerRadius(PANEL_RADIUS)))
        }
        Layer {
            clipPath(panel) {
                drawRect(Brush.verticalGradient(listOf(skyTop, skyBottom), startY = 0f, endY = boardHeight))
                val skyBodyY = 0.26f * boardHeight
                if (dark) drawMoon(p, Offset(146f, skyBodyY - 14f), 28f) else drawSun(p, Offset(160f, skyBodyY), 14f)
                // the far clouds, small and slow
                drawDriftingCloud(p, Offset(150f, 0.38f * boardHeight), 0.7f, farClouds.value)
                drawDriftingCloud(p, Offset(36f, 0.56f * boardHeight), 0.7f, farClouds.value)
            }
        }
        // two rows of hills, the far one higher up and slower; on the light sky they are paler
        HillStrip(boardHeight, R.drawable.hills_far, { farHills.value }, FAR_HILLS_LIFT, if (dark) 0.8f else 0.4f)
        HillStrip(boardHeight, R.drawable.hills_near, { nearHills.value }, 0f, if (dark) 1f else 0.55f)
        // The owl flies across the sky on a gentle wave, in at the left and out at the right, and
        // comes round again; the sky picture cuts it off at its rounded edge. Its box is placed where
        // the owl is half way across, and moved from there.
        Box(Modifier.size(unit * SCENE_BOARD, unit * boardHeight).clip(RoundedCornerShape(unit * PANEL_RADIUS))) {
            SideOwlFlight(
                modifier = Modifier
                    .offset(
                        x = unit * (SCENE_BOARD / 2f - OWL_ART_CENTRE_X * OWL_SIDE),
                        y = unit * (restY - OWL_ART_CENTRE_Y * OWL_SIDE),
                    )
                    .size(unit * OWL_SIDE)
                    .graphicsLayer {
                        val unitPx = size.width / OWL_SIDE
                        val t = if (moving) travel.value else HALF_WAY
                        translationX = (flightX(t) - SCENE_BOARD / 2f) * unitPx
                        translationY = flightWave(t) * unitPx
                        rotationZ = if (moving) flightPitch(t) else 0f
                    },
                moving = moving,
            )
        }
        Layer {
            clipPath(panel) {
                // the near clouds, larger and quicker, pass in front of the sky and the hills
                drawDriftingCloud(p, Offset(70f, 0.74f * boardHeight), 1.1f, nearClouds.value)
                drawDriftingCloud(p, Offset(170f, 0.14f * boardHeight), 1f, nearClouds.value)
            }
            drawRoundRect(
                p.ink.copy(alpha = 0.12f),
                size = Size(SCENE_BOARD, boardHeight),
                cornerRadius = CornerRadius(PANEL_RADIUS),
                style = Stroke(1.2f),
            )
        }
    }

/**
 * A cloud drifting left once across the picture for each [phase] of 0 to 1, coming back in from the
 * right: [start] is where it is at a phase of 0.
 */
private fun DrawScope.drawDriftingCloud(p: ScenePalette, start: Offset, size: Float, phase: Float) {
    val span = SCENE_BOARD + 2f * CLOUD_MARGIN
    val travelled = (start.x + CLOUD_MARGIN - phase * span) % span
    val x = (if (travelled < 0f) travelled + span else travelled) - CLOUD_MARGIN
    val center = Offset(x, start.y)
    scale(size, pivot = center) { drawCloud(p, center) }
}

/**
 * A row of hills (a 2000 by 496 picture) as wide as the sky picture, scrolling left for ever: each
 * [phase] of 0 to 1 moves it two picture widths. It is tiled one way up, then flipped, then the
 * first way again, so the joins always match even where the picture's own edges do not. [lift]
 * raises it by that many board units from the bottom of the sky, which is [boardHeight] tall.
 */
@Composable
private fun SceneScope.HillStrip(boardHeight: Float, drawable: Int, phase: () -> Float, lift: Float, alpha: Float) {
    Box(Modifier.size(unit * SCENE_BOARD, unit * boardHeight).clip(RoundedCornerShape(unit * PANEL_RADIUS))) {
        for (tile in 0..2) {
            Image(
                painter = painterResource(drawable),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                alpha = alpha,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(y = -(unit * lift))
                    .requiredSize(unit * SCENE_BOARD, unit * SCENE_BOARD * HILLS_HEIGHT_PER_WIDTH)
                    .graphicsLayer {
                        translationX = (tile - phase() * 2f) * size.width
                        scaleX = if (tile % 2 == 1) -1f else 1f
                    },
            )
        }
    }
}

/** One crossing of the sky, from off the left edge to off the right edge. */
private const val TRAVEL_MILLIS = 9_000

// The clouds and the hills hardly move: the owl is what crosses the sky.
private const val FAR_CLOUD_MILLIS = 120_000
private const val NEAR_CLOUD_MILLIS = 70_000
private const val FAR_HILLS_MILLIS = 400_000
private const val NEAR_HILLS_MILLIS = 260_000

/** Where the owl's middle is across the board, at [t] of a crossing (0 to 1). */
private fun flightX(t: Float): Float = FLIGHT_FROM_X + (FLIGHT_TO_X - FLIGHT_FROM_X) * t

/** How far above or below its middle height the owl is at [t]: a gentle wave. */
private fun flightWave(t: Float): Float = WAVE_HEIGHT * sin(2f * PI.toFloat() * WAVES * t)

/** The owl's tilt in degrees: nose up while it climbs, down while it sinks, half as steep as the path. */
private fun flightPitch(t: Float): Float {
    val rise = WAVE_HEIGHT * 2f * PI.toFloat() * WAVES * cos(2f * PI.toFloat() * WAVES * t)
    return atan2(rise, FLIGHT_TO_X - FLIGHT_FROM_X) * (180f / PI.toFloat()) * PITCH_FOLLOW
}

private const val FLIGHT_FROM_X = -70f
private const val FLIGHT_TO_X = 270f
private const val WAVE_HEIGHT = 10f
private const val WAVES = 2f
private const val PITCH_FOLLOW = 0.5f
private const val HALF_WAY = 0.5f

/** How far down the sky the owl's middle is, as a share of its height. */
private const val REST_HEIGHT = 0.48f

// The owl's picture is boxed 170 board units across; its body is centred a little left of and
// above the middle of that box.
private const val OWL_SIDE = 170f
private const val OWL_ART_CENTRE_X = 0.44f
private const val OWL_ART_CENTRE_Y = 0.5f

private const val FAR_HILLS_LIFT = 24f
private const val HILLS_HEIGHT_PER_WIDTH = 496f / 2000f
private const val PANEL_RADIUS = 12f
private const val CLOUD_MARGIN = 40f
