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

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize

/**
 * Where the player and its mini bar agree on where they meet on screen.
 *
 * Opening and closing the player is one movement on screen: the sheet's
 * artwork travels between its own place and the mini player's cover while the
 * rest of the player fades — see the sleeve in [NowPlayingScreen]. The player
 * and the mini player are one panel in the app's own window (see the app's
 * `PlayerPanel`), grown from the pill's rect to the whole window; the two ends
 * of the movement are still each reported by the side that owns it, and met
 * in screen coordinates.
 *
 * The panel half of this — the pill's rect, [panelBounds], [panelCornerPx],
 * [panelFill] — is ported from Zenith (rezapranata089-bit/Zenith, GPL-3.0),
 * a sibling fork of BitChord.
 *
 * Read in layout and draw only. Nothing in it is worth a recomposition: a
 * frame of the sheet's travel moves the artwork and re-tints a layer, and that
 * frame belongs to drawing, not to laying the hierarchy out again.
 */
@Stable
class PlayerDock {
    /**
     * Live screen-pixel offset of the sheet (`0f` open, [sheetTravel] at the
     * mini player, `NaN` between motions), polled inside graphicsLayer/layout
     * blocks.
     */
    var sheetOffset: (() -> Float)? = null

    /** Full travel distance in pixels (usually the screen height). */
    var sheetTravel: Float by mutableFloatStateOf(0f)

    /**
     * Whether the player's own screen is mounted and taking part in the trip.
     *
     * While it is false — during a sheet's closing frames, after its content
     * has left — the dock sits on the last offset it was holding rather than
     * moving with the window underneath.
     */
    var attached: Boolean by mutableStateOf(false)

    /**
     * The mini player's cover artwork, as last laid out.
     *
     * Held by reference rather than resolved into a Rect inside onPlaced:
     * scrolling folds the glass bar and lifts the row without always firing
     * a new placement on the Image inside it, so reading position on demand
     * catches whatever transform is above the cover right now.
     */
    private var miniArt: LayoutCoordinates? = null
    private var miniArtCorner: Dp = 8.dp

    /** Reported by the mini player's cover whenever it is placed. */
    fun reportMiniArt(coordinates: LayoutCoordinates, corner: Dp) {
        miniArt = coordinates
        miniArtCorner = corner
    }

    /** From the mini cover leaving composition, so a stale rect is never flown to. */
    fun releaseMiniArt(coordinates: LayoutCoordinates) {
        if (miniArt === coordinates) miniArt = null
    }

    internal fun hasMiniArt(): Boolean = miniArt?.isAttached == true

    /**
     * The mini player's title and artist, as last laid out.
     *
     * The other end of the credits' journey. The player's own title leaves
     * the screen when the sheet closes and arrives in the bar; the two are
     * met in screen coordinates, the same way the cover is.
     */
    private var miniText: LayoutCoordinates? = null
    private var miniTextTitlePx: Float = 0f

    /**
     * Reported by the mini player's text whenever it is placed.
     *
     * [titleSizePx] is the size of the bar's title in pixels, so the title
     * that flies there can be scaled to meet it rather than to arrive beside
     * it.
     */
    fun reportMiniText(coordinates: LayoutCoordinates, titleSizePx: Float) {
        miniText = coordinates
        miniTextTitlePx = titleSizePx
    }

    /** From the text leaving composition, so a stale rect is never flown to. */
    fun releaseMiniText(coordinates: LayoutCoordinates) {
        if (miniText === coordinates) {
            miniText = null
            miniTextTitlePx = 0f
        }
    }

    internal fun hasMiniText(): Boolean = miniText?.isAttached == true

    /** The bar's title and artist on screen, for the credits to land on. */
    internal fun miniTextOnScreen(): Rect? {
        val coordinates = miniText?.takeIf { it.isAttached } ?: return null
        val size = coordinates.size.toSize()
        return Rect(
            coordinates.localToScreen(Offset.Zero),
            coordinates.localToScreen(Offset(size.width, size.height)),
        )
    }

    /** The bar's title size in pixels — see [reportMiniText]. */
    internal fun miniTextTitlePx(): Float = miniTextTitlePx

    /**
     * Both corners mapped through every transform above the cover, not its
     * position plus its laid-out size: the glass bar draws its player scaled,
     * and the size reported by layout is the unscaled size before that layer.
     */
    internal fun miniArtOnScreen(): Rect? {
        val coordinates = miniArt?.takeIf { it.isAttached } ?: return null
        val size = coordinates.size.toSize()
        return Rect(
            coordinates.localToScreen(Offset.Zero),
            coordinates.localToScreen(Offset(size.width, size.height)),
        )
    }

