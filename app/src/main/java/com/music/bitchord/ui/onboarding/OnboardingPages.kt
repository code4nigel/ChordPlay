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

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BlurOff
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MotionPhotosOff
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.bitchord.R
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.settings.ChordPlayPalette
import com.music.bitchord.data.stats.ChordPlayIdentityStore
import com.music.bitchord.ui.components.GitHubIconBadge
import com.music.bitchord.ui.components.RedditIconBadge
import com.music.bitchord.ui.components.isGlassSupported
import com.music.bitchord.ui.player.MeshGradientBackground
import com.music.bitchord.ui.player.MeshPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// 1. Welcome, Wave Identity Card & Community Showcase
// ---------------------------------------------------------------------------

@Composable
internal fun WelcomeStep(
    name: String,
    palette: MeshPalette,
    onName: (String) -> Unit,
    onIdentityRestored: (String) -> Unit,
    footer: @Composable ColumnScope.() -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tilt = rememberDeviceTilt()
    var discovered by remember { mutableStateOf<List<ChordPlayIdentityStore.DiscoveredIdentity>>(emptyList()) }
    var restoredStatus by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        discovered = ChordPlayIdentityStore.discoverLocalIdentities(context)
    }

    fun restoreUri(uri: Uri) {
        scope.launch {
            ChordPlayIdentityStore.restoreFromUri(context, uri)
                .onSuccess { summary ->
                    restoredStatus = context.getString(
                        R.string.onboarding_identity_restored,
                        summary.name,
                        summary.settings,
                    )
                    onIdentityRestored(summary.name)
                }
                .onFailure { err ->
                    restoredStatus = err.message ?: context.getString(R.string.unknown_error)
                }
        }
    }

    val restorePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) restoreUri(uri)
    }

    OnboardingPage(
        badge = { PageBadge(Icons.Rounded.GraphicEq) },
        title = stringResource(R.string.onboarding_name_title),
        subtitle = stringResource(R.string.onboarding_welcome_subtitle),
        footer = footer,
    ) {
        ChordPlayIdentityCard(
            name = name,
            onName = onName,
            onRestoreClick = {
                restorePicker.launch(arrayOf("image/png", "application/json", "*/*"))
            },
            restoredStatus = restoredStatus,
            palette = palette,
            tilt = tilt,
        )

        if (discovered.isNotEmpty() && restoredStatus == null) {
            Spacer(Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(discovered.take(3), key = { "${it.code}:${it.name}" }) { item ->
                    val pillShape = remember { RoundedCornerShape(50) }
                    Row(
                        modifier = Modifier
                            .onbSurface(shape = pillShape)
                            .clip(pillShape)
                            .clickable { restoreUri(item.uri) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = null,
                            tint = Color(0xFF86EFAC),
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = stringResource(R.string.onboarding_welcome_back_restore, item.name),
                            color = Onb.Title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        OnbSectionLabel("COMMUNITY & OPEN SOURCE")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CommunityShowcaseCard(
                iconRes = R.drawable.ic_github,
                title = "GitHub",
                tagline = stringResource(R.string.onboarding_github_tag),
                accentPrimary = Color(0xFF818CF8),
                accentSecondary = Color(0xFFE2E8F0),
                isReddit = false,
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/code4nigel/ChordPlay")),
                        )
                    }
                },
                modifier = Modifier.weight(1f),
            )
            CommunityShowcaseCard(
                iconRes = R.drawable.ic_reddit,
                title = "Reddit",
                tagline = stringResource(R.string.onboarding_reddit_tag),
                accentPrimary = Color(0xFFFF4500),
                accentSecondary = Color(0xFFFF7847),
                isReddit = true,
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.reddit.com/r/ChordPlay/")),
                        )
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CommunityShowcaseCard(
    iconRes: Int,
    title: String,
    tagline: String,
    accentPrimary: Color,
    accentSecondary: Color,
    isReddit: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardShape = remember { RoundedCornerShape(24.dp) }
    OnbCard(
        modifier = modifier.height(136.dp),
        shape = cardShape,
        onClick = onClick,
    ) {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.matchParentSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accentPrimary.copy(alpha = 0.34f), Color.Transparent),
                        center = Offset(size.width * 0.22f, size.height * 0.22f),
                        radius = size.width * 0.78f,
                    ),
                    radius = size.width * 0.78f,
                    center = Offset(size.width * 0.22f, size.height * 0.22f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accentSecondary.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.88f, size.height * 0.86f),
                        radius = size.width * 0.55f,
                    ),
                    radius = size.width * 0.55f,
                    center = Offset(size.width * 0.88f, size.height * 0.86f),
                )

                if (isReddit) {
                    val center = Offset(size.width * 0.86f, size.height * 0.22f)
                    repeat(2) { idx ->
                        drawCircle(
                            color = accentSecondary.copy(alpha = 0.20f - idx * 0.06f),
                            radius = size.width * (0.20f + idx * 0.14f),
                            center = center,
                            style = Stroke(width = 1.dp.toPx()),
                        )
                    }
                } else {
                    // Subtle 4-point star sparkle for GitHub
                    val cx = size.width * 0.76f
                    val cy = size.height * 0.28f
                    val r = 7.dp.toPx()
                    val star = Path().apply {
                        moveTo(cx, cy - r)
                        quadraticTo(cx, cy, cx + r, cy)
                        quadraticTo(cx, cy, cx, cy + r)
                        quadraticTo(cx, cy, cx - r, cy)
                        quadraticTo(cx, cy, cx, cy - r)
                        close()
                    }
                    drawPath(star, color = accentSecondary.copy(alpha = 0.38f))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    if (isReddit) {
                        RedditIconBadge(size = 44.dp)
                    } else {
                        GitHubIconBadge(size = 44.dp)
                    }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .border(0.75.dp, Color.White.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NorthEast,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.88f),
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        color = Onb.Title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = tagline,
                        color = Onb.Subtitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 2. Design & Visual Style (Color Palette + Black OLED + Liquid Glass / Blur)
// ---------------------------------------------------------------------------

@Composable
internal fun DesignStyleStep(
    song: Song?,
    palette: MeshPalette,
    colorPalette: ChordPlayPalette,
    onColorPalette: (ChordPlayPalette) -> Unit,
    amoledDarkMode: Boolean,
    onAmoledDarkMode: (Boolean) -> Unit,
    liquidGlass: Boolean,
    onLiquidGlass: (Boolean) -> Unit,
    reduceBlur: Boolean,
    onReduceBlur: (Boolean) -> Unit,
    reduceAnimation: Boolean,
    disableInfiniteLoops: Boolean = reduceAnimation,
    onReduceAnimation: (Boolean) -> Unit,
    intensity: Float,
    onIntensity: (Float) -> Unit,
    pace: Float?,
    footer: @Composable ColumnScope.() -> Unit,
) {
    val glassAvailable = isGlassSupported()
    val stopInfinite = reduceAnimation || disableInfiniteLoops

    var live by remember { mutableFloatStateOf(intensity) }
    val publish = rememberUpdatedState(onIntensity)
    val published = remember { mutableStateOf(intensity) }
    LaunchedEffect(live) {
        if (live == published.value) return@LaunchedEffect
        delay(GLASS_SETTLE_MS)
        publish.value(live)
        published.value = live
    }
    DisposableEffect(Unit) {
        onDispose {
            if (live != published.value) publish.value(live)
        }
    }

    OnboardingPage(
        badge = { PageBadge(Icons.Rounded.AutoAwesome) },
        title = stringResource(R.string.onboarding_style_title),
        subtitle = stringResource(R.string.onboarding_style_subtitle),
        footer = footer,
    ) {
        NowPlayingStylePreview(
            song = song,
            palette = palette,
            colorPalette = colorPalette,
            amoledDarkMode = amoledDarkMode,
            liquidGlass = liquidGlass && glassAvailable,
            reduceBlur = reduceBlur,
            reduceAnimation = stopInfinite,
        )
        Spacer(Modifier.height(14.dp))
        OnbSectionLabel(stringResource(R.string.color_palette))
        PaletteSwatchStrip(
            selected = colorPalette,
            onSelect = onColorPalette,
            reduceAnimation = stopInfinite,
        )
        Spacer(Modifier.height(12.dp))
        OnbToggle(
            icon = Icons.Rounded.DarkMode,
            title = stringResource(R.string.onboarding_black_oled),
            description = stringResource(R.string.pure_black_amoled_desc),
            checked = amoledDarkMode,
            onCheckedChange = onAmoledDarkMode,
        )
        Spacer(Modifier.height(9.dp))
        OnbToggle(
            icon = Icons.Rounded.Animation,
            title = stringResource(R.string.liquid_glass),
            description = if (glassAvailable) {
                stringResource(R.string.onboarding_glass_desc)
            } else {
                stringResource(R.string.onboarding_glass_unsupported)
            },
            checked = liquidGlass && glassAvailable,
            onCheckedChange = onLiquidGlass,
            enabled = glassAvailable,
        )
        AnimatedVisibility(
            visible = liquidGlass && glassAvailable,
            enter = fadeIn(tween(220)) + expandVertically(spring(stiffness = Spring.StiffnessMediumLow)),
            exit = fadeOut(tween(160)) + shrinkVertically(),
        ) {
            Column {
                Spacer(Modifier.height(9.dp))
                OnbCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.onboarding_glass_strength),
                                color = Onb.Title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = "${(live * 100).toInt()}%",
                                color = Onb.Subtitle,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        GlassStrengthBar(value = live, onChange = { live = it })
                    }
                }
            }
        }
        Spacer(Modifier.height(9.dp))
        OnbToggle(
            icon = Icons.Rounded.BlurOff,
            title = stringResource(R.string.reduce_dynamic_blur),
            description = stringResource(R.string.onboarding_reduce_blur_desc),
            checked = reduceBlur,
            onCheckedChange = onReduceBlur,
        )
        Spacer(Modifier.height(9.dp))
        OnbToggle(
            icon = Icons.Rounded.MotionPhotosOff,
            title = stringResource(R.string.reduce_animation),
            description = stringResource(R.string.reduce_animation_subtitle),
            checked = reduceAnimation,
            onCheckedChange = onReduceAnimation,
        )
        if (pace != null && pace > 0.18f && liquidGlass && !reduceBlur) {
            Spacer(Modifier.height(10.dp))
            OnbCard(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = Onb.AccentCopper,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(
                            R.string.onboarding_frame_drop_hint,
                            (pace * 100).toInt(),
                        ),
                        color = Onb.Subtitle,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun PaletteSwatchStrip(
    selected: ChordPlayPalette,
    onSelect: (ChordPlayPalette) -> Unit,
    reduceAnimation: Boolean,
) {
    val haptics = LocalHapticFeedback.current
    val pillShape = remember { RoundedCornerShape(18.dp) }
    val prismColors = remember {
        listOf(
            Color(0xFFFF3B6B),
            Color(0xFFA855F7),
            Color(0xFF38BDF8),
            Color(0xFF34D399),
            Color(0xFFFBBF24),
            Color(0xFFFF3B6B),
        )
    }
    val prismRotation = if (reduceAnimation) {
        0f
    } else {
        val infinite = rememberInfiniteTransition(label = "dynamicArtShimmer")
        val deg by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "dynamicArtRotation",
        )
        deg
    }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(ChordPlayPalette.entries, key = { it.name }) { item ->
            val isSelected = item == selected
            val isDynamicArt = item == ChordPlayPalette.DYNAMIC_ARTWORK
            val primaryColor = Color(item.primaryColorHex)
            val secondaryColor = Color(item.secondaryColorHex)
            val selectScale by animateFloatAsState(
                targetValue = if (isSelected) 1.03f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
                label = "palettePillScale",
            )

            val borderModifier = if (isDynamicArt) {
                Modifier.border(
                    width = if (isSelected) 2.dp else 1.25.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFF4D79),
                            Color(0xFFA855F7),
                            Color(0xFF38BDF8),
                        ),
                    ),
                    shape = pillShape,
                )
            } else {
                Modifier.border(
                    width = if (isSelected) 1.6.dp else 0.75.dp,
                    color = if (isSelected) Onb.Title else Onb.Hairline,
                    shape = pillShape,
                )
            }

            OnbCard(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = selectScale
                        scaleY = selectScale
                    }
                    .then(borderModifier),
                shape = pillShape,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelect(item)
                },
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            when {
                                isDynamicArt && isSelected -> Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFFFF3B6B).copy(alpha = 0.26f),
                                        Color(0xFF7C4DFF).copy(alpha = 0.26f),
                                        Color(0xFF00D2D3).copy(alpha = 0.22f),
                                    ),
                                )
                                isDynamicArt -> Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFFFF3B6B).copy(alpha = 0.12f),
                                        Color(0xFF7C4DFF).copy(alpha = 0.12f),
                                    ),
                                )
                                isSelected -> Brush.horizontalGradient(
                                    listOf(
                                        primaryColor.copy(alpha = 0.24f),
                                        secondaryColor.copy(alpha = 0.16f),
                                    ),
                                )
                                else -> Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                            },
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (isDynamicArt) {
                        Box(
                            modifier = Modifier.size(22.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Canvas(Modifier.fillMaxSize()) {
                                rotate(prismRotation) {
                                    drawCircle(
                                        brush = Brush.sweepGradient(prismColors),
                                        radius = size.minDimension / 2f,
                                    )
                                }
                                drawCircle(
                                    color = Color.Black.copy(alpha = 0.28f),
                                    radius = size.minDimension / 2f,
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    } else {
                        Canvas(Modifier.size(18.dp)) {
                            drawArc(
                                color = primaryColor,
                                startAngle = -90f,
                                sweepAngle = 180f,
                                useCenter = true,
                            )
                            drawArc(
                                color = secondaryColor,
                                startAngle = 90f,
                                sweepAngle = 180f,
                                useCenter = true,
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = if (isSelected) 0.55f else 0.22f),
                                radius = size.minDimension / 2f,
                                style = Stroke(width = 1.dp.toPx()),
                            )
                        }
                    }
                    Text(
                        text = item.shortOnboardingLabel(),
                        color = if (isSelected || isDynamicArt) Onb.Title else Onb.Subtitle,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected || isDynamicArt) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                    )
                    if (isDynamicArt) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.18f))
                                .padding(horizontal = 5.dp, vertical = 1.5.dp),
                        ) {
                            Text(
                                text = "AUTO",
                                color = Color.White,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.06.em,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun ChordPlayPalette.shortOnboardingLabel(): String = when (this) {
    ChordPlayPalette.CHORDPLAY_CRIMSON -> "Crimson"
    ChordPlayPalette.DYNAMIC_ARTWORK -> "Dynamic Art"
    ChordPlayPalette.ELECTRIC_VIOLET -> "Violet"
    ChordPlayPalette.SUNSET_AMBER -> "Amber"
    ChordPlayPalette.EMERALD_NOIR -> "Emerald"
    ChordPlayPalette.MONOCHROME -> "Mono"
    ChordPlayPalette.DYNAMIC_MONET -> "Material You"
}

@Composable
private fun GlassStrengthBar(value: Float, onChange: (Float) -> Unit) {
    val clamped = value.coerceIn(0.1f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    onChange((change.position.x / size.width).coerceIn(0.1f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onChange((offset.x / size.width).coerceIn(0.1f, 1f))
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Onb.Hairline),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(clamped)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Onb.PillPrimary.copy(alpha = 0.9f)),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(clamped)
                .height(24.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Onb.PillPrimary),
            )
        }
    }
}

