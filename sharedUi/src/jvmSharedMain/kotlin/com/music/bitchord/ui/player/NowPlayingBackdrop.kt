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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.lyrics.LyricLine
import com.music.bitchord.sharedui.resources.Res
import com.music.bitchord.sharedui.resources.instrumental
import com.music.bitchord.sharedui.resources.open_lyrics
import com.music.bitchord.ui.icons.BitChordIcons
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.stringResource

/**
 * The portrait player's backdrop: the artwork mesh (or legacy blobs), the
 * full-cover blur the lyrics and the queue stand on, and ChordPlay's nocturne
 * aura for WavePlay.
 */
@Composable
internal fun BoxScope.PortraitBackdrop(
    legacyMesh: Boolean,
    tablet: Boolean,
    panelOpen: Boolean,
    collapseP: (() -> Float)? = null,
    canvasFirstPortrait: Boolean,
    spotifyCanvasPresentation: Boolean,
    isWavePlay: Boolean,
    isPlaying: Boolean,
    synesthesiaPalette: MeshPalette,
    trackKey: String,
    imageUrl: String?,
    artPx: Int,
    meshLiveFrames: Flow<ImageBitmap?>?,
    seam: () -> Dp,
    lift: () -> Dp,
    liftFrom: () -> Dp,
    fullArtworkBlur: @Composable (Modifier) -> Unit,
    playerHaze: HazeState,
) {
    val animatedPanelProgress by animateFloatAsState(
        targetValue = if (panelOpen) 1f else 0f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "fullArtworkBackdropCrossfade",
    )
    val panelProgress: () -> Float = collapseP?.let { fn ->
        { fn().coerceIn(0f, 1f) }
    } ?: { animatedPanelProgress }
    val fullArtworkBackdropAlpha: () -> Float = {
        when {
            legacyMesh || canvasFirstPortrait -> 0f
            tablet -> 1f
            else -> panelProgress()
        }
    }
    val blurDrawn by remember { derivedStateOf { fullArtworkBackdropAlpha() > 0.001f } }
    val nocturneAlpha: () -> Float = { (1f - panelProgress()).coerceIn(0f, 1f) }
    val nocturneDrawn by remember { derivedStateOf { nocturneAlpha() > 0.001f } }

    if (legacyMesh && !canvasFirstPortrait && !tablet && !spotifyCanvasPresentation) {
        LegacyMeshBackdrop(
            imageUrl = imageUrl,
            liveFrames = meshLiveFrames,
            trackKey = trackKey,
            modifier = Modifier.hazeSource(playerHaze),
        )
    } else if (!tablet && !spotifyCanvasPresentation) {
        PlayerMeshBackdrop(
            trackKey = trackKey,
            imageUrl = imageUrl,
            artPx = artPx,
            liveFrames = meshLiveFrames,
            seam = seam,
            lift = lift,
            liftFrom = liftFrom,
            modifier = Modifier.hazeSource(playerHaze),
        )
    }
    if (blurDrawn || tablet) {
        fullArtworkBlur(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = if (tablet) 1f else fullArtworkBackdropAlpha() }
                .hazeSource(playerHaze),
        )
    }
    if (isWavePlay && nocturneDrawn && !spotifyCanvasPresentation) {
        SynesthesiaNocturneOverlay(
            palette = synesthesiaPalette,
            isPlaying = isPlaying,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = nocturneAlpha() },
        )
    }
}

@Composable
internal fun PlayerMeshBackdrop(
    trackKey: String,
    imageUrl: String?,
    artPx: Int,
    liveFrames: Flow<ImageBitmap?>?,
    seam: () -> Dp,
    modifier: Modifier = Modifier,
    lift: () -> Dp = { 0.dp },
    liftFrom: () -> Dp = { 0.dp },
) {
    val mesh = key(trackKey) {
        rememberArtworkMesh(imageUrl, artPx = artPx, liveFrames = liveFrames)
    }
    ArtworkMeshBackdrop(
        mesh = mesh,
        seam = seam,
        modifier = modifier,
        lift = lift,
        liftFrom = liftFrom,
    )
}

@Composable
internal fun LegacyMeshBackdrop(
    imageUrl: String?,
    liveFrames: Flow<ImageBitmap?>?,
    trackKey: String,
    modifier: Modifier = Modifier,
) {
    var frame by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(liveFrames) {
        liveFrames?.collectLatest { frame = it }
    }
    MeshGradientBackground(
        palette = rememberArtworkColors(imageUrl, frame),
        trackKey = trackKey,
        modifier = modifier.fillMaxSize(),
    )
}

/**
 * Spotify Canvas's progressive legibility gradient over its video while the
 * lower control deck is open.
 */
@Composable
internal fun BoxScope.SpotifyCanvasDeckScrim(
    visible: Boolean,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .fillMaxHeight(0.56f),
        enter = fadeIn(tween(SPOTIFY_CANVAS_CONTROLS_ANIMATION_MS)) + playerDeckSlideIn(),
        exit = fadeOut(tween(SPOTIFY_CANVAS_CONTROLS_ANIMATION_MS)) + playerDeckSlideOut(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.00f to Color.Transparent,
                        SPOTIFY_DECK_TOP_FADE_FRACTION to Color.Black.copy(alpha = 0.35f),
                        1.00f to Color.Black.copy(alpha = 0.65f),
                    ),
                ),
        )
    }
}

