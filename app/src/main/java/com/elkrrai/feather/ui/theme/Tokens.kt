package com.elkrrai.feather.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Shapes: pebbles and soft corners. Cards use `large`, the Today header and sheets `extraLarge`.
val NestShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
val SheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)

/** Chips and segmented options. */
val Pebble = CircleShape

/** The add button (60dp) and the send button (56dp). */
val FabShape = RoundedCornerShape(20.dp)

object Space {
    val x05 = 2.dp
    val x1 = 4.dp
    val x2 = 8.dp
    val x3 = 12.dp
    val x4 = 16.dp
    val x5 = 20.dp
    val x6 = 24.dp
    val x8 = 32.dp
    val x12 = 48.dp
    val x16 = 64.dp
    val gutter = 20.dp
    val rowV = 12.dp
    val rowH = 16.dp
    val touch = 48.dp
}

/** Elevation is tonal; shadows (warm brown) are only for sheets, menus and the add button, in the light theme. */
object Elev {
    val l0 = 0.dp
    val l1 = 1.dp
    val l2 = 6.dp
    val l3 = 12.dp
    val shadowAmbient = Color(0x123B2A10)
    val shadowSpot = Color(0x243B2A10)
}

/** Durations in milliseconds. */
object Dur {
    const val Micro = 90
    const val Short1 = 140
    const val Short2 = 200
    const val Medium1 = 280
    const val Medium2 = 360
    const val Long1 = 480

    /** The longest any celebration burst may run. */
    const val Burst = 700

    /** The once-a-day "all done" celebration; always interruptible. */
    const val LastTask = 1100
    const val Ambient = 4000
}

object Ease {
    /** Things arriving. */
    val Settle = CubicBezierEasing(0.20f, 0.00f, 0.00f, 1.00f)

    /** Things moving. */
    val Soft = CubicBezierEasing(0.30f, 0.00f, 0.20f, 1.00f)

    /** Things leaving. */
    val Leave = CubicBezierEasing(0.40f, 0.00f, 1.00f, 1.00f)
    val InOut = CubicBezierEasing(0.40f, 0.00f, 0.20f, 1.00f)

    /** The stroke of a check mark. */
    val Draw = CubicBezierEasing(0.50f, 0.00f, 0.10f, 1.00f)

    fun <T> nestle(): SpringSpec<T> = spring(dampingRatio = 0.70f, stiffness = 380f)
    fun <T> pop(): SpringSpec<T> = spring(dampingRatio = 0.55f, stiffness = 500f)
}
