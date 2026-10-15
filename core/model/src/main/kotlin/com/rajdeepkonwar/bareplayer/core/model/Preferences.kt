package com.rajdeepkonwar.bareplayer.core.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    /** Absolute folder paths hidden from the library, including their subfolders. */
    val excludedFolders: Set<String> = emptySet(),
)
