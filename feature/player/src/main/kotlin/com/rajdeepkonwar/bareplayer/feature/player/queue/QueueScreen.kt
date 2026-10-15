package com.rajdeepkonwar.bareplayer.feature.player.queue

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rajdeepkonwar.bareplayer.core.model.QueueItem
import com.rajdeepkonwar.bareplayer.core.ui.LocalAppDependencies
import com.rajdeepkonwar.bareplayer.core.ui.components.ArtImage
import com.rajdeepkonwar.bareplayer.core.ui.components.EmptyState
import com.rajdeepkonwar.bareplayer.core.ui.components.artistLabel
import com.rajdeepkonwar.bareplayer.core.ui.icons.BareIcons
import com.rajdeepkonwar.bareplayer.feature.player.R
import kotlinx.coroutines.launch
import com.rajdeepkonwar.bareplayer.core.ui.R as UiR

/** Stable keys for queue rows; the same track may appear more than once. */
private fun keysFor(queue: List<QueueItem>): List<String> {
    val seen = HashMap<Long, Int>()
    return queue.map { item ->
        val n = seen.merge(item.trackId, 1, Int::plus)!!
        "${item.trackId}#$n"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QueueScreen(onBack: () -> Unit) {
    val playback = LocalAppDependencies.current.playbackController
    val state by playback.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Local copy so drags can reorder instantly; upstream changes replace it when not dragging.
    val items = remember { mutableStateListOf<QueueItem>() }
    val reorder = remember(listState) {
        ReorderState(listState) { from, to -> items.add(to, items.removeAt(from)) }
    }
    LaunchedEffect(state.queue) {
        if (reorder.draggingIndex == null) {
            items.clear()
            items.addAll(state.queue)
        }
    }
    LaunchedEffect(Unit) {
        val position = playback.state.value.currentQueuePosition
        if (position > 0) listState.scrollToItem(position)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.queue))
                        Text(
                            text = pluralStringResource(UiR.plurals.song_count, state.queue.size, state.queue.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(BareIcons.Back), contentDescription = stringResource(UiR.string.back))
                    }
                },
            )
        },
    ) { padding ->
        if (state.queue.isEmpty()) {
            EmptyState(BareIcons.Queue, stringResource(R.string.queue_empty), Modifier.padding(padding))
            return@Scaffold
        }
        val keys = keysFor(items)
        val moveUp = stringResource(R.string.move_up)
        val moveDown = stringResource(R.string.move_down)
        val remove = stringResource(R.string.remove_from_queue)
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            itemsIndexed(items, key = { index, _ -> keys[index] }, contentType = { _, _ -> "queue" }) { index, item ->
                val dragging = reorder.draggingIndex == index
                val rowModifier = if (dragging) {
                    Modifier.zIndex(1f).graphicsLayer { translationY = reorder.dragOffset }
                } else {
                    Modifier.animateItem()
                }
                SwipeToRemove(onRemove = { playback.removeQueueItem(index) }, modifier = rowModifier) {
                    QueueRow(
                        item = item,
                        isCurrent = index == state.currentQueuePosition && reorder.draggingIndex == null,
                        elevated = dragging,
                        onClick = { playback.playQueuePosition(index) },
                        dragHandle = Modifier.pointerInput(keys[index]) {
                            detectDragGestures(
                                onDragStart = { reorder.onDragStart(index) },
                                onDragEnd = { reorder.onDragEnd()?.let { (from, to) -> playback.moveQueueItem(from, to) } },
                                onDragCancel = { reorder.onDragEnd()?.let { (from, to) -> playback.moveQueueItem(from, to) } },
                            ) { change, amount ->
                                change.consume()
                                reorder.onDrag(amount.y)
                            }
                        },
                        modifier = Modifier.semantics {
                            customActions = buildList {
                                if (index > 0) add(CustomAccessibilityAction(moveUp) { playback.moveQueueItem(index, index - 1); true })
                                if (index < items.lastIndex) {
                                    add(CustomAccessibilityAction(moveDown) { playback.moveQueueItem(index, index + 1); true })
                                }
                                add(CustomAccessibilityAction(remove) { playback.removeQueueItem(index); true })
                            }
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToRemove(onRemove: () -> Unit, modifier: Modifier, content: @Composable () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        onDismiss = { value ->
            if (value != SwipeToDismissBoxValue.Settled) onRemove()
            // Row keys can be reused by a duplicate track, so never leave the state dismissed.
            scope.launch { dismissState.reset() }
        },
        backgroundContent = {
            Box(
                contentAlignment = Alignment.CenterEnd,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
            ) {
                Icon(
                    painterResource(BareIcons.Delete),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
        content = { content() },
    )
}

@Composable
private fun QueueRow(
    item: QueueItem,
    isCurrent: Boolean,
    elevated: Boolean,
    onClick: () -> Unit,
    dragHandle: Modifier,
    modifier: Modifier = Modifier,
) {
    Surface(
        tonalElevation = if (elevated) 6.dp else 0.dp,
        modifier = modifier.then(if (elevated) Modifier.shadow(6.dp) else Modifier),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clickable(onClick = onClick)
                .padding(start = 16.dp),
        ) {
            ArtImage(item.artKey, Modifier.size(44.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else null,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = artistLabel(item.artist),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = dragHandle.size(48.dp),
            ) {
                Icon(
                    painterResource(BareIcons.DragHandle),
                    contentDescription = stringResource(R.string.reorder),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