@Composable
internal fun CanvasLyricPill(
    lines: List<LyricLine>,
    trackKey: String,
    playhead: LyricPlayhead,
    isPlaying: Boolean,
    lyricsUnavailable: Boolean,
    loadingText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSynced = remember(lines) { lines.any { it.timeMs > 0L } }
    val clock = rememberLyricClock(trackKey, playhead, isPlaying)
    val index by remember(lines) {
        derivedStateOf { lines.indexOfLast { it.timeMs <= clock.longValue } }
    }
    val current = lines.getOrNull(index)
    val instrumental = current == null || current.isGap
    val firstSung = remember(lines) { lines.indexOfFirst { !it.isGap } }
    val intro = instrumental && firstSung >= 0 && index < firstSung

    val (activeText, isInstrumental) = when {
        lines.isNotEmpty() && isSynced -> {
            when {
                intro -> stringResource(Res.string.instrumental) to true
                instrumental -> stringResource(Res.string.instrumental) to true
                else -> (current.text.takeIf { it.isNotBlank() } ?: "") to false
            }
        }
        lines.isNotEmpty() && !isSynced -> stringResource(Res.string.open_lyrics) to false
        !lyricsUnavailable && loadingText.isNotBlank() -> loadingText to false
        else -> "" to false
    }

    if (activeText.isBlank()) return

    val reduceAnimation by PlayerSettings.reduceAnimation.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .widthIn(max = PLAYER_MAX_WIDTH)
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black.copy(alpha = 0.50f))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(20.dp),
                )
                .clickable(onClick = onClick)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                )
                .padding(horizontal = 16.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = Triple(index, activeText, isInstrumental),
                transitionSpec = {
                    val duration = if (reduceAnimation) 0 else 320
                    if (reduceAnimation) {
                        (fadeIn(snap()) togetherWith fadeOut(snap())).using(
                            SizeTransform(clip = false, sizeAnimationSpec = { _, _ -> snap() }),
                        )
                    } else {
                        (fadeIn(animationSpec = tween(duration, easing = FastOutSlowInEasing)) +
                            slideInVertically(animationSpec = tween(duration, easing = FastOutSlowInEasing)) { height -> (height * 0.40f).toInt() })
                            .togetherWith(
                                fadeOut(animationSpec = tween(duration, easing = FastOutSlowInEasing)) +
                                    slideOutVertically(animationSpec = tween(duration, easing = FastOutSlowInEasing)) { height -> -(height * 0.40f).toInt() },
                            ).using(
                                SizeTransform(
                                    clip = false,
                                    sizeAnimationSpec = { _, _ ->
                                        spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMediumLow,
                                        )
                                    },
                                ),
                            )
                    }
                },
                label = "CanvasLyricPillTransition",
            ) { (_, text, isInst) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (isInst) {
                        Icon(
                            imageVector = BitChordIcons.MusicNote,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.90f),
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = text,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.95f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Mirror of the main player lyric strip positioned below Title + Menu row
 * in fullscreen Canvas / artwork mode.
 */
@Composable
internal fun CanvasLyricStrip(
    lines: List<LyricLine>,
    trackKey: String,
    playhead: LyricPlayhead,
    isPlaying: Boolean,
    lyricsUnavailable: Boolean,
    loadingText: String,
    titleStart: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSynced = remember(lines) { lines.any { it.timeMs > 0L } }
    val clock = rememberLyricClock(trackKey, playhead, isPlaying)
    val index by remember(lines) {
        derivedStateOf { lines.indexOfLast { it.timeMs <= clock.longValue } }
    }
    val current = lines.getOrNull(index)
    val instrumental = current == null || current.isGap
    val firstSung = remember(lines) { lines.indexOfFirst { !it.isGap } }
    val intro = instrumental && firstSung >= 0 && index < firstSung

    val (activeText, isInstrumental) = when {
        lines.isNotEmpty() && isSynced -> {
            when {
                intro -> stringResource(Res.string.instrumental) to true
                instrumental -> stringResource(Res.string.instrumental) to true
                else -> (current.text.takeIf { it.isNotBlank() } ?: "") to false
            }
        }
        lines.isNotEmpty() && !isSynced -> stringResource(Res.string.open_lyrics) to false
        !lyricsUnavailable && loadingText.isNotBlank() -> loadingText to false
        else -> "" to false
    }

    if (activeText.isBlank()) return

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(top = 6.dp, start = titleStart, end = 20.dp),
    ) {
        AnimatedContent(
            targetState = Triple(index, current, isInstrumental),
            transitionSpec = {
                (fadeIn(tween(320, easing = FastOutSlowInEasing)) +
                    slideInVertically(tween(320, easing = FastOutSlowInEasing)) { (it * 0.4f).toInt() })
                    .togetherWith(
                        fadeOut(tween(320, easing = FastOutSlowInEasing)) +
                            slideOutVertically(tween(320, easing = FastOutSlowInEasing)) { -(it * 0.4f).toInt() },
                    ).using(SizeTransform(clip = false))
            },
            modifier = Modifier.weight(1f, fill = false),
            label = "CanvasLyricStripTransition",
        ) { (_, lineItem, isInst) ->
            Row(verticalAlignment = Alignment.Top) {
                if (isInst) {
                    Icon(
                        imageVector = BitChordIcons.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(15.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                }
                val swept = lineItem?.takeIf { !isInst && it.isWordSynced }
                if (swept != null) {
                    SweptLyricLine(
                        line = swept,
                        clock = clock,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        ),
                        dimAlpha = 0.55f,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        rise = false,
                    )
                } else {
                    Text(
                        text = if (isInst) activeText else (lineItem?.text ?: activeText),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isInst) Color.White.copy(alpha = 0.75f) else Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
