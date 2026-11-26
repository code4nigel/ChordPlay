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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.music.bitchord.data.settings.SpeedDialTileStyle
import com.music.bitchord.sharedui.resources.Res
import com.music.bitchord.sharedui.resources.chord_wave
import com.music.bitchord.sharedui.resources.chord_wave_subtitle
import com.music.bitchord.ui.graphics.ColorUtils
import com.music.bitchord.ui.graphics.forPixelAccess
import com.music.bitchord.ui.graphics.paletteSwatches
import com.music.bitchord.ui.graphics.toImageBitmap
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import com.music.bitchord.ui.theme.ArtworkPalette
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

private enum class ChordWavePhase {
    IDLE,
    FLOWING,
    LOCKING,
}

private const val WAVE_MIN_FLOW_MS = 520L
private const val WAVE_MAX_WAIT_MS = 10_000L
private const val WAVE_TICK_INTERVAL_MS = 240L
private const val WAVE_SHOCKWAVE_MS = 640
private const val WAVE_RAMP_MS = 280
private const val WAVE_SETTLE_MS = 820
private const val WAVE_BLOOM_RISE_MS = 220
private const val WAVE_BLOOM_FADE_MS = 780
private const val COLOR_FADE_MS = 1100

/**
 * Preserves continuous integrated phases across pager swipes and scroll recycling
 * so the aurora silk never jumps back to t = 0 when re-entering composition.
 */
private object ChordWaveSessionClock {
    var flowPhase: Float = 0.7f
    var driftPhase: Float = 1.4f
    var sparklePhase: Float = 0.3f
}

private val AuroraSwatchCache = LinkedHashMap<String, List<Color>>(32, 0.75f, true)
private const val MAX_SWATCH_CACHE = 32

@Immutable
data class ChordWaveAuroraPalette(
    val baseDeep: Color,
    val baseMid: Color,
    val primary: Color,
    val secondary: Color,
    val highlight: Color,
    val filament: Color,
)

@Composable
private fun rememberChordWaveController(
    reduceAnimation: Boolean,
    isPlaying: Boolean,
    artworkUrl: String?,
    onRoll: (onLanded: () -> Unit) -> Unit,
): ChordWaveController {
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    val controller = remember(scope, haptics) { ChordWaveController(scope, haptics) }

    controller.reduceAnimation = reduceAnimation
    controller.isPlaying = isPlaying
    controller.onRoll = onRoll

    // Continuous velocity integration: flowPhase += dt * speed guarantees
    // C1-smooth acceleration and deceleration without rewinds or loop seams.
    LaunchedEffect(reduceAnimation) {
        if (reduceAnimation) return@LaunchedEffect
        var lastMs = withFrameMillis { it }
        while (true) {
            val now = withFrameMillis { it }
            val dt = ((now - lastMs) / 1000f).coerceIn(0f, 0.05f)
            lastMs = now

            val e = controller.energy.value.coerceIn(0f, 1.25f)
            val playDrive = if (controller.isPlaying) 1.0f else 0.56f
            controller.flowPhase += dt * (0.72f * playDrive + 3.15f * e)
            controller.driftPhase += dt * (0.34f * playDrive + 0.95f * e)
            controller.sparklePhase += dt * (0.36f * playDrive + 1.55f * e)

            ChordWaveSessionClock.flowPhase = controller.flowPhase
            ChordWaveSessionClock.driftPhase = controller.driftPhase
            ChordWaveSessionClock.sparklePhase = controller.sparklePhase
        }
    }

    // Soft chromatic afterglow whenever the playing track's artwork changes externally.
    var prevArtwork by remember { mutableStateOf(artworkUrl) }
    LaunchedEffect(artworkUrl, reduceAnimation) {
        val old = prevArtwork
        prevArtwork = artworkUrl
        if (!reduceAnimation && old != null && artworkUrl != null && old != artworkUrl &&
            controller.phase == ChordWavePhase.IDLE
        ) {
            controller.pulseColorShift()
        }
    }

    return controller
}

