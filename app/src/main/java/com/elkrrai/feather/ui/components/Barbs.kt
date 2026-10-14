package com.elkrrai.feather.ui.components

import androidx.compose.ui.geometry.Offset
import com.elkrrai.feather.domain.model.Priority

/** How many small strokes ("barbs") a priority draws inside the completion ring: none, one, two or three. */
fun Priority.barbCount(): Int = when (this) {
    Priority.NONE -> 0
    Priority.LOW -> 1
    Priority.MEDIUM -> 2
    Priority.HIGH -> 3
}

/**
 * The strokes for [count] barbs, as start and end points in the ring's 26 by 26 drawing box: short
 * slanted ticks side by side, centered in the ring, like tally marks on a feather's vane.
 */
internal fun barbTicks(count: Int): List<Pair<Offset, Offset>> {
    if (count <= 0) return emptyList()
    val spacing = 3.2f
    val first = 13f - (count - 1) * spacing / 2f
    return List(count) { index ->
        val x = first + index * spacing
        Offset(x - 1.5f, 16.2f) to Offset(x + 1.5f, 9.8f)
    }
}
