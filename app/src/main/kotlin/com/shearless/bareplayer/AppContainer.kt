package com.shearless.bareplayer

import android.app.Application
import com.shearless.bareplayer.core.billing.ForegroundActivityTracker
import com.shearless.bareplayer.core.billing.PlayTipJar
import com.shearless.bareplayer.core.billing.PreviewTipJar
import com.shearless.bareplayer.core.data.db.BareDatabase
import com.shearless.bareplayer.core.data.repository.DataStorePreferences
import com.shearless.bareplayer.core.data.repository.DefaultLibraryRepository
import com.shearless.bareplayer.core.data.repository.DefaultPlayHistoryRepository
import com.shearless.bareplayer.core.data.repository.DefaultPlaylistRepository
import com.shearless.bareplayer.core.data.repository.DefaultQueueRepository
import com.shearless.bareplayer.core.data.scan.LibrarySync
import com.shearless.bareplayer.core.data.scan.MediaStoreScanner
import com.shearless.bareplayer.core.domain.AppDependencies
import com.shearless.bareplayer.core.domain.repository.LibraryRepository
import com.shearless.bareplayer.core.domain.repository.PlayHistoryRepository
import com.shearless.bareplayer.core.domain.repository.PlaylistRepository
import com.shearless.bareplayer.core.domain.repository.PreferencesRepository
import com.shearless.bareplayer.core.domain.repository.QueueRepository
import com.shearless.bareplayer.core.domain.tips.TipJar
import com.shearless.bareplayer.core.playback.PlaybackDependencies
import com.shearless.bareplayer.core.playback.PlayerConnection
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
    override val tipJar: TipJar by lazy {
        // Billing only works for Play installs; debug builds preview the UI without charging.
        if (BuildConfig.DEBUG) PreviewTipJar() else PlayTipJar(application, activityTracker)
    }
}
