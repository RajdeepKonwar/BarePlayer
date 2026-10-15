package com.rajdeepkonwar.bareplayer.core.data.db

import com.rajdeepkonwar.bareplayer.core.domain.library.LibraryDiff
import com.rajdeepkonwar.bareplayer.core.domain.library.SortKeys
import com.rajdeepkonwar.bareplayer.core.model.Album
import com.rajdeepkonwar.bareplayer.core.model.ArtKey
import com.rajdeepkonwar.bareplayer.core.model.Artist
import com.rajdeepkonwar.bareplayer.core.model.Folder
import com.rajdeepkonwar.bareplayer.core.model.Genre
import com.rajdeepkonwar.bareplayer.core.model.Playlist
import com.rajdeepkonwar.bareplayer.core.model.PlaylistEntry
import com.rajdeepkonwar.bareplayer.core.model.QueueSnapshot
import com.rajdeepkonwar.bareplayer.core.model.RepeatMode
import com.rajdeepkonwar.bareplayer.core.model.Track

internal fun TrackEntity.toModel() = Track(
    id = id,
    uri = uri,
    title = title,
    artist = artist,
    artistId = artistId,
    album = album,
    albumId = albumId,
    albumArtist = albumArtist,
    genre = genre,
    durationMs = durationMs,
    trackNumber = trackNumber,
    discNumber = discNumber,
    year = year,
    folderPath = folderPath,
    fileName = fileName,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    dateAddedSec = dateAddedSec,
    dateModifiedSec = dateModifiedSec,
)

internal fun Track.toEntity() = TrackEntity(
    id = id,
    uri = uri,
    title = title,
    titleSort = SortKeys.sortKey(title),
    artist = artist,
    artistSort = SortKeys.sortKey(artist),
    artistId = artistId,
    album = album,
    albumSort = SortKeys.sortKey(album),
    albumId = albumId,
    albumArtist = albumArtist,
    genre = genre,
    durationMs = durationMs,
    trackNumber = trackNumber,
    discNumber = discNumber,
    year = year,
    folderPath = folderPath,
    fileName = fileName,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    dateAddedSec = dateAddedSec,
    dateModifiedSec = dateModifiedSec,
    fingerprint = LibraryDiff.fingerprint(this),
)

internal fun AlbumRow.toModel() = Album(
    id = id,
    title = title,
    artist = artist,
    year = year,
    trackCount = trackCount,
    durationMs = durationMs,
    artKey = ArtKey(albumId = id, trackUri = artUri, folderPath = artFolder),
)

internal fun ArtistRow.toModel() = Artist(
    id = id,
    name = name,
    albumCount = albumCount,
    trackCount = trackCount,
    artKey = ArtKey(albumId = artAlbumId, trackUri = artUri, folderPath = artFolder),
)

internal fun GenreRow.toModel() = Genre(name = name, trackCount = trackCount)

internal fun FolderRow.toModel() = Folder(
    path = path,
    name = path.trimEnd('/').substringAfterLast('/').ifEmpty { path },
    trackCount = trackCount,
)

internal fun PlaylistRow.toModel() = Playlist(
    id = id,
    name = name,
    trackCount = trackCount,
    createdAtMs = createdAt,
    updatedAtMs = updatedAt,
)

internal fun PlaylistEntryRow.toModel() = PlaylistEntry(
    entryId = entryId,
    position = position,
    track = track.toModel(),
)

internal fun QueueSnapshot.toEntity() = QueueStateEntity(
    trackIds = trackIds.joinToString(","),
    currentIndex = currentIndex,
    positionMs = positionMs,
    shuffleEnabled = shuffleEnabled,
    shuffleOrder = shuffleOrder.joinToString(","),
    repeatMode = repeatMode.ordinal,
)

internal fun QueueStateEntity.toModel() = QueueSnapshot(
    trackIds = trackIds.splitToNumbers { it.toLongOrNull() },
    currentIndex = currentIndex,
    positionMs = positionMs,
    shuffleEnabled = shuffleEnabled,
    shuffleOrder = shuffleOrder.splitToNumbers { it.toIntOrNull() },
    repeatMode = RepeatMode.entries.getOrElse(repeatMode) { RepeatMode.OFF },
)

private inline fun <T : Any> String.splitToNumbers(parse: (String) -> T?): List<T> =
    if (isEmpty()) emptyList() else split(',').mapNotNull(parse)
