/*
 * Adapted from Dial (https://github.com/steeben/Dial) by steeben.
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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics

/**
 * Apple-style frosted glass capsule switch: wide pill track with a milky glass
 * thumb that stretches toward its destination while pressed. Plain Canvas/draw
 * modifiers only (no per-switch blur capture) so long settings columns scroll
 * smoothly.
 */
@Composable
fun IosSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    @Suppress("UNUSED_PARAMETER") colors: SwitchColors? = null,
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reduceAnimation by AppSettings.reduceAnimation.collectAsStateWithLifecycle()

    val density = rememberFrostedGlassDensity()

    val position by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = if (reduceAnimation) snap() else spring(dampingRatio = 0.78f, stiffness = 700f),
        label = "iosSwitchThumb",
    )
    val stretch by animateFloatAsState(
        targetValue = if (pressed && !reduceAnimation) 1.18f else 1f,
        animationSpec = if (reduceAnimation) snap() else spring(dampingRatio = 0.7f, stiffness = 900f),
        label = "iosSwitchStretch",
    )
    val thumbClarity by animateFloatAsState(
        targetValue = if (pressed) 0.55f else 1f,
        animationSpec = if (reduceAnimation) snap() else spring(dampingRatio = 0.8f, stiffness = 900f),
        label = "iosSwitchThumbClarity",
    )
    val accent = MaterialTheme.colorScheme.primary
    val track by animateColorAsState(
        targetValue = if (checked) {
            frostedOnTint(dark, density, accent)
        } else {
            frostedTrackTint(dark, density)
        },
        label = "iosSwitchTrack",
    )
    val trackRim = if (dark) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.55f)
    val thumbFill = Color.White.copy(alpha = thumbAlpha(dark) * thumbClarity)
    val ltr = LocalLayoutDirection.current == LayoutDirection.Ltr

    val haptics = rememberHaptics()
    var settled by remember { mutableStateOf(false) }
    LaunchedEffect(checked) {
        if (settled) haptics.play(if (checked) Haptic.ToggleOn else Haptic.ToggleOff)
        settled = true
    }

    Box(
        modifier = modifier
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
            .then(
                if (onCheckedChange != null) {
                    Modifier.minimumInteractiveComponentSize().toggleable(
                        value = checked,
                        enabled = enabled,
                        interactionSource = interaction,
                        indication = null,
                        role = Role.Switch,
                        onValueChange = onCheckedChange,
                    )
                } else {
                    Modifier
                },
            )
            .size(TrackWidth, TrackHeight)
            .drawBehind {
                val radius = CornerRadius(size.height / 2f)
                drawRoundRect(color = track, cornerRadius = radius)
                drawRoundRect(brush = TRACK_SHEEN, cornerRadius = radius)
            }
            .border(GLASS_EDGE_WIDTH, trackRim, CircleShape),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .graphicsLayer {
                    val travel = (TrackWidth - ThumbWidth - ThumbPadding * 2).toPx()
                    val x = ThumbPadding.toPx() + travel * position
                    translationX = if (ltr) x else -x
                    transformOrigin = TransformOrigin(if (checked == ltr) 1f else 0f, 0.5f)
                    scaleX = stretch
                }
                .size(ThumbWidth, ThumbHeight)
                .shadow(1.5.dp, CircleShape, clip = false)
                .drawBehind {
                    val radius = CornerRadius(size.height / 2f)
                    drawRoundRect(color = thumbFill, cornerRadius = radius)
                    drawRoundRect(brush = THUMB_SHEEN, cornerRadius = radius)
                }
                .border(GLASS_EDGE_WIDTH, THUMB_RIM, CircleShape),
        )
    }
}

@Composable
internal fun rememberFrostedGlassDensity(): Float {
    val liquidGlass by AppSettings.liquidGlass.collectAsStateWithLifecycle()
    val intensity by AppSettings.liquidGlassIntensity.collectAsStateWithLifecycle()
    return if (liquidGlass && LocalLiquidGlassEnabled.current && isGlassSupported()) {
        (0.65f + 0.55f * intensity).coerceIn(0.55f, 1.25f)
    } else {
        1f
    }
}

internal fun Modifier.frostedGlassFill(tint: Color, shape: Shape, dark: Boolean): Modifier =
    this
        .clip(shape)
        .background(tint, shape)
        .background(TRACK_SHEEN, shape)
        .border(GLASS_EDGE_WIDTH, if (dark) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.55f), shape)

internal fun thumbAlpha(dark: Boolean): Float = if (dark) 0.84f else 0.9f

internal fun frostedTrackTint(dark: Boolean, density: Float): Color =
    if (dark) Color.White.copy(alpha = (0.16f * density).coerceIn(0f, 1f))
    else Color.Black.copy(alpha = (0.09f * density).coerceIn(0f, 1f))

internal fun frostedOnTint(dark: Boolean, density: Float, accent: Color? = null): Color {
    val chroma = accent?.let {
        maxOf(it.red, it.green, it.blue) - minOf(it.red, it.green, it.blue)
    } ?: 0f
    val baseColor = if (accent != null && chroma >= 0.08f) {
        accent
    } else if (dark) {
        Color(0xFF30D158)
    } else {
        Color(0xFF34C759)
    }
    return baseColor.copy(alpha = (ON_TRACK_ALPHA * density).coerceIn(0f, 1f))
}

internal const val ON_TRACK_ALPHA = 0.80f

private val TRACK_SHEEN = Brush.verticalGradient(
    0f to Color.White.copy(alpha = 0.20f),
    0.55f to Color.White.copy(alpha = 0.03f),
    1f to Color.Transparent,
)

internal val THUMB_SHEEN = Brush.verticalGradient(
    0f to Color.White.copy(alpha = 0.55f),
    0.5f to Color.White.copy(alpha = 0.08f),
    1f to Color.Black.copy(alpha = 0.06f),
)

internal val THUMB_RIM = Color.White.copy(alpha = 0.7f)

private val TrackWidth = 58.dp
private val TrackHeight = 28.dp
private val ThumbWidth = 36.dp
private val ThumbHeight = 24.dp
private val ThumbPadding = 2.dp
