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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.MiniPlayerTimelineStyle
import com.music.bitchord.ui.components.ChordWaveAuroraPalette
import com.music.bitchord.ui.components.MiniPlayerNextGlyph
import com.music.bitchord.ui.components.MiniPlayerTimelinePaths
import com.music.bitchord.ui.components.drawMiniPlayerTimelineOverlay
import com.music.bitchord.ui.components.drawMiniPlayerTimelineUnderlay
import com.music.bitchord.ui.components.formatMiniScrubTime
import com.music.bitchord.ui.components.rememberMiniPlayerAuroraPhase
import com.music.bitchord.ui.screens.SegmentedControl
import com.music.bitchord.ui.screens.SettingsGroup
import com.music.bitchord.ui.screens.SettingsSearchEmpty
import com.music.bitchord.ui.screens.SettingsSubPageHeader
import com.music.bitchord.ui.screens.matchesSearch

private data class MiniPlayerPreviewPreset(
    val title: String,
    val artist: String,
    val primary: Color,
    val secondary: Color,
    val highlight: Color,
    val groundDark: Color,
    val groundLight: Color,
)

private val MiniPlayerPreviewPresets = listOf(
    MiniPlayerPreviewPreset(
        title = "Midnight Reverie",
        artist = "ChordPlay Radio",
        primary = Color(0xFF42D9F5),
        secondary = Color(0xFF7B61FF),
        highlight = Color(0xFF9DF0FF),
        groundDark = Color(0xFF101926),
        groundLight = Color(0xFFE7F3FA),
    ),
    MiniPlayerPreviewPreset(
        title = "Blood In Your Eyes",
        artist = "Velvet Echo",
        primary = Color(0xFFFF4D7D),
        secondary = Color(0xFF9C27B0),
        highlight = Color(0xFFFFA3BD),
        groundDark = Color(0xFF210E18),
        groundLight = Color(0xFFFCE9EF),
    ),
    MiniPlayerPreviewPreset(
        title = "Golden Horizon",
        artist = "Amber Tide",
        primary = Color(0xFFFFA73B),
        secondary = Color(0xFFE84A7E),
        highlight = Color(0xFFFFDF8E),
        groundDark = Color(0xFF1F160E),
        groundLight = Color(0xFFFCF2E4),
    ),
)

