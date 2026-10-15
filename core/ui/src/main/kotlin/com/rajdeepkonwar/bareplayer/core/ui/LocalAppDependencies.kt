package com.rajdeepkonwar.bareplayer.core.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.rajdeepkonwar.bareplayer.core.domain.AppDependencies

val LocalAppDependencies = staticCompositionLocalOf<AppDependencies> {
    error("AppDependencies not provided")
}
