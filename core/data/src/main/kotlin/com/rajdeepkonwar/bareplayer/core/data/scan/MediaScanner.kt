package com.rajdeepkonwar.bareplayer.core.data.scan

import com.rajdeepkonwar.bareplayer.core.model.Track

/** Source of truth for audio files on the device. */
interface MediaScanner {
    /**
     * A token that changes whenever the media database may have changed, or null when the
     * platform cannot provide one (API < 30). Used to skip redundant scans at startup.
     */
    fun changeToken(): String?

    suspend fun scan(): List<Track>
}
