package com.shearless.bareplayer.core.domain.playback

import com.shearless.bareplayer.core.domain.library.testTrack
import com.shearless.bareplayer.core.domain.repository.LibraryRepository
import com.shearless.bareplayer.core.domain.repository.QueueRepository
import com.shearless.bareplayer.core.model.Album
import com.shearless.bareplayer.core.model.Artist
import com.shearless.bareplayer.core.model.Folder
import com.shearless.bareplayer.core.model.Genre
import com.shearless.bareplayer.core.model.QueueSnapshot
import com.shearless.bareplayer.core.model.RepeatMode
import com.shearless.bareplayer.core.model.ScanState
import com.shearless.bareplayer.core.model.SearchResults
import com.shearless.bareplayer.core.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QueueRestorerTest {

    private class FakeQueue(var saved: QueueSnapshot?) : QueueRepository {
        override suspend fun save(snapshot: QueueSnapshot) { saved = snapshot }
        override suspend fun load() = saved
        override suspend fun clear() { saved = null }
    }

    private class FakeLibrary(private val tracks: List<Track>) : LibraryRepository {
        override val scanState: StateFlow<ScanState> = MutableStateFlow(ScanState.Idle)
        override suspend fun tracksByIds(ids: List<Long>): List<Track> {
            val byId = tracks.associateBy { it.id }
            return ids.mapNotNull { byId[it] }
        }
        override fun tracks(): Flow<List<Track>> = emptyFlow()
        override fun albums(): Flow<List<Album>> = emptyFlow()
        override fun artists(): Flow<List<Artist>> = emptyFlow()
        override fun genres(): Flow<List<Genre>> = emptyFlow()
        override fun folders(): Flow<List<Folder>> = emptyFlow()
        override fun album(albumId: Long): Flow<Album?> = emptyFlow()
        override fun albumTracks(albumId: Long): Flow<List<Track>> = emptyFlow()
        override fun artist(artistId: Long): Flow<Artist?> = emptyFlow()
        override fun artistTracks(artistId: Long): Flow<List<Track>> = emptyFlow()
        override fun artistAlbums(artistId: Long): Flow<List<Album>> = emptyFlow()
        override fun genreTracks(genre: String): Flow<List<Track>> = emptyFlow()
        override fun folderTracks(path: String): Flow<List<Track>> = emptyFlow()
        override fun search(query: String): Flow<SearchResults> = emptyFlow()
        override suspend fun refresh(force: Boolean) = Unit
    }

    private fun snapshot(ids: List<Long>, current: Int) =
        QueueSnapshot(ids, current, positionMs = 42_000, shuffleEnabled = true, shuffleOrder = ids.indices.reversed().toList(), RepeatMode.ALL)

    @Test
    fun restore_returnsNullWithoutSavedQueue() = runTest {
        assertNull(QueueRestorer(FakeQueue(null), FakeLibrary(listOf(testTrack(1)))).restore())
    }

    @Test
    fun restore_keepsDuplicatesAndAlignsTracksWithIds() = runTest {
        val restorer = QueueRestorer(
            FakeQueue(snapshot(listOf(2, 1, 2), current = 2)),
            FakeLibrary(listOf(testTrack(1), testTrack(2))),
        )
        val restored = restorer.restore()!!
        assertEquals(listOf(2L, 1L, 2L), restored.tracks.map { it.id })
        assertEquals(2, restored.snapshot.currentIndex)
        assertEquals(42_000L, restored.snapshot.positionMs)
        assertEquals(listOf(2, 1, 0), restored.snapshot.shuffleOrder)
    }

    @Test
    fun restore_dropsDeletedTracks() = runTest {
        val restorer = QueueRestorer(
            FakeQueue(snapshot(listOf(1, 2, 3), current = 0)),
            FakeLibrary(listOf(testTrack(1), testTrack(3))),
        )
        val restored = restorer.restore()!!
        assertEquals(listOf(1L, 3L), restored.tracks.map { it.id })
        assertEquals(listOf(1, 0), restored.snapshot.shuffleOrder)
    }

    @Test
    fun restore_returnsNullWhenEveryTrackIsGone() = runTest {
        assertNull(QueueRestorer(FakeQueue(snapshot(listOf(5, 6), 0)), FakeLibrary(emptyList())).restore())
    }
}
