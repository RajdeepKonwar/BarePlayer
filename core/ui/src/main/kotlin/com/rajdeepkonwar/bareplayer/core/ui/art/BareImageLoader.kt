package com.rajdeepkonwar.bareplayer.core.ui.art

import android.content.Context
import coil3.ImageLoader
import coil3.memory.MemoryCache
import coil3.request.crossfade

object BareImageLoader {
    /** Local-only image loader: no network fetchers, no disk cache (MediaStore caches thumbnails). */
    fun create(context: Context): ImageLoader = ImageLoader.Builder(context)
        .components {
            add(ArtFetcher.AlbumKeyer())
            add(ArtFetcher.Factory())
        }
        .memoryCache { MemoryCache.Builder().maxSizePercent(context, 0.20).build() }
        .diskCache(null)
        .crossfade(150)
        .build()
}
