package com.rajdeepkonwar.bareplayer

import android.app.Application
import com.rajdeepkonwar.bareplayer.core.billing.ForegroundActivityTracker
import com.rajdeepkonwar.bareplayer.core.billing.PlayTipJar
import com.rajdeepkonwar.bareplayer.core.data.db.BareDatabase
import com.rajdeepkonwar.bareplayer.core.data.repository.DataStorePreferences
import com.rajdeepkonwar.bareplayer.core.data.repository.DefaultLibraryRepository
import com.rajdeepkonwar.bareplayer.core.data.repository.DefaultPlayHistoryRepository
import com.rajdeepkonwar.bareplayer.core.data.repository.DefaultPlaylistRepository
import com.rajdeepkonwar.bareplayer.core.data.repository.DefaultQueueRepository
import com.rajdeepkonwar.bareplayer.core.data.scan.LibrarySync
import com.rajdeepkonwar.bareplayer.core.data.scan.MediaStoreScanner
import com.rajdeepkonwar.bareplayer.core.domain.AppDependencies
import com.rajdeepkonwar.bareplayer.core.domain.repository.LibraryRepository
import com.rajdeepkonwar.bareplayer.core.domain.repository.PlayHistoryRepository
import com.rajdeepkonwar.bareplayer.core.domain.repository.PlaylistRepository
import com.rajdeepkonwar.bareplayer.core.domain.repository.PreferencesRepository
import com.rajdeepkonwar.bareplayer.core.domain.repository.QueueRepository
import com.rajdeepkonwar.bareplayer.core.domain.tips.TipJar
import com.rajdeepkonwar.bareplayer.core.playback.PlaybackDependencies
import com.rajdeepkonwar.bareplayer.core.playback.PlayerConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency graph. Everything is lazy so cold start only pays for what the first frame
 * needs (preferences); the database opens on first library access.
 */
class AppContainer(private val application: Application) : AppDependencies, PlaybackDependencies {
    override val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database by lazy { BareDatabase.create(application) }
    private val preferences by lazy { DataStorePreferences(application) }

    val librarySync by lazy {
        LibrarySync(
            database = database,
            scanner = MediaStoreScanner(application),
            tokenStore = preferences,
            scope = applicationScope,
            excludedFolders = preferences::excludedFolders,
        )
    }

    override val libraryRepository: LibraryRepository by lazy { DefaultLibraryRepository(database, librarySync) }
    override val playlistRepository: PlaylistRepository by lazy { DefaultPlaylistRepository(database) }
    override val preferencesRepository: PreferencesRepository get() = preferences
    override val queueRepository: QueueRepository by lazy { DefaultQueueRepository(database) }
    override val playHistoryRepository: PlayHistoryRepository by lazy { DefaultPlayHistoryRepository(database) }

    val playerConnection: PlayerConnection by lazy { PlayerConnection(application) }
    override val playbackController get() = playerConnection

    val activityTracker = ForegroundActivityTracker()
    override val tipJar: TipJar by lazy { PlayTipJar(application, activityTracker) }
}
