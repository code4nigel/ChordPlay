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
package com.music.bitchord.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.music.bitchord.data.settings.MiniPlayerTimelineStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

private object MiniPlayerAuroraSessionClock {
    var flowPhase: Float = 0.8f
    var driftPhase: Float = 1.5f
}

internal class MiniPlayerAuroraPhaseState(
    initialFlow: Float,
    initialDrift: Float,
) {
    var flowPhase by mutableFloatStateOf(initialFlow)
    var driftPhase by mutableFloatStateOf(initialDrift)
}

@Composable
internal fun rememberMiniPlayerAuroraPhase(
    enabled: Boolean,
    isPlaying: Boolean,
    isScrubbing: Boolean,
    reduceAnimation: Boolean,
    persistSessionClock: Boolean = true,
): MiniPlayerAuroraPhaseState {
    val state = remember {
        MiniPlayerAuroraPhaseState(
            initialFlow = MiniPlayerAuroraSessionClock.flowPhase,
            initialDrift = MiniPlayerAuroraSessionClock.driftPhase,
        )
    }

    LaunchedEffect(enabled, isPlaying, isScrubbing, reduceAnimation) {
        if (!enabled || reduceAnimation) return@LaunchedEffect
        var lastMs = withFrameMillis { it }
        while (true) {
            val now = withFrameMillis { it }
            val dt = ((now - lastMs) / 1000f).coerceIn(0f, 0.05f)
            lastMs = now

            val speedDrive = when {
                isScrubbing -> 2.15f
                isPlaying -> 1.00f
                else -> 0.40f
            }
            state.flowPhase += dt * (0.95f * speedDrive)
            state.driftPhase += dt * (0.44f * speedDrive)
            if (persistSessionClock) {
                MiniPlayerAuroraSessionClock.flowPhase = state.flowPhase
                MiniPlayerAuroraSessionClock.driftPhase = state.driftPhase
            }
        }
    }
    return state
}

internal class MiniPlayerTimelinePaths {
    val capsulePath = Path()
    val segmentPath = Path()
    val waveAreaPath = Path()
    val frontEdgePath = Path()
    val silkPath = Path()
    val pathMeasure = PathMeasure()
}

private fun DrawScope.updateCapsulePath(capsulePath: Path, insetPx: Float) {
    val h = (size.height - 2f * insetPx).coerceAtLeast(1f)
    val r = h / 2f
    capsulePath.rewind()
    capsulePath.addRoundRect(
        RoundRect(
            left = insetPx,
            top = insetPx,
            right = (size.width - insetPx).coerceAtLeast(insetPx + 1f),
            bottom = (size.height - insetPx).coerceAtLeast(insetPx + 1f),
            cornerRadius = CornerRadius(r, r),
        ),
    )
}

/**
 * Draws the translucent Aurora Horizon Tide & floating silk currents behind MiniPlayer text/controls
 * when [style] is [MiniPlayerTimelineStyle.AURORA_WAVE].
 */
