package com.elkrrai.feather.ui.components

import androidx.compose.ui.graphics.Path

/** A leaf pointed at both ends, in a box from 0 to 1. Shared with the subtask leaves. */
internal fun leafShape(): Path = Path().apply {
    moveTo(0.12f, 0.88f)
    cubicTo(0.10f, 0.40f, 0.50f, 0.10f, 0.90f, 0.10f)
    cubicTo(0.92f, 0.55f, 0.60f, 0.90f, 0.12f, 0.88f)
    close()
}

/** A crescent moon, in a box from 0 to 1. */
internal fun moonShape(): Path = Path().apply {
    moveTo(0.62f, 0.08f)
    cubicTo(0.28f, 0.12f, 0.10f, 0.40f, 0.18f, 0.68f)
    cubicTo(0.28f, 0.90f, 0.58f, 0.98f, 0.84f, 0.80f)
    cubicTo(0.50f, 0.82f, 0.38f, 0.48f, 0.62f, 0.08f)
    close()
}
