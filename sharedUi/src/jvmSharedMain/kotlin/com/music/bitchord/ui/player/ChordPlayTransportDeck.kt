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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.NerdStats
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.settings.PlayerElasticConfig
import com.music.bitchord.data.settings.PlayerWaveStyle
import com.music.bitchord.data.settings.TrackAudioWaveform
import com.music.bitchord.sharedui.resources.Res
import com.music.bitchord.sharedui.resources.audio_output
import com.music.bitchord.sharedui.resources.ic_player_next
import com.music.bitchord.sharedui.resources.ic_player_pause
import com.music.bitchord.sharedui.resources.ic_player_play
import com.music.bitchord.sharedui.resources.ic_player_previous
import com.music.bitchord.sharedui.resources.pause
import com.music.bitchord.sharedui.resources.play
import com.music.bitchord.sharedui.resources.repeat_all
import com.music.bitchord.sharedui.resources.repeat_off
import com.music.bitchord.sharedui.resources.repeat_one
import com.music.bitchord.sharedui.resources.shuffle_off
import com.music.bitchord.sharedui.resources.shuffle_on
import com.music.bitchord.sharedui.resources.widget_next
import com.music.bitchord.sharedui.resources.widget_previous
import com.music.bitchord.ui.components.OdometerText
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import com.music.bitchord.ui.icons.BitChordIcons
import com.music.bitchord.ui.rememberIsForeground
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

private class RippleWave(
    val id: Long,
    val anim: Animatable<Float, AnimationVector1D> = Animatable(0f),
)

private class WaveformScrubberPaths {
    val upperPath = Path()
    val upperFillPath = Path()
    val veilPath = Path()
    val lowerPath = Path()
}

private class AuroraScrubberClockState(
    initialSettle: Float,
    initialScrub: Float,
) {
    var flowPhase by mutableFloatStateOf(0.7f)
    var silkPhase by mutableFloatStateOf(1.4f)
    var settleMix by mutableFloatStateOf(initialSettle)
    var scrubMix by mutableFloatStateOf(initialScrub)
}

@Composable
private fun rememberAuroraScrubberClock(
    enabled: Boolean,
    isPlaying: Boolean,
    isDragging: Boolean,
    reduceAnimation: Boolean,
    idleFrameMs: Long = 0L,
): AuroraScrubberClockState {
    val state = remember {
        AuroraScrubberClockState(
            initialSettle = if (isPlaying || isDragging) 1f else 0f,
            initialScrub = if (isDragging) 1f else 0f,
        )
    }

    LaunchedEffect(enabled, isPlaying, isDragging, reduceAnimation, idleFrameMs) {
        if (!enabled) return@LaunchedEffect
        if (reduceAnimation) {
            state.settleMix = if (isPlaying || isDragging) 1f else 0f
            state.scrubMix = if (isDragging) 1f else 0f
            return@LaunchedEffect
        }

        var lastMs = withFrameMillis { it }
        while (true) {
            val frameIntervalMs = if (isDragging || state.scrubMix > 0.01f) 0L else idleFrameMs
            if (frameIntervalMs > 0L) {
                delay(frameIntervalMs)
            }
            val now = withFrameMillis { it }
            val dt = ((now - lastMs) / 1000f).coerceIn(0f, 0.08f)
            lastMs = now

            val targetScrub = if (isDragging) 1f else 0f
            val scrubRate = if (isDragging) 11f else 5.5f
            state.scrubMix += (targetScrub - state.scrubMix) * (1f - exp(-scrubRate * dt))

            val targetSettle = if (isPlaying || isDragging) 1f else 0f
            val settleRate = if (isPlaying || isDragging) 5.5f else 3.0f
            state.settleMix += (targetSettle - state.settleMix) * (1f - exp(-settleRate * dt))

            val calmSpeed = 1.05f
            val scrubSpeed = 2.65f
            val speed = (calmSpeed + (scrubSpeed - calmSpeed) * state.scrubMix) * state.settleMix
            state.flowPhase += dt * speed
            state.silkPhase += dt * (speed * 1.28f)

            if (!isPlaying && !isDragging && state.settleMix < 0.003f && state.scrubMix < 0.003f) {
                state.settleMix = 0f
                state.scrubMix = 0f
                break
            }
        }
    }
    return state
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return "%d:%02d".format(Locale.ROOT, minutes, seconds)
}

/**
 * Computes a song-specific, deterministic artistic energy curve across 96 points.
 * Used for [PlayerWaveStyle.OG_WAVE] and [PlayerWaveStyle.AURORA_SILK].
 */
@Composable
private fun rememberSongWaveform(trackKey: String, durationMs: Long): FloatArray {
    return remember(trackKey, durationMs) {
        val seed = (trackKey.hashCode().toLong() shl 16) xor durationMs
        val rng = Random(seed)
        val numPeaks = 4 + (rng.nextInt(4))
        val peakCenters = FloatArray(numPeaks) {
            0.10f + 0.80f * (it.toFloat() / (numPeaks - 1)) + (rng.nextFloat() * 0.08f - 0.04f)
        }
        val peakWidths = FloatArray(numPeaks) { 0.05f + rng.nextFloat() * 0.045f }
        val peakHeights = FloatArray(numPeaks) { 0.40f + rng.nextFloat() * 0.55f }
        val secondaryFrequencies = FloatArray(3) { 8f + rng.nextFloat() * 12f }
        val secondaryPhases = FloatArray(3) { rng.nextFloat() * (2f * PI).toFloat() }

        val samples = 96
        FloatArray(samples + 1) { i ->
            val t = i.toFloat() / samples
            var energy = 0.08f
            for (p in 0 until numPeaks) {
                val dx = (t - peakCenters[p]) / peakWidths[p]
                energy += peakHeights[p] * exp(-dx * dx)
            }
            for (s in 0 until 3) {
                energy += 0.04f * sin(t * secondaryFrequencies[s] + secondaryPhases[s])
            }
            val taper = sin((t * PI).toFloat()).coerceIn(0f, 1f)
            (energy * (0.35f + 0.65f * taper)).coerceIn(0.06f, 1.0f)
        }
    }
}

/**
 * Multi-style waveform seekbar for WavePlay mode:
 * - [PlayerWaveStyle.OG_WAVE]: Classic Synesthesia multi-peak wave scrubber
 * - [PlayerWaveStyle.AURORA_SILK]: Layered volumetric artwork silk ribbons + starlight playhead
 * - [PlayerWaveStyle.AUDIO_GRAPH]: Measured audio waveform spectral graph + live pulse
 */
