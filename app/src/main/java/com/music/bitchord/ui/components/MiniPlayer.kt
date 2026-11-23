/*
 * Copyright (C) 2024-2026 Kushagra Singh and BitChord contributors <https://github.com/kushagrasinghx/BitChord>
 * Copyright (C) 2026 Shivanshu (code4nigel) <https://github.com/code4nigel/ChordPlay>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.music.bitchord.ui.components

import com.music.bitchord.R
import com.music.bitchord.ui.player.LiveBadge

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.bitchord.data.model.ROW_ART_PX
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.data.settings.AppDesignStyle
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.playback.PlaybackPosition
import com.music.bitchord.sharedui.resources.Res as SharedRes
import com.music.bitchord.sharedui.resources.ic_player_next
import com.music.bitchord.ui.components.thumbnailBorder
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import com.music.bitchord.ui.player.PlayerDock
import com.music.bitchord.ui.theme.rememberArtworkPalette
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.math.sqrt

/**
 * The transport buttons' touch target. Material's default 48dp is what a bar
 * this slim is really made of, so it sets the height on its own.
 */
private val GLYPH_SLOT = 40.dp

/**
 * The play and skip glyphs themselves.
 *
 * Deliberately grown inside [GLYPH_SLOT] rather than by growing the slot: the
 * slot is level with the 40dp artwork opposite it, and it is the taller of the
 * two that sets the row's height — so a bigger slot would make the whole bar
 * taller, which is not what a bigger glyph is being asked for. At 32 there is
 * still 4dp of clearance to the slot's edge on every side.
 */
private val GLYPH_SIZE = 32.dp

/** The spinner that stands in for the play glyph, kept in proportion to it. */
private val SPINNER_SIZE = 22.dp

/**
 * The gap between the two transport controls.
 *
 * Material asks for at least 8dp between adjacent touch targets, and these had
 * none: two [GLYPH_SLOT] boxes sharing an edge, so the boundary between "pause"
 * and "skip" was a line with nothing either side of it. What space there looked
 * to be was only the margin each glyph keeps inside its own slot, and a thumb
 * lands on a target's edge far more often than it lands on a glyph's.
 *
 * Taken from the title's width rather than the bar's height, so nothing above
 * or below it moves.
 */
private val TRANSPORT_GAP = 8.dp

/**
 * Vertical padding, which with the 40dp artwork sets the bar's height at 56dp
 * and so its pill radius at 28.
 */
private val ROW_PADDING_VERTICAL = 8.dp

/**
 * Horizontal padding, deliberately larger than the vertical.
 *
 * A pill's ends are semicircles, so the edge nearest the artwork is not the
 * one beside it but the one curving away above and below it. At the artwork's
 * top corner that edge has already come 8.4dp in from the left — level with
 * where square corners would have put the whole side. Padding the ends by the
 * vertical figure would leave the artwork touching the curve; 12 clears it
 * with room, and reads as centred rather than jammed into the round.
 */
private val ROW_PADDING_HORIZONTAL = 12.dp

/**
 * The artwork's corner, on the 8dp every other thumbnail in the app carries.
 *
 * It used to be 7, picked so the bar's corner could sit concentric with it.
 * A pill has no corner to be concentric with — its radius is whatever half the
 * height happens to be — so that constraint is gone and the artwork can go
 * back to matching [SongRow].
 */
private val ART_CORNER = 8.dp

/** Distance that makes a horizontal drag an intentional track change. */
private val TRACK_SWIPE_THRESHOLD = 60.dp

/**
 * How much further a drag may go sideways than up and still be a pull. A
 * thumb swiping up from the bottom of the screen travels on an arc, and judged
 * level, 1:1, a good share of real pulls came out sideways — too short to skip
 * a track, so they did nothing at all. Steeper than about 34 degrees above
 * level is a pull.
 */
private const val PULL_BIAS = 1.5f

/**
 * The finger distance over which the bar's own rise slows to about half its
 * starting rate. It is the player that follows the finger; the bar gives a
 * little and then less, so the pull visibly takes hold of something before
 * the player has come up over it.
 */
private val PULL_LIFT_EASE = 72.dp

/** The bar's rise per pixel of finger as a pull starts. */
private const val PULL_LIFT_RATE = 0.5f

