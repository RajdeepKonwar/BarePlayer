package com.shearless.bareplayer.feature.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shearless.bareplayer.core.model.PlaybackState
import com.shearless.bareplayer.core.model.RepeatMode
import com.shearless.bareplayer.core.ui.components.formatDuration
import com.shearless.bareplayer.core.ui.icons.BareIcons
import kotlinx.coroutines.flow.Flow

/** Seek slider; only this composable recomposes as the position ticks. */
@Composable
internal fun SeekBar(position: Flow<Long>, durationMs: Long, onSeek: (Long) -> Unit, modifier: Modifier = Modifier) {
    val positionMs by position.collectAsStateWithLifecycle(initialValue = 0L)
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val duration = durationMs.coerceAtLeast(1L)
    val fraction = dragFraction ?: (positionMs.toFloat() / duration).coerceIn(0f, 1f)
    Column(modifier) {
        Slider(
            value = fraction,
            onValueChange = { dragFraction = it },
            onValueChangeFinished = {
                dragFraction?.let { onSeek((it * duration).toLong()) }
                dragFraction = null
            },
            enabled = durationMs > 0,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val shown = dragFraction?.let { (it * duration).toLong() } ?: positionMs
            Text(formatDuration(shown), style = MaterialTheme.typography.labelMedium)
            Text(formatDuration(durationMs), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
internal fun TransportControls(
    state: PlaybackState,
    onShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val active = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onShuffle) {
            Icon(
                painter = painterResource(BareIcons.Shuffle),
                contentDescription = stringResource(if (state.shuffleEnabled) R.string.shuffle_on else R.string.shuffle_off),
                tint = if (state.shuffleEnabled) active else inactive,
            )
        }
        IconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
            Icon(
                painterResource(BareIcons.SkipPrevious),
                contentDescription = stringResource(R.string.previous),
                modifier = Modifier.size(36.dp),
            )
        }
        FilledIconButton(
            onClick = onPlayPause,
            modifier = Modifier.size(72.dp),
            colors = IconButtonDefaults.filledIconButtonColors(),
        ) {
            Icon(
                painter = painterResource(if (state.isPlaying) BareIcons.Pause else BareIcons.Play),
                contentDescription = stringResource(if (state.isPlaying) R.string.pause else R.string.play),
                modifier = Modifier.size(40.dp),
            )
        }
        IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
            Icon(
                painterResource(BareIcons.SkipNext),
                contentDescription = stringResource(R.string.next),
                modifier = Modifier.size(36.dp),
            )
        }
        IconButton(onClick = onRepeat) {
            Icon(
                painter = painterResource(if (state.repeatMode == RepeatMode.ONE) BareIcons.RepeatOne else BareIcons.Repeat),
                contentDescription = stringResource(
                    when (state.repeatMode) {
                        RepeatMode.OFF -> R.string.repeat_off
                        RepeatMode.ALL -> R.string.repeat_all
                        RepeatMode.ONE -> R.string.repeat_one
                    },
                ),
                tint = if (state.repeatMode == RepeatMode.OFF) inactive else active,
            )
        }
    }
}
