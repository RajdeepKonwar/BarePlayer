package com.rajdeepkonwar.bareplayer.core.domain.library

import com.rajdeepkonwar.bareplayer.core.model.Track

/** Result of comparing a fresh MediaStore scan against the cached index. */
data class LibraryDiff(
    val upserts: List<Track>,
    val deletedIds: List<Long>,
) {
    val isEmpty: Boolean get() = upserts.isEmpty() && deletedIds.isEmpty()

    companion object {
        /**
         * [cached] maps track id to a fingerprint of the cached row. A scanned track is rewritten
         * when it is new or its fingerprint changed (file modified, or MediaStore re-parsed tags).
         */
        fun compute(scanned: List<Track>, cached: Map<Long, Long>): LibraryDiff {
            val scannedIds = HashSet<Long>(scanned.size * 2)
            val upserts = ArrayList<Track>()
            for (track in scanned) {
                scannedIds += track.id
                if (cached[track.id] != fingerprint(track)) upserts += track
            }
            val deleted = cached.keys.filter { it !in scannedIds }
            return LibraryDiff(upserts, deleted)
        }

        fun fingerprint(track: Track): Long {
            var h = track.dateModifiedSec
            h = 31 * h + track.sizeBytes
            h = 31 * h + track.title.hashCode()
            h = 31 * h + track.artist.hashCode()
            h = 31 * h + track.album.hashCode()
            h = 31 * h + track.albumArtist.hashCode()
            h = 31 * h + track.genre.hashCode()
            h = 31 * h + track.folderPath.hashCode()
            h = 31 * h + track.trackNumber
            h = 31 * h + track.discNumber
            h = 31 * h + track.year
            h = 31 * h + track.durationMs
            return h
        }
    }
}
