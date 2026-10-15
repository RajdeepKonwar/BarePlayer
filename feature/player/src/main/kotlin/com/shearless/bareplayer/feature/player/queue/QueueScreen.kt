package com.shearless.bareplayer.feature.player.queue

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
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
import com.shearless.bareplayer.core.model.QueueItem
import com.shearless.bareplayer.core.ui.LocalAppDependencies
import com.shearless.bareplayer.core.ui.components.ArtImage
import com.shearless.bareplayer.core.ui.components.EmptyState
import com.shearless.bareplayer.core.ui.components.ReorderState
import com.shearless.bareplayer.core.ui.components.reorderHandle
import com.shearless.bareplayer.core.ui.components.artistLabel
import com.shearless.bareplayer.core.ui.icons.BareIcons
import com.shearless.bareplayer.feature.player.R
import kotlinx.coroutines.launch
import com.shearless.bareplayer.core.ui.R as UiR

/** A queue row with its list key; the same track may appear more than once, so keys count occurrences. */
private data class QueueRowItem(val key: String, val item: QueueItem)

private fun keyed(queue: List<QueueItem>): List<QueueRowItem> {
    val seen = HashMap<Long, Int>()
    return queue.map { item ->
        val n = seen.merge(item.trackId, 1, Int::plus)!!
        QueueRowItem("${item.trackId}#$n", item)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QueueScreen(onBack: () -> Unit) {
    val playback = LocalAppDependencies.current.playbackController
    val state by playback.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Local copy so drags can reorder instantly; upstream changes replace it when not dragging.
    // Keys travel with their rows, so the list can never be measured against stale keys.
    val items = remember { mutableStateListOf<QueueRowItem>() }
    val reorder = remember(listState) {
        ReorderState(listState) { from, to -> items.add(to, items.removeAt(from)) }
    }
    LaunchedEffect(state.queue) {
        if (reorder.draggingIndex == null) {
            items.clear()
            items.addAll(keyed(state.queue))
        }
    }
    var scrolledToCurrent by remember { mutableStateOf(false) }
    LaunchedEffect(items.isNotEmpty()) {
        if (items.isEmpty() || scrolledToCurrent) return@LaunchedEffect
        scrolledToCurrent = true
        val position = playback.state.value.currentQueuePosition
        if (position in 1 until items.size) listState.scrollToItem(position)
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
        val moveUp = stringResource(R.string.move_up)
        val moveDown = stringResource(R.string.move_down)
        val remove = stringResource(R.string.remove_from_queue)
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            itemsIndexed(items, key = { _, row -> row.key }, contentType = { _, _ -> "queue" }) { index, row ->
                val dragging = reorder.draggingIndex == index
                val rowModifier = if (dragging) {
                    Modifier.zIndex(1f).graphicsLayer { translationY = reorder.dragOffset }
                } else {
                    Modifier.animateItem()
                }
                SwipeToRemove(onRemove = { playback.removeQueueItem(index) }, modifier = rowModifier) {
                    QueueRow(
                        item = row.item,
                        isCurrent = index == state.currentQueuePosition && reorder.draggingIndex == null,
                        elevated = dragging,
                        onClick = { playback.playQueuePosition(index) },
                        dragHandle = {
                            DragHandle(Modifier.reorderHandle(reorder, row.key, index, items.size, playback::moveQueueItem))
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
    dragHandle: @Composable () -> Unit,
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
            dragHandle()
        }
    }
}

@Composable
private fun DragHandle(modifier: Modifier) {
    Box(contentAlignment = Alignment.Center, modifier = modifier.size(48.dp)) {
        Icon(
            painterResource(BareIcons.DragHandle),
            contentDescription = stringResource(R.string.reorder),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
