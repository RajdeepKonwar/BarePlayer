package com.rajdeepkonwar.bareplayer.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * A draggable scrollbar thumb with a section bubble, shown while the list scrolls. State is read
 * in the layout phase so scrolling does not recompose this composable.
 */
@Composable
fun FastScroller(
    itemCount: Int,
    firstVisibleIndex: () -> Int,
    isScrollInProgress: Boolean,
    scrollToIndex: suspend (Int) -> Unit,
    sectionAt: (Int) -> String?,
    modifier: Modifier = Modifier,
) {
    if (itemCount < MIN_ITEMS) return
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var visible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isScrollInProgress, dragging) {
        if (isScrollInProgress || dragging) {
            visible = true
        } else {
            delay(HIDE_DELAY_MS)
            visible = false
        }
    }

    BoxWithConstraints(modifier.fillMaxHeight()) {
        val thumbHeight = 48.dp
        val travelPx = with(LocalDensity.current) { (maxHeight - thumbHeight).toPx() }.coerceAtLeast(1f)
        val lastIndex = (itemCount - 1).coerceAtLeast(1)

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.offset {
                    val fraction = if (dragging) dragFraction else firstVisibleIndex() / lastIndex.toFloat()
                    IntOffset(0, (fraction.coerceIn(0f, 1f) * travelPx).roundToInt())
                },
            ) {
                if (dragging) {
                    val section = sectionAt((dragFraction * lastIndex).roundToInt())
                    if (section != null) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        ) {
                            Text(
                                text = section,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
                Box(
                    Modifier
                        .padding(end = 4.dp)
                        .size(width = 8.dp, height = thumbHeight)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                        .pointerInput(itemCount, travelPx) {
                            detectVerticalDragGestures(
                                onDragStart = {
                                    dragFraction = firstVisibleIndex() / lastIndex.toFloat()
                                    dragging = true
                                },
                                onDragEnd = { dragging = false },
                                onDragCancel = { dragging = false },
                            ) { change, dragAmount ->
                                change.consume()
                                dragFraction = (dragFraction + dragAmount / travelPx).coerceIn(0f, 1f)
                                val target = (dragFraction * lastIndex).roundToInt()
                                scope.launch { scrollToIndex(target) }
                            }
                        },
                )
            }
        }
    }
}

private const val MIN_ITEMS = 40
private const val HIDE_DELAY_MS = 1_500L