/**
 * How far the bar rises for [pulled] pixels of finger: [PULL_LIFT_RATE] of the
 * finger to begin with, slowing the further it goes — a rubber band with no
 * end to it, so the bar keeps creeping up for as long as the finger does
 * rather than meeting a ceiling. Its rate is RATE / sqrt(1 + pulled / ease).
 */
private fun pullLift(pulled: Float, ease: Float): Float =
    if (pulled <= 0f || ease <= 0f) {
        0f
    } else {
        PULL_LIFT_RATE * 2f * ease * (sqrt(1f + pulled / ease) - 1f)
    }

/**
 * Pulling the mini player up into the full player, the way the full player is
 * pulled back down into it: the player follows the finger the whole way, and
 * on release carries on to whichever end the gesture meant — open for a flick
 * upwards or for most of the way up, closed for anything less.
 *
 * Implemented by the host, which owns the player's sheet.
 */
interface MiniPlayerPull {
    /** The finger moved this far, in pixels — negative is upwards. */
    fun drag(delta: Float)

    /** The finger lifted, moving this fast in pixels a second. */
    fun release(velocity: Float)
}

/**
 * Both of the bar's drags, for both mini-player materials: sideways through
 * the queue — a left swipe advances, a right one goes back, matching the full
 * player's artwork — and up into the player through [pull].
 *
 * One detector rather than one per axis, so a gesture is decided once, by the
 * way it is heading as it passes the touch slop. Two detectors raced each
 * other to their own axis's slop, and the sideways one was asked first — see
 * [PULL_BIAS] for what that did to pulls. The tap to expand is left to the
 * clickable, and is cancelled by this consuming the drag, as it was before.
 *
 * Under a pull the bar itself rises with it, less and less ([pullLift]), so this goes
 * where the bar's own transforms start: the finger is read outside that lift,
 * so a rising bar doesn't take distance off the finger it is following.
 *
 * Track swipes wait until the finger lifts, so one long gesture never skips
 * more than one item.
 */
