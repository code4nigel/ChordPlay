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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.data.settings.SpeedDialTileStyle
import com.music.bitchord.sharedui.resources.Res
import com.music.bitchord.sharedui.resources.chord_wave
import com.music.bitchord.sharedui.resources.chord_wave_subtitle
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import com.music.bitchord.ui.theme.ArtworkPalette
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private enum class ChordWavePhase {
    IDLE,
    FLOWING,
    LOCKING,
}

private const val WAVE_MIN_FLOW_MS = 520L
private const val WAVE_MAX_WAIT_MS = 10_000L
private const val WAVE_TICK_INTERVAL_MS = 240L
private const val WAVE_RIPPLE_MS = 340
private const val WAVE_RAMP_MS = 220
private const val WAVE_BLOOM_MS = 360

private val CHORD_VIOLET = Color(0xFF7C4DFF)
private val CHORD_CYAN = Color(0xFF00E5FF)

@Composable
private fun rememberChordWaveController(
    reduceAnimation: Boolean,
    onRoll: (onLanded: () -> Unit) -> Unit,
): ChordWaveController {
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    val controller = remember(scope, haptics) { ChordWaveController(scope, haptics) }

    LaunchedEffect(reduceAnimation) {
        if (reduceAnimation) {
            controller.clockSec = 0f
            return@LaunchedEffect
        }
        val startMs = withFrameMillis { it }
        while (true) {
            withFrameMillis { now ->
                controller.clockSec = (now - startMs) / 1000f
            }
        }
    }

    controller.reduceAnimation = reduceAnimation
    controller.onRoll = onRoll
    return controller
}

private class ChordWaveController(
    private val scope: kotlinx.coroutines.CoroutineScope,
    private val haptics: com.music.bitchord.ui.haptics.Haptics,
) {
    var phase by mutableStateOf(ChordWavePhase.IDLE)
    var clockSec by mutableFloatStateOf(0f)
    var reduceAnimation: Boolean = false
    var onRoll: (onLanded: () -> Unit) -> Unit = { it() }

    val energy = Animatable(0f)
    val ripple = Animatable(0f)
    val bloom = Animatable(0f)

    fun trigger() {
        if (phase != ChordWavePhase.IDLE) return
        phase = ChordWavePhase.FLOWING
        haptics.play(Haptic.Tap)

        val landed = CompletableDeferred<Unit>()
        onRoll { landed.complete(Unit) }

        scope.launch {
            try {
                if (reduceAnimation) {
                    energy.snapTo(1f)
                    delay(WAVE_MIN_FLOW_MS)
                    withTimeoutOrNull(WAVE_MAX_WAIT_MS - WAVE_MIN_FLOW_MS) { landed.await() }
                    haptics.play(Haptic.Select)
                    phase = ChordWavePhase.LOCKING
                    energy.snapTo(0f)
                } else {
                    val rippleJob = launch {
                        ripple.snapTo(0f)
                        ripple.animateTo(1f, tween(WAVE_RIPPLE_MS, easing = FastOutSlowInEasing))
                        ripple.snapTo(0f)
                    }
                    val rampJob = launch {
                        energy.animateTo(1f, tween(WAVE_RAMP_MS, easing = FastOutSlowInEasing))
                    }
                    val tickJob = launch {
                        delay(120L)
                        while (true) {
                            haptics.play(Haptic.Tick)
                            delay(WAVE_TICK_INTERVAL_MS)
                        }
                    }

                    delay(WAVE_MIN_FLOW_MS)
                    withTimeoutOrNull(WAVE_MAX_WAIT_MS - WAVE_MIN_FLOW_MS) { landed.await() }
                    tickJob.cancel()
                    rampJob.join()
                    rippleJob.join()

                    phase = ChordWavePhase.LOCKING
                    haptics.play(Haptic.Select)

                    val bloomJob = launch {
                        bloom.snapTo(0f)
                        bloom.animateTo(1f, tween(WAVE_BLOOM_MS / 2, easing = FastOutSlowInEasing))
                        bloom.animateTo(0f, tween(WAVE_BLOOM_MS / 2, easing = FastOutSlowInEasing))
                    }
                    energy.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow,
                        ),
                    )
                    bloomJob.join()
                }
            } finally {
                phase = ChordWavePhase.IDLE
            }
        }
    }
}

/**
 * Full-bleed square "Chord Wave" discovery tile for the 3×3 and 3×4 Your Space grids.
 * Renders 3 layered liquid harmonic ribbons that breathe gently at rest, ripple & surge into a
 * continuous fluid wave while a lucky radio track resolves, and lock with a luminous bloom the
 * moment audio begins playing.
 */
