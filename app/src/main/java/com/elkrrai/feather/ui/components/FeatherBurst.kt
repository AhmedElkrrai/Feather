package com.elkrrai.feather.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * One small leaf-feather in the burst. The numbers are fixed, not random, so every burst looks the
 * same and the layout can be tested.
 */
internal data class BurstParticle(
    /** The direction it flies in, in degrees; 0 is toward the end, -90 is up. */
    val angleDegrees: Float,
    /** How far it flies, in dp. */
    val distanceDp: Float,
    /** How far it spins on the way, in degrees (negative turns the other way). */
    val spinDegrees: Float,
    /** Which of the burst colors it takes. */
    val colorIndex: Int,
    /** Its length, in dp. */
    val sizeDp: Float,
)

private val Distances = listOf(44f, 34f, 52f, 28f, 48f, 38f, 46f)
private val Spins = listOf(35f, -30f, 40f, -38f, 25f, -40f, 30f)
private val Sizes = listOf(11f, 9f, 12f, 10f, 11f, 9f, 12f)

/** How far a feather falls while it flies, in dp. */
internal const val BURST_FALL_DP = 10f

/**
 * [count] feathers spread evenly around the ring, with the first one flying up. Every other one is
 * nudged a little so they do not look like the spokes of a wheel. [colors] is how many colors the
 * burst has; feathers take them in turn.
 */
internal fun burstParticles(count: Int, colors: Int = 4): List<BurstParticle> =
    List(count) { index ->
        BurstParticle(
            angleDegrees = -90f + index * 360f / count + if (index % 2 == 0) 0f else 14f,
            distanceDp = Distances[index % Distances.size],
            spinDegrees = Spins[index % Spins.size],
            colorIndex = index % colors,
            sizeDp = Sizes[index % Sizes.size],
        )
    }

/** Full strength for the first 60% of the burst, then a fade to nothing over the last 40%. */
internal fun burstAlpha(progress: Float): Float {
    val t = progress.coerceIn(0f, 1f)
    return if (t < 0.6f) 1f else ((1f - t) / 0.4f).coerceIn(0f, 1f)
}

/** A leaf pointed at both ends, in a box from -1 to 1 on both axes. */
private fun leafPath(): Path = Path().apply {
    moveTo(0f, -1f)
    cubicTo(0.55f, -0.35f, 0.55f, 0.35f, 0f, 1f)
    cubicTo(-0.55f, 0.35f, -0.55f, -0.35f, 0f, -1f)
    close()
}

/**
 * Draws a burst of leaf-feathers flying out of the middle of this element and fading away, while
 * [progress] runs from 0 to 1 (nothing is drawn at 0). It draws behind the content and outside the
 * element's bounds, so it costs no layout and no extra composables. Read [progress] from an
 * animation so each frame only redraws.
 */
internal fun Modifier.featherBurst(
    progress: () -> Float,
    particles: List<BurstParticle>,
    colors: List<Color>,
): Modifier = drawBehind {
    val t = progress()
    if (t <= 0f || t >= 1f) return@drawBehind
    val alpha = burstAlpha(t)
    val leaf = LeafPath
    val center = Offset(size.width / 2f, size.height / 2f)
    for (particle in particles) {
        val direction = particle.angleDegrees * (PI / 180.0)
        val distance = particle.distanceDp.dp.toPx() * t
        val x = center.x + (cos(direction) * distance).toFloat()
        val y = center.y + (sin(direction) * distance).toFloat() + BURST_FALL_DP.dp.toPx() * t
        val half = particle.sizeDp.dp.toPx() / 2f
        translate(left = x, top = y) {
            // The leaf points along the way it is flying, then turns as it goes.
            rotate(degrees = particle.angleDegrees + 90f + particle.spinDegrees * t, pivot = Offset.Zero) {
                scale(scaleX = half * 0.8f, scaleY = half, pivot = Offset.Zero) {
                    drawPath(leaf, colors[particle.colorIndex % colors.size].copy(alpha = alpha))
                }
            }
        }
    }
}

private val LeafPath = leafPath()

/** A soft ring that grows and fades: the quiet stand-in for the burst when motion is reduced. */
internal fun Modifier.halo(progress: () -> Float, color: Color): Modifier = drawBehind {
    val t = progress()
    if (t <= 0f || t >= 1f) return@drawBehind
    drawCircle(
        color = color.copy(alpha = 0.35f * (1f - t)),
        radius = (13f + 11f * t).dp.toPx(),
        center = Offset(size.width / 2f, size.height / 2f),
    )
}