@Composable
fun SynesthesiaWaveformScrubber(
    shown: () -> Float,
    durationMs: Long,
    isPlaying: Boolean,
    onScrub: (Float) -> Unit,
    onScrubFinished: () -> Unit,
    trackKey: String = "",
    palette: MeshPalette = remember { MeshPalette(emptyList()) },
    modifier: Modifier = Modifier,
    showTimestamps: Boolean = true,
    liveWaveMotion: Boolean = false,
    scrubEnabled: Boolean = true,
    styleOverride: PlayerWaveStyle? = null,
    audioWaveformOverride: TrackAudioWaveform? = null,
    liveAudioLevelOverride: Float? = null,
) {
    val progress = shown().coerceIn(0f, 1f)
    val haptics = rememberHaptics()
    var isDragging by remember { mutableStateOf(false) }
    var lastScrubSec by remember { mutableLongStateOf(-1L) }
    val reduceAnimation by PlayerSettings.reduceAnimation.collectAsStateWithLifecycle()
    val disableInfiniteLoops by PlayerSettings.disableInfiniteLoops.collectAsStateWithLifecycle()
    val idleFrameMs by PlayerSettings.idleAnimationFrameMs.collectAsStateWithLifecycle()
    val stopInfinite = reduceAnimation || disableInfiniteLoops
    val persistedWaveStyle by PlayerSettings.playerWaveStyle.collectAsStateWithLifecycle()
    val effectiveStyle = if (liveWaveMotion) PlayerWaveStyle.OG_WAVE else (styleOverride ?: persistedWaveStyle)

    val liveTrackWaveform by PlayerSettings.activeTrackWaveform.collectAsStateWithLifecycle()
    val activeAudioWave = audioWaveformOverride
        ?: liveTrackWaveform?.takeIf { it.trackId == trackKey }
    val livePcmLevel by PlayerSettings.liveAudioLevel.collectAsStateWithLifecycle()
    val effectiveLiveLevel = if (isPlaying && !stopInfinite) {
        (liveAudioLevelOverride ?: livePcmLevel).coerceIn(0f, 1f)
    } else {
        0f
    }

    // Lifted Artwork Light-Color Palette (readable across both dark and bright covers)
    val accents = rememberLiftedArtworkAccents(palette)
    val accentColor = accents.primary
    val secondaryAccent = accents.secondary
    val highlightAccent = accents.highlight
    val filamentAccent = accents.filament

    val dragScale by animateFloatAsState(
        targetValue = if (isDragging) 1.24f else 1f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessMedium),
        label = "WaveDragScale",
    )

    // OG Wave / Live Station phase
    val liveMotionAmp by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.24f,
        animationSpec = tween(420),
        label = "LiveWaveAmp",
    )

    val isForeground = rememberIsForeground()
    val livePhase = if (!liveWaveMotion && effectiveStyle == PlayerWaveStyle.OG_WAVE && isPlaying && isForeground && !stopInfinite) {
        val infinite = rememberInfiniteTransition(label = "WaveBreath")
        val wavePhase by infinite.animateFloat(
            initialValue = 0f,
            targetValue = (2f * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "WavePhase",
        )
        wavePhase
    } else {
        0f
    }

    var liveWavePhase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(liveWaveMotion, isPlaying, stopInfinite, idleFrameMs) {
        if (!liveWaveMotion || !isPlaying || stopInfinite) return@LaunchedEffect
        var lastNanos = withFrameNanos { it }
        val omega = (2.0 * PI / 2.6).toFloat()
        while (true) {
            if (idleFrameMs > 0L) {
                delay(idleFrameMs)
            }
            val now = withFrameNanos { it }
            val dtSec = ((now - lastNanos).coerceIn(0L, 100_000_000L)) / 1_000_000_000f
            lastNanos = now
            liveWavePhase = (liveWavePhase + omega * dtSec) % (2f * PI).toFloat()
        }
    }

    // Aurora Silk zero-seam integrated phase physics
    val auroraClock = rememberAuroraScrubberClock(
        enabled = effectiveStyle != PlayerWaveStyle.OG_WAVE && !liveWaveMotion,
        isPlaying = isPlaying,
        isDragging = isDragging,
        reduceAnimation = stopInfinite,
        idleFrameMs = idleFrameMs,
    )

    // Smoothed bar array for Audio Graph so real PCM bins glide in seamlessly as decode completes
    val displayBars = remember(trackKey) { FloatArray(AUDIO_GRAPH_BARS) { AUDIO_GRAPH_PENDING_LEVEL } }
    LaunchedEffect(activeAudioWave, trackKey, effectiveStyle, reduceAnimation) {
        if (effectiveStyle != PlayerWaveStyle.AUDIO_GRAPH) return@LaunchedEffect
        val src = activeAudioWave?.bars
        val measuredFrac = activeAudioWave?.measuredFraction ?: 0f
        if (reduceAnimation || src == null) {
            for (i in 0 until AUDIO_GRAPH_BARS) {
                val u = i.toFloat() / (AUDIO_GRAPH_BARS - 1).coerceAtLeast(1)
                displayBars[i] = if (src != null && u <= measuredFrac) {
                    sampleBarsInterpolated(src, u)
                } else {
                    AUDIO_GRAPH_PENDING_LEVEL
                }
            }
            return@LaunchedEffect
        }
        var lastMs = withFrameMillis { it }
        while (true) {
            val now = withFrameMillis { it }
            val dt = ((now - lastMs) / 1000f).coerceIn(0f, 0.05f)
            lastMs = now
            val mix = 1f - exp(-10f * dt)
            var maxDelta = 0f
            for (i in 0 until AUDIO_GRAPH_BARS) {
                val u = i.toFloat() / (AUDIO_GRAPH_BARS - 1).coerceAtLeast(1)
                val target = if (u <= measuredFrac) sampleBarsInterpolated(src, u) else AUDIO_GRAPH_PENDING_LEVEL
                val next = displayBars[i] + (target - displayBars[i]) * mix
                maxDelta = maxOf(maxDelta, abs(target - next))
                displayBars[i] = next
            }
            if (maxDelta < 0.002f) {
                for (i in 0 until AUDIO_GRAPH_BARS) {
                    val u = i.toFloat() / (AUDIO_GRAPH_BARS - 1).coerceAtLeast(1)
                    displayBars[i] = if (u <= measuredFrac) sampleBarsInterpolated(src, u) else AUDIO_GRAPH_PENDING_LEVEL
                }
                break
            }
        }
    }

    val songWave = rememberSongWaveform(trackKey, if (liveWaveMotion) 0L else durationMs)
    val paths = remember { WaveformScrubberPaths() }

    Column(modifier = modifier.fillMaxWidth()) {
        val gestureModifier = if (scrubEnabled) {
            Modifier
                .pointerInput(durationMs, scrubEnabled) {
                    val edge = 6.8.dp.toPx()
                    fun fractionFor(x: Float): Float {
                        val usable = (size.width.toFloat() - 2f * edge).coerceAtLeast(1f)
                        return ((x - edge) / usable).coerceIn(0f, 1f)
                    }
                    detectTapGestures { offset ->
                        onScrub(fractionFor(offset.x))
                        onScrubFinished()
                    }
                }
                .pointerInput(durationMs, scrubEnabled) {
                    val edge = 6.8.dp.toPx()
                    fun fractionFor(x: Float): Float {
                        val usable = (size.width.toFloat() - 2f * edge).coerceAtLeast(1f)
                        return ((x - edge) / usable).coerceIn(0f, 1f)
                    }
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            onScrub(fractionFor(offset.x))
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val frac = fractionFor(change.position.x)
                            if (durationMs > 0L) {
                                val sec = (frac * durationMs / 1000L).toLong()
                                if (sec != lastScrubSec) {
                                    lastScrubSec = sec
                                    haptics.play(Haptic.Tick)
                                }
                            }
                            onScrub(frac)
                        },
                        onDragEnd = {
                            isDragging = false
                            lastScrubSec = -1L
                            onScrubFinished()
                        },
                        onDragCancel = {
                            isDragging = false
                            lastScrubSec = -1L
                            onScrubFinished()
                        },
                    )
                }
        } else {
            Modifier
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .then(gestureModifier),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                when (effectiveStyle) {
                    PlayerWaveStyle.OG_WAVE -> drawOgWaveScrubber(
                        progress = progress,
                        isPlaying = isPlaying,
                        isDragging = isDragging,
                        dragScale = dragScale,
                        liveWaveMotion = liveWaveMotion,
                        liveMotionAmp = liveMotionAmp,
                        liveWavePhase = liveWavePhase,
                        livePhase = livePhase,
                        songWave = songWave,
                        accentColor = accentColor,
                        secondaryAccent = secondaryAccent,
                        paths = paths,
                    )

                    PlayerWaveStyle.AURORA_SILK -> drawAuroraSilkScrubber(
                        progress = progress,
                        isDragging = isDragging,
                        dragScale = dragScale,
                        clock = auroraClock,
                        songWave = songWave,
                        accents = accents,
                        paths = paths,
                    )

                    PlayerWaveStyle.AUDIO_GRAPH -> drawAudioGraphScrubber(
                        progress = progress,
                        isDragging = isDragging,
                        dragScale = dragScale,
                        displayBars = displayBars,
                        measuredFraction = activeAudioWave?.measuredFraction ?: 0f,
                        liveAudioLevel = effectiveLiveLevel,
                        clock = auroraClock,
                        accents = accents,
                    )
                }
            }
        }

        if (showTimestamps) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                OdometerText(
                    value = formatTime((progress * durationMs).toLong()),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isDragging) highlightAccent else Color.White.copy(alpha = 0.72f),
                )
                OdometerText(
                    value = formatTime(durationMs.coerceAtLeast(0L)),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.65f),
                )
            }
        }
    }
}

private fun sampleBarsInterpolated(bars: FloatArray, u: Float): Float {
    if (bars.isEmpty()) return AUDIO_GRAPH_PENDING_LEVEL
    val fIdx = (u.coerceIn(0f, 1f) * (bars.size - 1))
    val lo = fIdx.toInt().coerceIn(0, bars.size - 1)
    val hi = (lo + 1).coerceIn(0, bars.size - 1)
    val frac = fIdx - lo
    return (bars[lo] + (bars[hi] - bars[lo]) * frac).coerceIn(0.08f, 1f)
}

private fun DrawScope.drawOgWaveScrubber(
    progress: Float,
    isPlaying: Boolean,
    isDragging: Boolean,
    dragScale: Float,
    liveWaveMotion: Boolean,
    liveMotionAmp: Float,
    liveWavePhase: Float,
    livePhase: Float,
    songWave: FloatArray,
    accentColor: Color,
    secondaryAccent: Color,
    paths: WaveformScrubberPaths,
) {
    val w = size.width
    val h = size.height
    val midY = h * 0.56f
    val maxAmp = h * 0.36f * dragScale
    val orbRadius = if (isDragging) 8.5.dp.toPx() else 6.8.dp.toPx()
    val trackSpan = (w - 2f * orbRadius).coerceAtLeast(0f)
    val thumbX = (orbRadius + progress * trackSpan).coerceIn(0f, w)
    val playedRight = if (progress >= 0.999f) w else thumbX

    val steps = 96
    fun waveSample(i: Int): Float {
        if (liveWaveMotion) {
            val t = i.toFloat() / steps
            val p = liveWavePhase
            val env = 0.24f + 0.76f * sin((t * PI).toFloat()).coerceIn(0f, 1f)
            val w1 = sin(t * 10f - p) * 0.48f
            val w2 = sin(t * 19f - p * 2f) * 0.28f
            val w3 = cos(t * 7f + p) * 0.24f
            val raw = 0.44f + 0.46f * (w1 + w2 + w3) * liveMotionAmp
            return (raw * env).coerceIn(0.08f, 1.0f)
        }
        val base = if (i in songWave.indices) songWave[i] else 0.2f
        val breath = if (isPlaying) 0.05f * sin(i * 0.25f + livePhase) else 0f
        return (base + breath).coerceIn(0.05f, 1.0f)
    }

    fun waveLower(i: Int): Float {
        if (liveWaveMotion) {
            val t = i.toFloat() / steps
            val p = liveWavePhase
            val env = 0.24f + 0.76f * sin((t * PI).toFloat()).coerceIn(0f, 1f)
            val w1 = cos(t * 11f - p) * 0.46f
            val w2 = sin(t * 17f + p) * 0.30f
            val w3 = sin(t * 23f - p * 2f) * 0.24f
            val raw = 0.26f + 0.32f * (w1 + w2 + w3) * liveMotionAmp
            return (raw * env).coerceIn(0.04f, 0.72f)
        }
        val base = (if (i in songWave.indices) songWave[i] else 0.2f) * 0.45f
        val breath = if (isPlaying) 0.03f * cos(i * 0.22f - livePhase) else 0f
        return (base + breath).coerceIn(0.02f, 0.65f)
    }

    paths.upperPath.rewind()
    paths.upperFillPath.rewind()
    paths.lowerPath.rewind()

    paths.upperFillPath.moveTo(0f, midY)
    for (i in 0..steps) {
        val t = i.toFloat() / steps
        val x = t * w
        val y = midY - waveSample(i) * maxAmp
        if (i == 0) paths.upperPath.moveTo(x, y) else paths.upperPath.lineTo(x, y)
        paths.upperFillPath.lineTo(x, y)
    }
    paths.upperFillPath.lineTo(w, midY)
    paths.upperFillPath.close()

    for (i in 0..steps) {
        val t = i.toFloat() / steps
        val x = t * w
        val y = midY + waveLower(i) * maxAmp
        if (i == 0) paths.lowerPath.moveTo(x, y) else paths.lowerPath.lineTo(x, y)
    }

    if (playedRight < w) {
        clipRect(left = playedRight, top = 0f, right = w, bottom = h) {
            drawLine(
                color = Color.White.copy(alpha = 0.18f),
                start = Offset(0f, midY),
                end = Offset(w, midY),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawPath(
                path = paths.upperFillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
                    startY = midY - maxAmp,
                    endY = midY,
                ),
            )
            drawPath(
                path = paths.upperPath,
                color = Color.White.copy(alpha = 0.24f),
                style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round),
            )
            drawPath(
                path = paths.lowerPath,
                color = Color.White.copy(alpha = 0.16f),
                style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }

    if (playedRight > 0f) {
        clipRect(left = 0f, top = 0f, right = playedRight, bottom = h) {
            drawPath(
                path = paths.upperFillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.56f),
                        secondaryAccent.copy(alpha = 0.20f),
                        Color.Transparent,
                    ),
                    startY = midY - maxAmp,
                    endY = midY + 4.dp.toPx(),
                ),
            )
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(accentColor, secondaryAccent),
                    startX = 0f,
                    endX = playedRight.coerceAtLeast(1f),
                ),
                start = Offset(0f, midY),
                end = Offset(playedRight, midY),
                strokeWidth = 2.6.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawPath(
                path = paths.upperPath,
                color = accentColor.copy(alpha = 0.40f),
                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
            )
            drawPath(
                path = paths.upperPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(accentColor, secondaryAccent),
                    startX = 0f,
                    endX = playedRight.coerceAtLeast(1f),
                ),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
            )
            if (liveWaveMotion) {
                drawPath(
                    path = paths.lowerPath,
                    color = secondaryAccent.copy(alpha = 0.36f),
                    style = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accentColor.copy(alpha = 0.65f), Color.Transparent),
            center = Offset(thumbX, midY),
            radius = orbRadius * 2.6f,
        ),
        center = Offset(thumbX, midY),
        radius = orbRadius * 2.6f,
    )
    drawCircle(
        color = accentColor,
        center = Offset(thumbX, midY),
        radius = orbRadius,
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.92f),
        center = Offset(thumbX, midY),
        radius = orbRadius,
        style = Stroke(width = 1.8.dp.toPx()),
    )
}

