package com.elkrrai.feather.ui.brand

import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

/** What the bird is doing. */
enum class BirdMood {
    /** Sitting, bobbing a little, blinking now and then. */
    IDLE,

    /** Eyes closed, breathing slowly. */
    SLEEPING,

    /** One hop with flapping wings. */
    HAPPY,

    /** Flapping on the spot. */
    FLYING,
}

/**
 * How the bird looks at one moment. Angles are in degrees and offsets in the 96 by 96 units of
 * [BirdShapes]; a negative wing angle lifts the wing and a negative offset moves the bird up.
 */
data class BirdPose(
    val wingAngle: Float,
    val bodyOffsetY: Float,
    /** 0 is closed, 1 is wide open. */
    val eyeOpen: Float,
    val bodyScale: Float,
    val tilt: Float,
)

/**
 * The pose of a bird in [mood] at [phase], where one whole cycle of the movement is a phase of
 * 1 (any value is accepted; only the fractional part counts). Pure math, no drawing, so it is
 * tested without a screen. A phase of 0 is a calm, still pose in every mood, which is what is
 * shown when movement is switched off.
 */
fun birdPose(mood: BirdMood, phase: Float): BirdPose {
    val p = phase - floor(phase)
    val angle = (2.0 * PI * p).toFloat()
    return when (mood) {
        BirdMood.IDLE -> BirdPose(
            wingAngle = -3f * sin(angle),
            bodyOffsetY = 1.2f * sin(angle),
            eyeOpen = blink(p),
            bodyScale = 1f,
            tilt = 0f,
        )

        BirdMood.SLEEPING -> BirdPose(
            wingAngle = 0f,
            bodyOffsetY = 0f,
            eyeOpen = 0f,
            bodyScale = 1f + 0.025f * sin(angle),
            tilt = SLEEP_TILT,
        )

        BirdMood.HAPPY -> {
            val hop = sin((PI * p).toFloat())
            BirdPose(
                wingAngle = -30f * sin(3f * angle),
                bodyOffsetY = -HOP_HEIGHT * hop,
                eyeOpen = 0.4f,
                bodyScale = 1f,
                tilt = -6f * hop,
            )
        }

        BirdMood.FLYING -> BirdPose(
            wingAngle = 28f * sin(2f * angle),
            bodyOffsetY = 3f * sin(angle),
            eyeOpen = 1f,
            bodyScale = 1f,
            tilt = -8f,
        )
    }
}

/** A quick blink near the end of each cycle: open, shut for a moment, open again. */
private fun blink(p: Float): Float {
    if (p < BLINK_START || p > BLINK_END) return 1f
    val x = (p - BLINK_START) / (BLINK_END - BLINK_START)
    return (1f - 0.95f * sin((PI * x).toFloat())).coerceIn(0f, 1f)
}

private const val HOP_HEIGHT = 14f
private const val SLEEP_TILT = 5f
private const val BLINK_START = 0.90f
private const val BLINK_END = 0.96f
