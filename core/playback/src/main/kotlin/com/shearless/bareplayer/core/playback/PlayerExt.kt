package com.shearless.bareplayer.core.playback

import androidx.media3.common.C
import androidx.media3.common.Player
import com.shearless.bareplayer.core.model.QueueSnapshot
import com.shearless.bareplayer.core.model.RepeatMode

/** Playlist indices in the order they will play (the shuffle order when shuffle is on). */
internal fun Player.playOrder(): List<Int> {
    val count = mediaItemCount
    if (!shuffleModeEnabled) return List(count) { it }
    val timeline = currentTimeline
    if (timeline.isEmpty) return emptyList()
    val order = ArrayList<Int>(count)
    var index = timeline.getFirstWindowIndex(true)
    while (index != C.INDEX_UNSET && order.size < count) {
        order += index
        index = timeline.getNextWindowIndex(index, Player.REPEAT_MODE_OFF, true)
    }
    return if (order.size == count) order else List(count) { it }
}

internal fun Player.toQueueSnapshot(): QueueSnapshot = QueueSnapshot(
    trackIds = List(mediaItemCount) { getMediaItemAt(it).mediaId.toLongOrNull() ?: -1L },
    currentIndex = currentMediaItemIndex,
    positionMs = currentPosition.coerceAtLeast(0L),
    shuffleEnabled = shuffleModeEnabled,
    shuffleOrder = if (shuffleModeEnabled) playOrder() else emptyList(),
    repeatMode = repeatMode.toRepeatMode(),
)

internal fun Int.toRepeatMode(): RepeatMode = when (this) {
    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
    else -> RepeatMode.OFF
}

internal fun RepeatMode.toPlayerRepeatMode(): Int = when (this) {
    RepeatMode.OFF -> Player.REPEAT_MODE_OFF
    RepeatMode.ALL -> Player.REPEAT_MODE_ALL
    RepeatMode.ONE -> Player.REPEAT_MODE_ONE
}
