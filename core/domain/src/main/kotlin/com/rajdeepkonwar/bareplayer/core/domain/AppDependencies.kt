package com.rajdeepkonwar.bareplayer.core.domain

import com.rajdeepkonwar.bareplayer.core.domain.playback.PlaybackController
import com.rajdeepkonwar.bareplayer.core.domain.repository.LibraryRepository
import com.rajdeepkonwar.bareplayer.core.domain.repository.PlaylistRepository
import com.rajdeepkonwar.bareplayer.core.domain.repository.PreferencesRepository
import com.rajdeepkonwar.bareplayer.core.domain.tips.TipJar

/** The dependency graph exposed to feature modules (manual DI, see the app's AppContainer). */
interface AppDependencies {
    val libraryRepository: LibraryRepository
    val playlistRepository: PlaylistRepository
    val preferencesRepository: PreferencesRepository
    val playbackController: PlaybackController
    val tipJar: TipJar
}
