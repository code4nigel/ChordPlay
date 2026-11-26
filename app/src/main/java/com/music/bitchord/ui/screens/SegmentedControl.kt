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
package com.music.bitchord.ui.screens

import android.os.Build
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.components.GLASS_EDGE_WIDTH
import com.music.bitchord.ui.components.LocalLiquidGlassEnabled
import com.music.bitchord.ui.components.THUMB_RIM
import com.music.bitchord.ui.components.THUMB_SHEEN
import com.music.bitchord.ui.components.backdrop.Backdrop
import com.music.bitchord.ui.components.backdrop.backdrops.LayerBackdrop
import com.music.bitchord.ui.components.backdrop.backdrops.layerBackdrop
import com.music.bitchord.ui.components.backdrop.backdrops.rememberLayerBackdrop
import com.music.bitchord.ui.components.backdrop.drawBackdrop
import com.music.bitchord.ui.components.backdrop.effects.lens
import com.music.bitchord.ui.components.backdrop.highlight.Highlight
import com.music.bitchord.ui.components.backdrop.internal.ShapeProvider
import com.music.bitchord.ui.components.backdrop.shadow.Shadow
import com.music.bitchord.ui.components.backdrop.shadow.ShadowElement
import com.music.bitchord.ui.components.floatingtabbar.GlassPillMotion
import com.music.bitchord.ui.components.floatingtabbar.pillPlacement
import com.music.bitchord.ui.components.frostedGlassFill
import com.music.bitchord.ui.components.frostedTrackTint
import com.music.bitchord.ui.components.isGlassSupported
import com.music.bitchord.ui.components.lightweightLiquidGlass
import com.music.bitchord.ui.components.rememberFrostedGlassDensity
import com.music.bitchord.ui.components.thumbAlpha
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Apple-style segmented selector with a sliding milky frosted pill thumb
 * powered by [GlassPillMotion] (tap springs, drag-to-slide, and optional lens
 * refraction when Liquid Glass is enabled on Android 13+).
 */
