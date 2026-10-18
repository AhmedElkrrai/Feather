package com.elkrrai.feather.ui.brand

import kotlin.math.PI
import kotlin.math.cos
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
    /** The head turning on the neck; a negative angle lifts the beak. */
    val headTilt: Float = 0f,
    /** The tail turning at its root; a positive angle lifts the tip. */
    val tailFlick: Float = 0f,
    /** How far the bird has moved to the right of where it sits, in the units of [BirdShapes]. */
    val bodyOffsetX: Float = 0f,
    /** 1 faces right, -1 faces left; the values between are the bird turning round. */
    val facing: Float = 1f,
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
        // One idle cycle is long (see periodMillis): the breathing and bobbing go round twice in it,
        // there are two blinks, and once a cycle each the head tilts to look up and the tail flicks.
        BirdMood.IDLE -> BirdPose(
            wingAngle = -3f * sin(2f * angle),
            bodyOffsetY = 1.2f * sin(2f * angle),
            eyeOpen = minOf(blink(p), blink((p + 0.5f) % 1f)),
            bodyScale = 1f,
            tilt = 0f,
            headTilt = HEAD_TILT * envelope(p, HEAD_START, HEAD_END),
            tailFlick = TAIL_FLICK * envelope(p, TAIL_START, TAIL_END) * sin(2f * TAIL_WAGS * PI.toFloat() * windowPosition(p, TAIL_START, TAIL_END)),
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

/** The whole flight (crouch, one lap in the air, landing) lasts this long. */
const val FLIGHT_MILLIS = 2200

/** How far across the lap's circle is (the loop is this many units wide in every direction). */
internal const val FLIGHT_RADIUS = 40f

private const val FLIGHT_CROUCH_END = 0.10f
private const val FLIGHT_LAP_END = 0.80f
private const val FLIGHT_WING_BEATS = 7f

/**
 * The bird flying a lap over its perch and landing again, [progress] of the way through
 * ([FLIGHT_MILLIS]). It crouches, takes off to the right, goes up and round in a circle (counter
 * clockwise on the screen, so it always flies nose first: it turns round over the top), comes down
 * with its wings flared and settles with a content squint. Pure math like [birdPose]. At 0 and at 1 it
 * is the calm pose, so the idle movement can take over without a jump.
 */
fun birdFlightPose(progress: Float): BirdPose {
    val p = progress.coerceIn(0f, 1f)
    return when {
        p < FLIGHT_CROUCH_END -> {
            val t = p / FLIGHT_CROUCH_END
            val dip = sin(PI * t).toFloat()
            BirdPose(
                wingAngle = -12f * dip,
                bodyOffsetY = 2.5f * dip,
                eyeOpen = 1f,
                bodyScale = 1f - 0.04f * dip,
                tilt = 0f,
            )
        }

        p < FLIGHT_LAP_END -> {
            val t = (p - FLIGHT_CROUCH_END) / (FLIGHT_LAP_END - FLIGHT_CROUCH_END)
            // slow at take-off and at landing, quickest over the top
            val lap = t * t * (3f - 2f * t)
            val phi = (2.0 * PI * lap).toFloat()
            val facing = (2.5f * cos(phi)).coerceIn(-1f, 1f)
            BirdPose(
                wingAngle = 30f * sin((2.0 * PI * FLIGHT_WING_BEATS * t).toFloat()),
                bodyOffsetY = -FLIGHT_RADIUS + FLIGHT_RADIUS * cos(phi),
                eyeOpen = 1f,
                bodyScale = 1f,
                // nose up while climbing, nose down while coming down
                tilt = -12f * sin(phi) * facing,
                tailFlick = 7f * sin(phi),
                bodyOffsetX = FLIGHT_RADIUS * sin(phi),
                facing = facing,
            )
        }

        else -> {
            val t = (p - FLIGHT_LAP_END) / (1f - FLIGHT_LAP_END)
            val touchdown = if (t < 0.6f) sin(PI * t / 0.6).toFloat() else 0f
            BirdPose(
                wingAngle = -24f * sin(PI * t).toFloat() * (1f - t),
                bodyOffsetY = 2.2f * touchdown,
                eyeOpen = 1f - 0.55f * sin(PI * t).toFloat(),
                bodyScale = 1f - 0.03f * touchdown,
                tilt = 0f,
            )
        }
    }
}

/** A quick blink near the end of each cycle: open, shut for a moment, open again. */
private fun blink(p: Float): Float {
    if (p < BLINK_START || p > BLINK_END) return 1f
    val x = (p - BLINK_START) / (BLINK_END - BLINK_START)
    return (1f - 0.95f * sin((PI * x).toFloat())).coerceIn(0f, 1f)
}

/** Where [p] is inside the window from [start] to [end], 0 to 1 (0 outside it). */
private fun windowPosition(p: Float, start: Float, end: Float): Float =
    if (p < start || p > end) 0f else (p - start) / (end - start)

/** A soft hill over the window from [start] to [end]: 0 outside, easing up to 1 and back to 0. */
private fun envelope(p: Float, start: Float, end: Float): Float {
    if (p < start || p > end) return 0f
    val s = sin(PI * windowPosition(p, start, end)).toFloat().coerceIn(0f, 1f)
    return s * s * (3f - 2f * s)
}

private const val HEAD_TILT = -11f
private const val HEAD_START = 0.14f
private const val HEAD_END = 0.38f
private const val TAIL_FLICK = 10f
private const val TAIL_START = 0.60f
private const val TAIL_END = 0.74f
private const val TAIL_WAGS = 2

private const val HOP_HEIGHT = 14f
private const val SLEEP_TILT = 5f
private const val BLINK_START = 0.90f
private const val BLINK_END = 0.96f
