package com.shearless.bareplayer.core.data

import com.shearless.bareplayer.core.data.db.BareDatabase
import com.shearless.bareplayer.core.data.db.PlayCountEntity
import com.shearless.bareplayer.core.data.db.toEntity
import com.shearless.bareplayer.core.data.repository.DefaultPlayHistoryRepository
import com.shearless.bareplayer.core.data.repository.DefaultPlaylistRepository
import com.shearless.bareplayer.core.data.repository.DefaultQueueRepository
import com.shearless.bareplayer.core.model.QueueSnapshot
import com.shearless.bareplayer.core.model.RepeatMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PlaylistRepositoryTest {
    private lateinit var db: BareDatabase
    private lateinit var repo: DefaultPlaylistRepository
    private var now = 1_000L

    @Before
    fun setUp() = runTest {
        db = inMemoryDatabase()
        repo = DefaultPlaylistRepository(db) { now }
        db.trackDao().upsert((1L..5L).map { track(it).toEntity() })
    }

    @After fun tearDown() { db.close() }

    private suspend fun entryTrackIds(playlistId: Long) = repo.entries(playlistId).first().map { it.track.id }

    @Test
    fun create_trimsNameAndListsAlphabetically() = runTest {
        repo.create("  Road trip ")
        repo.create("chill")
        assertEquals(listOf("chill", "Road trip"), repo.playlists().first().map { it.name })
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_rejectsBlankNames() = runTest {
        repo.create("   ")
    }

    @Test
    fun rename_updatesNameAndTimestamp() = runTest {
        val id = repo.create("Old")
        now = 2_000L
        repo.rename(id, "New")
        val playlist = repo.playlist(id).first()!!
        assertEquals("New", playlist.name)
        assertEquals(2_000L, playlist.updatedAtMs)
    }

    @Test
    fun addTracks_appendsInOrderAndAllowsDuplicates() = runTest {
        val id = repo.create("Mix")
        repo.addTracks(id, listOf(3, 1))
        repo.addTracks(id, listOf(3))
        assertEquals(listOf(3L, 1L, 3L), entryTrackIds(id))
        assertEquals(3, repo.playlist(id).first()!!.trackCount)
    }

    @Test
    fun removeEntry_removesOnlyThatOccurrence() = runTest {
        val id = repo.create("Mix")
        repo.addTracks(id, listOf(1, 2, 1))
        val firstEntry = repo.entries(id).first().first()
        repo.removeEntry(id, firstEntry.entryId)
        assertEquals(listOf(2L, 1L), entryTrackIds(id))
    }

    @Test
    fun forgetTrack_removesItFromEveryPlaylist() = runTest {
        val first = repo.create("One")
        val second = repo.create("Two")
        repo.addTracks(first, listOf(1, 2))
        repo.addTracks(second, listOf(2, 3))
        db.playCountDao().insert(PlayCountEntity(trackId = 2, count = 4, lastPlayed = 1))
        repo.forgetTrack(2)
        assertEquals(listOf(1L), entryTrackIds(first))
        assertEquals(listOf(3L), entryTrackIds(second))
        assertNull(db.playCountDao().count(2))
    }

    @Test
    fun moveEntry_reordersVisibleEntries() = runTest {
        val id = repo.create("Mix")
        repo.addTracks(id, listOf(1, 2, 3, 4))
        repo.moveEntry(id, from = 0, to = 2)
        assertEquals(listOf(2L, 3L, 1L, 4L), entryTrackIds(id))
        repo.moveEntry(id, from = 3, to = 0)
        assertEquals(listOf(4L, 2L, 3L, 1L), entryTrackIds(id))
    }

    @Test
    fun missingTracks_areHiddenButKept() = runTest {
        val id = repo.create("Mix")
        repo.addTracks(id, listOf(1, 2, 3))
        db.trackDao().deleteByIds(listOf(2))
        assertEquals(listOf(1L, 3L), entryTrackIds(id))
        assertEquals(2, repo.playlist(id).first()!!.trackCount)

        repo.moveEntry(id, from = 1, to = 0)
        db.trackDao().upsert(listOf(track(2).toEntity()))
        assertEquals(listOf(3L, 1L, 2L), entryTrackIds(id))
    }

    @Test
    fun delete_cascadesToEntries() = runTest {
        val id = repo.create("Mix")
        repo.addTracks(id, listOf(1, 2))
        repo.delete(id)
        assertEquals(emptyList<Any>(), repo.playlists().first())
        assertEquals(emptyList<Any>(), db.playlistDao().entries(id))
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class QueueRepositoryTest {
    private lateinit var db: BareDatabase

    @Before fun setUp() { db = inMemoryDatabase() }
    @After fun tearDown() { db.close() }

    @Test
    fun saveAndLoad_roundTripsSnapshot() = runTest {
        val repo = DefaultQueueRepository(db)
        val snapshot = QueueSnapshot(
            trackIds = (1L..10_000L).toList(),
            currentIndex = 42,
            positionMs = 61_000,
            shuffleEnabled = true,
            shuffleOrder = (0 until 10_000).reversed().toList(),
            repeatMode = RepeatMode.ONE,
        )
        repo.save(snapshot)
        assertEquals(snapshot, repo.load())
    }

    @Test
    fun savingEmptyQueue_clearsState() = runTest {
        val repo = DefaultQueueRepository(db)
        repo.save(QueueSnapshot(listOf(1), 0, 0, false, emptyList(), RepeatMode.OFF))
        repo.save(QueueSnapshot(emptyList(), 0, 0, false, emptyList(), RepeatMode.OFF))
        assertNull(repo.load())
    }

    @Test
    fun playHistory_countsPlays() = runTest {
        val repo = DefaultPlayHistoryRepository(db) { 5L }
        repo.recordPlay(7)
        repo.recordPlay(7)
        assertEquals(2, repo.playCount(7))
        assertEquals(0, repo.playCount(8))
    }
}
