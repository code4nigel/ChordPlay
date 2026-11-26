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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppSettings
import kotlin.math.PI
import kotlin.math.sin

internal const val MIN_CHECK_SPIN_MS = 1200L

/**
 * ChordPlay's animated wave mark used when checking for updates:
 * emits layered aurora sine-wave filaments and concentric sonic rings around
 * [R.drawable.ic_logo], settling smoothly back to rest once done and staying
 * static under [AppSettings.reduceAnimation].
 */
@Composable
fun ChordPlayWaveUpdateMark(
    checking: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    waveWidth: Dp = 156.dp,
    tint: Color = MaterialTheme.colorScheme.primary,
    secondaryTint: Color = MaterialTheme.colorScheme.secondary,
) {
    val reduceAnimation by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    val activeMix by animateFloatAsState(
        targetValue = if (checking) 1f else 0f,
        animationSpec = spring(stiffness = 280f),
        label = "chordplayWaveActiveMix",
    )

    val phase = if (checking && !reduceAnimation) {
        val transition = rememberInfiniteTransition(label = "chordplayUpdateWave")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "wavePhase",
        ).value
    } else {
        0f
    }

    val pulse = if (checking && !reduceAnimation) {
        val transition = rememberInfiniteTransition(label = "chordplayUpdatePulse")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "ringPulse",
        ).value
    } else {
        0f
    }

    Box(
        modifier = modifier.size(width = waveWidth, height = size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val cy = h / 2f
            val cx = w / 2f
            val coreRadius = h * 0.36f

            // Concentric radar / sonic rings while checking
            if (activeMix > 0.01f && !reduceAnimation) {
                for (i in 0 until 2) {
                    val t = (pulse + i * 0.5f) % 1f
                    val radius = coreRadius + (h * 0.42f * t)
                    val ringAlpha = (1f - t) * 0.38f * activeMix
                    drawCircle(
                        color = (if (i == 0) tint else secondaryTint).copy(alpha = ringAlpha),
                        radius = radius,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.6.dp.toPx()),
                    )
                }
            }

            // Twin tapered aurora sine ribbons flowing horizontally behind the emblem
            val steps = 48
            val primaryPath = Path()
            val secondaryPath = Path()
            val baseAmp = if (activeMix > 0.01f && !reduceAnimation) {
                h * (0.08f + 0.16f * activeMix)
            } else {
                h * 0.08f
            }

            for (i in 0..steps) {
                val u = i.toFloat() / steps
                val x = w * 0.04f + w * 0.92f * u
                val taper = sin(PI * u).toFloat().let { it * it }
                val y1 = cy + sin((u * 4.0 * PI - phase).toDouble()).toFloat() * baseAmp * taper
                val y2 = cy + sin((u * 5.0 * PI + phase * 1.3).toDouble()).toFloat() * (baseAmp * 0.72f) * taper
                if (i == 0) {
                    primaryPath.moveTo(x, y1)
                    secondaryPath.moveTo(x, y2)
                } else {
                    primaryPath.lineTo(x, y1)
                    secondaryPath.lineTo(x, y2)
                }
            }

            val restAlpha = 0.20f + 0.55f * activeMix
            drawPath(
                path = secondaryPath,
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, secondaryTint.copy(alpha = restAlpha * 0.7f), Color.Transparent),
                ),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
            )
            drawPath(
                path = primaryPath,
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, tint.copy(alpha = restAlpha), secondaryTint.copy(alpha = restAlpha * 0.85f), Color.Transparent),
                ),
                style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        val iconScale = if (checking && !reduceAnimation) {
            1f + 0.06f * sin(phase * 2f)
        } else {
            1f
        }
        Box(
            modifier = Modifier
                .size(size * 0.68f)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                }
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.16f + 0.10f * activeMix)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_logo),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(size * 0.42f),
            )
        }
    }
}
