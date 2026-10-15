package com.shearless.bareplayer.core.data.db

import androidx.room.Embedded

data class IdFingerprint(val id: Long, val fingerprint: Long)

/** Aggregated album row. art* columns come from the album's lowest-id track (SQLite bare-column MIN). */
data class AlbumRow(
    val id: Long,
    val title: String,
    val artist: String,
    val year: Int,
    val trackCount: Int,
    val durationMs: Long,
    val artTrackId: Long,
    val artUri: String,
    val artFolder: String,
)

data class ArtistRow(
    val id: Long,
    val name: String,
    val albumCount: Int,
    val trackCount: Int,
    val artTrackId: Long,
    val artAlbumId: Long,
    val artUri: String,
    val artFolder: String,
)

data class GenreRow(val name: String, val trackCount: Int)

data class FolderRow(val path: String, val trackCount: Int)

data class PlaylistRow(
    val id: Long,
    val name: String,
    val trackCount: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

data class PlaylistEntryRow(
    val entryId: Long,
    val position: Int,
    @Embedded val track: TrackEntity,
)
