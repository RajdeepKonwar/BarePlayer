package com.rajdeepkonwar.bareplayer.feature.library

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rajdeepkonwar.bareplayer.core.model.ScanState
import com.rajdeepkonwar.bareplayer.core.ui.LocalAppDependencies
import com.rajdeepkonwar.bareplayer.core.ui.icons.BareIcons
import com.rajdeepkonwar.bareplayer.feature.library.tabs.AlbumsTab
import com.rajdeepkonwar.bareplayer.feature.library.tabs.ArtistsTab
import com.rajdeepkonwar.bareplayer.feature.library.tabs.FoldersTab
import com.rajdeepkonwar.bareplayer.feature.library.tabs.GenresTab
import com.rajdeepkonwar.bareplayer.feature.library.tabs.SongsTab
import kotlinx.coroutines.launch

private enum class LibraryTab(@param:StringRes val title: Int) {
    Songs(R.string.tab_songs),
    Albums(R.string.tab_albums),
    Artists(R.string.tab_artists),
    Genres(R.string.tab_genres),
    Folders(R.string.tab_folders),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibraryScreen(
    onOpenCollection: (CollectionRoute) -> Unit,
    onSearch: () -> Unit,
    onAddToPlaylist: (List<Long>) -> Unit,
) {
    val deps = LocalAppDependencies.current
    val viewModel = viewModel {
        LibraryViewModel(deps.libraryRepository, deps.playbackController, deps.preferencesRepository)
    }
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val menu = rememberTrackMenu(viewModel.playback, onAddToPlaylist, onOpenCollection)
    val pagerState = rememberPagerState { LibraryTab.entries.size }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.library_title)) },
                actions = {
                    IconButton(onClick = onSearch) {
                        Icon(painterResource(BareIcons.Search), contentDescription = stringResource(R.string.search))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            PrimaryScrollableTabRow(selectedTabIndex = pagerState.currentPage, edgePadding = 8.dp) {
                LibraryTab.entries.forEachIndexed { index, tab ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(stringResource(tab.title)) },
                    )
                }
            }
            Box(Modifier.fillMaxWidth()) {
                if (scanState is ScanState.Scanning) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize(), key = { it }) { page ->
                when (LibraryTab.entries[page]) {
                    LibraryTab.Songs -> SongsTab(viewModel, menu)
                    LibraryTab.Albums -> AlbumsTab(viewModel) { onOpenCollection(CollectionRoute(CollectionType.ALBUM, id = it.id)) }
                    LibraryTab.Artists -> ArtistsTab(viewModel) { onOpenCollection(CollectionRoute(CollectionType.ARTIST, id = it.id)) }
                    LibraryTab.Genres -> GenresTab(viewModel) { onOpenCollection(CollectionRoute(CollectionType.GENRE, key = it.name)) }
                    LibraryTab.Folders -> FoldersTab(viewModel) { onOpenCollection(CollectionRoute(CollectionType.FOLDER, key = it.path)) }
                }
            }
        }
    }
}