@Composable
internal fun Modifier.miniPlayerGestures(
    pull: MiniPlayerPull?,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    /** Listening in a party whose host holds the controls. Swipes say so instead of skipping. */
    locked: Boolean = false,
    onBlocked: () -> Unit = {},
    enableSwipeToSkip: Boolean = true,
    onHorizontalDrag: ((Float) -> Unit)? = null,
    onHorizontalDragEnd: (() -> Unit)? = null,
    durationMs: Long = 0L,
    currentPositionMs: () -> Long = { 0L },
    onScrubStart: (() -> Unit)? = null,
    onScrubMove: ((Float) -> Unit)? = null,
    onScrubSeek: ((Long) -> Unit)? = null,
    onScrubEnd: ((Long) -> Unit)? = null,
    onScrubCancel: (() -> Unit)? = null,
): Modifier {
    // Playback state updates can recompose the bar while a finger is down.
    // Keep the gesture coroutine alive through those updates while still
    // dispatching to the latest controller callbacks when the drag finishes.
    val currentPull by rememberUpdatedState(pull)
    val currentOnNext by rememberUpdatedState(onNext)
    val currentOnPrevious by rememberUpdatedState(onPrevious)
    val currentLocked by rememberUpdatedState(locked)
    val currentOnBlocked by rememberUpdatedState(onBlocked)
    val currentEnableSwipe by rememberUpdatedState(enableSwipeToSkip)
    val currentOnDrag by rememberUpdatedState(onHorizontalDrag)
    val currentOnDragEnd by rememberUpdatedState(onHorizontalDragEnd)
    val currentDurationMs by rememberUpdatedState(durationMs)
    val currentPosProvider by rememberUpdatedState(currentPositionMs)
    val currentOnScrubStart by rememberUpdatedState(onScrubStart)
    val currentOnScrubMove by rememberUpdatedState(onScrubMove)
    val currentOnScrubSeek by rememberUpdatedState(onScrubSeek)
    val currentOnScrubEnd by rememberUpdatedState(onScrubEnd)
    val currentOnScrubCancel by rememberUpdatedState(onScrubCancel)
    val haptics = rememberHaptics()
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    // Read at draw only, in the layer below: a pull moves the bar without
    // recomposing it.
    val lift = remember { mutableFloatStateOf(0f) }
    val settle = remember { arrayOfNulls<Job>(1) }
    return pointerInput(Unit) {
        val trackThreshold = TRACK_SWIPE_THRESHOLD.toPx()
        val liftEase = PULL_LIFT_EASE.toPx()
        val maxVelocity = viewConfiguration.maximumFlingVelocity
        val holdTimeoutMs = viewConfiguration.longPressTimeoutMillis.coerceAtLeast(350L)
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val pointer = down.id
            // Up to the slop or hold timeout: which way this is heading, scrub, or tap.
            var start: PointerInputChange? = null
            var vertical = false
            var enteredScrub = false
            val canScrubNow = currentDurationMs > 0L && !currentLocked &&
                (currentOnScrubEnd != null || currentOnScrubSeek != null)

            if (canScrubNow) {
                var aborted = false
                val movedBeforeTimeout: PointerInputChange? = withTimeoutOrNull<PointerInputChange?>(holdTimeoutMs) {
                    var found: PointerInputChange? = null
                    while (found == null) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == pointer }
                        if (change == null || !change.pressed || change.isConsumed) {
                            aborted = true
                            break
                        }
                        val moved = change.position - down.position
                        if (moved.getDistance() >= viewConfiguration.touchSlop) {
                            found = change
                        }
                    }
                    found
                }
                if (aborted) return@awaitEachGesture
                if (movedBeforeTimeout != null) {
                    val moved = movedBeforeTimeout.position - down.position
                    vertical = moved.y < 0f && abs(moved.x) < -moved.y * PULL_BIAS
                    if (!vertical && abs(moved.x) < abs(moved.y)) return@awaitEachGesture
                    if (vertical && currentPull == null) return@awaitEachGesture
                    movedBeforeTimeout.consume()
                    start = movedBeforeTimeout
                } else {
                    enteredScrub = true
                }
            } else {
                while (start == null) {
                    val change = awaitPointerEvent().changes.firstOrNull { it.id == pointer }
                        ?: return@awaitEachGesture
                    // Lifted inside the slop is a tap, and the tap is the clickable's.
                    if (!change.pressed || change.isConsumed) return@awaitEachGesture
                    val moved = change.position - down.position
                    if (moved.getDistance() < viewConfiguration.touchSlop) continue
                    vertical = moved.y < 0f && abs(moved.x) < -moved.y * PULL_BIAS
                    // Downwards has nothing to do on the bar, and nor has a pull
                    // with no player to pull: left as they always were, a drag that
                    // ends inside the bar still counts as its tap.
                    if (!vertical && abs(moved.x) < abs(moved.y)) return@awaitEachGesture
                    if (vertical && currentPull == null) return@awaitEachGesture
                    change.consume()
                    start = change
                }
            }

            if (enteredScrub) {
                val activeDuration = currentDurationMs.coerceAtLeast(1L)
                val startFraction = (currentPosProvider().toFloat() / activeDuration.toFloat()).coerceIn(0f, 1f)
                var currentFraction = startFraction
                var totalDragX = 0f
                var lastSeekNanos = 0L
                var lastDispatchedMs = -1L

                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                haptics.play(Haptic.Select)
                currentOnScrubStart?.invoke()
                currentOnScrubMove?.invoke(currentFraction)

                var completed = false
                try {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointer } ?: break
                        val deltaX = change.positionChange().x
                        change.consume()
                        if (!change.pressed) {
                            completed = true
                            break
                        }
                        if (deltaX != 0f) {
                            totalDragX += deltaX
                            val widthPx = size.width.coerceAtLeast(1).toFloat()
                            currentFraction = (startFraction + totalDragX / widthPx).coerceIn(0f, 1f)
                            currentOnScrubMove?.invoke(currentFraction)

                            val targetMs = (currentFraction * activeDuration).roundToLong().coerceIn(0L, activeDuration)
                            val now = System.nanoTime()
                            if (now - lastSeekNanos >= 100_000_000L && abs(targetMs - lastDispatchedMs) >= 250L) {
                                lastSeekNanos = now
                                lastDispatchedMs = targetMs
                                currentOnScrubSeek?.invoke(targetMs)
                            }
                        }
                    }
                    if (completed) {
                        val finalMs = (currentFraction * activeDuration).roundToLong().coerceIn(0L, activeDuration)
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        haptics.play(Haptic.Tick)
                        currentOnScrubEnd?.invoke(finalMs)
                    }
                } finally {
                    if (!completed) currentOnScrubCancel?.invoke()
                }
                return@awaitEachGesture
            }

            val slopped = start ?: return@awaitEachGesture

            if (!vertical) {
                if (!currentEnableSwipe) return@awaitEachGesture
                var dragged = slopped.position.x - down.position.x
                var thresholdTickFired = false
                currentOnDrag?.invoke(dragged)
                val finished = drag(pointer) { change ->
                    val delta = change.positionChange().x
                    dragged += delta
                    change.consume()
                    currentOnDrag?.invoke(dragged)
                    if (!thresholdTickFired && abs(dragged) >= trackThreshold) {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        haptics.play(Haptic.Tick)
                        thresholdTickFired = true
                    } else if (thresholdTickFired && abs(dragged) < trackThreshold) {
                        thresholdTickFired = false
                    }
                }
                currentOnDragEnd?.invoke()
                if (!finished) return@awaitEachGesture
                val crossed = dragged <= -trackThreshold || dragged >= trackThreshold
                when {
                    currentLocked -> if (crossed) currentOnBlocked()
                    dragged <= -trackThreshold -> currentOnNext()
                    dragged >= trackThreshold -> currentOnPrevious()
                }
                return@awaitEachGesture
            }

            val target = currentPull ?: return@awaitEachGesture
            settle[0]?.cancel()
            val tracker = VelocityTracker()
            tracker.addPointerInputChange(down)
            tracker.addPointerInputChange(slopped)
            // The player gets all of it from the touch down, slop included, so
            // it starts out level with the finger. The bar starts from here,
            // from nothing: lifted by the slop as well, it jumped a few pixels
            // in one frame before it began to follow.
            target.drag(slopped.position.y - down.position.y)
            var pulled = 0f
            val finished = drag(pointer) { change ->
                val delta = change.positionChange().y
                change.consume()
                tracker.addPointerInputChange(change)
                pulled -= delta
                target.drag(delta)
                lift.floatValue = pullLift(pulled, liftEase)
            }
            target.release(
                if (finished) tracker.calculateVelocity().y.coerceIn(-maxVelocity, maxVelocity) else 0f,
            )
            // Let go, the bar springs home: under the player on its way up, or
            // in full view if the pull fell short and the player went back.
            val from = lift.floatValue
            settle[0] = scope.launch {
                animate(
                    initialValue = from,
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = 0.65f,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                ) { value, _ -> lift.floatValue = value }
            }
        }
    }.graphicsLayer { translationY = -lift.floatValue }
}

