package com.shearless.bareplayer.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shearless.bareplayer.core.domain.library.SetFolderExcludedUseCase
import com.shearless.bareplayer.core.model.ScanState
import com.shearless.bareplayer.core.model.ThemeMode
import com.shearless.bareplayer.core.model.UserPreferences
import com.shearless.bareplayer.core.ui.LocalAppDependencies
import com.shearless.bareplayer.core.ui.theme.supportsDynamicColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(appVersion: String) {
    val deps = LocalAppDependencies.current
    val prefs by deps.preferencesRepository.preferences.collectAsStateWithLifecycle(initialValue = UserPreferences())
    val scanState by deps.libraryRepository.scanState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val setFolderExcluded = remember(deps) {
        SetFolderExcludedUseCase(deps.preferencesRepository, deps.libraryRepository)
    }

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader(stringResource(R.string.section_appearance))
            Column(Modifier.selectableGroup()) {
                ThemeMode.entries.forEach { mode ->
                    val label = stringResource(
                        when (mode) {
                            ThemeMode.SYSTEM -> R.string.theme_system
                            ThemeMode.LIGHT -> R.string.theme_light
                            ThemeMode.DARK -> R.string.theme_dark
                        },
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .selectable(
                                selected = prefs.themeMode == mode,
                                role = Role.RadioButton,
                                onClick = { scope.launch { deps.preferencesRepository.setThemeMode(mode) } },
                            )
                            .padding(horizontal = 16.dp),
                    ) {
                        RadioButton(selected = prefs.themeMode == mode, onClick = null)
                        Spacer(Modifier.width(16.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            if (supportsDynamicColor) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 72.dp)
                        .toggleable(
                            value = prefs.dynamicColor,
                            role = Role.Switch,
                            onValueChange = { enabled -> scope.launch { deps.preferencesRepository.setDynamicColor(enabled) } },
                        )
                        .padding(horizontal = 16.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.dynamic_color), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(R.string.dynamic_color_summary),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = prefs.dynamicColor, onCheckedChange = null)
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionHeader(stringResource(R.string.section_library))
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = when (val s = scanState) {
                        ScanState.Idle -> stringResource(R.string.scan_idle)
                        ScanState.Scanning -> stringResource(R.string.scan_running)
                        is ScanState.Done -> pluralStringResource(R.plurals.scan_done, s.trackCount, s.trackCount)
                        is ScanState.Failed -> stringResource(R.string.scan_failed, s.message)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = { scope.launch { deps.libraryRepository.refresh(force = true) } },
                    enabled = scanState !is ScanState.Scanning,
                    modifier = Modifier.padding(vertical = 8.dp),
                ) { Text(stringResource(R.string.rescan_library)) }
            }
            ExcludedFoldersSection(
                excluded = prefs.excludedFolders,
                onSetExcluded = { path, excluded -> scope.launch { setFolderExcluded(path, excluded) } },
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SupportSection(deps.tipJar, snackbarHostState)
            SectionHeader(stringResource(R.string.section_about))
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text(stringResource(R.string.privacy_statement), style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = stringResource(R.string.developer_credit),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    text = stringResource(R.string.version, appVersion),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
internal fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}
