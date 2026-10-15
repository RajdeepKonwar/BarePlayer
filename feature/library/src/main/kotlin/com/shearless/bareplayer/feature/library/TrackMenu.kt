package com.shearless.bareplayer.feature.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.shearless.bareplayer.core.domain.playback.PlaybackController
import com.shearless.bareplayer.core.model.Track
import com.shearless.bareplayer.core.ui.components.TrackMenuActions
import com.shearless.bareplayer.core.ui.components.rememberDeleteSong

@Composable
internal fun rememberTrackMenu(
    playback: PlaybackController,
    onAddToPlaylist: (List<Long>) -> Unit,
    onOpenCollection: (CollectionRoute) -> Unit,
): TrackMenuActions {
    val addToPlaylist by rememberUpdatedState(onAddToPlaylist)
    val open by rememberUpdatedState(onOpenCollection)
    val deleteSong = rememberDeleteSong()
    return remember(playback) {
        object : TrackMenuActions {
            override fun playNext(track: Track) = playback.playNext(listOf(track))
            override fun addToQueue(track: Track) = playback.addToQueue(listOf(track))
            override fun addToPlaylist(track: Track) = addToPlaylist(listOf(track.id))
            override fun goToAlbum(track: Track) = open(CollectionRoute(CollectionType.ALBUM, id = track.albumId))
            override fun goToArtist(track: Track) = open(CollectionRoute(CollectionType.ARTIST, id = track.artistId))
            override fun delete(track: Track) = deleteSong(track)
        }
    }
}
