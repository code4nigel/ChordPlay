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

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.PlayerElasticConfig
import com.music.bitchord.data.settings.PlayerWaveStyle
import com.music.bitchord.data.settings.TrackAudioWaveform
import com.music.bitchord.ui.player.ChordPlayInteractiveDeckPreview
import com.music.bitchord.ui.player.MeshPalette
import com.music.bitchord.ui.player.RepeatModes
import com.music.bitchord.ui.player.SynesthesiaWaveformScrubber
import com.music.bitchord.ui.screens.RowDivider
import com.music.bitchord.ui.screens.SegmentedControl
import com.music.bitchord.ui.screens.SettingsGroup
import com.music.bitchord.ui.screens.SettingsRow
import com.music.bitchord.ui.screens.SettingsSearchEmpty
import com.music.bitchord.ui.screens.SettingsSubPageHeader
import com.music.bitchord.ui.screens.matchesSearch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

private data class PlayerWavePreviewPreset(
    val title: String,
    val artist: String,
    val primary: Color,
    val secondary: Color,
    val highlight: Color,
)

private val PlayerWavePreviewPresets = listOf(
    PlayerWavePreviewPreset(
        title = "Bairiya Re",
        artist = "Gursheet Khanuja, Debabrata Gogoi",
        primary = Color(0xFFD94025),
        secondary = Color(0xFFF37D4C),
        highlight = Color(0xFFFFB28B),
    ),
    PlayerWavePreviewPreset(
        title = "Midnight Reverie",
        artist = "ChordPlay Radio",
        primary = Color(0xFF42D9F5),
        secondary = Color(0xFF7B61FF),
        highlight = Color(0xFF9DF0FF),
    ),
    PlayerWavePreviewPreset(
        title = "Velvet Echo",
        artist = "Amber Tide",
        primary = Color(0xFFFF4D7D),
        secondary = Color(0xFF9C27B0),
        highlight = Color(0xFFFFA3BD),
    ),
)

private data class ElasticButtonToggleItem(
    val icon: ImageVector,
    val titleRes: Int,
    val checked: (PlayerElasticConfig) -> Boolean,
    val onToggle: (PlayerElasticConfig, Boolean) -> PlayerElasticConfig,
)

private val ELASTIC_BUTTON_TOGGLES = listOf(
    ElasticButtonToggleItem(
        icon = Icons.Rounded.PlayArrow,
        titleRes = R.string.player_elastic_btn_play_pause,
        checked = { it.playPause },
        onToggle = { c, v -> c.copy(playPause = v) },
    ),
    ElasticButtonToggleItem(
        icon = Icons.Rounded.SkipPrevious,
        titleRes = R.string.player_elastic_btn_previous,
        checked = { it.previous },
        onToggle = { c, v -> c.copy(previous = v) },
    ),
    ElasticButtonToggleItem(
        icon = Icons.Rounded.SkipNext,
        titleRes = R.string.player_elastic_btn_next,
        checked = { it.next },
        onToggle = { c, v -> c.copy(next = v) },
    ),
    ElasticButtonToggleItem(
        icon = Icons.Rounded.Shuffle,
        titleRes = R.string.player_elastic_btn_shuffle,
        checked = { it.shuffle },
        onToggle = { c, v -> c.copy(shuffle = v) },
    ),
    ElasticButtonToggleItem(
        icon = Icons.Rounded.Repeat,
        titleRes = R.string.player_elastic_btn_repeat,
        checked = { it.repeat },
        onToggle = { c, v -> c.copy(repeat = v) },
    ),
    ElasticButtonToggleItem(
        icon = Icons.Rounded.FormatQuote,
        titleRes = R.string.player_elastic_btn_lyrics,
        checked = { it.lyrics },
        onToggle = { c, v -> c.copy(lyrics = v) },
    ),
    ElasticButtonToggleItem(
        icon = Icons.Rounded.GraphicEq,
        titleRes = R.string.player_elastic_btn_quality_pill,
        checked = { it.qualityPill },
        onToggle = { c, v -> c.copy(qualityPill = v) },
    ),
    ElasticButtonToggleItem(
        icon = Icons.AutoMirrored.Rounded.QueueMusic,
        titleRes = R.string.player_elastic_btn_queue,
        checked = { it.queue },
        onToggle = { c, v -> c.copy(queue = v) },
    ),
)

