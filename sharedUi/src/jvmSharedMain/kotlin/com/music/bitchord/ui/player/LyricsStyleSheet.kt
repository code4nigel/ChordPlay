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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.sharedui.resources.Res
import com.music.bitchord.sharedui.resources.align_center
import com.music.bitchord.sharedui.resources.align_left
import com.music.bitchord.sharedui.resources.browse
import com.music.bitchord.sharedui.resources.change
import com.music.bitchord.sharedui.resources.custom_font_active_hint
import com.music.bitchord.sharedui.resources.font_family
import com.music.bitchord.sharedui.resources.font_family_default
import com.music.bitchord.sharedui.resources.font_family_monospace
import com.music.bitchord.sharedui.resources.font_family_sans_serif
import com.music.bitchord.sharedui.resources.font_family_script
import com.music.bitchord.sharedui.resources.font_family_serif
import com.music.bitchord.sharedui.resources.font_weight
import com.music.bitchord.sharedui.resources.font_weight_bold
import com.music.bitchord.sharedui.resources.font_weight_heavy
import com.music.bitchord.sharedui.resources.font_weight_medium
import com.music.bitchord.sharedui.resources.font_weight_semibold
import com.music.bitchord.sharedui.resources.import_font_hint
import com.music.bitchord.sharedui.resources.lyrics_highlight_center
import com.music.bitchord.sharedui.resources.lyrics_highlight_position
import com.music.bitchord.sharedui.resources.lyrics_highlight_top
import com.music.bitchord.sharedui.resources.lyrics_highlight_top_mid
import com.music.bitchord.sharedui.resources.lyrics_offset_reset
import com.music.bitchord.sharedui.resources.lyrics_options
import com.music.bitchord.sharedui.resources.lyrics_options_subtitle
import com.music.bitchord.sharedui.resources.music_feels_better
import com.music.bitchord.sharedui.resources.sync_offset
import com.music.bitchord.sharedui.resources.text_alignment
import com.music.bitchord.sharedui.resources.text_size
import com.music.bitchord.sharedui.resources.upload_custom_font
import com.music.bitchord.ui.components.PillSegmentedControl
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import com.music.bitchord.ui.utils.containSheetGestures
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val SheetShape = RoundedCornerShape(30.dp)
private val GroupCardShape = RoundedCornerShape(22.dp)
private val ActionPillShape = RoundedCornerShape(10.dp)
private val ScrimColor = Color.Black.copy(alpha = 0.44f)
private val DrawerMaxWidth = 640.dp
private const val DismissDragFraction = 0.25f

private const val MinFontScale = 0.55f
private const val MaxFontScale = 2.20f

/**
 * Floating frosted bottom sheet providing typography, layout, and timing
 * customization for the Now Playing lyrics surface.
 *
 * Uses the shared [playerModalSurfaceModifier], [PillSegmentedControl], and
 * [ThinSlider] + [relativeSlider] primitives so every group card, control
 * height, and haptic cue aligns with ChordPlay's updated iOS-inspired settings.
 */
