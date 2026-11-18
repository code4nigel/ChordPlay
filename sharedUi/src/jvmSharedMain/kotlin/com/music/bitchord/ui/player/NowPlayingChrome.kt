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
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.zIndex
import com.music.bitchord.data.model.Song
import com.music.bitchord.sharedui.resources.Res
import com.music.bitchord.sharedui.resources.spotify_logo
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

internal val PLAYER_GUTTER = 30.dp
internal val PLAYER_MAX_WIDTH = 560.dp

internal const val LYRICS_CONTROLS_IDLE_MS = 5_000L
internal const val SPOTIFY_CANVAS_CONTROLS_IDLE_MS = 5_000L
internal const val SPOTIFY_CANVAS_CONTROLS_ANIMATION_MS = 420
internal const val PLAYER_DECK_FADE_IN_MS = 700
internal const val SPOTIFY_DECK_TOP_FADE_FRACTION = 0.28f

internal fun playerDeckSlideIn() = slideInVertically(
    animationSpec = tween(
        SPOTIFY_CANVAS_CONTROLS_ANIMATION_MS,
        easing = FastOutSlowInEasing,
    ),
    initialOffsetY = { it },
)

internal fun playerDeckSlideOut() = slideOutVertically(
    animationSpec = tween(
        SPOTIFY_CANVAS_CONTROLS_ANIMATION_MS,
        easing = FastOutSlowInEasing,
    ),
    targetOffsetY = { it },
)

/**
 * One lower-deck component, with one animation value owning both its pixels
 * and the space it occupies.
 */
@Composable
internal fun SlidingPlayerDeck(
    visible: Boolean,
    reveal: Animatable<Float, AnimationVector1D>,
    modifier: Modifier = Modifier,
    fadeIn: () -> Boolean = { false },
    content: @Composable () -> Unit,
) {
    var mounted by remember { mutableStateOf(visible) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(visible) {
        if (visible) {
            if (fadeIn() && reveal.value < 1f) {
                alpha.snapTo(0f)
                reveal.snapTo(1f)
                mounted = true
                alpha.animateTo(
                    1f,
                    tween(PLAYER_DECK_FADE_IN_MS, easing = LinearEasing),
                )
            } else {
                alpha.snapTo(1f)
                mounted = true
                reveal.animateTo(
                    1f,
                    tween(
                        SPOTIFY_CANVAS_CONTROLS_ANIMATION_MS,
                        easing = FastOutSlowInEasing,
                    ),
                )
            }
        } else {
            reveal.animateTo(
                0f,
                tween(
                    SPOTIFY_CANVAS_CONTROLS_ANIMATION_MS,
                    easing = FastOutSlowInEasing,
                ),
            )
            mounted = false
        }
    }

    if (mounted) {
        Box(
            modifier = modifier
                .graphicsLayer { this.alpha = alpha.value }
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints.copy(minHeight = 0))
                    val animatedHeight = (placeable.height * reveal.value)
                        .roundToInt()
                        .coerceIn(constraints.minHeight, constraints.maxHeight)
                    layout(placeable.width, animatedHeight) {
                        placeable.placeRelative(0, 0)
                    }
                },
        ) {
            content()
        }
    }
}

@Composable
internal fun NowPlayingTopBar(
    topStrip: Dp,
    statusBarTop: Dp,
    topScrimBrush: Brush,
    isWavePlay: Boolean,
    spotifyChromeAlpha: Float,
    p: () -> Float,
    collapseAlmostDone: Boolean,
    onOpenPlaybackSource: () -> Unit,
    topBarGlyphTint: Color,
    hideSongStatus: Boolean,
    song: Song,
    playedBy: String?,
    openPlaybackOrigin: () -> Unit,
    animatedCanvas: Boolean,
    isTopBright: Boolean,
    onToggleAnimatedCanvas: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .zIndex(1f)
            .fillMaxWidth()
            .height(topStrip)
            .drawBehind {
                val above = statusBarTop.toPx()
                translate(top = -above) {
                    drawRect(
                        brush = topScrimBrush,
                        size = Size(size.width, size.height + above),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (!isWavePlay) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .offset {
                        IntOffset(
                            x = 0,
                            y = lerp(
                                6.dp,
                                (topStrip - 5.dp).coerceAtLeast(0.dp) / 2,
                                p(),
                            ).roundToPx(),
                        )
                    }
                    .width(38.dp)
                    .height(5.dp)
                    .graphicsLayer { alpha = spotifyChromeAlpha }
                    .shadow(2.dp, RoundedCornerShape(3.dp), clip = false)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.70f)),
            )
        }
        if (isWavePlay) {
            WhileShown({ !collapseAlmostDone }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .graphicsLayer { alpha = (1f - p()) * spotifyChromeAlpha },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onOpenPlaybackSource,
                            ),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = topBarGlyphTint,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    if (!hideSongStatus) {
                        PlaybackOriginCaption(
                            text = playbackOriginText(song, playedBy),
                            onClick = openPlaybackOrigin,
                            textAlign = TextAlign.Center,
                            contentPadding = PaddingValues(start = 8.dp, end = 8.dp),
                            textColor = topBarGlyphTint,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    SpotifyCanvasToggleButton(
                        animatedCanvas = animatedCanvas,
                        isTopBright = isTopBright,
                        topBarGlyphTint = topBarGlyphTint,
                        onToggle = onToggleAnimatedCanvas,
                    )
                }
            }
        } else if (!hideSongStatus) {
            WhileShown({ !collapseAlmostDone }) {
                PlaybackOriginCaption(
                    text = playbackOriginText(song, playedBy),
                    onClick = openPlaybackOrigin,
                    textAlign = TextAlign.Center,
                    contentPadding = PaddingValues(start = PLAYER_GUTTER, end = PLAYER_GUTTER, bottom = 1.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .graphicsLayer { alpha = (1f - p()) * spotifyChromeAlpha },
                )
            }
        }
    }
}

@Composable
internal fun SpotifyCanvasToggleButton(
    animatedCanvas: Boolean,
    isTopBright: Boolean,
    topBarGlyphTint: Color,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle,
            ),
        contentAlignment = Alignment.CenterEnd,
    ) {
        val activeTint = if (isTopBright) Color(0xFF15883E) else Color(0xFF1DB954)
        val iconTint = if (animatedCanvas) activeTint else topBarGlyphTint.copy(alpha = 0.38f)
        Icon(
            painter = painterResource(Res.drawable.spotify_logo),
            contentDescription = if (animatedCanvas) "Turn off Spotify Canvas" else "Turn on Spotify Canvas",
            tint = iconTint,
            modifier = Modifier
                .size(22.dp)
                .then(
                    if (!animatedCanvas) {
                        Modifier.drawWithContent {
                            drawContent()
                            val pad = 2.dp.toPx()
                            val strokeWidth = 2.dp.toPx()
                            drawLine(
                                color = iconTint,
                                start = Offset(size.width - pad, pad),
                                end = Offset(pad, size.height - pad),
                                strokeWidth = strokeWidth,
                                cap = StrokeCap.Round,
                            )
                        }
                    } else {
                        Modifier
                    },
                ),
        )
    }
}