private fun DrawScope.drawAuroraSilkScrubber(
    progress: Float,
    isDragging: Boolean,
    dragScale: Float,
    clock: AuroraScrubberClockState,
    songWave: FloatArray,
    accents: ChordPlayLiftedAccents,
    paths: WaveformScrubberPaths,
) {
    val w = size.width
    val h = size.height
    val midY = h * 0.57f
    val maxAmp = h * 0.38f * dragScale
    val orbRadius = if (isDragging) 7.8.dp.toPx() else 6.0.dp.toPx()
    val trackSpan = (w - 2f * orbRadius).coerceAtLeast(0f)
    val thumbX = (orbRadius + progress * trackSpan).coerceIn(0f, w)
    val playedRight = if (progress >= 0.999f) w else thumbX

    val scrubMix = clock.scrubMix
    val settleMix = clock.settleMix
    val flowPhase = clock.flowPhase
    val silkPhase = clock.silkPhase
    val steps = 96

    paths.upperPath.rewind()
    paths.upperFillPath.rewind()
    paths.veilPath.rewind()
    paths.lowerPath.rewind()

    paths.upperFillPath.moveTo(0f, midY)
    for (i in 0..steps) {
        val u = i.toFloat() / steps
        val x = u * w
        val env = sin(PI * u).toFloat().coerceAtLeast(0f).pow(0.68f)
        val base = if (i in songWave.indices) songWave[i] else 0.25f

        val ripple = (0.045f + 0.085f * scrubMix) * settleMix *
            (sin(u * 13f - flowPhase) + 0.45f * cos(u * 21f + silkPhase * 0.72f))
        val upperAmp = (base + ripple * env).coerceIn(0.06f, 1.05f)
        val y = (midY - upperAmp * maxAmp).coerceIn(1.dp.toPx(), h - 2.dp.toPx())
        if (i == 0) paths.upperPath.moveTo(x, y) else paths.upperPath.lineTo(x, y)
        paths.upperFillPath.lineTo(x, y)

        // Counter-weaving secondary translucent silk ribbon
        val veilOffset = (0.11f + 0.07f * scrubMix * settleMix) *
            sin(u * 11f + silkPhase * 0.85f + 1.4f) * env
        val veilAmp = (base * 0.74f + veilOffset).coerceIn(0.04f, 0.94f)
        val veilY = (midY - veilAmp * maxAmp).coerceIn(1.dp.toPx(), h - 2.dp.toPx())
        if (i == 0) paths.veilPath.moveTo(x, veilY) else paths.veilPath.lineTo(x, veilY)

        // Calm reflected shoreline ripple below centerline
        val lowRipple = 0.04f * settleMix * cos(u * 15f - silkPhase * 0.7f) * env
        val lowerAmp = (base * 0.34f + lowRipple).coerceIn(0.02f, 0.50f)
        val lowY = (midY + lowerAmp * maxAmp).coerceIn(midY, h - 1.5.dp.toPx())
        if (i == 0) paths.lowerPath.moveTo(x, lowY) else paths.lowerPath.lineTo(x, lowY)
    }
    paths.upperFillPath.lineTo(w, midY)
    paths.upperFillPath.close()

    // 1. Unplayed frosted horizon
    if (playedRight < w) {
        clipRect(left = playedRight, top = 0f, right = w, bottom = h) {
            drawLine(
                color = Color.White.copy(alpha = 0.16f),
                start = Offset(0f, midY),
                end = Offset(w, midY),
                strokeWidth = 1.4.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawPath(
                path = paths.upperFillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.09f), Color.Transparent),
                    startY = midY - maxAmp,
                    endY = midY,
                ),
            )
            drawPath(
                path = paths.veilPath,
                color = Color.White.copy(alpha = 0.13f),
                style = Stroke(width = 1.1.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            drawPath(
                path = paths.upperPath,
                color = Color.White.copy(alpha = 0.25f),
                style = Stroke(width = 1.45.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            drawPath(
                path = paths.lowerPath,
                color = Color.White.copy(alpha = 0.14f),
                style = Stroke(width = 1.05.dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }

    // 2. Played Aurora Silk volumetric curtains + pearlescent filament
    if (playedRight > 0f) {
        val endX = playedRight.coerceAtLeast(1f)
        val scrubBoost = 1f + 0.24f * scrubMix
        clipRect(left = 0f, top = 0f, right = playedRight, bottom = h) {
            drawPath(
                path = paths.upperFillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        accents.primary.copy(alpha = (0.48f * scrubBoost).coerceAtMost(0.68f)),
                        accents.secondary.copy(alpha = 0.18f),
                        Color.Transparent,
                    ),
                    startY = midY - maxAmp,
                    endY = midY + 4.dp.toPx(),
                ),
            )
            drawLine(
                brush = Brush.horizontalGradient(
                    0.00f to accents.secondary.copy(alpha = 0.72f),
                    0.65f to accents.primary,
                    1.00f to accents.highlight,
                    startX = 0f,
                    endX = endX,
                ),
                start = Offset(0f, midY),
                end = Offset(playedRight, midY),
                strokeWidth = 2.4.dp.toPx(),
                cap = StrokeCap.Round,
            )
            // Soft lower reflection filament
            drawPath(
                path = paths.lowerPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(accents.secondary.copy(alpha = 0.24f), accents.highlight.copy(alpha = 0.42f)),
                    startX = 0f,
                    endX = endX,
                ),
                style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            // Counter-weaving secondary translucent silk ribbon
            drawPath(
                path = paths.veilPath,
                brush = Brush.horizontalGradient(
                    0.00f to accents.primary.copy(alpha = 0.30f),
                    0.70f to accents.highlight.copy(alpha = 0.64f),
                    1.00f to accents.filament.copy(alpha = 0.78f),
                    startX = 0f,
                    endX = endX,
                ),
                style = Stroke(width = 1.75.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            // Outer aura halo along main ribbon
            drawPath(
                path = paths.upperPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(accents.secondary.copy(alpha = 0.30f), accents.primary.copy(alpha = 0.48f)),
                    startX = 0f,
                    endX = endX,
                ),
                style = Stroke(width = (4.4f + 1.1f * scrubMix).dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            // Main silk core gradient
            drawPath(
                path = paths.upperPath,
                brush = Brush.horizontalGradient(
                    0.00f to accents.secondary,
                    0.60f to accents.primary,
                    1.00f to accents.highlight,
                    startX = 0f,
                    endX = endX,
                ),
                style = Stroke(width = 2.15.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            // Pearlescent filament spine warming into white near playhead
            drawPath(
                path = paths.upperPath,
                brush = Brush.horizontalGradient(
                    0.00f to accents.secondary.copy(alpha = 0.24f),
                    0.65f to accents.highlight.copy(alpha = 0.76f),
                    1.00f to accents.filament.copy(alpha = 0.96f),
                    startX = 0f,
                    endX = endX,
                ),
                style = Stroke(width = 1.1.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }

    // 3. Starlight Playhead Orb
    val crest = Offset(thumbX, midY)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                accents.highlight.copy(alpha = if (isDragging) 0.78f else 0.58f),
                accents.primary.copy(alpha = 0.22f),
                Color.Transparent,
            ),
            center = crest,
            radius = orbRadius * 3.1f,
        ),
        center = crest,
        radius = orbRadius * 3.1f,
    )
    drawCircle(
        color = accents.primary,
        center = crest,
        radius = orbRadius,
    )
    drawCircle(
        color = accents.filament.copy(alpha = 0.95f),
        center = crest,
        radius = orbRadius * 0.56f,
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.96f),
        center = crest,
        radius = orbRadius,
        style = Stroke(width = 1.6.dp.toPx()),
    )
}

private fun DrawScope.drawAudioGraphScrubber(
    progress: Float,
    isDragging: Boolean,
    dragScale: Float,
    displayBars: FloatArray,
    measuredFraction: Float,
    liveAudioLevel: Float,
    clock: AuroraScrubberClockState,
    accents: ChordPlayLiftedAccents,
) {
    val w = size.width
    val h = size.height
    val midY = h * 0.52f
    val maxUp = h * 0.44f * dragScale
    val maxDown = h * 0.34f * dragScale
    val orbRadius = if (isDragging) 7.4.dp.toPx() else 5.6.dp.toPx()
    val trackSpan = (w - 2f * orbRadius).coerceAtLeast(0f)
    val thumbX = (orbRadius + progress * trackSpan).coerceIn(0f, w)
    val playedRight = if (progress >= 0.999f) w else thumbX

    // Subtle hairline centerline across full span
    drawLine(
        color = Color.White.copy(alpha = 0.14f),
        start = Offset(0f, midY),
        end = Offset(w, midY),
        strokeWidth = 1.dp.toPx(),
        cap = StrokeCap.Round,
    )

    val count = AUDIO_GRAPH_BARS
    val slotW = w / count
    val barW = (slotW * 0.56f).coerceIn(1.8.dp.toPx(), 3.6.dp.toPx())
    val corner = CornerRadius(barW / 2f, barW / 2f)
    val scrubSurge = 1f + 0.14f * clock.scrubMix

    for (i in 0 until count) {
        val u = i.toFloat() / (count - 1).coerceAtLeast(1)
        val cx = (i + 0.5f) * slotW
        val isMeasured = u <= measuredFraction && measuredFraction > 0.01f

        val dist = abs(u - progress)
        val proximity = (1f - dist / 0.075f).coerceIn(0f, 1f)
        val liveLift = if (liveAudioLevel > 0.01f) {
            liveAudioLevel * (0.26f * proximity) * (0.6f + 0.4f * clock.settleMix)
        } else {
            0f
        }

        val amp = (displayBars[i] * scrubSurge + liveLift).coerceIn(0.10f, 1.0f)
        val upH = maxOf(barW * 0.65f, amp * maxUp)
        val downH = maxOf(barW * 0.45f, amp * maxDown)
        val topY = (midY - upH).coerceAtLeast(1.dp.toPx())
        val totalH = (upH + downH).coerceAtMost(h - 2.dp.toPx())
        val leftX = cx - barW / 2f

        val barColor = if (cx <= playedRight) {
            val gradT = (cx / w.coerceAtLeast(1f)).coerceIn(0f, 1f)
            val base = lerp(accents.secondary, accents.primary, gradT)
            val nearPlayhead = (1f - dist / 0.06f).coerceIn(0f, 1f)
            val finalColor = lerp(base, accents.filament, nearPlayhead * 0.65f)
            if (isMeasured) finalColor else finalColor.copy(alpha = 0.62f)
        } else {
            val baseAlpha = if (isMeasured) 0.25f else 0.12f
            Color.White.copy(alpha = baseAlpha)
        }

        if (cx <= playedRight && proximity > 0.25f && liveAudioLevel > 0.05f) {
            drawRoundRect(
                color = accents.highlight.copy(alpha = 0.28f * proximity),
                topLeft = Offset(leftX - 0.8.dp.toPx(), topY - 1.dp.toPx()),
                size = Size(barW + 1.6.dp.toPx(), totalH + 2.dp.toPx()),
                cornerRadius = corner,
            )
        }

        drawRoundRect(
            color = barColor,
            topLeft = Offset(leftX, topY),
            size = Size(barW, totalH),
            cornerRadius = corner,
        )
    }

    // Starlight Playhead Cursor & Gem
    val crest = Offset(thumbX, midY)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                accents.highlight.copy(alpha = if (isDragging) 0.82f else 0.56f),
                accents.primary.copy(alpha = 0.20f),
                Color.Transparent,
            ),
            center = crest,
            radius = orbRadius * 3.0f,
        ),
        center = crest,
        radius = orbRadius * 3.0f,
    )
    drawCircle(
        color = accents.primary,
        center = crest,
        radius = orbRadius,
    )
    drawCircle(
        color = accents.filament,
        center = crest,
        radius = orbRadius * 0.55f,
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.96f),
        center = crest,
        radius = orbRadius,
        style = Stroke(width = 1.5.dp.toPx()),
    )
}

@Stable
internal class DeckPhysicsCoupling(
    private val scope: CoroutineScope,
    private val reduceAnimation: () -> Boolean,
) {
    // Top -> Bottom coupling (Prev / Play / Next -> Quality Pill + Bottom Row)
    var isPrevPressed by mutableStateOf(false)
    var isPlayPressed by mutableStateOf(false)
    var isNextPressed by mutableStateOf(false)
    var upperOrigin by mutableStateOf(2)
    var upperDirection by mutableFloatStateOf(0f)
    val upperImpulse = Animatable(0f)

    // Bottom -> Top coupling (Lyrics / Pill / Queue -> 5 Upper Transport Nodes)
    var isLyricsPressed by mutableStateOf(false)
    var isBottomPillPressed by mutableStateOf(false)
    var isQueuePressed by mutableStateOf(false)
    val bottomToUpper = Array(5) { Animatable(0f) }
    val bottomPushSign = FloatArray(5)

    fun triggerUpperAction(origin: Int, strength: Float = 1f, directionalSign: Float = 0f) {
        if (reduceAnimation()) return
        upperOrigin = origin
        upperDirection = directionalSign
        scope.launch {
            delay(24L)
            upperImpulse.stop()
            upperImpulse.snapTo(0f)
            upperImpulse.animateTo(
                targetValue = strength,
                animationSpec = tween(durationMillis = 54, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)),
            )
            upperImpulse.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.62f,
                    stiffness = 540f,
                ),
            )
        }
    }

    fun triggerBottomAction(origin: Int, strength: Float = 1f) {
        if (reduceAnimation()) return
        val originUpperIndex = when (origin) {
            0 -> 0.5f // Lyrics aligns below Shuffle(0) & Previous(1)
            2 -> 3.5f // Queue aligns below Next(3) & Repeat(4)
            else -> 2f // Quality Pill aligns directly below Play/Pause(2)
        }
        for (i in 0 until 5) {
            val d = abs(i - originUpperIndex)
            val delaySteps = when {
                d < 0.75f -> 0
                d < 1.75f -> 1
                d < 2.75f -> 2
                else -> 3
            }
            val weight = when (delaySteps) {
                0 -> 0.84f
                1 -> 0.56f
                2 -> 0.34f
                else -> 0.18f
            } * strength
            bottomPushSign[i] = when {
                abs(i - originUpperIndex) < 0.25f -> 0f
                i < originUpperIndex -> -1f
                else -> 1f
            }
            scope.launch {
                delay(22L + delaySteps * 22L)
                bottomToUpper[i].stop()
                bottomToUpper[i].snapTo(0f)
                bottomToUpper[i].animateTo(
                    targetValue = weight,
                    animationSpec = tween(durationMillis = 48, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)),
                )
                bottomToUpper[i].animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = 0.68f,
                        stiffness = 580f,
                    ),
                )
            }
        }
    }
}