/**
 * The mini player's cover as one end of the player opening and closing — see
 * [PlayerDock]. Shared by both mini-player materials.
 *
 * Reports where it is on being placed, and nothing more: the coordinates are
 * the same object for as long as the cover is on screen, and the player turns
 * them into a position only on the frames it is actually flying to them, so a
 * bar folding and unfolding on every scroll pays nothing for this.
 *
 * Stands aside while the player's own artwork is on its way to or from here.
 * Read at draw, off the sheet's offset — a layer property per frame of the
 * sheet's travel, never a recomposition of the bar.
 */
@Composable
internal fun Modifier.playerDockArt(dock: PlayerDock?, corner: Dp): Modifier {
    if (dock == null) return this
    val placed = remember { arrayOfNulls<LayoutCoordinates>(1) }
    DisposableEffect(dock) {
        onDispose { placed[0]?.let(dock::releaseMiniArt) }
    }
    return this
        .onPlaced { coordinates ->
            placed[0] = coordinates
            dock.reportMiniArt(coordinates, corner)
        }
        .graphicsLayer { alpha = if (dock.coversMiniArt) 0f else 1f }
}

/**
 * Reports where the mini player's pill stands so the player's panel grows out
 * of it and shrinks back into it — see [PlayerDock.panelBounds]. Placed ahead
 * of the pull lift on purpose, so it reports the pill at rest rather than the
 * bar rising under the finger.
 *
 * Ported from Zenith (rezapranata089-bit/Zenith, GPL-3.0).
 */
