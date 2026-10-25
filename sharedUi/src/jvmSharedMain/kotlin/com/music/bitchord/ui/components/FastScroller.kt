package com.music.bitchord.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

/**
 * Adapter abstracting scroll and layout details for LazyListState and LazyGridState.
 */
interface FastScrollAdapter {
    val totalItemsCount: Int
    val visibleItemsCount: Int
    val firstVisibleItemIndex: Int
    val firstVisibleItemScrollOffset: Int
    val isScrollInProgress: Boolean
    val firstItemSizePx: Int
    suspend fun scrollToItem(index: Int)
}

/**
 * [FastScrollAdapter] implementation for [LazyListState].
 */
class LazyListFastScrollAdapter(
    val state: LazyListState,
) : FastScrollAdapter {
    override val totalItemsCount: Int
        get() = state.layoutInfo.totalItemsCount

    override val visibleItemsCount: Int
        get() = state.layoutInfo.visibleItemsInfo.size

    override val firstVisibleItemIndex: Int
        get() = state.firstVisibleItemIndex

    override val firstVisibleItemScrollOffset: Int
        get() = state.firstVisibleItemScrollOffset

    override val isScrollInProgress: Boolean
        get() = state.isScrollInProgress

    override val firstItemSizePx: Int
        get() = state.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0

    override suspend fun scrollToItem(index: Int) {
        state.scrollToItem(index)
    }
}

/**
 * [FastScrollAdapter] implementation for [LazyGridState].
 */
class LazyGridFastScrollAdapter(
    val state: LazyGridState,
) : FastScrollAdapter {
    override val totalItemsCount: Int
        get() = state.layoutInfo.totalItemsCount

    override val visibleItemsCount: Int
        get() = state.layoutInfo.visibleItemsInfo.size

    override val firstVisibleItemIndex: Int
        get() = state.firstVisibleItemIndex

    override val firstVisibleItemScrollOffset: Int
        get() = state.firstVisibleItemScrollOffset

    override val isScrollInProgress: Boolean
        get() = state.isScrollInProgress

    override val firstItemSizePx: Int
        get() = state.layoutInfo.visibleItemsInfo.firstOrNull()?.size?.height ?: 0

    override suspend fun scrollToItem(index: Int) {
        state.scrollToItem(index)
    }
}

/**
 * FastScroller container wrapping a [LazyListState] content.
 * Displays a sleek draggable thumb along the right edge with auto-hide and responsive touch feedback.
 */
@Composable
fun FastScroller(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit,
) {
    val adapter = remember(listState) { LazyListFastScrollAdapter(listState) }
    FastScrollerBox(
        adapter = adapter,
        modifier = modifier,
        contentPadding = contentPadding,
        content = content,
    )
}

/**
 * FastScroller container overload wrapping a [LazyGridState] content.
 */
@Composable
fun FastScroller(
    gridState: LazyGridState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit,
) {
    val adapter = remember(gridState) { LazyGridFastScrollAdapter(gridState) }
    FastScrollerBox(
        adapter = adapter,
        modifier = modifier,
        contentPadding = contentPadding,
        content = content,
    )
}

@Composable
private fun FastScrollerBox(
    adapter: FastScrollAdapter,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        content()

        FastScrollOverlay(
            adapter = adapter,
            contentPadding = contentPadding,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(),
        )
    }
}

private val MIN_THUMB_HEIGHT: Dp = 44.dp
private val TRACK_TOUCH_WIDTH: Dp = 32.dp
private val THUMB_IDLE_WIDTH: Dp = 4.dp
private val THUMB_DRAG_WIDTH: Dp = 6.dp
private val THUMB_RIGHT_INSET: Dp = 2.dp