private class ChordWaveController(
    private val scope: kotlinx.coroutines.CoroutineScope,
    private val haptics: com.music.bitchord.ui.haptics.Haptics,
) {
    var phase by mutableStateOf(ChordWavePhase.IDLE)
    var flowPhase by mutableFloatStateOf(ChordWaveSessionClock.flowPhase)
    var driftPhase by mutableFloatStateOf(ChordWaveSessionClock.driftPhase)
    var sparklePhase by mutableFloatStateOf(ChordWaveSessionClock.sparklePhase)
    var reduceAnimation: Boolean = false
    var isPlaying: Boolean = false
    var onRoll: (onLanded: () -> Unit) -> Unit = { it() }

    val energy = Animatable(0f)
    val shockwave = Animatable(0f)
    val bloom = Animatable(0f)
    val tapPress = Animatable(0f)

    fun pulseColorShift() {
        scope.launch {
            bloom.animateTo(0.45f, tween(260, easing = FastOutSlowInEasing))
            bloom.animateTo(0f, tween(680, easing = FastOutSlowInEasing))
        }
    }

    fun trigger() {
        if (phase != ChordWavePhase.IDLE) return
        phase = ChordWavePhase.FLOWING
        haptics.play(Haptic.Tap)

        val landed = CompletableDeferred<Unit>()
        onRoll { landed.complete(Unit) }

        scope.launch {
            try {
                if (reduceAnimation) {
                    energy.snapTo(0.5f)
                    delay(WAVE_MIN_FLOW_MS)
                    withTimeoutOrNull(WAVE_MAX_WAIT_MS - WAVE_MIN_FLOW_MS) { landed.await() }
                    haptics.play(Haptic.Select)
                    phase = ChordWavePhase.LOCKING
                    energy.snapTo(0f)
                } else {
                    val squishJob = launch {
                        tapPress.animateTo(1f, tween(110, easing = FastOutSlowInEasing))
                        tapPress.animateTo(
                            0f,
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                        )
                    }
                    // Act 1: Iridescent shockwave ring that reaches alpha 0 cleanly before resetting
                    val shockJob = launch {
                        shockwave.snapTo(0f)
                        shockwave.animateTo(
                            1f,
                            tween(WAVE_SHOCKWAVE_MS, easing = CubicBezierEasing(0.18f, 0.85f, 0.30f, 1f)),
                        )
                    }
                    // Act 2: Smooth acceleration into convergent harmonic orbit
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

                    // Act 3: Harmonic lock, luminous rebirth bloom & critically damped return
                    phase = ChordWavePhase.LOCKING
                    haptics.play(Haptic.Select)

                    val bloomJob = launch {
                        bloom.animateTo(1f, tween(WAVE_BLOOM_RISE_MS, easing = FastOutSlowInEasing))
                        bloom.animateTo(0f, tween(WAVE_BLOOM_FADE_MS, easing = FastOutSlowInEasing))
                    }
                    energy.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(WAVE_SETTLE_MS, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)),
                    )
                    shockJob.join()
                    squishJob.join()
                    bloomJob.join()
                }
            } finally {
                phase = ChordWavePhase.IDLE
            }
        }
    }
}

