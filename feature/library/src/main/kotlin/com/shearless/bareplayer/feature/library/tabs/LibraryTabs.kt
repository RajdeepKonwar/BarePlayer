package com.shearless.bareplayer.feature.library.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shearless.bareplayer.core.domain.library.SortKeys
import com.shearless.bareplayer.core.model.Album
import com.shearless.bareplayer.core.model.Artist
import com.shearless.bareplayer.core.model.Folder
import com.shearless.bareplayer.core.model.Genre
import com.shearless.bareplayer.core.model.ScanState
import com.shearless.bareplayer.core.ui.components.ArtImage
import com.shearless.bareplayer.core.ui.components.EmptyState
import com.shearless.bareplayer.core.ui.components.FastScroller
import com.shearless.bareplayer.core.ui.components.LoadingState
import com.shearless.bareplayer.core.ui.components.TrackMenuActions
import com.shearless.bareplayer.core.ui.components.TrackRow
import com.shearless.bareplayer.core.ui.components.albumLabel
import com.shearless.bareplayer.core.ui.components.artistLabel
import com.shearless.bareplayer.core.ui.icons.BareIcons
import com.shearless.bareplayer.feature.library.LibraryViewModel
import com.shearless.bareplayer.feature.library.R
import com.shearless.bareplayer.core.ui.R as UiR

private val ListBottomPadding = PaddingValues(bottom = 16.dp)

/** Shows loading / empty states for a tab; [content] runs only for a non-empty list. */
@Composable
private fun <T> TabContent(
    items: List<T>?,
    scanState: ScanState,
    emptyIcon: Int,
    content: @Composable (List<T>) -> Unit,
) {
    when {
        items == null -> LoadingState()
        items.isEmpty() && scanState is ScanState.Scanning -> LoadingState()
        items.isEmpty() -> EmptyState(emptyIcon, stringResource(R.string.no_music))
        else -> content(items)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SongsTab(viewModel: LibraryViewModel, menu: TrackMenuActions) {
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val currentId by viewModel.currentTrackId.collectAsStateWithLifecycle()

    PullToRefreshBox(
        isRefreshing = scanState is ScanState.Scanning,
        onRefresh = viewModel::rescan,
        modifier = Modifier.fillMaxSize(),
    ) {
        TabContent(tracks, scanState, BareIcons.MusicNote) { list ->
            val listState = rememberLazyListState()
            Box(Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    contentPadding = ListBottomPadding,
                    modifier = Modifier.fillMaxSize().testTag("songs_list"),
                ) {
                    item(key = "header", contentType = "header") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = pluralStringResource(UiR.plurals.song_count, list.size, list.size),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            FilledTonalButton(onClick = viewModel::shuffleAll) {
                                Icon(painterResource(BareIcons.Shuffle), contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.shuffle_all))
                            }
                        }
                    }
                    itemsIndexed(list, key = { _, track -> track.id }, contentType = { _, _ -> "track" }) { index, track ->
                        TrackRow(
                            track = track,
                            isCurrent = track.id == currentId,
                            onClick = { viewModel.play(list, index) },
                            menu = menu,
                        )
                    }
                }
                FastScroller(
                    itemCount = list.size,
                    firstVisibleIndex = { (listState.firstVisibleItemIndex - 1).coerceAtLeast(0) },
                    isScrollInProgress = listState.isScrollInProgress,
                    scrollToIndex = { listState.scrollToItem(it + 1) },
                    sectionAt = { list.getOrNull(it)?.let { t -> SortKeys.sectionOf(t.title).toString() } },
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
        }
    }
}

