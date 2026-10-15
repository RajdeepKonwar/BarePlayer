package com.rajdeepkonwar.bareplayer.core.domain.library

import com.rajdeepkonwar.bareplayer.core.domain.repository.LibraryRepository
import com.rajdeepkonwar.bareplayer.core.domain.repository.PreferencesRepository
import com.rajdeepkonwar.bareplayer.core.model.Track

/** Folder exclusion rules. Excluding a folder also excludes everything below it. */
object FolderFilter {
    fun normalize(path: String): String = path.trim().trimEnd('/')

    fun isExcluded(folderPath: String, excluded: Collection<String>): Boolean {
        if (excluded.isEmpty()) return false
        val path = normalize(folderPath)
        return excluded.any { rule ->
            val root = normalize(rule)
            // Shared storage on Android is case-insensitive.
            root.isNotEmpty() && (path.equals(root, ignoreCase = true) || path.startsWith("$root/", ignoreCase = true))
        }
    }

    fun apply(tracks: List<Track>, excluded: Collection<String>): List<Track> =
        if (excluded.isEmpty()) tracks else tracks.filterNot { isExcluded(it.folderPath, excluded) }

    /** Stable fingerprint of a set of rules, so a changed rule set invalidates cached scans. */
    fun fingerprint(excluded: Collection<String>): String =
        excluded.map { normalize(it).lowercase() }.filter { it.isNotEmpty() }.sorted()
            .joinToString("\n").hashCode().toUInt().toString(16)
}

/** Updates an exclusion rule and rescans so every view reflects it immediately. */
class SetFolderExcludedUseCase(
    private val preferences: PreferencesRepository,
    private val library: LibraryRepository,
) {
    suspend operator fun invoke(path: String, excluded: Boolean) {
        val normalized = FolderFilter.normalize(path)
        if (normalized.isEmpty()) return
        preferences.setFolderExcluded(normalized, excluded)
        library.refresh(force = true)
    }
}
