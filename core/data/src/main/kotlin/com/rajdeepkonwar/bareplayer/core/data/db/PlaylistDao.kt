package com.rajdeepkonwar.bareplayer.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

private const val PLAYLIST_COLUMNS = """
    p.id AS id, p.name AS name, p.created_at AS createdAt, p.updated_at AS updatedAt,
    (SELECT COUNT(*) FROM playlist_entries e JOIN tracks t ON t.id = e.track_id
        WHERE e.playlist_id = p.id) AS trackCount
"""

@Dao
interface PlaylistDao {
    @Query("SELECT $PLAYLIST_COLUMNS FROM playlists p ORDER BY p.name COLLATE NOCASE")
    fun observePlaylists(): Flow<List<PlaylistRow>>

    @Query("SELECT $PLAYLIST_COLUMNS FROM playlists p WHERE p.id = :id")
    fun observePlaylist(id: Long): Flow<PlaylistRow?>

    @Query(
        "SELECT e.entry_id AS entryId, e.position AS position, t.* FROM playlist_entries e " +
            "JOIN tracks t ON t.id = e.track_id WHERE e.playlist_id = :playlistId ORDER BY e.position",
    )
    fun observeEntries(playlistId: Long): Flow<List<PlaylistEntryRow>>

    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name, updated_at = :now WHERE id = :id")
    suspend fun rename(id: Long, name: String, now: Long)

    @Query("UPDATE playlists SET updated_at = :now WHERE id = :id")
    suspend fun touch(id: Long, now: Long)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_entries WHERE playlist_id = :playlistId")
    suspend fun maxPosition(playlistId: Long): Int

    @Insert
    suspend fun insertEntries(entries: List<PlaylistEntryEntity>)

    @Query("SELECT * FROM playlist_entries WHERE playlist_id = :playlistId ORDER BY position")
    suspend fun entries(playlistId: Long): List<PlaylistEntryEntity>

    @Query(
        "SELECT e.entry_id FROM playlist_entries e JOIN tracks t ON t.id = e.track_id " +
            "WHERE e.playlist_id = :playlistId ORDER BY e.position",
    )
    suspend fun visibleEntryIds(playlistId: Long): List<Long>

    @Query("DELETE FROM playlist_entries WHERE playlist_id = :playlistId AND entry_id = :entryId")
    suspend fun deleteEntry(playlistId: Long, entryId: Long)

    @Update
    suspend fun updateEntries(entries: List<PlaylistEntryEntity>)
}
