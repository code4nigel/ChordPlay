package com.music.bitchord.ui.player

import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.Haptics

/** The least the faded player is drawn at while docking — see the player's fade. */
internal const val DOCK_FADE_FLOOR = 1f / 255f

/**
 * The player's own end of the docking into the mini player, for whichever
 * shape it takes: where it sits on screen, the fade, and the artwork drawn
 * over that fade — see [PlayerDock].
 */
internal fun Modifier.playerDockHost(
    dock: PlayerDock?,
    dockFrame: DockFrame,
    docking: () -> Boolean,
    dockFade: () -> Float,
    sleeveLayer: GraphicsLayer,
): Modifier = this
    .then(
        if (dock != null) {
            Modifier.onGloballyPositioned { coordinates ->
                dockFrame.host = coordinates
                val sizeChanged = dockFrame.anchorFor != coordinates.size
                if (sizeChanged && dockFrame.anchorFor != IntSize.Zero) {
                    dockFrame.anchor = null
                    dockFrame.anchorFor = IntSize.Zero
                    dockFrame.miniInBox = null
                    dockFrame.portalLeft = 0f
                    dockFrame.portalTop = 0f
                    dockFrame.portalScale = 1f
                }
                if (dockFrame.anchor != null && !sizeChanged) {
                    return@onGloballyPositioned
                }
                val offset = dock.offset() ?: return@onGloballyPositioned
                dockFrame.anchorFor = coordinates.size
                dockFrame.anchor = coordinates.positionOnScreen() - Offset(0f, offset.toInt().toFloat())
                if (!dock.attached) dock.attached = true
            }
        } else {
            Modifier
        },
    )
    // Outside the fade below, so the artwork is the one thing on the
    // player that doesn't fade on its way to the bar. Drawn on top of
    // everything while it travels, as the one thing still moving.
    .drawWithContent {
        drawContent()
        if (docking()) {
            translate(dockFrame.portalLeft, dockFrame.portalTop) {
                val portalScale = dockFrame.portalScale
                scale(portalScale, portalScale, pivot = Offset.Zero) { drawLayer(sleeveLayer) }
            }
        }
    }
    // Never quite zero while docking. A layer at alpha 0 is skipped
    // outright, children and all.
    .graphicsLayer { alpha = if (docking()) dockFade().coerceAtLeast(DOCK_FADE_FLOOR) else 1f }

/**
 * What one frame of docking into the mini player worked out — see [PlayerDock].
 */
internal class DockFrame {
    var anchor: Offset? by mutableStateOf(null)
    var anchorFor = IntSize.Zero
    var miniInBox: Rect? = null
    var offset = 0f
    var ride = 0f
    var sleeve: Rect = Rect.Zero

    private var cornerRadius = Dp.Unspecified
    private var corner: Shape = RectangleShape
    fun cornerShape(radius: Dp): Shape {
        if (radius != cornerRadius) {
            cornerRadius = radius
            corner = RoundedCornerShape(radius)
        }
        return corner
    }

    var portalLeft = 0f
    var portalTop = 0f
    var portalScale = 1f
    var host: LayoutCoordinates? = null
    var landscapeArt: LayoutCoordinates? = null
}

/** Where the landscape sleeve sits this frame while docking into the mini player. */
internal class LandscapeDockPose(val left: Float, val top: Float, val scale: Float)

/** Keeps content composed only while [shown] returns true. */
@Composable
internal fun WhileShown(shown: () -> Boolean, content: @Composable () -> Unit) {
    if (shown()) content()
}

/**
 * Horizontal fling skips tracks; the artwork follows the finger through [setSwipeOffset].
 */
internal fun Modifier.skipSwipeGesture(
    pipelineOpen: Boolean,
    castOpen: Boolean,
    panelScrolling: Boolean,
    controlsLocked: Boolean,
    swipeThreshold: Float,
    haptics: Haptics,
    onBlockedControl: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    setSwipeOffset: (Float) -> Unit,
): Modifier = pointerInput(pipelineOpen, castOpen, panelScrolling, controlsLocked) {
    if (pipelineOpen || castOpen || panelScrolling) return@pointerInput
    var total = 0f
    detectHorizontalDragGestures(
        onDragStart = { total = 0f },
        onDragCancel = { setSwipeOffset(0f) },
        onDragEnd = {
            val crossed = total <= -swipeThreshold || total >= swipeThreshold
            when {
                controlsLocked -> if (crossed) onBlockedControl()
                total <= -swipeThreshold -> {
                    haptics.play(Haptic.SkipNext)
                    onNext()
                }
                total >= swipeThreshold -> {
                    haptics.play(Haptic.SkipPrevious)
                    onPrevious()
                }
            }
            setSwipeOffset(0f)
        },
        onHorizontalDrag = { _, delta ->
            total += delta
            setSwipeOffset(total * 0.35f)
        },
    )
}

internal suspend fun AwaitPointerEventScope.dragQueueIn(
    down: PointerInputChange,
    travel: Float,
    slide: androidx.compose.runtime.MutableFloatState,
    onHold: (Boolean) -> Unit,
    onSettle: (Boolean) -> Unit,
) {
    if (travel <= 0f) return
    val slop = awaitVerticalTouchSlopOrCancellation(down.id) { change, overSlop ->
        if (overSlop < 0f) change.consume()
    } ?: return

    val initialDrag = slop.positionChange().y
    if (initialDrag >= 0f) return
    slop.consume()

    onHold(true)
    var totalDy = initialDrag
    var prevT = System.nanoTime()
    var velocity = 0f
    slide.floatValue = (-totalDy / travel).coerceIn(0f, 1f)

    val completed = verticalDrag(slop.id) { change ->
        val dy = change.positionChange().y
        change.consume()
        totalDy += dy
        val now = System.nanoTime()
        val dt = ((now - prevT) / 1e9f).coerceAtLeast(1e-4f)
        velocity = -dy / dt
        prevT = now
        slide.floatValue = (-totalDy / travel).coerceIn(0f, 1f)
    }

    onHold(false)
    val open = if (completed) {
        val current = slide.floatValue
        when {
            velocity > 450f / travel -> true
            velocity < -450f / travel -> false
            else -> current >= 0.3f
        }
    } else {
        false
    }
    onSettle(open)
}