@Composable
internal fun LyricsStyleSheet(
    hazeState: HazeState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color? = null,
) {
    val customStyle by LyricsStyleManager.style.collectAsStateWithLifecycle()
    val offsetMs by PlayerSettings.lyricsOffsetMs.collectAsStateWithLifecycle()
    val reduceDynamicBlur by PlayerSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val reduceAnimation by PlayerSettings.reduceAnimation.collectAsStateWithLifecycle()
    val animationSpeed by PlayerSettings.playerAnimationSpeed.collectAsStateWithLifecycle()

    val fontPicker = rememberCustomFontPicker { name, path -> LyricsStyleManager.setCustomFont(name, path) }
    val haptics = rememberHaptics()
    val effectiveAccent = accentColor ?: MaterialTheme.colorScheme.primary
    // Dark/saturated artwork accents (like deep crimson) lose contrast on dark frosted glass.
    // Keep effectiveAccent for filled pills/bars, and lift inkAccent for labels, badges, and icons.
    val inkAccent = remember(effectiveAccent) {
        if (effectiveAccent.luminance() < 0.44f) {
            lerpColor(effectiveAccent, Color.White, 0.58f)
        } else {
            effectiveAccent
        }
    }
    val sliderActiveColor = remember(effectiveAccent) {
        if (effectiveAccent.luminance() < 0.22f) {
            lerpColor(effectiveAccent, Color.White, 0.34f)
        } else {
            effectiveAccent
        }
    }

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val slideOffsetPx = with(density) { 44.dp.toPx() }
    val enterProgress = remember { Animatable(0f) }
    var dismissing by remember { mutableStateOf(false) }

    var dragY by remember { mutableFloatStateOf(0f) }
    var sheetHeight by remember { mutableIntStateOf(0) }
    val animatedDragY by animateFloatAsState(
        targetValue = dragY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "lyricsStyleDragY",
    )

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
                    val outDuration = (200f / speed).roundToInt().coerceIn(80, 340)
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

    var isDraggingFontSlider by remember { mutableStateOf(false) }
    var isDraggingOffsetSlider by remember { mutableStateOf(false) }
    val isDraggingAnySlider = isDraggingFontSlider || isDraggingOffsetSlider

    val sheetChromeAlpha by animateFloatAsState(
        targetValue = if (isDraggingAnySlider) 0f else 1f,
        animationSpec = tween(140),
        label = "lyricsSheetChromeAlpha",
    )
    val fontSliderCardAlpha by animateFloatAsState(
        targetValue = if (isDraggingOffsetSlider) 0f else 1f,
        animationSpec = tween(140),
        label = "lyricsFontSliderAlpha",
    )
    val offsetSliderCardAlpha by animateFloatAsState(
        targetValue = if (isDraggingFontSlider) 0f else 1f,
        animationSpec = tween(140),
        label = "lyricsOffsetSliderAlpha",
    )

    val p = enterProgress.value.coerceIn(0f, 1f)
    val dragAlpha = if (sheetHeight > 0) (1f - animatedDragY / sheetHeight).coerceIn(0f, 1f) else 1f
    val combinedAlpha = p * dragAlpha

    val scrollState = rememberScrollState()
    val dismissOnRelease: () -> Unit = {
        if (sheetHeight > 0 && dragY > sheetHeight * DismissDragFraction) requestDismiss() else dragY = 0f
    }

    val drawerScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (dragY <= 0f || available.y >= 0f) return Offset.Zero
                val used = available.y.coerceAtLeast(-dragY)
                dragY += used
                return Offset(0f, used)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
                    dragY += available.y
                }
                if (dragY > 0f && available.y < 0f) {
                    val used = available.y.coerceAtLeast(-dragY)
                    dragY += used
                    return Offset(0f, used)
                }
                return available
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (dragY <= 0f) return Velocity.Zero
                dismissOnRelease()
                return available
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                dismissOnRelease()
                return available
            }
        }
    }

    val isStyleModified = customStyle != LyricsCustomStyle() || offsetMs != 0
    val isDefaultScale = abs(customStyle.effectiveScale - 1.0f) < 0.02f

    Box(
        modifier = modifier
            .fillMaxSize()
            .containSheetGestures()
            .background(ScrimColor.copy(alpha = ScrimColor.alpha * combinedAlpha * sheetChromeAlpha))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = requestDismiss,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val surfaceModifier = playerModalSurfaceModifier(
            shape = SheetShape,
            hazeState = hazeState,
            accentColor = effectiveAccent,
            reduceDynamicBlur = reduceDynamicBlur,
        )

        Column(
            modifier = Modifier
                .widthIn(max = DrawerMaxWidth)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .heightIn(max = 670.dp)
                .onSizeChanged { sheetHeight = it.height }
                .offset { IntOffset(0, animatedDragY.roundToInt()) }
                .graphicsLayer {
                    alpha = p
                    if (!reduceAnimation) {
                        translationY = (1f - p) * slideOffsetPx
                        val s = lerp(0.95f, 1f, p)
                        scaleX = s
                        scaleY = s
                    }
                }
                .then(
                    if (isDraggingAnySlider) {
                        Modifier
                    } else {
                        Modifier
                            .shadow(
                                elevation = 24.dp,
                                shape = SheetShape,
                                ambientColor = Color.Black.copy(alpha = 0.35f),
                                spotColor = Color.Black.copy(alpha = 0.35f),
                            )
                            .then(surfaceModifier)
                    },
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                )
                .nestedScroll(drawerScroll)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(top = 10.dp, bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top handle + Header + Preview fade together when a slider peeks at live lyrics
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = sheetChromeAlpha },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(sheetHeight) {
                            detectVerticalDragGestures(
                                onDragEnd = dismissOnRelease,
                                onDragCancel = { dragY = 0f },
                            ) { _, delta -> dragY = (dragY + delta).coerceAtLeast(0f) }
                        }
                        .padding(bottom = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.28f)),
                    )
                }

                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.lyrics_options),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 20.sp,
                                lineHeight = 24.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = Color.White,
                        )
                        Text(
                            text = stringResource(Res.string.lyrics_options_subtitle),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.5.sp,
                                lineHeight = 17.sp,
                            ),
                            color = Color.White.copy(alpha = 0.62f),
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isStyleModified) {
                                    effectiveAccent.copy(alpha = 0.24f)
                                } else {
                                    Color.White.copy(alpha = 0.10f)
                                },
                            )
                            .border(
                                width = 0.75.dp,
                                color = if (isStyleModified) {
                                    inkAccent.copy(alpha = 0.52f)
                                } else {
                                    Color.White.copy(alpha = 0.16f)
                                },
                                shape = CircleShape,
                            )
                            .clickable(enabled = isStyleModified) {
                                haptics.play(Haptic.Tap)
                                LyricsStyleManager.reset()
                                PlayerSettings.setLyricsOffsetMs(0)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = stringResource(Res.string.lyrics_offset_reset),
                            tint = if (isStyleModified) inkAccent else Color.White.copy(alpha = 0.52f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Live Preview Card
                val previewFont = rememberLyricsFontFamily(customStyle)
                val previewFontSize = (26f * customStyle.effectiveScale).coerceIn(17f, 44f).sp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 84.dp)
                        .clip(GroupCardShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    effectiveAccent.copy(alpha = 0.14f),
                                    Color.White.copy(alpha = 0.05f),
                                ),
                            ),
                        )
                        .border(
                            width = 0.75.dp,
                            color = Color.White.copy(alpha = 0.12f),
                            shape = GroupCardShape,
                        )
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(Res.string.music_feels_better),
                        fontFamily = previewFont,
                        fontSize = previewFontSize,
                        lineHeight = (previewFontSize.value * 1.24f).sp,
                        fontWeight = customStyle.weightChoice.weight,
                        textAlign = if (customStyle.centerAligned) TextAlign.Center else TextAlign.Start,
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Group 1: Font Family + Custom Font
                val bundledFonts = remember { LyricsFontChoice.entries.filter { it != LyricsFontChoice.CUSTOM } }
                val fontLabels = bundledFonts.map { fontLabel(it) }
                val fontFamilies = bundledFonts.map { font ->
                    rememberLyricsFontFamily(customStyle.copy(fontChoice = font))
                }
                val isCustomActive = customStyle.fontChoice == LyricsFontChoice.CUSTOM
                val activeFontValue = if (isCustomActive) {
                    customStyle.customFontName ?: stringResource(Res.string.upload_custom_font)
                } else {
                    fontLabel(customStyle.fontChoice)
                }

                LyricsGroupCard(opaque = reduceDynamicBlur) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(Res.string.font_family),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                            )
                            Text(
                                text = activeFontValue,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = inkAccent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        PillSegmentedControl(
                            options = fontLabels,
                            selectedIndex = if (isCustomActive) -1 else bundledFonts.indexOf(customStyle.fontChoice),
                            onSelect = { index ->
                                haptics.play(Haptic.Select)
                                LyricsStyleManager.updateFont(bundledFonts[index])
                            },
                            accentColor = effectiveAccent,
                            optionFontFamilies = fontFamilies,
                            forceDarkSurface = true,
                            labelHorizontalPadding = 14.dp,
                        )
                    }

                    LyricsRowDivider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (customStyle.customFontPath != null) {
                                    haptics.play(Haptic.Select)
                                    LyricsStyleManager.updateFont(LyricsFontChoice.CUSTOM)
                                } else {
                                    haptics.play(Haptic.Tap)
                                    fontPicker()
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isCustomActive) effectiveAccent.copy(alpha = 0.24f)
                                    else Color.White.copy(alpha = 0.10f),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (isCustomActive) Icons.Rounded.Check else Icons.Rounded.Add,
                                contentDescription = null,
                                tint = if (isCustomActive) inkAccent else Color.White.copy(alpha = 0.90f),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = customStyle.customFontName ?: stringResource(Res.string.upload_custom_font),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = if (customStyle.customFontName != null) {
                                    stringResource(Res.string.custom_font_active_hint)
                                } else {
                                    stringResource(Res.string.import_font_hint)
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = Color.White.copy(alpha = 0.62f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(ActionPillShape)
                                .background(
                                    if (isCustomActive) effectiveAccent.copy(alpha = 0.24f)
                                    else Color.White.copy(alpha = 0.12f),
                                )
                                .border(
                                    width = 0.75.dp,
                                    color = if (isCustomActive) inkAccent.copy(alpha = 0.45f)
                                    else Color.White.copy(alpha = 0.16f),
                                    shape = ActionPillShape,
                                )
                                .clickable {
                                    haptics.play(Haptic.Tap)
                                    fontPicker()
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (customStyle.customFontName != null) {
                                    stringResource(Res.string.change)
                                } else {
                                    stringResource(Res.string.browse)
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isCustomActive) inkAccent else Color.White.copy(alpha = 0.94f),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
            }

            // Group 2A: Compact standalone Text Size card so dragging fades every other card completely
            LyricsGroupCard(
                opaque = reduceDynamicBlur || isDraggingFontSlider,
                backdropOnly = isDraggingFontSlider,
                modifier = Modifier.graphicsLayer { alpha = fontSliderCardAlpha },
            ) {
                LyricsSliderBlock(
                    title = stringResource(Res.string.text_size),
                    badgeText = "${(customStyle.effectiveScale * 100).roundToInt()}%",
                    isDefault = isDefaultScale,
                    resetLabel = stringResource(Res.string.lyrics_offset_reset),
                    onReset = {
                        haptics.play(Haptic.Tap)
                        LyricsStyleManager.updateFontScale(1.0f)
                    },
                    normalizedValue = ((customStyle.effectiveScale - MinFontScale) / (MaxFontScale - MinFontScale))
                        .coerceIn(0f, 1f),
                    steps = 32,
                    activeTrackColor = sliderActiveColor,
                    badgeFillColor = effectiveAccent,
                    inkAccent = inkAccent,
                    isDragging = isDraggingFontSlider,
                    onDraggingChange = { isDraggingFontSlider = it },
                    onNormalizedChange = { fraction ->
                        val rawScale = MinFontScale + fraction * (MaxFontScale - MinFontScale)
                        val snapped = ((rawScale * 100f).roundToInt() / 100f).coerceIn(MinFontScale, MaxFontScale)
                        LyricsStyleManager.updateFontScale(snapped)
                    },
                    onDecrement = {
                        haptics.play(Haptic.Tap)
                        val next = (((customStyle.effectiveScale - 0.05f) * 100f).roundToInt() / 100f)
                            .coerceIn(MinFontScale, MaxFontScale)
                        LyricsStyleManager.updateFontScale(next)
                    },
                    onIncrement = {
                        haptics.play(Haptic.Tap)
                        val next = (((customStyle.effectiveScale + 0.05f) * 100f).roundToInt() / 100f)
                            .coerceIn(MinFontScale, MaxFontScale)
                        LyricsStyleManager.updateFontScale(next)
                    },
                    decrementEnabled = customStyle.effectiveScale > MinFontScale + 0.01f,
                    incrementEnabled = customStyle.effectiveScale < MaxFontScale - 0.01f,
                    decrementDescription = stringResource(Res.string.text_size),
                    incrementDescription = stringResource(Res.string.text_size),
                )
            }

            // Group 2B: Font Weight + Alignment + Highlight Position fade completely with sheetChromeAlpha
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = sheetChromeAlpha },
            ) {
                Spacer(Modifier.height(12.dp))

                LyricsGroupCard(opaque = reduceDynamicBlur) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.font_weight),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                        val weightOptions = LyricsWeightChoice.entries.map { weightLabel(it) }
                        PillSegmentedControl(
                            options = weightOptions,
                            selectedIndex = LyricsWeightChoice.entries.indexOf(customStyle.weightChoice).coerceAtLeast(0),
                            onSelect = { index ->
                                haptics.play(Haptic.Select)
                                LyricsStyleManager.updateWeight(LyricsWeightChoice.entries[index])
                            },
                            accentColor = effectiveAccent,
                            forceDarkSurface = true,
                            labelHorizontalPadding = 8.dp,
                        )
                    }

                    LyricsRowDivider()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.text_alignment),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                        PillSegmentedControl(
                            options = listOf(
                                stringResource(Res.string.align_left),
                                stringResource(Res.string.align_center),
                            ),
                            selectedIndex = if (customStyle.centerAligned) 1 else 0,
                            onSelect = { index ->
                                haptics.play(Haptic.Select)
                                LyricsStyleManager.updateCenterAligned(index == 1)
                            },
                            accentColor = effectiveAccent,
                            forceDarkSurface = true,
                            labelHorizontalPadding = 12.dp,
                        )
                    }

                    LyricsRowDivider()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.lyrics_highlight_position),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                        val positionOptions = LyricsHighlightPosition.entries.map { highlightLabel(it) }
                        PillSegmentedControl(
                            options = positionOptions,
                            selectedIndex = LyricsHighlightPosition.entries.indexOf(customStyle.highlightPosition)
                                .coerceAtLeast(0),
                            onSelect = { index ->
                                haptics.play(Haptic.Select)
                                LyricsStyleManager.updateHighlightPosition(LyricsHighlightPosition.entries[index])
                            },
                            accentColor = effectiveAccent,
                            forceDarkSurface = true,
                            labelHorizontalPadding = 10.dp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Group 3: Sync Offset (stays visible while dragging so timing changes can be observed live)
            LyricsGroupCard(
                opaque = reduceDynamicBlur || isDraggingOffsetSlider,
                backdropOnly = isDraggingOffsetSlider,
                modifier = Modifier.graphicsLayer { alpha = offsetSliderCardAlpha },
            ) {
                val offsetSpan = (MAX_LYRICS_OFFSET_MS - MIN_LYRICS_OFFSET_MS).toFloat()
                LyricsSliderBlock(
                    title = stringResource(Res.string.sync_offset),
                    badgeText = String.format(Locale.getDefault(), "%+.1f s", offsetMs / 1_000f),
                    isDefault = offsetMs == 0,
                    resetLabel = stringResource(Res.string.lyrics_offset_reset),
                    onReset = {
                        haptics.play(Haptic.Tap)
                        PlayerSettings.setLyricsOffsetMs(0)
                    },
                    normalizedValue = ((offsetMs - MIN_LYRICS_OFFSET_MS) / offsetSpan).coerceIn(0f, 1f),
                    steps = (MAX_LYRICS_OFFSET_MS - MIN_LYRICS_OFFSET_MS) / 100 - 1,
                    activeTrackColor = sliderActiveColor,
                    badgeFillColor = effectiveAccent,
                    inkAccent = inkAccent,
                    isDragging = isDraggingOffsetSlider,
                    onDraggingChange = { isDraggingOffsetSlider = it },
                    onNormalizedChange = { fraction ->
                        val raw = MIN_LYRICS_OFFSET_MS + fraction * offsetSpan
                        val snapped = ((raw / 100f).roundToInt() * 100)
                            .coerceIn(MIN_LYRICS_OFFSET_MS, MAX_LYRICS_OFFSET_MS)
                        if (snapped != offsetMs) {
                            PlayerSettings.setLyricsOffsetMs(snapped)
                        }
                    },
                    onDecrement = {
                        haptics.play(Haptic.Select)
                        PlayerSettings.setLyricsOffsetMs((offsetMs - 100).coerceAtLeast(MIN_LYRICS_OFFSET_MS))
                    },
                    onIncrement = {
                        haptics.play(Haptic.Select)
                        PlayerSettings.setLyricsOffsetMs((offsetMs + 100).coerceAtMost(MAX_LYRICS_OFFSET_MS))
                    },
                    decrementEnabled = offsetMs > MIN_LYRICS_OFFSET_MS,
                    incrementEnabled = offsetMs < MAX_LYRICS_OFFSET_MS,
                    decrementDescription = stringResource(Res.string.sync_offset),
                    incrementDescription = stringResource(Res.string.sync_offset),
                )
            }
        }
    }
}

