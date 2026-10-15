package com.rajdeepkonwar.bareplayer.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rajdeepkonwar.bareplayer.BuildConfig
import com.rajdeepkonwar.bareplayer.R
import com.rajdeepkonwar.bareplayer.core.ui.icons.BareIcons
import com.rajdeepkonwar.bareplayer.feature.library.CollectionRoute
import com.rajdeepkonwar.bareplayer.feature.library.CollectionType
import com.rajdeepkonwar.bareplayer.feature.library.LibraryRoute
import com.rajdeepkonwar.bareplayer.feature.library.libraryGraph
import com.rajdeepkonwar.bareplayer.feature.library.navigateToCollection
import com.rajdeepkonwar.bareplayer.feature.player.MiniPlayer
import com.rajdeepkonwar.bareplayer.feature.player.PlaybackErrorsEffect
import com.rajdeepkonwar.bareplayer.feature.player.isPlayerDestination
import com.rajdeepkonwar.bareplayer.feature.player.navigateToNowPlaying
import com.rajdeepkonwar.bareplayer.feature.player.playerGraph
import com.rajdeepkonwar.bareplayer.feature.playlists.AddToPlaylistDialog
import com.rajdeepkonwar.bareplayer.feature.playlists.PlaylistsRoute
import com.rajdeepkonwar.bareplayer.feature.playlists.playlistsGraph
import com.rajdeepkonwar.bareplayer.feature.settings.SettingsRoute
import com.rajdeepkonwar.bareplayer.feature.settings.settingsScreen
import kotlinx.coroutines.launch
import kotlin.reflect.KClass
import com.rajdeepkonwar.bareplayer.feature.playlists.R as PlaylistsR

private enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    @param:DrawableRes val icon: Int,
    @param:StringRes val label: Int,
) {
    Library(LibraryRoute, LibraryRoute::class, BareIcons.Library, R.string.nav_library),
    Playlists(PlaylistsRoute, PlaylistsRoute::class, BareIcons.PlaylistPlay, R.string.nav_playlists),
    Settings(SettingsRoute, SettingsRoute::class, BareIcons.Settings, R.string.nav_settings),
}

@Composable
fun BareApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val fullScreenPlayer = destination?.isPlayerDestination() == true
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalContext.current.resources
    var pendingAddToPlaylist by rememberSaveable { mutableStateOf<List<Long>?>(null) }
    val onAddToPlaylist: (List<Long>) -> Unit = { pendingAddToPlaylist = it }

    PlaybackErrorsEffect(snackbarHostState)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState, Modifier.navigationBarsPadding()) },
        bottomBar = {
            if (!fullScreenPlayer) {
                Column {
                    MiniPlayer(onOpen = navController::navigateToNowPlaying)
                    NavigationBar {
                        TopLevelDestination.entries.forEach { item ->
                            val selected = destination?.hierarchy?.any { it.hasRoute(item.routeClass) } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(painterResource(item.icon), contentDescription = null) },
                                label = { Text(stringResource(item.label)) },
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = LibraryRoute,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            libraryGraph(navController, onAddToPlaylist)
            playerGraph(navController, onAddToPlaylist)
            playlistsGraph(
                navController = navController,
                onAddToPlaylist = onAddToPlaylist,
                onOpenAlbum = { navController.navigateToCollection(CollectionRoute(CollectionType.ALBUM, id = it)) },
                onOpenArtist = { navController.navigateToCollection(CollectionRoute(CollectionType.ARTIST, id = it)) },
            )
            settingsScreen(appVersion = BuildConfig.VERSION_NAME)
        }
    }

    pendingAddToPlaylist?.let { trackIds ->
        AddToPlaylistDialog(
            trackIds = trackIds,
            onAdded = { result ->
                scope.launch {
                    snackbarHostState.showSnackbar(
                        resources.getString(PlaylistsR.string.added_to_playlist, result.playlistName),
                    )
                }
            },
            onDismiss = { pendingAddToPlaylist = null },
        )
    }
}
