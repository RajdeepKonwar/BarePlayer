package com.rajdeepkonwar.bareplayer.feature.player

import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.rajdeepkonwar.bareplayer.feature.player.queue.QueueScreen
import kotlinx.serialization.Serializable

@Serializable
data object NowPlayingRoute

@Serializable
data object QueueRoute

/** Full-screen player destinations hide the bottom bar and mini-player. */
fun NavDestination.isPlayerDestination(): Boolean = hasRoute(NowPlayingRoute::class) || hasRoute(QueueRoute::class)

fun NavController.navigateToNowPlaying() = navigate(NowPlayingRoute) { launchSingleTop = true }

fun NavGraphBuilder.playerGraph(
    navController: NavController,
    onAddToPlaylist: (List<Long>) -> Unit,
) {
    composable<NowPlayingRoute>(
        enterTransition = { slideInVertically { it } },
        popExitTransition = { slideOutVertically { it } },
    ) {
        NowPlayingScreen(
            onCollapse = navController::popBackStack,
            onOpenQueue = { navController.navigate(QueueRoute) { launchSingleTop = true } },
            onAddToPlaylist = onAddToPlaylist,
        )
    }
    composable<QueueRoute> {
        QueueScreen(onBack = navController::popBackStack)
    }
}