@Composable
internal fun MiniPlayerSection(
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    initialSearchQuery: String,
) {
    val style by AppSettings.miniPlayerTimelineStyle.collectAsStateWithLifecycle()
    val reducedMotion by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    var query by remember(initialSearchQuery) { mutableStateOf(initialSearchQuery) }

    val showTimeline = matchesSearch(
        query,
        stringResource(R.string.miniplayer_animation),
        stringResource(R.string.miniplayer_style_og_circle),
        stringResource(R.string.miniplayer_style_aurora_wave),
        stringResource(R.string.miniplayer_style_aurora_silk),
        stringResource(R.string.miniplayer_animation_subtitle),
        "miniplayer", "timeline", "progress", "circle", "wave", "silk", "aurora", "pill",
    )

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
    ) {
        SettingsSubPageHeader(
            title = stringResource(R.string.settings_miniplayer),
            onBack = onBack,
            searchQuery = query,
            onSearchQueryChange = { query = it },
        )
        if (!showTimeline) {
            SettingsSearchEmpty(query)
        } else {
            SettingsGroup(
                header = stringResource(R.string.miniplayer_animation),
                footer = stringResource(R.string.miniplayer_animation_subtitle),
            ) {
                MiniPlayerPreview(style = style, reducedMotion = reducedMotion)
                val styles = MiniPlayerTimelineStyle.entries
                SegmentedControl(
                    options = styles.map { option ->
                        stringResource(
                            when (option) {
                                MiniPlayerTimelineStyle.OG_CIRCLE -> R.string.miniplayer_style_og_circle
                                MiniPlayerTimelineStyle.AURORA_WAVE -> R.string.miniplayer_style_aurora_wave
                                MiniPlayerTimelineStyle.AURORA_SILK -> R.string.miniplayer_style_aurora_silk
                            },
                        )
                    },
                    selectedIndex = styles.indexOf(style).coerceAtLeast(0),
                    onSelect = { index -> AppSettings.setMiniPlayerTimelineStyle(styles[index]) },
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                )
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MiniPlayerPreview(
    style: MiniPlayerTimelineStyle,
    reducedMotion: Boolean,
) {
    var presetIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(true) }
    var progress by remember { mutableFloatStateOf(0.42f) }
    var isScrubbing by remember { mutableStateOf(false) }

    LaunchedEffect(isPlaying, isScrubbing, reducedMotion) {
        if (!isPlaying || isScrubbing || reducedMotion) return@LaunchedEffect
        var lastMs = withFrameMillis { it }
        while (true) {
            val now = withFrameMillis { it }
            val dt = ((now - lastMs) / 1000f).coerceIn(0f, 0.05f)
            lastMs = now
            var next = progress + dt * 0.055f
            if (next >= 1f) next = 0.04f
            progress = next
        }
    }

    val preset = MiniPlayerPreviewPresets[presetIndex % MiniPlayerPreviewPresets.size]
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val colorSpec = if (reducedMotion) snap<Color>() else tween(950, easing = FastOutSlowInEasing)

    val pPrimary by animateColorAsState(preset.primary, colorSpec, label = "mpPreviewPrimary")
    val pSecondary by animateColorAsState(preset.secondary, colorSpec, label = "mpPreviewSecondary")
    val pHighlight by animateColorAsState(preset.highlight, colorSpec, label = "mpPreviewHighlight")
    val pBaseDeep by animateColorAsState(
        if (isDark) preset.groundDark else preset.groundLight,
        colorSpec,
        label = "mpPreviewBaseDeep",
    )
    val aurora = AuroraPaletteForPreview(
        baseDeep = pBaseDeep,
        primary = pPrimary,
        secondary = pSecondary,
        highlight = pHighlight,
    )
    val auroraPhase = rememberMiniPlayerAuroraPhase(
        enabled = style != MiniPlayerTimelineStyle.OG_CIRCLE,
        isPlaying = isPlaying,
        isScrubbing = isScrubbing,
        reduceAnimation = reducedMotion,
        persistSessionClock = false,
    )
    val timelinePaths = remember { MiniPlayerTimelinePaths() }
    val previewShape = RoundedCornerShape(percent = 50)
    val pillBg = if (isDark) Color(0xFF1A1C24) else MaterialTheme.colorScheme.surfaceVariant
    val totalMs = 194_000L

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(previewShape)
                .background(pillBg.copy(alpha = 0.88f))
                .border(0.5.dp, Color.White.copy(alpha = if (isDark) 0.12f else 0.28f), previewShape)
                .drawWithContent {
                    drawMiniPlayerTimelineUnderlay(
                        style = style,
                        progress = progress,
                        scrubbing = isScrubbing,
                        phase = auroraPhase,
                        aurora = aurora,
                        isDark = isDark,
                        paths = timelinePaths,
                    )
                    drawContent()
                    drawMiniPlayerTimelineOverlay(
                        style = style,
                        progress = progress,
                        scrubbing = isScrubbing,
                        phase = auroraPhase,
                        aurora = aurora,
                        ogGlowColor = pPrimary,
                        isInline = false,
                        paths = timelinePaths,
                    )
                },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.linearGradient(listOf(pSecondary, pPrimary, pHighlight))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preset.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val subtitleText = if (isScrubbing) {
                        val ms = (progress * totalMs).toLong().coerceIn(0L, totalMs)
                        "${formatMiniScrubTime(ms)} / ${formatMiniScrubTime(totalMs)}"
                    } else {
                        preset.artist
                    }
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isScrubbing) pPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(
                    onClick = { isPlaying = !isPlaying },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = { presetIndex = (presetIndex + 1) % MiniPlayerPreviewPresets.size },
                    modifier = Modifier.size(36.dp),
                ) {
                    MiniPlayerNextGlyph(tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Slider(
            value = progress,
            onValueChange = {
                isScrubbing = true
                progress = it
            },
            onValueChangeFinished = { isScrubbing = false },
            colors = SliderDefaults.colors(
                thumbColor = pPrimary,
                activeTrackColor = pPrimary,
                inactiveTrackColor = pPrimary.copy(alpha = 0.20f),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
        )

        Spacer(Modifier.height(6.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MiniPlayerPreviewButton(
                icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                label = stringResource(if (isPlaying) R.string.pause else R.string.play),
                accent = pPrimary,
                onClick = { isPlaying = !isPlaying },
            )
            MiniPlayerPreviewButton(
                icon = Icons.Rounded.Palette,
                label = stringResource(R.string.cover_preview_next),
                accent = pPrimary,
                onClick = { presetIndex = (presetIndex + 1) % MiniPlayerPreviewPresets.size },
            )
        }
    }
}

private fun AuroraPaletteForPreview(
    baseDeep: Color,
    primary: Color,
    secondary: Color,
    highlight: Color,
): ChordWaveAuroraPalette = ChordWaveAuroraPalette(
    baseDeep = baseDeep,
    baseMid = lerp(baseDeep, secondary, 0.25f),
    primary = primary,
    secondary = secondary,
    highlight = highlight,
    filament = lerp(highlight, Color.White, 0.55f),
)

@Composable
private fun MiniPlayerPreviewButton(
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