@Composable
internal fun AlbumsTab(viewModel: LibraryViewModel, onClick: (Album) -> Unit) {
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    TabContent(albums, scanState, BareIcons.Album) { list ->
        val gridState = rememberLazyGridState()
        Box(Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(150.dp),
                contentPadding = PaddingValues(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(list, key = { it.id }, contentType = { "album" }) { album ->
                    AlbumCard(album, onClick = { onClick(album) })
                }
            }
            FastScroller(
                itemCount = list.size,
                firstVisibleIndex = { gridState.firstVisibleItemIndex },
                isScrollInProgress = gridState.isScrollInProgress,
                scrollToIndex = { gridState.scrollToItem(it) },
                sectionAt = { list.getOrNull(it)?.let { a -> SortKeys.sectionOf(a.title).toString() } },
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

@Composable
internal fun AlbumCard(album: Album, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        ArtImage(
            artKey = album.artKey,
            placeholderIcon = BareIcons.Album,
            placeholderSize = 48.dp,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = albumLabel(album.title),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = artistLabel(album.artist),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
internal fun ArtistsTab(viewModel: LibraryViewModel, onClick: (Artist) -> Unit) {
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    TabContent(artists, scanState, BareIcons.Person) { list ->
        val listState = rememberLazyListState()
        Box(Modifier.fillMaxSize()) {
            LazyColumn(state = listState, contentPadding = ListBottomPadding, modifier = Modifier.fillMaxSize()) {
                items(list, key = { it.id }, contentType = { "artist" }) { artist ->
                    ArtistRow(artist, onClick = { onClick(artist) })
                }
            }
            FastScroller(
                itemCount = list.size,
                firstVisibleIndex = { listState.firstVisibleItemIndex },
                isScrollInProgress = listState.isScrollInProgress,
                scrollToIndex = { listState.scrollToItem(it) },
                sectionAt = { list.getOrNull(it)?.let { a -> SortKeys.sectionOf(a.name).toString() } },
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

@Composable
internal fun ArtistRow(artist: Artist, onClick: () -> Unit) {
    SimpleRow(
        title = artistLabel(artist.name),
        subtitle = pluralStringResource(UiR.plurals.album_count, artist.albumCount, artist.albumCount) + " · " +
            pluralStringResource(UiR.plurals.song_count, artist.trackCount, artist.trackCount),
        onClick = onClick,
        leading = {
            ArtImage(
                artKey = artist.artKey,
                shape = CircleShape,
                placeholderIcon = BareIcons.Person,
                modifier = Modifier.size(48.dp),
            )
        },
    )
}

@Composable
internal fun GenresTab(viewModel: LibraryViewModel, onClick: (Genre) -> Unit) {
    val genres by viewModel.genres.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    TabContent(genres, scanState, BareIcons.Genre) { list ->
        LazyColumn(contentPadding = ListBottomPadding, modifier = Modifier.fillMaxSize()) {
            items(list, key = { it.name }, contentType = { "genre" }) { genre ->
                SimpleRow(
                    title = genre.name,
                    subtitle = pluralStringResource(UiR.plurals.song_count, genre.trackCount, genre.trackCount),
                    onClick = { onClick(genre) },
                    leading = { IconBadge(BareIcons.Genre) },
                )
            }
        }
    }
}

@Composable
internal fun FoldersTab(viewModel: LibraryViewModel, onClick: (Folder) -> Unit) {
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    var confirmExclude by rememberSaveable { mutableStateOf<String?>(null) }
    TabContent(folders, scanState, BareIcons.Folder) { list ->
        LazyColumn(contentPadding = ListBottomPadding, modifier = Modifier.fillMaxSize()) {
            items(list, key = { it.path }, contentType = { "folder" }) { folder ->
                SimpleRow(
                    title = folder.name,
                    subtitle = pluralStringResource(UiR.plurals.song_count, folder.trackCount, folder.trackCount) +
                        " · " + folder.path,
                    onClick = { onClick(folder) },
                    leading = { IconBadge(BareIcons.Folder) },
                    trailing = { FolderMenu(onExclude = { confirmExclude = folder.path }) },
                )
            }
        }
    }
    confirmExclude?.let { path ->
        AlertDialog(
            onDismissRequest = { confirmExclude = null },
            title = { Text(stringResource(R.string.exclude_folder_title)) },
            text = { Text(stringResource(R.string.exclude_folder_body, path)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmExclude = null
                    viewModel.excludeFolder(path)
                }) { Text(stringResource(R.string.exclude)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmExclude = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun FolderMenu(onExclude: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(painterResource(BareIcons.More), contentDescription = stringResource(UiR.string.more_options))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.exclude_folder)) },
                onClick = {
                    expanded = false
                    onExclude()
                },
            )
        }
    }
}

@Composable
private fun IconBadge(icon: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(48.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
internal fun SimpleRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = if (trailing != null) 4.dp else 16.dp),
    ) {
        leading()
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailing?.invoke()
    }
}