    internal fun miniArtCorner(): Dp = miniArtCorner

    // ---- The one panel: the mini player's pill and the full player ----
    // The pill's rect (at rest, without the pull lift) is where the player
    // starts and ends; the panel is a rounded rect that grows from that pill
    // to the whole window — see [panelBounds].
    private var miniPill: LayoutCoordinates? = null
    private var originW = -1
    private var originH = -1

    /** Where the panel's window starts on screen. */
    private var windowOrigin: Offset = Offset.Zero

    // Dozens of layout, graphics-layer and draw blocks ask for the pill every
    // frame of an open or close, and each ask walks the coordinate chain twice.
    // The pill does not move during the trip, so one answer serves the frame.
    private var pillCache: Rect? = null
    private var pillCacheNanos = 0L

    /** Reported by the mini player's pill whenever it is placed. */
    fun reportMiniPill(coordinates: LayoutCoordinates) {
        miniPill = coordinates
        pillCache = null
        pillCacheNanos = 0L
    }

    /** From the pill leaving composition, so a stale rect is never grown from. */
    fun releaseMiniPill(coordinates: LayoutCoordinates) {
        if (miniPill === coordinates) {
            miniPill = null
            pillCache = null
            pillCacheNanos = 0L
        }
    }

    /** The pill in the panel window's own pixels, or null with no pill on screen. */
    fun miniPillInWindow(): Rect? {
        val c = miniPill?.takeIf { it.isAttached } ?: return null
        val now = System.nanoTime()
        pillCache?.let { if (now - pillCacheNanos < PILL_CACHE_NANOS) return it }
        val size = c.size.toSize()
        return Rect(
            c.localToScreen(Offset.Zero) - windowOrigin,
            c.localToScreen(Offset(size.width, size.height)) - windowOrigin,
        ).also {
            pillCache = it
            pillCacheNanos = now
        }
    }

    /**
     * How far the player travels between fully open and docked on the pill:
     * the pill's top edge. Falls back to the whole window with no pill.
     */
    fun travel(): Float {
        val top = miniPillInWindow()?.top
        return if (top != null && top > 1f) top else sheetTravel
    }

    /**
     * True while the single panel stands where the mini player's pill is, so
     * the pill itself stands aside instead of showing as a second panel.
     */
    var panelShown by mutableStateOf(false)
        private set

    /**
     * The panel's plain fill while it grows out of the pill: taken from the
     * track's cover by the host, a near-black before that is known.
     */
    var panelColor by mutableStateOf(PANEL_FALLBACK_COLOR)

    /** From the panel's root, once placed: where its window starts on screen. */
    fun panelPlaced(coordinates: LayoutCoordinates) {
        val origin = coordinates.localToScreen(Offset.Zero)
        val size = coordinates.size
        if (origin != windowOrigin || originW != size.width || originH != size.height) {
            pillCache = null
            pillCacheNanos = 0L
        }
        windowOrigin = origin
        originW = size.width
        originH = size.height
    }

    /** The pill comes back. Called as the panel goes, not after it has. */
    fun releasePanel() {
        panelShown = false
    }

    private fun panelProgress(pill: Rect): Float =
        (1f - (offset() ?: 0f) / pill.top).coerceIn(0f, 1f)

    /**
     * The panel's rect this frame, in the window's own pixels. At the bottom
     * of its travel it is the pill's rect; fully open, the whole window. In
     * between its left and right close in on the pill, its top follows the
     * player's offset, and its bottom rises to meet the pill: one shape
     * changing size. With no pill to grow from it is the whole window,
     * sliding as one.
     */
    fun panelBounds(window: Size): Rect {
        val offset = offset() ?: 0f
        val pill = miniPillInWindow()
        if (pill == null || pill.top <= 1f) {
            return Rect(0f, offset, window.width, window.height + offset)
        }
        val progress = panelProgress(pill)
        // The width catches up faster than the rise, so the screen's corners
        // are covered well before halfway.
        val wide = panelWideEase(progress)
        val left = pill.left * (1f - wide)
        val right = window.width - (window.width - pill.right) * (1f - wide)
        val bottom = pill.bottom + (window.height - pill.bottom) * progress
        return Rect(left, offset, right.coerceAtLeast(left + 1f), bottom.coerceAtLeast(offset + 1f))
    }

    /** 0 on the pill (all glass), 1 once the plain panel has fully covered the glass. */
    fun panelFill(until: Float = PANEL_FILL_UNTIL): Float = (openFraction() / until).coerceIn(0f, 1f)

