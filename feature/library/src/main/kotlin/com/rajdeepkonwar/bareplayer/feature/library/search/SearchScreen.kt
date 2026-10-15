package com.rajdeepkonwar.bareplayer.feature.library.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rajdeepkonwar.bareplayer.core.ui.LocalAppDependencies
import com.rajdeepkonwar.bareplayer.core.ui.components.EmptyState
import com.rajdeepkonwar.bareplayer.core.ui.components.TrackRow
import com.rajdeepkonwar.bareplayer.core.ui.icons.BareIcons
import com.rajdeepkonwar.bareplayer.feature.library.CollectionRoute
import com.rajdeepkonwar.bareplayer.feature.library.CollectionType
import com.rajdeepkonwar.bareplayer.feature.library.R
import com.rajdeepkonwar.bareplayer.feature.library.collection.SectionTitle
import com.rajdeepkonwar.bareplayer.feature.library.rememberTrackMenu
import com.rajdeepkonwar.bareplayer.feature.library.tabs.AlbumCard
import com.rajdeepkonwar.bareplayer.feature.library.tabs.ArtistRow
import com.rajdeepkonwar.bareplayer.core.ui.R as UiR

@Composable
internal fun SearchScreen(
    onBack: () -> Unit,
    onOpenCollection: (CollectionRoute) -> Unit,
    onAddToPlaylist: (List<Long>) -> Unit,
) {
    val deps = LocalAppDependencies.current
    val viewModel = viewModel { SearchViewModel(deps.libraryRepository, deps.playbackController) }
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val currentId by viewModel.currentTrackId.collectAsStateWithLifecycle()
    val menu = rememberTrackMenu(viewModel.playback, onAddToPlaylist, onOpenCollection)
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(4.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(painterResource(BareIcons.Back), contentDescription = stringResource(UiR.string.back))
                }
                TextField(
                    value = query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onQueryChange("") }) {
                                Icon(painterResource(BareIcons.Close), contentDescription = stringResource(R.string.clear_search))
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                )
            }
        },
    ) { padding ->
        if (query.isNotBlank() && results.isEmpty && results.query.isNotEmpty()) {
            EmptyState(BareIcons.Search, stringResource(R.string.no_results), Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            if (results.artists.isNotEmpty()) {
                item(key = "artists-title") { SectionTitle(stringResource(R.string.section_artists)) }
                items(results.artists, key = { "artist-${it.id}" }) { artist ->
                    ArtistRow(artist) {
                        keyboard?.hide()
                        onOpenCollection(CollectionRoute(CollectionType.ARTIST, id = artist.id))
                    }
                }
            }
            if (results.albums.isNotEmpty()) {
                item(key = "albums") {
                    SectionTitle(stringResource(R.string.section_albums))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(results.albums, key = { it.id }) { album ->
                            AlbumCard(
                                album = album,
                                onClick = {
                                    keyboard?.hide()
                                    onOpenCollection(CollectionRoute(CollectionType.ALBUM, id = album.id))
                                },
                                modifier = Modifier.width(140.dp),
                            )
                        }
                    }
                }
            }
            if (results.tracks.isNotEmpty()) {
                item(key = "songs-title") { SectionTitle(stringResource(R.string.section_songs)) }
                itemsIndexed(results.tracks, key = { _, t -> "track-${t.id}" }) { index, track ->
                    TrackRow(
                        track = track,
                        isCurrent = track.id == currentId,
                        onClick = {
                            keyboard?.hide()
                            viewModel.playback.playTracks(results.tracks, index)
                        },
                        menu = menu,
                    )
                }
            }
        }
    }
}