internal val LocalDeckPhysicsCoupling = staticCompositionLocalOf<DeckPhysicsCoupling?> { null }

/**
 * Coupled 5-node elastic physics state across `[0=Shuffle, 1=Previous, 2=Play/Pause, 3=Next, 4=Repeat]`.
 *
 * When any control is pressed or triggered, energy ripples across neighboring controls
 * (elastic push/pull displacement, sympathetic squash/stretch, tilt, and center orb spotlight drift),
 * turning the transport row into a single cohesive organic environment.
 */
private class TransportFieldState(
    private val scope: kotlinx.coroutines.CoroutineScope,
    private val reduceAnimation: () -> Boolean,
) {
    val impulse = Array(5) { Animatable(0f) }
    val pushSign = FloatArray(5)

    fun triggerWave(origin: Int, strength: Float = 1f, directionalSign: Float = 0f) {
        if (reduceAnimation()) return
        for (i in 0 until 5) {
            val dist = abs(i - origin)
            val weight = when (dist) {
                0 -> 1.0f
                1 -> 0.72f
                2 -> 0.38f
                else -> 0.16f
            } * strength
            pushSign[i] = when {
                i == origin -> directionalSign
                directionalSign != 0f -> directionalSign
                i < origin -> -1f
                else -> 1f
            }
            scope.launch {
                if (dist > 0) delay(dist * 34L)
                impulse[i].stop()
                impulse[i].snapTo(0f)
                impulse[i].animateTo(
                    targetValue = weight,
                    animationSpec = spring(
                        dampingRatio = 0.48f,
                        stiffness = 780f,
                    ),
                )
                impulse[i].animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = 0.44f,
                        stiffness = 260f,
                    ),
                )
            }
        }
    }
}

/**
 * Modern 5-button transport deck for WavePlay mode.
 *
 * Hierarchical sizing keeps the Play/Pause orb unmistakably primary (68.dp core inside 86.dp stage),
 * skips secondary (borderless soft glow), and Shuffle/Repeat tertiary (compact 34.dp borderless frosted
 * droplets centered inside accessible 48.dp hit targets). All 5 controls participate in a coupled
 * elastic physics field where touching or toggling one organically pushes, pulls, tilts, and morphs
 * its neighbors.
 */