@Composable
private fun FastScrollOverlay(
    adapter: FastScrollAdapter,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    var isDragging by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }

    val isScrolling = adapter.isScrollInProgress

    // Auto-hide: smoothly fades out after 1.5s of inactivity; wakes immediately on scroll or drag
    LaunchedEffect(isScrolling, isDragging) {
        if (isScrolling || isDragging) {
            isVisible = true
        } else {
            delay(1500L)
            isVisible = false
        }
    }

    val canScroll by remember(adapter) {
        derivedStateOf {
            adapter.totalItemsCount > adapter.visibleItemsCount && adapter.visibleItemsCount > 0
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible && canScroll) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isVisible) 150 else 400,
            easing = FastOutSlowInEasing,
        ),
        label = "FastScrollerAlpha",
    )

    val thumbWidth by animateDpAsState(
        targetValue = if (isDragging) THUMB_DRAG_WIDTH else THUMB_IDLE_WIDTH,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "FastScrollerWidth",
    )

    val thumbColor by animateColorAsState(
        targetValue = if (isDragging) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.35f),
        animationSpec = tween(durationMillis = 150),
        label = "FastScrollerColor",
    )

    val topPadding = contentPadding.calculateTopPadding()
    val bottomPadding = contentPadding.calculateBottomPadding()
    val endPadding = contentPadding.calculateEndPadding(layoutDirection)

    BoxWithConstraints(
        modifier = modifier
            .padding(top = topPadding, bottom = bottomPadding, end = endPadding)
            .width(TRACK_TOUCH_WIDTH)
            .fillMaxHeight(),
    ) {
        val trackHeightPx = constraints.maxHeight.toFloat()
        if (trackHeightPx <= 0f || !canScroll) return@BoxWithConstraints

        val minThumbHeightPx = with(density) { MIN_THUMB_HEIGHT.toPx() }
        val totalItems = adapter.totalItemsCount
        val visibleItems = adapter.visibleItemsCount

        val thumbHeightPx = remember(totalItems, visibleItems, trackHeightPx, minThumbHeightPx) {
            if (totalItems <= 0) minThumbHeightPx
            else {
                val ratio = (visibleItems.toFloat() / totalItems.toFloat()).coerceIn(0f, 1f)
                (trackHeightPx * ratio).coerceIn(minThumbHeightPx, trackHeightPx)
            }
        }

        val maxThumbY = (trackHeightPx - thumbHeightPx).coerceAtLeast(0f)

        // Compute idle thumb position based on scroll offset
        val computedThumbY by remember(adapter, maxThumbY) {
            derivedStateOf {
                val maxScrollIndex = (adapter.totalItemsCount - adapter.visibleItemsCount).coerceAtLeast(1)
                val itemSize = adapter.firstItemSizePx
                val fractionalOffset = if (itemSize > 0) {
                    (adapter.firstVisibleItemScrollOffset.toFloat() / itemSize).coerceIn(0f, 1f)
                } else 0f

                val progress = ((adapter.firstVisibleItemIndex + fractionalOffset) / maxScrollIndex).coerceIn(0f, 1f)
                progress * maxThumbY
            }
        }

        var dragThumbY by remember { mutableFloatStateOf(0f) }
        var targetIndexToScroll by remember { mutableIntStateOf(-1) }

        val currentMaxThumbY by rememberUpdatedState(maxThumbY)
        val currentThumbHeightPx by rememberUpdatedState(thumbHeightPx)
        val currentTotalItems by rememberUpdatedState(totalItems)
        val currentComputedThumbY by rememberUpdatedState(computedThumbY)
        val currentCanScroll by rememberUpdatedState(canScroll)
        val currentAlpha by rememberUpdatedState(alpha)
        val touchSlopPx = with(density) { 24.dp.toPx() }
        val currentTouchSlopPx by rememberUpdatedState(touchSlopPx)

        // 60 FPS smooth throttled scrolling
        LaunchedEffect(isDragging) {
            if (isDragging) {
                var lastScrolled = -1
                while (isActive) {
                    val target = targetIndexToScroll
                    if (target >= 0 && target != lastScrolled) {
                        lastScrolled = target
                        adapter.scrollToItem(target)
                    }
                    delay(16)
                }
            }
        }

        val gestureModifier = Modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                if (!currentCanScroll || currentAlpha <= 0.05f) {
                    return@awaitEachGesture
                }

                val startThumbY = currentComputedThumbY
                val thumbHeight = currentThumbHeightPx
                val slop = currentTouchSlopPx
                val minY = startThumbY - slop
                val maxY = startThumbY + thumbHeight + slop

                if (down.position.y !in minY..maxY) {
                    // Touch is NOT on the thumb: allow event to pass through to song rows and 3-dots option buttons
                    return@awaitEachGesture
                }

                // Touch IS on the thumb: consume down, set isDragging = true, and record touchOffsetInThumb
                down.consume()
                isDragging = true
                val touchOffsetInThumb = down.position.y - startThumbY
                dragThumbY = startThumbY

                try {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                            ?: event.changes.firstOrNull()
                            ?: break

                        if (!change.pressed) {
                            break
                        }

                        change.consume()
                        val maxThumb = currentMaxThumbY
                        val total = currentTotalItems
                        val newDragThumbY = if (maxThumb > 0f) {
                            (change.position.y - touchOffsetInThumb).coerceIn(0f, maxThumb)
                        } else 0f

                        dragThumbY = newDragThumbY
                        val fraction = if (maxThumb > 0f) newDragThumbY / maxThumb else 0f
                        if (total > 0) {
                            val targetIndex = (fraction * (total - 1)).roundToInt().coerceIn(0, total - 1)
                            targetIndexToScroll = targetIndex
                        }
                    }
                } finally {
                    isDragging = false
                    targetIndexToScroll = -1
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(gestureModifier),
        ) {
            if (alpha <= 0.001f) return@Canvas

            val currentThumbY = if (isDragging) dragThumbY else computedThumbY
            val wPx = thumbWidth.toPx()
            val rPx = THUMB_RIGHT_INSET.toPx()
            val left = size.width - wPx - rPx
            val cornerRadius = CornerRadius(wPx / 2f, wPx / 2f)

            drawRoundRect(
                color = thumbColor.copy(alpha = thumbColor.alpha * alpha),
                topLeft = Offset(left, currentThumbY),
                size = Size(wPx, thumbHeightPx),
                cornerRadius = cornerRadius,
            )
        }
    }
}
