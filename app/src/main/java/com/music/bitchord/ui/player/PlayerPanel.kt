package com.music.bitchord.ui.player

// Ported from Zenith (rezapranata089-bit/Zenith, GPL-3.0), a sibling fork of
// BitChord: the mini player's pill and the full player as one panel.

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import com.music.bitchord.data.model.Song
import dev.chrisbanes.haze.HazeState
import kotlin.math.roundToInt

/** What the mini player's pill is made of, kept for host compatibility. */
internal enum class PanelPillMaterial {
    Glass,
    Frost,
    Solid,
}

/**
 * Two-plane player transition container: the mini player stays intact on Plane 1
 * underneath while the full player slides and fades cleanly on Plane 2 above it,
 * with the cover artwork flying between both planes via [PlayerDock].
 */
@Suppress("UNUSED_PARAMETER")
@Composable
internal fun PlayerPanel(
    dock: PlayerDock?,
    motion: PlayerSheetMotion?,
    song: Song,
    isPlaying: Boolean,
    isLoading: Boolean,
    controlsLocked: Boolean,
    onBlockedControl: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    scrimAlpha: Float,
    material: PanelPillMaterial,
    hazeState: HazeState,
    onLaidOut: (IntSize) -> Unit,
    modifier: Modifier = Modifier,
    /** Drawn above the panel, unclipped — the ⋯ menu & playlist overlays. */
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable () -> Unit,
) {
    DisposableEffect(dock) {
        onDispose { dock?.releasePanel() }
    }

    Box(
        modifier
            .fillMaxSize()
            .onSizeChanged(onLaidOut)
            .onGloballyPositioned { dock?.panelPlaced(it) }
            .drawBehind {
                if (dock == null) return@drawBehind
                val open = dock.openFraction()
                // Not under a portrait player that is fully up: its own
                // background is opaque, and a full-screen blend behind it
                // would be paid on every frame for nothing anyone can see.
                if (open > 0f && !(open >= 1f && dock.attached)) {
                    drawRect(Color.Black.copy(alpha = scrimAlpha * open))
                }
            },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = dock?.offset()?.roundToInt()?.toFloat() ?: 0f
                }
                // The panel is the touch target wherever it stands, so a tap
                // on a part of the player that has no control of its own
                // never falls through to the page underneath.
                .pointerInput(Unit) {
                    awaitEachGesture { awaitFirstDown(requireUnconsumed = false) }
                }
                .then(if (motion != null) Modifier.playerPullDown(motion) else Modifier),
        ) {
            content()
        }
        overlay()
    }
}

/**
 * Drag the open player down to put it away — what the modal sheet used to do
 * for it. A drag nothing inside claims, and the pull a list leaves over when
 * it is already at its top, both move the panel through [PlayerSheetMotion]:
 * the same motion the pill's pull and every close run on.
 */
@Composable
internal fun Modifier.playerPullDown(motion: PlayerSheetMotion): Modifier {
    val connection = remember(motion) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Back up first: a pulled panel returns before the list scrolls again.
                if (!motion.pulling || available.y >= 0f) return Offset.Zero
                return Offset(0f, motion.pullBy(available.y))
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source != NestedScrollSource.UserInput || available.y <= 0f) {
                    return Offset.Zero
                }
                return Offset(0f, motion.pullBy(available.y))
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!motion.pulling) return Velocity.Zero
                motion.releasePull(available.y)
                return available
            }
        }
    }
    val dragState = rememberDraggableState { delta -> motion.pullBy(delta) }
    return this
        .nestedScroll(connection)
        .draggable(
            state = dragState,
            orientation = Orientation.Vertical,
            onDragStopped = { velocity -> motion.releasePull(velocity) },
        )
}

