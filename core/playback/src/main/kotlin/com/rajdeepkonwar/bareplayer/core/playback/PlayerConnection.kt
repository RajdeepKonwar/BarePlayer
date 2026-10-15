package com.rajdeepkonwar.bareplayer.core.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.rajdeepkonwar.bareplayer.core.domain.playback.PlaybackController
import com.rajdeepkonwar.bareplayer.core.domain.playback.PlaybackError
import com.rajdeepkonwar.bareplayer.core.domain.playback.QueueMath
import com.rajdeepkonwar.bareplayer.core.model.PlaybackState
import com.rajdeepkonwar.bareplayer.core.model.QueueItem
import com.rajdeepkonwar.bareplayer.core.model.RepeatMode
import com.rajdeepkonwar.bareplayer.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * [PlaybackController] backed by a Media3 [MediaController] bound to [PlaybackService]. All
 * controller access happens on the main thread. Call [connect] when the UI starts and [release]
 * when it stops; commands issued while disconnected are queued until the controller is ready.
 */
class PlayerConnection(context: Context) : PlaybackController {
    private val appContext = context.applicationContext
    private val mainExecutor = ContextCompat.getMainExecutor(appContext)

    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val _errors = MutableSharedFlow<PlaybackError>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val errors: Flow<PlaybackError> = _errors.asSharedFlow()

    override val position: Flow<Long> = flow {
        while (true) {
            val c = controller
            emit(c?.currentPosition ?: 0L)
            delay(if (c?.isPlaying == true) 250L else 1_000L)
        }
    }.distinctUntilChanged().flowOn(Dispatchers.Main.immediate)

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private val pending = ArrayList<(MediaController) -> Unit>()

    /** Queue items by playlist index, rebuilt only when the timeline changes. */
    private var playlistItems: List<QueueItem> = emptyList()

    /** Playlist indices in play order. */
    private var playOrder: List<Int> = emptyList()

    fun connect() {
        if (controllerFuture != null) return
        val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val future = MediaController.Builder(appContext, token)
            .setListener(controllerListener)
            .buildAsync()
        controllerFuture = future
        future.addListener({
            if (controllerFuture !== future) return@addListener
            val c = runCatching { future.get() }.getOrNull() ?: run {
                controllerFuture = null
                return@addListener
            }
            controller = c
            c.addListener(playerListener)
            refresh(c, timelineChanged = true)
            pending.forEach { it(c) }
            pending.clear()
        }, mainExecutor)
    }

    fun release() {
        controllerFuture?.let(MediaController::releaseFuture)
        controllerFuture = null
        controller = null
        pending.clear()
        _state.value = _state.value.copy(isConnected = false, isPlaying = false)
    }

