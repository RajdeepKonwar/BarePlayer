package com.shearless.bareplayer.feature.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shearless.bareplayer.core.domain.playback.PlaybackController
import com.shearless.bareplayer.core.model.PlaybackState
import com.shearless.bareplayer.core.model.QueueItem
import com.shearless.bareplayer.core.ui.LocalAppDependencies
import com.shearless.bareplayer.core.ui.components.ArtImage
import com.shearless.bareplayer.core.ui.components.EmptyState
import com.shearless.bareplayer.core.ui.components.albumLabel
import com.shearless.bareplayer.core.ui.components.artistLabel
import com.shearless.bareplayer.core.ui.icons.BareIcons
import com.shearless.bareplayer.core.ui.R as UiR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NowPlayingScreen(
    onCollapse: () -> Unit,
    onOpenQueue: () -> Unit,
    onAddToPlaylist: (List<Long>) -> Unit,
) {
    val playback = LocalAppDependencies.current.playbackController
    val state by playback.state.collectAsStateWithLifecycle()
    val current = state.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.now_playing)) },
                navigationIcon = {
                    IconButton(onClick = onCollapse) {
                        Icon(painterResource(BareIcons.ExpandMore), contentDescription = stringResource(R.string.collapse))
                    }
                },
                actions = {
                    if (current != null) {
                        IconButton(onClick = { onAddToPlaylist(listOf(current.trackId)) }) {
                            Icon(
                                painterResource(BareIcons.PlaylistAdd),
                                contentDescription = stringResource(UiR.string.add_to_playlist),
                            )
                        }
                        IconButton(onClick = onOpenQueue) {
                            Icon(painterResource(BareIcons.Queue), contentDescription = stringResource(R.string.open_queue))
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (current == null) {
            EmptyState(BareIcons.MusicNote, stringResource(R.string.nothing_playing), Modifier.padding(padding))
            return@Scaffold
        }
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp)) {
            if (maxWidth > maxHeight) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Artwork(current, Modifier.fillMaxHeight(0.85f).aspectRatio(1f))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                        PlayerDetails(current, state, playback)
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Spacer(Modifier.weight(1f))
                    Artwork(current, Modifier.fillMaxWidth().widthIn(max = 420.dp).aspectRatio(1f))
                    Spacer(Modifier.weight(1f))
                    PlayerDetails(current, state, playback)
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun Artwork(item: QueueItem, modifier: Modifier) {
    ArtImage(
        artKey = item.artKey,
        shape = RoundedCornerShape(20.dp),
        placeholderSize = 96.dp,
        modifier = modifier,
    )
}

@Composable
private fun ColumnScope.PlayerDetails(item: QueueItem, state: PlaybackState, playback: PlaybackController) {
    Text(
        text = item.title,
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        text = "${artistLabel(item.artist)} · ${albumLabel(item.album)}",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(16.dp))
    SeekBar(position = playback.position, durationMs = state.durationMs, onSeek = playback::seekTo)
    Spacer(Modifier.height(8.dp))
    TransportControls(
        state = state,
        onShuffle = { playback.setShuffleEnabled(!state.shuffleEnabled) },
        onPrevious = playback::skipToPrevious,
        onPlayPause = playback::togglePlayPause,
        onNext = playback::skipToNext,
        onRepeat = playback::cycleRepeatMode,
        modifier = Modifier.align(Alignment.CenterHorizontally),
    )
}
