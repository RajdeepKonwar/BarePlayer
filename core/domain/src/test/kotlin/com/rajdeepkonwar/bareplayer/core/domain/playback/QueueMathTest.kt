package com.rajdeepkonwar.bareplayer.core.domain.playback

import com.rajdeepkonwar.bareplayer.core.model.QueueSnapshot
import com.rajdeepkonwar.bareplayer.core.model.RepeatMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QueueMathTest {

    @Test
    fun currentFirstShuffle_startsWithCurrentAndIsPermutation() {
        repeat(20) { seed ->
            val order = QueueMath.currentFirstShuffle(size = 10, currentIndex = 7, random = Random(seed))
            assertEquals(7, order.first())
            assertTrue(QueueMath.isValidOrder(order, 10))
        }
    }

    @Test
    fun currentFirstShuffle_handlesEmptyAndOutOfRangeCurrent() {
        assertEquals(emptyList<Int>(), QueueMath.currentFirstShuffle(0, 0))
        assertTrue(QueueMath.isValidOrder(QueueMath.currentFirstShuffle(3, -1), 3))
    }

    @Test
    fun isValidOrder_rejectsDuplicatesWrongSizeAndOutOfRange() {
        assertTrue(QueueMath.isValidOrder(listOf(2, 0, 1), 3))
        assertFalse(QueueMath.isValidOrder(listOf(0, 0, 1), 3))
        assertFalse(QueueMath.isValidOrder(listOf(0, 1), 3))
        assertFalse(QueueMath.isValidOrder(listOf(0, 1, 3), 3))
    }

    @Test
    fun playOrder_fallsBackToIdentityWhenShuffleOffOrInvalid() {
        assertEquals(listOf(0, 1, 2), QueueMath.playOrder(3, shuffleEnabled = false, listOf(2, 1, 0)))
        assertEquals(listOf(2, 1, 0), QueueMath.playOrder(3, shuffleEnabled = true, listOf(2, 1, 0)))
        assertEquals(listOf(0, 1, 2), QueueMath.playOrder(3, shuffleEnabled = true, listOf(2, 2, 0)))
    }

    @Test
    fun move_movesItemAndIgnoresInvalidIndices() {
        assertEquals(listOf("b", "c", "a"), QueueMath.move(listOf("a", "b", "c"), 0, 2))
        assertEquals(listOf("c", "a", "b"), QueueMath.move(listOf("a", "b", "c"), 2, 0))
        assertEquals(listOf("a", "b"), QueueMath.move(listOf("a", "b"), 0, 5))
    }

    @Test
    fun insert_shiftsExistingIndicesAndPlacesNewItemsAtPlayPosition() {
        // Playlist [A B C] played as C A B; two items inserted at playlist index 1, to play right after C.
        val result = QueueMath.insert(order = listOf(2, 0, 1), insertedAt = 1, count = 2, playPosition = 1)
        assertEquals(listOf(4, 1, 2, 0, 3), result)
        assertTrue(QueueMath.isValidOrder(result, 5))
    }

    @Test
    fun insert_atEndAppends() {
        assertEquals(listOf(1, 0, 2), QueueMath.insert(listOf(1, 0), insertedAt = 2, count = 1, playPosition = 99))
    }

    @Test
    fun remove_dropsIndexAndCompacts() {
        assertEquals(listOf(1, 0), QueueMath.remove(listOf(2, 0, 1), removedIndex = 0))
        assertEquals(listOf(0, 1), QueueMath.remove(listOf(0, 2, 1), removedIndex = 2))
    }

    @Test
    fun nextRepeatMode_cycles() {
        assertEquals(RepeatMode.ALL, QueueMath.nextRepeatMode(RepeatMode.OFF))
        assertEquals(RepeatMode.ONE, QueueMath.nextRepeatMode(RepeatMode.ALL))
        assertEquals(RepeatMode.OFF, QueueMath.nextRepeatMode(RepeatMode.ONE))
    }

    private fun snapshot(
        ids: List<Long>,
        current: Int,
        shuffle: Boolean = false,
        order: List<Int> = emptyList(),
    ) = QueueSnapshot(ids, current, positionMs = 5_000, shuffle, order, RepeatMode.ALL)

    @Test
    fun restore_keepsEverythingWhenAllTracksExist() {
        val s = snapshot(listOf(10, 20, 30), current = 1, shuffle = true, order = listOf(1, 2, 0))
        assertEquals(s, QueueMath.restore(s, setOf(10, 20, 30)))
    }

    @Test
    fun restore_returnsNullWhenNothingRemains() {
        assertNull(QueueMath.restore(snapshot(listOf(1, 2), 0), emptySet()))
    }

    @Test
    fun restore_remapsIndicesWhenTracksAreMissing() {
        val s = snapshot(listOf(10, 20, 30, 40), current = 2, shuffle = true, order = listOf(2, 0, 3, 1))
        val restored = QueueMath.restore(s, setOf(10, 30, 40))!!
        assertEquals(listOf(10L, 30L, 40L), restored.trackIds)
        assertEquals(1, restored.currentIndex)
        assertEquals(5_000L, restored.positionMs)
        assertEquals(listOf(1, 0, 2), restored.shuffleOrder)
    }

    @Test
    fun restore_movesToNextTrackWhenCurrentIsMissing() {
        val restored = QueueMath.restore(snapshot(listOf(10, 20, 30), current = 1), setOf(10, 30))!!
        assertEquals(listOf(10L, 30L), restored.trackIds)
        assertEquals(1, restored.currentIndex)
        assertEquals(0L, restored.positionMs)
    }
}