    private fun withController(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null) {
            block(c)
        } else {
            pending += block
            connect()
        }
    }

    override fun playTracks(tracks: List<Track>, startIndex: Int, shuffle: Boolean) {
        if (tracks.isEmpty()) return
        val start = startIndex.coerceIn(0, tracks.lastIndex)
        withController { c ->
            c.setMediaItems(tracks.map { it.toMediaItem() }, start, C.TIME_UNSET)
            c.shuffleModeEnabled = shuffle
            if (shuffle) sendShuffleOrder(c, QueueMath.currentFirstShuffle(tracks.size, start))
            c.prepare()
            c.play()
        }
    }

    override fun playNext(tracks: List<Track>) = insert(tracks, next = true)

    override fun addToQueue(tracks: List<Track>) = insert(tracks, next = false)

    private fun insert(tracks: List<Track>, next: Boolean) {
        if (tracks.isEmpty()) return
        withController { c ->
            if (c.mediaItemCount == 0) {
                playTracks(tracks, 0, shuffle = false)
                return@withController
            }
            val oldOrder = computePlayOrder(c)
            val insertAt = if (next) c.currentMediaItemIndex + 1 else c.mediaItemCount
            val playPosition = if (next) oldOrder.indexOf(c.currentMediaItemIndex) + 1 else oldOrder.size
            c.addMediaItems(insertAt, tracks.map { it.toMediaItem() })
            if (c.shuffleModeEnabled) {
                sendShuffleOrder(c, QueueMath.insert(oldOrder, insertAt, tracks.size, playPosition))
            }
        }
    }

    override fun togglePlayPause() = withController { c ->
        when {
            c.isPlaying -> c.pause()
            c.playbackState == Player.STATE_IDLE -> { c.prepare(); c.play() }
            c.playbackState == Player.STATE_ENDED -> { c.seekToDefaultPosition(); c.play() }
            else -> c.play()
        }
    }

    override fun skipToNext() = withController { it.seekToNext() }

    override fun skipToPrevious() = withController { it.seekToPrevious() }

    override fun seekTo(positionMs: Long) = withController { it.seekTo(positionMs.coerceAtLeast(0)) }

    override fun setShuffleEnabled(enabled: Boolean) = withController { it.shuffleModeEnabled = enabled }

    override fun cycleRepeatMode() = withController { c ->
        c.repeatMode = QueueMath.nextRepeatMode(c.repeatMode.toRepeatMode()).toPlayerRepeatMode()
    }

    override fun playQueuePosition(position: Int) = withController { c ->
        val index = playOrder.getOrNull(position) ?: return@withController
        c.seekToDefaultPosition(index)
        if (c.playbackState == Player.STATE_IDLE) c.prepare()
        c.play()
    }

    override fun moveQueueItem(from: Int, to: Int) = withController { c ->
        if (c.shuffleModeEnabled) {
            sendShuffleOrder(c, QueueMath.move(computePlayOrder(c), from, to))
        } else if (from in 0 until c.mediaItemCount && to in 0 until c.mediaItemCount) {
            c.moveMediaItem(from, to)
        }
    }

    override fun removeQueueItem(position: Int) = withController { c ->
        // ExoPlayer keeps the relative shuffle order of the remaining items on removal.
        playOrder.getOrNull(position)?.let(c::removeMediaItem)
    }

    private fun sendShuffleOrder(c: MediaController, order: List<Int>) {
        val command = SessionCommand(PlaybackCommands.SET_SHUFFLE_ORDER, Bundle.EMPTY)
        if (!c.isSessionCommandAvailable(command)) return
        c.sendCustomCommand(command, Bundle(1).apply { putIntArray(PlaybackCommands.KEY_ORDER, order.toIntArray()) })
    }

    private fun computePlayOrder(c: Player): List<Int> {
        val count = c.mediaItemCount
        if (!c.shuffleModeEnabled) return List(count) { it }
        val timeline = c.currentTimeline
        if (timeline.isEmpty) return emptyList()
        val order = ArrayList<Int>(count)
        var index = timeline.getFirstWindowIndex(true)
        while (index != C.INDEX_UNSET && order.size < count) {
            order += index
            index = timeline.getNextWindowIndex(index, Player.REPEAT_MODE_OFF, true)
        }
        return if (order.size == count) order else List(count) { it }
    }

    private fun refresh(c: Player, timelineChanged: Boolean) {
        if (timelineChanged) {
            playlistItems = List(c.mediaItemCount) { c.getMediaItemAt(it).toQueueItem() }
        }
        playOrder = computePlayOrder(c)
        val currentIndex = c.currentMediaItemIndex
        val current = playlistItems.getOrNull(currentIndex)
        _state.value = PlaybackState(
            isConnected = true,
            isPlaying = c.isPlaying,
            isBuffering = c.playbackState == Player.STATE_BUFFERING,
            current = current,
            durationMs = c.duration.takeIf { it != C.TIME_UNSET } ?: current?.durationMs ?: 0L,
            shuffleEnabled = c.shuffleModeEnabled,
            repeatMode = c.repeatMode.toRepeatMode(),
            queue = playOrder.map { playlistItems[it] },
            currentQueuePosition = if (current == null) -1 else playOrder.indexOf(currentIndex),
        )
    }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            val timelineChanged = events.contains(Player.EVENT_TIMELINE_CHANGED)
            refresh(player, timelineChanged)
        }

        override fun onPlayerError(error: PlaybackException) {
            val title = controller?.currentMediaItem?.mediaMetadata?.title?.toString()
            if (error.errorCode != PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED) {
                _errors.tryEmit(PlaybackError.CannotPlay(title))
            }
        }
    }

    private val controllerListener = object : MediaController.Listener {
        override fun onCustomCommand(
            controller: MediaController,
            command: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (command.customAction == PlaybackCommands.TRACK_SKIPPED &&
                args.getBoolean(PlaybackCommands.KEY_UNSUPPORTED)
            ) {
                _errors.tryEmit(PlaybackError.UnsupportedFormat(args.getString(PlaybackCommands.KEY_TITLE)))
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        override fun onDisconnected(controller: MediaController) {
            if (this@PlayerConnection.controller === controller) release()
        }
    }
}

private fun Int.toRepeatMode(): RepeatMode = when (this) {
    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
    else -> RepeatMode.OFF
}

private fun RepeatMode.toPlayerRepeatMode(): Int = when (this) {
    RepeatMode.OFF -> Player.REPEAT_MODE_OFF
    RepeatMode.ALL -> Player.REPEAT_MODE_ALL
    RepeatMode.ONE -> Player.REPEAT_MODE_ONE
}