    /** The panel's corner radius this frame: the pill's, rounding off to square. */
    fun panelCornerPx(sheetCornerPx: Float = 0f): Float {
        val pill = miniPillInWindow() ?: return 0f
        if (pill.top <= 1f) return 0f
        val progress = panelProgress(pill)
        val pillCorner = pill.height / 2f * (1f - progress)
        // The top corners stay rounded while the panel rises, as a sheet's
        // do, and square off only as it covers the whole screen.
        val sheetCorner = sheetCornerPx *
            (progress / PANEL_WIDE_AT).coerceIn(0f, 1f) *
            (1f - ((progress - 0.9f) / 0.1f).coerceIn(0f, 1f))
        return maxOf(pillCorner, sheetCorner)
    }

    /** The sheet's offset this frame, or null with no sheet laid out. */
    fun offset(): Float? = sheetOffset?.invoke()?.takeUnless { it.isNaN() }

    /**
     * 1 with the player fully open, 0 down on the mini player. 0 too while
     * there is no sheet yet, which is where an opening player starts.
     */
    fun openFraction(): Float {
        val travel = travel()
        val offset = offset() ?: return 0f
        if (travel <= 0f) return 0f
        return (1f - offset / travel).coerceIn(0f, 1f)
    }

    /**
     * Whether the player is fully open **and has stopped moving**.
     *
     * The answer work needs before it may touch the screen: the player's own
     * heavy layouts — the lyrics list, the queue — must not be laid out in the
     * middle of the sheet's slide up, because a list laid out mid-slide is a
     * dropped frame in the middle of the slide.
     *
     * Derived, so a reader hears about it once — as the sheet arrives — rather
     * than on every frame of the travel.
     */
    val settled: Boolean get() = settledState.value

    private val settledState = derivedStateOf { openFraction() >= 1f }

    /**
     * Whether the mini player's own cover should stand aside, because the
     * player's is on its way to or from it. Not before the sheet has moved at
     * all — while the player is resting open, the cover isn't docked, and the
     * mini player underneath doesn't need to keep up or re-render.
     */
    val coversMiniArt: Boolean get() = coversMiniArtState.value

    private val coversMiniArtState = derivedStateOf { attached && (openFraction() > 0f || panelShown) }
}

/** How long one measurement of the mini pill stands, in nanoseconds: under a frame. */
private const val PILL_CACHE_NANOS = 8_000_000L

/** The share of the opening over which the glass gives way to the plain panel. */
private const val PANEL_FILL_UNTIL = 0.2f

/** How far open the panel is once it is the window's full width. */
private const val PANEL_WIDE_AT = 0.8f

/** The panel's fill before the cover's colour is known. */
val PANEL_FALLBACK_COLOR = Color(0xFF151517)

/** The panel's width curve: 0 on the pill, 1 once it is the window's width. */
internal fun panelWideEase(t: Float): Float {
    val x = (t / PANEL_WIDE_AT).coerceIn(0f, 1f)
    return 1f - (1f - x) * (1f - x)
}

/**
 * Leaves the page undrawn while the portrait player fully covers it: its own
 * background is opaque, so everything drawn under it (glass bars, blur, lists)
 * would be paid for every frame for nobody to see. Layout, composition and
 * input are untouched; only drawing stops.
 *
 * Put on the page, never on a parent of the player panel, or the panel would
 * be skipped with it. The flag is derived, so the draw is invalidated twice
 * per open or close — when it flips — and not on every frame of the travel.
 */
@Composable
fun Modifier.skipDrawWhenPlayerCovers(dock: PlayerDock?): Modifier {
    val covered = remember(dock) {
        derivedStateOf { dock != null && dock.attached && dock.openFraction() >= 1f }
    }
    return drawWithContent { if (!covered.value) drawContent() }
}

/**
 * Signature buttery ease-out curve for miniplayer and player transitions:
 * immediate quick takeoff followed by a long, dream-like deceleration glide.
 */
val DreamyEaseOutEasing = CubicBezierEasing(0.16f, 1.0f, 0.24f, 1.0f)

/** Smooth C1 Hermite curve used to blend crossfades without linear corners. */
fun smoothStep01(t: Float): Float {
    val c = t.coerceIn(0f, 1f)
    return c * c * (3f - 2f * c)
}

/** The [PlayerDock] around the player, if its host has a mini player to dock into. */
val LocalPlayerDock = staticCompositionLocalOf<PlayerDock?> { null }
