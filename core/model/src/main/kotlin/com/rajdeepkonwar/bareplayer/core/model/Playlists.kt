package com.rajdeepkonwar.bareplayer.core.model

data class Playlist(
    val id: Long,
    val name: String,
    val trackCount: Int,
    val createdAtMs: Long,
    val updatedAtMs: Long,
)

/** A track inside a playlist; [entryId] distinguishes duplicates of the same track. */
data class PlaylistEntry(
    val entryId: Long,
    val position: Int,
    val track: Track,
)
