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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.settings.SpeedDialLayoutMode
import com.music.bitchord.data.settings.SpeedDialSource
import com.music.bitchord.data.settings.SpeedDialTileStyle
import com.music.bitchord.sharedui.resources.Res
import com.music.bitchord.sharedui.resources.customize_your_space
import com.music.bitchord.sharedui.resources.speed_dial_artist_dial
import com.music.bitchord.sharedui.resources.speed_dial_chord_wave
import com.music.bitchord.sharedui.resources.speed_dial_layout
import com.music.bitchord.sharedui.resources.speed_dial_mixes_dial
import com.music.bitchord.sharedui.resources.speed_dial_picks
import com.music.bitchord.sharedui.resources.speed_dial_reset_hidden
import com.music.bitchord.sharedui.resources.speed_dial_show_names
import com.music.bitchord.sharedui.resources.speed_dial_show_pins
import com.music.bitchord.sharedui.resources.speed_dial_tile_border
import com.music.bitchord.sharedui.resources.your_space_drag_hint
import com.music.bitchord.ui.AppUi
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import com.music.bitchord.ui.player.PlayerPlatform
import dev.chrisbanes.haze.HazeState
import org.jetbrains.compose.resources.stringResource

/**
 * Frosted glass live customization sheet for ChordPlay's "Your Space" section.
 * Uses a light scrim so changes to grid size, tile style, labels, and dials are visible live
 * behind the sheet as they are toggled.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YourSpaceCustomizeSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    hazeState: HazeState? = null,
) {
    if (!visible) return

    val layoutMode by AppUi.host.speedDialLayoutMode.collectAsStateWithLifecycle()
    val source by AppUi.host.speedDialSource.collectAsStateWithLifecycle()
    val tileStyle by AppUi.host.speedDialTileStyle.collectAsStateWithLifecycle()
    val showLabels by AppUi.host.speedDialShowLabels.collectAsStateWithLifecycle()
    val showPinBadge by AppUi.host.speedDialShowPinBadge.collectAsStateWithLifecycle()
    val showMixes by AppUi.host.speedDialShowMixes.collectAsStateWithLifecycle()
    val showArtists by AppUi.host.speedDialShowArtists.collectAsStateWithLifecycle()
    val showChordWave by AppUi.host.speedDialShowChordWave.collectAsStateWithLifecycle()
    val hiddenIds by AppUi.host.hiddenSpeedDialIds.collectAsStateWithLifecycle()
    val spotifyConnected by AppUi.host.spotifyConnected.collectAsStateWithLifecycle()
    val reduceDynamicBlur by PlayerPlatform.host.settings.reduceDynamicBlur.collectAsStateWithLifecycle()

    val haptics = rememberHaptics()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val accent = MaterialTheme.colorScheme.primary

    val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val baseSurface = if (isDark) Color(0xFF16181F) else Color(0xFFF6F7FA)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = sheetShape,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.24f),
        dragHandle = null,
    ) {
        val frostModifier = if (hazeState != null && !reduceDynamicBlur) {
            Modifier
                .clip(sheetShape)
                .optimizedHazeEffect(
                    state = hazeState,
                    style = menuFrost(baseSurface, alpha = 0.78f),
                )
        } else {
            Modifier
                .clip(sheetShape)
                .background(baseSurface.copy(alpha = 0.96f))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(frostModifier)
                .border(1.dp, borderColor, sheetShape)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            // Drag pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)),
                )
            }

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.customize_your_space),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(Res.string.your_space_drag_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                modifier = Modifier.padding(vertical = 6.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Layout Selector
                val layoutOptions = SpeedDialLayoutMode.entries
                val layoutLabels = listOf("2×3", "3×3", "3×4", "List")
                SheetSegmentedSection(
                    title = stringResource(Res.string.speed_dial_layout),
                    labels = layoutLabels,
                    selectedIndex = layoutOptions.indexOf(layoutMode).coerceAtLeast(0),
                    accent = accent,
                    isDark = isDark,
                    onSelect = { idx ->
                        haptics.play(Haptic.Select)
                        AppUi.host.setSpeedDialLayoutMode(layoutOptions[idx])
                    },
                )

                // Picks Source Selector
                val sources = SpeedDialSource.choices(spotifyConnected)
                val activeSource = source.effective(spotifyConnected)
                SheetSegmentedSection(
                    title = stringResource(Res.string.speed_dial_picks),
                    labels = sources.map { it.label },
                    selectedIndex = sources.indexOf(activeSource).coerceAtLeast(0),
                    accent = accent,
                    isDark = isDark,
                    onSelect = { idx ->
                        haptics.play(Haptic.Select)
                        AppUi.host.setSpeedDialSource(sources[idx])
                    },
                )

                // Tile Style Selector (applicable in 3-column artwork grids)
                val tileStyles = SpeedDialTileStyle.entries
                SheetSegmentedSection(
                    title = stringResource(Res.string.speed_dial_tile_border),
                    labels = listOf("Minimal", "Bevel", "Glass"),
                    selectedIndex = tileStyles.indexOf(tileStyle).coerceAtLeast(0),
                    accent = accent,
                    isDark = isDark,
                    onSelect = { idx ->
                        haptics.play(Haptic.Select)
                        AppUi.host.setSpeedDialTileStyle(tileStyles[idx])
                    },
                )

                // Toggles
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(SmoothCornerShape(18.dp))
                        .background(
                            if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.04f),
                        )
                        .border(
                            0.75.dp,
                            if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                            SmoothCornerShape(18.dp),
                        ),
                ) {
                    SheetSwitchRow(
                        label = stringResource(Res.string.speed_dial_mixes_dial),
                        checked = showMixes,
                        accent = accent,
                        onCheckedChange = {
                            haptics.play(if (it) Haptic.ToggleOn else Haptic.ToggleOff)
                            AppUi.host.setSpeedDialShowMixes(it)
                        },
                    )
                    RowHairline(isDark)
                    SheetSwitchRow(
                        label = stringResource(Res.string.speed_dial_artist_dial),
                        checked = showArtists,
                        accent = accent,
                        onCheckedChange = {
                            haptics.play(if (it) Haptic.ToggleOn else Haptic.ToggleOff)
                            AppUi.host.setSpeedDialShowArtists(it)
                        },
                    )
                    RowHairline(isDark)
                    SheetSwitchRow(
                        label = stringResource(Res.string.speed_dial_chord_wave),
                        checked = showChordWave,
                        accent = accent,
                        onCheckedChange = {
                            haptics.play(if (it) Haptic.ToggleOn else Haptic.ToggleOff)
                            AppUi.host.setSpeedDialShowChordWave(it)
                        },
                    )
                    RowHairline(isDark)
                    SheetSwitchRow(
                        label = stringResource(Res.string.speed_dial_show_names),
                        checked = showLabels,
                        accent = accent,
                        onCheckedChange = {
                            haptics.play(if (it) Haptic.ToggleOn else Haptic.ToggleOff)
                            AppUi.host.setSpeedDialShowLabels(it)
                        },
                    )
                    RowHairline(isDark)
                    SheetSwitchRow(
                        label = stringResource(Res.string.speed_dial_show_pins),
                        checked = showPinBadge,
                        accent = accent,
                        onCheckedChange = {
                            haptics.play(if (it) Haptic.ToggleOn else Haptic.ToggleOff)
                            AppUi.host.setSpeedDialShowPinBadge(it)
                        },
                    )
                }

                AnimatedVisibility(visible = hiddenIds.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(SmoothCornerShape(16.dp))
                            .background(accent.copy(alpha = 0.12f))
                            .clickable {
                                haptics.play(Haptic.Select)
                                AppUi.host.resetHiddenSpeedDial()
                            }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${stringResource(Res.string.speed_dial_reset_hidden)} (${hiddenIds.size})",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = accent,
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun SheetSegmentedSection(
    title: String,
    labels: List<String>,
    selectedIndex: Int,
    accent: Color,
    isDark: Boolean,
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(SmoothCornerShape(14.dp))
                .background(if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.05f))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            labels.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val pillBg = if (selected) {
                    if (isDark) accent.copy(alpha = 0.26f) else Color.White
                } else {
                    Color.Transparent
                }
                val textColor = when {
                    selected && isDark -> Color.White
                    selected -> accent
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 42.dp)
                        .clip(SmoothCornerShape(11.dp))
                        .background(pillBg)
                        .then(
                            if (selected) {
                                Modifier.border(
                                    0.75.dp,
                                    accent.copy(alpha = if (isDark) 0.45f else 0.28f),
                                    SmoothCornerShape(11.dp),
                                )
                            } else {
                                Modifier
                            },
                        )
                        .clickable(role = Role.RadioButton) { onSelect(index) }
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        ),
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetSwitchRow(
    label: String,
    checked: Boolean,
    accent: Color,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .clickable(role = Role.Switch) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(end = 12.dp),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accent,
            ),
        )
    }
}

@Composable
private fun RowHairline(isDark: Boolean) {
    HorizontalDivider(
        thickness = 0.5.dp,
        color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.08f),
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}
