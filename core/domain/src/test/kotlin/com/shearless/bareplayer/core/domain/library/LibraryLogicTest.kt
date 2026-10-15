package com.shearless.bareplayer.core.domain.library

import com.shearless.bareplayer.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

fun testTrack(
    id: Long,
    title: String = "Track $id",
    artist: String = "Artist",
    album: String = "Album",
    albumId: Long = 1,
    genre: String = "",
    folder: String = "/music",
    modified: Long = 100,
) = Track(
    id = id,
    uri = "content://media/external/audio/media/$id",
    title = title,
    artist = artist,
    artistId = artist.hashCode().toLong(),
    album = album,
    albumId = albumId,
    albumArtist = artist,
    genre = genre,
    durationMs = 180_000,
    trackNumber = id.toInt(),
    discNumber = 1,
    year = 2020,
    folderPath = folder,
    fileName = "$title.mp3",
    mimeType = "audio/mpeg",
    sizeBytes = 1_000,
    dateAddedSec = 1,
    dateModifiedSec = modified,
)

class LibraryDiffTest {
    @Test
    fun emptyCache_insertsEverything() {
        val scanned = listOf(testTrack(1), testTrack(2))
        val diff = LibraryDiff.compute(scanned, emptyMap())
        assertEquals(scanned, diff.upserts)
        assertTrue(diff.deletedIds.isEmpty())
    }

    @Test
    fun unchangedTracks_produceEmptyDiff() {
        val scanned = listOf(testTrack(1), testTrack(2))
        val cached = scanned.associate { it.id to LibraryDiff.fingerprint(it) }
        assertTrue(LibraryDiff.compute(scanned, cached).isEmpty)
    }

    @Test
    fun modifiedAndRemovedTracks_areDetected() {
        val old = listOf(testTrack(1), testTrack(2), testTrack(3))
        val cached = old.associate { it.id to LibraryDiff.fingerprint(it) }
        val retagged = testTrack(2, title = "New title")
        val diff = LibraryDiff.compute(listOf(testTrack(1), retagged, testTrack(4)), cached)
        assertEquals(listOf(retagged, testTrack(4)), diff.upserts)
        assertEquals(listOf(3L), diff.deletedIds)
    }
}

class SortKeysTest {
    @Test
    fun sortKey_ignoresCaseAccentsAndLeadingArticles() {
        assertEquals("beatles", SortKeys.sortKey("The Beatles"))
        assertEquals("ete", SortKeys.sortKey("  Été"))
        assertEquals("perfect circle", SortKeys.sortKey("A Perfect Circle"))
        assertEquals("the", SortKeys.sortKey("The"))
    }

    @Test
    fun sectionOf_groupsNonLettersUnderHash() {
        assertEquals('B', SortKeys.sectionOf("the beatles"))
        assertEquals('#', SortKeys.sectionOf("2Pac"))
        assertEquals('#', SortKeys.sectionOf(""))
        assertEquals('E', SortKeys.sectionOf("Énigme"))
    }
}

class SearchQueryTest {
    @Test
    fun toFtsMatch_buildsQuotedPrefixTokens() {
        assertEquals("\"beat*\" \"it*\"", SearchQuery.toFtsMatch("  Beat   IT "))
    }

    @Test
    fun toFtsMatch_stripsFtsSyntax() {
        // Operators become quoted literal terms, so user input can never form FTS syntax.
        assertEquals("\"ac*\" \"dc*\" \"or*\"", SearchQuery.toFtsMatch("AC/DC\" OR *"))
        assertNull(SearchQuery.toFtsMatch("\"*()"))
    }

    @Test
    fun toLikePattern_escapesWildcards() {
        assertEquals("%100\\%\\_x%", SearchQuery.toLikePattern(" 100%_x "))
        assertNull(SearchQuery.toLikePattern("   "))
    }
}
