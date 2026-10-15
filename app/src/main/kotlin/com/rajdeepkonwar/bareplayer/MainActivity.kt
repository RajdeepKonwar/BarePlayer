package com.rajdeepkonwar.bareplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.rajdeepkonwar.bareplayer.core.ui.theme.BareTheme
import com.rajdeepkonwar.bareplayer.ui.BareApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BareTheme {
                BareApp()
            }
        }
    }
}
