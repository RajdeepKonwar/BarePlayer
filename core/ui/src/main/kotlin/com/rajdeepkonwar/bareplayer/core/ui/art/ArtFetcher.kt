package com.rajdeepkonwar.bareplayer.core.ui.art

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.core.net.toUri
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.fetch.SourceFetchResult
import coil3.key.Keyer
import coil3.request.Options
import coil3.size.pxOrElse
import com.rajdeepkonwar.bareplayer.core.model.ArtKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import java.io.File

/**
 * Resolves artwork for an [ArtKey] without any extra permission:
 * - API 29+: MediaStore thumbnails, which the system builds from embedded art and falls back to
 *   folder images (cover.jpg, folder.jpg, albumart*) on its own.
 * - API 26-28: MediaStore album art, then embedded art, then cover/folder images next to the file
 *   (readable thanks to READ_EXTERNAL_STORAGE on these versions).
 */
class ArtFetcher(
    private val key: ArtKey,
    private val options: Options,
    private val context: Context,
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        val trackUri = key.trackUri.toUri()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            loadThumbnail(trackUri)?.let { return@withContext it }
        } else {
            legacyAlbumArt()?.let { return@withContext it }
        }
        embeddedPicture(trackUri)?.let { return@withContext it }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) folderImage()?.let { return@withContext it }
        null
    }

    private fun loadThumbnail(uri: Uri): FetchResult? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val width = options.size.width.pxOrElse { DEFAULT_SIZE_PX }
        val height = options.size.height.pxOrElse { DEFAULT_SIZE_PX }
        return runCatching {
            val bitmap = context.contentResolver.loadThumbnail(uri, Size(width, height), null)
            ImageFetchResult(image = bitmap.asImage(), isSampled = true, dataSource = DataSource.DISK)
        }.getOrNull()
    }

    private fun legacyAlbumArt(): FetchResult? = runCatching {
        val uri = ContentUris.withAppendedId(LEGACY_ALBUM_ART, key.albumId)
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        bytes.toResult()
    }.getOrNull()

    private fun embeddedPicture(uri: Uri): FetchResult? = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            retriever.embeddedPicture?.toResult()
        } finally {
            retriever.release()
        }
    }.getOrNull()

    private fun folderImage(): FetchResult? {
        if (key.folderPath.isEmpty()) return null
        val folder = File(key.folderPath)
        val file = FOLDER_ART_NAMES.asSequence().map { File(folder, it) }.firstOrNull { it.canRead() } ?: return null
        return SourceFetchResult(
            source = ImageSource(file.toOkioPath(), FileSystem.SYSTEM),
            mimeType = null,
            dataSource = DataSource.DISK,
        )
    }

    private fun ByteArray.toResult(): FetchResult = SourceFetchResult(
        source = ImageSource(Buffer().write(this), FileSystem.SYSTEM),
        mimeType = null,
        dataSource = DataSource.DISK,
    )

    class Factory : Fetcher.Factory<ArtKey> {
        override fun create(data: ArtKey, options: Options, imageLoader: ImageLoader): Fetcher =
            ArtFetcher(data, options, options.context)
    }

    /** Art is shared by every track of an album, so cache by album. */
    class AlbumKeyer : Keyer<ArtKey> {
        override fun key(data: ArtKey, options: Options): String = "album-art:${data.albumId}"
    }

    private companion object {
        const val DEFAULT_SIZE_PX = 512
        val LEGACY_ALBUM_ART: Uri = "content://media/external/audio/albumart".toUri()
        val FOLDER_ART_NAMES = listOf(
            "cover.jpg", "cover.png", "folder.jpg", "folder.png",
            "Cover.jpg", "Cover.png", "Folder.jpg", "Folder.png", "AlbumArt.jpg",
        )
    }
}
