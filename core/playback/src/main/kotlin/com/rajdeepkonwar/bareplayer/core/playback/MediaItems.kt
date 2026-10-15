package com.rajdeepkonwar.bareplayer.core.playback

import android.content.ContentUris
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.rajdeepkonwar.bareplayer.core.model.ArtKey
import com.rajdeepkonwar.bareplayer.core.model.QueueItem
import com.rajdeepkonwar.bareplayer.core.model.Track

internal object PlaybackCommands {
    /** Controller -> session: replace the shuffle order. Args: [KEY_ORDER] (IntArray of playlist indices). */
    const val SET_SHUFFLE_ORDER = "com.rajdeepkonwar.bareplayer.SET_SHUFFLE_ORDER"

    /** Session -> controllers: a track was skipped. Args: [KEY_TITLE], [KEY_UNSUPPORTED]. */
    const val TRACK_SKIPPED = "com.rajdeepkonwar.bareplayer.TRACK_SKIPPED"

    const val KEY_ORDER = "order"
    const val KEY_TITLE = "title"
    const val KEY_UNSUPPORTED = "unsupported"
}

private const val EXTRA_ALBUM_ID = "album_id"
private const val EXTRA_FOLDER = "folder"
private const val EXTRA_URI = "uri"

private val ALBUM_ART: Uri = Uri.parse("content://media/external/audio/albumart")

internal fun Track.toMediaItem(): MediaItem {
    val uri = Uri.parse(uri)
    val extras = Bundle(3).apply {
        putLong(EXTRA_ALBUM_ID, albumId)
        putString(EXTRA_FOLDER, folderPath)
        putString(EXTRA_URI, this@toMediaItem.uri)
    }
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist.ifEmpty { null })
        .setAlbumTitle(album.ifEmpty { null })
        .setAlbumArtist(albumArtist.ifEmpty { null })
        .setGenre(genre.ifEmpty { null })
        .setTrackNumber(trackNumber.takeIf { it > 0 })
        .setDiscNumber(discNumber.takeIf { it > 0 })
        .setRecordingYear(year.takeIf { it > 0 })
        .setDurationMs(durationMs.takeIf { it > 0 })
        .setArtworkUri(ContentUris.withAppendedId(ALBUM_ART, albumId))
        .setIsPlayable(true)
        .setIsBrowsable(false)
        .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
        .setExtras(extras)
        .build()
    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        // Local configuration (the URI) is dropped when items cross from controller to session,
        // so the URI is also sent as request metadata and restored in onAddMediaItems.
        .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
        .setMediaMetadata(metadata)
        .build()
}

internal fun MediaItem.withPlayableUri(): MediaItem {
    if (localConfiguration != null) return this
    val uri = requestMetadata.mediaUri
        ?: mediaMetadata.extras?.getString(EXTRA_URI)?.let(Uri::parse)
        ?: return this
    return buildUpon().setUri(uri).build()
}

internal fun MediaItem.toQueueItem(): QueueItem {
    val metadata = mediaMetadata
    val extras = metadata.extras
    return QueueItem(
        trackId = mediaId.toLongOrNull() ?: -1L,
        title = metadata.title?.toString().orEmpty(),
        artist = metadata.artist?.toString().orEmpty(),
        album = metadata.albumTitle?.toString().orEmpty(),
        durationMs = metadata.durationMs ?: 0L,
        artKey = ArtKey(
            albumId = extras?.getLong(EXTRA_ALBUM_ID) ?: 0L,
            trackUri = extras?.getString(EXTRA_URI) ?: requestMetadata.mediaUri?.toString().orEmpty(),
            folderPath = extras?.getString(EXTRA_FOLDER).orEmpty(),
        ),
    )
}
