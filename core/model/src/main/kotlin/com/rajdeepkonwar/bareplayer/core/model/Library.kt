package com.rajdeepkonwar.bareplayer.core.model

/**
 * A single audio file from the device library. [uri] is a `content://` URI kept as a String so this
 * module stays free of Android types.
 */
data class Track(
    val id: Long,
    val uri: String,
    val title: String,
    val artist: String,
    val artistId: Long,
    val album: String,
    val albumId: Long,
    val albumArtist: String,
    val genre: String,
    val durationMs: Long,
    val trackNumber: Int,
    val discNumber: Int,
    val year: Int,
    val folderPath: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val dateAddedSec: Long,
    val dateModifiedSec: Long,
) {
    val artKey: ArtKey get() = ArtKey(albumId = albumId, trackUri = uri, folderPath = folderPath)
}

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val year: Int,
    val trackCount: Int,
    val durationMs: Long,
    val artKey: ArtKey,
)

data class Artist(
    val id: Long,
    val name: String,
    val albumCount: Int,
    val trackCount: Int,
    val artKey: ArtKey,
)

data class Genre(
    val name: String,
    val trackCount: Int,
)

data class Folder(
    val path: String,
    val name: String,
    val trackCount: Int,
)

data class SearchResults(
    val query: String,
    val tracks: List<Track>,
    val albums: List<Album>,
    val artists: List<Artist>,
) {
    val isEmpty: Boolean get() = tracks.isEmpty() && albums.isEmpty() && artists.isEmpty()

    companion object {
        val Empty = SearchResults("", emptyList(), emptyList(), emptyList())
    }
}

/**
 * Identifies the artwork for a track or album. Artwork is resolved lazily by the UI layer:
 * embedded art first, then folder art (cover.jpg / folder.jpg), then a placeholder.
 */
data class ArtKey(
    val albumId: Long,
    val trackUri: String,
    val folderPath: String,
)

sealed interface ScanState {
    data object Idle : ScanState
    data object Scanning : ScanState
    data class Done(val trackCount: Int, val durationMs: Long) : ScanState
    data class Failed(val message: String) : ScanState
}
