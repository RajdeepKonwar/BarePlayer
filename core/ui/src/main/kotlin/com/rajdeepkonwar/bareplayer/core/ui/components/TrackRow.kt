package com.rajdeepkonwar.bareplayer.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rajdeepkonwar.bareplayer.core.model.Track
import com.rajdeepkonwar.bareplayer.core.ui.R
import com.rajdeepkonwar.bareplayer.core.ui.icons.BareIcons

/** Actions offered from a track's overflow menu. Implementations should be stable (remembered). */
interface TrackMenuActions {
    fun playNext(track: Track)
    fun addToQueue(track: Track)
    fun addToPlaylist(track: Track)
    fun goToAlbum(track: Track)
    fun goToArtist(track: Track)
}

/** Extra, screen-specific overflow entry (e.g. "Remove from playlist"). */
data class ExtraTrackAction(val label: String, val onClick: (Track) -> Unit)

@Composable
fun TrackRow(
    track: Track,
    isCurrent: Boolean,
    onClick: () -> Unit,
    menu: TrackMenuActions?,
    modifier: Modifier = Modifier,
    showArt: Boolean = true,
    leadingNumber: Int? = null,
    showAlbumLink: Boolean = true,
    showArtistLink: Boolean = true,
    extraActions: List<ExtraTrackAction> = emptyList(),
    trailing: (@Composable () -> Unit)? = null,
) {
    val titleColor = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            leadingNumber != null -> Text(
                text = if (leadingNumber > 0) leadingNumber.toString() else "–",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(32.dp),
            )
            showArt -> {
                ArtImage(track.artKey, Modifier.size(48.dp))
                Spacer(Modifier.width(16.dp))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.SemiBold else null,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${artistLabel(track.artist)} · ${albumLabel(track.album)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailing?.invoke()
        if (menu != null || extraActions.isNotEmpty()) {
            TrackOverflowMenu(track, menu, showAlbumLink, showArtistLink, extraActions)
        }
    }
}

@Composable
private fun TrackOverflowMenu(
    track: Track,
    menu: TrackMenuActions?,
    showAlbumLink: Boolean,
    showArtistLink: Boolean,
    extraActions: List<ExtraTrackAction>,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(painterResource(BareIcons.More), contentDescription = stringResource(R.string.more_options))
        }
        if (!expanded) return@Box
        val dismiss = { expanded = false }
        DropdownMenu(expanded = true, onDismissRequest = dismiss) {
            if (menu != null) {
                MenuItem(stringResource(R.string.play_next), dismiss) { menu.playNext(track) }
                MenuItem(stringResource(R.string.add_to_queue), dismiss) { menu.addToQueue(track) }
                MenuItem(stringResource(R.string.add_to_playlist), dismiss) { menu.addToPlaylist(track) }
                if (showAlbumLink) MenuItem(stringResource(R.string.go_to_album), dismiss) { menu.goToAlbum(track) }
                if (showArtistLink) MenuItem(stringResource(R.string.go_to_artist), dismiss) { menu.goToArtist(track) }
            }
            extraActions.forEach { extra -> MenuItem(extra.label, dismiss) { extra.onClick(track) } }
        }
    }
}

@Composable
private fun MenuItem(label: String, dismiss: () -> Unit, action: () -> Unit) {
    DropdownMenuItem(text = { Text(label) }, onClick = { dismiss(); action() })
}
