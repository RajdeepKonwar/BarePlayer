package com.rajdeepkonwar.bareplayer.core.domain.repository

import com.rajdeepkonwar.bareplayer.core.model.ThemeMode
import com.rajdeepkonwar.bareplayer.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDynamicColor(enabled: Boolean)
}