@Composable
internal fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    glass: Boolean = false,
) {
    if (options.isEmpty()) return
    val haptics = rememberHaptics()
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val glassDensity = rememberFrostedGlassDensity()
    val reduceAnimation by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val lens = LocalLiquidGlassEnabled.current && isGlassSupported() && !reduceDynamicBlur &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    val count = options.size
    val shown = if (selectedIndex in 0 until count) selectedIndex else -1
    val currentShown by rememberUpdatedState(shown)
    val currentOnSelect by rememberUpdatedState(onSelect)

    val scope = rememberCoroutineScope()
    val pill = remember { GlassPillMotion(scope, shown.coerceAtLeast(0)) }
    pill.reduceMotion = reduceAnimation

    var rowSize by remember { mutableStateOf(IntSize.Zero) }
    var lastShown by remember { mutableIntStateOf(shown) }
    var lastTick by remember { mutableIntStateOf(shown) }
    val density = LocalDensity.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val gapPx = with(density) { SEGMENT_GAP.toPx() }
    val padPx = with(density) { TRACK_PADDING.toPx() }
    val cellWidthPx = if (rowSize.width > 0 && count > 0) (rowSize.width - gapPx * (count - 1)) / count else 0f
    val stepPx = if (rowSize.width > 0 && count > 0) (rowSize.width + gapPx) / count else 0f
    pill.stepDp = stepPx / density.density

    val rowHeightPx = rowSize.height.toFloat()
    val restSize = Size(cellWidthPx, rowHeightPx)
    val growPx = padPx * 2 + with(density) { THUMB_GROW.toPx() }
    val liftedSize = Size(cellWidthPx + growPx, rowHeightPx + growPx)
    val centerInRow: (Float) -> Offset = { position ->
        val x = position * stepPx + cellWidthPx / 2f
        Offset(if (isRtl) rowSize.width - x else x, rowHeightPx / 2f)
    }
    val showThumb = shown >= 0 && cellWidthPx > 0f
    val lensUp = lens && showThumb && !pill.isFlat

    LaunchedEffect(shown) {
        if (shown >= 0) {
            if (lastShown < 0) pill.snapTo(shown) else pill.animateTo(shown)
        }
        lastShown = shown
        lastTick = shown
    }
    LaunchedEffect(count) {
        if (shown >= 0) pill.snapTo(shown)
    }

    val cover: (Int) -> Float = { index ->
        if (!showThumb) {
            0f
        } else {
            (1f - abs(pill.position - index)).coerceIn(0f, 1f) * (if (lens) 1f - pill.material else 1f)
        }
    }

    val labels = rememberLayerBackdrop()
    val cutout = remember { Path() }
    val thumbShadow = remember(lens) {
        ShadowElement(ShapeProvider { ThumbShape }) {
            val alpha = if (lens) 1f - pill.material else 1f
            if (alpha > 0f) THUMB_SHADOW.copy(alpha = alpha) else null
        }
    }
    val labelStyle = MaterialTheme.typography.labelMedium
    val idleColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .matchParentSize()
                .then(
                    if (glass) {
                        Modifier.lightweightLiquidGlass(
                            shape = TrackShape,
                            fallbackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        )
                    } else {
                        Modifier.frostedGlassFill(frostedTrackTint(dark, glassDensity), TrackShape, dark)
                    },
                ),
        )

        if (showThumb) {
            Box(
                Modifier
                    .matchParentSize()
                    .pillPlacement(
                        center = { centerInRow(pill.position) + Offset(padPx, padPx) },
                        size = { pill.liveSize(restSize, liftedSize) },
                    )
                    .then(thumbShadow)
                    .drawBehind {
                        val lifted = if (lens) pill.material else 0f
                        val rest = thumbAlpha(dark)
                        val radius = CornerRadius(THUMB_CORNER.toPx())
                        drawRoundRect(
                            color = Color.White.copy(alpha = rest + (liftedThumbAlpha(dark) - rest) * lifted),
                            cornerRadius = radius,
                        )
                        drawRoundRect(brush = THUMB_SHEEN, cornerRadius = radius, alpha = 1f - 0.5f * lifted)
                        val edge = GLASS_EDGE_WIDTH.toPx()
                        drawRoundRect(
                            color = THUMB_RIM,
                            topLeft = Offset(edge / 2f, edge / 2f),
                            size = Size(size.width - edge, size.height - edge),
                            cornerRadius = radius,
                            style = Stroke(edge),
                        )
                    },
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TRACK_PADDING)
                .onSizeChanged { rowSize = it }
                .drawWithContent {
                    if (lensUp) {
                        val centre = centerInRow(pill.position)
                        val live = pill.liveSize(restSize, liftedSize)
                        cutout.reset()
                        cutout.addRoundRect(
                            RoundRect(
                                Rect(
                                    offset = Offset(centre.x - live.width / 2f, centre.y - live.height / 2f),
                                    size = live,
                                ),
                                CornerRadius(THUMB_CORNER.toPx()),
                            ),
                        )
                        clipPath(cutout, clipOp = ClipOp.Difference) { this@drawWithContent.drawContent() }
                    } else {
                        drawContent()
                    }
                }
                .then(if (lens) Modifier.layerBackdrop(labels) else Modifier)
                .pointerInput(count, stepPx, isRtl, enabled, shown >= 0) {
                    if (!enabled || currentShown < 0 || stepPx <= 0f) return@pointerInput

                    val direction = if (isRtl) -1f else 1f
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = {
                            totalDrag = 0f
                            pill.startDrag()
                        },
                        onDragCancel = { pill.release(currentShown.coerceAtLeast(0)) },
                        onDragEnd = {
                            val from = currentShown.coerceAtLeast(0)
                            val ratio = totalDrag / stepPx
                            val shift = when {
                                ratio > 0.35f -> maxOf(1, ratio.roundToInt())
                                ratio < -0.35f -> minOf(-1, ratio.roundToInt())
                                else -> 0
                            }
                            val next = (from + shift).coerceIn(0, count - 1)
                            pill.release(next)
                            if (next != from) currentOnSelect(next)
                        },
                        onHorizontalDrag = { _, delta ->
                            val from = currentShown.coerceAtLeast(0)
                            totalDrag += delta * direction
                            val offset = when {
                                totalDrag > 0 && from == count - 1 -> totalDrag * 0.25f
                                totalDrag < 0 && from == 0 -> totalDrag * 0.25f
                                else -> totalDrag
                            }
                            pill.dragTo(from + offset / stepPx)

                            val over = (from + offset / stepPx).coerceIn(0f, (count - 1).toFloat()).roundToInt()
                            if (over != lastTick) {
                                haptics.play(Haptic.Tick)
                                lastTick = over
                            }
                        },
                    )
                },
            horizontalArrangement = Arrangement.spacedBy(SEGMENT_GAP),
        ) {
            options.forEachIndexed { index, label ->
                val chosen = index == shown
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .selectable(
                            selected = chosen,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = enabled,
                            role = Role.Tab,
                        ) {
                            if (!chosen) {
                                haptics.play(Haptic.Select)
                                onSelect(index)
                            }
                        }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = labelStyle,
                        color = idleColor,
                        maxLines = 1,
                        modifier = Modifier.graphicsLayer {
                            alpha = 1f - cover(index)
                            compositingStrategy = CompositingStrategy.ModulateAlpha
                        },
                    )
                    Text(
                        text = label,
                        style = labelStyle,
                        color = Color.Black.copy(alpha = 0.87f),
                        maxLines = 1,
                        modifier = Modifier.graphicsLayer {
                            alpha = cover(index)
                            compositingStrategy = CompositingStrategy.ModulateAlpha
                        },
                    )
                }
            }
        }

        if (lensUp) {
            SelectorLens(
                motion = pill,
                labels = labels,
                modifier = Modifier
                    .matchParentSize()
                    .pillPlacement(
                        center = { centerInRow(pill.position) + Offset(padPx, padPx) },
                        size = { pill.liveSize(restSize, liftedSize) },
                    ),
            )
        }
    }
}

