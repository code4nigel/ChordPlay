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
package com.music.bitchord.ui.player

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.music.bitchord.data.lyrics.LyricLine
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.settings.LastPlayerScreen
import com.music.bitchord.playback.PlaybackPosition
import kotlinx.coroutines.delay

/**
 * Which surface the portrait player is showing — the sleeve, the lyrics or the
 * queue — and the state that travels with the two panels.
 */
@Stable
internal class PlayerPanelState(restored: LastPlayerScreen) {
    var queueOpen by mutableStateOf(restored == LastPlayerScreen.QUEUE)
    var lyricsOpen by mutableStateOf(restored == LastPlayerScreen.LYRICS)

    val queueSlide: MutableFloatState = mutableFloatStateOf(0f)

    var lyricsScrolling by mutableStateOf(false)
    var queueScrolling by mutableStateOf(false)

    var lyricsControlsOpen by mutableStateOf(restored == LastPlayerScreen.LYRICS)
    var queueControlsOpen by mutableStateOf(true)

    var queueDragging by mutableStateOf(false)
    var queueReleased by mutableIntStateOf(0)

    val openLyrics: () -> Unit = {
        lyricsControlsOpen = true
        lyricsOpen = true
        queueOpen = false
    }
    val closeLyrics: () -> Unit = {
        lyricsControlsOpen = false
        lyricsOpen = false
    }
    val toggleLyrics: () -> Unit = {
        if (lyricsOpen) closeLyrics() else openLyrics()
    }
    val toggleQueue: () -> Unit = {
        val opening = !queueOpen
        if (opening && lyricsOpen) queueSlide.floatValue = 1f
        if (opening) queueControlsOpen = true
        queueOpen = opening
        if (opening) closeLyrics()
    }
    val closePanels: () -> Unit = {
        queueOpen = false
        closeLyrics()
    }
}

/**
 * How far the portrait sleeve has collapsed into the header, 0 to 1, and the
 * handful of thresholds composition is allowed to ask of it.
 */
@Stable
internal class PlayerCollapse(panels: PlayerPanelState, animated: State<Float>) {
    val p: () -> Float = {
        val queueOwnsCollapse = !panels.lyricsOpen &&
            (panels.queueOpen || panels.queueDragging || panels.queueSlide.floatValue > 0.001f)
        if (queueOwnsCollapse) panels.queueSlide.floatValue else animated.value
    }
    val started by derivedStateOf { p() > 0f }
    val atRest by derivedStateOf { p() == 0f }
    val pastSettling by derivedStateOf { p() >= 0.01f }
    val pastHalf by derivedStateOf { p() >= 0.5f }
    val almostDone by derivedStateOf { p() >= 0.999f }
    val done by derivedStateOf { p() >= 1f }
    val queueShowing by derivedStateOf { panels.queueSlide.floatValue > 0.01f }
}

/**
 * The full lyrics panel of the portrait player, with its translation,
 * romanisation and share controls.
 */
@Composable
internal fun BoxScope.PortraitLyricsPane(
    visible: Boolean,
    fade: () -> Float,
    panels: PlayerPanelState,
    lyrics: List<LyricLine>?,
    lyricsTranslation: LyricsTranslationUi,
    song: Song,
    lyricsPlayhead: LyricPlayhead,
    lyricsUnavailable: Boolean,
    isPlaying: Boolean,
    position: PlaybackPosition,
    onSeekToLine: (Long) -> Unit,
    canPick: Boolean,
    lyricPicker: LyricsPicker,
) {
    LyricsTranslationMotion(
        trigger = lyricsTranslation.transition,
        reduceMotion = lyricsTranslation.reduceMotion,
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 60.dp)
            .offset {
                if (visible) IntOffset.Zero else IntOffset(100_000, 0)
            }
            .graphicsLayer { alpha = if (visible) fade() else 0f },
    ) { particleProgress ->
        LyricsPanel(
            lines = lyrics.orEmpty(),
            subLines = lyricsTranslation.subLines,
            trackKey = song.videoId,
            playhead = lyricsPlayhead,
            looking = !lyricsUnavailable,
            isPlaying = isPlaying && position.advancing,
            active = visible,
            onSeekToLine = onSeekToLine,
            controlsOpen = panels.lyricsControlsOpen,
            onRevealControls = { panels.lyricsControlsOpen = true },
            onHideControls = { panels.lyricsControlsOpen = false },
            translationProgress = particleProgress,
            onScrollingChange = { panels.lyricsScrolling = it },
            canPick = canPick,
            picking = lyricPicker.picking,
            picked = lyricPicker.picks,
            onPickLine = lyricPicker.pick,
            onTogglePick = lyricPicker.toggle,
            modifier = Modifier.fillMaxSize(),
        )
    }

    if (panels.lyricsOpen && lyricPicker.picking) {
        LyricsPickBar(
            shareEnabled = lyricPicker.picks.isNotEmpty() && !lyricPicker.overBudget,
            onCancel = lyricPicker.cancel,
            onShare = lyricPicker.share,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/**
 * The queue panel of the portrait player.
 */
@Composable
internal fun PortraitQueuePane(
    visible: Boolean,
    fade: () -> Float,
    panels: PlayerPanelState,
    queue: List<Song>,
    queueIndex: Int,
    autoplayEnabled: Boolean,
    controlsLocked: Boolean,
    onJumpTo: (Int) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onMoveInQueue: (Int, Int) -> Unit,
    onClearQueue: () -> Unit,
    onQueueDragActiveChange: (Boolean) -> Unit,
    onPlayNext: (Song) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 60.dp)
            .offset {
                if (visible) IntOffset.Zero else IntOffset(100_000, 0)
            }
            .graphicsLayer {
                alpha = if (!visible) {
                    0f
                } else if (panels.queueDragging) {
                    ((panels.queueSlide.floatValue - 0.45f) / 0.55f).coerceIn(0f, 1f)
                } else {
                    fade()
                }
                translationY = if (visible) {
                    (1f - panels.queueSlide.floatValue) * 26.dp.toPx()
                } else {
                    0f
                }
            },
    ) {
        InlineQueue(
            queue = queue,
            currentIndex = queueIndex,
            autoplayEnabled = autoplayEnabled,
            controlsLocked = controlsLocked,
            onJumpTo = onJumpTo,
            onRemove = onRemoveFromQueue,
            onMove = onMoveInQueue,
            onClear = onClearQueue,
            onScrollingChange = { panels.queueScrolling = it },
            onDragActiveChange = onQueueDragActiveChange,
            collapsePlayerOnScroll = true,
            onRevealPlayer = { panels.queueControlsOpen = true },
            onHidePlayer = { panels.queueControlsOpen = false },
            onPlayNext = onPlayNext,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * The lyrics panel's half player stands down after [idleMs] left alone, and
 * never while a finger is on the scrubber or the volume bar.
 */
@Composable
internal fun LyricsControlsIdleEffect(
    panels: PlayerPanelState,
    scrub: PlayerScrub,
    volume: PlayerVolume,
    idleMs: Long = 5_000L,
) {
    LaunchedEffect(panels.lyricsOpen, panels.lyricsControlsOpen, scrub.scrubbing, volume.dragging) {
        if (panels.lyricsOpen && panels.lyricsControlsOpen && !scrub.scrubbing && !volume.dragging) {
            delay(idleMs)
            panels.lyricsControlsOpen = false
        }
    }
}

