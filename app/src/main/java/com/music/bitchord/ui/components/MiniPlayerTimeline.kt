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
import com.music.bitchord.ui.rememberIsForeground
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private object MiniPlayerAuroraSessionClock {
    var flowPhase: Float = 0.8f
    var driftPhase: Float = 1.5f
    var silkPhase: Float = 0.8f
}

internal class MiniPlayerAuroraPhaseState(
    initialFlow: Float,
    initialDrift: Float,
    initialSilk: Float,
    initialScrubMix: Float,
    initialSettleMix: Float,
) {
    var flowPhase by mutableFloatStateOf(initialFlow)
    var driftPhase by mutableFloatStateOf(initialDrift)
    var silkPhase by mutableFloatStateOf(initialSilk)
    var scrubMix by mutableFloatStateOf(initialScrubMix)
    var settleMix by mutableFloatStateOf(initialSettleMix)
}

@Composable
internal fun rememberMiniPlayerAuroraPhase(
    enabled: Boolean,
    isPlaying: Boolean,
    isScrubbing: Boolean,
    reduceAnimation: Boolean,
    persistSessionClock: Boolean = true,
    idleFrameMs: Long = 0L,
): MiniPlayerAuroraPhaseState {
    val state = remember {
        MiniPlayerAuroraPhaseState(
            initialFlow = MiniPlayerAuroraSessionClock.flowPhase,
            initialDrift = MiniPlayerAuroraSessionClock.driftPhase,
            initialSilk = MiniPlayerAuroraSessionClock.silkPhase,
            initialScrubMix = if (isScrubbing) 1f else 0f,
            initialSettleMix = if (isPlaying || isScrubbing) 1f else 0f,
        )
    }

    val isForeground = rememberIsForeground()

    LaunchedEffect(enabled, isForeground, isPlaying, isScrubbing, reduceAnimation, idleFrameMs) {
        if (!enabled || !isForeground) return@LaunchedEffect
        if (reduceAnimation) {
            state.scrubMix = if (isScrubbing) 1f else 0f
            state.settleMix = if (isPlaying || isScrubbing) 1f else 0f
            return@LaunchedEffect
        }

        var lastMs = withFrameMillis { it }
        while (true) {
            val frameIntervalMs = if (isScrubbing || state.scrubMix > 0.01f) 0L else idleFrameMs
            if (frameIntervalMs > 0L) {
                delay(frameIntervalMs)
            }
            val now = withFrameMillis { it }
            val dt = ((now - lastMs) / 1000f).coerceIn(0f, 0.08f)
            lastMs = now

            val targetScrub = if (isScrubbing) 1f else 0f
            val scrubRate = if (isScrubbing) 10f else 5.2f
            state.scrubMix += (targetScrub - state.scrubMix) * (1f - exp(-scrubRate * dt))

            val targetSettle = if (isPlaying || isScrubbing) 1f else 0f
            val settleRate = if (isPlaying || isScrubbing) 5.8f else 2.8f
            state.settleMix += (targetSettle - state.settleMix) * (1f - exp(-settleRate * dt))

            val calmPlayDrive = 0.82f
            val scrubDrive = 2.15f
            val activeDrive = (calmPlayDrive + (scrubDrive - calmPlayDrive) * state.scrubMix) * state.settleMix
            val silkDrive = 1.48f * state.settleMix
            state.flowPhase += dt * (0.95f * activeDrive)
            state.driftPhase += dt * (0.44f * activeDrive)
            state.silkPhase += dt * (0.95f * silkDrive)
            if (persistSessionClock) {
                MiniPlayerAuroraSessionClock.flowPhase = state.flowPhase
                MiniPlayerAuroraSessionClock.driftPhase = state.driftPhase
                MiniPlayerAuroraSessionClock.silkPhase = state.silkPhase
            }

            if (!isPlaying && !isScrubbing && state.settleMix < 0.003f && state.scrubMix < 0.003f) {
                state.settleMix = 0f
                state.scrubMix = 0f
                break
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
    val silkSecondaryPath = Path()
    val pathMeasure = PathMeasure()
}

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction

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
 * [MiniPlayerTimelineStyle.AURORA_SILK] keeps the pill background clean frosted glass.
 */
internal fun DrawScope.drawMiniPlayerTimelineUnderlay(
    style: MiniPlayerTimelineStyle,
    progress: Float,
    scrubbing: Boolean,
    phase: MiniPlayerAuroraPhaseState,
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

    // Calm slow wave during normal playback, lively original frequency on touch-and-hold scrub,
    // and smoothly settled straight horizon when paused.
    val scrubMix = phase.scrubMix
    val settleMix = phase.settleMix
    val flowPhase = phase.flowPhase
    val driftPhase = phase.driftPhase

    val primaryCycles = lerpFloat(0.84f, 1.15f, scrubMix)
    val secondaryCycles = lerpFloat(1.65f, 2.25f, scrubMix)
    val secondaryWeight = lerpFloat(0.24f, 0.42f, scrubMix)
    val frontAmp = lerpFloat(4.5f, 7.5f, scrubMix).dp.toPx() * edgeTaper * settleMix

    paths.waveAreaPath.rewind()
    paths.frontEdgePath.rewind()
    paths.waveAreaPath.moveTo(0f, 0f)

    val frontSamples = 16
    for (i in 0..frontSamples) {
        val uY = i.toFloat() / frontSamples
        val y = uY * h
        val ripple = (sin(uY * PI * 2.0 * primaryCycles + flowPhase) +
            secondaryWeight * cos(uY * PI * 2.0 * secondaryCycles - flowPhase * 0.72f)).toFloat()
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

    val washBoost = 1f + 0.28f * scrubMix
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
            val cloudCenterY = h * (0.48f + 0.14f * sin(driftPhase * 0.85f) * settleMix)
            drawNebulaCloud(
                color = aurora.highlight,
                center = Offset(baseX * 0.88f, cloudCenterY),
                radius = h * 1.15f,
                alpha = (if (isDark) 0.28f else 0.20f) * washBoost,
            )

            // 3. Two delicate flowing silk filaments inside the played aurora tide (settle softly on pause)
            val silkSamples = 26
            val spanW = (baseX + frontAmp * 1.5f).coerceAtLeast(12f)
            val silkMotionScale = 0.15f + 0.85f * settleMix
            for (band in 0 until 2) {
                paths.silkPath.rewind()
                val bandColor = if (band == 0) aurora.primary else aurora.filament
                val baseBandY = if (band == 0) h * 0.56f else h * 0.42f
                val bandSpeed = if (band == 0) 0.92f else -1.18f
                val bandAmp = h * (if (band == 0) 0.14f else 0.10f) * silkMotionScale
                for (i in 0..silkSamples) {
                    val u = i.toFloat() / silkSamples
                    val x = u * spanW
                    val env = sin(PI * u).toFloat().coerceAtLeast(0f).pow(0.85f)
                    val w1 = sin(u * PI * lerpFloat(2.1f, 3.0f, scrubMix) + flowPhase * bandSpeed + band * 1.9f).toFloat()
                    val y = baseBandY + w1 * bandAmp * env
                    if (i == 0) {
                        paths.silkPath.moveTo(x, y)
                    } else {
                        paths.silkPath.lineTo(x, y)
                    }
                }
                drawPath(
                    path = paths.silkPath,
                    color = bandColor.copy(alpha = (if (band == 0) 0.20f else 0.28f) * washBoost),
                    style = Stroke(
                        width = (if (band == 0) 3.0f else 1.2f).dp.toPx(),
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
                color = aurora.primary.copy(alpha = if (scrubbing) 0.45f else lerpFloat(0.18f, 0.28f, settleMix)),
                style = Stroke(width = lerpFloat(2.6f, 4.2f, scrubMix).dp.toPx(), cap = StrokeCap.Round),
            )
            drawPath(
                path = paths.frontEdgePath,
                color = lerp(aurora.filament, Color.White, 0.35f).copy(
                    alpha = if (scrubbing) 0.72f else lerpFloat(0.32f, 0.48f, settleMix),
                ),
                style = Stroke(width = 1.1.dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }
}

/**
 * Draws the MiniPlayer progress overlay after [drawContent]:
 * - [MiniPlayerTimelineStyle.OG_CIRCLE]: 3-layer perimeter glow stroke.
 * - [MiniPlayerTimelineStyle.AURORA_WAVE]: crisp bottom shoreline & starlight crest complementing the tide.
 * - [MiniPlayerTimelineStyle.AURORA_SILK]: multi-layered luminous aurora silk ribbon hugging the pill's
 *   bottom edge from 0 -> progress and ending in a soft glowing starlight playhead orb.
 */
internal fun DrawScope.drawMiniPlayerTimelineOverlay(
    style: MiniPlayerTimelineStyle,
    progress: Float,
    scrubbing: Boolean,
    phase: MiniPlayerAuroraPhaseState,
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
            drawAuroraBottomShoreline(
                clampedProgress = clampedProgress,
                scrubbing = scrubbing,
                phase = phase,
                aurora = aurora,
                isInline = isInline,
                isDedicatedSilkStyle = false,
                paths = paths,
            )
        }

        MiniPlayerTimelineStyle.AURORA_SILK -> {
            drawAuroraBottomShoreline(
                clampedProgress = clampedProgress,
                scrubbing = scrubbing,
                phase = phase,
                aurora = aurora,
                isInline = isInline,
                isDedicatedSilkStyle = true,
                paths = paths,
            )
        }
    }
}

private fun DrawScope.drawAuroraBottomShoreline(
    clampedProgress: Float,
    scrubbing: Boolean,
    phase: MiniPlayerAuroraPhaseState,
    aurora: ChordWaveAuroraPalette,
    isInline: Boolean,
    isDedicatedSilkStyle: Boolean,
    paths: MiniPlayerTimelinePaths,
) {
    updateCapsulePath(paths.capsulePath, insetPx = 0f)
    val w = size.width
    val h = size.height
    val r = h / 2f
    val edgeInset = (if (isInline) 1.6f else 2.0f).dp.toPx()
    val rInner = (r - edgeInset).coerceAtLeast(1f)
    val startX = (r * 0.28f).coerceAtMost(w * 0.16f)
    val maxEndX = (w - startX).coerceAtLeast(startX + 1f)
    val xEnd = (startX + clampedProgress * (maxEndX - startX)).coerceIn(startX + 0.5f, maxEndX)

    val settleMix = phase.settleMix
    val silkPhase = phase.silkPhase

    val shoreSamples = 32
    val waveAmp = (if (isDedicatedSilkStyle) 1.95f else 1.55f).dp.toPx() * settleMix
    val edgeTaper = sin(PI * clampedProgress).toFloat().coerceAtLeast(0f).pow(0.45f)
    val cycles = 2.2f

    fun capsuleBottomY(x: Float): Float {
        return when {
            x < r -> {
                val dx = (r - x).coerceAtMost(rInner)
                r + sqrt((rInner * rInner - dx * dx).coerceAtLeast(0f))
            }
            x > w - r -> {
                val dx = (x - (w - r)).coerceAtMost(rInner)
                r + sqrt((rInner * rInner - dx * dx).coerceAtLeast(0f))
            }
            else -> h - edgeInset
        }
    }

    paths.silkPath.rewind()
    if (isDedicatedSilkStyle) paths.silkSecondaryPath.rewind()
    var endY = capsuleBottomY(xEnd)

    for (i in 0..shoreSamples) {
        val u = i.toFloat() / shoreSamples
        val x = startX + u * (xEnd - startX)
        val cornerRamp = (r * 0.72f).coerceAtLeast(1f)
        val arcWeight = ((x - startX) / cornerRamp).coerceIn(0f, 1f) *
            ((maxEndX - x) / cornerRamp).coerceIn(0f, 1f)
        val baseY = capsuleBottomY(x)
        val env = sin(PI * u * 0.85f).toFloat().coerceAtLeast(0f) * edgeTaper * arcWeight

        val primaryWave = sin(u * PI * cycles - silkPhase * 1.15f).toFloat()
        val y = (baseY - primaryWave * waveAmp * env).coerceIn(edgeInset, h - edgeInset * 0.55f)
        if (i == 0) {
            paths.silkPath.moveTo(x, y)
        } else {
            paths.silkPath.lineTo(x, y)
        }

        if (isDedicatedSilkStyle) {
            val veilWave = sin(u * PI * (cycles * 0.78f) + silkPhase * 0.85f + 1.55f).toFloat()
            val veilY = (baseY - edgeInset * 0.35f - veilWave * waveAmp * 1.35f * env)
                .coerceIn(edgeInset, h - edgeInset * 0.45f)
            if (i == 0) {
                paths.silkSecondaryPath.moveTo(x, veilY)
            } else {
                paths.silkSecondaryPath.lineTo(x, veilY)
            }
        }

        if (i == shoreSamples) endY = y
    }

    clipPath(paths.capsulePath) {
        // Layer 1: Diffused multi-stop aurora aura hugging the bottom glass rim
        drawPath(
            path = paths.silkPath,
            brush = Brush.horizontalGradient(
                0.00f to aurora.secondary.copy(alpha = if (scrubbing) 0.42f else 0.22f),
                0.65f to aurora.primary.copy(alpha = if (scrubbing) 0.65f else if (isDedicatedSilkStyle) 0.44f else 0.36f),
                1.00f to aurora.highlight.copy(alpha = if (scrubbing) 0.75f else if (isDedicatedSilkStyle) 0.54f else 0.42f),
                startX = startX,
                endX = xEnd.coerceAtLeast(startX + 1f),
            ),
            style = Stroke(
                width = (if (isDedicatedSilkStyle) {
                    if (isInline) 3.8f else 4.8f
                } else {
                    if (isInline) 2.8f else 3.4f
                }).dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )

        // Layer 2 (Aurora Silk): Counter-weaving secondary translucent silk ribbon
        if (isDedicatedSilkStyle) {
            drawPath(
                path = paths.silkSecondaryPath,
                brush = Brush.horizontalGradient(
                    0.00f to aurora.primary.copy(alpha = if (scrubbing) 0.40f else 0.24f),
                    0.70f to aurora.highlight.copy(alpha = if (scrubbing) 0.72f else 0.48f),
                    1.00f to aurora.filament.copy(alpha = if (scrubbing) 0.84f else 0.58f),
                    startX = startX,
                    endX = xEnd.coerceAtLeast(startX + 1f),
                ),
                style = Stroke(
                    width = (if (isInline) 1.75f else 2.25f).dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }

        // Layer 3: Tapered pearlescent core shoreline filament
        drawPath(
            path = paths.silkPath,
            brush = Brush.horizontalGradient(
                0.00f to aurora.secondary.copy(alpha = if (isDedicatedSilkStyle) 0.34f else 0.25f),
                0.58f to aurora.highlight.copy(alpha = if (scrubbing) 0.94f else if (isDedicatedSilkStyle) 0.82f else 0.72f),
                1.00f to lerp(aurora.filament, Color.White, 0.58f).copy(alpha = if (scrubbing) 1f else 0.94f),
                startX = startX,
                endX = xEnd.coerceAtLeast(startX + 1f),
            ),
            style = Stroke(
                width = (if (isInline) 1.15f else 1.45f).dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )

        // Layer 4: Soft glowing starlight playhead dot marking the live position
        val crestCenter = Offset(xEnd, endY)
        drawNebulaCloud(
            color = aurora.highlight,
            center = crestCenter,
            radius = (if (scrubbing) 9.2f else if (isInline) 6.0f else 7.2f).dp.toPx(),
            alpha = if (scrubbing) 0.85f else if (isDedicatedSilkStyle) 0.62f else 0.55f,
        )
        if (isDedicatedSilkStyle) {
            drawCircle(
                color = aurora.filament.copy(alpha = if (scrubbing) 0.82f else 0.60f),
                radius = (if (scrubbing) 3.2f else if (isInline) 2.2f else 2.6f).dp.toPx(),
                center = crestCenter,
            )
        }
        drawCircle(
            color = Color.White.copy(alpha = if (scrubbing) 1f else 0.94f),
            radius = (if (scrubbing) 2.1f else if (isInline) 1.35f else 1.6f).dp.toPx(),
            center = crestCenter,
        )
    }
}