@Composable
private fun LyricsGroupCard(
    opaque: Boolean,
    modifier: Modifier = Modifier,
    backdropOnly: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val bgColor = when {
        backdropOnly -> Color(0xFF17181E).copy(alpha = 0.90f)
        opaque -> Color.White.copy(alpha = 0.06f)
        else -> Color.White.copy(alpha = 0.07f)
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(GroupCardShape)
            .background(bgColor)
            .border(
                width = 0.75.dp,
                color = Color.White.copy(alpha = if (backdropOnly) 0.18f else 0.10f),
                shape = GroupCardShape,
            ),
        content = content,
    )
}

@Composable
private fun LyricsRowDivider() {
    HorizontalDivider(
        thickness = 0.5.dp,
        color = Color.White.copy(alpha = 0.09f),
    )
}

@Composable
private fun LyricsSliderBlock(
    title: String,
    badgeText: String,
    isDefault: Boolean,
    resetLabel: String,
    onReset: () -> Unit,
    normalizedValue: Float,
    steps: Int,
    activeTrackColor: Color,
    badgeFillColor: Color,
    inkAccent: Color,
    isDragging: Boolean,
    onDraggingChange: (Boolean) -> Unit,
    onNormalizedChange: (Float) -> Unit,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    decrementEnabled: Boolean,
    incrementEnabled: Boolean,
    decrementDescription: String,
    incrementDescription: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeFillColor.copy(alpha = 0.28f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = inkAccent,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(ActionPillShape)
                    .background(
                        if (!isDefault) Color.White.copy(alpha = 0.14f)
                        else Color.White.copy(alpha = 0.08f),
                    )
                    .clickable(enabled = !isDefault, onClick = onReset)
                    .padding(horizontal = 9.dp, vertical = 5.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = resetLabel,
                    tint = if (!isDefault) inkAccent else Color.White.copy(alpha = 0.52f),
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    text = resetLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (!isDefault) inkAccent else Color.White.copy(alpha = 0.52f),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .relativeSlider(
                    label = title,
                    value = normalizedValue,
                    steps = steps,
                    onPressChange = onDraggingChange,
                    onValueChange = { next ->
                        onDraggingChange(true)
                        onNormalizedChange(next)
                    },
                    onFinished = { onDraggingChange(false) },
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StepCircleButton(
                icon = Icons.Rounded.Remove,
                contentDescription = decrementDescription,
                enabled = decrementEnabled,
                onClick = onDecrement,
            )
            ThinSlider(
                value = normalizedValue,
                onValueChange = null,
                idleHeight = 6.dp,
                activeHeight = 14.dp,
                active = isDragging,
                activeColor = activeTrackColor,
                inactiveColor = Color.White.copy(alpha = 0.22f),
                modifier = Modifier.weight(1f),
            )
            StepCircleButton(
                icon = Icons.Rounded.Add,
                contentDescription = incrementDescription,
                enabled = incrementEnabled,
                onClick = onIncrement,
            )
        }
    }
}

@Composable
private fun StepCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (enabled) 0.10f else 0.04f))
            .border(
                width = 0.75.dp,
                color = Color.White.copy(alpha = if (enabled) 0.14f else 0.06f),
                shape = CircleShape,
            )
            .alpha(if (enabled) 1f else 0.40f)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White.copy(alpha = 0.90f),
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun fontLabel(choice: LyricsFontChoice): String = when (choice) {
    LyricsFontChoice.DEFAULT -> stringResource(Res.string.font_family_default)
    LyricsFontChoice.SERIF -> stringResource(Res.string.font_family_serif)
    LyricsFontChoice.SANS_SERIF -> stringResource(Res.string.font_family_sans_serif)
    LyricsFontChoice.MONOSPACE -> stringResource(Res.string.font_family_monospace)
    LyricsFontChoice.CURSIVE -> stringResource(Res.string.font_family_script)
    LyricsFontChoice.CUSTOM -> stringResource(Res.string.upload_custom_font)
    LyricsFontChoice.POPPINS, LyricsFontChoice.PARKINSANS -> choice.label
}

@Composable
private fun weightLabel(choice: LyricsWeightChoice): String = when (choice) {
    LyricsWeightChoice.MEDIUM -> stringResource(Res.string.font_weight_medium)
    LyricsWeightChoice.SEMI_BOLD -> stringResource(Res.string.font_weight_semibold)
    LyricsWeightChoice.BOLD -> stringResource(Res.string.font_weight_bold)
    LyricsWeightChoice.HEAVY -> stringResource(Res.string.font_weight_heavy)
}

@Composable
private fun highlightLabel(position: LyricsHighlightPosition): String = when (position) {
    LyricsHighlightPosition.TOP -> stringResource(Res.string.lyrics_highlight_top)
    LyricsHighlightPosition.UPPER_CENTER -> stringResource(Res.string.lyrics_highlight_top_mid)
    LyricsHighlightPosition.CENTER -> stringResource(Res.string.lyrics_highlight_center)
}
