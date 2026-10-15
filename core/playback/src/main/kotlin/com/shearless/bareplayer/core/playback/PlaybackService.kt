package com.shearless.bareplayer.core.playback

import android.app.PendingIntent
import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.audiofx.LoudnessEnhancer
import android.os.Bundle
import android.os.SystemClock
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
import androidx.media3.exoplayer.analytics.AnalyticsListener
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
import com.shearless.bareplayer.core.domain.playback.QueueMath
import com.shearless.bareplayer.core.domain.playback.QueueRestorer
import com.shearless.bareplayer.core.domain.playback.RestoredQueue
import com.shearless.bareplayer.core.model.QueueSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Hosts the ExoPlayer and the media session. Media3 drives the media notification, lock screen,
 * Bluetooth/headset buttons and foreground-service lifecycle from the session. The queue is saved
 * to Room as it changes and restored (paused) when the service starts.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaLibraryService() {

    private var session: MediaLibrarySession? = null
    private lateinit var player: ExoPlayer
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private val audioManager by lazy { getSystemService(AudioManager::class.java) }
    private var blockExternalPlayUntilMs = 0L
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val dependencies: PlaybackDependencies
        get() = (application as PlaybackDependencies.Provider).playbackDependencies
    private val restorer by lazy { QueueRestorer(dependencies.queueRepository, dependencies.libraryRepository) }

    /** Saving is held off until the startup restore finishes, so it can't overwrite the saved queue. */
    private var restoreFinished = false
    private var restoreStarted = false
    private var pendingSave: Job? = null
    private var positionSaver: Job? = null

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
        player.addAnalyticsListener(audioSessionListener)

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this).build().apply { setSmallIcon(R.drawable.ic_notification) },
        )
        session = MediaLibrarySession.Builder(this, player, SessionCallback())
            .apply { sessionActivity()?.let(::setSessionActivity) }
            .build()
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
        if (audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any(AudioDeviceInfo::isCarBluetoothOutput)) {
            guardAgainstCarAutoPlay()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        saveNow()
        val p = session?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0 || p.playbackState == Player.STATE_ENDED) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        saveNow()
        mainScope.cancel()
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        loudnessEnhancer?.release()
        loudnessEnhancer = null
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

    private fun applyRestored(restored: RestoredQueue) {
        val snapshot = restored.snapshot
        player.setMediaItems(restored.tracks.map { it.toMediaItem() }, snapshot.currentIndex, snapshot.positionMs)
        player.repeatMode = snapshot.repeatMode.toPlayerRepeatMode()
        player.shuffleModeEnabled = snapshot.shuffleEnabled
        if (snapshot.shuffleEnabled && QueueMath.isValidOrder(snapshot.shuffleOrder, restored.tracks.size)) {
            player.setShuffleOrder(DefaultShuffleOrder(snapshot.shuffleOrder.toIntArray(), Random.nextLong()))
        }
        player.prepare()
    }

    /**
     * Restores a paused queue only after BarePlayer's own UI connects. External controllers such
     * as a car therefore cannot cold-start the app and immediately resume yesterday's queue.
     */
    private fun startRestoreIfNeeded() {
        if (restoreStarted) return
        restoreStarted = true
        mainScope.launch {
            try {
                val restored = runCatching { restorer.restore() }.getOrNull()
                // The user may have started something while the saved queue was loading.
                if (restored != null && player.mediaItemCount == 0) applyRestored(restored)
            } finally {
                restoreFinished = true
            }
        }
    }

    // region Queue persistence

    private fun scheduleSave() {
        if (!restoreFinished) return
        pendingSave?.cancel()
        pendingSave = mainScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            saveNow()
        }
    }

    private fun saveNow() {
        if (!restoreFinished || !::player.isInitialized) return
        pendingSave?.cancel()
        val snapshot: QueueSnapshot = player.toQueueSnapshot()
        // Application scope: the write must outlive the service when it is being destroyed.
        dependencies.applicationScope.launch { dependencies.queueRepository.save(snapshot) }
    }

    private fun updatePositionSaver(isPlaying: Boolean) {
        positionSaver?.cancel()
        if (!isPlaying) return
        positionSaver = mainScope.launch {
            while (isActive) {
                delay(POSITION_SAVE_INTERVAL_MS)
                saveNow()
            }
        }
    }

    // endregion

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
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.containsAny(
                    Player.EVENT_TIMELINE_CHANGED,
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                    Player.EVENT_REPEAT_MODE_CHANGED,
                    Player.EVENT_IS_PLAYING_CHANGED,
                    Player.EVENT_POSITION_DISCONTINUITY,
                )
            ) {
                scheduleSave()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) = updatePositionSaver(isPlaying)

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            if (shuffleModeEnabled && restoreFinished) applyCurrentFirstShuffle()
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

    private val audioSessionListener = object : AnalyticsListener {
        override fun onAudioSessionIdChanged(eventTime: AnalyticsListener.EventTime, audioSessionId: Int) {
            loudnessEnhancer?.release()
            loudnessEnhancer = runCatching {
                LoudnessEnhancer(audioSessionId).apply {
                    setTargetGain(LOUDNESS_TARGET_GAIN_MB)
                    enabled = true
                }
            }.getOrNull()
        }
    }

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            if (addedDevices.any(AudioDeviceInfo::isCarBluetoothOutput)) guardAgainstCarAutoPlay()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            if (removedDevices.any(AudioDeviceInfo::isCarBluetoothOutput) && player.playWhenReady) {
                player.pause()
            }
        }
    }

    private fun guardAgainstCarAutoPlay() {
        if (!player.playWhenReady) {
            blockExternalPlayUntilMs = SystemClock.elapsedRealtime() + EXTERNAL_PLAY_GUARD_MS
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
                startRestoreIfNeeded()
                builder.setAvailableSessionCommands(
                    MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
                        .add(SessionCommand(PlaybackCommands.SET_SHUFFLE_ORDER, Bundle.EMPTY))
                        .build(),
                )
            }
            return builder.build()
        }

        @Suppress("DEPRECATION")
        override fun onPlayerCommandRequest(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            playerCommand: Int,
        ): Int {
            val unsolicitedExternalPlay =
                playerCommand == Player.COMMAND_PLAY_PAUSE &&
                    controller.packageName != packageName &&
                    !player.playWhenReady &&
                    SystemClock.elapsedRealtime() < blockExternalPlayUntilMs
            return if (unsolicitedExternalPlay) SessionError.ERROR_NOT_SUPPORTED else SessionResult.RESULT_SUCCESS
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

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            isForPlayback: Boolean,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> =
            mainScope.future {
                val restored = restorer.restore() ?: throw UnsupportedOperationException("No queue to resume")
                val snapshot = restored.snapshot
                player.repeatMode = snapshot.repeatMode.toPlayerRepeatMode()
                MediaSession.MediaItemsWithStartPosition(
                    restored.tracks.map { it.toMediaItem() },
                    snapshot.currentIndex,
                    snapshot.positionMs,
                )
            }

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
        const val LOUDNESS_TARGET_GAIN_MB = 600
        const val EXTERNAL_PLAY_GUARD_MS = 5_000L
        const val SAVE_DEBOUNCE_MS = 750L
        const val POSITION_SAVE_INTERVAL_MS = 10_000L
    }
}

private fun AudioDeviceInfo.isCarBluetoothOutput(): Boolean =
    type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
        type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
        type == AudioDeviceInfo.TYPE_BLE_SPEAKER
