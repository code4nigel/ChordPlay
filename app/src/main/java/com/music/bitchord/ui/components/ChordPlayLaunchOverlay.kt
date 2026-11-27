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
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.player.DreamyEaseOutEasing
import kotlin.math.roundToInt

private const val LAUNCH_ANIM_MS = 460
private val LAUNCH_START_ICON_SIZE = 84.dp
private val LAUNCH_TARGET_ICON_SIZE = 44.dp
private val TOP_LOCKUP_GUTTER = 16.dp
private val TOP_LOCKUP_TOP_PAD = 4.dp

/**
 * Brief one-shot `/chordplay-ui` Aurora Silk launch reveal: starts with `ic_logo` centered
 * inside an atmospheric nebula bloom over obsidian black, then glides and scales cleanly
 * into [HomeMorphingBrandLockup]'s top-left mark (`16.dp`, `statusBarTop + 4.dp`) while fading.
 * Unmounts on completion and never intercepts touch input.
 */
@Composable
fun ChordPlayLaunchOverlay(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduceAnimation by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    val disableInfiniteLoops by AppSettings.disableInfiniteLoops.collectAsStateWithLifecycle()
    val progress = remember { Animatable(0f) }

    LaunchedEffect(reduceAnimation, disableInfiniteLoops) {
        if (reduceAnimation || disableInfiniteLoops) {
            onFinished()
            return@LaunchedEffect
        }
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = LAUNCH_ANIM_MS, easing = DreamyEaseOutEasing),
        )
        onFinished()
    }

    if (reduceAnimation || disableInfiniteLoops) return

    val density = LocalDensity.current
    val statusBarTopDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val startSizePx = with(density) { LAUNCH_START_ICON_SIZE.toPx() }
        val targetSizePx = with(density) { LAUNCH_TARGET_ICON_SIZE.toPx() }
        val targetCenterX = with(density) { (TOP_LOCKUP_GUTTER + LAUNCH_TARGET_ICON_SIZE / 2f).toPx() }
        val targetCenterY = with(density) { (statusBarTopDp + TOP_LOCKUP_TOP_PAD + LAUNCH_TARGET_ICON_SIZE / 2f).toPx() }
        val targetScale = targetSizePx / startSizePx

        Canvas(modifier = Modifier.fillMaxSize()) {
            val p = progress.value.coerceIn(0f, 1f)
            val backdropAlpha = (1f - (p / 0.72f)).coerceIn(0f, 1f)
            if (backdropAlpha > 0f) {
                drawRect(Color.Black.copy(alpha = backdropAlpha))
            }

            val cx = (widthPx * 0.5f) + (targetCenterX - widthPx * 0.5f) * p
            val cy = (heightPx * 0.5f) + (targetCenterY - heightPx * 0.5f) * p
            val center = Offset(cx, cy)

            val auroraAlpha = (1f - (p / 0.78f)).coerceIn(0f, 1f)
            if (auroraAlpha > 0.005f) {
                val glowRadius = with(density) { (64.dp + 76.dp * p).toPx() }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primary.copy(alpha = 0.42f * auroraAlpha),
                            secondary.copy(alpha = 0.20f * auroraAlpha),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = glowRadius,
                    ),
                    radius = glowRadius,
                    center = center,
                )

                val ringRadius = with(density) { (38.dp + 54.dp * p).toPx() }
                drawCircle(
                    color = primary.copy(alpha = 0.36f * auroraAlpha),
                    radius = ringRadius,
                    center = center,
                    style = Stroke(width = with(density) { 1.5.dp.toPx() }),
                )
            }
        }

        Image(
            painter = painterResource(R.drawable.ic_logo),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(LAUNCH_START_ICON_SIZE)
                .offset {
                    val p = progress.value.coerceIn(0f, 1f)
                    val cx = (widthPx * 0.5f) + (targetCenterX - widthPx * 0.5f) * p
                    val cy = (heightPx * 0.5f) + (targetCenterY - heightPx * 0.5f) * p
                    IntOffset(
                        x = (cx - startSizePx * 0.5f).roundToInt(),
                        y = (cy - startSizePx * 0.5f).roundToInt(),
                    )
                }
                .graphicsLayer {
                    val p = progress.value.coerceIn(0f, 1f)
                    val s = 1f + (targetScale - 1f) * p
                    scaleX = s
                    scaleY = s
                    alpha = (1f - ((p - 0.82f) / 0.18f)).coerceIn(0f, 1f)
                },
        )
    }
}
