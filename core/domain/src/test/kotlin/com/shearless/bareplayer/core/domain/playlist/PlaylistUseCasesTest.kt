package com.shearless.bareplayer.core.domain.playlist

import com.shearless.bareplayer.core.domain.repository.PlaylistRepository
import com.shearless.bareplayer.core.model.Playlist
import com.shearless.bareplayer.core.model.PlaylistEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaylistNamesTest {
    @Test
    fun normalize_trimsCollapsesAndCaps() {
        assertEquals("Road trip", PlaylistNames.normalize("  Road \t  trip \n"))
        assertNull(PlaylistNames.normalize("   "))
        assertEquals(PlaylistNames.MAX_LENGTH, PlaylistNames.normalize("x".repeat(200))!!.length)
    }

    @Test
    fun suggest_picksFirstFreeNameCaseInsensitively() {
        assertEquals("New playlist", PlaylistNames.suggest("New playlist", listOf("Chill")))
        assertEquals("New playlist 2", PlaylistNames.suggest("New playlist", listOf("new PLAYLIST")))
        assertEquals("New playlist 4", PlaylistNames.suggest("New playlist", listOf("New playlist", "New playlist 2", "New playlist 3")))
    }
}

class AddToPlaylistUseCaseTest {
    private class FakePlaylists : PlaylistRepository {
        val created = mutableListOf<String>()
        val added = mutableMapOf<Long, MutableList<Long>>()
        override suspend fun create(name: String): Long {
            created += name
            return 100L + created.size
        }
        override suspend fun addTracks(playlistId: Long, trackIds: List<Long>) {
            added.getOrPut(playlistId) { mutableListOf() } += trackIds
        }
        override fun playlists(): Flow<List<Playlist>> = emptyFlow()
        override fun playlist(id: Long): Flow<Playlist?> = emptyFlow()
        override fun entries(playlistId: Long): Flow<List<PlaylistEntry>> = emptyFlow()
        override suspend fun rename(id: Long, name: String) = Unit
        override suspend fun delete(id: Long) = Unit
        override suspend fun removeEntry(playlistId: Long, entryId: Long) = Unit
        override suspend fun forgetTrack(trackId: Long) = Unit
        override suspend fun moveEntry(playlistId: Long, from: Int, to: Int) = Unit
    }

    @Test
    fun existingPlaylist_receivesTracks() = runTest {
        val repo = FakePlaylists()
        val result = AddToPlaylistUseCase(repo)(PlaylistTarget.Existing(7, "Mix"), listOf(1, 2))
        assertEquals(AddToPlaylistResult(7, "Mix", 2), result)
        assertEquals(listOf(1L, 2L), repo.added[7])
        assertEquals(emptyList<String>(), repo.created)
    }

    @Test
    fun newPlaylist_isCreatedWithNormalizedNameThenFilled() = runTest {
        val repo = FakePlaylists()
        val result = AddToPlaylistUseCase(repo)(PlaylistTarget.New("  Gym   mix "), listOf(5))
        assertEquals(listOf("Gym mix"), repo.created)
        assertEquals(AddToPlaylistResult(101, "Gym mix", 1), result)
        assertEquals(listOf(5L), repo.added[101])
    }

    @Test(expected = IllegalArgumentException::class)
    fun newPlaylist_withBlankName_isRejected() = runTest {
        AddToPlaylistUseCase(FakePlaylists())(PlaylistTarget.New("  "), listOf(1))
    }
}
