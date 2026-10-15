package com.shearless.bareplayer.core.data

import com.shearless.bareplayer.core.data.db.BareDatabase
import com.shearless.bareplayer.core.data.repository.DefaultLibraryRepository
import com.shearless.bareplayer.core.data.scan.LibrarySync
import com.shearless.bareplayer.core.data.scan.MediaScanner
import com.shearless.bareplayer.core.data.scan.ScanTokenStore
import com.shearless.bareplayer.core.model.Track
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.measureTime

/** Guards query plans and rough timings for a 10k-track library. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LargeLibraryTest {
    private lateinit var db: BareDatabase
    private lateinit var repo: DefaultLibraryRepository
    private val scanned = (1L..TRACKS).map { id ->
        track(
            id = id,
            title = "Song ${id.toString().reversed()}",
            artist = "Artist ${id % 400}",
            artistId = id % 400,
            album = "Album ${id % 900}",
            albumId = id % 900,
            genre = "Genre ${id % 25}",
            folder = "/storage/emulated/0/Music/${id % 300}",
        )
    }

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        val scanner = object : MediaScanner {
            override fun changeToken(): String? = null
            override suspend fun scan(): List<Track> = scanned
        }
        val tokens = object : ScanTokenStore {
            override suspend fun read(): String? = null
            override suspend fun write(token: String?) = Unit
        }
        repo = DefaultLibraryRepository(db, LibrarySync(db, scanner, tokens, TestScope()))
    }

    @After fun tearDown() { db.close() }

    private fun queryPlan(sql: String): String {
        db.openHelper.readableDatabase.query("EXPLAIN QUERY PLAN $sql").use { c ->
            return buildString { while (c.moveToNext()) appendLine(c.getString(c.columnCount - 1)) }
        }
    }

    @Test
    fun songsListIsServedByIndexWithoutSorting() {
        val plan = queryPlan("SELECT * FROM tracks ORDER BY title_sort")
        assertFalse("Unexpected sort step:\n$plan", plan.contains("TEMP B-TREE"))
    }

    @Test
    fun detailQueriesUseIndexes() {
        listOf(
            "SELECT * FROM tracks WHERE album_id = 1",
            "SELECT * FROM tracks WHERE artist_id = 1",
            "SELECT * FROM tracks WHERE folder_path = 'x'",
        ).forEach { sql ->
            val plan = queryPlan(sql)
            assertTrue("Expected index use for $sql:\n$plan", plan.contains("USING INDEX"))
        }
    }

    /**
     * Correctness at 10k tracks. Timings are logged rather than asserted: wall-clock limits fail on
     * a busy machine, while the query-plan tests above catch the regressions that matter.
     */
    @Test
    fun scanAndQueriesHandleTenThousandTracks() = runTest {
        val syncTime = measureTime { repo.refresh(force = true) }
        val queryTime = measureTime {
            assertEquals(TRACKS.toInt(), repo.tracks().first().size)
            assertEquals(900, repo.albums().first().size)
            assertEquals(400, repo.artists().first().size)
            assertEquals(25, repo.genres().first().size)
            assertEquals(300, repo.folders().first().size)
            assertTrue(repo.search("song 1").first().tracks.isNotEmpty())
        }
        val resyncTime = measureTime { repo.refresh(force = true) }
        assertEquals(TRACKS.toInt(), repo.tracks().first().size)
        println("10k tracks: sync=$syncTime, queries=$queryTime, no-op resync=$resyncTime")
    }

    private companion object {
        const val TRACKS = 10_000L
    }
}
