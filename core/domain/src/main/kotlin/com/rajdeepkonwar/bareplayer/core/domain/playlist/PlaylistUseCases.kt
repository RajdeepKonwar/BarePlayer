package com.rajdeepkonwar.bareplayer.core.domain.playlist

import com.rajdeepkonwar.bareplayer.core.domain.repository.PlaylistRepository

object PlaylistNames {
    const val MAX_LENGTH = 80
    private val WHITESPACE = Regex("\\s+")

    /** Trims, collapses inner whitespace and caps the length. Null when nothing is left. */
    fun normalize(input: String): String? =
        input.trim().replace(WHITESPACE, " ").take(MAX_LENGTH).trimEnd().ifEmpty { null }

    /** [base], or "[base] 2", "[base] 3", ... — the first name not already taken (case-insensitive). */
    fun suggest(base: String, existing: Collection<String>): String {
        val taken = existing.mapTo(HashSet()) { it.lowercase() }
        if (base.lowercase() !in taken) return base
        return generateSequence(2) { it + 1 }.map { "$base $it" }.first { it.lowercase() !in taken }
    }
}

sealed interface PlaylistTarget {
    data class Existing(val id: Long, val name: String) : PlaylistTarget
    data class New(val name: String) : PlaylistTarget
}

data class AddToPlaylistResult(val playlistId: Long, val playlistName: String, val addedCount: Int)

/** Adds tracks to an existing playlist, or creates the playlist first. */
class AddToPlaylistUseCase(private val playlists: PlaylistRepository) {
    suspend operator fun invoke(target: PlaylistTarget, trackIds: List<Long>): AddToPlaylistResult {
        val (id, name) = when (target) {
            is PlaylistTarget.Existing -> target.id to target.name
            is PlaylistTarget.New -> {
                val name = requireNotNull(PlaylistNames.normalize(target.name)) { "Playlist name must not be blank" }
                playlists.create(name) to name
            }
        }
        playlists.addTracks(id, trackIds)
        return AddToPlaylistResult(id, name, trackIds.size)
    }
}
