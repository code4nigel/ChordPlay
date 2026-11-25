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

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import com.music.bitchord.data.settings.SpeedDialTileStyle

/**
 * Applies ChordPlay's selectable edge/border treatment to a Speed Dial artwork tile.
 * Uses [drawWithCache] so strokes and gradient brushes allocate zero objects per frame.
 */
fun Modifier.speedDialTileEdge(
    style: SpeedDialTileStyle,
    shape: Shape,
    accent: Color,
    isDark: Boolean,
    isCurrent: Boolean,
): Modifier = drawWithCache {
    when (style) {
        SpeedDialTileStyle.MINIMAL -> {
            val strokeWidth = if (isCurrent) 1.5.dp.toPx() else 0.75.dp.toPx()
            val inset = strokeWidth / 2f
            val outline = shape.createOutline(
                size.copy(
                    width = (size.width - strokeWidth).coerceAtLeast(1f),
                    height = (size.height - strokeWidth).coerceAtLeast(1f),
                ),
                layoutDirection,
                this,
            )
            val borderColor = when {
                isCurrent -> accent.copy(alpha = 0.72f)
                isDark -> Color.White.copy(alpha = 0.11f)
                else -> Color.Black.copy(alpha = 0.09f)
            }
            onDrawWithContent {
                drawContent()
                translate(inset, inset) {
                    drawOutline(outline, color = borderColor, style = Stroke(strokeWidth))
                }
            }
        }

        SpeedDialTileStyle.BEVEL_EDGE -> {
            val strokeWidth = (if (isCurrent) 1.75.dp else 1.5.dp).toPx()
            val inset = strokeWidth / 2f
            val outline = shape.createOutline(
                size.copy(
                    width = (size.width - strokeWidth).coerceAtLeast(1f),
                    height = (size.height - strokeWidth).coerceAtLeast(1f),
                ),
                layoutDirection,
                this,
            )
            val rimBrush = if (isCurrent) {
                Brush.verticalGradient(
                    0f to accent.copy(alpha = 0.85f),
                    0.35f to Color.White.copy(alpha = if (isDark) 0.40f else 0.65f),
                    1f to accent.copy(alpha = 0.45f),
                )
            } else if (isDark) {
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.50f),
                    0.20f to Color.White.copy(alpha = 0.20f),
                    0.55f to Color.White.copy(alpha = 0.06f),
                    1f to Color.White.copy(alpha = 0.16f),
                )
            } else {
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.80f),
                    0.22f to Color.White.copy(alpha = 0.35f),
                    0.65f to Color.Black.copy(alpha = 0.06f),
                    1f to Color.Black.copy(alpha = 0.12f),
                )
            }
            val topSheen = Brush.verticalGradient(
                0f to Color.White.copy(alpha = if (isDark) 0.11f else 0.18f),
                0.15f to Color.Transparent,
            )
            onDrawWithContent {
                drawContent()
                drawRect(topSheen)
                translate(inset, inset) {
                    drawOutline(outline, brush = rimBrush, style = Stroke(strokeWidth))
                }
            }
        }

        SpeedDialTileStyle.FROSTED_GLASS -> {
            val haloWidth = 3.dp.toPx()
            val haloInset = haloWidth / 2f
            val haloOutline = shape.createOutline(
                size.copy(
                    width = (size.width - haloWidth).coerceAtLeast(1f),
                    height = (size.height - haloWidth).coerceAtLeast(1f),
                ),
                layoutDirection,
                this,
            )
            val rimWidth = (if (isCurrent) 1.5.dp else 1.25.dp).toPx()
            val rimInset = rimWidth / 2f
            val rimOutline = shape.createOutline(
                size.copy(
                    width = (size.width - rimWidth).coerceAtLeast(1f),
                    height = (size.height - rimWidth).coerceAtLeast(1f),
                ),
                layoutDirection,
                this,
            )
            val haloBrush = Brush.verticalGradient(
                0f to (if (isCurrent) accent else Color.White).copy(alpha = if (isDark) 0.22f else 0.32f),
                0.5f to accent.copy(alpha = if (isCurrent) 0.14f else 0.06f),
                1f to Color.White.copy(alpha = if (isDark) 0.10f else 0.18f),
            )
            val highlightBrush = Brush.verticalGradient(
                0f to (if (isCurrent) accent else Color.White).copy(alpha = if (isDark) 0.56f else 0.78f),
                0.30f to Color.White.copy(alpha = if (isDark) 0.18f else 0.28f),
                0.78f to accent.copy(alpha = if (isCurrent) 0.35f else 0.10f),
                1f to Color.White.copy(alpha = if (isDark) 0.22f else 0.32f),
            )
            onDrawWithContent {
                drawContent()
                translate(haloInset, haloInset) {
                    drawOutline(haloOutline, brush = haloBrush, style = Stroke(haloWidth))
                }
                translate(rimInset, rimInset) {
                    drawOutline(rimOutline, brush = highlightBrush, style = Stroke(rimWidth))
                }
            }
        }
    }
}
