/*
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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Where a set of live colours came from.
 *
 * Three sources, three answers, and the player asks each of them separately
 * because a picture is not the same thing as the sleeve that replaced it.
 */
internal enum class LiveColorSource {
    /**
     * The still sleeve, read once when it arrives.
     *
     * The sleeve's own answer, and the right one for every surface the sleeve
     * is actually on.
     */
    STATIC_ART,

    /**
     * The album art *as it is being drawn* — a clip playing over it.
     *
     * A moving picture and a still one are different pictures. A clip graded
     * black over a white sleeve is not a white picture with a black window in
     * it; it is a black picture, and the colours have to be read off the
     * clip rather than off the sleeve it is standing in for.
     */
    LIVE_ART,

    /**
     * The Spotify Canvas, read off its own frames as it plays.
     *
     * Its own source because its own rule: the Canvas owns the whole screen
     * when it is on, so nothing of the sleeve is left to be read.
     */
    CANVAS,
}

/**
 * A set of colours read off a picture, and where that picture came from.
 */
@Immutable
internal data class LivePalette(
    /** The ink, the alphas — see [NowPlayingPalette]. */
    val palette: NowPlayingPalette,
    val source: LiveColorSource,
)

/**
 * The colours of a moving picture, read as it moves.
 *
 * The same rules as the sleeve's own palette — a legible tint of the picture's
 * secondary colour, darkened or lightened by how bright the picture is — but
 * read from a frame rather than from a cover, and read *quickly*, because
 * this runs several times a second while a clip is playing.
 *
 * The picture it reads is the one on screen, which is the whole point: the
 * controls must belong to what is behind them, and what is behind them is a
 * video.
 */
internal fun computeLivePalette(frame: ImageBitmap, source: LiveColorSource): LivePalette =
    LivePalette(computeNowPlayingPalette(frame.liveSamples()), source)

/**
 * The colours of a moving picture, kept current as new frames arrive.
 *
 * The sampling happens off the main thread: a frame is read pixel by pixel,
 * and doing that on the thread that draws the screen is a frame of jank every
 * time the picture changes.
 *
 * Read on a clock of its own rather than once per frame, and that is the whole
 * point of this being separate from the backdrop's reads.
 *
 * The answer is handed back as a [MutableState] rather than as a value, and
 * the frames arrive as a [Flow] rather than as a parameter, for the same
 * reason: the palette changes while the player is open, and a value read in
 * the player's own composition would recompose the whole screen every time
 * the ink moved. The state is read by the ink's own scope instead — see
 * `rememberPlayerInk` — so a change of colour costs the texts and glyphs that
 * wear it and nothing else.
 */
@Composable
internal fun rememberLivePalette(
    frames: Flow<ImageBitmap?>,
    source: LiveColorSource,
): MutableState<LivePalette?> {
    val live = remember(source) { mutableStateOf<LivePalette?>(null) }
    val liveBlur by PlayerSettings.liveBlur.collectAsStateWithLifecycle()
    LaunchedEffect(frames, source, liveBlur) {
        var lastReadAt = 0L
        val gapMs = if (liveBlur) PALETTE_MIN_GAP_LIVE_MS else PALETTE_MIN_GAP_MS
        frames.collect { current ->
            if (current == null) return@collect
            // The first frame is taken at once, so the ink belongs to the
            // picture from the player's first drawn frame rather than a fifth
            // of a second into it.
            val now = System.nanoTime()
            if (lastReadAt != 0L && now - lastReadAt < gapMs * 1_000_000L) return@collect
            lastReadAt = now
            val next = withContext(Dispatchers.Default) {
                computeLivePalette(current, source)
            }
            // Published only when the ink has actually moved. Reading the
            // picture thirty times a second is cheap; *changing* the ink
            // thirty times a second is not.
            val previous = live.value
            if (previous != null && !inkMoved(previous.palette, next.palette)) return@collect
            live.value = next
        }
    }
    return live
}

/**
 * Whether the ink has moved far enough to be worth a redraw.
 */
private fun inkMoved(previous: NowPlayingPalette, next: NowPlayingPalette): Boolean {
    val a = previous.fg
    val b = next.fg
    if (
        kotlin.math.abs(a.red - b.red) > INK_MOVE_EPSILON ||
        kotlin.math.abs(a.green - b.green) > INK_MOVE_EPSILON ||
        kotlin.math.abs(a.blue - b.blue) > INK_MOVE_EPSILON ||
        kotlin.math.abs(a.alpha - b.alpha) > INK_MOVE_EPSILON
    ) {
        return true
    }
    val pa = previous.alphas
    val na = next.alphas
    return kotlin.math.abs(pa.primary - na.primary) > ALPHA_MOVE_EPSILON ||
        kotlin.math.abs(pa.secondary - na.secondary) > ALPHA_MOVE_EPSILON ||
        kotlin.math.abs(pa.tertiary - na.tertiary) > ALPHA_MOVE_EPSILON ||
        kotlin.math.abs(pa.track - na.track) > ALPHA_MOVE_EPSILON ||
        kotlin.math.abs(pa.buttonFill - na.buttonFill) > ALPHA_MOVE_EPSILON
}

/**
 * A bounded sample of a picture's pixels, in reading order.
 */
internal fun ImageBitmap.liveSamples(limit: Int = LIVE_SAMPLES): List<Int> {
    if (width <= 0 || height <= 0) return emptyList()
    val pixels = IntArray(width * height)
    readPixels(pixels, startX = 0, startY = 0, width = width, height = height)
    val step = (pixels.size / limit).coerceAtLeast(1)
    return buildList(minOf(pixels.size, limit)) {
        var index = 0
        while (index < pixels.size) {
            add(pixels[index])
            index += step
        }
    }
}

/** How many pixels are read off a moving picture. */
private const val LIVE_SAMPLES = 1_024

/** How long between reads of a moving picture for the ink, with live blur off. */
private const val PALETTE_MIN_GAP_MS = 400L

/** The same read, with live blur on. */
private const val PALETTE_MIN_GAP_LIVE_MS = 250L

/** How far the ink has to move before it is published. See [inkMoved]. */
private const val INK_MOVE_EPSILON = 0.01f

/** How far an opacity has to move before it is published. See [inkMoved]. */
private const val ALPHA_MOVE_EPSILON = 0.005f

