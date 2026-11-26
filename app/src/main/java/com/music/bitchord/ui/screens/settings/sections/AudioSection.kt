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

import android.media.AudioFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.music.bitchord.ui.components.IosSwitch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.NerdStats
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.OutputPcmMode
import com.music.bitchord.data.sources.DeviceCodecs
import com.music.bitchord.data.taste.TasteCategory
import com.music.bitchord.data.taste.TasteProfileRepository
import com.music.bitchord.playback.AudioOutputStatus
import com.music.bitchord.playback.smart.AutomixModelManager
import com.music.bitchord.ui.screens.*
import kotlin.math.roundToInt

/**
 * Audio quality, playback DSP, and output settings sub-page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioSection(
    onSources: () -> Unit,
    onEqualizer: () -> Unit,
    onAudioChoices: () -> Unit = {},
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    initialSearchQuery: String = "",
    modifier: Modifier = Modifier,
) {
    val wifiQuality by AppSettings.audioQualityWifi.collectAsStateWithLifecycle()
    val cellularQuality by AppSettings.audioQualityCellular.collectAsStateWithLifecycle()
    val metered by AppSettings.meteredConnection.collectAsStateWithLifecycle()
    val crossfade by AppSettings.crossfadeSeconds.collectAsStateWithLifecycle()
    val smartFade by AppSettings.smartFadeEnabled.collectAsStateWithLifecycle()
    val automixModelState by AutomixModelManager.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(Unit) { AutomixModelManager.refreshState(context) }
    val automixPerformance by AppSettings.automixPerformanceMode.collectAsStateWithLifecycle()
    val skipSilence by AppSettings.skipSilence.collectAsStateWithLifecycle()
    val dolbyAtmos by AppSettings.dolbyAtmos.collectAsStateWithLifecycle()
    val dolbyAtmosSupported = DeviceCodecs.playsDolbyAtmos
    val spatialAudio by AppSettings.spatialAudio.collectAsStateWithLifecycle()
    val outputPcmMode by AppSettings.outputPcmMode.collectAsStateWithLifecycle()
    val preferUsbDac by AppSettings.preferUsbDac.collectAsStateWithLifecycle()
    val loudnessNormalization by AppSettings.loudnessNormalization.collectAsStateWithLifecycle()
    val loudnessOffOnSpeaker by AppSettings.loudnessOffOnSpeaker.collectAsStateWithLifecycle()
    val preferMusicOnly by AppSettings.preferMusicOnly.collectAsStateWithLifecycle()
    val outputStatus by AudioOutputStatus.current.collectAsStateWithLifecycle()
    val playingFormat by NerdStats.current.collectAsStateWithLifecycle()
    val playingDolbyAtmos = playingFormat?.isDolbyAtmos == true

    var pickingQuality by remember { mutableStateOf<QualityTarget?>(null) }
    var pickingAutomixPerformance by remember { mutableStateOf(false) }
    var searchQuery by remember(initialSearchQuery) { mutableStateOf(initialSearchQuery) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(contentPadding),
    ) {
        SettingsSubPageHeader(
            title = stringResource(R.string.audio_quality),
            onBack = onBack,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            scrollState = scrollState,
        )

        val showSources = matchesSearch(
            searchQuery,
            stringResource(R.string.source),
            stringResource(R.string.sources_subtitle),
            "source", "where audio comes from", "youtube", "jiosaavn", "local", "webdav", "smb",
        )
        val showWifi = matchesSearch(
            searchQuery,
            stringResource(R.string.on_wifi),
            stringResource(R.string.wifi),
            "wi-fi", "wifi", "quality", "streaming", "lossless", "high", "medium", "low",
        )
        val showCellular = matchesSearch(
            searchQuery,
            stringResource(R.string.on_mobile_data),
            stringResource(R.string.mobile_data),
            "cellular", "mobile data", "data", "quality", "streaming", "lossless", "high", "medium", "low",
        )
        val showDolby = matchesSearch(
            searchQuery,
            stringResource(R.string.dolby_atmos),
            stringResource(R.string.dolby_atmos_subtitle),
            "surround", "atmos", "spatial", "immersive",
        )
        val showPreferMusicOnly = matchesSearch(
            searchQuery,
            stringResource(R.string.prefer_music_only),
            stringResource(R.string.prefer_music_only_subtitle),
            "video", "music video", "catalogue",
        )
        val tasteSnapshot by TasteProfileRepository.snapshot.collectAsStateWithLifecycle()
        val showAudioChoices = matchesSearch(
            searchQuery,
            stringResource(R.string.audio_choices),
            stringResource(R.string.audio_choices_subtitle),
            "audio choices", "taste", "languages", "genres", "themes", "blocked", "personalize", "hide",
        )
        val showHideAiMusic = matchesSearch(
            searchQuery,
            stringResource(R.string.hide_ai_music),
            stringResource(R.string.hide_ai_music_subtitle),
            "ai", "ai music", "artificial", "slop", "filter",
        )
        val showCrossfade = (!smartFade) && matchesSearch(
            searchQuery,
            stringResource(R.string.crossfade),
            stringResource(R.string.crossfade_subtitle),
            "fade", "transition", "overlap",
        )
        val showAutomix = matchesSearch(
            searchQuery,
            stringResource(R.string.automix),
            stringResource(R.string.automix_enabled_subtitle),
            stringResource(R.string.automix_disabled_subtitle),
            "smart fade", "automix", "seamless",
        )
        val showAutomixPerf = matchesSearch(
            searchQuery,
            stringResource(R.string.automix_performance),
            stringResource(R.string.automix_performance_subtitle),
            automixPerformance.localizedLabel(),
            "automix performance", "mode", "efficient", "balanced",
        )
        val showSkipSilence = matchesSearch(
            searchQuery,
            stringResource(R.string.skip_silence),
            stringResource(R.string.skip_silence_subtitle),
            "silence", "gapless", "skip",
        )
        val showSpatialAudio = matchesSearch(
            searchQuery,
            stringResource(R.string.spatial_audio),
            stringResource(R.string.spatial_audio_subtitle),
            "spatial", "surround", "3d",
        )
        val showEqualizer = matchesSearch(
            searchQuery,
            stringResource(R.string.equalizer),
            stringResource(R.string.equalizer_subtitle),
            "eq", "bass", "treble", "sound", "system equalizer",
        )
        val showOutputPrecision = matchesSearch(
            searchQuery,
            stringResource(R.string.output_precision),
            stringResource(R.string.prefer_usb_dac),
            "output", "precision", "usb", "dac", "bit perfect", "pcm", "float",
        )
        val showLoudness = matchesSearch(
            searchQuery,
            stringResource(R.string.loudness_normalization),
            stringResource(R.string.loudness_normalization_subtitle),
            "loudness", "normalization", "volume",
        )

        val anyAudioMatch = showSources || showWifi || showCellular || showDolby ||
            showPreferMusicOnly || showAudioChoices || showHideAiMusic || showCrossfade || showAutomix || showAutomixPerf ||
            showSkipSilence || showSpatialAudio || showEqualizer || showOutputPrecision || showLoudness

        if (searchQuery.isNotBlank() && !anyAudioMatch) {
            SettingsSearchEmpty(query = searchQuery)
        } else {
            // Single Unified Audio Settings Block (duplicate small header removed)
            SettingsGroup(header = null, topSpacing = 14.dp) {
                if (showSources) {
                    val sourceTitle = stringResource(R.string.source)
                    SettingsRow(
                        icon = Icons.Rounded.Extension,
                        title = sourceTitle,
                        subtitle = stringResource(R.string.sources_subtitle),
                        onClick = onSources,
                    )
                }

                if (showWifi) {
                    if (showSources) RowDivider()

                    val onWifiTitle = stringResource(R.string.on_wifi)
                    SettingsRow(
                        icon = Icons.Rounded.Wifi,
                        title = onWifiTitle,
                        badge = stringResource(R.string.in_use).takeIf { metered == false },
                        value = wifiQuality.localizedQualityLabel(),
                        onClick = { pickingQuality = QualityTarget.WIFI },
                    )
                }

                if (showCellular) {
                    if (showSources || showWifi) RowDivider()

                    val onMobileDataTitle = stringResource(R.string.on_mobile_data)
                    SettingsRow(
                        icon = Icons.Rounded.SignalCellularAlt,
                        title = onMobileDataTitle,
                        badge = stringResource(R.string.in_use).takeIf { metered == true },
                        value = cellularQuality.localizedQualityLabel(),
                        onClick = { pickingQuality = QualityTarget.CELLULAR },
                    )
                }

                if (showDolby) {
                    if (showSources || showWifi || showCellular) RowDivider()

                    val dolbyAtmosTitle = stringResource(R.string.dolby_atmos)
                    SettingsRow(
                        iconPainter = painterResource(R.drawable.ic_dolby_atmos),
                        title = dolbyAtmosTitle,
                        subtitle = stringResource(
                            if (dolbyAtmosSupported) R.string.dolby_atmos_subtitle
                            else R.string.dolby_atmos_unavailable,
                        ),
                        enabled = dolbyAtmosSupported,
                        trailing = {
                            IosSwitch(
                                checked = dolbyAtmos && dolbyAtmosSupported,
                                onCheckedChange = AppSettings::setDolbyAtmos,
                                enabled = dolbyAtmosSupported,
                            )
                        },
                        onClick = { AppSettings.setDolbyAtmos(!dolbyAtmos) },
                    )
                }

                // Playback Processing & Blending (Seamlessly integrated in the same block)
                if (showPreferMusicOnly || showAudioChoices || showHideAiMusic || showCrossfade || showAutomix || showAutomixPerf || showSkipSilence || showSpatialAudio || showEqualizer) {
                    if (showSources || showWifi || showCellular || showDolby) {
                        SettingsSectionDivider(title = stringResource(R.string.playback))
                    }

                    if (showAudioChoices) {
                        val preferredLabels = (
                            tasteSnapshot.topPreferredOrWeighted(TasteCategory.LANGUAGE, limit = 2) +
                                tasteSnapshot.topPreferredOrWeighted(TasteCategory.GENRE, limit = 2)
                            ).map { it.displayName }.distinct().take(3)
                        val blockedCount = tasteSnapshot.blockedSongs.size +
                            tasteSnapshot.blockedArtistKeys.size +
                            tasteSnapshot.blockedNodes(TasteCategory.LANGUAGE).size +
                            tasteSnapshot.blockedNodes(TasteCategory.GENRE).size +
                            tasteSnapshot.blockedNodes(TasteCategory.THEME).size
                        val summary = buildString {
                            if (preferredLabels.isNotEmpty()) {
                                append(preferredLabels.joinToString(", "))
                            } else {
                                append(stringResource(R.string.audio_choices_subtitle))
                            }
                            if (blockedCount > 0) {
                                append(" · ")
                                append(stringResource(R.string.audio_choices_blocked_count, blockedCount))
                            }
                        }
                        SettingsRow(
                            icon = Icons.Rounded.Tune,
                            title = stringResource(R.string.audio_choices),
                            subtitle = summary,
                            onClick = onAudioChoices,
                        )
                        if (showPreferMusicOnly || showHideAiMusic) RowDivider()
                    }

                    if (showPreferMusicOnly) {
                        val preferMusicOnlyTitle = stringResource(R.string.prefer_music_only)
                        SettingsRow(
                            icon = Icons.Rounded.SmartDisplay,
                            title = preferMusicOnlyTitle,
                            subtitle = stringResource(R.string.prefer_music_only_subtitle),
                            trailing = {
                                IosSwitch(
                                    checked = preferMusicOnly,
                                    onCheckedChange = AppSettings::setPreferMusicOnly,
                                )
                            },
                            onClick = { AppSettings.setPreferMusicOnly(!preferMusicOnly) },
                        )
                    }

                    if (showHideAiMusic) {
                        if (showPreferMusicOnly) RowDivider()
                        HideAiMusicRow(title = stringResource(R.string.hide_ai_music))
                    }

                    if (showCrossfade) {
                        if (showAudioChoices || showPreferMusicOnly || showHideAiMusic) RowDivider()

                        val crossfadeTitle = stringResource(R.string.crossfade)
                        SliderRow(
                            icon = Icons.Rounded.Waves,
                            title = crossfadeTitle,
                            subtitle = stringResource(R.string.crossfade_subtitle),
                            value = if (crossfade == 0) stringResource(R.string.off) else "${crossfade}s",
                            sliderValue = crossfade.toFloat(),
                            onSliderValue = { AppSettings.setCrossfadeSeconds(it.roundToInt()) },
                            valueRange = 0f..12f,
                            steps = 11,
                        )
                    }

                    if (showAutomix) {
                        if (showPreferMusicOnly || showHideAiMusic || showCrossfade) RowDivider()

                        val automixTitle = stringResource(R.string.automix)
                        val automixSubtitle = when {
                            smartFade && automixModelState is AutomixModelManager.ModelState.Downloading -> {
                                val pct = ((automixModelState as AutomixModelManager.ModelState.Downloading).progress * 100).toInt()
                                "Downloading Automix models ($pct%)…"
                            }
                            smartFade && automixModelState is AutomixModelManager.ModelState.Error -> {
                                "Automix enabled (tap to retry model download)"
                            }
                            smartFade -> stringResource(R.string.automix_enabled_subtitle)
                            else -> stringResource(R.string.automix_disabled_subtitle)
                        }
                        val toggleAutomix: (Boolean) -> Unit = { enabled ->
                            AppSettings.setSmartFadeEnabled(enabled)
                            if (enabled) {
                                AutomixModelManager.ensureModelsDownloaded(context)
                            }
                        }
                        SettingsRow(
                            icon = Icons.Rounded.AutoAwesome,
                            title = automixTitle,
                            subtitle = automixSubtitle,
                            trailing = {
                                IosSwitch(
                                    checked = smartFade,
                                    onCheckedChange = toggleAutomix,
                                )
                            },
                            onClick = {
                                if (smartFade && automixModelState is AutomixModelManager.ModelState.Error) {
                                    AutomixModelManager.ensureModelsDownloaded(context)
                                } else {
                                    toggleAutomix(!smartFade)
                                }
                            },
                        )
                    }

                    if (showAutomixPerf) {
                        if (showPreferMusicOnly || showHideAiMusic || showCrossfade || showAutomix) RowDivider()

                        val automixPerformanceTitle = stringResource(R.string.automix_performance)
                        SettingsRow(
                            icon = Icons.Rounded.Tune,
                            title = automixPerformanceTitle,
                            subtitle = stringResource(R.string.automix_performance_subtitle),
                            value = automixPerformance.localizedLabel(),
                            onClick = { pickingAutomixPerformance = true },
                        )
                    }

                    if (showSkipSilence) {
                        if (showPreferMusicOnly || showHideAiMusic || showCrossfade || showAutomix || showAutomixPerf) RowDivider()

                        val skipSilenceTitle = stringResource(R.string.skip_silence)
                        SettingsRow(
                            icon = Icons.AutoMirrored.Rounded.VolumeOff,
                            title = skipSilenceTitle,
                            subtitle = if (outputStatus.actualEncoding == AudioFormat.ENCODING_PCM_FLOAT) {
                                stringResource(R.string.skip_silence_float_subtitle)
                            } else {
                                stringResource(R.string.skip_silence_subtitle)
                            },
                            trailing = {
                                IosSwitch(
                                    checked = skipSilence,
                                    onCheckedChange = AppSettings::setSkipSilence,
                                )
                            },
                            onClick = { AppSettings.setSkipSilence(!skipSilence) },
                        )
                    }

                    if (showSpatialAudio) {
                        if (showPreferMusicOnly || showHideAiMusic || showCrossfade || showAutomix || showAutomixPerf || showSkipSilence) RowDivider()

                        val spatialAudioTitle = stringResource(R.string.spatial_audio)
                        SettingsRow(
                            icon = Icons.Rounded.SurroundSound,
                            title = spatialAudioTitle,
                            subtitle = if (playingDolbyAtmos) {
                                stringResource(R.string.spatial_audio_atmos_subtitle)
                            } else {
                                stringResource(R.string.spatial_audio_subtitle)
                            },
                            trailing = {
                                IosSwitch(
                                    checked = spatialAudio,
                                    onCheckedChange = AppSettings::setSpatialAudio,
                                )
                            },
                            onClick = { AppSettings.setSpatialAudio(!spatialAudio) },
                        )
                    }

                    if (showEqualizer) {
                        if (showPreferMusicOnly || showHideAiMusic || showCrossfade || showAutomix || showAutomixPerf || showSkipSilence || showSpatialAudio) RowDivider()

                        val equalizerTitle = stringResource(R.string.equalizer)
                        SettingsRow(
                            icon = Icons.Rounded.Tune,
                            title = equalizerTitle,
                            subtitle = stringResource(R.string.equalizer_subtitle),
                            onClick = onEqualizer,
                        )
                    }
                }

                // Hardware Output & Normalization (Seamlessly integrated in the same block)
                if (showOutputPrecision || showLoudness) {
                    if (showSources || showWifi || showCellular || showDolby || showPreferMusicOnly || showHideAiMusic || showAudioChoices || showCrossfade || showAutomix || showAutomixPerf || showSkipSilence || showSpatialAudio || showEqualizer) {
                        SettingsSectionDivider(title = stringResource(R.string.output_precision))
                    }

                    if (showOutputPrecision) {
                        val outputPrecisionTitle = stringResource(R.string.output_precision)
                        SettingsRow(
                            icon = Icons.Rounded.GraphicEq,
                            title = outputPrecisionTitle,
                            subtitle = buildString {
                                append(outputStatus.sink)
                                append(" · ")
                                append(outputStatus.deviceName)
                                (outputStatus.actualSampleRateHz ?: outputStatus.sampleRatesHz.firstOrNull())
                                    ?.let { append(" · ${it / 1000.0} kHz") }
                                append(" · ")
                                append(AudioOutputStatus.encodingLabel(outputStatus))
                            },
                        )
                        SegmentedControl(
                            options = OutputPcmMode.entries.map(OutputPcmMode::label),
                            selectedIndex = OutputPcmMode.entries.indexOf(outputPcmMode),
                            onSelect = { AppSettings.setOutputPcmMode(OutputPcmMode.entries[it]) },
                            modifier = Modifier.padding(start = TEXT_INSET, end = ROW_INSET, bottom = 14.dp),
                        )

                        val preferUsbDacTitle = stringResource(R.string.prefer_usb_dac)
                        SettingsSubRow(
                            title = preferUsbDacTitle,
                            checked = preferUsbDac,
                            onCheckedChange = AppSettings::setPreferUsbDac,
                            badge = stringResource(R.string.connected).takeIf { outputStatus.isUsb },
                        )
                    }

                    if (showLoudness) {
                        if (showOutputPrecision) RowDivider()

                        val loudnessTitle = stringResource(R.string.loudness_normalization)
                        SettingsRow(
                            icon = Icons.Rounded.VolumeUp,
                            title = loudnessTitle,
                            subtitle = stringResource(R.string.loudness_normalization_subtitle),
                            trailing = {
                                IosSwitch(
                                    checked = loudnessNormalization,
                                    onCheckedChange = AppSettings::setLoudnessNormalization,
                                )
                            },
                            onClick = { AppSettings.setLoudnessNormalization(!loudnessNormalization) },
                        )

                        val loudnessSpeakerTitle = stringResource(R.string.loudness_off_on_speaker)
                        SettingsSubRow(
                            title = loudnessSpeakerTitle,
                            checked = loudnessOffOnSpeaker,
                            onCheckedChange = AppSettings::setLoudnessOffOnSpeaker,
                        )
                    }
                }
            }
        }
    }

    pickingQuality?.let { target ->
        ModalBottomSheet(
            onDismissRequest = { pickingQuality = null },
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            QualitySheet(
                target = target,
                selected = when (target) {
                    QualityTarget.WIFI -> wifiQuality
                    QualityTarget.CELLULAR -> cellularQuality
                },
                onSelect = { quality ->
                    when (target) {
                        QualityTarget.WIFI -> AppSettings.setAudioQualityWifi(quality)
                        QualityTarget.CELLULAR -> AppSettings.setAudioQualityCellular(quality)
                    }
                    pickingQuality = null
                },
            )
        }
    }

    if (pickingAutomixPerformance) {
        ModalBottomSheet(
            onDismissRequest = { pickingAutomixPerformance = false },
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            AutomixPerformanceSheet(
                selected = automixPerformance,
                onSelect = { mode ->
                    AppSettings.setAutomixPerformanceMode(mode)
                    pickingAutomixPerformance = false
                },
            )
        }
    }
}