@Composable
fun rememberChordWaveAuroraPalette(
    artworkUrl: String?,
    fallbackPalette: ArtworkPalette,
    isDark: Boolean,
    reduceAnimation: Boolean,
): ChordWaveAuroraPalette {
    val context = LocalPlatformContext.current
    var extracted by remember {
        mutableStateOf<List<Color>?>(
            artworkUrl?.let { synchronized(AuroraSwatchCache) { AuroraSwatchCache[it] } },
        )
    }

    LaunchedEffect(artworkUrl) {
        if (artworkUrl.isNullOrBlank()) {
            extracted = null
            return@LaunchedEffect
        }
        val cached = synchronized(AuroraSwatchCache) { AuroraSwatchCache[artworkUrl] }
        if (cached != null) {
            extracted = cached
            return@LaunchedEffect
        }
        val result = SingletonImageLoader.get(context).execute(
            ImageRequest.Builder(context)
                .data(artworkUrl)
                .size(96)
                .forPixelAccess()
                .build(),
        )
        val bitmap = (result as? SuccessResult)?.image?.toImageBitmap() ?: return@LaunchedEffect
        val swatches = withContext(Dispatchers.Default) { extractAuroraSwatches(bitmap) }
        synchronized(AuroraSwatchCache) {
            if (AuroraSwatchCache.size >= MAX_SWATCH_CACHE) {
                val eldest = AuroraSwatchCache.keys.firstOrNull()
                if (eldest != null) AuroraSwatchCache.remove(eldest)
            }
            AuroraSwatchCache[artworkUrl] = swatches
        }
        extracted = swatches
    }

    val rawTarget = remember(extracted, fallbackPalette, isDark) {
        buildAuroraPalette(extracted, fallbackPalette, isDark)
    }

    val spec = if (reduceAnimation) snap<Color>() else tween(COLOR_FADE_MS, easing = FastOutSlowInEasing)
    return ChordWaveAuroraPalette(
        baseDeep = animateColorAsState(rawTarget.baseDeep, spec, label = "auroraBaseDeep").value,
        baseMid = animateColorAsState(rawTarget.baseMid, spec, label = "auroraBaseMid").value,
        primary = animateColorAsState(rawTarget.primary, spec, label = "auroraPrimary").value,
        secondary = animateColorAsState(rawTarget.secondary, spec, label = "auroraSecondary").value,
        highlight = animateColorAsState(rawTarget.highlight, spec, label = "auroraHighlight").value,
        filament = animateColorAsState(rawTarget.filament, spec, label = "auroraFilament").value,
    )
}

private fun extractAuroraSwatches(bitmap: ImageBitmap): List<Color> {
    fun readSwatches(clearFilters: Boolean): List<Color> =
        paletteSwatches(bitmap, 24, clearFilters)
            .sortedByDescending { it.population }
            .map { Color(it.rgb) }

    val found = readSwatches(clearFilters = false).ifEmpty { readSwatches(clearFilters = true) }
    val distinct = mutableListOf<Color>()
    for (color in found) {
        if (distinct.none { it.isChromaticallyClose(color) }) {
            distinct += color
        }
    }
    if (distinct.isEmpty()) return emptyList()
    while (distinct.size < 4) {
        val base = distinct[(distinct.size - 1).coerceAtLeast(0)]
        val step = distinct.size
        distinct += base.hueShifted(32f * step, if (step % 2 == 1) 0.08f else -0.06f)
    }
    return distinct.take(4)
}

private fun buildAuroraPalette(
    swatches: List<Color>?,
    fallback: ArtworkPalette,
    isDark: Boolean,
): ChordWaveAuroraPalette {
    val seeds = if (!swatches.isNullOrEmpty()) {
        swatches
    } else {
        listOf(
            fallback.accent,
            fallback.accent.hueShifted(38f, 0.04f),
            fallback.accent.hueShifted(-34f, 0.10f),
            fallback.elevated,
        )
    }
    val s0 = seeds[0]
    val s1 = seeds.getOrElse(1) { s0.hueShifted(34f, 0.05f) }
    val s2 = seeds.getOrElse(2) { s1.hueShifted(-30f, 0.08f) }

    return if (isDark) {
        val primary = s0.tunedAuroraColor(minSat = 0.62f, minLum = 0.52f, maxLum = 0.68f)
        val secondary = s1.tunedAuroraColor(minSat = 0.58f, minLum = 0.48f, maxLum = 0.65f)
        val highlight = s2.tunedAuroraColor(minSat = 0.56f, minLum = 0.64f, maxLum = 0.78f)
        ChordWaveAuroraPalette(
            baseDeep = s0.tunedAuroraGround(targetLum = 0.085f, maxSat = 0.62f),
            baseMid = s1.tunedAuroraGround(targetLum = 0.145f, maxSat = 0.68f),
            primary = primary,
            secondary = secondary,
            highlight = highlight,
            filament = lerp(highlight, Color.White, 0.55f),
        )
    } else {
        val primary = s0.tunedAuroraColor(minSat = 0.64f, minLum = 0.40f, maxLum = 0.56f)
        val secondary = s1.tunedAuroraColor(minSat = 0.58f, minLum = 0.42f, maxLum = 0.58f)
        val highlight = s2.tunedAuroraColor(minSat = 0.62f, minLum = 0.50f, maxLum = 0.66f)
        ChordWaveAuroraPalette(
            baseDeep = s0.tunedAuroraGround(targetLum = 0.90f, maxSat = 0.36f),
            baseMid = s1.tunedAuroraGround(targetLum = 0.83f, maxSat = 0.44f),
            primary = primary,
            secondary = secondary,
            highlight = highlight,
            filament = lerp(primary, Color.White, 0.35f),
        )
    }
}

