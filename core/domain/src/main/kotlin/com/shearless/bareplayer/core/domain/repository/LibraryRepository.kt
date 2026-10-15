package com.shearless.bareplayer.core.domain.repository

import com.shearless.bareplayer.core.model.Album
import com.shearless.bareplayer.core.model.Artist
import com.shearless.bareplayer.core.model.Folder
import com.shearless.bareplayer.core.model.Genre
import com.shearless.bareplayer.core.model.ScanState
import com.shearless.bareplayer.core.model.SearchResults
import com.shearless.bareplayer.core.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface LibraryRepository {
    val scanState: StateFlow<ScanState>

    fun tracks(): Flow<List<Track>>
    fun albums(): Flow<List<Album>>
    fun artists(): Flow<List<Artist>>
    fun genres(): Flow<List<Genre>>
    fun folders(): Flow<List<Folder>>

    fun album(albumId: Long): Flow<Album?>
    fun albumTracks(albumId: Long): Flow<List<Track>>
    fun artist(artistId: Long): Flow<Artist?>
    fun artistTracks(artistId: Long): Flow<List<Track>>
    fun artistAlbums(artistId: Long): Flow<List<Album>>
    fun genreTracks(genre: String): Flow<List<Track>>
    fun folderTracks(path: String): Flow<List<Track>>

    fun search(query: String): Flow<SearchResults>

    /** Returns tracks in the same order as [ids]; ids no longer in the library are dropped. */
    suspend fun tracksByIds(ids: List<Long>): List<Track>

    /** Rescans the device library. When [force] is false the scan may be skipped if nothing changed. */
    suspend fun refresh(force: Boolean = false)
}
