package com.rajdeepkonwar.bareplayer.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rajdeepkonwar.bareplayer.core.data.db.BareDatabase
import com.rajdeepkonwar.bareplayer.core.model.Track

fun inMemoryDatabase(): BareDatabase =
    Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), BareDatabase::class.java)
        .allowMainThreadQueries()
        .build()

fun track(
    id: Long,
    title: String = "Track $id",
    artist: String = "Artist",
    artistId: Long = 1,
    album: String = "Album",
    albumId: Long = 1,
    genre: String = "",
    folder: String = "/storage/emulated/0/Music",
    trackNumber: Int = id.toInt(),
    modified: Long = 100,
) = Track(
    id = id,
    uri = "content://media/external/audio/media/$id",
    title = title,
    artist = artist,
    artistId = artistId,
    album = album,
    albumId = albumId,
    albumArtist = artist,
    genre = genre,
    durationMs = 200_000,
    trackNumber = trackNumber,
    discNumber = 1,
    year = 2021,
    folderPath = folder,
    fileName = "$title.flac",
    mimeType = "audio/flac",
    sizeBytes = 10_000,
    dateAddedSec = 1,
    dateModifiedSec = modified,
)
