package com.shearless.bareplayer

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.shearless.bareplayer.core.model.UserPreferences
import com.shearless.bareplayer.core.ui.LocalAppDependencies
import com.shearless.bareplayer.core.ui.theme.BareTheme
import com.shearless.bareplayer.core.ui.theme.isDarkTheme
import com.shearless.bareplayer.ui.AudioPermissionGate
import com.shearless.bareplayer.ui.BareApp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MainActivity : ComponentActivity() {

    private val container: AppContainer get() = (application as BareApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Hold the splash until preferences load so the first frame already uses the right theme.
        val preferences: StateFlow<UserPreferences?> = container.preferencesRepository.preferences
            .stateIn(lifecycleScope, SharingStarted.Eagerly, null)
        splash.setKeepOnScreenCondition { preferences.value == null }

        enableEdgeToEdge()
        setContent {
            val prefs by preferences.collectAsStateWithLifecycle()
            val current = prefs ?: return@setContent
            val dark = isDarkTheme(current.themeMode)
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark },
                )
                onDispose {}
            }
            CompositionLocalProvider(LocalAppDependencies provides container) {
                BareTheme(themeMode = current.themeMode, dynamicColor = current.dynamicColor) {
                    AudioPermissionGate(onGranted = { container.librarySync.start(applicationContext) }) {
                        BareApp()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        container.playerConnection.connect()
    }

    override fun onStop() {
        // Unbinding lets the service stop itself once playback is paused and the UI is gone.
        container.playerConnection.release()
        super.onStop()
    }

    private companion object {
        val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
