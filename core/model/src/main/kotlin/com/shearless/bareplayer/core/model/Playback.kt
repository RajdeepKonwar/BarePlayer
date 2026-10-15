package com.shearless.bareplayer.core.model

enum class RepeatMode { OFF, ALL, ONE }

/** A track as it appears in the live playback queue. */
data class QueueItem(
    val trackId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val artKey: ArtKey,
)

data class PlaybackState(
    val isConnected: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val current: QueueItem? = null,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    /** Queue items in the order they will play (shuffled order when shuffle is on). */
    val queue: List<QueueItem> = emptyList(),
    /** Index of [current] within [queue]. */
    val currentQueuePosition: Int = -1,
) {
    val hasMedia: Boolean get() = current != null
}

/**
 * Persisted queue state. [trackIds] is the playlist order; [shuffleOrder] lists indices into
 * [trackIds] in play order and is only meaningful when [shuffleEnabled] is true.
 */
data class QueueSnapshot(
    val trackIds: List<Long>,
    val currentIndex: Int,
    val positionMs: Long,
    val shuffleEnabled: Boolean,
    val shuffleOrder: List<Int>,
    val repeatMode: RepeatMode,
)
