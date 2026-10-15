package com.rajdeepkonwar.bareplayer.feature.library.collection

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rajdeepkonwar.bareplayer.core.ui.LocalAppDependencies
import com.rajdeepkonwar.bareplayer.core.ui.components.ArtImage
import com.rajdeepkonwar.bareplayer.core.ui.components.LoadingState
import com.rajdeepkonwar.bareplayer.core.ui.components.TrackRow
import com.rajdeepkonwar.bareplayer.core.ui.components.albumLabel
import com.rajdeepkonwar.bareplayer.core.ui.components.artistLabel
import com.rajdeepkonwar.bareplayer.core.ui.components.formatDuration
import com.rajdeepkonwar.bareplayer.core.ui.icons.BareIcons
import com.rajdeepkonwar.bareplayer.feature.library.CollectionRoute
import com.rajdeepkonwar.bareplayer.feature.library.CollectionType
import com.rajdeepkonwar.bareplayer.feature.library.R
import com.rajdeepkonwar.bareplayer.feature.library.rememberTrackMenu
import com.rajdeepkonwar.bareplayer.feature.library.tabs.AlbumCard
import com.rajdeepkonwar.bareplayer.core.ui.R as UiR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CollectionScreen(
    route: CollectionRoute,
    onBack: () -> Unit,
    onOpenCollection: (CollectionRoute) -> Unit,
    onAddToPlaylist: (List<Long>) -> Unit,
) {
    val deps = LocalAppDependencies.current
    val viewModel = viewModel(key = route.toString()) {
        CollectionViewModel(route, deps.libraryRepository, deps.playbackController)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentId by viewModel.currentTrackId.collectAsStateWithLifecycle()
    val menu = rememberTrackMenu(viewModel.playback, onAddToPlaylist, onOpenCollection)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(BareIcons.Back), contentDescription = stringResource(UiR.string.back))
                    }
                },
            )
        },
    ) { padding ->
        val ui = state
        if (ui == null) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            item(key = "header", contentType = "header") {
                CollectionHeader(ui, onPlay = { viewModel.play() }, onShuffle = { viewModel.play(shuffle = true) })
            }
            if (ui.albums.isNotEmpty()) {
                item(key = "albums", contentType = "albums") {
                    Column {
                        SectionTitle(stringResource(R.string.section_albums))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(ui.albums, key = { it.id }) { album ->
                                AlbumCard(
                                    album = album,
                                    onClick = { onOpenCollection(CollectionRoute(CollectionType.ALBUM, id = album.id)) },
                                    modifier = Modifier.width(140.dp),
                                )
                            }
                        }
                        SectionTitle(stringResource(R.string.section_songs))
                    }
                }
            }
            val isAlbum = ui.type == CollectionType.ALBUM
            itemsIndexed(ui.tracks, key = { _, t -> t.id }, contentType = { _, _ -> "track" }) { index, track ->
                TrackRow(
                    track = track,
                    isCurrent = track.id == currentId,
                    onClick = { viewModel.play(index) },
                    menu = menu,
                    showArt = !isAlbum,
                    leadingNumber = if (isAlbum) track.trackNumber else null,
                    showAlbumLink = !isAlbum,
                    showArtistLink = ui.type != CollectionType.ARTIST,
                )
            }
        }
    }
}

@Composable
private fun CollectionHeader(ui: CollectionUiState, onPlay: () -> Unit, onShuffle: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        val (icon, shape) = when (ui.type) {
            CollectionType.ALBUM -> BareIcons.Album to RoundedCornerShape(16.dp)
            CollectionType.ARTIST -> BareIcons.Person to CircleShape
            CollectionType.GENRE -> BareIcons.Genre to RoundedCornerShape(16.dp)
            CollectionType.FOLDER -> BareIcons.Folder to RoundedCornerShape(16.dp)
        }
        val showArt = ui.type == CollectionType.ALBUM || ui.type == CollectionType.ARTIST
        ArtImage(
            artKey = if (showArt) ui.artKey else null,
            shape = shape,
            placeholderIcon = icon,
            placeholderSize = 64.dp,
            modifier = Modifier.size(if (ui.type == CollectionType.ALBUM) 220.dp else 160.dp),
        )
        Spacer(Modifier.height(16.dp))
        val title = when (ui.type) {
            CollectionType.ALBUM -> albumLabel(ui.title)
            CollectionType.ARTIST -> artistLabel(ui.title)
            else -> ui.title
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        val details = buildList {
            if (ui.type == CollectionType.ALBUM) add(artistLabel(ui.artist.orEmpty()))
            if (ui.type == CollectionType.FOLDER) ui.artist?.let(::add)
            if (ui.year > 0) add(ui.year.toString())
            if (ui.type == CollectionType.ARTIST) {
                add(pluralStringResource(UiR.plurals.album_count, ui.albumCount, ui.albumCount))
            }
            add(pluralStringResource(UiR.plurals.song_count, ui.tracks.size, ui.tracks.size))
            add(formatDuration(ui.totalDurationMs))
        }
        Text(
            text = details.joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onPlay, enabled = ui.tracks.isNotEmpty()) {
                Icon(painterResource(BareIcons.Play), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(UiR.string.play))
            }
            FilledTonalButton(onClick = onShuffle, enabled = ui.tracks.isNotEmpty()) {
                Icon(painterResource(BareIcons.Shuffle), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(UiR.string.shuffle))
            }
        }
    }
}

@Composable
internal fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}
