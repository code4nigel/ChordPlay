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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrain
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.bitchord.data.canvas.CanvasArtwork
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.settings.FullscreenArtworkLyricsStyle
import com.music.bitchord.playback.PlaybackPosition
import com.music.bitchord.ui.icons.BitChordIcons
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * What one frame off the playing artwork clip holds — kept off the player's
 * own composition via [CanvasFrameHolder] so frame reads don't recompose the
 * entire player screen.
 */
@Stable
internal class CanvasClipState {
    var aspect by mutableFloatStateOf(0f)
    val rendered = mutableStateOf(false)
    val cover = mutableFloatStateOf(0f)
    val frames = CanvasFrameHolder()
    var spotifyControlsOpen by mutableStateOf(true)
}

/**
 * Measurements published out of layout passes in portrait mode without forcing
 * parent recompositions on every animation step.
 */
@Stable
internal class PlayerMeasures {
    var playerBounds by mutableStateOf(IntSize.Zero)
    var heroHeight by mutableStateOf(0.dp)
    var cardBottom by mutableStateOf(0.dp)
    var spotifyDeckHeight by mutableStateOf(0.dp)
    var spotifyExpandedTopHeight by mutableStateOf(0.dp)
    var dismissBandTop by mutableFloatStateOf(0f)
    var dismissBandBottom by mutableFloatStateOf(0f)
    var dismissBandSpace: LayoutCoordinates? = null
}

/**
 * Portrait player sleeve, vinyl/modern ambient effects, music video box, and
 * fullscreen Canvas lyrics pill/strip.
 */
