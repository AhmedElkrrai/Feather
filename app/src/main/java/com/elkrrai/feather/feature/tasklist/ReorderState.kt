package com.elkrrai.feather.feature.tasklist

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.model.Task
import com.elkrrai.feather.ui.Haptics

/**
 * Drag-to-reorder for the rows of a single [LazyListState] list.
 *
 * While a row is dragged, the list shown on screen is [order], a local copy that is reordered
 * as the row passes its neighbors; when the finger lifts, the new order of ids goes to
 * [onCommit] and the screen goes back to showing the saved order. The list must hold only
 * these rows (no headers), so a row's index in the list is its index in [order].
 */
@Stable
class ReorderState(
    private val listState: LazyListState,
    private val order: SnapshotStateList<Task>,
    private val haptics: Haptics,
    private val onCommit: (List<String>) -> Unit,
) {
    /** The row being dragged, or null. */
    var draggingId by mutableStateOf<String?>(null)
        private set

    /** How far the dragged row is from where the list lays it out, in pixels. */
    var offset by mutableFloatStateOf(0f)
        private set

    fun start(id: String) {
        draggingId = id
        offset = 0f
        haptics.longPress()
    }

    fun drag(delta: Float) {
        val id = draggingId ?: return
        offset += delta

        val info = listState.layoutInfo
        val current = info.visibleItemsInfo.firstOrNull { it.key == id } ?: return
        val center = current.offset + offset + current.size / 2f

        // Near the top or bottom edge the list scrolls along, so a long list can be crossed.
        if (center < info.viewportStartOffset + EDGE_PX) {
            listState.dispatchRawDelta(-SCROLL_STEP_PX)
            offset -= SCROLL_STEP_PX
        } else if (center > info.viewportEndOffset - EDGE_PX) {
            listState.dispatchRawDelta(SCROLL_STEP_PX)
            offset += SCROLL_STEP_PX
        }

        val target = info.visibleItemsInfo.firstOrNull {
            it.key != id && center >= it.offset && center <= it.offset + it.size
        } ?: return
        val from = order.indexOfFirst { it.id == id }
        val to = order.indexOfFirst { it.id == target.key }
        if (from < 0 || to < 0) return

        order.add(to, order.removeAt(from))
        // The dragged row now sits where the neighbor was; keep it under the finger.
        offset += current.offset - target.offset
        haptics.tick()
    }

    fun end() {
        val id = draggingId ?: return
        draggingId = null
        offset = 0f
        if (id.isNotEmpty()) onCommit(order.map { it.id })
    }

    /** Moves a row one place up or down without dragging (the TalkBack actions "Move up" and "Move down"). */
    fun moveBy(id: String, steps: Int) {
        val from = order.indexOfFirst { it.id == id }
        val to = from + steps
        if (from < 0 || to !in order.indices) return
        order.add(to, order.removeAt(from))
        onCommit(order.map { it.id })
    }

    private companion object {
        const val EDGE_PX = 120f
        const val SCROLL_STEP_PX = 24f
    }
}

/** The grip at the end of a row; dragging it up or down moves the row. */
@Composable
fun DragHandle(reorder: ReorderState, taskId: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .pointerInput(taskId, reorder) {
                detectVerticalDragGestures(
                    onDragStart = { reorder.start(taskId) },
                    onVerticalDrag = { change, amount ->
                        change.consume()
                        reorder.drag(amount)
                    },
                    onDragEnd = { reorder.end() },
                    onDragCancel = { reorder.end() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_drag_handle),
            contentDescription = stringResource(R.string.reorder_handle),
            modifier = Modifier.size(24.dp),
        )
    }
}
