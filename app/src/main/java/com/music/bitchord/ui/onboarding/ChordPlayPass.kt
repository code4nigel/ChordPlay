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
package com.music.bitchord.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.stats.WaveIdentitySignature
import com.music.bitchord.ui.player.MeshPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Interactive frosted-glass ChordPlay Wave Identity Card.
 *
 * Typing a name morphs the card's acoustic waves, colors, harmonic frequencies,
 * orbital geometry, and 11-bar spectrum fingerprint deterministically — giving
 * every listener a unique ChordPlay visual identity backed locally under
 * `Downloads/Music/ChordPlay/you`.
 */
@Composable
internal fun ChordPlayIdentityCard(
    name: String,
    onName: (String) -> Unit,
    onRestoreClick: () -> Unit,
    restoredStatus: String? = null,
    palette: MeshPalette? = null,
    tilt: () -> Pair<Float, Float> = { 0f to 0f },
    modifier: Modifier = Modifier,
) {
    val reduceAnimation by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    val disableInfiniteLoops by AppSettings.disableInfiniteLoops.collectAsStateWithLifecycle()
    val sig = remember(name) { WaveIdentitySignature.fromName(name) }
    val hasCustomName = name.isNotBlank()

    val targetPrimary = if (hasCustomName) sig.primaryColor else (palette?.colors?.getOrNull(0) ?: sig.primaryColor)
    val targetSecondary = if (hasCustomName) sig.secondaryColor else (palette?.colors?.getOrNull(1) ?: sig.secondaryColor)
    val targetTertiary = if (hasCustomName) sig.tertiaryColor else (palette?.colors?.getOrNull(2) ?: sig.tertiaryColor)

    val primaryGlow by animateColorAsState(
        targetValue = targetPrimary,
        animationSpec = tween(420),
        label = "identityPrimaryGlow",
    )
    val secondaryGlow by animateColorAsState(
        targetValue = targetSecondary,
        animationSpec = tween(420),
        label = "identitySecondaryGlow",
    )
    val tertiaryGlow by animateColorAsState(
        targetValue = targetTertiary,
        animationSpec = tween(420),
        label = "identityTertiaryGlow",
    )
    val waveAmplitude by animateFloatAsState(
        targetValue = sig.amplitude,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "identityWaveAmp",
    )
    val primaryFreq by animateFloatAsState(
        targetValue = sig.primaryFreq,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "identityFreq1",
    )
    val secondaryFreq by animateFloatAsState(
        targetValue = sig.secondaryFreq,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "identityFreq2",
    )

    val phase = if (reduceAnimation || disableInfiniteLoops) {
        0f
    } else {
        val infinite = rememberInfiniteTransition(label = "identityWaveMotion")
        val rawPhase by infinite.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "identityPhase",
        )
        rawPhase * sig.phaseSpeed
    }

    val shape = remember { RoundedCornerShape(26.dp) }
    val inputShape = remember { RoundedCornerShape(50) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                val (rx, ry) = tilt()
                rotationX = -rx
                rotationY = ry
                cameraDistance = 18f * density
            }
            .onbSurface(shape = shape, elevated = true)
            .clip(shape),
    ) {
        // Unique animated wave & orbital identity artwork inside the glass card
        Canvas(Modifier.matchParentSize()) {
            // Atmospheric color aura glows keyed to the name signature
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryGlow.copy(alpha = if (hasCustomName) 0.42f else 0.30f), Color.Transparent),
                    center = Offset(size.width * 0.82f, size.height * 0.24f),
                    radius = size.width * 0.56f,
                ),
                radius = size.width * 0.56f,
                center = Offset(size.width * 0.82f, size.height * 0.24f),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(secondaryGlow.copy(alpha = if (hasCustomName) 0.34f else 0.24f), Color.Transparent),
                    center = Offset(size.width * 0.14f, size.height * 0.78f),
                    radius = size.width * 0.50f,
                ),
                radius = size.width * 0.50f,
                center = Offset(size.width * 0.14f, size.height * 0.78f),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(tertiaryGlow.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(size.width * 0.52f, size.height * 0.56f),
                    radius = size.width * 0.42f,
                ),
                radius = size.width * 0.42f,
                center = Offset(size.width * 0.52f, size.height * 0.56f),
            )

            // Orbital harmonic rings + orbiting node
            val orbit = Offset(size.width * 0.84f, size.height * 0.32f)
            val baseRadius = size.height * 0.19f
            for (idx in 0 until sig.ringCount) {
                val ringRadius = baseRadius * (1f + idx * 0.36f)
                drawCircle(
                    color = Color.White.copy(alpha = (0.14f - idx * 0.022f).coerceAtLeast(0.03f)),
                    radius = ringRadius,
                    center = orbit,
                    style = Stroke(width = 1.15f * density),
                )
            }
            val orbitAngle = phase * 0.75f + 2.2f
            drawCircle(
                color = primaryGlow.copy(alpha = 0.92f),
                radius = 3.5f * density,
                center = Offset(
                    orbit.x + cos(orbitAngle) * baseRadius,
                    orbit.y + sin(orbitAngle) * baseRadius,
                ),
            )

            // Multi-layer acoustic waves whose intensity & harmonics respond to the name
            val waveMidY = size.height * 0.52f
            val steps = 48
            val primaryWave = Path().apply {
                for (i in 0..steps) {
                    val f = i / steps.toFloat()
                    val x = size.width * f
                    val env = sin(f * PI).toFloat()
                    val y = waveMidY + sin(f * PI * primaryFreq + phase).toFloat() * (16f * density * waveAmplitude) * env
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            drawPath(
                path = primaryWave,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        secondaryGlow.copy(alpha = 0.55f),
                        primaryGlow.copy(alpha = 0.85f),
                        tertiaryGlow.copy(alpha = 0.65f),
                        Color.Transparent,
                    ),
                ),
                style = Stroke(width = 2.0f * density, cap = StrokeCap.Round),
            )

            val secondaryWave = Path().apply {
                for (i in 0..steps) {
                    val f = i / steps.toFloat()
                    val x = size.width * f
                    val env = sin(f * PI).toFloat()
                    val y = waveMidY + sin(f * PI * secondaryFreq - phase * 0.8f + 0.9f).toFloat() * (11f * density * waveAmplitude) * env
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            drawPath(
                path = secondaryWave,
                color = Color.White.copy(alpha = 0.24f),
                style = Stroke(width = 1.2f * density, cap = StrokeCap.Round),
            )

            if (sig.waveCount >= 3) {
                val harmonicWave = Path().apply {
                    for (i in 0..steps) {
                        val f = i / steps.toFloat()
                        val x = size.width * f
                        val env = sin(f * PI).toFloat()
                        val y = waveMidY + sin(f * PI * (primaryFreq + 1.4f) + phase * 1.25f).toFloat() * (7.5f * density * waveAmplitude) * env
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }
                drawPath(
                    path = harmonicWave,
                    color = tertiaryGlow.copy(alpha = 0.42f),
                    style = Stroke(width = 1.0f * density, cap = StrokeCap.Round),
                )
            }

            if (sig.waveCount >= 4) {
                val microWave = Path().apply {
                    for (i in 0..steps) {
                        val f = i / steps.toFloat()
                        val x = size.width * f
                        val env = sin(f * PI).toFloat()
                        val y = waveMidY + cos(f * PI * (secondaryFreq + 2.1f) - phase * 1.1f).toFloat() * (5.5f * density * waveAmplitude) * env
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }
                drawPath(
                    path = microWave,
                    color = primaryGlow.copy(alpha = 0.34f),
                    style = Stroke(width = 0.9f * density, cap = StrokeCap.Round),
                )
            }

            // Unique 11-bar acoustic identity fingerprint at bottom-right
            val bars = sig.spectrumBars
            val barWidth = 2.8.dp.toPx()
            val barGap = 4.dp.toPx()
            val startX = size.width - 18.dp.toPx() - bars.size * (barWidth + barGap)
            val baseBottom = size.height - 14.dp.toPx()
            bars.forEachIndexed { idx, baseHeight ->
                val pulseFactor = if (reduceAnimation) {
                    1f
                } else {
                    0.82f + 0.18f * sin(phase * 1.4f + idx * 0.65f)
                }
                val barHeight = (20.dp.toPx() * baseHeight * waveAmplitude.coerceIn(0.75f, 1.15f) * pulseFactor)
                    .coerceAtLeast(3.dp.toPx())
                drawRoundRect(
                    color = when (idx % 3) {
                        0 -> primaryGlow.copy(alpha = 0.78f)
                        1 -> secondaryGlow.copy(alpha = 0.65f)
                        else -> Color.White.copy(alpha = 0.40f)
                    },
                    topLeft = Offset(startX + idx * (barWidth + barGap), baseBottom - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Top Header: Emblem, Identity Code, Clean Tagline & Restore Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val iconShape = RoundedCornerShape(15.dp)
                Image(
                    painter = painterResource(R.drawable.ic_chordplay_squircle),
                    contentDescription = "ChordPlay",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(iconShape)
                        .border(1.dp, Color.White.copy(alpha = 0.24f), iconShape),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "CHORDPLAY · ${sig.code}",
                        color = primaryGlow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.16.em,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.onboarding_hero_tagline),
                        color = Onb.Title,
                        fontSize = 17.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = OnbSubtitleTracking,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(0.75.dp, Color.White.copy(alpha = 0.20f), CircleShape)
                        .clickable(onClick = onRestoreClick)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FileOpen,
                        contentDescription = stringResource(R.string.onboarding_restore_identity),
                        tint = Onb.Title,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = stringResource(R.string.onboarding_restore_identity),
                        color = Onb.Title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
            }

            // Integrated Name Field directly inside the Identity Card
            BasicTextField(
                value = name,
                onValueChange = onName,
                singleLine = true,
                textStyle = TextStyle(
                    color = Onb.Title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = OnbSubtitleTracking,
                ),
                cursorBrush = SolidColor(primaryGlow),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                decorationBox = { inner ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(inputShape)
                            .background(Color.Black.copy(alpha = 0.28f))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = if (hasCustomName) primaryGlow else Onb.Subtitle,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Box(Modifier.weight(1f)) {
                            if (name.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.onboarding_identity_placeholder),
                                    color = Onb.Faint,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            inner()
                        }
                        if (name.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = null,
                                tint = Onb.Muted,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { onName("") },
                                    ),
                            )
                        }
                    }
                },
            )

            // Bottom local-storage or restored badge (leaves bottom-right room for the spectrum bars)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 92.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (!restoredStatus.isNullOrBlank()) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4ADE80),
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = restoredStatus,
                        color = Color(0xFF86EFAC),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.onboarding_identity_saved_hint),
                        color = Onb.Muted,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
