package com.rajdeepkonwar.bareplayer.feature.library

import androidx.annotation.Keep
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.rajdeepkonwar.bareplayer.feature.library.collection.CollectionScreen
import com.rajdeepkonwar.bareplayer.feature.library.search.SearchScreen
import kotlinx.serialization.Serializable

@Serializable
data object LibraryRoute

@Serializable
data object SearchRoute

@Keep
enum class CollectionType { ALBUM, ARTIST, GENRE, FOLDER }

/** A list of tracks grouped by album, artist, genre ([key] = name) or folder ([key] = path). */
@Serializable
data class CollectionRoute(val type: CollectionType, val id: Long = 0L, val key: String = "")

fun NavController.navigateToCollection(route: CollectionRoute) = navigate(route) { launchSingleTop = true }

fun NavGraphBuilder.libraryGraph(
    navController: NavController,
    onAddToPlaylist: (List<Long>) -> Unit,
) {
    composable<LibraryRoute> {
        LibraryScreen(
            onOpenCollection = navController::navigateToCollection,
            onSearch = { navController.navigate(SearchRoute) { launchSingleTop = true } },
            onAddToPlaylist = onAddToPlaylist,
        )
    }
    composable<SearchRoute> {
        SearchScreen(
            onBack = navController::popBackStack,
            onOpenCollection = navController::navigateToCollection,
            onAddToPlaylist = onAddToPlaylist,
        )
    }
    composable<CollectionRoute> { entry ->
        CollectionScreen(
            route = entry.toRoute(),
            onBack = navController::popBackStack,
            onOpenCollection = navController::navigateToCollection,
            onAddToPlaylist = onAddToPlaylist,
        )
    }
}