private fun Color.isChromaticallyClose(other: Color): Boolean {
    val a = FloatArray(3).also { ColorUtils.colorToHSL(toArgb(), it) }
    val b = FloatArray(3).also { ColorUtils.colorToHSL(other.toArgb(), it) }
    val hueGap = abs(a[0] - b[0]).let { min(it, 360f - it) }
    return hueGap < 18f && abs(a[2] - b[2]) < 0.14f
}

private fun Color.hueShifted(hueDelta: Float, lumDelta: Float): Color {
    val hsl = FloatArray(3).also { ColorUtils.colorToHSL(toArgb(), it) }
    hsl[0] = ((hsl[0] + hueDelta) % 360f + 360f) % 360f
    hsl[2] = (hsl[2] + lumDelta).coerceIn(0.22f, 0.74f)
    return Color(ColorUtils.HSLToColor(hsl))
}

private fun Color.tunedAuroraColor(minSat: Float, minLum: Float, maxLum: Float): Color {
    val hsl = FloatArray(3).also { ColorUtils.colorToHSL(toArgb(), it) }
    hsl[1] = (hsl[1] * 1.32f).coerceIn(minSat, 0.98f)
    hsl[2] = hsl[2].coerceIn(minLum, maxLum)
    return Color(ColorUtils.HSLToColor(hsl))
}

private fun Color.tunedAuroraGround(targetLum: Float, maxSat: Float): Color {
    val hsl = FloatArray(3).also { ColorUtils.colorToHSL(toArgb(), it) }
    hsl[1] = (hsl[1] * 0.85f).coerceIn(0.20f, maxSat)
    hsl[2] = targetLum
    return Color(ColorUtils.HSLToColor(hsl))
}

/**
 * Full-bleed square "Chord Wave" discovery tile for the 3×3 Your Space grid.
 * Renders an artwork-driven ethereal aurora silk veil with drifting stardust motes
 * that shimmer continuously at rest, converge smoothly while a lucky radio pick
 * resolves, and bloom with the newly picked song's colors on lock.
 */
