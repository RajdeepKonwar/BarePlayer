package com.shearless.bareplayer.core.domain.playback

import com.shearless.bareplayer.core.model.QueueSnapshot
import com.shearless.bareplayer.core.model.RepeatMode
import kotlin.random.Random

/**
 * Pure queue logic. A "play order" is a permutation of playlist indices listing the order in which
 * items play; with shuffle off it is the identity.
 */
object QueueMath {

    /** A random play order that starts with [currentIndex], so enabling shuffle never skips the song. */
    fun currentFirstShuffle(size: Int, currentIndex: Int, random: Random = Random.Default): List<Int> {
        if (size <= 0) return emptyList()
        val rest = (0 until size).filter { it != currentIndex }.shuffled(random)
        return if (currentIndex in 0 until size) listOf(currentIndex) + rest else rest
    }

    fun isValidOrder(order: List<Int>, size: Int): Boolean {
        if (order.size != size) return false
        val seen = BooleanArray(size)
        for (index in order) {
            if (index !in 0 until size || seen[index]) return false
            seen[index] = true
        }
        return true
    }

    fun playOrder(size: Int, shuffleEnabled: Boolean, shuffleOrder: List<Int>): List<Int> =
        if (shuffleEnabled && isValidOrder(shuffleOrder, size)) shuffleOrder else List(size) { it }

    fun <T> move(list: List<T>, from: Int, to: Int): List<T> {
        if (from == to || from !in list.indices || to !in list.indices) return list
        val result = list.toMutableList()
        result.add(to, result.removeAt(from))
        return result
    }

    /**
     * Updates a play order after [count] items were inserted into the playlist at [insertedAt].
     * The new items are placed at [playPosition] in the play order, in insertion order.
     */
    fun insert(order: List<Int>, insertedAt: Int, count: Int, playPosition: Int): List<Int> {
        if (count <= 0) return order
        val shifted = order.map { if (it >= insertedAt) it + count else it }.toMutableList()
        val position = playPosition.coerceIn(0, shifted.size)
        shifted.addAll(position, List(count) { insertedAt + it })
        return shifted
    }

    /** Updates a play order after the playlist item at [removedIndex] was removed. */
    fun remove(order: List<Int>, removedIndex: Int): List<Int> =
        order.filter { it != removedIndex }.map { if (it > removedIndex) it - 1 else it }

    fun nextRepeatMode(mode: RepeatMode): RepeatMode = when (mode) {
        RepeatMode.OFF -> RepeatMode.ALL
        RepeatMode.ALL -> RepeatMode.ONE
        RepeatMode.ONE -> RepeatMode.OFF
    }

    /**
     * Drops tracks that no longer exist from a persisted queue and remaps the current index and
     * shuffle order. Returns null when nothing playable remains.
     */
    fun restore(snapshot: QueueSnapshot, available: Set<Long>): QueueSnapshot? {
        val keptOldIndices = snapshot.trackIds.indices.filter { snapshot.trackIds[it] in available }
        if (keptOldIndices.isEmpty()) return null
        if (keptOldIndices.size == snapshot.trackIds.size) {
            return snapshot.copy(
                currentIndex = snapshot.currentIndex.coerceIn(0, snapshot.trackIds.lastIndex),
                shuffleOrder = playOrder(snapshot.trackIds.size, snapshot.shuffleEnabled, snapshot.shuffleOrder),
            )
        }
        val oldToNew = IntArray(snapshot.trackIds.size) { -1 }
        keptOldIndices.forEachIndexed { newIndex, oldIndex -> oldToNew[oldIndex] = newIndex }

        val currentStillExists = snapshot.currentIndex in snapshot.trackIds.indices &&
            oldToNew[snapshot.currentIndex] >= 0
        val newCurrent = if (currentStillExists) {
            oldToNew[snapshot.currentIndex]
        } else {
            // The current track vanished: continue from the next surviving track in playlist order.
            keptOldIndices.firstOrNull { it > snapshot.currentIndex }?.let { oldToNew[it] } ?: 0
        }
        val oldOrder = playOrder(snapshot.trackIds.size, snapshot.shuffleEnabled, snapshot.shuffleOrder)
        val newOrder = oldOrder.map { oldToNew[it] }.filter { it >= 0 }
        return QueueSnapshot(
            trackIds = keptOldIndices.map { snapshot.trackIds[it] },
            currentIndex = newCurrent,
            positionMs = if (currentStillExists) snapshot.positionMs else 0L,
            shuffleEnabled = snapshot.shuffleEnabled,
            shuffleOrder = newOrder,
            repeatMode = snapshot.repeatMode,
        )
    }
}