@Composable
fun ChordWaveArtworkTile(
    palette: ArtworkPalette,
    tileStyle: SpeedDialTileStyle,
    showLabel: Boolean,
    reduceAnimation: Boolean,
    onRoll: (onLanded: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    dim: () -> Float = { 0f },
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val label = stringResource(Res.string.chord_wave)
    val subtitle = stringResource(Res.string.chord_wave_subtitle)
    val controller = rememberChordWaveController(reduceAnimation, onRoll)
    val shape = SmoothCornerShape(14.dp)

    val ground = if (isDark) Color(0xFF12131A) else Color(0xFFF3F4F8)

    Box(
        modifier = modifier
            .press3D(enabled = !reduceAnimation)
            .clip(shape)
            .background(ground)
            .speedDialTileEdge(
                style = tileStyle,
                shape = shape,
                accent = palette.accent,
                isDark = isDark,
                isCurrent = controller.phase != ChordWavePhase.IDLE,
            )
            .semantics { contentDescription = "$label, $subtitle" }
            .clickable(role = Role.Button, onClickLabel = label) { controller.trigger() },
    ) {
        ChordWaveCanvas(
            controller = controller,
            accent = palette.accent,
            isDark = isDark,
            compact = false,
            modifier = Modifier.fillMaxSize(),
        )

        if (showLabel) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                (if (isDark) Color.Black else Color(0xFF0E1018)).copy(alpha = if (isDark) 0.68f else 0.54f),
                            ),
                        ),
                    ),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.15).sp,
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val d = dim()
                    if (d > 0f) drawRect(Color.Black.copy(alpha = d))
                },
        )
    }
}

/**
 * Compact horizontal "Chord Wave" card for 2×3 Cards and 1×4 List modes.
 */
