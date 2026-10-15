package com.shearless.bareplayer.feature.library.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shearless.bareplayer.core.domain.playback.PlaybackController
import com.shearless.bareplayer.core.domain.repository.LibraryRepository
import com.shearless.bareplayer.core.model.Album
import com.shearless.bareplayer.core.model.ArtKey
import com.shearless.bareplayer.core.model.Track
import com.shearless.bareplayer.feature.library.CollectionRoute
import com.shearless.bareplayer.feature.library.CollectionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class CollectionUiState(
    val type: CollectionType,
    val title: String,
    val artist: String? = null,
    val year: Int = 0,
    val artKey: ArtKey? = null,
    val albumCount: Int = 0,
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
) {
    val totalDurationMs: Long get() = tracks.sumOf { it.durationMs }
}

class CollectionViewModel(
    route: CollectionRoute,
    library: LibraryRepository,
    val playback: PlaybackController,
) : ViewModel() {

    val state: StateFlow<CollectionUiState?> = uiState(route, library)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val currentTrackId: StateFlow<Long?> = playback.state
        .map { it.current?.trackId }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun play(index: Int = 0, shuffle: Boolean = false) {
        val tracks = state.value?.tracks.orEmpty()
        if (tracks.isEmpty()) return
        val start = if (shuffle) tracks.indices.random() else index
        playback.playTracks(tracks, start, shuffle)
    }

    private fun uiState(route: CollectionRoute, library: LibraryRepository): Flow<CollectionUiState> =
        when (route.type) {
            CollectionType.ALBUM -> combine(library.album(route.id), library.albumTracks(route.id)) { album, tracks ->
                CollectionUiState(
                    type = route.type,
                    title = album?.title.orEmpty(),
                    artist = album?.artist,
                    year = album?.year ?: 0,
                    artKey = album?.artKey,
                    tracks = tracks,
                )
            }
            CollectionType.ARTIST -> combine(
                library.artist(route.id),
                library.artistAlbums(route.id),
                library.artistTracks(route.id),
            ) { artist, albums, tracks ->
                CollectionUiState(
                    type = route.type,
                    title = artist?.name.orEmpty(),
                    artKey = artist?.artKey,
                    albumCount = albums.size,
                    tracks = tracks,
                    albums = albums,
                )
            }
            CollectionType.GENRE -> library.genreTracks(route.key).map { tracks ->
                CollectionUiState(type = route.type, title = route.key, tracks = tracks)
            }
            CollectionType.FOLDER -> library.folderTracks(route.key).map { tracks ->
                CollectionUiState(
                    type = route.type,
                    title = route.key.trimEnd('/').substringAfterLast('/'),
                    artist = route.key,
                    tracks = tracks,
                )
            }
        }
}