@Composable
internal fun BoxScope.PortraitSleeve(
    song: Song,
    art: PlayerArtwork,
    coverPresentation: CoverPresentation?,
    clipState: CanvasClipState,
    canvas: CanvasArtwork?,
    heroClip: CanvasArtwork?,
    heroMode: Boolean,
    tabletArtworkBackdrop: Boolean,
    panels: PlayerPanelState,
    collapse: PlayerCollapse,
    isPlaying: Boolean,
    position: PlaybackPosition,
    durationMs: Long,
    scrub: PlayerScrub,
    dock: PlayerDock?,
    dockFrame: DockFrame,
    dockT: () -> Float,
    docking: () -> Boolean,
    dockMoving: Boolean,
    sleeveLayer: GraphicsLayer,
    playerHaze: HazeState,
    density: Density,
    playerBounds: IntSize,
    heroHeight: Dp,
    boxWidthPx: Int,
    bannerTopPx: Int,
    maxWidth: Dp,
    maxHeight: Dp,
    fullArt: Dp,
    groupTop: Dp,
    thumbSize: Dp,
    headerHeight: Dp,
    artTitleGap: Dp,
    thumbWidth: Dp,
    bannerShare: () -> Float,
    bannerShown: () -> Float,
    cardScale: () -> Float,
    artSize: () -> Dp,
    artTop: () -> Dp,
    artStart: () -> Dp,
    currentTitleTop: () -> Dp,
    titleStart: () -> Dp,
    isModernAmbient: Boolean,
    enableBreathingAura: Boolean,
    isVinylRecord: Boolean,
    videoMode: Boolean,
    videoHeroMode: Boolean,
    videoRendered: Boolean,
    onVideoRenderedChange: (Boolean) -> Unit,
    videoAspect: Float,
    videoFill: Boolean,
    videoFullBleed: Boolean,
    videoControlsVisible: Boolean,
    videoControlsAlpha: Float,
    onToggleVideoControls: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onOpenVideoQuality: () -> Unit,
    swipeSettle: Float,
    swipeThreshold: Float,
    swipeHintShown: Boolean,
    swipeHintNext: Boolean,
    hasPrevious: Boolean,
    hasNext: Boolean,
    spotifyCanvasPresentation: Boolean,
    spotifyChromeAlpha: Float,
    syncedLyricsEnabled: Boolean,
    hideLyricsPreview: Boolean = false,
    fullscreenLyricsStyle: FullscreenArtworkLyricsStyle,
    lyricsTranslation: LyricsTranslationUi,
    lyricsPlayhead: LyricPlayhead,
    lyricsUnavailable: Boolean,
    lyricsLoadingText: String,
    deckRevealValue: () -> Float,
    meshRefreshMs: Long?,
    onMeshFrameCaptured: (ImageBitmap?) -> Unit,
    onDismissBandTop: (Float) -> Unit,
    heroFadeFraction: Float,
    onToggleFullBleedArtwork: (() -> Unit)? = null,
) {
    val reduceDynamicBlur by PlayerSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val p = collapse.p

    fun cardRect(): Rect = with(density) {
        val side = artSize().roundToPx().toFloat()
        val square = Rect(
            offset = Offset(artStart().roundToPx().toFloat(), artTop().roundToPx().toFloat()),
            size = Size(side, side),
        )
        if (!videoMode) return@with square
        val height = boxWidthPx / videoAspect.coerceIn(1f, 2.4f)
        val video = Rect(
            offset = Offset(0f, square.center.y - height / 2f),
            size = Size(boxWidthPx.toFloat(), height),
        )
        val thumbW = thumbWidth.roundToPx().toFloat()
        val thumbH = thumbSize.roundToPx().toFloat()
        val thumb = Rect(Offset.Zero, Size(thumbW, thumbH))
        lerp(video, thumb, p())
    }

    fun sleeveRect(): Rect {
        val card = cardRect()
        val share = bannerShare()
        if (share == 0f) return card
        val banner = with(density) {
            Rect(
                offset = Offset(-(playerBounds.width - boxWidthPx) / 2f, bannerTopPx.toFloat()),
                size = Size(playerBounds.width.toFloat(), heroHeight.roundToPx().toFloat()),
            )
        }
        val thumbSide = with(density) { thumbSize.roundToPx().toFloat() }
        val thumb = Rect(Offset.Zero, Size(thumbSide, thumbSide))
        return lerp(card, lerp(banner, thumb, p()), share)
    }

    val boxLeftPx = ((playerBounds.width - boxWidthPx) / 2f).roundToInt()

    fun miniInBox(): Rect? {
        if (dock == null || !docking()) return null
        val mini = dock.miniArtOnScreen() ?: return null
        val anchor = dockFrame.anchor ?: return null
        val offset = dock.offset() ?: return null
        dockFrame.offset = offset.roundToInt().toFloat()
        return mini.translate(
            -(anchor.x + boxLeftPx),
            -(anchor.y + dockFrame.offset - bannerTopPx),
        )
    }

    fun dockRide(sleeve: Rect, mini: Rect): Float {
        val travel = dock?.sheetTravel ?: return 0f
        if (travel <= 0f) return 0f
        return ((mini.top + dockFrame.offset - sleeve.top) / travel).coerceIn(0f, 1f)
    }

    fun towardsDock(rect: Rect, mini: Rect?): Rect {
        if (mini == null) return rect
        val unridden = dockFrame.offset * (1f - dockFrame.ride)
        return lerp(mini, rect.translate(0f, -unridden), dockT())
    }

    fun sleeveShown(): Float = bannerShown() * dockT()

    fun sleeveCorner(): Dp {
        val baseCorner = if (isModernAmbient) 24.dp else if (isVinylRecord) 16.dp else 8.dp
        val corner = baseCorner * (1f - sleeveShown())
        val t = dockT()
        return if (dock == null || t >= 1f) corner else lerp(dock.miniArtCorner(), corner, t)
    }

    // Vinyl turntable record sliding out from sleeve
    if (isVinylRecord && art.loaded && dockT() > 0.8f && !heroMode && !videoMode && bannerShare() == 0f) {
        ChordPlayVinylRecord(
            model = art.request,
            isPlaying = isPlaying,
            modifier = Modifier
                .layout { measurable, _ ->
                    val card = towardsDock(cardRect(), dockFrame.miniInBox)
                    val discSize = (card.height * 0.95f).roundToInt()
                    val placeable = measurable.measure(Constraints.fixed(discSize, discSize))
                    val dockProgress = ((dockT() - 0.8f) / 0.2f).coerceIn(0f, 1f)
                    val shiftX = -card.width * 0.14f * dockProgress
                    val slideX = card.width * 0.28f * dockProgress
                    val offsetY = (card.height - discSize) / 2f
                    layout(0, 0) {
                        placeable.place((card.left + shiftX + slideX).roundToInt(), (card.top + offsetY).roundToInt())
                    }
                },
        )
    }

    // The sleeve proper — card, banner and thumbnail in turn.
    Box(
        modifier = Modifier
            .layout { measurable, _ ->
                val mini = miniInBox()
                val open = sleeveRect()
                if (mini != null) dockFrame.ride = dockRide(open, mini)
                val rect = towardsDock(open, mini)
                dockFrame.miniInBox = mini
                dockFrame.sleeve = rect
                val placeable = measurable.measure(
                    Constraints.fixed(rect.width.roundToInt(), rect.height.roundToInt()),
                )
                layout(0, 0) {
                    val dockProgress = ((dockT() - 0.8f) / 0.2f).coerceIn(0f, 1f)
                    val shiftX = if (isVinylRecord && bannerShare() == 0f) {
                        (-rect.width * 0.14f * dockProgress).roundToInt()
                    } else {
                        0
                    }
                    val x = rect.left.roundToInt() + shiftX
                    val y = rect.top.roundToInt()
                    dockFrame.portalLeft = (boxLeftPx + x).toFloat()
                    dockFrame.portalScale = 1f
                    dockFrame.portalTop = (y - bannerTopPx).toFloat()
                    placeable.place(x, y)
                }
            }
            .drawWithContent {
                if (docking()) {
                    sleeveLayer.record { this@drawWithContent.drawContent() }
                } else {
                    drawContent()
                }
            }
            .graphicsLayer {
                val share = bannerShare()
                val idle = cardScale()
                val t = dockT()
                val open = idle + (1f - idle) * share
                val scale = if (t >= 1f) open else 1f + (open - 1f) * t
                scaleX = scale
                scaleY = scale
                translationX = swipeSettle * (1f - p()) * (1f - share) * t
            }
            .then(if (reduceDynamicBlur) Modifier else Modifier.hazeSource(playerHaze))
            .graphicsLayer {
                alpha = when {
                    videoMode && !docking() -> (p() * 2f - 1f).coerceIn(0f, 1f)
                    heroClip != null -> 1f - clipState.cover.floatValue
                    else -> 1f
                }.coerceIn(0f, 1f)
            }
            .graphicsLayer {
                val shown = sleeveShown()
                shadowElevation = if (art.loaded) {
                    10.dp.toPx() * (1f - 2f * shown).coerceAtLeast(0f) * dockT()
                } else {
                    0f
                }
                shape = dockFrame.cornerShape(sleeveCorner())
                clip = art.loaded
            }
            .graphicsLayer {
                shape = dockFrame.cornerShape(sleeveCorner())
                clip = true
                compositingStrategy = if (sleeveShown() > 0f) {
                    CompositingStrategy.Offscreen
                } else {
                    CompositingStrategy.Auto
                }
            }
            .drawWithContent {
                drawContent()
                val shown = sleeveShown()
                if (shown > 0f) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Black, Color.Black.copy(alpha = 1f - shown)),
                            startY = size.height * (1f - heroFadeFraction),
                            endY = size.height,
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                }
            },
    ) {
        val placeholderVisible = coverPresentation == null && !art.loaded && !clipState.rendered.value &&
            (!videoMode || docking() || collapse.pastHalf)
        Box(
            modifier = Modifier
                .layout { measurable, _ ->
                    val card = towardsDock(cardRect(), dockFrame.miniInBox)
                    val sleeve = dockFrame.sleeve
                    val placeable = measurable.measure(
                        Constraints.fixed(card.width.roundToInt(), card.height.roundToInt()),
                    )
                    layout(0, 0) {
                        placeable.place(
                            (card.left - sleeve.left).roundToInt(),
                            (card.top - sleeve.top).roundToInt(),
                        )
                    }
                }
                .graphicsLayer {
                    val share = bannerShare()
                    val idle = cardScale()
                    val t = dockT()
                    val scale = if (t >= 1f) {
                        idle / (idle + (1f - idle) * share)
                    } else {
                        (1f + (idle - 1f) * t) / (1f + (idle + (1f - idle) * share - 1f) * t)
                    }
                    scaleX = scale
                    scaleY = scale
                    translationX = swipeSettle * (1f - p()) * share * t
                    shape = RoundedCornerShape(8.dp)
                    clip = share > 0f
                }
                .then(if (placeholderVisible) Modifier.background(Color.Black.copy(alpha = 0.18f)) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (placeholderVisible) {
                Icon(
                    imageVector = BitChordIcons.MusicNote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.35f),
                    modifier = Modifier
                        .size(40.dp)
                        .graphicsLayer {
                            val scale = 1f - 0.5f * p()
                            scaleX = scale
                            scaleY = scale
                        },
                )
            }
        }
        AsyncImage(
            model = art.request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            onState = art::onState,
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    if (coverPresentation == null && (art.loaded || !clipState.rendered.value) && (!videoMode || docking() || collapse.pastHalf)) {
                        drawContent()
                    }
                },
        )
        if (coverPresentation != null) CoverMotion(coverPresentation)
        if (!heroMode && canvas != null && coverPresentation == null) {
            WhileShown({ !collapse.pastHalf }) {
                CanvasArtworkPlayer(
                    canvas = canvas,
                    isPlaying = isPlaying,
                    positionMs = if (scrub.scrubbing) (scrub.value * durationMs).toLong() else position.positionMs,
                    pausedForTransition = panels.lyricsOpen || panels.queueOpen || panels.queueDragging ||
                        collapse.started || dockMoving,
                    onRenderedChanged = { clipState.rendered.value = it },
                    onFrameCaptured = {
                        if (!tabletArtworkBackdrop && !panels.lyricsOpen && !panels.queueOpen) {
                            onMeshFrameCaptured(it)
                        }
                    },
                    refreshFrameEveryMs = if (
                        tabletArtworkBackdrop || panels.lyricsOpen || panels.queueOpen || dockMoving
                    ) {
                        null
                    } else {
                        meshRefreshMs
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    // The card's slot: takes click gestures and holds nerd stats.
    Box(
        modifier = Modifier
            .offset { IntOffset(artStart().roundToPx(), artTop().roundToPx()) }
            .layout { measurable, constraints ->
                val side = artSize().roundToPx()
                val placeable = measurable.measure(constraints.constrain(Constraints.fixed(side, side)))
                layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
            }
            .onGloballyPositioned { onDismissBandTop(it.boundsInRoot().top) }
            .graphicsLayer {
                val idle = cardScale()
                scaleX = idle
                scaleY = idle
                translationX = swipeSettle * (1f - p())
            }
            .then(
                when {
                    panels.queueOpen || panels.lyricsOpen -> Modifier.clickable {
                        panels.closePanels()
                    }
                    videoHeroMode -> Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleVideoControls,
                    )
                    !videoMode && onToggleFullBleedArtwork != null -> Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleFullBleedArtwork,
                    )
                    else -> Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        WhileShown({ !collapse.pastHalf }) {
            SleeveNerdStats(
                song = song,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .graphicsLayer {
                        alpha = (1f - p() * 2f) * spotifyChromeAlpha
                    },
            )
        }
    }

    // Inline music video card (non-hero mode)
    if (videoMode && !videoHeroMode) {
        WhileShown({ !collapse.pastHalf && !docking() }) {
            val aspect = videoAspect.coerceIn(1f, 2.4f)
            Box(
                modifier = Modifier
                    .offset {
                        val width = if (videoFill && playerBounds.width > boxWidthPx) {
                            playerBounds.width
                        } else {
                            boxWidthPx
                        }
                        val height = if (videoFill) {
                            artSize().roundToPx().coerceAtLeast((width / aspect).roundToInt())
                        } else {
                            (boxWidthPx / aspect).roundToInt()
                        }
                        val centre = (artTop() + artSize() / 2).roundToPx()
                        IntOffset(-((width - boxWidthPx) / 2), centre - height / 2)
                    }
                    .layout { measurable, _ ->
                        val width = if (videoFill && playerBounds.width > boxWidthPx) {
                            playerBounds.width
                        } else {
                            boxWidthPx
                        }
                        val height = if (videoFill) {
                            artSize().roundToPx().coerceAtLeast((width / aspect).roundToInt())
                        } else {
                            (boxWidthPx / aspect).roundToInt()
                        }
                        val placeable = measurable.measure(Constraints.fixed(width, height))
                        layout(width, height) { placeable.placeRelative(0, 0) }
                    }
                    .graphicsLayer {
                        alpha = (1f - p() * 2f).coerceIn(0f, 1f)
                        translationX = swipeSettle * (1f - p())
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleVideoControls,
                    ),
            ) {
                if (!videoRendered) {
                    VideoLoadingBackdrop(
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp)),
                    )
                }
                MusicVideoSurface(
                    videoId = song.videoId,
                    fit = !videoFill,
                    onRenderedChanged = onVideoRenderedChange,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp)),
                )
                if (videoControlsVisible || videoControlsAlpha > 0f) {
                    VideoControlsDock(
                        videoFill = videoFill,
                        onToggleVideoFill = { PlayerSettings.setVideoFill(!videoFill) },
                        videoFullBleed = videoFullBleed,
                        onToggleVideoFullBleed = { PlayerSettings.setVideoFullBleed(!videoFullBleed) },
                        fullscreen = false,
                        onToggleFullscreen = onToggleFullscreen,
                        onOpenQuality = onOpenVideoQuality,
                        onEnterPip = { PlayerPlatform.host.enterPip() },
                        pipSupported = PlayerPlatform.host.pipSupported,
                        showFullBleedToggle = true,
                        hazeState = null,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .graphicsLayer { alpha = videoControlsAlpha }
                            .padding(8.dp),
                    )
                }
            }
        }
    }

    if (swipeHintShown) {
        val showNext = swipeHintNext
        val enabled = if (showNext) hasNext else hasPrevious
        val hintAlpha = if (enabled) 0.85f else 0.3f
        Icon(
            imageVector = if (showNext) Icons.Rounded.FastForward else Icons.Rounded.FastRewind,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset {
                    IntOffset(0, (artTop() + artSize() + (artTitleGap - 16.dp) / 2).roundToPx())
                }
                .size(16.dp)
                .graphicsLayer {
                    alpha = (abs(swipeSettle) / swipeThreshold).coerceIn(0f, 1f) * (1f - p()) * hintAlpha
                },
        )
    }

    // Persistent frosted one-line lyrics in fullscreen Canvas mode
    if (spotifyCanvasPresentation && syncedLyricsEnabled && !hideLyricsPreview && !panels.lyricsOpen && !panels.queueOpen && deckRevealValue() < 0.999f) {
        val pillAlpha = (1f - deckRevealValue()).coerceIn(0f, 1f)
        when (fullscreenLyricsStyle) {
            FullscreenArtworkLyricsStyle.FLOATING_CAPSULE -> {
                var pillHeightPx by remember { mutableIntStateOf(with(density) { 34.dp.roundToPx() }) }
                CanvasLyricPill(
                    lines = lyricsTranslation.displayedLyrics,
                    trackKey = song.videoId,
                    playhead = lyricsPlayhead,
                    isPlaying = isPlaying && position.advancing,
                    lyricsUnavailable = lyricsUnavailable,
                    loadingText = lyricsLoadingText,
                    onClick = panels.openLyrics,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { if (it.height > 0) pillHeightPx = it.height }
                        .offset {
                            val titleTopPx = (
                                currentTitleTop() -
                                    lerp(0.dp, (headerHeight - thumbSize) / 2, p())
                                ).roundToPx()
                            val gapPx = 14.dp.roundToPx()
                            IntOffset(x = 0, y = titleTopPx - pillHeightPx - gapPx)
                        }
                        .graphicsLayer { alpha = pillAlpha },
                )
            }
            FullscreenArtworkLyricsStyle.PLAYER_STRIP -> {
                CanvasLyricStrip(
                    lines = lyricsTranslation.displayedLyrics,
                    trackKey = song.videoId,
                    playhead = lyricsPlayhead,
                    isPlaying = isPlaying && position.advancing,
                    lyricsUnavailable = lyricsUnavailable,
                    loadingText = lyricsLoadingText,
                    titleStart = titleStart(),
                    onClick = panels.openLyrics,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset {
                            val titleTopPx = (
                                currentTitleTop() -
                                    lerp(0.dp, (headerHeight - thumbSize) / 2, p())
                                ).roundToPx()
                            val titleHeightPx = (headerHeight - 6.dp).roundToPx()
                            IntOffset(x = 0, y = titleTopPx + titleHeightPx)
                        }
                        .graphicsLayer { alpha = pillAlpha },
                )
            }
            FullscreenArtworkLyricsStyle.OFF -> Unit
        }
    }
}
