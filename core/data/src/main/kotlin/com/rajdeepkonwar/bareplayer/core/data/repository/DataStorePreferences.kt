package com.rajdeepkonwar.bareplayer.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rajdeepkonwar.bareplayer.core.data.scan.ScanTokenStore
import com.rajdeepkonwar.bareplayer.core.domain.repository.PreferencesRepository
import com.rajdeepkonwar.bareplayer.core.model.ThemeMode
import com.rajdeepkonwar.bareplayer.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStorePreferences(context: Context) : PreferencesRepository, ScanTokenStore {
    private val store = context.applicationContext.settingsStore

    override val preferences: Flow<UserPreferences> = store.data
        .map { prefs ->
            UserPreferences(
                themeMode = prefs[THEME_MODE]?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
                    ?: ThemeMode.SYSTEM,
                dynamicColor = prefs[DYNAMIC_COLOR] ?: true,
            )
        }
        .distinctUntilChanged()

    override suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[THEME_MODE] = mode.name }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        store.edit { it[DYNAMIC_COLOR] = enabled }
    }

    override suspend fun read(): String? = store.data.first()[SCAN_TOKEN]

    override suspend fun write(token: String?) {
        store.edit { prefs -> if (token == null) prefs.remove(SCAN_TOKEN) else prefs[SCAN_TOKEN] = token }
    }

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val SCAN_TOKEN = stringPreferencesKey("media_store_token")
    }
}
