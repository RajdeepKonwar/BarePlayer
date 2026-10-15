package com.shearless.bareplayer.core.domain

import com.shearless.bareplayer.core.domain.playback.PlaybackController
import com.shearless.bareplayer.core.domain.repository.LibraryRepository
import com.shearless.bareplayer.core.domain.repository.PlaylistRepository
import com.shearless.bareplayer.core.domain.repository.PreferencesRepository
import com.shearless.bareplayer.core.domain.tips.TipJar

/** The dependency graph exposed to feature modules (manual DI, see the app's AppContainer). */
interface AppDependencies {
    val libraryRepository: LibraryRepository
    val playlistRepository: PlaylistRepository
    val preferencesRepository: PreferencesRepository
    val playbackController: PlaybackController
    val tipJar: TipJar
}
