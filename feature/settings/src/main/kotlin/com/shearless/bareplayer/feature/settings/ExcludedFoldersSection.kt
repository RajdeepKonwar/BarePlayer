package com.shearless.bareplayer.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shearless.bareplayer.core.domain.library.FolderFilter
import com.shearless.bareplayer.core.ui.LocalAppDependencies
import com.shearless.bareplayer.core.ui.icons.BareIcons
import com.shearless.bareplayer.core.ui.R as UiR

@Composable
internal fun ExcludedFoldersSection(
    excluded: Set<String>,
    onSetExcluded: (path: String, excluded: Boolean) -> Unit,
) {
    var picking by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.padding(top = 8.dp)) {
        Text(
            text = stringResource(R.string.excluded_folders),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Text(
            text = stringResource(R.string.excluded_folders_summary),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        if (excluded.isEmpty()) {
            Text(
                text = stringResource(R.string.excluded_folders_none),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        excluded.sortedBy { it.lowercase() }.forEach { path ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 16.dp, end = 4.dp),
            ) {
                Icon(
                    painterResource(BareIcons.Folder),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = path.substringAfterLast('/').ifEmpty { path },
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = path,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = { onSetExcluded(path, false) }) {
                    Icon(
                        painterResource(BareIcons.Close),
                        contentDescription = stringResource(R.string.include_folder_again, path),
                    )
                }
            }
        }
        OutlinedButton(
            onClick = { picking = true },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) { Text(stringResource(R.string.exclude_a_folder)) }
    }

    if (picking) {
        FolderPickerDialog(
            excluded = excluded,
            onPick = { path ->
                picking = false
                onSetExcluded(path, true)
            },
            onDismiss = { picking = false },
        )
    }
}

/** Lists the folders currently in the library; already-excluded folders aren't indexed, so they never appear. */
@Composable
private fun FolderPickerDialog(excluded: Set<String>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val library = LocalAppDependencies.current.libraryRepository
    val folders by remember(library) { library.folders() }.collectAsStateWithLifecycle(initialValue = null)
    val choices = folders.orEmpty().filterNot { FolderFilter.isExcluded(it.path, excluded) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.exclude_a_folder)) },
        text = {
            if (folders != null && choices.isEmpty()) {
                Text(stringResource(R.string.no_folders_to_exclude))
            } else {
                LazyColumn(Modifier.heightIn(max = 400.dp)) {
                    items(choices, key = { it.path }) { folder ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .clickable { onPick(folder.path) }
                                .padding(vertical = 8.dp),
                        ) {
                            Text(folder.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = pluralStringResource(UiR.plurals.song_count, folder.trackCount, folder.trackCount) +
                                    " · " + folder.path,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
