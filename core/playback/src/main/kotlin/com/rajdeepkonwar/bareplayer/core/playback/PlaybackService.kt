package com.rajdeepkonwar.bareplayer.core.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.rajdeepkonwar.bareplayer.core.domain.playback.QueueMath
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Hosts the ExoPlayer and the media session. Media3 drives the media notification, lock screen,
 * Bluetooth/headset buttons and foreground-service lifecycle from the session.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaLibraryService() {

    private var session: MediaLibrarySession? = null
    private lateinit var player: ExoPlayer
    private val dependencies: PlaybackDependencies
        get() = (application as PlaybackDependencies.Provider).playbackDependencies

    /** Tracks skipped in a row, so a queue of only-unplayable files can't loop forever. */
    private var consecutiveSkips = 0
    private var lastMediaId: String? = null

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            // Local files only: don't observe network type changes.
            .setBandwidthMeter(DefaultBandwidthMeter.Builder(this).setResetOnNetworkTypeChange(false).build())
            .build()
        player.addListener(playerListener)

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this).build().apply { setSmallIcon(R.drawable.ic_notification) },
        )
        session = MediaLibrarySession.Builder(this, player, SessionCallback())
            .apply { sessionActivity()?.let(::setSessionActivity) }
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = session?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0 || p.playbackState == Player.STATE_ENDED) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    private fun sessionActivity(): PendingIntent? {
        val launch = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        return PendingIntent.getActivity(
            this,
            0,
            launch,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun applyCurrentFirstShuffle() {
        val count = player.mediaItemCount
        if (count == 0) return
        val order = QueueMath.currentFirstShuffle(count, player.currentMediaItemIndex)
        player.setShuffleOrder(DefaultShuffleOrder(order.toIntArray(), Random.nextLong()))
    }

    private fun skipUnplayable(unsupported: Boolean) {
        val title = player.currentMediaItem?.mediaMetadata?.title?.toString()
        session?.broadcastCustomCommand(
            SessionCommand(PlaybackCommands.TRACK_SKIPPED, Bundle.EMPTY),
            Bundle(2).apply {
                putString(PlaybackCommands.KEY_TITLE, title)
                putBoolean(PlaybackCommands.KEY_UNSUPPORTED, unsupported)
            },
        )
        consecutiveSkips++
        if (consecutiveSkips < player.mediaItemCount && player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
            player.prepare()
        } else {
            consecutiveSkips = 0
            player.pause()
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            if (shuffleModeEnabled) applyCurrentFirstShuffle()
        }

        override fun onTracksChanged(tracks: Tracks) {
            val hasAudio = tracks.containsType(C.TRACK_TYPE_AUDIO)
            if (hasAudio && !tracks.isTypeSupported(C.TRACK_TYPE_AUDIO, /* allowExceedsCapabilities = */ true)) {
                // No decoder on this device (e.g. FLAC on Android 8.0): skip instead of playing silence.
                skipUnplayable(unsupported = true)
            } else if (hasAudio) {
                consecutiveSkips = 0
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            skipUnplayable(unsupported = error.errorCode == PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val finished = lastMediaId
            if (finished != null &&
                (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT)
            ) {
                finished.toLongOrNull()?.let { id ->
                    dependencies.applicationScope.launch { dependencies.playHistoryRepository.recordPlay(id) }
                }
            }
            lastMediaId = mediaItem?.mediaId
        }
    }

    private inner class SessionCallback : MediaLibrarySession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val builder = MediaSession.ConnectionResult.AcceptedResultBuilder(session, controller)
            // Only BarePlayer's own UI may rewrite the shuffle order.
            if (controller.packageName == packageName) {
                builder.setAvailableSessionCommands(
                    MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
                        .add(SessionCommand(PlaybackCommands.SET_SHUFFLE_ORDER, Bundle.EMPTY))
                        .build(),
                )
            }
            return builder.build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction != PlaybackCommands.SET_SHUFFLE_ORDER) {
                return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
            }
            val order = args.getIntArray(PlaybackCommands.KEY_ORDER)?.toList()
            if (order == null || !QueueMath.isValidOrder(order, player.mediaItemCount)) {
                return Futures.immediateFuture(SessionResult(SessionError.ERROR_BAD_VALUE))
            }
            player.setShuffleOrder(DefaultShuffleOrder(order.toIntArray(), Random.nextLong()))
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> =
            Futures.immediateFuture(mediaItems.mapTo(ArrayList(mediaItems.size)) { it.withPlayableUri() })

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val root = MediaItem.Builder()
                .setMediaId(ROOT_ID)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .build(),
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(root, params))
        }

        // Browsing (for Android Auto and similar) is not part of the MVP; the root is empty.
        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> =
            Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.of(), params))
    }

    private companion object {
        const val ROOT_ID = "root"
    }
}
