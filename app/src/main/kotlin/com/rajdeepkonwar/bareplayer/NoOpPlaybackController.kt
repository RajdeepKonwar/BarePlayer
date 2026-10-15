package com.rajdeepkonwar.bareplayer

import com.rajdeepkonwar.bareplayer.core.domain.playback.PlaybackController
import com.rajdeepkonwar.bareplayer.core.model.PlaybackState
import com.rajdeepkonwar.bareplayer.core.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

/** Placeholder until the Media3 playback service lands. */
internal object NoOpPlaybackController : PlaybackController {
    override val state: StateFlow<PlaybackState> = MutableStateFlow(PlaybackState())
    override val position: Flow<Long> = flowOf(0L)
    override fun playTracks(tracks: List<Track>, startIndex: Int, shuffle: Boolean) = Unit
    override fun playNext(tracks: List<Track>) = Unit
    override fun addToQueue(tracks: List<Track>) = Unit
    override fun togglePlayPause() = Unit
    override fun skipToNext() = Unit
    override fun skipToPrevious() = Unit
    override fun seekTo(positionMs: Long) = Unit
    override fun setShuffleEnabled(enabled: Boolean) = Unit
    override fun cycleRepeatMode() = Unit
    override fun playQueuePosition(position: Int) = Unit
    override fun moveQueueItem(from: Int, to: Int) = Unit
    override fun removeQueueItem(position: Int) = Unit
}
