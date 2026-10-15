package com.rajdeepkonwar.bareplayer.feature.player.queue

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Minimal drag-to-reorder state for a LazyColumn. The caller keeps a local copy of the list,
 * applies [onMove] swaps to it while dragging, and commits the overall move in [onDragEnd].
 */
@Stable
internal class ReorderState(
    private val listState: LazyListState,
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

    fun onDrag(delta: Float) {
        val index = draggingIndex ?: return
        dragOffset += delta
        val dragged = itemInfo(index) ?: return
        val center = dragged.offset + dragOffset + dragged.size / 2f
        val target = listState.layoutInfo.visibleItemsInfo.firstOrNull {
            it.index != index && center >= it.offset && center <= it.offset + it.size
        } ?: return
        onMove(index, target.index)
        // Keep the dragged row under the finger after it changes slots.
        dragOffset += dragged.offset - target.offset
        draggingIndex = target.index
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
        listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
}
