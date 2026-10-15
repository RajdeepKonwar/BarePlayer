package com.rajdeepkonwar.bareplayer.feature.player

import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object NowPlayingRoute

/** Full-screen player destinations hide the bottom bar and mini-player. */
fun NavDestination.isPlayerDestination(): Boolean = hasRoute(NowPlayingRoute::class)

fun NavController.navigateToNowPlaying() = navigate(NowPlayingRoute) { launchSingleTop = true }

fun NavGraphBuilder.playerGraph(
    navController: NavController,
    onAddToPlaylist: (List<Long>) -> Unit,
) {
    composable<NowPlayingRoute>(
        enterTransition = { slideInVertically { it } },
        exitTransition = { slideOutVertically { it } },
        popExitTransition = { slideOutVertically { it } },
    ) {
        NowPlayingScreen(
            onCollapse = navController::popBackStack,
            onAddToPlaylist = onAddToPlaylist,
        )
    }
}
