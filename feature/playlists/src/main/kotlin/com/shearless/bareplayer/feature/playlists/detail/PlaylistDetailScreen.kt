package com.shearless.bareplayer.feature.playlists.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shearless.bareplayer.core.model.PlaylistEntry
import com.shearless.bareplayer.core.model.Track
import com.shearless.bareplayer.core.ui.LocalAppDependencies
import com.shearless.bareplayer.core.ui.components.EmptyState
import com.shearless.bareplayer.core.ui.components.ExtraTrackAction
import com.shearless.bareplayer.core.ui.components.LoadingState
import com.shearless.bareplayer.core.ui.components.ReorderState
import com.shearless.bareplayer.core.ui.components.TrackMenuActions
import com.shearless.bareplayer.core.ui.components.rememberDeleteSong
import com.shearless.bareplayer.core.ui.components.TrackRow
import com.shearless.bareplayer.core.ui.components.formatDuration
import com.shearless.bareplayer.core.ui.components.reorderHandle
import com.shearless.bareplayer.core.ui.icons.BareIcons
import com.shearless.bareplayer.feature.playlists.ConfirmDeleteDialog
import com.shearless.bareplayer.feature.playlists.PlaylistNameDialog
import com.shearless.bareplayer.feature.playlists.R
import com.shearless.bareplayer.core.ui.R as UiR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaylistDetailScreen(
    playlistId: Long,
    onBack: () -> Unit,
    onAddToPlaylist: (List<Long>) -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onOpenArtist: (Long) -> Unit,
) {
    val deps = LocalAppDependencies.current
    val viewModel = viewModel(key = "playlist-$playlistId") {
        PlaylistDetailViewModel(playlistId, deps.playlistRepository, deps.playbackController)
    }
    val playlist by viewModel.playlist.collectAsStateWithLifecycle()
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val currentId by viewModel.currentTrackId.collectAsStateWithLifecycle()
    var renaming by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }

    val menu = rememberPlaylistTrackMenu(viewModel, onAddToPlaylist, onOpenAlbum, onOpenArtist)
    val removeLabel = stringResource(R.string.remove_from_playlist)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(BareIcons.Back), contentDescription = stringResource(UiR.string.back))
                    }
                },
                actions = {
                    if (playlist != null) {
                        IconButton(onClick = { renaming = true }) {
                            Icon(painterResource(BareIcons.Edit), contentDescription = stringResource(R.string.rename_playlist))
                        }
                        IconButton(onClick = { deleting = true }) {
                            Icon(painterResource(BareIcons.Delete), contentDescription = stringResource(R.string.delete_playlist_title))
                        }
                    }
                },
            )
        },
    ) { padding ->
        val current = playlist
        val list = entries
        if (current == null || list == null) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }

        val listState = rememberLazyListState()
        val rows = remember { mutableStateListOf<PlaylistEntry>() }
        val reorder = remember(listState) {
            ReorderState(listState, indexOffset = 1) { from, to -> rows.add(to, rows.removeAt(from)) }
        }
        LaunchedEffect(list) {
            if (reorder.draggingIndex == null) {
                rows.clear()
                rows.addAll(list)
            }
        }

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            item(key = "header", contentType = "header") {
                PlaylistHeader(
                    name = current.name,
                    trackCount = list.size,
                    durationMs = list.sumOf { it.track.durationMs },
                    onPlay = { viewModel.play() },
                    onShuffle = { viewModel.play(shuffle = true) },
                )
            }
            if (rows.isEmpty()) {
                item(key = "empty") {
                    Box(Modifier.height(240.dp)) { EmptyState(BareIcons.MusicNote, stringResource(R.string.playlist_empty)) }
                }
            }
            itemsIndexed(rows, key = { _, entry -> entry.entryId }, contentType = { _, _ -> "entry" }) { index, entry ->
                val dragging = reorder.draggingIndex == index
                Surface(
                    tonalElevation = if (dragging) 6.dp else 0.dp,
                    modifier = if (dragging) {
                        Modifier.zIndex(1f).graphicsLayer { translationY = reorder.dragOffset }
                    } else {
                        Modifier.animateItem()
                    },
                ) {
                    TrackRow(
                        track = entry.track,
                        isCurrent = entry.track.id == currentId,
                        onClick = { viewModel.play(index) },
                        menu = menu,
                        extraActions = listOf(ExtraTrackAction(removeLabel) { viewModel.remove(entry.entryId) }),
                        trailing = {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .reorderHandle(reorder, entry.entryId, index, rows.size, viewModel::move)
                                    .size(48.dp),
                            ) {
                                Icon(
                                    painterResource(BareIcons.DragHandle),
                                    contentDescription = stringResource(R.string.reorder),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                    )
                }
            }
        }
    }

    val current = playlist
    if (renaming && current != null) {
        PlaylistNameDialog(
            title = stringResource(R.string.rename_playlist),
            confirmLabel = stringResource(R.string.rename),
            initialName = current.name,
            onConfirm = { renaming = false; viewModel.rename(it) },
            onDismiss = { renaming = false },
        )
    }
    if (deleting && current != null) {
        ConfirmDeleteDialog(
            name = current.name,
            onConfirm = {
                deleting = false
                viewModel.delete(onDeleted = onBack)
            },
            onDismiss = { deleting = false },
        )
    }
}

@Composable
private fun rememberPlaylistTrackMenu(
    viewModel: PlaylistDetailViewModel,
    onAddToPlaylist: (List<Long>) -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onOpenArtist: (Long) -> Unit,
): TrackMenuActions {
    val addToPlaylist by rememberUpdatedState(onAddToPlaylist)
    val openAlbum by rememberUpdatedState(onOpenAlbum)
    val openArtist by rememberUpdatedState(onOpenArtist)
    val deleteSong = rememberDeleteSong()
    return remember(viewModel) {
        object : TrackMenuActions {
            override fun playNext(track: Track) = viewModel.playback.playNext(listOf(track))
            override fun addToQueue(track: Track) = viewModel.playback.addToQueue(listOf(track))
            override fun addToPlaylist(track: Track) = addToPlaylist(listOf(track.id))
            override fun goToAlbum(track: Track) = openAlbum(track.albumId)
            override fun goToArtist(track: Track) = openArtist(track.artistId)
            override fun delete(track: Track) = deleteSong(track)
        }
    }
}

@Composable
private fun PlaylistHeader(
    name: String,
    trackCount: Int,
    durationMs: Long,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        Text(name, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(
            text = pluralStringResource(UiR.plurals.song_count, trackCount, trackCount) + " · " + formatDuration(durationMs),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onPlay, enabled = trackCount > 0) {
                Icon(painterResource(BareIcons.Play), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(UiR.string.play))
            }
            FilledTonalButton(onClick = onShuffle, enabled = trackCount > 0) {
                Icon(painterResource(BareIcons.Shuffle), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(UiR.string.shuffle))
            }
        }
    }
}
