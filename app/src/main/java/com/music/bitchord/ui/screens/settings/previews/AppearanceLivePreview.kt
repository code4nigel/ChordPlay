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
package com.music.bitchord.ui.screens.settings.previews

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.ChordPlayPalette
import com.music.bitchord.data.settings.DockStyle
import com.music.bitchord.data.settings.MiniPlayerTimelineStyle
import com.music.bitchord.data.settings.NavPage
import com.music.bitchord.data.settings.ThemeMode
import com.music.bitchord.ui.icons.BitChordIcons
import com.music.bitchord.ui.performance.displayDensityLabel
import com.music.bitchord.ui.screens.localizedLabel
import com.music.bitchord.ui.screens.settings.localizedLabel
import com.music.bitchord.ui.theme.fontFamilyFor

/**
 * Live Appearance preview mirroring ChordPlay's active theme mode, AMOLED setting,
 * palette or custom preset, selected app typography, display density scale,
 * dock style (Normal vs Adaptive vs Liquid Glass) with blur/glass intensity,
 * miniplayer timeline accent, and configured bottom dock tabs.
 */
@Composable
fun AppearanceLivePreview(
    modifier: Modifier = Modifier,
) {
    val showLivePreviews by AppSettings.showSettingsLivePreviews.collectAsStateWithLifecycle()
    if (!showLivePreviews) return

    val theme by AppSettings.themeMode.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    val amoledDarkMode by AppSettings.amoledDarkMode.collectAsStateWithLifecycle()
    val appFontChoice by AppSettings.appFontChoice.collectAsStateWithLifecycle()
    val displayDensityScale by AppSettings.displayDensityScale.collectAsStateWithLifecycle()
    val palette by AppSettings.colorPalette.collectAsStateWithLifecycle()
    val dockStyle by AppSettings.dockStyle.collectAsStateWithLifecycle()
    val adaptiveDockBlurIntensity by AppSettings.adaptiveDockBlurIntensity.collectAsStateWithLifecycle()
    val liquidGlass by AppSettings.liquidGlass.collectAsStateWithLifecycle()
    val liquidGlassIntensity by AppSettings.liquidGlassIntensity.collectAsStateWithLifecycle()
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val navTabs by AppSettings.navTabs.collectAsStateWithLifecycle()
    val miniPlayerTimelineStyle by AppSettings.miniPlayerTimelineStyle.collectAsStateWithLifecycle()
    val activeArtworkUrl by AppSettings.activeTrackArtworkUrl.collectAsStateWithLifecycle()
    val activeTitle by AppSettings.activeTrackTitle.collectAsStateWithLifecycle()
    val activeArtist by AppSettings.activeTrackArtist.collectAsStateWithLifecycle()
    val customPalettes by AppSettings.customPalettes.collectAsStateWithLifecycle()
    val activeCustomPaletteId by AppSettings.activeCustomPaletteId.collectAsStateWithLifecycle()
    val activeCustomPalette = activeCustomPaletteId?.let { id ->
        customPalettes.firstOrNull { it.id == id }
    }

    val isLight = when (theme) {
        ThemeMode.LIGHT -> true
        ThemeMode.DARK -> false
        ThemeMode.SYSTEM -> !systemDark
    }
    val fontFamily = fontFamilyFor(appFontChoice)

    val activePrimary = if (activeCustomPalette != null) {
        Color(activeCustomPalette.primaryHex)
    } else if (palette == ChordPlayPalette.DYNAMIC_MONET) {
        MaterialTheme.colorScheme.primary
    } else {
        Color(palette.primaryColorHex)
    }

    val activeSecondary = if (activeCustomPalette != null) {
        Color(activeCustomPalette.secondaryHex)
    } else if (palette == ChordPlayPalette.DYNAMIC_MONET) {
        MaterialTheme.colorScheme.secondary
    } else {
        Color(palette.secondaryColorHex)
    }

    val targetBg = when {
        !isLight && amoledDarkMode -> Color.Black
        activeCustomPalette != null -> {
            Color(activeCustomPalette.surfaceHex).copy(alpha = if (reduceDynamicBlur) 1f else 0.92f)
        }
        palette == ChordPlayPalette.DYNAMIC_MONET -> {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (reduceDynamicBlur) 1f else 0.90f)
        }
        isLight && reduceDynamicBlur -> Color(0xFFF0F2F6)
        isLight -> Color(0xFFF7F8FA).copy(alpha = 0.96f)
        reduceDynamicBlur -> when (palette) {
            ChordPlayPalette.CHORDPLAY_CRIMSON -> Color(0xFF160D12)
            ChordPlayPalette.ELECTRIC_VIOLET -> Color(0xFF141622)
            ChordPlayPalette.SUNSET_AMBER -> Color(0xFF1A1410)
            ChordPlayPalette.EMERALD_NOIR -> Color(0xFF0F1D16)
            ChordPlayPalette.MONOCHROME -> Color(0xFF121214)
            ChordPlayPalette.DYNAMIC_ARTWORK -> Color(0xFF15161B)
            ChordPlayPalette.DYNAMIC_MONET -> MaterialTheme.colorScheme.surfaceVariant
        }
        else -> when (palette) {
            ChordPlayPalette.CHORDPLAY_CRIMSON -> Color(0xFF160D12).copy(alpha = 0.92f)
            ChordPlayPalette.ELECTRIC_VIOLET -> Color(0xFF141622).copy(alpha = 0.92f)
            ChordPlayPalette.SUNSET_AMBER -> Color(0xFF1A1410).copy(alpha = 0.92f)
            ChordPlayPalette.EMERALD_NOIR -> Color(0xFF0F1D16).copy(alpha = 0.92f)
            ChordPlayPalette.MONOCHROME -> Color(0xFF121214).copy(alpha = 0.92f)
            ChordPlayPalette.DYNAMIC_ARTWORK -> Color(0xFF15161B).copy(alpha = 0.92f)
            ChordPlayPalette.DYNAMIC_MONET -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f)
        }
    }

    val animatedBg by animateColorAsState(targetValue = targetBg, animationSpec = tween(280), label = "appearanceBg")
    val animatedPrimary by animateColorAsState(targetValue = activePrimary, animationSpec = tween(280), label = "appearancePrimary")

    val textColor = if (isLight) Color(0xFF121318) else Color.White
    val subtextColor = if (isLight) Color(0xFF5A5E6B) else Color.White.copy(alpha = 0.68f)
    val tileSurface = if (isLight) Color(0xFFE7EAF2).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.08f)

    val effectiveGlassIntensity = when {
        dockStyle == DockStyle.LIQUID || liquidGlass -> liquidGlassIntensity
        dockStyle == DockStyle.ADAPTIVE -> adaptiveDockBlurIntensity
        else -> 0.25f
    }.coerceIn(0.1f, 1f)

    val borderColor = when {
        dockStyle == DockStyle.LIQUID || liquidGlass -> animatedPrimary.copy(alpha = 0.22f + 0.28f * effectiveGlassIntensity)
        isLight -> MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
        else -> animatedPrimary.copy(alpha = 0.22f)
    }

    val densityMultiplier = displayDensityScale.coerceIn(0.85f, 1.15f)
    val visibleTabs = AppSettings.visibleNavPages(navTabs)

    val paletteLabel = activeCustomPalette?.name ?: palette.label
    val dockLabel = dockStyle.localizedLabel()
    val densityLabel = displayDensityLabel(displayDensityScale)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = animatedPrimary.copy(alpha = 0.18f),
            )
            .clip(RoundedCornerShape(22.dp))
            .background(animatedBg)
            .border(1.dp, borderColor, RoundedCornerShape(22.dp))
            .padding(14.dp),
    ) {
        CompositionLocalProvider(LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = fontFamily)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header Row reflecting real active settings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(animatedPrimary),
                    )
                    Text(
                        text = "PREVIEW",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = fontFamily,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                        ),
                        color = animatedPrimary,
                    )
                    Text(
                        text = "• $paletteLabel • $dockLabel • $densityLabel",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = fontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = subtextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Tiles scaled gently by Display Density
                val tileHeight = (50f * densityMultiplier).dp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(tileHeight)
                            .clip(RoundedCornerShape(12.dp))
                            .background(tileSurface)
                            .border(1.dp, textColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(tileHeight)
                                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                                    .background(Brush.linearGradient(listOf(animatedPrimary, activeSecondary))),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (!activeArtworkUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = activeArtworkUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_logo),
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                            ) {
                                Text(
                                    text = activeTitle ?: "Now Playing",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = fontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = textColor,
                                )
                                Text(
                                    text = activeArtist ?: appFontChoice.localizedLabel(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = fontFamily,
                                        fontSize = 9.5.sp,
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = subtextColor,
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(tileHeight)
                            .clip(RoundedCornerShape(12.dp))
                            .background(tileSurface)
                            .border(1.dp, textColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(tileHeight)
                                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                                    .background(Brush.linearGradient(listOf(activeSecondary, animatedPrimary))),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                            ) {
                                Text(
                                    text = theme.localizedLabel(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = fontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = textColor,
                                )
                                Text(
                                    text = if (!isLight && amoledDarkMode) "AMOLED" else appFontChoice.localizedLabel(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = fontFamily,
                                        fontSize = 9.5.sp,
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = subtextColor,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Real Dock & MiniPlayer Reflection:
                // Normal shows floating pill over separate dock bar; Adaptive/Liquid shows integrated dock
                val isIntegratedDock = dockStyle != DockStyle.NORMAL
                val miniSurfaceAlpha = when {
                    reduceDynamicBlur -> 0.96f
                    dockStyle == DockStyle.LIQUID || liquidGlass -> 0.52f + 0.28f * effectiveGlassIntensity
                    dockStyle == DockStyle.ADAPTIVE -> 0.65f + 0.25f * effectiveGlassIntensity
                    else -> 0.90f
                }
                val miniBackground = if (isLight) Color.White.copy(alpha = miniSurfaceAlpha) else Color(0xFF1A1C23).copy(alpha = miniSurfaceAlpha)

                // MiniPlayer pill
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(miniBackground)
                        .border(
                            width = if (dockStyle == DockStyle.LIQUID || liquidGlass) 1.2.dp else 0.8.dp,
                            color = animatedPrimary.copy(alpha = 0.20f + 0.35f * effectiveGlassIntensity),
                            shape = RoundedCornerShape(percent = 50),
                        ),
                ) {
                    // Reflect real MiniPlayerTimelineStyle
                    val timelineBrush = when (miniPlayerTimelineStyle) {
                        MiniPlayerTimelineStyle.OG_CIRCLE -> Brush.horizontalGradient(listOf(animatedPrimary, animatedPrimary))
                        MiniPlayerTimelineStyle.AURORA_WAVE -> Brush.horizontalGradient(listOf(animatedPrimary, activeSecondary))
                        MiniPlayerTimelineStyle.AURORA_SILK -> Brush.horizontalGradient(listOf(activeSecondary, animatedPrimary, Color.White))
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth(0.45f)
                            .height(2.5.dp)
                            .background(timelineBrush),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Brush.linearGradient(listOf(animatedPrimary, activeSecondary))),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (!activeArtworkUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = activeArtworkUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.ic_logo),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp),
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = activeTitle ?: "Now Playing",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = fontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = textColor,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = animatedPrimary,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }

                Spacer(Modifier.height(if (isIntegratedDock) 5.dp else 8.dp))

                // Real Navigation Dock Bar tabs reflecting `AppSettings.navTabs` order & visibility
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(percent = 50))
                        .background(
                            if (isLight) {
                                Color.White.copy(alpha = if (reduceDynamicBlur) 0.95f else 0.72f)
                            } else {
                                Color(0xFF15171E).copy(alpha = if (reduceDynamicBlur) 0.95f else 0.75f)
                            },
                        )
                        .border(
                            0.8.dp,
                            if (isIntegratedDock) animatedPrimary.copy(alpha = 0.25f) else textColor.copy(alpha = 0.10f),
                            RoundedCornerShape(percent = 50),
                        )
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    visibleTabs.forEachIndexed { idx, page ->
                        val tabIcon = when (page) {
                            NavPage.HOME -> BitChordIcons.Home
                            NavPage.RADIO -> BitChordIcons.TabRadio
                            NavPage.LIBRARY -> BitChordIcons.TabLibrary
                            NavPage.SEARCH -> BitChordIcons.TabSearch
                        }
                        val tabLabel = stringResource(
                            when (page) {
                                NavPage.HOME -> R.string.home
                                NavPage.RADIO -> R.string.radio
                                NavPage.LIBRARY -> R.string.library
                                NavPage.SEARCH -> R.string.search
                            },
                        )
                        val isSelected = idx == 0
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (isSelected) animatedPrimary.copy(alpha = 0.16f) else Color.Transparent)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = tabIcon,
                                contentDescription = null,
                                tint = if (isSelected) animatedPrimary else subtextColor,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = tabLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = fontFamily,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                ),
                                color = if (isSelected) animatedPrimary else subtextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}
