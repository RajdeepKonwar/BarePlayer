package com.rajdeepkonwar.bareplayer.core.data

import com.rajdeepkonwar.bareplayer.core.data.db.BareDatabase
import com.rajdeepkonwar.bareplayer.core.data.repository.DefaultLibraryRepository
import com.rajdeepkonwar.bareplayer.core.data.scan.LibrarySync
import com.rajdeepkonwar.bareplayer.core.data.scan.MediaScanner
import com.rajdeepkonwar.bareplayer.core.data.scan.ScanTokenStore
import com.rajdeepkonwar.bareplayer.core.model.ScanState
import com.rajdeepkonwar.bareplayer.core.model.Track
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LibrarySyncTest {
    private lateinit var db: BareDatabase
    private val scanner = FakeScanner()
    private val tokens = object : ScanTokenStore {
        var token: String? = null
        override suspend fun read() = token
        override suspend fun write(token: String?) { this.token = token }
    }

    @Before fun setUp() { db = inMemoryDatabase() }
    @After fun tearDown() { db.close() }

    private fun sync() = LibrarySync(db, scanner, tokens, TestScope())

    @Test
    fun firstSync_insertsScannedTracks() = runTest {
        scanner.tracks = listOf(track(1, title = "Beta"), track(2, title = "Alpha"))
        val sync = sync()
        sync.sync(force = false)

        val repo = DefaultLibraryRepository(db, sync)
        assertEquals(listOf("Alpha", "Beta"), repo.tracks().first().map { it.title })
        assertEquals(ScanState.Done::class, sync.state.value::class)
    }

    @Test
    fun resync_appliesUpdatesAndDeletions() = runTest {
        val sync = sync()
        scanner.tracks = listOf(track(1), track(2), track(3))
        sync.sync(force = true)

        scanner.tracks = listOf(track(1), track(2, title = "Renamed", modified = 200))
        sync.sync(force = true)

        val titles = DefaultLibraryRepository(db, sync).tracks().first().associate { it.id to it.title }
        assertEquals(mapOf(1L to "Track 1", 2L to "Renamed"), titles)
    }

    @Test
    fun unchangedToken_skipsScan() = runTest {
        val sync = sync()
        scanner.token = "v1:5"
        scanner.tracks = listOf(track(1))
        sync.sync(force = false)
        assertEquals(1, scanner.scanCount)

        sync.sync(force = false)
        assertEquals(1, scanner.scanCount)

        scanner.token = "v1:6"
        sync.sync(force = false)
        assertEquals(2, scanner.scanCount)
    }

    @Test
    fun scannerFailure_reportsFailedState() = runTest {
        scanner.failure = SecurityException("no permission")
        val sync = sync()
        sync.sync(force = true)
        val state = sync.state.value
        assertTrue(state is ScanState.Failed && state.message == "no permission")
    }

    private class FakeScanner : MediaScanner {
        var tracks: List<Track> = emptyList()
        var token: String? = null
        var failure: Exception? = null
        var scanCount = 0
        override fun changeToken() = token
        override suspend fun scan(): List<Track> {
            failure?.let { throw it }
            scanCount++
            return tracks
        }
    }
}