@Composable
internal fun Modifier.playerDockPill(dock: PlayerDock?): Modifier {
    if (dock == null) return this
    val placed = remember { arrayOfNulls<LayoutCoordinates>(1) }
    DisposableEffect(dock) {
        onDispose { placed[0]?.let(dock::releaseMiniPill) }
    }
    return this
        .onPlaced { coordinates ->
            placed[0] = coordinates
            dock.reportMiniPill(coordinates)
        }
        // The player's panel is standing where this pill is: it is one surface
        // with it, so the pill stands aside rather than show under it.
        .graphicsLayer { alpha = if (dock.panelShown) 0f else 1f }
}

/**
 * The mini player's title and artist as the other end of the player's own
 * credits — see [PlayerDock].
 *
 * Reports where the text is and how big its title is, so the full player's
 * title can fly into the bar and land on it rather than beside it.
 */
@Composable
internal fun Modifier.playerDockText(dock: PlayerDock?, titleSizePx: Float): Modifier {
    if (dock == null) return this
    val placed = remember { arrayOfNulls<LayoutCoordinates>(1) }
    DisposableEffect(dock) {
        onDispose { placed[0]?.let(dock::releaseMiniText) }
    }
    return this.onPlaced { coordinates ->
        placed[0] = coordinates
        dock.reportMiniText(coordinates, titleSizePx)
    }
}

