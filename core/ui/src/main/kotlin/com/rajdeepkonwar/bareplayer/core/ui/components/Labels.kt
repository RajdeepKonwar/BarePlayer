package com.rajdeepkonwar.bareplayer.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rajdeepkonwar.bareplayer.core.ui.R

@Composable
fun artistLabel(name: String): String = name.ifEmpty { stringResource(R.string.unknown_artist) }

@Composable
fun albumLabel(name: String): String = name.ifEmpty { stringResource(R.string.unknown_album) }

/** Formats milliseconds as m:ss, or h:mm:ss for an hour or more. */
fun formatDuration(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0) / 1000)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
