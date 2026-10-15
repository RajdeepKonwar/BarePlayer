package com.rajdeepkonwar.bareplayer.feature.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rajdeepkonwar.bareplayer.core.domain.playback.PlaybackError
import com.rajdeepkonwar.bareplayer.core.ui.LocalAppDependencies
import com.rajdeepkonwar.bareplayer.core.ui.components.ArtImage
import com.rajdeepkonwar.bareplayer.core.ui.components.artistLabel
import com.rajdeepkonwar.bareplayer.core.ui.icons.BareIcons

/** Persistent bar above the navigation bar; renders nothing when no track is loaded. */
@Composable
fun MiniPlayer(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val playback = LocalAppDependencies.current.playbackController
    val state by playback.state.collectAsStateWithLifecycle()
    val current = state.current ?: return
    val position = playback.position.collectAsStateWithLifecycle(initialValue = 0L)
    val duration = state.durationMs

    Surface(tonalElevation = 3.dp, modifier = modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Column {
            LinearProgressIndicator(
                // Read in the draw phase so position ticks don't recompose the bar.
                progress = { if (duration > 0) (position.value.toFloat() / duration).coerceIn(0f, 1f) else 0f },
                drawStopIndicator = {},
                gapSize = 0.dp,
                modifier = Modifier.fillMaxWidth().height(2.dp),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(64.dp).padding(start = 12.dp, end = 4.dp),
            ) {
                ArtImage(current.artKey, Modifier.size(44.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = current.title,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = artistLabel(current.artist),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = playback::togglePlayPause) {
                    Icon(
                        painter = painterResource(if (state.isPlaying) BareIcons.Pause else BareIcons.Play),
                        contentDescription = stringResource(if (state.isPlaying) R.string.pause else R.string.play),
                    )
                }
                IconButton(onClick = playback::skipToNext) {
                    Icon(painterResource(BareIcons.SkipNext), contentDescription = stringResource(R.string.next))
                }
            }
        }
    }
}

/** Surfaces playback errors (skipped or unplayable tracks) as snackbars. */
@Composable
fun PlaybackErrorsEffect(snackbarHostState: SnackbarHostState) {
    val playback = LocalAppDependencies.current.playbackController
    val resources = LocalContext.current.resources
    LaunchedEffect(playback) {
        playback.errors.collect { error ->
            val title = error.title?.takeIf { it.isNotBlank() } ?: resources.getString(R.string.this_track)
            val message = when (error) {
                is PlaybackError.UnsupportedFormat -> resources.getString(R.string.error_unsupported_format, title)
                is PlaybackError.CannotPlay -> resources.getString(R.string.error_cannot_play, title)
            }
            snackbarHostState.showSnackbar(message)
        }
    }
}
