package com.shearless.bareplayer.core.data.scan

import android.database.MatrixCursor
import android.net.Uri
import android.provider.MediaStore
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TrackCursorReaderTest {
    private val collection = Uri.parse("content://media/external/audio/media")

    private fun cursor(sdk: Int, vararg rows: Map<String, Any?>): MatrixCursor {
        val projection = TrackCursorReader.projection(sdk)
        return MatrixCursor(projection).apply {
            rows.forEach { row -> addRow(projection.map { row[it] }) }
        }
    }

    private fun row(vararg overrides: Pair<String, Any?>): Map<String, Any?> = mapOf(
        MediaStore.Audio.Media._ID to 7L,
        MediaStore.Audio.Media.TITLE to "Song",
        MediaStore.Audio.Media.ARTIST to "Artist",
        MediaStore.Audio.Media.ARTIST_ID to 3L,
        MediaStore.Audio.Media.ALBUM to "Album",
        MediaStore.Audio.Media.ALBUM_ID to 4L,
        MediaStore.Audio.Media.DURATION to 123_000L,
        MediaStore.Audio.Media.TRACK to 2005,
        MediaStore.Audio.Media.YEAR to 1999,
        "_data" to "/storage/emulated/0/Music/Album/05 Song.flac",
        MediaStore.Audio.Media.DISPLAY_NAME to "05 Song.flac",
        MediaStore.Audio.Media.MIME_TYPE to "audio/flac",
        MediaStore.Audio.Media.SIZE to 30_000_000L,
        MediaStore.Audio.Media.DATE_ADDED to 10L,
        MediaStore.Audio.Media.DATE_MODIFIED to 20L,
        "relative_path" to "Music/Album/",
        "volume_name" to "external_primary",
        "album_artist" to "Various Artists",
        "genre" to " Jazz ",
        "disc_number" to "2/2",
    ) + overrides

    @Test
    fun mapsAllColumnsOnModernSdk() {
        val track = TrackCursorReader(cursor(35, row()), 35).readAll(collection, emptyMap()).single()
        assertEquals("content://media/external/audio/media/7", track.uri)
        assertEquals("Song", track.title)
        assertEquals("Various Artists", track.albumArtist)
        assertEquals("Jazz", track.genre)
        assertEquals(2, track.discNumber)
        assertEquals(5, track.trackNumber)
        assertEquals("/storage/emulated/0/Music/Album", track.folderPath)
    }

    @Test
    fun normalizesUnknownAndBlankValues() {
        val track = TrackCursorReader(
            cursor(
                35,
                row(
                    MediaStore.Audio.Media.TITLE to "",
                    MediaStore.Audio.Media.ARTIST to MediaStore.UNKNOWN_STRING,
                    "album_artist" to null,
                    MediaStore.Audio.Media.TRACK to 3,
                    "disc_number" to null,
                    "_data" to null,
                ),
            ),
            35,
        ).readAll(collection, emptyMap()).single()
        assertEquals("05 Song", track.title)
        assertEquals("", track.artist)
        assertEquals("", track.albumArtist)
        assertEquals(0, track.discNumber)
        assertEquals(3, track.trackNumber)
        assertEquals("external_primary/Music/Album", track.folderPath)
    }

    @Test
    fun legacySdkUsesGenreMapAndDataPath() {
        val track = TrackCursorReader(cursor(28, row()), 28).readAll(collection, mapOf(7L to "Blues")).single()
        assertEquals("Blues", track.genre)
        assertEquals("Artist", track.albumArtist)
        assertEquals(2, track.discNumber)
    }

    @Test
    fun parseLeadingInt_handlesFractionsAndGarbage() {
        assertEquals(1, TrackCursorReader.parseLeadingInt("1/2"))
        assertEquals(0, TrackCursorReader.parseLeadingInt("x"))
    }
}
