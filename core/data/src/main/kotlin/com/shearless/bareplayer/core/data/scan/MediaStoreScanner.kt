package com.shearless.bareplayer.core.data.scan

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.shearless.bareplayer.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreScanner(context: Context) : MediaScanner {
    private val appContext = context.applicationContext
    private val resolver: ContentResolver = appContext.contentResolver

    override fun changeToken(): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return runCatching {
            val version = MediaStore.getVersion(appContext, MediaStore.VOLUME_EXTERNAL)
            val generation = MediaStore.getGeneration(appContext, MediaStore.VOLUME_EXTERNAL)
            "$version:$generation"
        }.getOrNull()
    }

    override suspend fun scan(): List<Track> = withContext(Dispatchers.IO) {
        val sdk = Build.VERSION.SDK_INT
        val collection = audioCollection()
        val legacyGenres = if (sdk < Build.VERSION_CODES.R) queryLegacyGenres() else emptyMap()
        val cursor = resolver.query(
            collection,
            TrackCursorReader.projection(sdk),
            "${MediaStore.Audio.AudioColumns.IS_MUSIC} != 0",
            null,
            null,
        ) ?: return@withContext emptyList()
        cursor.use { TrackCursorReader(it, sdk).readAll(collection, legacyGenres) }
    }

    /** Pre-Android 11 genres live in a separate table; build a track-id to genre map once per scan. */
    private fun queryLegacyGenres(): Map<Long, String> {
        val result = HashMap<Long, String>()
        val genres = resolver.query(
            MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME),
            null,
            null,
            null,
        ) ?: return result
        genres.use { g ->
            val idCol = g.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID)
            val nameCol = g.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME)
            while (g.moveToNext()) {
                val name = g.getString(nameCol)?.trim().orEmpty()
                if (name.isEmpty()) continue
                val members = resolver.query(
                    MediaStore.Audio.Genres.Members.getContentUri("external", g.getLong(idCol)),
                    arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID),
                    null,
                    null,
                    null,
                ) ?: continue
                members.use { m ->
                    val audioCol = m.getColumnIndexOrThrow(MediaStore.Audio.Genres.Members.AUDIO_ID)
                    while (m.moveToNext()) result.putIfAbsent(m.getLong(audioCol), name)
                }
            }
        }
        return result
    }

    private fun audioCollection(): Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
}

/** Maps MediaStore audio rows to [Track]s. Separate from the scanner so it can be unit tested. */
internal class TrackCursorReader(private val cursor: Cursor, private val sdk: Int) {

    fun readAll(collection: Uri, legacyGenres: Map<Long, String>): List<Track> {
        val c = cursor
        val id = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val title = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val artistId = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
        val album = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val albumId = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
        val duration = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val track = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
        val year = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
        val data = c.getColumnIndex(DATA)
        val displayName = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
        val mime = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
        val size = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
        val added = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
        val modified = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
        val relativePath = c.getColumnIndex(RELATIVE_PATH)
        val volume = c.getColumnIndex(VOLUME_NAME)
        val albumArtist = c.getColumnIndex(ALBUM_ARTIST)
        val genre = c.getColumnIndex(GENRE)
        val discNumber = c.getColumnIndex(DISC_NUMBER)

        val result = ArrayList<Track>(c.count.coerceAtLeast(0))
        while (c.moveToNext()) {
            val trackId = c.getLong(id)
            val fileName = c.getStringOrEmpty(displayName)
            val artistName = c.getStringOrEmpty(artist).normalizeUnknown()
            val encodedTrack = c.getIntOrZero(track)
            val disc = (encodedTrack / 1000).takeIf { it > 0 }
                ?: parseLeadingInt(c.getStringOrEmpty(discNumber))
            result += Track(
                id = trackId,
                uri = ContentUris.withAppendedId(collection, trackId).toString(),
                title = c.getStringOrEmpty(title).ifBlank { fileName.substringBeforeLast('.') },
                artist = artistName,
                artistId = c.getLong(artistId),
                album = c.getStringOrEmpty(album).normalizeUnknown(),
                albumId = c.getLong(albumId),
                albumArtist = c.getStringOrEmpty(albumArtist).normalizeUnknown().ifEmpty { artistName },
                genre = c.getStringOrEmpty(genre).ifEmpty { legacyGenres[trackId].orEmpty() }.trim(),
                durationMs = c.getLongOrZero(duration),
                trackNumber = encodedTrack % 1000,
                discNumber = disc,
                year = c.getIntOrZero(year),
                folderPath = folderPath(
                    data = c.getStringOrEmpty(data),
                    volume = c.getStringOrEmpty(volume),
                    relativePath = c.getStringOrEmpty(relativePath),
                ),
                fileName = fileName,
                mimeType = c.getStringOrEmpty(mime),
                sizeBytes = c.getLongOrZero(size),
                dateAddedSec = c.getLongOrZero(added),
                dateModifiedSec = c.getLongOrZero(modified),
            )
        }
        return result
    }

    companion object {
        // Column names spelled out so older SDK constants (or their absence) don't matter.
        private const val DATA = "_data"
        private const val RELATIVE_PATH = "relative_path"
        private const val VOLUME_NAME = "volume_name"
        private const val ALBUM_ARTIST = "album_artist"
        private const val GENRE = "genre"
        private const val DISC_NUMBER = "disc_number"

        fun projection(sdk: Int): Array<String> = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ARTIST_ID)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.ALBUM_ID)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.TRACK)
            add(MediaStore.Audio.Media.YEAR)
            add(DATA)
            add(MediaStore.Audio.Media.DISPLAY_NAME)
            add(MediaStore.Audio.Media.MIME_TYPE)
            add(MediaStore.Audio.Media.SIZE)
            add(MediaStore.Audio.Media.DATE_ADDED)
            add(MediaStore.Audio.Media.DATE_MODIFIED)
            if (sdk >= Build.VERSION_CODES.Q) {
                add(RELATIVE_PATH)
                add(VOLUME_NAME)
            }
            if (sdk >= Build.VERSION_CODES.R) {
                add(ALBUM_ARTIST)
                add(GENRE)
                add(DISC_NUMBER)
            }
        }.toTypedArray()

        internal fun folderPath(data: String, volume: String, relativePath: String): String {
            val fromData = data.substringBeforeLast('/', missingDelimiterValue = "")
            if (fromData.isNotEmpty()) return fromData
            val relative = relativePath.trim('/')
            return listOf(volume, relative).filter { it.isNotEmpty() }.joinToString("/")
        }

        internal fun parseLeadingInt(value: String): Int =
            value.trim().takeWhile { it.isDigit() }.toIntOrNull() ?: 0

        private fun String.normalizeUnknown(): String =
            if (this == MediaStore.UNKNOWN_STRING) "" else trim()

        private fun Cursor.getStringOrEmpty(index: Int): String =
            if (index < 0 || isNull(index)) "" else getString(index).orEmpty()

        private fun Cursor.getLongOrZero(index: Int): Long =
            if (index < 0 || isNull(index)) 0L else getLong(index)

        private fun Cursor.getIntOrZero(index: Int): Int =
            if (index < 0 || isNull(index)) 0 else getInt(index)
    }
}
