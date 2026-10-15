package com.shearless.bareplayer.feature.playlists.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shearless.bareplayer.core.domain.playback.PlaybackController
import com.shearless.bareplayer.core.domain.repository.PlaylistRepository
import com.shearless.bareplayer.core.model.Playlist
import com.shearless.bareplayer.core.model.PlaylistEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlaylistDetailViewModel(
    private val playlistId: Long,
    private val repository: PlaylistRepository,
    val playback: PlaybackController,
) : ViewModel() {

    val playlist: StateFlow<Playlist?> = repository.playlist(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val entries: StateFlow<List<PlaylistEntry>?> = repository.entries(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val currentTrackId: StateFlow<Long?> = playback.state
        .map { it.current?.trackId }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun play(index: Int = 0, shuffle: Boolean = false) {
        val tracks = entries.value.orEmpty().map { it.track }
        if (tracks.isEmpty()) return
        playback.playTracks(tracks, if (shuffle) tracks.indices.random() else index, shuffle)
    }

    fun rename(name: String) = launch { repository.rename(playlistId, name) }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.delete(playlistId)
            onDeleted()
        }
    }

    fun remove(entryId: Long) = launch { repository.removeEntry(playlistId, entryId) }

    fun move(from: Int, to: Int) = launch { repository.moveEntry(playlistId, from, to) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