internal fun DrawScope.drawMiniPlayerTimelineUnderlay(
    style: MiniPlayerTimelineStyle,
    progress: Float,
    scrubbing: Boolean,
    flowPhase: Float,
    driftPhase: Float,
    aurora: ChordWaveAuroraPalette,
    isDark: Boolean,
    paths: MiniPlayerTimelinePaths,
) {
    if (style != MiniPlayerTimelineStyle.AURORA_WAVE) return
    val clampedProgress = progress.coerceIn(0f, 1f)
    if (clampedProgress <= 0.0005f || size.width <= 1f || size.height <= 1f) return

    updateCapsulePath(paths.capsulePath, insetPx = 0f)
    val w = size.width
    val h = size.height
    val baseX = clampedProgress * w
    val edgeTaper = sin(PI * clampedProgress).toFloat().coerceAtLeast(0f).pow(0.55f)
    val frontAmp = ((if (scrubbing) 7.5f else 5.2f).dp.toPx()) * edgeTaper

    paths.waveAreaPath.rewind()
    paths.frontEdgePath.rewind()
    paths.waveAreaPath.moveTo(0f, 0f)

    val frontSamples = 14
    for (i in 0..frontSamples) {
        val uY = i.toFloat() / frontSamples
        val y = uY * h
        val ripple = (sin(uY * PI * 2.0 * 1.15f + flowPhase) +
            0.42f * cos(uY * PI * 2.0 * 2.25f - flowPhase * 0.72f)).toFloat()
        val x = (baseX + ripple * frontAmp).coerceIn(0f, w)
        paths.waveAreaPath.lineTo(x, y)
        if (i == 0) {
            paths.frontEdgePath.moveTo(x, y)
        } else {
            paths.frontEdgePath.lineTo(x, y)
        }
    }
    paths.waveAreaPath.lineTo(0f, h)
    paths.waveAreaPath.close()

    val washBoost = if (scrubbing) 1.28f else 1.0f
    val leftAlpha = (if (isDark) 0.14f else 0.11f) * washBoost
    val midAlpha = (if (isDark) 0.24f else 0.17f) * washBoost
    val edgeAlpha = (if (isDark) 0.34f else 0.24f) * washBoost

    clipPath(paths.capsulePath) {
        clipPath(paths.waveAreaPath) {
            // 1. Translucent artwork aurora tide
            drawRect(
                brush = Brush.horizontalGradient(
                    0.00f to aurora.secondary.copy(alpha = leftAlpha.coerceAtMost(0.45f)),
                    0.55f to aurora.primary.copy(alpha = midAlpha.coerceAtMost(0.55f)),
                    1.00f to aurora.highlight.copy(alpha = edgeAlpha.coerceAtMost(0.65f)),
                    startX = 0f,
                    endX = (baseX + frontAmp).coerceAtLeast(1f),
                ),
            )

            // 2. Orbiting nebula glow near the active wave front
            val cloudCenterY = h * (0.48f + 0.14f * sin(driftPhase * 0.85f))
            drawNebulaCloud(
                color = aurora.highlight,
                center = Offset(baseX * 0.88f, cloudCenterY),
                radius = h * 1.15f,
                alpha = (if (isDark) 0.28f else 0.20f) * washBoost,
            )

            // 3. Two delicate flowing silk filaments inside the played aurora tide
            val silkSamples = 26
            val spanW = (baseX + frontAmp * 1.5f).coerceAtLeast(12f)
            for (band in 0 until 2) {
                paths.silkPath.rewind()
                val bandColor = if (band == 0) aurora.primary else aurora.filament
                val baseBandY = if (band == 0) h * 0.56f else h * 0.42f
                val bandSpeed = if (band == 0) 0.92f else -1.18f
                val bandAmp = h * (if (band == 0) 0.16f else 0.12f)
                for (i in 0..silkSamples) {
                    val u = i.toFloat() / silkSamples
                    val x = u * spanW
                    val env = sin(PI * u).toFloat().coerceAtLeast(0f).pow(0.85f)
                    val w1 = sin(u * PI * 3.0 + flowPhase * bandSpeed + band * 1.9f).toFloat()
                    val y = baseBandY + w1 * bandAmp * env
                    if (i == 0) {
                        paths.silkPath.moveTo(x, y)
                    } else {
                        paths.silkPath.lineTo(x, y)
                    }
                }
                drawPath(
                    path = paths.silkPath,
                    color = bandColor.copy(alpha = (if (band == 0) 0.22f else 0.30f) * washBoost),
                    style = Stroke(
                        width = (if (band == 0) 3.2f else 1.2f).dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
        }

        // 4. Shimmering iridescent veil along the wave-front edge
        if (clampedProgress > 0.015f && clampedProgress < 0.985f) {
            drawPath(
                path = paths.frontEdgePath,
                color = aurora.primary.copy(alpha = if (scrubbing) 0.45f else 0.26f),
                style = Stroke(width = (if (scrubbing) 4.2f else 3.0f).dp.toPx(), cap = StrokeCap.Round),
            )
            drawPath(
                path = paths.frontEdgePath,
                color = lerp(aurora.filament, Color.White, 0.35f).copy(alpha = if (scrubbing) 0.72f else 0.46f),
                style = Stroke(width = 1.1.dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }
}

/**
 * Draws either the classic OG Circle perimeter stroke ([MiniPlayerTimelineStyle.OG_CIRCLE])
 * or the luminous bottom silk shoreline + starlight playhead crest ([MiniPlayerTimelineStyle.AURORA_WAVE])
 * over the capsule after [drawContent].
 */
internal fun DrawScope.drawMiniPlayerTimelineOverlay(
    style: MiniPlayerTimelineStyle,
    progress: Float,
    scrubbing: Boolean,
    flowPhase: Float,
    aurora: ChordWaveAuroraPalette,
    ogGlowColor: Color,
    isInline: Boolean,
    paths: MiniPlayerTimelinePaths,
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    if (clampedProgress <= 0.0005f || size.width <= 1f || size.height <= 1f) return

    when (style) {
        MiniPlayerTimelineStyle.OG_CIRCLE -> {
            val strokeInset = 1.dp.toPx()
            updateCapsulePath(paths.capsulePath, insetPx = strokeInset)
            paths.pathMeasure.setPath(paths.capsulePath, forceClosed = true)
            val totalLen = paths.pathMeasure.length
            val segmentLen = clampedProgress * totalLen
            if (segmentLen <= 0f) return

            paths.segmentPath.rewind()
            paths.pathMeasure.getSegment(0f, segmentLen, paths.segmentPath, startWithMoveTo = true)
            drawPath(
                path = paths.segmentPath,
                color = ogGlowColor.copy(alpha = if (scrubbing) 0.50f else 0.35f),
                style = Stroke(
                    width = (if (isInline) (if (scrubbing) 4.5f else 3.5f) else (if (scrubbing) 5.0f else 4.0f)).dp.toPx(),
                    cap = StrokeCap.Round,
                ),
            )
            drawPath(
                path = paths.segmentPath,
                color = ogGlowColor.copy(alpha = if (scrubbing) 0.90f else 0.75f),
                style = Stroke(
                    width = (if (isInline) (if (scrubbing) 2.2f else 1.7f) else (if (scrubbing) 2.8f else 2.2f)).dp.toPx(),
                    cap = StrokeCap.Round,
                ),
            )
            drawPath(
                path = paths.segmentPath,
                color = Color.White.copy(alpha = if (scrubbing) 1.0f else 0.90f),
                style = Stroke(
                    width = (if (isInline) (if (scrubbing) 1.3f else 1.0f) else (if (scrubbing) 1.5f else 1.2f)).dp.toPx(),
                    cap = StrokeCap.Round,
                ),
            )
        }

        MiniPlayerTimelineStyle.AURORA_WAVE -> {
            updateCapsulePath(paths.capsulePath, insetPx = 0f)
            val w = size.width
            val h = size.height
            val insetX = 3.dp.toPx()
            val xEnd = (clampedProgress * w).coerceIn(insetX + 1f, w - insetX)
            val bottomY = h - (if (isInline) 1.4f else 1.8f).dp.toPx()
            val shoreSamples = 22
            val waveAmp = (if (scrubbing) 1.6f else 1.05f).dp.toPx()
            val edgeTaper = sin(PI * clampedProgress).toFloat().coerceAtLeast(0f).pow(0.55f)

            paths.silkPath.rewind()
            var endY = bottomY
            for (i in 0..shoreSamples) {
                val u = i.toFloat() / shoreSamples
                val x = insetX + u * (xEnd - insetX)
                val env = sin(PI * u * 0.85f).toFloat().coerceAtLeast(0f)
                val ripple = sin(u * PI * 3.2f - flowPhase * 1.15f).toFloat()
                val y = bottomY - ripple * waveAmp * env * edgeTaper
                if (i == 0) {
                    paths.silkPath.moveTo(x, y)
                } else {
                    paths.silkPath.lineTo(x, y)
                }
                if (i == shoreSamples) endY = y
            }

            clipPath(paths.capsulePath) {
                // Soft colored aura along the shoreline
                drawPath(
                    path = paths.silkPath,
                    color = aurora.primary.copy(alpha = if (scrubbing) 0.58f else 0.36f),
                    style = Stroke(
                        width = (if (isInline) 2.8f else 3.4f).dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
                // Tapered pearlescent shoreline filament
                drawPath(
                    path = paths.silkPath,
                    brush = Brush.horizontalGradient(
                        0.00f to aurora.secondary.copy(alpha = 0.25f),
                        0.60f to aurora.highlight.copy(alpha = if (scrubbing) 0.92f else 0.72f),
                        1.00f to lerp(aurora.filament, Color.White, 0.55f).copy(alpha = if (scrubbing) 1f else 0.90f),
                        startX = insetX,
                        endX = xEnd.coerceAtLeast(insetX + 1f),
                    ),
                    style = Stroke(
                        width = (if (isInline) 1.25f else 1.55f).dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )

                // Starlight playhead crest orb marking the live position
                val crestCenter = Offset(xEnd, endY)
                drawNebulaCloud(
                    color = aurora.highlight,
                    center = crestCenter,
                    radius = (if (scrubbing) 8.5f else 6.0f).dp.toPx(),
                    alpha = if (scrubbing) 0.82f else 0.55f,
                )
                drawCircle(
                    color = Color.White.copy(alpha = if (scrubbing) 1f else 0.92f),
                    radius = (if (scrubbing) 2.2f else 1.6f).dp.toPx(),
                    center = crestCenter,
                )
            }
        }
    }
}
