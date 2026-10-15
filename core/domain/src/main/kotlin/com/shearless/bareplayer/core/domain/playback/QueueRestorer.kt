package com.shearless.bareplayer.core.domain.playback

import com.shearless.bareplayer.core.domain.repository.LibraryRepository
import com.shearless.bareplayer.core.domain.repository.QueueRepository
import com.shearless.bareplayer.core.model.QueueSnapshot
import com.shearless.bareplayer.core.model.Track

/** A persisted queue resolved against the current library; [tracks] align with [snapshot].trackIds. */
data class RestoredQueue(val snapshot: QueueSnapshot, val tracks: List<Track>)

class QueueRestorer(
    private val queueRepository: QueueRepository,
    private val libraryRepository: LibraryRepository,
) {
    /** Loads the last queue, dropping tracks that were deleted since. Null when nothing is playable. */
    suspend fun restore(): RestoredQueue? {
        val saved = queueRepository.load() ?: return null
        val available = libraryRepository.tracksByIds(saved.trackIds.distinct())
        val byId = available.associateBy { it.id }
        val snapshot = QueueMath.restore(saved, byId.keys) ?: return null
        return RestoredQueue(snapshot, snapshot.trackIds.map(byId::getValue))
    }
}
