package com.shearless.bareplayer.core.ui.components

import android.Manifest
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.shearless.bareplayer.core.model.Track
import com.shearless.bareplayer.core.ui.LocalAppDependencies
import com.shearless.bareplayer.core.ui.R
import kotlinx.coroutines.launch
import java.io.File

/**
 * Returns an action that confirms, then permanently deletes a song. Android 10 and newer show the
 * system approval dialog; Android 8–9 use the legacy storage permission.
 */
@Composable
fun rememberDeleteSong(): (Track) -> Unit {
    val context = LocalContext.current
    val dependencies = LocalAppDependencies.current
    val scope = rememberCoroutineScope()
    var candidate by remember { mutableStateOf<Track?>(null) }
    var awaitingSystem by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    fun deleted(trackId: Long) {
        dependencies.playbackController.removeTrack(trackId)
        scope.launch {
            dependencies.playlistRepository.forgetTrack(trackId)
            dependencies.libraryRepository.refresh(force = true)
        }
    }

    fun fail() {
        candidate = null
        awaitingSystem = false
        failed = true
    }

    val systemApproval = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val track = candidate
        candidate = null
        awaitingSystem = false
        if (result.resultCode == Activity.RESULT_OK && track != null) deleted(track.id)
    }

    fun request(track: Track) {
        try {
            when (val outcome = SongDeletion.request(context, track)) {
                SongDeletion.Outcome.Deleted -> {
                    candidate = null
                    awaitingSystem = false
                    deleted(track.id)
                }
                is SongDeletion.Outcome.NeedsApproval -> {
                    awaitingSystem = true
                    systemApproval.launch(IntentSenderRequest.Builder(outcome.intentSender).build())
                }
            }
        } catch (_: Exception) {
            fail()
        }
    }

    val legacyPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val track = candidate
        if (granted && track != null) {
            request(track)
        } else {
            candidate = null
            awaitingSystem = false
        }
    }

    val pending = candidate
    if (pending != null && !awaitingSystem) {
        DeleteSongDialog(
            title = pending.title,
            onConfirm = {
                awaitingSystem = true
                if (SongDeletion.needsLegacyWritePermission(context)) {
                    legacyPermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                } else {
                    request(pending)
                }
            },
            onDismiss = {
                candidate = null
                awaitingSystem = false
            },
        )
    }
    if (failed) {
        AlertDialog(
            onDismissRequest = { failed = false },
            title = { Text(stringResource(R.string.delete_song_failed_title)) },
            text = { Text(stringResource(R.string.delete_song_failed)) },
            confirmButton = { TextButton(onClick = { failed = false }) { Text(stringResource(R.string.ok)) } },
        )
    }
    return { candidate = it }
}

@Composable
private fun DeleteSongDialog(title: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_song_title)) },
        text = { Text(stringResource(R.string.delete_song_message, title)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete_song_confirm), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

internal object SongDeletion {
    sealed interface Outcome {
        data object Deleted : Outcome
        data class NeedsApproval(val intentSender: IntentSender) : Outcome
    }

    fun needsLegacyWritePermission(context: Context): Boolean =
        Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED

    fun request(context: Context, track: Track): Outcome {
        val uri = track.uri.toUri()
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Outcome.NeedsApproval(MediaStore.createDeleteRequest(resolver, listOf(uri)).intentSender)
        }
        try {
            if (resolver.delete(uri, null, null) > 0) return Outcome.Deleted
        } catch (security: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && security is RecoverableSecurityException) {
                return Outcome.NeedsApproval(security.userAction.actionIntent.intentSender)
            }
            throw security
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val file = File(track.folderPath, track.fileName)
            if (file.isFile && file.delete()) {
                runCatching { resolver.delete(uri, null, null) }
                return Outcome.Deleted
            }
        }
        throw SecurityException("Unable to delete ${track.uri}")
    }
}
