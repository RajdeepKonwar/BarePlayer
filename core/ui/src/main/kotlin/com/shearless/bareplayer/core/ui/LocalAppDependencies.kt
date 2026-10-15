package com.shearless.bareplayer.core.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.shearless.bareplayer.core.domain.AppDependencies

val LocalAppDependencies = staticCompositionLocalOf<AppDependencies> {
    error("AppDependencies not provided")
}
