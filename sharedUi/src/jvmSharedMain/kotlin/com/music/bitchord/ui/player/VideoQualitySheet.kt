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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.settings.VideoQuality
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import com.music.bitchord.ui.utils.containSheetGestures
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val QualityCardShape = RoundedCornerShape(28.dp)
private val QualityRowShape = RoundedCornerShape(16.dp)
private val FALLBACK_QUALITY_HEIGHTS = listOf(1080, 720, 480, 360)

/**
 * Unified floating glassy (or artwork-tinted opaque when Reduce Blur is enabled)
 * Video Playback Quality selector populated dynamically from the actual stream's
 * extracted resolutions, with network-aware Auto selected by default.
 */
@Composable
fun VideoQualitySheet(
    hazeState: HazeState?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color? = null,
) {
    val haptics = rememberHaptics()
    val activeQuality by PlayerSettings.videoQuality.collectAsStateWithLifecycle()
    val mvQuality by PlayerPlatform.host.musicVideoQuality.collectAsStateWithLifecycle()
    val reduceDynamicBlur by PlayerSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val reduceAnimation by PlayerSettings.reduceAnimation.collectAsStateWithLifecycle()
    val animationSpeed by PlayerSettings.playerAnimationSpeed.collectAsStateWithLifecycle()
    val effectiveAccent = accentColor ?: MaterialTheme.colorScheme.primary

    val availableHeights = remember(mvQuality.heights) {
        mvQuality.heights.filter { it > 0 }.distinct().sortedDescending()
            .ifEmpty { FALLBACK_QUALITY_HEIGHTS }
    }
    val autoSelected = mvQuality.held == null && activeQuality.isAuto
    val effectiveHeld = mvQuality.held ?: if (!activeQuality.isAuto) activeQuality.maxHeight else null

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val slideOffsetPx = with(density) { 36.dp.toPx() }
    val enterProgress = remember { Animatable(0f) }
    var dismissing by remember { mutableStateOf(false) }
    val speed = animationSpeed.coerceIn(0.5f, 2.0f)

    LaunchedEffect(Unit) {
        if (reduceAnimation) {
            enterProgress.snapTo(1f)
        } else {
            enterProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.86f,
                    stiffness = 460f * speed * speed,
                ),
            )
        }
    }

    val requestDismiss: () -> Unit = {
        if (!dismissing) {
            dismissing = true
            if (reduceAnimation) {
                onDismiss()
            } else {
                scope.launch {
                    val outDuration = (195f / speed).roundToInt().coerceIn(80, 340)
                    enterProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = outDuration, easing = DreamyEaseOutEasing),
                    )
                    onDismiss()
                }
            }
        }
    }

    PlayerBackHandler(enabled = true, onBack = requestDismiss)

    val p = enterProgress.value.coerceIn(0f, 1f)
    val surfaceModifier = playerModalSurfaceModifier(
        shape = QualityCardShape,
        hazeState = hazeState,
        accentColor = effectiveAccent,
        reduceDynamicBlur = reduceDynamicBlur,
    )

    val cardContent: @Composable (Modifier) -> Unit = { cardModifier ->
        Column(
            modifier = cardModifier
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .heightIn(max = 540.dp)
                .graphicsLayer {
                    alpha = p
                    if (!reduceAnimation) {
                        translationY = (1f - p) * slideOffsetPx
                        val s = lerp(0.94f, 1f, p)
                        scaleX = s
                        scaleY = s
                    }
                }
                .shadow(
                    elevation = 24.dp,
                    shape = QualityCardShape,
                    ambientColor = Color.Black.copy(alpha = 0.34f),
                    spotColor = Color.Black.copy(alpha = 0.34f),
                )
                .then(surfaceModifier)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Text(
                text = "Video Playback Quality",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Auto picks the sharpest stream your connection handles cleanly.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.68f),
            )
            Spacer(Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                val autoDetail = mvQuality.activeHeight?.takeIf { it > 0 }?.let { active ->
                    val tier = videoQualityLabel(active)
                    if (tier == "${active}p") "Adaptive · $tier" else "Adaptive · $tier (${active}p)"
                } ?: "Adaptive"

                VideoQualityOptionRow(
                    title = "Auto",
                    detail = autoDetail,
                    selected = autoSelected,
                    accent = effectiveAccent,
                    onClick = {
                        haptics.play(Haptic.Select)
                        PlayerPlatform.host.settings.setVideoQuality(VideoQuality.AUTO)
                        PlayerPlatform.host.setMusicVideoQuality(null)
                        requestDismiss()
                    },
                )

                availableHeights.forEach { height ->
                    val tier = videoQualityLabel(height)
                    val detail = if (tier == "${height}p") null else "${height}p"
                    VideoQualityOptionRow(
                        title = tier,
                        detail = detail,
                        selected = effectiveHeld == height,
                        accent = effectiveAccent,
                        onClick = {
                            haptics.play(Haptic.Select)
                            PlayerPlatform.host.setMusicVideoQuality(height)
                            requestDismiss()
                        },
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = requestDismiss) {
                    Text(
                        text = "Close",
                        color = effectiveAccent,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .containSheetGestures()
            .background(Color.Black.copy(alpha = (if (hazeState != null) 0.44f else 0.50f) * p))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = requestDismiss,
            )
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        cardContent(Modifier)
    }
}

@Composable
private fun VideoQualityOptionRow(
    title: String,
    detail: String?,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "qualityRowScale",
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.06f),
        animationSpec = tween(200),
        label = "qualityRowBg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.44f) else Color.White.copy(alpha = 0.08f),
        animationSpec = tween(200),
        label = "qualityRowBorder",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(QualityRowShape)
            .background(bgColor)
            .border(0.75.dp, borderColor, QualityRowShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.92f),
            modifier = Modifier.weight(1f),
        )
        if (detail != null) {
            Text(
                text = detail,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) accent else Color.White.copy(alpha = 0.58f),
                modifier = Modifier.padding(end = 8.dp),
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Selected",
                tint = accent,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