@Composable
fun ChordWaveArtworkTile(
    palette: ArtworkPalette,
    tileStyle: SpeedDialTileStyle,
    showLabel: Boolean,
    reduceAnimation: Boolean,
    onRoll: (onLanded: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    artworkUrl: String? = null,
    isPlaying: Boolean = false,
    dim: () -> Float = { 0f },
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val label = stringResource(Res.string.chord_wave)
    val subtitle = stringResource(Res.string.chord_wave_subtitle)
    val controller = rememberChordWaveController(reduceAnimation, isPlaying, artworkUrl, onRoll)
    val aurora = rememberChordWaveAuroraPalette(artworkUrl, palette, isDark, reduceAnimation)
    val shape = SmoothCornerShape(14.dp)

    val rimBrush = Brush.linearGradient(
        colors = listOf(
            lerp(aurora.filament, Color.White, 0.35f).copy(alpha = if (isDark) 0.32f else 0.45f),
            aurora.primary.copy(alpha = 0.14f),
            aurora.secondary.copy(alpha = 0.28f),
        ),
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                val squish = 1f - 0.042f * controller.tapPress.value
                scaleX = squish
                scaleY = squish
            }
            .press3D(enabled = !reduceAnimation)
            .clip(shape)
            .background(aurora.baseDeep)
            .border(0.75.dp, rimBrush, shape)
            .speedDialTileEdge(
                style = tileStyle,
                shape = shape,
                accent = aurora.primary,
                isDark = isDark,
                isCurrent = controller.phase != ChordWavePhase.IDLE,
            )
            .semantics { contentDescription = "$label, $subtitle" }
            .clickable(role = Role.Button, onClickLabel = label) { controller.trigger() },
    ) {
        ChordWaveCanvas(
            controller = controller,
            aurora = aurora,
            isDark = isDark,
            compact = false,
            modifier = Modifier.fillMaxSize(),
        )

        if (showLabel) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                lerp(Color(0xFF090A10), aurora.baseDeep, 0.35f)
                                    .copy(alpha = if (isDark) 0.68f else 0.58f),
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
 * Compact horizontal "Chord Wave" card for 2×4 Cards and 1×4 List modes.
 */
@Composable
fun ChordWaveCompactCard(
    palette: ArtworkPalette,
    isDark: Boolean,
    reduceAnimation: Boolean,
    onRoll: (onLanded: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    artworkUrl: String? = null,
    isPlaying: Boolean = false,
    isCompact: Boolean = false,
) {
    val label = stringResource(Res.string.chord_wave)
    val subtitle = stringResource(Res.string.chord_wave_subtitle)
    val controller = rememberChordWaveController(reduceAnimation, isPlaying, artworkUrl, onRoll)
    val aurora = rememberChordWaveAuroraPalette(artworkUrl, palette, isDark, reduceAnimation)

    val tileHeight = if (isCompact) 56.dp else 64.dp
    val outerShape = SmoothCornerShape(if (isCompact) 18.dp else 22.dp)
    val cardPadding = if (isCompact) 4.5.dp else 5.5.dp
    val artSize = tileHeight - (cardPadding * 2)
    val artworkShape = SmoothCornerShape(if (isCompact) 13.dp else 15.dp)

    val neutralBase = if (isDark) Color(0xFF18191E) else MaterialTheme.colorScheme.surfaceVariant
    val cardBg = lerp(neutralBase, aurora.primary, if (isDark) 0.14f else 0.10f)
        .copy(alpha = if (isDark) 0.88f else 0.93f)
    val cardBorder = if (controller.phase != ChordWavePhase.IDLE) {
        aurora.primary.copy(alpha = 0.62f)
    } else if (isDark) {
        aurora.primary.copy(alpha = 0.28f)
    } else {
        aurora.primary.copy(alpha = 0.18f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(tileHeight)
            .graphicsLayer {
                val squish = 1f - 0.025f * controller.tapPress.value
                scaleX = squish
                scaleY = squish
            }
            .press3D(enabled = !reduceAnimation)
            .clip(outerShape)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        lerp(cardBg, aurora.secondary, if (isDark) 0.12f else 0.08f),
                        cardBg,
                    ),
                ),
            )
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
                    .background(aurora.baseDeep)
                    .border(
                        width = 0.75.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                lerp(aurora.filament, Color.White, 0.35f).copy(alpha = 0.38f),
                                aurora.primary.copy(alpha = 0.26f),
                            ),
                        ),
                        shape = artworkShape,
                    ),
            ) {
                ChordWaveCanvas(
                    controller = controller,
                    aurora = aurora,
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
    aurora: ChordWaveAuroraPalette,
    isDark: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val veilPath = remember { Path() }
    val crestPath = remember { Path() }
    val samples = if (compact) 26 else 44
    val ysTop = remember(samples) { FloatArray(samples + 1) }
    val ysBot = remember(samples) { FloatArray(samples + 1) }

    val ribbons = remember(compact) {
        listOf(
            SilkWaveSpec(
                freq1 = 1.15f,
                freq2 = 2.05f,
                speed1 = 0.82f,
                speed2 = 0.53f,
                phaseOffset = 0.4f,
                baseOffsetRatio = if (compact) 0.04f else -0.05f,
                ampScale = 0.92f,
                thicknessRatio = if (compact) 0.19f else 0.17f,
                veilAlpha = if (compact) 0.34f else 0.36f,
                glowWidthDp = if (compact) 4.0f else 5.5f,
                coreWidthDp = if (compact) 1.0f else 1.3f,
            ),
            SilkWaveSpec(
                freq1 = 1.58f,
                freq2 = 2.65f,
                speed1 = 1.08f,
                speed2 = 0.74f,
                phaseOffset = 2.15f,
                baseOffsetRatio = if (compact) 0.0f else 0.01f,
                ampScale = 1.06f,
                thicknessRatio = if (compact) 0.22f else 0.19f,
                veilAlpha = if (compact) 0.42f else 0.44f,
                glowWidthDp = if (compact) 4.8f else 6.8f,
                coreWidthDp = if (compact) 1.25f else 1.55f,
            ),
            SilkWaveSpec(
                freq1 = 2.10f,
                freq2 = 3.25f,
                speed1 = -0.94f,
                speed2 = 1.18f,
                phaseOffset = 4.05f,
                baseOffsetRatio = if (compact) -0.04f else 0.06f,
                ampScale = 0.78f,
                thicknessRatio = if (compact) 0.14f else 0.125f,
                veilAlpha = if (compact) 0.36f else 0.38f,
                glowWidthDp = if (compact) 3.4f else 4.8f,
                coreWidthDp = if (compact) 0.95f else 1.2f,
            ),
        )
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val flow = controller.flowPhase
        val drift = controller.driftPhase
        val sparkle = controller.sparklePhase
        val energy = controller.energy.value.coerceIn(0f, 1.25f)
        val shock = controller.shockwave.value.coerceIn(0f, 1f)
        val bloom = controller.bloom.value.coerceIn(0f, 1f)
        val dim = size.minDimension
        val centerY = if (compact) h * 0.50f else h * 0.44f

        // 1. Deep atmospheric ground wash
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(aurora.baseDeep, aurora.baseMid, aurora.baseDeep),
                start = Offset.Zero,
                end = Offset(w, h),
            ),
        )

        // 2. Orbiting nebula clouds driven by irrational speeds so patterns never repeat
        val cloudBoost = 1f + energy * 0.42f + bloom * 0.55f
        val d1x = cos(drift * 0.53f) * 0.11f
        val d1y = sin(drift * 0.41f) * 0.10f
        val d2x = sin(drift * 0.47f + 1.3f) * 0.12f
        val d2y = cos(drift * 0.61f + 0.7f) * 0.11f

        drawNebulaCloud(
            color = aurora.primary,
            center = Offset(w * (0.26f + d1x), h * (0.30f + d1y)),
            radius = dim * (0.82f + 0.10f * energy),
            alpha = (if (isDark) 0.40f else 0.28f) * cloudBoost,
        )
        drawNebulaCloud(
            color = aurora.secondary,
            center = Offset(w * (0.76f - d1x), h * (0.68f - d1y)),
            radius = dim * (0.86f + 0.12f * energy),
            alpha = (if (isDark) 0.36f else 0.24f) * cloudBoost,
        )
        drawNebulaCloud(
            color = aurora.highlight,
            center = Offset(w * (0.68f + d2x), h * (0.24f - d2y)),
            radius = dim * 0.68f,
            alpha = (if (isDark) 0.28f else 0.22f) * cloudBoost,
        )
        if (!compact) {
            drawNebulaCloud(
                color = aurora.primary,
                center = Offset(w * (0.32f - d2x), h * (0.74f + d2y)),
                radius = dim * 0.62f,
                alpha = (if (isDark) 0.24f else 0.18f) * cloudBoost,
            )
        }

        // 3. Three interwoven volumetric aurora silk ribbons
        val ribbonColors = listOf(aurora.secondary, aurora.primary, aurora.highlight)
        val baseAmp = dim * (0.068f + 0.088f * energy) * (1f - bloom * 0.22f)
        val breathe = 0.88f + 0.12f * sin(drift * 1.15f)

        ribbons.forEachIndexed { rIdx, spec ->
            val color = ribbonColors[rIdx]
            val bandColor = if (isDark) color else lerp(color, Color(0xFF151726), 0.12f)
            val crestColor = lerp(bandColor, aurora.filament, 0.58f + bloom * 0.22f)
            // Ribbons converge gently toward center during Act 2 tuning
            val ribbonCenter = centerY + h * spec.baseOffsetRatio * (1f - 0.65f * energy)
            val amp = baseAmp * spec.ampScale * breathe
            val ribbonHalfMax = dim * spec.thicknessRatio * (0.85f + 0.35f * energy)

            veilPath.rewind()
            crestPath.rewind()

            var minY = h
            var maxY = 0f

            for (i in 0..samples) {
                val u = i.toFloat() / samples
                val env = sin(PI * u).toFloat().coerceAtLeast(0f).pow(1.25f)
                val w1 = sin(u * PI * 2.0 * spec.freq1 + flow * spec.speed1 + spec.phaseOffset).toFloat()
                val w2 = sin(u * PI * 2.0 * spec.freq2 - flow * spec.speed2 + spec.phaseOffset * 1.6f).toFloat()
                val spineY = ribbonCenter + (w1 * 0.72f + w2 * (0.28f + 0.22f * energy)) * amp * env

                val shimmerThick = ribbonHalfMax * env *
                    (0.68f + 0.32f * sin(u * PI * 3.0 + flow * 0.7f + rIdx).toFloat())
                val topY = spineY - shimmerThick * 0.68f
                val botY = spineY + shimmerThick * 0.82f

                ysTop[i] = topY
                ysBot[i] = botY
                if (topY < minY) minY = topY
                if (botY > maxY) maxY = botY

                val x = u * w
                if (i == 0) {
                    crestPath.moveTo(x, topY)
                    veilPath.moveTo(x, topY)
                } else {
                    val prevU = (i - 1).toFloat() / samples
                    val midX = ((prevU + u) * 0.5f) * w
                    crestPath.quadraticTo(midX, (ysTop[i - 1] + topY) * 0.5f, x, topY)
                    veilPath.quadraticTo(midX, (ysTop[i - 1] + topY) * 0.5f, x, topY)
                }
            }
            for (i in samples downTo 0) {
                val u = i.toFloat() / samples
                val x = u * w
                if (i == samples) {
                    veilPath.lineTo(x, ysBot[i])
                } else {
                    val nextU = (i + 1).toFloat() / samples
                    val midX = ((nextU + u) * 0.5f) * w
                    veilPath.quadraticTo(midX, (ysBot[i + 1] + ysBot[i]) * 0.5f, x, ysBot[i])
                }
            }
            veilPath.close()

            // Sheer luminous silk body
            val veilTopAlpha = (spec.veilAlpha + energy * 0.16f + bloom * 0.18f).coerceAtMost(0.75f)
            drawPath(
                path = veilPath,
                brush = Brush.verticalGradient(
                    0.0f to crestColor.copy(alpha = veilTopAlpha),
                    0.45f to bandColor.copy(alpha = veilTopAlpha * 0.62f),
                    1.0f to bandColor.copy(alpha = 0.04f),
                    startY = minY.coerceAtLeast(0f),
                    endY = maxY.coerceAtLeast(minY + 1f),
                ),
            )

            // Soft diffused aurora halo along the upper silk fold
            drawPath(
                path = crestPath,
                color = bandColor.copy(alpha = (0.28f + energy * 0.18f + bloom * 0.22f).coerceAtMost(0.65f)),
                style = Stroke(
                    width = spec.glowWidthDp.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )

            // Crisp pearlescent filament core
            val travelingSheen = 0.62f + 0.18f * sin(drift * 1.4f + rIdx * 2.1f)
            drawPath(
                path = crestPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        crestColor.copy(alpha = 0.08f),
                        lerp(crestColor, Color.White, 0.35f).copy(alpha = (travelingSheen + bloom * 0.22f).coerceAtMost(0.95f)),
                        aurora.filament.copy(alpha = (0.78f + bloom * 0.20f).coerceAtMost(0.98f)),
                        crestColor.copy(alpha = 0.08f),
                    ),
                ),
                style = Stroke(
                    width = spec.coreWidthDp.dp.toPx() * (1f + bloom * 0.28f),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }

        // 4. Subtle drifting stardust / bokeh motes floating along the current
        val moteCount = if (compact) 4 else 7
        for (p in 0 until moteCount) {
            val seedX = ((p * 0.618034f) % 1f)
            val seedY = 0.22f + ((p * 0.381966f) % 0.56f)
            val speedX = 0.045f + (p % 3) * 0.018f
            val xNorm = (seedX + sparkle * speedX) % 1f
            val ySway = sin(sparkle * (0.95f + p * 0.21f) + p * 1.9f) * 0.075f
            val convergeFactor = 1f - 0.32f * energy
            val yNorm = (0.5f + (seedY - 0.5f) * convergeFactor + ySway).coerceIn(0.08f, 0.92f)

            // Smooth organic fade at borders so motes never clip
            val edgeFade = sin(PI * xNorm).toFloat().coerceAtLeast(0f) *
                sin(PI * yNorm).toFloat().coerceAtLeast(0f)
            val twinkle = 0.38f + 0.62f * (0.5f + 0.5f * sin(sparkle * (1.7f + p * 0.35f) + p * 2.4f))
            val moteAlpha = edgeFade * twinkle * (0.68f + 0.28f * energy + 0.30f * bloom)

            if (moteAlpha > 0.02f) {
                val pos = Offset(xNorm * w, yNorm * (if (compact) h else h * 0.86f))
                val coreRadius = (if (compact) 0.85f else 1.15f).dp.toPx() * (0.75f + (p % 3) * 0.22f)
                val moteColor = if (p % 2 == 0) aurora.filament else lerp(aurora.primary, Color.White, 0.55f)
                drawNebulaCloud(
                    color = moteColor,
                    center = pos,
                    radius = coreRadius * 3.8f,
                    alpha = (moteAlpha * 0.42f).coerceAtMost(0.55f),
                )
                drawCircle(
                    color = Color.White.copy(alpha = (moteAlpha * 0.85f).coerceAtMost(0.95f)),
                    radius = coreRadius,
                    center = pos,
                )
            }
        }

        // 5. Act 1: Iridescent chromatic shockwave ripple on tap
        if (shock > 0.002f && shock < 0.995f) {
            val fadeOut = (1f - shock).pow(1.75f)
            val outerRadius = dim * (0.10f + 0.72f * shock)
            val innerRadius = dim * (0.04f + 0.56f * shock)
            drawCircle(
                color = aurora.highlight.copy(alpha = fadeOut * 0.48f),
                radius = outerRadius,
                center = Offset(w * 0.5f, centerY),
                style = Stroke(width = 3.2.dp.toPx() * (1f - shock * 0.4f)),
            )
            drawCircle(
                color = aurora.filament.copy(alpha = fadeOut * 0.72f),
                radius = innerRadius,
                center = Offset(w * 0.5f, centerY),
                style = Stroke(width = 1.4.dp.toPx()),
            )
        }

        // 6. Act 3: Chromatic rebirth bloom when playback locks onto the new track
        if (bloom > 0.002f) {
            drawNebulaCloud(
                color = lerp(aurora.highlight, Color.White, 0.35f),
                center = Offset(w * 0.5f, centerY),
                radius = dim * (0.38f + bloom * 0.58f),
                alpha = bloom * 0.58f,
            )
        }
    }
}

private data class SilkWaveSpec(
    val freq1: Float,
    val freq2: Float,
    val speed1: Float,
    val speed2: Float,
    val phaseOffset: Float,
    val baseOffsetRatio: Float,
    val ampScale: Float,
    val thicknessRatio: Float,
    val veilAlpha: Float,
    val glowWidthDp: Float,
    val coreWidthDp: Float,
)

fun DrawScope.drawNebulaCloud(
    color: Color,
    center: Offset,
    radius: Float,
    alpha: Float,
) {
    if (radius <= 0.5f || alpha <= 0.002f) return
    val clamped = alpha.coerceIn(0f, 1f)
    drawCircle(
        brush = Brush.radialGradient(
            0.00f to color.copy(alpha = clamped),
            0.46f to color.copy(alpha = clamped * 0.42f),
            1.00f to color.copy(alpha = 0f),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}
