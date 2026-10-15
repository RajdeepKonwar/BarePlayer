package com.rajdeepkonwar.bareplayer.core.domain.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderFilterTest {
    private val rules = setOf("/storage/emulated/0/Recordings/")

    @Test
    fun excludesFolderAndSubfolders() {
        assertTrue(FolderFilter.isExcluded("/storage/emulated/0/Recordings", rules))
        assertTrue(FolderFilter.isExcluded("/storage/emulated/0/Recordings/2026/Calls", rules))
        assertTrue(FolderFilter.isExcluded("/storage/emulated/0/recordings/old", rules))
    }

    @Test
    fun doesNotExcludeSiblingsWithSharedPrefix() {
        assertFalse(FolderFilter.isExcluded("/storage/emulated/0/Recordings Archive", rules))
        assertFalse(FolderFilter.isExcluded("/storage/emulated/0/Music", rules))
        assertFalse(FolderFilter.isExcluded("/storage/emulated/0/Music", emptySet()))
        assertFalse(FolderFilter.isExcluded("/storage/emulated/0/Music", setOf("  ")))
    }

    @Test
    fun apply_dropsOnlyExcludedTracks() {
        val kept = testTrack(1, folder = "/storage/emulated/0/Music")
        val dropped = testTrack(2, folder = "/storage/emulated/0/Recordings/2026")
        assertEquals(listOf(kept), FolderFilter.apply(listOf(kept, dropped), rules))
    }

    @Test
    fun fingerprint_ignoresOrderCaseAndTrailingSlashes() {
        assertEquals(FolderFilter.fingerprint(listOf("/a/B/", "/c")), FolderFilter.fingerprint(listOf("/c", "/a/b")))
        assertNotEquals(FolderFilter.fingerprint(listOf("/a")), FolderFilter.fingerprint(listOf("/a", "/c")))
        assertEquals(FolderFilter.fingerprint(emptyList()), FolderFilter.fingerprint(listOf("")))
    }
}
