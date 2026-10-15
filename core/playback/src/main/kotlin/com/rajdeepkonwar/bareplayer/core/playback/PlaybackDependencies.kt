package com.rajdeepkonwar.bareplayer.core.playback

import com.rajdeepkonwar.bareplayer.core.domain.repository.LibraryRepository
import com.rajdeepkonwar.bareplayer.core.domain.repository.PlayHistoryRepository
import com.rajdeepkonwar.bareplayer.core.domain.repository.QueueRepository
import kotlinx.coroutines.CoroutineScope

/** What [PlaybackService] needs from the app; the Application implements [Provider]. */
interface PlaybackDependencies {
    val libraryRepository: LibraryRepository
    val queueRepository: QueueRepository
    val playHistoryRepository: PlayHistoryRepository

    /** Outlives the service, so final queue saves on shutdown still complete. */
    val applicationScope: CoroutineScope

    interface Provider {
        val playbackDependencies: PlaybackDependencies
    }
}
