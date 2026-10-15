package com.shearless.bareplayer.core.domain.playback

sealed interface PlaybackError {
    val title: String?

    /** The device has no decoder for this file (e.g. FLAC on Android 8.0). The track is skipped. */
    data class UnsupportedFormat(override val title: String?) : PlaybackError

    /** The file could not be opened or decoded (deleted, corrupt, ...). */
    data class CannotPlay(override val title: String?) : PlaybackError
}