@Composable
fun ChordWaveCompactCard(
    palette: ArtworkPalette,
    isDark: Boolean,
    reduceAnimation: Boolean,
    onRoll: (onLanded: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
) {
    val label = stringResource(Res.string.chord_wave)
    val subtitle = stringResource(Res.string.chord_wave_subtitle)
    val controller = rememberChordWaveController(reduceAnimation, onRoll)

    val tileHeight = if (isCompact) 56.dp else 64.dp
    val outerShape = SmoothCornerShape(if (isCompact) 18.dp else 22.dp)
    val cardPadding = if (isCompact) 4.5.dp else 5.5.dp
    val artSize = tileHeight - (cardPadding * 2)
    val artworkShape = SmoothCornerShape(if (isCompact) 13.dp else 15.dp)

    val neutralBase = if (isDark) Color(0xFF1A1B1E) else MaterialTheme.colorScheme.surfaceVariant
    val cardBg = lerp(neutralBase, palette.elevated, if (isDark) 0.16f else 0.12f)
        .copy(alpha = if (isDark) 0.88f else 0.92f)
    val cardBorder = if (controller.phase != ChordWavePhase.IDLE) {
        palette.accent.copy(alpha = 0.55f)
    } else if (isDark) {
        palette.accent.copy(alpha = 0.22f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(tileHeight)
            .press3D(enabled = !reduceAnimation)
            .clip(outerShape)
            .background(cardBg)
            .border(0.75.dp, cardBorder, outerShape)
            .semantics { contentDescription = "$label, $subtitle" }
            .clickable(role = Role.Button, onClickLabel = label) { controller.trigger() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = cardPadding, end = if (isCompact) 8.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 7.dp else 9.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(artSize)
                    .clip(artworkShape)
                    .background(if (isDark) Color(0xFF12131A) else Color(0xFFEAECEF))
                    .border(0.75.dp, palette.accent.copy(alpha = 0.35f), artworkShape),
            ) {
                ChordWaveCanvas(
                    controller = controller,
                    accent = palette.accent,
                    isDark = isDark,
                    compact = true,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = label,
                    style = if (isCompact) {
                        MaterialTheme.typography.titleSmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.15).sp,
                        )
                    } else {
                        MaterialTheme.typography.titleMedium.copy(
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp,
                        )
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(1.5.dp))
                Text(
                    text = subtitle,
                    style = if (isCompact) {
                        MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    } else {
                        MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun ChordWaveCanvas(
    controller: ChordWaveController,
    accent: Color,
    isDark: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val fillPath = remember { Path() }
    val strokePath = remember { Path() }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val t = controller.clockSec
        val energy = controller.energy.value.coerceIn(0f, 1.25f)
        val ripple = controller.ripple.value.coerceIn(0f, 1f)
        val bloom = controller.bloom.value.coerceIn(0f, 1f)
        val dim = size.minDimension

        // 1. Living ambient aura blobs (drifting slowly without recomposing)
        val auraBoost = 1f + energy * 0.45f + bloom * 0.55f
        val driftX = sin(t * 0.7f) * 0.08f
        val driftY = cos(t * 0.55f) * 0.08f

        drawAuraBlob(
            color = accent,
            center = Offset(w * (0.24f + driftX), h * (0.28f + driftY)),
            radius = dim * 0.75f,
            alpha = (if (isDark) 0.30f else 0.22f) * auraBoost,
        )
        drawAuraBlob(
            color = CHORD_VIOLET,
            center = Offset(w * (0.78f - driftX), h * (0.72f - driftY)),
            radius = dim * 0.78f,
            alpha = (if (isDark) 0.28f else 0.20f) * auraBoost,
        )
        drawAuraBlob(
            color = CHORD_CYAN,
            center = Offset(w * (0.76f + driftY), h * (0.22f - driftX)),
            radius = dim * 0.58f,
            alpha = (if (isDark) 0.18f else 0.16f) * auraBoost,
        )

        // 2. Three layered harmonic chord ribbons (Root, Third, Fifth)
        val centerY = if (compact) h * 0.52f else h * 0.46f
        val baseAmp = dim * (0.065f + 0.105f * energy)
        val phaseAdvance = t * (1.5f + energy * 5.2f)
        val samples = if (compact) 28 else 40

        val ribbons = listOf(
            RibbonSpec(color = CHORD_VIOLET, freq = 1.45f, phase = 0.0f, ampScale = 0.90f, strokeDp = if (compact) 1.5f else 2.0f, alpha = 0.78f),
            RibbonSpec(color = CHORD_CYAN, freq = 2.15f, phase = 1.85f, ampScale = 0.72f, strokeDp = if (compact) 1.4f else 1.8f, alpha = 0.82f),
            RibbonSpec(color = accent, freq = 1.80f, phase = 3.60f, ampScale = 1.00f, strokeDp = if (compact) 1.7f else 2.3f, alpha = 0.95f),
        )

        ribbons.forEach { spec ->
            fillPath.rewind()
            strokePath.rewind()
            val amp = baseAmp * spec.ampScale * (1f - bloom * 0.35f)
            for (i in 0..samples) {
                val u = i.toFloat() / samples
                val x = u * w
                // Smooth window envelope tapers both ends to zero so ribbons float organically
                val envelope = sin(PI * u).toFloat()
                val harmonic = sin(u * PI * 2.0 * spec.freq + phaseAdvance + spec.phase).toFloat()
                val overtone = 0.28f * sin(u * PI * 4.0 + phaseAdvance * 1.35f + spec.phase).toFloat()
                val y = centerY + (harmonic + overtone * energy) * amp * envelope
                if (i == 0) {
                    strokePath.moveTo(x, y)
                    fillPath.moveTo(x, h)
                    fillPath.lineTo(x, y)
                } else {
                    strokePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }
            fillPath.lineTo(w, h)
            fillPath.close()

            val tint = if (isDark) spec.color else lerp(spec.color, Color(0xFF1A1B2E), 0.18f)
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        tint.copy(alpha = (0.16f + energy * 0.14f) * spec.alpha),
                        Color.Transparent,
                    ),
                    startY = centerY - baseAmp,
                    endY = h,
                ),
            )
            drawPath(
                path = strokePath,
                color = tint.copy(alpha = (spec.alpha + bloom * 0.15f).coerceAtMost(1f)),
                style = Stroke(
                    width = spec.strokeDp.dp.toPx() * (1f + bloom * 0.35f),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }

        // 3. Acoustic tap ripple ring
        if (ripple > 0.001f && ripple < 0.999f) {
            val ringRadius = dim * (0.12f + 0.62f * ripple)
            val ringAlpha = (1f - ripple) * 0.72f
            drawCircle(
                color = Color.White.copy(alpha = ringAlpha),
                radius = ringRadius,
                center = Offset(w / 2f, centerY),
                style = Stroke(width = (2.4.dp.toPx() * (1f - ripple * 0.5f))),
            )
        }

        // 4. Chord-lock luminous resonant crest bloom when the rolled track starts sounding
        if (bloom > 0.001f) {
            drawAuraBlob(
                color = lerp(accent, Color.White, 0.45f),
                center = Offset(w / 2f, centerY),
                radius = dim * (0.35f + bloom * 0.45f),
                alpha = bloom * 0.52f,
            )
        }
    }
}

private data class RibbonSpec(
    val color: Color,
    val freq: Float,
    val phase: Float,
    val ampScale: Float,
    val strokeDp: Float,
    val alpha: Float,
)

private fun DrawScope.drawAuraBlob(
    color: Color,
    center: Offset,
    radius: Float,
    alpha: Float,
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = alpha.coerceIn(0f, 1f)),
                color.copy(alpha = 0f),
            ),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}
