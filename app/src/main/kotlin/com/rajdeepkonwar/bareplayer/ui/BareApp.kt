package com.rajdeepkonwar.bareplayer.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rajdeepkonwar.bareplayer.R
import com.rajdeepkonwar.bareplayer.core.ui.icons.BareIcons
import com.rajdeepkonwar.bareplayer.feature.library.LibraryRoute
import com.rajdeepkonwar.bareplayer.feature.library.libraryScreen
import com.rajdeepkonwar.bareplayer.feature.playlists.PlaylistsRoute
import com.rajdeepkonwar.bareplayer.feature.playlists.playlistsScreen
import com.rajdeepkonwar.bareplayer.feature.settings.SettingsRoute
import com.rajdeepkonwar.bareplayer.feature.settings.settingsScreen
import kotlin.reflect.KClass

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

    Scaffold(
        bottomBar = {
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
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = LibraryRoute,
            modifier = Modifier.padding(padding),
        ) {
            libraryScreen()
            playlistsScreen()
            settingsScreen()
        }
    }
}
