package com.rajdeepkonwar.bareplayer.core.domain.playback

import com.rajdeepkonwar.bareplayer.core.model.PlaybackState
import com.rajdeepkonwar.bareplayer.core.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-neutral playback API used by the UI. Queue positions are always in play order, i.e. the
 * order shown to the user (the shuffled order when shuffle is on).
 */
interface PlaybackController {
    val state: StateFlow<PlaybackState>

    /** Playback position in ms, emitted periodically while collected. */
    val position: Flow<Long>

    /** One-off, user-facing playback problems (e.g. a track whose format the device can't decode). */
    val errors: Flow<PlaybackError>

    fun playTracks(tracks: List<Track>, startIndex: Int = 0, shuffle: Boolean = false)
    fun playNext(tracks: List<Track>)
    fun addToQueue(tracks: List<Track>)

    fun togglePlayPause()
    fun skipToNext()
    fun skipToPrevious()
    fun seekTo(positionMs: Long)
    fun setShuffleEnabled(enabled: Boolean)
    fun cycleRepeatMode()

    fun playQueuePosition(position: Int)
    fun moveQueueItem(from: Int, to: Int)
    fun removeQueueItem(position: Int)
}
