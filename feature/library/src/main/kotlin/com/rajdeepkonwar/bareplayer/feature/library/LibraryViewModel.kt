package com.rajdeepkonwar.bareplayer.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajdeepkonwar.bareplayer.core.domain.playback.PlaybackController
import com.rajdeepkonwar.bareplayer.core.domain.library.SetFolderExcludedUseCase
import com.rajdeepkonwar.bareplayer.core.domain.repository.LibraryRepository
import com.rajdeepkonwar.bareplayer.core.domain.repository.PreferencesRepository
import com.rajdeepkonwar.bareplayer.core.model.Album
import com.rajdeepkonwar.bareplayer.core.model.Artist
import com.rajdeepkonwar.bareplayer.core.model.Folder
import com.rajdeepkonwar.bareplayer.core.model.Genre
import com.rajdeepkonwar.bareplayer.core.model.ScanState
import com.rajdeepkonwar.bareplayer.core.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

class LibraryViewModel(
    private val library: LibraryRepository,
    val playback: PlaybackController,
    preferences: PreferencesRepository,
) : ViewModel() {
    private val setFolderExcluded = SetFolderExcludedUseCase(preferences, library)

    // Each tab collects only its own flow; null means "still loading".
    val tracks: StateFlow<List<Track>?> = library.tracks().stateInVm()
    val albums: StateFlow<List<Album>?> = library.albums().stateInVm()
    val artists: StateFlow<List<Artist>?> = library.artists().stateInVm()
    val genres: StateFlow<List<Genre>?> = library.genres().stateInVm()
    val folders: StateFlow<List<Folder>?> = library.folders().stateInVm()

    val scanState: StateFlow<ScanState> = library.scanState

    val currentTrackId: StateFlow<Long?> = playback.state
        .map { it.current?.trackId }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    fun play(tracks: List<Track>, index: Int) = playback.playTracks(tracks, index)

    fun shuffleAll() {
        val all = tracks.value.orEmpty()
        if (all.isNotEmpty()) playback.playTracks(all, Random.nextInt(all.size), shuffle = true)
    }

    fun rescan() {
        viewModelScope.launch { library.refresh(force = true) }
    }

    fun excludeFolder(path: String) {
        viewModelScope.launch { setFolderExcluded(path, excluded = true) }
    }

    private fun <T> Flow<List<T>>.stateInVm(): StateFlow<List<T>?> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
