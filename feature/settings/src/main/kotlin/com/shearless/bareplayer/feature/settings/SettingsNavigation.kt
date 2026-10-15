package com.shearless.bareplayer.feature.settings

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object SettingsRoute

fun NavGraphBuilder.settingsScreen(appVersion: String) {
    composable<SettingsRoute> { SettingsScreen(appVersion) }
}
