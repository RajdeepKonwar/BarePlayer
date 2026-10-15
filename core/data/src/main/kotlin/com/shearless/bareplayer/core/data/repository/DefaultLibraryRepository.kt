package com.shearless.bareplayer.core.data.repository

import com.shearless.bareplayer.core.data.db.BareDatabase
import com.shearless.bareplayer.core.data.db.toModel
import com.shearless.bareplayer.core.data.scan.LibrarySync
import com.shearless.bareplayer.core.domain.library.SearchQuery
import com.shearless.bareplayer.core.domain.repository.LibraryRepository
import com.shearless.bareplayer.core.model.Album
import com.shearless.bareplayer.core.model.Artist
import com.shearless.bareplayer.core.model.Folder
import com.shearless.bareplayer.core.model.Genre
import com.shearless.bareplayer.core.model.ScanState
import com.shearless.bareplayer.core.model.SearchResults
import com.shearless.bareplayer.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class DefaultLibraryRepository(
    database: BareDatabase,
    private val sync: LibrarySync,
) : LibraryRepository {
    private val dao = database.trackDao()

    override val scanState: StateFlow<ScanState> get() = sync.state

    override fun tracks(): Flow<List<Track>> = dao.observeAll().mapList { it.toModel() }
    override fun albums(): Flow<List<Album>> = dao.observeAlbums().mapList { it.toModel() }
    override fun artists(): Flow<List<Artist>> = dao.observeArtists().mapList { it.toModel() }
    override fun genres(): Flow<List<Genre>> = dao.observeGenres().mapList { it.toModel() }
    override fun folders(): Flow<List<Folder>> = dao.observeFolders().mapList { it.toModel() }

    override fun album(albumId: Long): Flow<Album?> =
        dao.observeAlbum(albumId).map { it?.toModel() }.distinctUntilChanged()

    override fun albumTracks(albumId: Long): Flow<List<Track>> =
        dao.observeAlbumTracks(albumId).mapList { it.toModel() }

    override fun artist(artistId: Long): Flow<Artist?> =
        dao.observeArtist(artistId).map { it?.toModel() }.distinctUntilChanged()

    override fun artistTracks(artistId: Long): Flow<List<Track>> =
        dao.observeArtistTracks(artistId).mapList { it.toModel() }

    override fun artistAlbums(artistId: Long): Flow<List<Album>> =
        dao.observeArtistAlbums(artistId).mapList { it.toModel() }

    override fun genreTracks(genre: String): Flow<List<Track>> =
        dao.observeGenreTracks(genre).mapList { it.toModel() }

    override fun folderTracks(path: String): Flow<List<Track>> =
        dao.observeFolderTracks(path).mapList { it.toModel() }

    override fun search(query: String): Flow<SearchResults> {
        val match = SearchQuery.toFtsMatch(query)
        val like = SearchQuery.toLikePattern(query)
        if (match == null || like == null) return flowOf(SearchResults.Empty.copy(query = query))
        return combine(
            dao.searchTracks(match, TRACK_LIMIT),
            dao.searchAlbums(like, GROUP_LIMIT),
            dao.searchArtists(like, GROUP_LIMIT),
        ) { tracks, albums, artists ->
            SearchResults(
                query = query,
                tracks = tracks.map { it.toModel() },
                albums = albums.map { it.toModel() },
                artists = artists.map { it.toModel() },
            )
        }.flowOn(Dispatchers.Default)
    }

    override suspend fun tracksByIds(ids: List<Long>): List<Track> {
        if (ids.isEmpty()) return emptyList()
        val byId = HashMap<Long, Track>(ids.size * 2)
        ids.distinct().chunked(SQL_VARIABLE_CHUNK).forEach { chunk ->
            dao.byIds(chunk).forEach { byId[it.id] = it.toModel() }
        }
        return ids.mapNotNull { byId[it] }
    }

    override suspend fun refresh(force: Boolean) = sync.sync(force)

    private inline fun <T, R> Flow<List<T>>.mapList(crossinline transform: (T) -> R): Flow<List<R>> =
        map { list -> list.map(transform) }.distinctUntilChanged().flowOn(Dispatchers.Default)

    private companion object {
        const val TRACK_LIMIT = 200
        const val GROUP_LIMIT = 20
        const val SQL_VARIABLE_CHUNK = 500
    }
}
