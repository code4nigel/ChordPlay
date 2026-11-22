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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.sharedui.resources.Res
import com.music.bitchord.sharedui.resources.jump_to_live
import com.music.bitchord.sharedui.resources.live_badge
import com.music.bitchord.sharedui.resources.live_on_air
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import org.jetbrains.compose.resources.stringResource
import java.util.Locale
import kotlin.math.abs

private val LiveRed = Color(0xFFE53935)
private val LiveDotBright = Color(0xFFFF5252)
private val LiveCrimson = Color(0xFFFF2D55)

internal fun formatBehindLive(offsetMs: Long): String {
    val totalSeconds = abs(offsetMs) / 1000L
    if (totalSeconds <= 0L) return "0:00"
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.US, "-%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "-%d:%02d", minutes, seconds)
    }
}

@Composable
fun LiveSoundwaveBars(
    modifier: Modifier = Modifier,
    color: Color = LiveCrimson,
    barCount: Int = 3,
    animated: Boolean = true,
) {
    val heights = if (animated) {
        val transition = rememberInfiniteTransition(label = "soundwave")
        val h1 by transition.animateFloat(
            initialValue = 0.28f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(480, easing = LinearEasing), RepeatMode.Reverse),
            label = "h1",
        )
        val h2 by transition.animateFloat(
            initialValue = 0.85f,
            targetValue = 0.22f,
            animationSpec = infiniteRepeatable(tween(370, easing = LinearEasing), RepeatMode.Reverse),
            label = "h2",
        )
        val h3 by transition.animateFloat(
            initialValue = 0.42f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(tween(540, easing = LinearEasing), RepeatMode.Reverse),
            label = "h3",
        )
        listOf(h1, h2, h3)
    } else {
        listOf(0.45f, 0.85f, 0.55f)
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        heights.take(barCount).forEach { fraction ->
            Box(
                Modifier
                    .width(2.5.dp)
                    .height((9.dp * fraction).coerceAtLeast(2.5.dp))
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@Composable
fun LiveBadge(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    showSoundwaves: Boolean = true,
    animated: Boolean = true,
) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color(0xFF0F0E17).copy(alpha = 0.78f))
            .background(
                androidx.compose.ui.graphics.Brush.horizontalGradient(
                    listOf(
                        LiveCrimson.copy(alpha = 0.35f),
                        LiveCrimson.copy(alpha = 0.16f),
                    ),
                ),
            )
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.16f),
                        Color.Transparent,
                    ),
                ),
            )
            .padding(
                horizontal = if (compact) 7.dp else 9.dp,
                vertical = if (compact) 3.dp else 4.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 5.dp),
        ) {
            if (showSoundwaves) {
                LiveSoundwaveBars(color = LiveCrimson, barCount = 3, animated = animated)
            } else {
                Box(
                    modifier = Modifier
                        .size(if (compact) 5.dp else 6.dp)
                        .clip(CircleShape)
                        .background(LiveCrimson),
                )
            }
            Text(
                text = stringResource(Res.string.live_badge),
                color = Color.White,
                fontSize = if (compact) 9.5.sp else 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun LiveStationBar(
    shown: () -> Float,
    durationMs: Long,
    isPlaying: Boolean,
    isSeekable: Boolean,
    isAtLiveEdge: Boolean,
    liveOffsetMs: Long,
    liveViewersText: String?,
    onScrub: (Float) -> Unit,
    onScrubFinished: () -> Unit,
    onJumpToLive: () -> Unit,
    trackKey: String = "",
    palette: MeshPalette = remember { MeshPalette(emptyList()) },
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val accentColor by animateColorAsState(
        targetValue = palette.colors.firstOrNull() ?: LiveDotBright,
        animationSpec = tween(500),
        label = "liveAccent",
    )
    val rightLabel = liveViewersText?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.live_on_air)

    @Composable
    fun StatusRow() {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isAtLiveEdge || !isSeekable || durationMs <= 0L) {
                LiveBadge(
                    showSoundwaves = true,
                    animated = isPlaying,
                )
            } else {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFF14121B).copy(alpha = 0.82f))
                        .background(accentColor.copy(alpha = 0.32f))
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
                            ),
                        )
                        .clickable {
                            haptics.play(Haptic.Select)
                            onJumpToLive()
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = stringResource(Res.string.jump_to_live),
                        tint = Color.White,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "${stringResource(Res.string.jump_to_live)} (${formatBehindLive(liveOffsetMs)})",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.4.sp,
                    )
                }
            }

            Text(
                text = rightLabel,
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    val canScrub = isSeekable && durationMs > 0L
    Column(modifier = modifier.fillMaxWidth()) {
        SynesthesiaWaveformScrubber(
            shown = if (canScrub) shown else ({ 1f }),
            durationMs = durationMs,
            isPlaying = isPlaying,
            onScrub = onScrub,
            onScrubFinished = onScrubFinished,
            trackKey = trackKey,
            palette = palette,
            showTimestamps = false,
            liveWaveMotion = true,
            scrubEnabled = canScrub,
        )
        Spacer(Modifier.height(6.dp))
        StatusRow()
    }
}
