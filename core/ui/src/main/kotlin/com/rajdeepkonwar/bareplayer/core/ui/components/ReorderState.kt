package com.rajdeepkonwar.bareplayer.core.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Minimal drag-to-reorder state for a LazyColumn. The caller keeps a local copy of the list,
 * applies [onMove] swaps to it while dragging, and commits the overall move when the drag ends.
 * [indexOffset] is the number of non-reorderable items (e.g. headers) before the first row.
 */
@Stable
class ReorderState(
    private val listState: LazyListState,
    private val indexOffset: Int = 0,
    private val onMove: (from: Int, to: Int) -> Unit,
) {
    var draggingIndex by mutableStateOf<Int?>(null)
        private set
    var dragOffset by mutableFloatStateOf(0f)
        private set
    private var startIndex: Int? = null

    fun onDragStart(index: Int) {
        draggingIndex = index
        startIndex = index
        dragOffset = 0f
    }

    fun onDrag(delta: Float, itemCount: Int) {
        val index = draggingIndex ?: return
        dragOffset += delta
        val dragged = itemInfo(index) ?: return
        val center = dragged.offset + dragOffset + dragged.size / 2f
        val target = listState.layoutInfo.visibleItemsInfo.firstOrNull {
            val row = it.index - indexOffset
            row != index && row in 0 until itemCount && center >= it.offset && center <= it.offset + it.size
        } ?: return
        onMove(index, target.index - indexOffset)
        // Keep the dragged row under the finger after it changes slots.
        dragOffset += dragged.offset - target.offset
        draggingIndex = target.index - indexOffset
    }

    /** Returns the (from, to) of the whole gesture, or null if nothing moved. */
    fun onDragEnd(): Pair<Int, Int>? {
        val from = startIndex
        val to = draggingIndex
        draggingIndex = null
        startIndex = null
        dragOffset = 0f
        return if (from != null && to != null && from != to) from to to else null
    }

    private fun itemInfo(index: Int): LazyListItemInfo? =
        listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index + indexOffset }
}

/** Drag-handle gesture for row [index]; [onMoved] receives the committed (from, to). */
fun Modifier.reorderHandle(
    state: ReorderState,
    key: Any,
    index: Int,
    itemCount: Int,
    onMoved: (from: Int, to: Int) -> Unit,
): Modifier = pointerInput(key) {
    detectDragGestures(
        onDragStart = { state.onDragStart(index) },
        onDragEnd = { state.onDragEnd()?.let { (from, to) -> onMoved(from, to) } },
        onDragCancel = { state.onDragEnd()?.let { (from, to) -> onMoved(from, to) } },
    ) { change, amount ->
        change.consume()
        state.onDrag(amount.y, itemCount)
    }
}