@Composable
private fun SelectorLens(
    motion: GlassPillMotion,
    labels: LayerBackdrop,
    modifier: Modifier,
) {
    val backdrop = remember(labels, motion) {
        MagnifiedBackdrop(labels) { 1f + LENS_MAGNIFY * motion.material }
    }
    Box(
        modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { ThumbShape },
            effects = {
                lens(
                    refractionHeight = LENS_HEIGHT.toPx() * motion.glassPresence,
                    refractionAmount = LENS_AMOUNT.toPx() * motion.material,
                    chromaticAberration = true,
                )
            },
            highlight = {
                val alpha = motion.material * rimOf(motion.glassPresence)
                if (alpha > 0f) Highlight.Default.copy(alpha = alpha) else null
            },
            shadow = {
                val alpha = motion.material * rimOf(motion.glassPresence)
                if (alpha > 0f) LENS_SHADOW.copy(alpha = alpha) else null
            },
        ),
    )
}

private fun rimOf(presence: Float): Float = ((presence - 0.45f) / 0.55f).coerceIn(0f, 1f)

private class MagnifiedBackdrop(
    private val content: Backdrop,
    private val magnify: () -> Float,
) : Backdrop {

    override val isCoordinatesDependent: Boolean = true

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
    ) {
        val factor = magnify()
        if (factor != 1f && coordinates != null) {
            val pivot = Offset(coordinates.size.width / 2f, coordinates.size.height / 2f)
            scale(factor, pivot) { with(content) { drawBackdrop(density, coordinates, layerBlock) } }
        } else {
            with(content) { drawBackdrop(density, coordinates, layerBlock) }
        }
    }
}

private fun liftedThumbAlpha(dark: Boolean): Float = if (dark) 0.12f else 0.35f

private val TRACK_PADDING = 4.dp
private val SEGMENT_GAP = 2.dp
private val THUMB_GROW = 6.dp
private val THUMB_CORNER = 8.dp
private val TrackShape = RoundedCornerShape(10.dp)
private val ThumbShape = RoundedCornerShape(THUMB_CORNER)

private val THUMB_SHADOW = Shadow(
    radius = 2.dp,
    offset = DpOffset(0.dp, 1.dp),
    color = Color.Black.copy(alpha = 0.22f),
)

private const val LENS_MAGNIFY = 0.14f
private val LENS_HEIGHT = 8.dp
private val LENS_AMOUNT = 10.dp

private val LENS_SHADOW = Shadow(
    radius = 10.dp,
    offset = DpOffset(0.dp, 2.dp),
    color = Color.Black.copy(alpha = 0.28f),
)