@Composable
fun ChordPlayTransportDeck(
    isPlaying: Boolean,
    isLoading: Boolean,
    previousEnabled: Boolean,
    nextEnabled: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    shuffleEnabled: Boolean = false,
    repeatMode: Int = RepeatModes.OFF,
    onToggleShuffle: () -> Unit = {},
    onCycleRepeat: () -> Unit = {},
    palette: MeshPalette = remember { MeshPalette(emptyList()) },
    hazeState: HazeState? = null,
    queueOpen: Boolean = false,
    isLive: Boolean = false,
    elasticOverride: PlayerElasticConfig? = null,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val reduceAnimation by PlayerSettings.reduceAnimation.collectAsStateWithLifecycle()
    val disableInfiniteLoops by PlayerSettings.disableInfiniteLoops.collectAsStateWithLifecycle()
    val persistedElastic by PlayerSettings.playerElasticConfig.collectAsStateWithLifecycle()
    val elastic = elasticOverride ?: persistedElastic

    val accents = rememberLiftedArtworkAccents(palette)
    val accentColor = accents.primary
    val secondaryAccent = accents.secondary
    val highlightAccent = accents.highlight

    val field = remember(scope) { TransportFieldState(scope) { reduceAnimation } }

    val ripples = remember { mutableStateListOf<RippleWave>() }
    var rippleCounter by remember { mutableLongStateOf(0L) }
    val triggerRipple: () -> Unit = {
        if (!reduceAnimation) {
            if (ripples.size >= 5) ripples.removeAt(0)
            val wave = RippleWave(id = ++rippleCounter)
            ripples.add(wave)
            scope.launch {
                try {
                    wave.anim.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 620, easing = CubicBezierEasing(0.12f, 1f, 0.28f, 1f)),
                    )
                } finally {
                    ripples.remove(wave)
                }
            }
        }
    }

    // Dedicated expressive icon morph states for Shuffle (crossover twist) & Repeat (orbital spin)
    val shuffleFlipAnim = remember { Animatable(1f) }
    val shuffleTwistAnim = remember { Animatable(0f) }
    val repeatSpinAnim = remember { Animatable(0f) }

    val motionScale = if (reduceAnimation || !elastic.enabled) 0f else 1f
    val crossScale = if (motionScale > 0f && elastic.playPillInteraction) 1f else 0f
    val sShuffle = motionScale * (if (elastic.shuffle) 1f else 0f)
    val sPrev = motionScale * (if (elastic.previous) 1f else 0f)
    val sPlay = motionScale * (if (elastic.playPause) 1f else 0f)
    val sNext = motionScale * (if (elastic.next) 1f else 0f)
    val sRepeat = motionScale * (if (elastic.repeat) 1f else 0f)

    val triggerShuffleMorph: () -> Unit = {
        if (sShuffle > 0f) {
            scope.launch {
                shuffleTwistAnim.stop()
                shuffleTwistAnim.snapTo(0f)
                shuffleTwistAnim.animateTo(18f, spring(dampingRatio = 0.45f, stiffness = 620f))
                shuffleTwistAnim.animateTo(0f, spring(dampingRatio = 0.52f, stiffness = 320f))
            }
            scope.launch {
                shuffleFlipAnim.stop()
                shuffleFlipAnim.snapTo(1f)
                shuffleFlipAnim.animateTo(0.28f, tween(95, easing = CubicBezierEasing(0.4f, 0f, 1f, 1f)))
                shuffleFlipAnim.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 460f))
            }
        }
    }

    val triggerRepeatSpin: () -> Unit = {
        if (sRepeat > 0f) {
            scope.launch {
                val start = repeatSpinAnim.value % 360f
                repeatSpinAnim.snapTo(start)
                repeatSpinAnim.animateTo(
                    targetValue = start + 360f,
                    animationSpec = spring(dampingRatio = 0.58f, stiffness = 290f),
                )
            }
        }
    }

    // Hold interactions for all 5 controls -> smooth live magnetic coupling while finger is down
    val shuffleInteraction = remember { MutableInteractionSource() }
    val isShufflePressed by shuffleInteraction.collectIsPressedAsState()

    val prevInteraction = remember { MutableInteractionSource() }
    val isPrevPressed by prevInteraction.collectIsPressedAsState()

    val playInteraction = remember { MutableInteractionSource() }
    val isPlayPressed by playInteraction.collectIsPressedAsState()

    val nextInteraction = remember { MutableInteractionSource() }
    val isNextPressed by nextInteraction.collectIsPressedAsState()

    val repeatInteraction = remember { MutableInteractionSource() }
    val isRepeatPressed by repeatInteraction.collectIsPressedAsState()

    val deckCoupling = LocalDeckPhysicsCoupling.current
    LaunchedEffect(
        deckCoupling,
        isPrevPressed,
        isPlayPressed,
        isNextPressed,
        crossScale,
        sPrev,
        sPlay,
        sNext,
    ) {
        deckCoupling?.let {
            it.isPrevPressed = isPrevPressed && crossScale > 0f && sPrev > 0f
            it.isPlayPressed = isPlayPressed && crossScale > 0f && sPlay > 0f
            it.isNextPressed = isNextPressed && crossScale > 0f && sNext > 0f
        }
    }
    DisposableEffect(deckCoupling) {
        onDispose {
            deckCoupling?.let {
                it.isPrevPressed = false
                it.isPlayPressed = false
                it.isNextPressed = false
            }
        }
    }

    val repeatActive = repeatMode != RepeatModes.OFF

    // Live press hold strengths (0..1)
    val holdSpring = spring<Float>(dampingRatio = 0.62f, stiffness = Spring.StiffnessMedium)
    val holdShuffle by animateFloatAsState(if (isShufflePressed && sShuffle > 0f) 1f else 0f, holdSpring, label = "HoldShuffle")
    val holdPrev by animateFloatAsState(if (isPrevPressed && sPrev > 0f) 1f else 0f, holdSpring, label = "HoldPrev")
    val holdPlay by animateFloatAsState(if (isPlayPressed && sPlay > 0f) 1f else 0f, holdSpring, label = "HoldPlay")
    val holdNext by animateFloatAsState(if (isNextPressed && sNext > 0f) 1f else 0f, holdSpring, label = "HoldNext")
    val holdRepeat by animateFloatAsState(if (isRepeatPressed && sRepeat > 0f) 1f else 0f, holdSpring, label = "HoldRepeat")

    // Bottom -> Top live holds & impulses from Lyrics / Quality Pill / Queue
    val bHoldLyrics by animateFloatAsState(
        targetValue = if (deckCoupling?.isLyricsPressed == true && crossScale > 0f) 1f else 0f,
        animationSpec = holdSpring,
        label = "BottomHoldLyrics",
    )
    val bHoldPill by animateFloatAsState(
        targetValue = if (deckCoupling?.isBottomPillPressed == true && crossScale > 0f) 1f else 0f,
        animationSpec = holdSpring,
        label = "BottomHoldPill",
    )
    val bHoldQueue by animateFloatAsState(
        targetValue = if (deckCoupling?.isQueuePressed == true && crossScale > 0f) 1f else 0f,
        animationSpec = holdSpring,
        label = "BottomHoldQueue",
    )

    val prevGlowAlpha by animateFloatAsState(if (isPrevPressed) 1f else 0f, tween(140), label = "PrevGlow")
    val nextGlowAlpha by animateFloatAsState(if (isNextPressed) 1f else 0f, tween(140), label = "NextGlow")

    val shuffleActiveGlow by animateFloatAsState(
        targetValue = if (!isLive && shuffleEnabled) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessMediumLow),
        label = "ShuffleActiveGlow",
    )
    val repeatActiveGlow by animateFloatAsState(
        targetValue = if (!isLive && repeatActive) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessMediumLow),
        label = "RepeatActiveGlow",
    )

    val playMorphProgress by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessMedium),
        label = "PlayMorphProgress",
    )

    val sideButtonsAlpha by animateFloatAsState(
        targetValue = if (queueOpen) 0f else 1f,
        animationSpec = tween(durationMillis = 280),
        label = "DeckSideAlpha",
    )
    val sideButtonsWidth by animateDpAsState(
        targetValue = if (queueOpen) 0.dp else 48.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "DeckSideWidth",
    )
    val skipButtonSize by animateDpAsState(
        targetValue = if (queueOpen) 64.dp else 52.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "DeckSkipButtonSize",
    )
    val skipIconSize by animateDpAsState(
        targetValue = if (queueOpen) 36.dp else 28.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "DeckSkipIconSize",
    )

    val isForeground = rememberIsForeground()
    val orbActive = isPlaying && isForeground && !reduceAnimation && !disableInfiniteLoops
    val orbPhaseState: State<Float>
    val breathScaleState: State<Float>
    if (orbActive) {
        val infinite = rememberInfiniteTransition(label = "OrbContours")
        orbPhaseState = infinite.animateFloat(
            initialValue = 0f,
            targetValue = (2f * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 6500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "OrbPhase",
        )
        breathScaleState = infinite.animateFloat(
            initialValue = 0.978f,
            targetValue = 1.032f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2200),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "OrbBreath",
        )
    } else {
        orbPhaseState = remember { mutableFloatStateOf(0f) }
        breathScaleState = remember { mutableFloatStateOf(1f) }
    }

    val ringsAlphaState = animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(500),
        label = "OrbRingsAlpha",
    )
    val buttonDullAlpha by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.80f,
        animationSpec = tween(400),
        label = "ButtonDullAlpha",
    )

    // Precompute coupled displacements & squash-stretch for each node (all scaled by motionScale)
    val w0 = field.impulse[0].value
    val w1 = field.impulse[1].value
    val w2 = field.impulse[2].value
    val w3 = field.impulse[3].value
    val w4 = field.impulse[4].value

    val bw0 = (deckCoupling?.bottomToUpper?.get(0)?.value ?: 0f) * crossScale
    val bw1 = (deckCoupling?.bottomToUpper?.get(1)?.value ?: 0f) * crossScale
    val bw2 = (deckCoupling?.bottomToUpper?.get(2)?.value ?: 0f) * crossScale
    val bw3 = (deckCoupling?.bottomToUpper?.get(3)?.value ?: 0f) * crossScale
    val bw4 = (deckCoupling?.bottomToUpper?.get(4)?.value ?: 0f) * crossScale
    val bs0 = deckCoupling?.bottomPushSign?.get(0) ?: 0f
    val bs1 = deckCoupling?.bottomPushSign?.get(1) ?: 0f
    val bs2 = deckCoupling?.bottomPushSign?.get(2) ?: 0f
    val bs3 = deckCoupling?.bottomPushSign?.get(3) ?: 0f
    val bs4 = deckCoupling?.bottomPushSign?.get(4) ?: 0f

    // Center gravity spotlight drift across the deck (responds to side buttons and bottom dock)
    val orbFocalBias = ((holdNext * 0.45f + holdRepeat * 0.25f + bHoldQueue * 0.22f) -
        (holdPrev * 0.45f + holdShuffle * 0.25f + bHoldLyrics * 0.22f) +
        (w2 * field.pushSign[2] * 0.55f) + (bw2 * bs2 * 0.22f)) * sPlay

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 1. Shuffle — Compact 34.dp borderless frosted droplet inside 48.dp accessible hit target
        val shuffleTint by animateColorAsState(
            targetValue = when {
                isLive -> Color.White.copy(alpha = 0.24f)
                shuffleEnabled -> accents.filament
                else -> Color.White.copy(alpha = 0.74f)
            },
            animationSpec = tween(220),
            label = "ShuffleTint",
        )
        // Coupled X/Y offset: pushed left by center shockwaves, softly nudged by prev/next & bottom dock
        val shuffleShiftDp = ((-5.0f * w0 * (if (field.pushSign[0] < 0f) 1f else -0.65f)) +
            (2.2f * holdPlay) - (1.8f * holdShuffle) +
            (bs0 * 1.4f * bw0) - (0.8f * bHoldLyrics) + (0.6f * bHoldPill)) * sShuffle
        val shuffleShiftYDp = (-1.9f * bw0 + 1.0f * bHoldLyrics + 0.5f * bHoldPill) * sShuffle
        val shuffleScaleX = if (isShufflePressed && sShuffle > 0f) 0.86f else 1f + (0.20f * w0 + 0.04f * shuffleActiveGlow + 0.06f * bw0) * sShuffle
        val shuffleScaleY = if (isShufflePressed && sShuffle > 0f) 0.86f else 1f + (-0.14f * w0 + 0.04f * shuffleActiveGlow - 0.04f * bw0) * sShuffle
        val shuffleRotZ = ((field.pushSign[0] * 4.5f * w0 + bs0 * 2.2f * bw0) * if (isShufflePressed) 0f else 1f) * sShuffle

        Box(
            modifier = Modifier
                .width(sideButtonsWidth)
                .height(48.dp)
                .graphicsLayer {
                    alpha = sideButtonsAlpha
                    translationX = shuffleShiftDp.dp.toPx()
                    translationY = shuffleShiftYDp.dp.toPx()
                    rotationZ = shuffleRotZ
                    clip = false
                }
                .clickable(
                    interactionSource = shuffleInteraction,
                    indication = null,
                    enabled = !queueOpen && !isLive,
                ) {
                    haptics.play(if (shuffleEnabled) Haptic.ToggleOff else Haptic.ToggleOn)
                    if (sShuffle > 0f) {
                        triggerShuffleMorph()
                        field.triggerWave(origin = 0, strength = 0.95f, directionalSign = 1f)
                    }
                    onToggleShuffle()
                },
            contentAlignment = Alignment.Center,
        ) {
            if (sideButtonsWidth > 4.dp) {
                // Soft borderless radial artwork bloom behind active droplet
                if (!isLive && (shuffleActiveGlow > 0.02f || w0 * sShuffle > 0.02f)) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .graphicsLayer {
                                val auraScale = 0.92f + 0.22f * shuffleActiveGlow + 0.28f * (w0 * sShuffle)
                                scaleX = auraScale
                                scaleY = auraScale
                                alpha = (0.55f * shuffleActiveGlow + 0.45f * (w0 * sShuffle)).coerceIn(0f, 0.85f)
                            }
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        accentColor.copy(alpha = 0.36f),
                                        highlightAccent.copy(alpha = 0.14f),
                                        Color.Transparent,
                                    ),
                                ),
                                shape = CircleShape,
                            ),
                    )
                }

                // Pure borderless translucent glass droplet (34.dp — clearly secondary vs 68.dp Play orb)
                val dropletIdleAlpha = 0.065f * (1f - shuffleActiveGlow)
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .graphicsLayer {
                            scaleX = shuffleScaleX
                            scaleY = shuffleScaleY
                        }
                        .clip(CircleShape)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = dropletIdleAlpha + 0.22f * shuffleActiveGlow),
                                    accentColor.copy(alpha = 0.28f * shuffleActiveGlow),
                                    Color.White.copy(alpha = 0.025f + 0.08f * shuffleActiveGlow),
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = BitChordIcons.PlayerShuffle,
                        contentDescription = stringResource(if (shuffleEnabled) Res.string.shuffle_on else Res.string.shuffle_off),
                        tint = shuffleTint,
                        modifier = Modifier
                            .size(19.dp)
                            .graphicsLayer {
                                scaleY = if (sShuffle <= 0f) 1f else shuffleFlipAnim.value
                                rotationZ = if (sShuffle <= 0f) 0f else shuffleTwistAnim.value
                            },
                    )
                }
            }
        }

        // 2. Previous Track — Secondary skip control with elastic push/pull coupling
        val prevShiftDp = (if (field.pushSign[1] == -1f) {
            -6.8f * w1 // pushed left by center orb or next
        } else {
            4.8f * w1 // pushed right by shuffle or wake
        } + (2.5f * holdPlay) - (2.4f * holdPrev) - (1.8f * holdShuffle) +
            (bs1 * 1.5f * bw1) - (0.8f * bHoldLyrics) + (0.7f * bHoldPill)) * sPrev
        val prevShiftYDp = (-2.1f * bw1 + 1.0f * bHoldLyrics + 0.8f * bHoldPill) * sPrev
        val prevStretchX = if (isPrevPressed && sPrev > 0f) 0.85f else 1f + (0.12f * w1 + 0.05f * bw1) * sPrev
        val prevStretchY = if (isPrevPressed && sPrev > 0f) 0.85f else 1f + (-0.09f * w1 - 0.035f * bw1) * sPrev
        val prevRotZ = (field.pushSign[1] * 5.2f * w1 + bs1 * 2.2f * bw1) * sPrev

        Box(
            modifier = Modifier
                .size(skipButtonSize)
                .graphicsLayer {
                    translationX = prevShiftDp.dp.toPx()
                    translationY = prevShiftYDp.dp.toPx()
                    scaleX = prevStretchX
                    scaleY = prevStretchY
                    rotationZ = prevRotZ
                    clip = false
                }
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = (0.18f * prevGlowAlpha + 0.14f * (w1 * sPrev) + 0.08f * (bw1 * sPrev)).coerceIn(0f, 0.28f)),
                            Color.Transparent,
                        ),
                    ),
                )
                .clickable(
                    interactionSource = prevInteraction,
                    indication = null,
                    enabled = previousEnabled,
                ) {
                    haptics.play(Haptic.SkipPrevious)
                    if (sPrev > 0f) {
                        field.triggerWave(origin = 1, strength = 1.0f, directionalSign = -1f)
                        if (crossScale > 0f) {
                            deckCoupling?.triggerUpperAction(origin = 1, strength = 0.82f, directionalSign = -1f)
                        }
                    }
                    onPrevious()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_player_previous),
                contentDescription = stringResource(Res.string.widget_previous),
                tint = Color.White.copy(alpha = if (previousEnabled) 0.95f else 0.35f),
                modifier = Modifier.size(skipIconSize),
            )
        }

        // 3. Central Signature Synesthesia Hero Play/Pause Orb (Primary centerpiece: 68.dp core in 86.dp stage)
        val playShiftDp = ((field.pushSign[2] * 3.8f * w2) +
            (2.0f * holdNext) - (2.0f * holdPrev) + (1.0f * holdRepeat) - (1.0f * holdShuffle) +
            (bs2 * 1.4f * bw2) - (0.8f * bHoldLyrics) + (0.8f * bHoldQueue)) * sPlay
        val playShiftYDp = (-2.3f * bw2 + 1.3f * bHoldPill + 0.6f * bHoldLyrics + 0.6f * bHoldQueue) * sPlay
        val playPulseScaleX = if (isPlayPressed && sPlay > 0f) {
            0.87f
        } else {
            val selfRebound = if (field.pushSign[2] == 0f) 0.11f * w2 else -0.04f * w2
            1f + (selfRebound + 0.045f * bw2) * sPlay
        }
        val playPulseScaleY = if (isPlayPressed && sPlay > 0f) {
            0.87f
        } else {
            val selfRebound = if (field.pushSign[2] == 0f) 0.11f * w2 else -0.04f * w2
            1f + (selfRebound - 0.032f * bw2) * sPlay
        }
        val playRotZ = (field.pushSign[2] * 2.8f * w2 + bs2 * 1.5f * bw2) * sPlay

        Box(
            modifier = Modifier
                .size(86.dp)
                .graphicsLayer {
                    val liveBreathScale = breathScaleState.value
                    translationX = playShiftDp.dp.toPx()
                    translationY = playShiftYDp.dp.toPx()
                    scaleX = playPulseScaleX * liveBreathScale
                    scaleY = playPulseScaleY * liveBreathScale
                    rotationZ = playRotZ
                    alpha = buttonDullAlpha
                    clip = false
                }
                .clickable(
                    interactionSource = playInteraction,
                    indication = null,
                ) {
                    if (sPlay > 0f) {
                        triggerRipple()
                        field.triggerWave(origin = 2, strength = 1.05f, directionalSign = 0f)
                        if (crossScale > 0f) {
                            deckCoupling?.triggerUpperAction(origin = 2, strength = 1.0f, directionalSign = 0f)
                        }
                    }
                    haptics.play(if (isPlaying) Haptic.Pause else Haptic.Resume)
                    onPlayPause()
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { clip = false },
            ) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val coreR = 34.dp.toPx()
                val biasX = orbFocalBias * coreR * 0.42f

                for (wave in ripples) {
                    val p = wave.anim.value
                    val r = coreR * (1f + p * 2.15f)
                    val a = (1f - p) * (1f - p) * 0.65f
                    drawCircle(
                        color = accentColor.copy(alpha = a * 0.30f),
                        radius = r * 0.9f,
                        center = Offset(cx, cy),
                    )
                    drawCircle(
                        color = highlightAccent.copy(alpha = a),
                        radius = r,
                        center = Offset(cx, cy),
                        style = Stroke(width = 2.3.dp.toPx() * (1f - p * 0.5f)),
                    )
                }

                val ringsAlpha = ringsAlphaState.value
                // Responsive magnetic halo shifted gently toward energized side buttons
                if (ringsAlpha > 0.01f || abs(orbFocalBias) > 0.02f) {
                    val haloAlpha = maxOf(ringsAlpha, abs(orbFocalBias) * 0.55f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.46f * haloAlpha),
                                secondaryAccent.copy(alpha = 0.18f * haloAlpha),
                                Color.Transparent,
                            ),
                            center = Offset(cx + biasX, cy),
                            radius = coreR * 1.72f,
                        ),
                        center = Offset(cx + biasX, cy),
                        radius = coreR * 1.72f,
                    )
                }

                if (ringsAlpha > 0.01f && !reduceAnimation && !disableInfiniteLoops) {
                    val orbPhase = orbPhaseState.value
                    data class ContourConfig(
                        val scaleBase: Float,
                        val harmonic1: Float,
                        val harmonic2: Float,
                        val phaseMult1: Float,
                        val phaseMult2: Float,
                        val amp1: Float,
                        val amp2: Float,
                        val ringColor: Color,
                    )
                    val contourConfigs = listOf(
                        ContourConfig(
                            scaleBase = 1.06f,
                            harmonic1 = 3f, harmonic2 = 5f,
                            phaseMult1 = 1f, phaseMult2 = -1f,
                            amp1 = 0.062f, amp2 = 0.038f,
                            ringColor = secondaryAccent.copy(alpha = 0.62f * ringsAlpha),
                        ),
                        ContourConfig(
                            scaleBase = 1.11f,
                            harmonic1 = 4f, harmonic2 = 2f,
                            phaseMult1 = -1f, phaseMult2 = 2f,
                            amp1 = 0.056f, amp2 = 0.042f,
                            ringColor = accentColor.copy(alpha = 0.48f * ringsAlpha),
                        ),
                        ContourConfig(
                            scaleBase = 1.03f,
                            harmonic1 = 2f, harmonic2 = 4f,
                            phaseMult1 = -1f, phaseMult2 = 1f,
                            amp1 = 0.050f, amp2 = 0.032f,
                            ringColor = highlightAccent.copy(alpha = 0.42f * ringsAlpha),
                        ),
                    )
                    val segs = 72
                    for (cfg in contourConfigs) {
                        val path = Path()
                        for (i in 0..segs) {
                            val theta = (i.toFloat() / segs) * (2f * PI).toFloat()
                            val wobble = cfg.amp1 * sin(cfg.harmonic1 * theta + orbPhase * cfg.phaseMult1) +
                                cfg.amp2 * cos(cfg.harmonic2 * theta + orbPhase * cfg.phaseMult2)
                            val r = coreR * (cfg.scaleBase + wobble)
                            val x = cx + r * cos(theta)
                            val y = cy + r * sin(theta)
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        path.close()
                        drawPath(
                            path = path,
                            color = cfg.ringColor,
                            style = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }
                }

                // Core Orb synced with artwork palette + dynamic directional highlight drift
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.94f),
                            palette.colors.getOrElse(0) { accentColor },
                            secondaryAccent,
                        ),
                        center = Offset(cx + biasX * 0.7f, cy - coreR * 0.16f),
                        radius = coreR * 1.06f,
                    ),
                    center = Offset(cx, cy),
                    radius = coreR,
                )
                drawCircle(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.25f), Color.Transparent),
                        startY = cy - coreR,
                        endY = cy + coreR * 0.2f,
                    ),
                    center = Offset(cx, cy),
                    radius = coreR * 0.96f,
                )

                // Soft luminous rim
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.86f),
                            highlightAccent.copy(alpha = 0.70f),
                            secondaryAccent.copy(alpha = 0.90f),
                        ),
                        start = Offset(cx - coreR, cy - coreR),
                        end = Offset(cx + coreR, cy + coreR),
                    ),
                    center = Offset(cx, cy),
                    radius = coreR,
                    style = Stroke(width = 1.6.dp.toPx()),
                )
            }

            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.8.dp,
                    modifier = Modifier.size(30.dp),
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_player_pause),
                        contentDescription = stringResource(Res.string.pause),
                        tint = Color.White,
                        modifier = Modifier
                            .size(33.dp)
                            .graphicsLayer {
                                val s = 0.55f + 0.45f * playMorphProgress
                                scaleX = s
                                scaleY = s
                                rotationZ = (1f - playMorphProgress) * -70f
                                alpha = playMorphProgress
                            },
                    )
                    Icon(
                        painter = painterResource(Res.drawable.ic_player_play),
                        contentDescription = stringResource(Res.string.play),
                        tint = Color.White,
                        modifier = Modifier
                            .size(33.dp)
                            .graphicsLayer {
                                val inv = 1f - playMorphProgress
                                val s = 0.55f + 0.45f * inv
                                scaleX = s
                                scaleY = s
                                rotationZ = playMorphProgress * 70f
                                alpha = inv
                            },
                    )
                }
            }
        }

        // 4. Next Track — Mirrored coupled physics
        val nextShiftDp = (if (field.pushSign[3] == 1f) {
            6.8f * w3
        } else {
            -4.8f * w3
        } - (2.5f * holdPlay) + (2.4f * holdNext) + (1.8f * holdRepeat) +
            (bs3 * 1.5f * bw3) + (0.8f * bHoldQueue) - (0.7f * bHoldPill)) * sNext
        val nextShiftYDp = (-2.1f * bw3 + 1.0f * bHoldQueue + 0.8f * bHoldPill) * sNext
        val nextStretchX = if (isNextPressed && sNext > 0f) 0.85f else 1f + (0.12f * w3 + 0.05f * bw3) * sNext
        val nextStretchY = if (isNextPressed && sNext > 0f) 0.85f else 1f + (-0.09f * w3 - 0.035f * bw3) * sNext
        val nextRotZ = (field.pushSign[3] * 5.2f * w3 + bs3 * 2.2f * bw3) * sNext

        Box(
            modifier = Modifier
                .size(skipButtonSize)
                .graphicsLayer {
                    translationX = nextShiftDp.dp.toPx()
                    translationY = nextShiftYDp.dp.toPx()
                    scaleX = nextStretchX
                    scaleY = nextStretchY
                    rotationZ = nextRotZ
                    clip = false
                }
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = (0.18f * nextGlowAlpha + 0.14f * (w3 * sNext) + 0.08f * (bw3 * sNext)).coerceIn(0f, 0.28f)),
                            Color.Transparent,
                        ),
                    ),
                )
                .clickable(
                    interactionSource = nextInteraction,
                    indication = null,
                    enabled = nextEnabled,
                ) {
                    haptics.play(Haptic.SkipNext)
                    if (sNext > 0f) {
                        field.triggerWave(origin = 3, strength = 1.0f, directionalSign = 1f)
                        if (crossScale > 0f) {
                            deckCoupling?.triggerUpperAction(origin = 3, strength = 0.82f, directionalSign = 1f)
                        }
                    }
                    onNext()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_player_next),
                contentDescription = stringResource(Res.string.widget_next),
                tint = Color.White.copy(alpha = if (nextEnabled) 0.95f else 0.35f),
                modifier = Modifier.size(skipIconSize),
            )
        }

        // 5. Repeat / Repeat-1 — Compact 34.dp borderless frosted droplet inside 48.dp hit target
        val repeatTint by animateColorAsState(
            targetValue = when {
                isLive -> Color.White.copy(alpha = 0.24f)
                repeatActive -> accents.filament
                else -> Color.White.copy(alpha = 0.74f)
            },
            animationSpec = tween(220),
            label = "RepeatTint",
        )
        val repeatShiftDp = ((5.0f * w4 * (if (field.pushSign[4] > 0f) 1f else -0.65f)) -
            (2.2f * holdPlay) + (1.8f * holdRepeat) +
            (bs4 * 1.4f * bw4) + (0.8f * bHoldQueue) - (0.6f * bHoldPill)) * sRepeat
        val repeatShiftYDp = (-1.9f * bw4 + 1.0f * bHoldQueue + 0.5f * bHoldPill) * sRepeat
        val repeatScaleX = if (isRepeatPressed && sRepeat > 0f) 0.86f else 1f + (0.18f * w4 + 0.04f * repeatActiveGlow + 0.06f * bw4) * sRepeat
        val repeatScaleY = if (isRepeatPressed && sRepeat > 0f) 0.86f else 1f + (-0.12f * w4 + 0.04f * repeatActiveGlow - 0.04f * bw4) * sRepeat
        val repeatRotZ = ((field.pushSign[4] * 4.5f * w4 + bs4 * 2.2f * bw4) * if (isRepeatPressed) 0f else 1f) * sRepeat

        Box(
            modifier = Modifier
                .width(sideButtonsWidth)
                .height(48.dp)
                .graphicsLayer {
                    alpha = sideButtonsAlpha
                    translationX = repeatShiftDp.dp.toPx()
                    translationY = repeatShiftYDp.dp.toPx()
                    rotationZ = repeatRotZ
                    clip = false
                }
                .clickable(
                    interactionSource = repeatInteraction,
                    indication = null,
                    enabled = !queueOpen && !isLive,
                ) {
                    haptics.play(
                        when (repeatMode) {
                            RepeatModes.OFF -> Haptic.ToggleOn
                            RepeatModes.ONE -> Haptic.ToggleOff
                            else -> Haptic.Select
                        },
                    )
                    if (sRepeat > 0f) {
                        triggerRepeatSpin()
                        field.triggerWave(origin = 4, strength = 0.95f, directionalSign = -1f)
                    }
                    onCycleRepeat()
                },
            contentAlignment = Alignment.Center,
        ) {
            if (sideButtonsWidth > 4.dp) {
                if (!isLive && (repeatActiveGlow > 0.02f || w4 * sRepeat > 0.02f)) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .graphicsLayer {
                                val auraScale = 0.92f + 0.22f * repeatActiveGlow + 0.28f * (w4 * sRepeat)
                                scaleX = auraScale
                                scaleY = auraScale
                                alpha = (0.55f * repeatActiveGlow + 0.45f * (w4 * sRepeat)).coerceIn(0f, 0.85f)
                            }
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        accentColor.copy(alpha = 0.36f),
                                        highlightAccent.copy(alpha = 0.14f),
                                        Color.Transparent,
                                    ),
                                ),
                                shape = CircleShape,
                            ),
                    )
                }

                // Pure borderless translucent glass droplet (34.dp)
                val dropletIdleAlpha = 0.065f * (1f - repeatActiveGlow)
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .graphicsLayer {
                            scaleX = repeatScaleX
                            scaleY = repeatScaleY
                        }
                        .clip(CircleShape)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = dropletIdleAlpha + 0.22f * repeatActiveGlow),
                                    accentColor.copy(alpha = 0.28f * repeatActiveGlow),
                                    Color.White.copy(alpha = 0.025f + 0.08f * repeatActiveGlow),
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedContent(
                        targetState = repeatMode,
                        transitionSpec = {
                            (fadeIn(tween(150)) + scaleIn(initialScale = 0.72f, animationSpec = tween(170))) togetherWith
                                (fadeOut(tween(110)) + scaleOut(targetScale = 0.72f, animationSpec = tween(110)))
                        },
                        label = "RepeatIconMode",
                    ) { mode ->
                        val glyph = if (!isLive && mode == RepeatModes.ONE) {
                            BitChordIcons.PlayerRepeatOne
                        } else {
                            BitChordIcons.PlayerRepeat
                        }
                        val desc = when (mode) {
                            RepeatModes.ONE -> Res.string.repeat_one
                            RepeatModes.ALL -> Res.string.repeat_all
                            else -> Res.string.repeat_off
                        }
                        Icon(
                            imageVector = glyph,
                            contentDescription = stringResource(desc),
                            tint = repeatTint,
                            modifier = Modifier
                                .size(19.dp)
                                .graphicsLayer {
                                    rotationZ = if (sRepeat <= 0f) 0f else repeatSpinAnim.value
                                },
                        )
                    }
                }
            }
        }
    }
}