@Composable
private fun NowPlayingStylePreview(
    song: Song?,
    palette: MeshPalette,
    colorPalette: ChordPlayPalette,
    amoledDarkMode: Boolean,
    liquidGlass: Boolean,
    reduceBlur: Boolean,
    reduceAnimation: Boolean,
) {
    val shape = remember { RoundedCornerShape(26.dp) }
    var playing by remember { mutableStateOf(false) }

    val (rawPrimary, rawSecondary) = when (colorPalette) {
        ChordPlayPalette.DYNAMIC_MONET ->
            MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.secondary
        ChordPlayPalette.DYNAMIC_ARTWORK ->
            (palette.colors.getOrNull(0) ?: Color(colorPalette.primaryColorHex)) to
                (palette.colors.getOrNull(1) ?: Color(colorPalette.secondaryColorHex))
        else ->
            Color(colorPalette.primaryColorHex) to Color(colorPalette.secondaryColorHex)
    }
    val accentPrimary by animateColorAsState(
        targetValue = rawPrimary,
        animationSpec = tween(320),
        label = "previewAccentPrimary",
    )
    val accentSecondary by animateColorAsState(
        targetValue = rawSecondary,
        animationSpec = tween(320),
        label = "previewAccentSecondary",
    )
    val previewMeshPalette = remember(colorPalette, palette, accentPrimary, accentSecondary) {
        if (colorPalette == ChordPlayPalette.DYNAMIC_ARTWORK) {
            palette
        } else {
            MeshPalette(
                listOf(
                    accentPrimary,
                    accentSecondary,
                    accentPrimary.copy(alpha = 0.65f),
                    Color(0xFF14141C),
                ),
            )
        }
    }
    val playIconTint = if (accentPrimary.luminance() > 0.55f) Onb.Ink else Color.White

    val progressFraction by animateFloatAsState(
        targetValue = if (playing) 0.68f else 0.36f,
        animationSpec = tween(durationMillis = if (playing) 1800 else 360),
        label = "previewProgress",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(248.dp)
            .onbSurface(shape = shape, elevated = true)
            .clip(shape),
    ) {
        when {
            amoledDarkMode -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                )
            }
            !reduceBlur -> {
                MeshGradientBackground(
                    palette = previewMeshPalette,
                    modifier = Modifier.fillMaxSize(),
                    trackKey = "${colorPalette.name}:${song?.thumbnailUrl.orEmpty()}",
                    animated = !reduceAnimation,
                    blurRadius = 32.dp,
                )
            }
            else -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    accentPrimary.copy(alpha = 0.24f),
                                    Color(0xFF13131A),
                                ),
                            ),
                        ),
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = colorPalette.label.uppercase(),
                    color = Onb.Title.copy(alpha = 0.82f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.14.em,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (amoledDarkMode) {
                    PreviewBadgePill("OLED", opaque = reduceBlur || amoledDarkMode)
                }
                PreviewBadgePill(
                    when {
                        reduceBlur -> "LOW BLUR"
                        liquidGlass -> "LIQUID GLASS"
                        else -> "ADAPTIVE"
                    },
                    opaque = reduceBlur || amoledDarkMode,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (reduceBlur || amoledDarkMode) Onb.SurfaceHigh else Color.White.copy(alpha = 0.12f),
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp)),
                ) {
                    if (song?.thumbnailUrl != null) {
                        AsyncImage(
                            model = song.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = song?.title ?: stringResource(R.string.onboarding_preview_loading),
                        color = Onb.Title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = OnbSubtitleTracking,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = song?.artist ?: "ChordPlay WavePlay",
                        color = Onb.Title.copy(alpha = 0.74f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Onb.Title.copy(alpha = 0.18f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(accentPrimary.copy(alpha = 0.94f)),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = null,
                        tint = Onb.Title.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(26.dp))
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(accentPrimary)
                            .clickable { playing = !playing },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = playIconTint,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(26.dp))
                    Icon(
                        Icons.Rounded.Download,
                        contentDescription = null,
                        tint = Onb.Title.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewBadgePill(label: String, opaque: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (opaque) Onb.Surface.copy(alpha = 0.80f) else Color.White.copy(alpha = 0.14f))
            .border(0.6.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(50))
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            color = Onb.Title.copy(alpha = 0.90f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.08.em,
            maxLines = 1,
        )
    }
}

private const val GLASS_SETTLE_MS = 280L
