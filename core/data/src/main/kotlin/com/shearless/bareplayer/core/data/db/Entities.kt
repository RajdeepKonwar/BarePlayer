package com.shearless.bareplayer.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.Index
import androidx.room.PrimaryKey

/** Cached copy of a MediaStore audio row. [id] is the MediaStore id and also the SQLite rowid. */
@Entity(
    tableName = "tracks",
    indices = [
        Index("title_sort"),
        Index("album_id"),
        Index("artist_id"),
        Index("genre"),
        Index("folder_path"),
    ],
)
data class TrackEntity(
    @PrimaryKey val id: Long,
    val uri: String,
    val title: String,
    @ColumnInfo(name = "title_sort") val titleSort: String,
    val artist: String,
    @ColumnInfo(name = "artist_sort") val artistSort: String,
    @ColumnInfo(name = "artist_id") val artistId: Long,
    val album: String,
    @ColumnInfo(name = "album_sort") val albumSort: String,
    @ColumnInfo(name = "album_id") val albumId: Long,
    @ColumnInfo(name = "album_artist") val albumArtist: String,
    val genre: String,
    @ColumnInfo(name = "duration_ms") val durationMs: Long,
    @ColumnInfo(name = "track_number") val trackNumber: Int,
    @ColumnInfo(name = "disc_number") val discNumber: Int,
    val year: Int,
    @ColumnInfo(name = "folder_path") val folderPath: String,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    @ColumnInfo(name = "date_added") val dateAddedSec: Long,
    @ColumnInfo(name = "date_modified") val dateModifiedSec: Long,
    val fingerprint: Long,
)

/** Full-text index over [TrackEntity]; Room keeps it in sync with triggers. */
@Fts4(contentEntity = TrackEntity::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "tracks_fts")
data class TrackFtsEntity(
    val title: String,
    val artist: String,
    val album: String,
    @ColumnInfo(name = "album_artist") val albumArtist: String,
    val genre: String,
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

/**
 * Playlist entries intentionally have no foreign key to tracks: a track that disappears (e.g. an SD
 * card is removed) is hidden, and reappears when the file comes back.
 */
@Entity(
    tableName = "playlist_entries",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlist_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("playlist_id", "position"), Index("track_id")],
)
data class PlaylistEntryEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "entry_id") val entryId: Long = 0,
    @ColumnInfo(name = "playlist_id") val playlistId: Long,
    @ColumnInfo(name = "track_id") val trackId: Long,
    val position: Int,
)

/** Single-row table holding the last playback queue. */
@Entity(tableName = "queue_state")
data class QueueStateEntity(
    @PrimaryKey val id: Int = 0,
    @ColumnInfo(name = "track_ids") val trackIds: String,
    @ColumnInfo(name = "current_index") val currentIndex: Int,
    @ColumnInfo(name = "position_ms") val positionMs: Long,
    @ColumnInfo(name = "shuffle_enabled") val shuffleEnabled: Boolean,
    @ColumnInfo(name = "shuffle_order") val shuffleOrder: String,
    @ColumnInfo(name = "repeat_mode") val repeatMode: Int,
)

@Entity(tableName = "play_counts")
data class PlayCountEntity(
    @PrimaryKey @ColumnInfo(name = "track_id") val trackId: Long,
    val count: Int,
    @ColumnInfo(name = "last_played") val lastPlayed: Long,
)
