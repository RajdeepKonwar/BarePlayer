package com.shearless.bareplayer.feature.playlists

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.shearless.bareplayer.feature.playlists.detail.PlaylistDetailScreen
import kotlinx.serialization.Serializable

@Serializable
data object PlaylistsRoute

@Serializable
data class PlaylistDetailRoute(val playlistId: Long)

fun NavGraphBuilder.playlistsGraph(
    navController: NavController,
    onAddToPlaylist: (List<Long>) -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onOpenArtist: (Long) -> Unit,
) {
    composable<PlaylistsRoute> {
        PlaylistsScreen(
            onOpenPlaylist = { id -> navController.navigate(PlaylistDetailRoute(id)) { launchSingleTop = true } },
        )
    }
    composable<PlaylistDetailRoute> { entry ->
        PlaylistDetailScreen(
            playlistId = entry.toRoute<PlaylistDetailRoute>().playlistId,
            onBack = navController::popBackStack,
            onAddToPlaylist = onAddToPlaylist,
            onOpenAlbum = onOpenAlbum,
            onOpenArtist = onOpenArtist,
        )
    }
}
