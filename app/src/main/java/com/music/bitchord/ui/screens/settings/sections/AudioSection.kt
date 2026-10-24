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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.NerdStats
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.OutputPcmMode
import com.music.bitchord.data.sources.DeviceCodecs
import com.music.bitchord.playback.AudioOutputStatus
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
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val wifiQuality by AppSettings.audioQualityWifi.collectAsStateWithLifecycle()
    val cellularQuality by AppSettings.audioQualityCellular.collectAsStateWithLifecycle()
    val metered by AppSettings.meteredConnection.collectAsStateWithLifecycle()
    val crossfade by AppSettings.crossfadeSeconds.collectAsStateWithLifecycle()
    val smartFade by AppSettings.smartFadeEnabled.collectAsStateWithLifecycle()
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
    ) {
        SettingsSubPageHeader(
            title = stringResource(R.string.audio_quality),
            onBack = onBack,
        )

        // Streaming Quality & Addon Sources
        SettingsGroup(header = stringResource(R.string.audio_quality)) {
            val sourceTitle = stringResource(R.string.source)
            SettingsRow(
                icon = Icons.Rounded.Extension,
                title = sourceTitle,
                subtitle = stringResource(R.string.sources_subtitle),
                onClick = onSources,
            )

            val onWifiTitle = stringResource(R.string.on_wifi)
            SettingsRow(
                icon = Icons.Rounded.Wifi,
                title = onWifiTitle,
                badge = stringResource(R.string.in_use).takeIf { metered == false },
                value = wifiQuality.localizedQualityLabel(),
                onClick = { pickingQuality = QualityTarget.WIFI },
            )

            val onMobileDataTitle = stringResource(R.string.on_mobile_data)
            SettingsRow(
                icon = Icons.Rounded.SignalCellularAlt,
                title = onMobileDataTitle,
                badge = stringResource(R.string.in_use).takeIf { metered == true },
                value = cellularQuality.localizedQualityLabel(),
                onClick = { pickingQuality = QualityTarget.CELLULAR },
            )

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
                    Switch(
                        checked = dolbyAtmos && dolbyAtmosSupported,
                        onCheckedChange = AppSettings::setDolbyAtmos,
                        enabled = dolbyAtmosSupported,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onClick = { AppSettings.setDolbyAtmos(!dolbyAtmos) },
            )
        }

        // Playback Processing & Blending
        SettingsGroup(header = stringResource(R.string.playback)) {
            val preferMusicOnlyTitle = stringResource(R.string.prefer_music_only)
            SettingsRow(
                icon = Icons.Rounded.SmartDisplay,
                title = preferMusicOnlyTitle,
                subtitle = stringResource(R.string.prefer_music_only_subtitle),
                trailing = {
                    Switch(
                        checked = preferMusicOnly,
                        onCheckedChange = AppSettings::setPreferMusicOnly,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onClick = { AppSettings.setPreferMusicOnly(!preferMusicOnly) },
            )

            if (!smartFade) {
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

            val automixTitle = stringResource(R.string.automix)
            SettingsRow(
                icon = Icons.Rounded.AutoAwesome,
                title = automixTitle,
                subtitle = stringResource(
                    if (smartFade) R.string.automix_enabled_subtitle
                    else R.string.automix_disabled_subtitle,
                ),
                trailing = {
                    Switch(
                        checked = smartFade,
                        onCheckedChange = AppSettings::setSmartFadeEnabled,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onClick = { AppSettings.setSmartFadeEnabled(!smartFade) },
            )

            val automixPerformanceTitle = stringResource(R.string.automix_performance)
            SettingsRow(
                icon = Icons.Rounded.Tune,
                title = automixPerformanceTitle,
                subtitle = stringResource(R.string.automix_performance_subtitle),
                value = automixPerformance.localizedLabel(),
                onClick = { pickingAutomixPerformance = true },
            )

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
                    Switch(
                        checked = skipSilence,
                        onCheckedChange = AppSettings::setSkipSilence,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onClick = { AppSettings.setSkipSilence(!skipSilence) },
            )

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
                    Switch(
                        checked = spatialAudio,
                        onCheckedChange = AppSettings::setSpatialAudio,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onClick = { AppSettings.setSpatialAudio(!spatialAudio) },
            )

            val equalizerTitle = stringResource(R.string.equalizer)
            SettingsRow(
                icon = Icons.Rounded.Tune,
                title = equalizerTitle,
                subtitle = stringResource(R.string.equalizer_subtitle),
                onClick = onEqualizer,
            )
        }

        // Hardware Output & Normalization Group
        SettingsGroup(header = stringResource(R.string.output_precision)) {
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

            val loudnessTitle = stringResource(R.string.loudness_normalization)
            SettingsRow(
                icon = Icons.Rounded.VolumeUp,
                title = loudnessTitle,
                subtitle = stringResource(R.string.loudness_normalization_subtitle),
                trailing = {
                    Switch(
                        checked = loudnessNormalization,
                        onCheckedChange = AppSettings::setLoudnessNormalization,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
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
