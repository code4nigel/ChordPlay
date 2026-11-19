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
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private val LiveRed = Color(0xFFE53935)
private val LiveDotBright = Color(0xFFFF5252)

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
fun LiveBadge(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val pulse = rememberInfiniteTransition(label = "liveBadgePulse")
    val dotAlpha by pulse.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "liveDotAlpha",
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(if (compact) 4.dp else 6.dp))
            .background(LiveRed.copy(alpha = 0.92f))
            .padding(
                horizontal = if (compact) 5.dp else 8.dp,
                vertical = if (compact) 2.dp else 3.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 5.dp else 6.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = dotAlpha)),
        )
        Spacer(Modifier.width(if (compact) 3.dp else 5.dp))
        Text(
            text = stringResource(Res.string.live_badge),
            color = Color.White,
            fontSize = if (compact) 9.sp else 11.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = if (compact) 10.sp else 13.sp,
            letterSpacing = 0.4.sp,
            maxLines = 1,
        )
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
    val secondaryAccent by animateColorAsState(
        targetValue = palette.colors.getOrNull(1) ?: accentColor,
        animationSpec = tween(500),
        label = "liveSecondaryAccent",
    )
    val pulse = rememberInfiniteTransition(label = "liveEdgePulse")
    val dotAlpha by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "liveEdgeDotAlpha",
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
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(LiveRed.copy(alpha = 0.22f))
                        .border(1.dp, LiveDotBright.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(LiveDotBright.copy(alpha = if (isPlaying) dotAlpha else 0.7f)),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(Res.string.live_badge),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(accentColor.copy(alpha = 0.24f))
                        .border(1.dp, accentColor.copy(alpha = 0.65f), RoundedCornerShape(999.dp))
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

    Column(modifier = modifier.fillMaxWidth()) {
        if (isSeekable && durationMs > 0L) {
            SynesthesiaWaveformScrubber(
                shown = shown,
                durationMs = durationMs,
                isPlaying = isPlaying,
                onScrub = onScrub,
                onScrubFinished = onScrubFinished,
                trackKey = trackKey,
                palette = palette,
                showTimestamps = false,
            )
            Spacer(Modifier.height(6.dp))
            StatusRow()
        } else {
            StatusRow()
            Spacer(Modifier.height(8.dp))
            PureLiveWaveStrip(
                isPlaying = isPlaying,
                accentColor = accentColor,
                secondaryAccent = secondaryAccent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp),
            )
        }
    }
}

@Composable
private fun PureLiveWaveStrip(
    isPlaying: Boolean,
    accentColor: Color,
    secondaryAccent: Color,
    modifier: Modifier = Modifier,
) {
    val phaseTransition = rememberInfiniteTransition(label = "pureLiveWave")
    val phase by phaseTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pureLivePhase",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val midY = h / 2f
        val maxAmp = h * 0.34f
        val steps = 64
        val activePhase = if (isPlaying) phase else 0f
        val ampScale = if (isPlaying) 1f else 0.25f

        val wavePath = Path()
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val x = t * w
            val env = sin(t * PI).toFloat()
            val signal = sin(t * 14f - activePhase) * 0.65f + sin(t * 23f + activePhase * 1.4f) * 0.35f
            val y = midY + signal * maxAmp * env * ampScale
            if (i == 0) wavePath.moveTo(x, y) else wavePath.lineTo(x, y)
        }

        drawLine(
            color = Color.White.copy(alpha = 0.14f),
            start = Offset(0f, midY),
            end = Offset(w, midY),
            strokeWidth = 1.2.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawPath(
            path = wavePath,
            color = accentColor.copy(alpha = 0.32f),
            style = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round),
        )
        drawPath(
            path = wavePath,
            brush = Brush.horizontalGradient(
                colors = listOf(accentColor, secondaryAccent),
            ),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}
