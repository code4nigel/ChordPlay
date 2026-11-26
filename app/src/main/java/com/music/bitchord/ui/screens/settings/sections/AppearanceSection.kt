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
package com.music.bitchord.ui.screens.settings.sections

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Brightness4
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.ViewWeek
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppFontChoice
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.ChordPlayPalette
import com.music.bitchord.data.settings.CustomPalette
import com.music.bitchord.data.settings.DockStyle
import com.music.bitchord.data.settings.MiniPlayerTimelineStyle
import com.music.bitchord.data.settings.NavPage
import com.music.bitchord.data.settings.PlayerWaveStyle
import com.music.bitchord.data.settings.ThemeMode
import com.music.bitchord.ui.components.IosSwitch
import com.music.bitchord.ui.components.isGlassSupported
import com.music.bitchord.ui.performance.displayDensityLabel
import com.music.bitchord.ui.screens.*
import com.music.bitchord.ui.screens.settings.localizedDetail
import com.music.bitchord.ui.screens.settings.localizedLabel
import com.music.bitchord.ui.screens.settings.previews.AppearanceLivePreview

/**
 * Appearance & Glass customization sub-page with a pinned live interactive preview card.
 */
@Composable
fun AppearanceSection(
    onBack: () -> Unit,
    onOpenCover: () -> Unit,
    onOpenMiniPlayer: () -> Unit,
    onOpenPlayerStyle: () -> Unit = {},
    contentPadding: PaddingValues,
    initialSearchQuery: String = "",
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current

    val theme by AppSettings.themeMode.collectAsStateWithLifecycle()
    val amoledDarkMode by AppSettings.amoledDarkMode.collectAsStateWithLifecycle()
    val appFontChoice by AppSettings.appFontChoice.collectAsStateWithLifecycle()
    val colorPalette by AppSettings.colorPalette.collectAsStateWithLifecycle()
    val customPalettes by AppSettings.customPalettes.collectAsStateWithLifecycle()
    val activeCustomPaletteId by AppSettings.activeCustomPaletteId.collectAsStateWithLifecycle()
    val displayDensityScale by AppSettings.displayDensityScale.collectAsStateWithLifecycle()

    var showRenameDialog by remember { mutableStateOf(false) }
    var paletteToRename by remember { mutableStateOf<CustomPalette?>(null) }
    var renameInput by remember { mutableStateOf("") }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var paletteToDelete by remember { mutableStateOf<CustomPalette?>(null) }
    val reduceAnimation by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    val miniPlayerTimelineStyle by AppSettings.miniPlayerTimelineStyle.collectAsStateWithLifecycle()
    val playerWaveStyle by AppSettings.playerWaveStyle.collectAsStateWithLifecycle()
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val dockStyle by AppSettings.dockStyle.collectAsStateWithLifecycle()
    val adaptiveDockBlurIntensity by AppSettings.adaptiveDockBlurIntensity.collectAsStateWithLifecycle()
    val liquidGlass by AppSettings.liquidGlass.collectAsStateWithLifecycle()
    val liquidGlassIntensity by AppSettings.liquidGlassIntensity.collectAsStateWithLifecycle()
    val liquidGlassSupported = isGlassSupported()
    val navTabs by AppSettings.navTabs.collectAsStateWithLifecycle()
    var showNavTabsDialog by remember { mutableStateOf(false) }

    var searchQuery by remember(initialSearchQuery) { mutableStateOf(initialSearchQuery) }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(contentPadding),
    ) {
        SettingsSubPageHeader(
            title = stringResource(R.string.appearance),
            onBack = onBack,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            scrollState = scrollState,
        )

        // Matching flags for filtering within Appearance settings
        val showColorPalette = matchesSearch(
            searchQuery,
            stringResource(R.string.color_palette),
            stringResource(R.string.color_palette_subtitle),
            stringResource(R.string.custom_saves),
            stringResource(R.string.saved_presets),
            "amber", "ruby", "amethyst", "jade", "sapphire", "emerald",
        )
        val showTheme = matchesSearch(
            searchQuery,
            stringResource(R.string.theme),
            stringResource(R.string.pure_black_amoled),
            stringResource(R.string.pure_black_amoled_desc),
            "dark", "light", "oled", "black", "system",
        )
        val showTypography = matchesSearch(
            searchQuery,
            stringResource(R.string.system_typography),
            appFontChoice.localizedLabel(),
            appFontChoice.localizedDetail(),
            "font", "slab", "modern", "default",
        )
        val showDisplayScale = matchesSearch(
            searchQuery,
            stringResource(R.string.display_density),
            stringResource(R.string.display_density_subtitle),
            "scale", "density", "compact", "comfortable",
        )
        val showDockStyle = matchesSearch(
            searchQuery,
            "dock", "dock style", "navigation", "adaptive",
            stringResource(R.string.dock_style),
            stringResource(R.string.dock_style_section),
            stringResource(R.string.adaptive_dock_blur),
            stringResource(R.string.adaptive_dock_blur_subtitle),
            stringResource(R.string.liquid_glass),
            stringResource(R.string.liquid_glass_subtitle),
            stringResource(R.string.glass_intensity),
            "liquid", "glass", "intensity", "blur", "refraction", "water",
        )
        val showNavTabs = matchesSearch(
            searchQuery,
            stringResource(R.string.dock_style_section),
            stringResource(R.string.nav_bar_tabs),
            stringResource(R.string.nav_bar_tabs_subtitle),
            "dock bar customisation", "dock bar customization", "navigation bar tabs",
            "navigation", "bar", "tabs", "bottom bar", "reorder", "hide",
        )
        val showDockSection = showDockStyle || showNavTabs

        val coverTitle = stringResource(R.string.settings_cover)
        val showCover = matchesSearch(searchQuery, coverTitle, "artwork", "animation", "canvas", "full screen")
        val miniPlayerTitle = stringResource(R.string.settings_miniplayer)
        val showMiniPlayer = matchesSearch(
            searchQuery,
            miniPlayerTitle,
            stringResource(R.string.miniplayer_animation),
            stringResource(R.string.miniplayer_style_og_circle),
            stringResource(R.string.miniplayer_style_aurora_wave),
            stringResource(R.string.miniplayer_style_aurora_silk),
            "miniplayer", "timeline", "circle", "wave", "silk", "aurora",
        )
        val playerStyleTitle = stringResource(R.string.settings_player_style)
        val showPlayerStyle = matchesSearch(
            searchQuery,
            playerStyleTitle,
            stringResource(R.string.player_wave_style),
            stringResource(R.string.player_wave_og),
            stringResource(R.string.player_wave_aurora_silk),
            stringResource(R.string.player_wave_audio_graph),
            stringResource(R.string.player_elastic_physics),
            stringResource(R.string.player_elastic_cross_title),
            "player style", "wave", "scrubber", "aurora", "silk", "audio", "graph",
            "elastic", "physics", "play", "quality pill", "shuffle", "repeat", "lyrics", "queue",
        )
        val anyBelowCover = showMiniPlayer || showPlayerStyle || showColorPalette ||
            showTheme || showTypography || showDisplayScale || showDockSection
        val anyBelowMiniPlayer = showPlayerStyle || showColorPalette ||
            showTheme || showTypography || showDisplayScale || showDockSection
        val anyBelowPlayerStyle = showColorPalette ||
            showTheme || showTypography || showDisplayScale || showDockSection
        val anyAppearanceMatch = showCover || anyBelowCover

        // Pinned Live Preview (hidden during active search)
        if (searchQuery.isBlank()) {
            AppearanceLivePreview()
        }

        if (searchQuery.isNotBlank() && !anyAppearanceMatch) {
            SettingsSearchEmpty(query = searchQuery)
        } else {
            // Single Unified Big Block for all Appearance Settings (duplicate small header removed)
            SettingsGroup(header = null, topSpacing = 14.dp) {
                if (showCover) {
                    SettingsRow(
                        icon = com.music.bitchord.ui.screens.settings.SettingsSection.COVER.icon,
                        title = coverTitle,
                        subtitle = stringResource(R.string.cover_animation),
                        onClick = onOpenCover,
                        trailing = { Chevron() },
                    )
                    if (anyBelowCover) {
                        RowDivider()
                    }
                }
                if (showMiniPlayer) {
                    SettingsRow(
                        icon = com.music.bitchord.ui.screens.settings.SettingsSection.MINIPLAYER.icon,
                        title = miniPlayerTitle,
                        subtitle = stringResource(
                            when (miniPlayerTimelineStyle) {
                                MiniPlayerTimelineStyle.OG_CIRCLE -> R.string.miniplayer_style_og_circle
                                MiniPlayerTimelineStyle.AURORA_WAVE -> R.string.miniplayer_style_aurora_wave
                                MiniPlayerTimelineStyle.AURORA_SILK -> R.string.miniplayer_style_aurora_silk
                            },
                        ),
                        onClick = onOpenMiniPlayer,
                        trailing = { Chevron() },
                    )
                    if (anyBelowMiniPlayer) {
                        RowDivider()
                    }
                }
                if (showPlayerStyle) {
                    SettingsRow(
                        icon = com.music.bitchord.ui.screens.settings.SettingsSection.PLAYER_STYLE.icon,
                        title = playerStyleTitle,
                        subtitle = stringResource(
                            when (playerWaveStyle) {
                                PlayerWaveStyle.OG_WAVE -> R.string.player_wave_og
                                PlayerWaveStyle.AURORA_SILK -> R.string.player_wave_aurora_silk
                                PlayerWaveStyle.AUDIO_GRAPH -> R.string.player_wave_audio_graph
                            },
                        ),
                        onClick = onOpenPlayerStyle,
                        trailing = { Chevron() },
                    )
                    if (anyBelowPlayerStyle) {
                        RowDivider()
                    }
                }
                // Curated Atmospheric Color Palettes
                if (showColorPalette) {

                SettingsRow(
                    icon = Icons.Rounded.Palette,
                    title = stringResource(R.string.color_palette),
                    subtitle = stringResource(R.string.color_palette_subtitle),
                )
                PaletteCarousel(
                    selectedPalette = if (activeCustomPaletteId != null) null else colorPalette,
                    onSelectPalette = { AppSettings.setColorPalette(it) },
                    reduceAnimation = reduceAnimation,
                    modifier = Modifier.padding(bottom = 14.dp),
                )

                // Custom Saves Subsection
                SettingsSectionDivider(title = stringResource(R.string.custom_saves))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ROW_INSET, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bookmarks,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(ICON_SIZE),
                        )
                        Spacer(Modifier.width(ICON_GAP))
                        Text(
                            text = stringResource(R.string.saved_presets),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        text = stringResource(R.string.saved_presets_count, customPalettes.size, AppSettings.MAX_CUSTOM_PALETTES),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (customPalettes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = ROW_INSET, end = ROW_INSET, bottom = 14.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(14.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.preserve_style_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        contentPadding = PaddingValues(horizontal = ROW_INSET),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(customPalettes, key = { it.id }) { palette ->
                            val isSelected = activeCustomPaletteId == palette.id
                            val primary = Color(palette.primaryHex)
                            val secondary = Color(palette.secondaryHex)
                            val bg = Color(palette.backgroundHex)
                            var showActionsMenu by remember { mutableStateOf(false) }

                            val orbScale by animateFloatAsState(
                                targetValue = if (isSelected) 1.05f else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium,
                                ),
                                label = "customPresetScale",
                            )

                            Box {
                                Column(
                                    modifier = Modifier
                                        .width(88.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .combinedClickable(
                                            onClick = {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                AppSettings.selectCustomPalette(palette.id)
                                            },
                                            onLongClick = {
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                showActionsMenu = true
                                            },
                                        )
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .graphicsLayer {
                                                scaleX = orbScale
                                                scaleY = orbScale
                                            }
                                            .then(
                                                if (isSelected) {
                                                    Modifier
                                                        .border(2.dp, primary, CircleShape)
                                                        .padding(3.dp)
                                                } else {
                                                    Modifier
                                                },
                                            )
                                            .clip(CircleShape)
                                            .background(
                                                Brush.sweepGradient(
                                                    listOf(primary, secondary, bg, primary),
                                                ),
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Black.copy(alpha = 0.60f)),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(15.dp),
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    Text(
                                        text = palette.name,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        ),
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                    )
                                }

                                DropdownMenu(
                                    expanded = showActionsMenu,
                                    onDismissRequest = { showActionsMenu = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.rename_preset)) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Rounded.Edit,
                                                contentDescription = null,
                                            )
                                        },
                                        onClick = {
                                            showActionsMenu = false
                                            paletteToRename = palette
                                            renameInput = palette.name
                                            showRenameDialog = true
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = stringResource(R.string.delete_preset),
                                                color = MaterialTheme.colorScheme.error,
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Rounded.Delete,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                            )
                                        },
                                        onClick = {
                                            showActionsMenu = false
                                            paletteToDelete = palette
                                            showDeleteDialog = true
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Theme Mode (Dark, Light, System)
            if (showTheme) {
                if (showColorPalette) SettingsSectionDivider(title = stringResource(R.string.theme))

                val themeTitle = stringResource(R.string.theme)
                SettingsRow(icon = Icons.Rounded.Brightness4, title = themeTitle)
                SegmentedControl(
                    options = ThemeMode.entries.map { it.localizedLabel() },
                    selectedIndex = ThemeMode.entries.indexOf(theme),
                    onSelect = { AppSettings.setThemeMode(ThemeMode.entries[it]) },
                    modifier = Modifier.padding(start = ROW_INSET, end = ROW_INSET, bottom = 14.dp),
                )
                SettingsRow(
                    icon = Icons.Rounded.Brightness4,
                    title = stringResource(R.string.pure_black_amoled),
                    subtitle = stringResource(R.string.pure_black_amoled_desc),
                    trailing = {
                        IosSwitch(
                            checked = amoledDarkMode,
                            onCheckedChange = AppSettings::setAmoledDarkMode,
                        )
                    },
                    onClick = { AppSettings.setAmoledDarkMode(!amoledDarkMode) },
                )
            }

            // System Typography
            if (showTypography) {
                if (showColorPalette || showTheme) SettingsSectionDivider(title = stringResource(R.string.system_typography))

                SettingsRow(
                    icon = Icons.Rounded.Style,
                    title = appFontChoice.localizedLabel(),
                    subtitle = appFontChoice.localizedDetail(),
                )
                SegmentedControl(
                    options = AppFontChoice.entries.map { it.localizedLabel() },
                    selectedIndex = AppFontChoice.entries.indexOf(appFontChoice),
                    onSelect = { AppSettings.setAppFontChoice(AppFontChoice.entries[it]) },
                    modifier = Modifier.padding(start = ROW_INSET, end = ROW_INSET, bottom = 14.dp),
                )
            }

            // Display Density
            if (showDisplayScale) {
                if (showColorPalette || showTheme || showTypography) SettingsSectionDivider(title = stringResource(R.string.display_density))

                val displayDensityTitle = stringResource(R.string.display_density)
                SliderRow(
                    icon = Icons.Rounded.GridView,
                    title = displayDensityTitle,
                    subtitle = stringResource(R.string.display_density_subtitle),
                    value = displayDensityLabel(displayDensityScale),
                    sliderValue = displayDensityScale,
                    onSliderValue = { AppSettings.setDisplayDensityScale(it) },
                    valueRange = AppSettings.MIN_DISPLAY_DENSITY_SCALE..AppSettings.MAX_DISPLAY_DENSITY_SCALE,
                    steps = 14,
                )
            }

            // Dock Style, Liquid Glass & Dock Bar Customisation
            if (showDockSection) {
                if (showColorPalette || showTheme || showTypography || showDisplayScale) {
                    SettingsSectionDivider(title = stringResource(R.string.dock_style_section))
                }

                if (showDockStyle) {
                    SettingsRow(
                        icon = Icons.Rounded.ViewAgenda,
                        title = stringResource(R.string.dock_style),
                        subtitle = when (dockStyle) {
                            DockStyle.NORMAL -> stringResource(R.string.dock_style_normal_desc)
                            DockStyle.ADAPTIVE -> stringResource(R.string.dock_style_adaptive_desc)
                            DockStyle.LIQUID -> stringResource(R.string.dock_style_liquid_desc)
                        },
                    )
                    SegmentedControl(
                        options = DockStyle.entries.map { it.localizedLabel() },
                        selectedIndex = DockStyle.entries.indexOf(dockStyle),
                        onSelect = { index ->
                            AppSettings.setDockStyle(DockStyle.entries[index])
                        },
                        modifier = Modifier.padding(start = ROW_INSET, end = ROW_INSET, bottom = 10.dp),
                    )

                    AnimatedVisibility(
                        visible = dockStyle == DockStyle.ADAPTIVE && !reduceDynamicBlur,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = TEXT_INSET, end = ROW_INSET, top = 2.dp, bottom = 12.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                    Text(
                                        text = stringResource(R.string.adaptive_dock_blur),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = stringResource(R.string.adaptive_dock_blur_subtitle),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    text = "${(adaptiveDockBlurIntensity * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Slider(
                                value = adaptiveDockBlurIntensity,
                                onValueChange = AppSettings::setAdaptiveDockBlurIntensity,
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                                ),
                            )
                        }
                    }

                    val liquidGlassTitle = stringResource(R.string.liquid_glass)
                    SettingsRow(
                        icon = Icons.Rounded.WaterDrop,
                        title = liquidGlassTitle,
                        subtitle = stringResource(
                            if (liquidGlassSupported) R.string.liquid_glass_subtitle
                            else R.string.liquid_glass_unavailable,
                        ),
                        enabled = liquidGlassSupported,
                        trailing = {
                            IosSwitch(
                                checked = liquidGlass,
                                onCheckedChange = AppSettings::setLiquidGlass,
                                enabled = liquidGlassSupported,
                            )
                        },
                        onClick = { AppSettings.setLiquidGlass(!liquidGlass) },
                    )

                    AnimatedVisibility(
                        visible = (liquidGlass || dockStyle == DockStyle.LIQUID) && liquidGlassSupported,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = TEXT_INSET, end = ROW_INSET, top = 4.dp, bottom = 12.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(R.string.glass_intensity),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "${(liquidGlassIntensity * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Slider(
                                value = liquidGlassIntensity,
                                onValueChange = AppSettings::setLiquidGlassIntensity,
                                valueRange = 0.1f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                                ),
                            )
                        }
                    }
                }

                if (showNavTabs) {
                    if (showDockStyle) RowDivider()
                    SettingsRow(
                        icon = Icons.Rounded.ViewWeek,
                        title = stringResource(R.string.nav_bar_tabs),
                        subtitle = stringResource(R.string.nav_bar_tabs_subtitle),
                        onClick = { showNavTabsDialog = true },
                    )
                }
            }
        }
    }
    }

    if (showNavTabsDialog) {
        ReorderDialog(
            title = stringResource(R.string.nav_bar_tabs),
            note = stringResource(R.string.nav_bar_tabs_note),
            items = navTabs,
            label = { it.label() },
            canDisable = { list, page ->
                page != NavPage.SEARCH && list.count { it.enabled } > 1
            },
            onChange = AppSettings::setNavTabs,
            onDismiss = { showNavTabsDialog = false },
        )
    }

    if (showRenameDialog && paletteToRename != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(stringResource(R.string.rename_preset_title)) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text(stringResource(R.string.preset_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newName = renameInput.trim()
                        if (newName.isNotBlank()) {
                            AppSettings.renameCustomPalette(paletteToRename!!.id, newName)
                        }
                        showRenameDialog = false
                    },
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showDeleteDialog && paletteToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_preset_title)) },
            text = {
                Text(stringResource(R.string.delete_preset_confirmation, paletteToDelete!!.name))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        AppSettings.deleteCustomPalette(paletteToDelete!!.id)
                        showDeleteDialog = false
                    },
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}


