package com.rajdeepkonwar.bareplayer.core.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

private const val ALBUM_COLUMNS = """
    album_id AS id, album AS title, MAX(album_artist) AS artist, MAX(year) AS year,
    COUNT(*) AS trackCount, SUM(duration_ms) AS durationMs,
    MIN(id) AS artTrackId, uri AS artUri, folder_path AS artFolder
"""

private const val ARTIST_COLUMNS = """
    artist_id AS id, artist AS name, COUNT(DISTINCT album_id) AS albumCount, COUNT(*) AS trackCount,
    MIN(id) AS artTrackId, album_id AS artAlbumId, uri AS artUri, folder_path AS artFolder
"""

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks ORDER BY title_sort")
    fun observeAll(): Flow<List<TrackEntity>>

    @Query("SELECT COUNT(*) FROM tracks")
    suspend fun count(): Int

    @Query("SELECT id, fingerprint FROM tracks")
    suspend fun fingerprints(): List<IdFingerprint>

    @Upsert
    suspend fun upsert(tracks: List<TrackEntity>)

    @Query("DELETE FROM tracks WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT * FROM tracks WHERE id IN (:ids)")
    suspend fun byIds(ids: List<Long>): List<TrackEntity>

    @Query("SELECT id FROM tracks WHERE id IN (:ids)")
    suspend fun existingIds(ids: List<Long>): List<Long>

    @Query("SELECT $ALBUM_COLUMNS FROM tracks GROUP BY album_id ORDER BY album_sort")
    fun observeAlbums(): Flow<List<AlbumRow>>

    @Query("SELECT $ALBUM_COLUMNS FROM tracks WHERE album_id = :albumId GROUP BY album_id")
    fun observeAlbum(albumId: Long): Flow<AlbumRow?>

    @Query("SELECT * FROM tracks WHERE album_id = :albumId ORDER BY disc_number, track_number, title_sort")
    fun observeAlbumTracks(albumId: Long): Flow<List<TrackEntity>>

    @Query("SELECT $ARTIST_COLUMNS FROM tracks GROUP BY artist_id ORDER BY artist_sort")
    fun observeArtists(): Flow<List<ArtistRow>>

    @Query("SELECT $ARTIST_COLUMNS FROM tracks WHERE artist_id = :artistId GROUP BY artist_id")
    fun observeArtist(artistId: Long): Flow<ArtistRow?>

    @Query(
        "SELECT * FROM tracks WHERE artist_id = :artistId " +
            "ORDER BY album_sort, album_id, disc_number, track_number, title_sort",
    )
    fun observeArtistTracks(artistId: Long): Flow<List<TrackEntity>>

    @Query(
        "SELECT $ALBUM_COLUMNS FROM tracks WHERE artist_id = :artistId " +
            "GROUP BY album_id ORDER BY year DESC, album_sort",
    )
    fun observeArtistAlbums(artistId: Long): Flow<List<AlbumRow>>

    @Query(
        "SELECT genre AS name, COUNT(*) AS trackCount FROM tracks WHERE genre != '' " +
            "GROUP BY genre COLLATE NOCASE ORDER BY genre COLLATE NOCASE",
    )
    fun observeGenres(): Flow<List<GenreRow>>

    @Query("SELECT * FROM tracks WHERE genre = :genre COLLATE NOCASE ORDER BY title_sort")
    fun observeGenreTracks(genre: String): Flow<List<TrackEntity>>

    @Query(
        "SELECT folder_path AS path, COUNT(*) AS trackCount FROM tracks " +
            "GROUP BY folder_path ORDER BY folder_path COLLATE NOCASE",
    )
    fun observeFolders(): Flow<List<FolderRow>>

    @Query("SELECT * FROM tracks WHERE folder_path = :path ORDER BY file_name COLLATE NOCASE")
    fun observeFolderTracks(path: String): Flow<List<TrackEntity>>

    @Query(
        "SELECT tracks.* FROM tracks JOIN tracks_fts ON tracks.id = tracks_fts.rowid " +
            "WHERE tracks_fts MATCH :match ORDER BY tracks.title_sort LIMIT :limit",
    )
    fun searchTracks(match: String, limit: Int): Flow<List<TrackEntity>>

    @Query(
        "SELECT $ALBUM_COLUMNS FROM tracks WHERE album LIKE :pattern ESCAPE '\\' " +
            "GROUP BY album_id ORDER BY album_sort LIMIT :limit",
    )
    fun searchAlbums(pattern: String, limit: Int): Flow<List<AlbumRow>>

    @Query(
        "SELECT $ARTIST_COLUMNS FROM tracks WHERE artist LIKE :pattern ESCAPE '\\' " +
            "GROUP BY artist_id ORDER BY artist_sort LIMIT :limit",
    )
    fun searchArtists(pattern: String, limit: Int): Flow<List<ArtistRow>>
}
