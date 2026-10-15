package com.rajdeepkonwar.bareplayer.feature.playlists

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object PlaylistsRoute

fun NavGraphBuilder.playlistsScreen() {
    composable<PlaylistsRoute> {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Playlists") }
    }
}
