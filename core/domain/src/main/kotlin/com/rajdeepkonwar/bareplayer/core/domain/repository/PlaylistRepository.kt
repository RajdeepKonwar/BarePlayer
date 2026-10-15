package com.rajdeepkonwar.bareplayer.core.domain.repository

import com.rajdeepkonwar.bareplayer.core.model.Playlist
import com.rajdeepkonwar.bareplayer.core.model.PlaylistEntry
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun playlists(): Flow<List<Playlist>>
    fun playlist(id: Long): Flow<Playlist?>
    fun entries(playlistId: Long): Flow<List<PlaylistEntry>>

    /** Creates a playlist and returns its id. Names are trimmed; blank names are rejected. */
    suspend fun create(name: String): Long
    suspend fun rename(id: Long, name: String)
    suspend fun delete(id: Long)

    suspend fun addTracks(playlistId: Long, trackIds: List<Long>)
    suspend fun removeEntry(playlistId: Long, entryId: Long)
    suspend fun moveEntry(playlistId: Long, from: Int, to: Int)
}