internal val PIPELINE_PILL_WIDTH = 186.dp

/**
 * Compact, fixed-width Audio Pipeline Bottom Pill (186.dp x 44.dp).
 *
 * Keeps an unwavering capsule width across streams and verbose source names
 * (such as "Local Storage") so neighboring bottom controls never drift.
 */
@Composable
fun ChordPlayPipelineBottomPill(
    song: Song,
    isLoading: Boolean,
    onOpenPipeline: () -> Unit,
    onOpenOutput: () -> Unit,
    palette: MeshPalette = remember { MeshPalette(emptyList()) },
    modifier: Modifier = Modifier,
    leftInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    rightInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    focalBias: Float = 0f,
    highlightBoost: Float = 0f,
) {
    val haptics = rememberHaptics()
    val videoQuality by PlayerSettings.videoQuality.collectAsStateWithLifecycle()
    val mvQuality by PlayerPlatform.host.musicVideoQuality.collectAsStateWithLifecycle()
    val reduceAnimation by PlayerSettings.reduceAnimation.collectAsStateWithLifecycle()
    val disableInfiniteLoops by PlayerSettings.disableInfiniteLoops.collectAsStateWithLifecycle()
    val nerdStats by NerdStats.current.collectAsStateWithLifecycle()
    val racingLossless by NerdStats.racingLossless.collectAsStateWithLifecycle()
    val stillRacing = song.videoId in racingLossless

    val accents = rememberLiftedArtworkAccents(palette)
    val accentColor = accents.primary
    val highlightAccent = accents.highlight

    val stats = nerdStats
    val hasMeasuredAudio = stats != null && (stats.mimeType != null || stats.bitrateKbps != null || stats.sampleRateHz != null)

    val qualityBadge = videoQualityBadge(mvQuality, videoQuality)

    val primaryLabel = when {
        song.isLive -> "LIVE • $qualityBadge"
        song.isVideo -> "VIDEO • $qualityBadge"
        isLoading || stillRacing || !hasMeasuredAudio -> "PROCESSING..."
        stats?.isHiRes == true -> "HD LOSSLESS"
        stats?.isLossless == true -> "LOSSLESS ${NerdStats.codecLabel(stats.mimeType) ?: "FLAC"}"
        stats?.isDolbyAtmos == true -> "DOLBY ATMOS"
        else -> {
            val codec = NerdStats.codecLabel(stats?.mimeType)?.uppercase() ?: "AUDIO"
            val kbps = stats?.bitrateKbps?.let { " • ${it}K" }.orEmpty()
            "$codec$kbps"
        }
    }

    val sampleRate = stats?.sampleRateHz
    val secondaryLabel = when {
        song.isLive -> "24/7 LIVE STATION"
        song.isVideo -> "VIDEO STREAM"
        isLoading || stillRacing || !hasMeasuredAudio -> "AUDIO PIPELINE"
        stats?.isLossless == true -> {
            val depth = stats.bitDepth ?: if (stats.isHiRes) 24 else 16
            val khz = ((sampleRate ?: 44100) / 1000f).let {
                if (it % 1f == 0f) it.toInt().toString() else "%.1f".format(it)
            }
            "${depth}-BIT / $khz kHz"
        }
        sampleRate != null -> {
            val khz = (sampleRate / 1000f).let {
                if (it % 1f == 0f) it.toInt().toString() else "%.1f".format(it)
            }
            "$khz kHz • ${stats?.sourceName?.uppercase() ?: "AUDIO PIPELINE"}"
        }
        else -> "AUDIO PIPELINE"
    }

    val rimStartAlpha = (0.48f + if (focalBias < 0f) -focalBias * 0.28f else 0f + highlightBoost * 0.2f).coerceIn(0f, 0.85f)
    val rimEndAlpha = (0.18f + if (focalBias > 0f) focalBias * 0.36f else 0f + highlightBoost * 0.2f).coerceIn(0f, 0.85f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .width(PIPELINE_PILL_WIDTH)
            .height(44.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.28f))
            .background(accentColor.copy(alpha = (0.12f + 0.08f * highlightBoost).coerceAtMost(0.24f)))
            .background(Color.White.copy(alpha = 0.08f))
            .border(
                width = 0.85.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        highlightAccent.copy(alpha = rimStartAlpha),
                        Color.White.copy(alpha = (0.20f + 0.14f * highlightBoost).coerceAtMost(0.42f)),
                        highlightAccent.copy(alpha = rimEndAlpha),
                    ),
                ),
                shape = CircleShape,
            )
            .padding(start = 13.dp, end = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable(
                    interactionSource = leftInteractionSource,
                    indication = null,
                ) {
                    haptics.play(Haptic.Tap)
                    onOpenPipeline()
                }
                .padding(end = 8.dp),
        ) {
            Icon(
                imageVector = if (song.isVideo) Icons.Rounded.HighQuality else Icons.Rounded.GraphicEq,
                contentDescription = null,
                tint = highlightAccent,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(7.dp))
            val marqueeMod = if (!reduceAnimation && !disableInfiniteLoops) {
                Modifier.basicMarquee(
                    iterations = Int.MAX_VALUE,
                    initialDelayMillis = 1600,
                    repeatDelayMillis = 3200,
                )
            } else {
                Modifier
            }
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = primaryLabel,
                    color = Color.White.copy(alpha = 0.96f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.25.sp,
                    lineHeight = 13.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = marqueeMod,
                )
                Text(
                    text = secondaryLabel,
                    color = Color.White.copy(alpha = 0.66f),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.25.sp,
                    lineHeight = 11.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = marqueeMod,
                )
            }
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(22.dp)
                .background(Color.White.copy(alpha = 0.18f)),
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = 42.dp, height = 42.dp)
                .clickable(
                    interactionSource = rightInteractionSource,
                    indication = null,
                ) {
                    haptics.play(Haptic.Tap)
                    onOpenOutput()
                },
        ) {
            Icon(
                imageVector = BitChordIcons.Earbuds,
                contentDescription = stringResource(Res.string.audio_output),
                tint = Color.White.copy(alpha = 0.94f),
                modifier = Modifier.size(21.dp),
            )
        }
    }
}

private const val AUDIO_GRAPH_BARS = 64
private const val AUDIO_GRAPH_PENDING_LEVEL = 0.13f
