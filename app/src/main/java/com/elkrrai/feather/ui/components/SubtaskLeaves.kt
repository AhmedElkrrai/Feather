package com.elkrrai.feather.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** How many leaves are drawn, and how many of them are filled. */
internal data class LeafProgress(val shown: Int, val filled: Int)

/**
 * Subtask progress as at most [max] leaves. With up to [max] subtasks there is one leaf each; with
 * more, the leaves show the share that is done. A filled leaf never appears for nothing done, and a
 * full row of filled leaves only when everything is.
 */
internal fun leafProgress(total: Int, done: Int, max: Int = 5): LeafProgress {
    if (total <= 0) return LeafProgress(0, 0)
    val completed = done.coerceIn(0, total)
    val shown = minOf(total, max)
    val filled = if (total <= max) {
        completed
    } else {
        (completed * shown / total.toFloat()).roundToInt().let {
            when {
                completed == total -> shown
                completed == 0 -> 0
                else -> it.coerceIn(1, shown - 1)
            }
        }
    }
    return LeafProgress(shown, filled)
}

/**
 * A row of small leaves, filled for subtasks that are done and outlined for those still open.
 * Decoration; the text "2 of 4" next to it carries the meaning.
 */
@Composable
fun SubtaskLeaves(done: Int, total: Int, color: Color, modifier: Modifier = Modifier) {
    val progress = leafProgress(total, done)
    if (progress.shown == 0) return
    val leaf = remember { leafShape() }
    Row(
        modifier = modifier.clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(progress.shown) { index ->
            Canvas(Modifier.size(11.dp)) {
                scale(this.size.width, pivot = Offset.Zero) {
                    drawLeaf(leaf, color, filled = index < progress.filled)
                }
            }
        }
    }
}
