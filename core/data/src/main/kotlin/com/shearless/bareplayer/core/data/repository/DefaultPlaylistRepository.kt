package com.shearless.bareplayer.core.data.repository

import androidx.room.withTransaction
import com.shearless.bareplayer.core.data.db.BareDatabase
import com.shearless.bareplayer.core.data.db.PlaylistEntity
import com.shearless.bareplayer.core.data.db.PlaylistEntryEntity
import com.shearless.bareplayer.core.data.db.toModel
import com.shearless.bareplayer.core.domain.playback.QueueMath
import com.shearless.bareplayer.core.domain.repository.PlaylistRepository
import com.shearless.bareplayer.core.model.Playlist
import com.shearless.bareplayer.core.model.PlaylistEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class DefaultPlaylistRepository(
    private val database: BareDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) : PlaylistRepository {
    private val dao = database.playlistDao()

    override fun playlists(): Flow<List<Playlist>> =
        dao.observePlaylists().map { rows -> rows.map { it.toModel() } }.distinctUntilChanged()

    override fun playlist(id: Long): Flow<Playlist?> =
        dao.observePlaylist(id).map { it?.toModel() }.distinctUntilChanged()

    override fun entries(playlistId: Long): Flow<List<PlaylistEntry>> =
        dao.observeEntries(playlistId).map { rows -> rows.map { it.toModel() } }.distinctUntilChanged()

    override suspend fun create(name: String): Long {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Playlist name must not be blank" }
        val now = clock()
        return dao.insertPlaylist(PlaylistEntity(name = trimmed, createdAt = now, updatedAt = now))
    }

    override suspend fun rename(id: Long, name: String) {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Playlist name must not be blank" }
        dao.rename(id, trimmed, clock())
    }

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun addTracks(playlistId: Long, trackIds: List<Long>) {
        if (trackIds.isEmpty()) return
        database.withTransaction {
            val start = dao.maxPosition(playlistId) + 1
            dao.insertEntries(
                trackIds.mapIndexed { i, trackId ->
                    PlaylistEntryEntity(playlistId = playlistId, trackId = trackId, position = start + i)
                },
            )
            dao.touch(playlistId, clock())
        }
    }

    override suspend fun forgetTrack(trackId: Long) {
        database.withTransaction {
            dao.deleteEntriesForTrack(trackId)
            database.playCountDao().delete(trackId)
        }
    }

    override suspend fun removeEntry(playlistId: Long, entryId: Long) {
        database.withTransaction {
            dao.deleteEntry(playlistId, entryId)
            dao.touch(playlistId, clock())
        }
    }

    /** [from] and [to] index the visible entries (those whose track still exists). */
    override suspend fun moveEntry(playlistId: Long, from: Int, to: Int) {
        database.withTransaction {
            val visible = dao.visibleEntryIds(playlistId)
            if (from !in visible.indices || to !in visible.indices || from == to) return@withTransaction
            val reorderedVisible = QueueMath.move(visible, from, to)
            val all = dao.entries(playlistId)
            val visibleSet = visible.toHashSet()
            val hidden = all.filter { it.entryId !in visibleSet }.map { it.entryId }
            val newPositions = (reorderedVisible + hidden).withIndex().associate { (i, id) -> id to i }
            dao.updateEntries(all.map { it.copy(position = newPositions.getValue(it.entryId)) })
            dao.touch(playlistId, clock())
        }
    }
}