@Composable
internal fun PlayerWaveStyleSection(
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    initialSearchQuery: String,
) {
    val style by AppSettings.playerWaveStyle.collectAsStateWithLifecycle()
    val elastic by AppSettings.playerElasticConfig.collectAsStateWithLifecycle()
    val reducedMotion by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    var query by remember(initialSearchQuery) { mutableStateOf(initialSearchQuery) }

    val showWaveStyle = matchesSearch(
        query,
        stringResource(R.string.settings_player_style),
        stringResource(R.string.player_wave_style),
        stringResource(R.string.player_wave_og),
        stringResource(R.string.player_wave_aurora_silk),
        stringResource(R.string.player_wave_audio_graph),
        stringResource(R.string.player_wave_subtitle),
        "player", "wave", "scrubber", "progress", "aurora", "silk", "audio", "graph", "og",
    )
    val showElasticPhysics = matchesSearch(
        query,
        stringResource(R.string.player_elastic_physics),
        stringResource(R.string.player_elastic_master_title),
        stringResource(R.string.player_elastic_master_subtitle),
        stringResource(R.string.player_elastic_cross_title),
        stringResource(R.string.player_elastic_cross_subtitle),
        "elastic", "physics", "play", "quality pill", "pill", "interaction", "animation",
    )
    val visibleButtonToggles = ELASTIC_BUTTON_TOGGLES.filter { item ->
        matchesSearch(
            query,
            stringResource(R.string.player_elastic_buttons_header),
            stringResource(R.string.player_elastic_buttons_footer),
            stringResource(item.titleRes),
            "elastic", "button", "animation",
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
    ) {
        SettingsSubPageHeader(
            title = stringResource(R.string.settings_player_style),
            onBack = onBack,
            searchQuery = query,
            onSearchQueryChange = { query = it },
        )
        if (!showWaveStyle && !showElasticPhysics && visibleButtonToggles.isEmpty()) {
            SettingsSearchEmpty(query)
        } else {
            if (showWaveStyle) {
                SettingsGroup(
                    header = stringResource(R.string.player_wave_style),
                    footer = stringResource(R.string.player_wave_subtitle),
                ) {
                    PlayerWavePreviewCard(
                        style = style,
                        elasticConfig = elastic,
                        reducedMotion = reducedMotion,
                    )
                    val styles = PlayerWaveStyle.entries
                    SegmentedControl(
                        options = styles.map { option ->
                            stringResource(
                                when (option) {
                                    PlayerWaveStyle.OG_WAVE -> R.string.player_wave_og
                                    PlayerWaveStyle.AURORA_SILK -> R.string.player_wave_aurora_silk
                                    PlayerWaveStyle.AUDIO_GRAPH -> R.string.player_wave_audio_graph
                                },
                            )
                        },
                        selectedIndex = styles.indexOf(style).coerceAtLeast(0),
                        onSelect = { index -> AppSettings.setPlayerWaveStyle(styles[index]) },
                        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    )
                }
            }
            if (showElasticPhysics) {
                SettingsGroup(header = stringResource(R.string.player_elastic_physics)) {
                    SettingsRow(
                        icon = Icons.Rounded.AutoAwesome,
                        title = stringResource(R.string.player_elastic_master_title),
                        subtitle = stringResource(R.string.player_elastic_master_subtitle),
                        onClick = { AppSettings.updatePlayerElasticConfig { it.copy(enabled = !it.enabled) } },
                        trailing = {
                            Switch(
                                checked = elastic.enabled,
                                onCheckedChange = { checked ->
                                    AppSettings.updatePlayerElasticConfig { it.copy(enabled = checked) }
                                },
                            )
                        },
                    )
                    AnimatedVisibility(visible = elastic.enabled) {
                        Column {
                            RowDivider()
                            SettingsRow(
                                icon = Icons.Rounded.SwapVert,
                                title = stringResource(R.string.player_elastic_cross_title),
                                subtitle = stringResource(R.string.player_elastic_cross_subtitle),
                                onClick = {
                                    AppSettings.updatePlayerElasticConfig {
                                        it.copy(playPillInteraction = !it.playPillInteraction)
                                    }
                                },
                                trailing = {
                                    Switch(
                                        checked = elastic.playPillInteraction,
                                        onCheckedChange = { checked ->
                                            AppSettings.updatePlayerElasticConfig {
                                                it.copy(playPillInteraction = checked)
                                            }
                                        },
                                    )
                                },
                            )
                        }
                    }
                }
            }
            AnimatedVisibility(visible = elastic.enabled && visibleButtonToggles.isNotEmpty()) {
                SettingsGroup(
                    header = stringResource(R.string.player_elastic_buttons_header),
                    footer = stringResource(R.string.player_elastic_buttons_footer),
                ) {
                    visibleButtonToggles.forEachIndexed { idx, item ->
                        if (idx > 0) RowDivider()
                        val checked = item.checked(elastic)
                        SettingsRow(
                            icon = item.icon,
                            title = stringResource(item.titleRes),
                            onClick = {
                                AppSettings.updatePlayerElasticConfig { current ->
                                    item.onToggle(current, !item.checked(current))
                                }
                            },
                            trailing = {
                                Switch(
                                    checked = checked,
                                    onCheckedChange = { value ->
                                        AppSettings.updatePlayerElasticConfig { current ->
                                            item.onToggle(current, value)
                                        }
                                    },
                                )
                            },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlayerWavePreviewCard(
    style: PlayerWaveStyle,
    elasticConfig: PlayerElasticConfig,
    reducedMotion: Boolean,
) {
    var presetIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(true) }
    var progress by remember { mutableFloatStateOf(0.38f) }
    var isScrubbing by remember { mutableStateOf(false) }
    var shuffleEnabled by remember { mutableStateOf(true) }
    var repeatMode by remember { mutableIntStateOf(RepeatModes.ONE) }

    val preset = PlayerWavePreviewPresets[presetIndex % PlayerWavePreviewPresets.size]
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val colorSpec = if (reducedMotion) snap<Color>() else tween(900, easing = FastOutSlowInEasing)

    val pPrimary by animateColorAsState(preset.primary, colorSpec, label = "pwPreviewPrimary")
    val pSecondary by animateColorAsState(preset.secondary, colorSpec, label = "pwPreviewSecondary")
    val pHighlight by animateColorAsState(preset.highlight, colorSpec, label = "pwPreviewHighlight")

    var simulatedLevel by remember { mutableFloatStateOf(0.45f) }
    LaunchedEffect(isPlaying, isScrubbing, reducedMotion) {
        if (!isPlaying || isScrubbing || reducedMotion) return@LaunchedEffect
        var lastMs = withFrameMillis { it }
        var phase = 0f
        while (true) {
            val now = withFrameMillis { it }
            val dt = ((now - lastMs) / 1000f).coerceIn(0f, 0.05f)
            lastMs = now
            var next = progress + dt * 0.045f
            if (next >= 1f) next = 0.05f
            progress = next
            phase += dt * 4.8f
            simulatedLevel = (0.35f + 0.55f * abs(sin(phase))).coerceIn(0f, 1f)
        }
    }

    // Representative sample audio envelope for the preview card
    val sampleWave = remember(presetIndex) {
        val bars = FloatArray(96) { i ->
            val u = i / 95f
            val verseDrop = 0.34f + 0.42f * exp(-((u - 0.38f) / 0.14f) * ((u - 0.38f) / 0.14f))
            val bridgeDrop = 0.36f * exp(-((u - 0.72f) / 0.11f) * ((u - 0.72f) / 0.11f))
            val beats = 0.22f * abs(sin(u * PI * 14.0 + presetIndex * 1.1)).toFloat()
            (verseDrop + bridgeDrop + beats).coerceIn(0.14f, 1f)
        }
        TrackAudioWaveform(trackId = "preview_$presetIndex", bars = bars, measuredFraction = 1f)
    }

    val stagePalette = remember(pPrimary, pSecondary, pHighlight) {
        MeshPalette(listOf(pPrimary, pSecondary, pHighlight))
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            pPrimary.copy(alpha = if (isDark) 0.28f else 0.22f),
                            Color(0xFF0E0F15),
                        ),
                    ),
                )
                .border(0.8.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = preset.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = preset.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                SynesthesiaWaveformScrubber(
                    shown = { progress },
                    durationMs = 207_000L,
                    isPlaying = isPlaying,
                    onScrub = {
                        isScrubbing = true
                        progress = it
                    },
                    onScrubFinished = { isScrubbing = false },
                    trackKey = "preview_$presetIndex",
                    palette = stagePalette,
                    styleOverride = style,
                    audioWaveformOverride = sampleWave,
                    liveAudioLevelOverride = if (isPlaying) simulatedLevel else 0f,
                )

                Spacer(Modifier.height(6.dp))

                ChordPlayInteractiveDeckPreview(
                    isPlaying = isPlaying,
                    onPrevious = {
                        presetIndex = (presetIndex - 1 + PlayerWavePreviewPresets.size) % PlayerWavePreviewPresets.size
                    },
                    onPlayPause = { isPlaying = !isPlaying },
                    onNext = {
                        presetIndex = (presetIndex + 1) % PlayerWavePreviewPresets.size
                    },
                    shuffleEnabled = shuffleEnabled,
                    repeatMode = repeatMode,
                    onToggleShuffle = { shuffleEnabled = !shuffleEnabled },
                    onCycleRepeat = {
                        repeatMode = when (repeatMode) {
                            RepeatModes.OFF -> RepeatModes.ALL
                            RepeatModes.ALL -> RepeatModes.ONE
                            else -> RepeatModes.OFF
                        }
                    },
                    palette = stagePalette,
                    elasticConfig = elasticConfig,
                )
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
                inactiveTrackColor = pPrimary.copy(alpha = 0.22f),
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
            PlayerWavePreviewButton(
                icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                label = stringResource(if (isPlaying) R.string.pause else R.string.play),
                accent = pPrimary,
                onClick = { isPlaying = !isPlaying },
            )
            PlayerWavePreviewButton(
                icon = Icons.Rounded.Palette,
                label = stringResource(R.string.cover_preview_next),
                accent = pPrimary,
                onClick = { presetIndex = (presetIndex + 1) % PlayerWavePreviewPresets.size },
            )
        }
    }
}

@Composable
private fun PlayerWavePreviewButton(
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
