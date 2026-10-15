package com.rajdeepkonwar.bareplayer.core.playback

import androidx.media3.common.MediaItem
import com.rajdeepkonwar.bareplayer.core.model.ArtKey
import com.rajdeepkonwar.bareplayer.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MediaItemsTest {
    private val track = Track(
        id = 42,
        uri = "content://media/external/audio/media/42",
        title = "Teardrop",
        artist = "Massive Attack",
        artistId = 7,
        album = "Mezzanine",
        albumId = 9,
        albumArtist = "Massive Attack",
        genre = "Trip-Hop",
        durationMs = 330_000,
        trackNumber = 3,
        discNumber = 1,
        year = 1998,
        folderPath = "/storage/emulated/0/Music/Mezzanine",
        fileName = "03 Teardrop.flac",
        mimeType = "audio/flac",
        sizeBytes = 40_000_000,
        dateAddedSec = 1,
        dateModifiedSec = 2,
    )

    @Test
    fun toMediaItem_thenToQueueItem_preservesDisplayFields() {
        val item = track.toMediaItem().toQueueItem()
        assertEquals(42L, item.trackId)
        assertEquals("Teardrop", item.title)
        assertEquals("Massive Attack", item.artist)
        assertEquals("Mezzanine", item.album)
        assertEquals(330_000L, item.durationMs)
        assertEquals(ArtKey(9, track.uri, track.folderPath), item.artKey)
    }

    @Test
    fun withPlayableUri_restoresUriStrippedInTransit() {
        val original = track.toMediaItem()
        val stripped = MediaItem.Builder()
            .setMediaId(original.mediaId)
            .setRequestMetadata(original.requestMetadata)
            .setMediaMetadata(original.mediaMetadata)
            .build()
        assertNull(stripped.localConfiguration)
        assertEquals(track.uri, stripped.withPlayableUri().localConfiguration?.uri.toString())
    }

    @Test
    fun blankTags_areNotSentToTheSession() {
        val metadata = track.copy(artist = "", album = "", genre = "").toMediaItem().mediaMetadata
        assertNull(metadata.artist)
        assertNull(metadata.albumTitle)
        assertNull(metadata.genre)
    }
}
