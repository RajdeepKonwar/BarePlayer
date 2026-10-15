package com.rajdeepkonwar.bareplayer.feature.playlists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rajdeepkonwar.bareplayer.core.domain.playlist.AddToPlaylistResult
import com.rajdeepkonwar.bareplayer.core.domain.playlist.AddToPlaylistUseCase
import com.rajdeepkonwar.bareplayer.core.domain.playlist.PlaylistNames
import com.rajdeepkonwar.bareplayer.core.domain.playlist.PlaylistTarget
import com.rajdeepkonwar.bareplayer.core.ui.LocalAppDependencies
import com.rajdeepkonwar.bareplayer.core.ui.icons.BareIcons
import kotlinx.coroutines.launch
import com.rajdeepkonwar.bareplayer.core.ui.R as UiR

/** Text-input dialog for creating or renaming a playlist. */
@Composable
fun PlaylistNameDialog(
    title: String,
    confirmLabel: String,
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember {
        mutableStateOf(TextFieldValue(initialName, selection = TextRange(0, initialName.length)))
    }
    val normalized = PlaylistNames.normalize(value.text)
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { if (it.text.length <= PlaylistNames.MAX_LENGTH) value = it },
                label = { Text(stringResource(R.string.playlist_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { normalized?.let(onConfirm) }),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            )
        },
        confirmButton = {
            TextButton(onClick = { normalized?.let(onConfirm) }, enabled = normalized != null) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/**
 * Lets the user pick a playlist (or create one) for [trackIds]. [onAdded] runs after the tracks
 * were saved, before the dialog closes.
 */
@Composable
fun AddToPlaylistDialog(
    trackIds: List<Long>,
    onAdded: (AddToPlaylistResult) -> Unit,
    onDismiss: () -> Unit,
) {
    val repository = LocalAppDependencies.current.playlistRepository
    val addToPlaylist = remember(repository) { AddToPlaylistUseCase(repository) }
    val playlists by remember(repository) { repository.playlists() }.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    var creating by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    fun add(target: PlaylistTarget) {
        if (busy) return
        busy = true
        scope.launch {
            val result = addToPlaylist(target, trackIds)
            onAdded(result)
            onDismiss()
        }
    }

    if (creating) {
        val base = stringResource(R.string.default_playlist_name)
        PlaylistNameDialog(
            title = stringResource(R.string.new_playlist),
            confirmLabel = stringResource(R.string.create),
            initialName = PlaylistNames.suggest(base, playlists.orEmpty().map { it.name }),
            onConfirm = { add(PlaylistTarget.New(it)) },
            onDismiss = { creating = false },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(UiR.string.add_to_playlist)) },
        text = {
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                item(key = "new") {
                    DialogRow(
                        icon = BareIcons.Add,
                        title = stringResource(R.string.new_playlist),
                        subtitle = null,
                        onClick = { creating = true },
                    )
                }
                items(playlists.orEmpty(), key = { it.id }) { playlist ->
                    DialogRow(
                        icon = BareIcons.PlaylistPlay,
                        title = playlist.name,
                        subtitle = pluralStringResource(UiR.plurals.song_count, playlist.trackCount, playlist.trackCount),
                        onClick = { add(PlaylistTarget.Existing(playlist.id, playlist.name)) },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun DialogRow(icon: Int, title: String, subtitle: String?, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ConfirmDeleteDialog(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_playlist_title)) },
        text = { Text(stringResource(R.string.delete_playlist_body, name)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
