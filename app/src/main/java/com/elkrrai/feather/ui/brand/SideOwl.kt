package com.elkrrai.feather.ui.brand

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import com.elkrrai.feather.R
import kotlin.math.PI
import kotlin.math.sin

/**
 * The owl in flight seen from the side, facing right, beating its wings for as long as movement is
 * allowed. Its three layers (`owl_side_body`, `owl_side_wing_down`, `owl_side_wing_up`) share one
 * artboard with the wings already set on the owl's shoulder, so the movement is only a hinge there:
 * each wing turns about the shoulder while the raised drawing fades in over the lowered one.
 *
 * A beat is a slow raise (the wings fold in a little as they go) and a quick downstroke. On the
 * downstroke the owl lifts and tips its nose up; on the raise it sinks and levels. A second wing,
 * darker and a little more raised, shows behind the body. Without movement the wings hang down.
 */
@Composable
internal fun SideOwlFlight(
    modifier: Modifier = Modifier,
    moving: Boolean = rememberBirdMotionAllowed(),
    beatMillis: Int = SIDE_BEAT_MILLIS,
) {
    val beat = rememberAmbientPhase(beatMillis, enabled = moving)
    // 0 is wings down, 1 is wings up
    fun flap(): Float = if (moving) flapAt(beat.value) else 0f
    fun fold(): Float = if (moving) foldAt(beat.value) else 0f
    val farWing = remember { ColorFilter.tint(Color.Black.copy(alpha = FAR_WING_SHADE), BlendMode.SrcAtop) }

    Box(
        modifier
            .fillMaxSize()
            .graphicsLayer {
                // lifted and nose up with the wings down, sunk and nose down with them raised
                translationY = size.height * SIDE_BOB * (flap() - 0.5f)
                transformOrigin = SIDE_BODY_CENTRE
                rotationZ = SIDE_PITCH_DEGREES * (flap() - 0.5f)
            },
    ) {
        // the far wing, behind the body
        SideWings(::flap, ::fold, extraDegrees = FAR_WING_LIFT, colorFilter = farWing)
        OwlLayer(R.drawable.owl_side_body)
        SideWings(::flap, ::fold, extraDegrees = 0f, colorFilter = null)
    }
}

@Composable
private fun SideWings(flap: () -> Float, fold: () -> Float, extraDegrees: Float, colorFilter: ColorFilter?) {
    Box(Modifier.fillMaxSize()) {
        OwlLayer(
            R.drawable.owl_side_wing_down,
            Modifier.graphicsLayer {
                transformOrigin = SIDE_SHOULDER
                rotationZ = flap() * SIDE_SWEEP_DEGREES + extraDegrees
                scaleX = 1f - WING_FOLD * fold()
                scaleY = 1f - WING_FOLD * fold()
                alpha = 1f - smoothStep(0.7f, 1f, flap())
            },
            colorFilter,
        )
        OwlLayer(
            R.drawable.owl_side_wing_up,
            Modifier.graphicsLayer {
                transformOrigin = SIDE_SHOULDER
                rotationZ = -(1f - flap()) * SIDE_SWEEP_DEGREES + extraDegrees
                scaleX = 1f - WING_FOLD * fold()
                scaleY = 1f - WING_FOLD * fold()
                alpha = smoothStep(0.3f, 0.7f, flap())
            },
            colorFilter,
        )
    }
}

/** How far up the wings are, 0 (hanging) to 1 (raised), at [phase] of a beat: a slow raise, a quick drop. */
internal fun flapAt(phase: Float): Float =
    if (phase < RAISE_SHARE) {
        smoothStep(0f, 1f, phase / RAISE_SHARE)
    } else {
        1f - smoothStep(0f, 1f, (phase - RAISE_SHARE) / (1f - RAISE_SHARE))
    }

/** How much the wings are folded in at [phase] of a beat: most half way up the raise, none on the downstroke. */
internal fun foldAt(phase: Float): Float =
    if (phase < RAISE_SHARE) sin(PI.toFloat() * phase / RAISE_SHARE) else 0f

/** One wing beat, in milliseconds. */
private const val SIDE_BEAT_MILLIS = 800

/** The raise takes this share of a beat; the downstroke takes the rest. */
private const val RAISE_SHARE = 0.58f

/** How far a wing turns between hanging and fully raised: the two drawings are 70 degrees apart. */
private const val SIDE_SWEEP_DEGREES = 70f

/** How much a wing shrinks about the shoulder at the middle of the raise. */
private const val WING_FOLD = 0.14f

/** Where the wings join the body, as shares of the artboard (856 and 736 of 1536). */
private val SIDE_SHOULDER = TransformOrigin(0.5573f, 0.4792f)

/** About the middle of the body, which the whole owl tips around. */
private val SIDE_BODY_CENTRE = TransformOrigin(0.44f, 0.5f)

/** How far the owl rises and sinks with each beat, as a share of its height. */
private const val SIDE_BOB = 0.05f

/** How far the nose tips between the top and the bottom of the beat. */
private const val SIDE_PITCH_DEGREES = 9f

private const val FAR_WING_SHADE = 0.45f
private const val FAR_WING_LIFT = 12f