@Composable
private fun PaletteCarousel(
    selectedPalette: ChordPlayPalette?,
    onSelectPalette: (ChordPlayPalette) -> Unit,
    reduceAnimation: Boolean,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val dynamicGradientBrush = remember {
        Brush.sweepGradient(
            listOf(
                Color(0xFFFF4D6D),
                Color(0xFFFFA62B),
                Color(0xFF38BDF8),
                Color(0xFFA855F7),
                Color(0xFFFF4D6D),
            ),
        )
    }
    val dynamicRotation = if (!reduceAnimation) {
        val transition = rememberInfiniteTransition(label = "dynamicArtworkGradient")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "dynamicArtworkAngle",
        )
    } else {
        null
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = ROW_INSET),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(ChordPlayPalette.entries) { item ->
            val isSelected = item == selectedPalette
            val primaryColor = if (item == ChordPlayPalette.DYNAMIC_MONET && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MaterialTheme.colorScheme.primary
            } else {
                Color(item.primaryColorHex)
            }
            val secondaryColor = if (item == ChordPlayPalette.DYNAMIC_MONET && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MaterialTheme.colorScheme.secondary
            } else {
                Color(item.secondaryColorHex)
            }

            val isDynamicArt = item == ChordPlayPalette.DYNAMIC_ARTWORK
            val staticBrush = remember(primaryColor, secondaryColor) {
                Brush.sweepGradient(listOf(primaryColor, secondaryColor, primaryColor))
            }
            val orbScale by animateFloatAsState(
                targetValue = if (isSelected) 1.05f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
                label = "appearancePaletteScale",
            )

            Column(
                modifier = Modifier
                    .width(88.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectPalette(item)
                    }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Borderless swatch orb; Dynamic Song Art animates a flowing gradient
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .graphicsLayer {
                            scaleX = orbScale
                            scaleY = orbScale
                        }
                        .then(
                            if (isSelected) {
                                Modifier
                                    .border(
                                        width = 2.dp,
                                        color = if (isDynamicArt) MaterialTheme.colorScheme.primary else primaryColor,
                                        shape = CircleShape,
                                    )
                                    .padding(3.dp)
                            } else {
                                Modifier
                            },
                        )
                        .clip(CircleShape)
                        .drawBehind {
                            if (isDynamicArt) {
                                rotate(degrees = dynamicRotation?.value ?: 0f) {
                                    drawRect(brush = dynamicGradientBrush)
                                }
                            } else {
                                drawRect(brush = staticBrush)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.60f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    ),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