/** Frosted mini player that rides just above the floating tab bar. */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    isLoading: Boolean,
    hazeState: HazeState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onExpand: () -> Unit,
    playbackPosition: PlaybackPosition? = null,
    durationMs: Long = 0L,
    isLive: Boolean = song.isLive,
    onSeek: ((Long) -> Unit)? = null,
    onPause: (() -> Unit)? = null,
    onResume: (() -> Unit)? = null,
    /** @see com.music.bitchord.data.listentogether.ListenTogether.State.controlsLocked */
    controlsLocked: Boolean = false,
    onBlockedControl: () -> Unit = {},
    /** Where the player's artwork lands when it closes into this bar. */
    dock: PlayerDock? = null,
    /** Dragging the bar up into the player. */
    pull: MiniPlayerPull? = null,
    modifier: Modifier = Modifier,
) {
    val designStyle by AppSettings.appDesignStyle.collectAsStateWithLifecycle()
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val haptics = rememberHaptics()
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val horizontalDragOffset = remember { Animatable(0f) }

    val effectiveIsLive = isLive || song.isLive
    val effectiveDurationMs = if (effectiveIsLive) 0L else durationMs
    var scrubFraction by remember(song.videoId) { mutableStateOf<Float?>(null) }
    var wasPlayingBeforeScrub by remember(song.videoId) { mutableStateOf(false) }
    var originalPositionBeforeScrub by remember(song.videoId) { mutableLongStateOf(0L) }
    var suppressExpandUntilNanos by remember { mutableLongStateOf(0L) }
    val currentDurationMs by rememberUpdatedState(effectiveDurationMs)
    val currentPlaybackPosition by rememberUpdatedState(playbackPosition)
    val currentOnSeek by rememberUpdatedState(onSeek)
    val currentIsPlaying by rememberUpdatedState(isPlaying)
    val currentOnPause by rememberUpdatedState(onPause)
    val currentOnResume by rememberUpdatedState(onResume)

    // Sample artwork accent color for dynamic perimeter progress glow
    val artworkPalette = rememberArtworkPalette(song.thumbnailUrl, artPx = ROW_ART_PX)
    val glowColor = if (artworkPalette.accent != Color.Unspecified) {
        artworkPalette.accent
    } else {
        MaterialTheme.colorScheme.primary
    }

    val pathMeasure = remember { PathMeasure() }
    val fullBorderPath = remember { Path() }
    val progressSegmentPath = remember { Path() }

    // percent rather than a dp figure, so the corner stays exactly half the
    // height if the row's contents ever change it — which is what keeps a pill
    // a pill instead of a rounded rectangle. Same idiom as [FloatingBottomBar]
    // directly below it, so the two shapes are the same family.
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier = modifier
            .padding(horizontal = BAR_GUTTER)
            // Ahead of the surface, so a pull lifts the whole of it, frost and all.
            .playerDockPill(dock)
            .miniPlayerGestures(
                pull = pull,
                onNext = {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    haptics.play(Haptic.SkipNext)
                    onNext()
                },
                onPrevious = {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    haptics.play(Haptic.SkipPrevious)
                    onPrevious()
                },
                locked = controlsLocked,
                onBlocked = onBlockedControl,
                enableSwipeToSkip = true,
                onHorizontalDrag = { offset ->
                    scope.launch { horizontalDragOffset.snapTo(offset) }
                },
                onHorizontalDragEnd = {
                    scope.launch {
                        horizontalDragOffset.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                        )
                    }
                },
                durationMs = effectiveDurationMs,
                currentPositionMs = { currentPlaybackPosition?.positionMs ?: 0L },
                onScrubStart = {
                    suppressExpandUntilNanos = Long.MAX_VALUE
                    wasPlayingBeforeScrub = currentIsPlaying
                    val currentPos = currentPlaybackPosition?.positionMs ?: 0L
                    originalPositionBeforeScrub = currentPos
                    if (currentIsPlaying) {
                        currentOnPause?.invoke()
                    }
                    scrubFraction = if (currentDurationMs > 0L) {
                        (currentPos.toFloat() / currentDurationMs.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                },
                onScrubMove = { fraction -> scrubFraction = fraction },
                onScrubSeek = null,
                onScrubEnd = { ms ->
                    currentOnSeek?.invoke(ms)
                    scrubFraction = null
                    suppressExpandUntilNanos = System.nanoTime() + 250_000_000L
                    if (wasPlayingBeforeScrub) {
                        currentOnResume?.invoke()
                    }
                    wasPlayingBeforeScrub = false
                },
                onScrubCancel = {
                    if (scrubFraction != null) {
                        scrubFraction = null
                        suppressExpandUntilNanos = System.nanoTime() + 250_000_000L
                        if (wasPlayingBeforeScrub) {
                            currentOnSeek?.invoke(originalPositionBeforeScrub)
                            currentOnResume?.invoke()
                        }
                        wasPlayingBeforeScrub = false
                    }
                },
            )
            .clip(shape)
            .then(
                if (reduceDynamicBlur) {
                    Modifier.background(MaterialTheme.colorScheme.surface)
                } else {
                    Modifier.optimizedHazeEffect(
                        state = hazeState,
                        style = HazeMaterials.thin(MaterialTheme.colorScheme.surface),
                    )
                },
            )
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), shape)
            .drawWithContent {
                drawContent()
                if (effectiveDurationMs > 0L) {
                    val activeScrub = scrubFraction
                    val currentMs = playbackPosition?.positionMs ?: 0L
                    val progress = activeScrub ?: (currentMs.toFloat() / effectiveDurationMs.toFloat()).coerceIn(0f, 1f)
                    if (progress > 0.0005f) {
                        val strokeInset = 1.dp.toPx()
                        val h = size.height - 2 * strokeInset
                        val r = h / 2f
                        fullBorderPath.reset()
                        fullBorderPath.addRoundRect(
                            RoundRect(
                                left = strokeInset,
                                top = strokeInset,
                                right = size.width - strokeInset,
                                bottom = size.height - strokeInset,
                                cornerRadius = CornerRadius(r, r),
                            ),
                        )
                        pathMeasure.setPath(fullBorderPath, forceClosed = true)
                        val totalLen = pathMeasure.length
                        val segmentLen = progress * totalLen
                        val scrubbing = activeScrub != null

                        progressSegmentPath.reset()
                        if (segmentLen > 0f) {
                            pathMeasure.getSegment(0f, segmentLen, progressSegmentPath, startWithMoveTo = true)
                            // 1. Subtle ambient outer glow
                            drawPath(
                                path = progressSegmentPath,
                                color = glowColor.copy(alpha = if (scrubbing) 0.50f else 0.35f),
                                style = Stroke(width = (if (scrubbing) 5.dp else 4.dp).toPx(), cap = StrokeCap.Round),
                            )
                            // 2. Focused vibrant glow
                            drawPath(
                                path = progressSegmentPath,
                                color = glowColor.copy(alpha = if (scrubbing) 0.90f else 0.75f),
                                style = Stroke(width = (if (scrubbing) 2.8.dp else 2.2.dp).toPx(), cap = StrokeCap.Round),
                            )
                            // 3. Crisp luminous core
                            drawPath(
                                path = progressSegmentPath,
                                color = Color.White.copy(alpha = if (scrubbing) 1f else 0.90f),
                                style = Stroke(width = (if (scrubbing) 1.5.dp else 1.2.dp).toPx(), cap = StrokeCap.Round),
                            )
                        }
                    }
                }
            }
            // Deliberately silent: the whole bar is the target, so it catches
            // stray taps meant for the page behind it, and the sheet rising is
            // its own confirmation. The glyphs on it still buzz.
            .clickable {
                if (scrubFraction == null && System.nanoTime() >= suppressExpandUntilNanos) {
                    onExpand()
                }
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = ROW_PADDING_HORIZONTAL,
                    vertical = ROW_PADDING_VERTICAL,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = rememberRemoteArtworkUrl(song)?.artworkAt(ROW_ART_PX),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer {
                        translationX = horizontalDragOffset.value
                    }
                    .playerDockArt(dock, ART_CORNER)
                    .clip(RoundedCornerShape(ART_CORNER))
                    .thumbnailBorder(RoundedCornerShape(ART_CORNER))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Spacer(Modifier.width(10.dp))
            val titleStyle = MaterialTheme.typography.titleMedium
            val titlePx = with(LocalDensity.current) { titleStyle.fontSize.toPx() }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer {
                        translationX = horizontalDragOffset.value * 0.35f
                    }
                    .playerDockText(dock, titlePx),
            ) {
                ExplicitSongTitle(
                    song = song,
                    style = titleStyle,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                val activeScrub = scrubFraction
                if (activeScrub != null && effectiveDurationMs > 0L) {
                    val previewMs = (activeScrub * effectiveDurationMs).roundToLong().coerceIn(0L, effectiveDurationMs)
                    Text(
                        text = "${formatMiniScrubTime(previewMs)} / ${formatMiniScrubTime(effectiveDurationMs)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = glowColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else if (effectiveIsLive) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(Modifier.width(6.dp))
                        LiveBadge(compact = true)
                    }
                } else {
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (isLoading) {
                Box(Modifier.size(GLYPH_SLOT), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onBackground,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(SPINNER_SIZE),
                    )
                }
            } else {
                IconButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        haptics.play(if (isPlaying) Haptic.Pause else Haptic.Resume)
                        onPlayPause()
                    },
                    modifier = Modifier.size(GLYPH_SLOT),
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(GLYPH_SIZE),
                    )
                }
            }
            Spacer(Modifier.width(TRANSPORT_GAP))
            // Faded and inert rather than removed while the host holds the
            // controls, so the bar keeps its shape — see [controlsLocked].
            IconButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    haptics.play(Haptic.SkipNext)
                    onNext()
                },
                enabled = !controlsLocked,
                modifier = Modifier.size(GLYPH_SLOT),
            ) {
                MiniPlayerNextGlyph(
                    tint = MaterialTheme.colorScheme.onBackground
                        .copy(alpha = if (controlsLocked) 0.3f else 1f),
                )
            }
        }
    }
}

/**
 * The full player's skip-forward glyph, at the mini player's scale.
 *
 * Sized against the play glyph beside it the way the full player sizes it
 * against its own play button — about half as wide again and two thirds as
 * tall — and squashed by the same 0.85, so the two players' next buttons
 * read as one glyph at two sizes. Shared with the glass bar's docked player.
 */
@Composable
internal fun MiniPlayerNextGlyph(tint: Color) {
    Icon(
        painter = painterResource(SharedRes.drawable.ic_player_next),
        contentDescription = stringResource(R.string.widget_next),
        tint = tint,
        modifier = Modifier
            .size(NEXT_GLYPH_SIZE)
            .graphicsLayer { scaleY = 0.85f },
    )
}

internal fun formatMiniScrubTime(ms: Long): String {
    val totalSeconds = ms.coerceAtLeast(0L) / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    val secStr = seconds.toString().padStart(2, '0')
    return if (hours > 0L) {
        "$hours:${minutes.toString().padStart(2, '0')}:$secStr"
    } else {
        "$minutes:$secStr"
    }
}

private val NEXT_GLYPH_SIZE = 25.dp
