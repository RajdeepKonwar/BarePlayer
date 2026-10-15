package com.shearless.bareplayer.core.data

import com.shearless.bareplayer.core.data.db.BareDatabase
import com.shearless.bareplayer.core.data.db.toEntity
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LibraryRepositoryTest {
    private lateinit var db: BareDatabase
    private lateinit var repo: DefaultLibraryRepository

    @Before
    fun setUp() = runTest {
        db = inMemoryDatabase()
        val noScanner = object : MediaScanner {
            override fun changeToken(): String? = null
            override suspend fun scan(): List<Track> = emptyList()
        }
        val noTokens = object : ScanTokenStore {
            override suspend fun read(): String? = null
            override suspend fun write(token: String?) = Unit
        }
        repo = DefaultLibraryRepository(db, LibrarySync(db, noScanner, noTokens, TestScope()))
        db.trackDao().upsert(
            listOf(
                track(1, title = "Come Together", artist = "The Beatles", artistId = 10, album = "Abbey Road", albumId = 100, genre = "Rock", trackNumber = 1),
                track(2, title = "Something", artist = "The Beatles", artistId = 10, album = "Abbey Road", albumId = 100, genre = "rock", trackNumber = 2),
                track(3, title = "Here Comes the Sun", artist = "The Beatles", artistId = 10, album = "Abbey Road", albumId = 100, genre = "Rock", trackNumber = 7),
                track(4, title = "Teardrop", artist = "Massive Attack", artistId = 20, album = "Mezzanine", albumId = 200, genre = "Trip-Hop", folder = "/storage/emulated/0/Music/Mezzanine"),
                track(5, title = "Café del Mar", artist = "Energy 52", artistId = 30, album = "Singles", albumId = 300),
            ).map { it.toEntity() },
        )
    }

    @After fun tearDown() { db.close() }

    @Test
    fun albums_areAggregatedAndSortedIgnoringArticles() = runTest {
        val albums = repo.albums().first()
        assertEquals(listOf("Abbey Road", "Mezzanine", "Singles"), albums.map { it.title })
        val abbey = albums.first()
        assertEquals(3, abbey.trackCount)
        assertEquals("The Beatles", abbey.artist)
        assertEquals("content://media/external/audio/media/1", abbey.artKey.trackUri)
    }

    @Test
    fun artists_sortIgnoringLeadingThe() = runTest {
        assertEquals(listOf("The Beatles", "Energy 52", "Massive Attack"), repo.artists().first().map { it.name })
    }

    @Test
    fun albumTracks_followTrackNumbers() = runTest {
        assertEquals(listOf(1L, 2L, 3L), repo.albumTracks(100).first().map { it.id })
    }

    @Test
    fun genres_groupCaseInsensitivelyAndSkipBlank() = runTest {
        val genres = repo.genres().first()
        assertEquals(listOf("Rock" to 3, "Trip-Hop" to 1), genres.map { it.name.replaceFirstChar(Char::uppercase) to it.trackCount })
        assertEquals(3, repo.genreTracks("ROCK").first().size)
    }

    @Test
    fun folders_exposeLastPathSegmentAsName() = runTest {
        val folders = repo.folders().first()
        assertEquals(listOf("Music", "Mezzanine"), folders.map { it.name })
    }

    @Test
    fun search_matchesPrefixesAcrossColumnsAndIgnoresDiacritics() = runTest {
        assertEquals(setOf(1L, 3L), repo.search("com").first().tracks.map { it.id }.toSet())
        assertEquals(listOf(4L), repo.search("massive tear").first().tracks.map { it.id })
        assertEquals(listOf(5L), repo.search("cafe").first().tracks.map { it.id })
        assertEquals(listOf("Abbey Road"), repo.search("abbey").first().albums.map { it.title })
        assertEquals(listOf("Massive Attack"), repo.search("attack").first().artists.map { it.name })
        assertTrue(repo.search("  ").first().isEmpty)
    }

    @Test
    fun tracksByIds_preservesRequestedOrderAndDropsMissing() = runTest {
        assertEquals(listOf(3L, 1L), repo.tracksByIds(listOf(3, 99, 1)).map { it.id })
    }
}
