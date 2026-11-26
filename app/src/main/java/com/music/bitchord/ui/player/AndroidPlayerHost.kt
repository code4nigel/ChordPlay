/*
 * Copyright (C) 2024-2026 Kushagra Singh and BitChord contributors <https://github.com/kushagrasinghx/BitChord>
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

import android.content.Context
import android.database.ContentObserver
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.canvas.CanvasArtwork
import com.music.bitchord.data.canvas.CanvasRepository
import com.music.bitchord.data.listentogether.ListenTogether
import com.music.bitchord.data.lyrics.LyricLine
import com.music.bitchord.data.lyrics.LyricsTranslation
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.LastPlayerScreen
import com.music.bitchord.playback.AndroidCoverPlayback
import com.music.bitchord.playback.AndroidAutomixStudio
import com.music.bitchord.playback.AudioOutputStatus
import com.music.bitchord.playback.AudioRouting
import com.music.bitchord.playback.LocalPlaybackVolume
import com.music.bitchord.playback.LocalVolumeTarget
import com.music.bitchord.playback.localVolumeTarget
import com.music.bitchord.playback.cast.CastController
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Locale

/**
 * The phone's answers to [PlayerHost]: AppSettings, the Media3 Canvas decoder,
 * AudioRouting and the music stream's volume, the party, and the lyric
 * translation endpoint — each exactly what the player read directly before it
 * moved into the shared module.
 */
class AndroidPlayerHost(context: Context) : PlayerHost {
    override val coverTrackChange get() = AndroidCoverPlayback.change
    override val automixStudio: AutomixStudio get() = AndroidAutomixStudio
    private val app = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val main = Handler(Looper.getMainLooper())

    override val settings: PlayerSettingsSource = object : PlayerSettingsSource {
        override val coverAnimation get() = AppSettings.coverAnimation
        override val animatedCanvas get() = AppSettings.animatedCanvas
        override val audioQualityCellular get() = AppSettings.audioQualityCellular
        override val audioQualityWifi get() = AppSettings.audioQualityWifi
        override val canvasOverCellular get() = AppSettings.canvasOverCellular
        override val fullBleedArtwork get() = AppSettings.fullBleedArtwork
        override val tapArtworkToToggleFullScreen get() = AppSettings.tapArtworkToToggleFullScreen
        override val hideSongStatus get() = AppSettings.hideSongStatus
        override val hideLyricsPreview get() = AppSettings.hideLyricsPreview
        override val hideVolumeBar get() = AppSettings.hideVolumeBar
        override val lastPlayerScreen get() = AppSettings.lastPlayerScreen
        override val legacyMeshGradient get() = AppSettings.legacyMeshGradient
        override val liveBlur get() = AppSettings.liveBlur
        override val lyricsBlur get() = AppSettings.lyricsBlur
        override val lyricsOffsetMs get() = AppSettings.lyricsOffsetMs
        override val lyricsSourceOrder get() = AppSettings.lyricsSourceOrder
        override val meteredConnection get() = AppSettings.meteredConnection
        override val preferUsbDac get() = AppSettings.preferUsbDac
        override val prioritizeSpotifyCanvas get() = AppSettings.prioritizeSpotifyCanvas
        override val canvasProviderOrder get() = AppSettings.canvasProviderOrder
        override val spotifySpdcToken get() = AppSettings.spotifySpdcToken
        override val reduceAnimation get() = AppSettings.reduceAnimation
        override val playerAnimationSpeed get() = AppSettings.playerAnimationSpeed
        override val reduceDynamicBlur get() = AppSettings.reduceDynamicBlur
        override val showNerdStats get() = AppSettings.showNerdStats
        override val smartAnalysis get() = AppSettings.smartAnalysis
        override val smartFadeEnabled get() = AppSettings.activeSmartFadeEnabled
        override val automixPerformance get() = AppSettings.automixPerformanceMode
        override val automixTempoMatching get() = AppSettings.activeAutomixTempoMatching
        override fun setAutomixPerformance(value: com.music.bitchord.data.settings.AutomixPerformanceMode) =
            AppSettings.setAutomixPerformanceMode(value)
        override fun setAutomixTempoMatching(value: Boolean) = AppSettings.setActiveAutomixTempoMatching(value)
        override val smartMixInProgress get() = AppSettings.smartMixInProgress
        override val smartMixBlend get() = AppSettings.smartMixBlend
        override val smartTransitionWindow get() = AppSettings.smartTransitionWindow
        override val spotifyCanvasAutoHide get() = AppSettings.spotifyCanvasAutoHide
        override val syncedLyrics get() = AppSettings.syncedLyrics
        override val translationLanguage get() = AppSettings.translationLanguage
        override val versionAlignmentInProgress get() = AppSettings.versionAlignmentInProgress
        override val appDesignStyle get() = AppSettings.appDesignStyle
        override val playerLayoutMode get() = AppSettings.playerLayoutMode
        override val playerInterfaceStyle get() = AppSettings.playerInterfaceStyle
        override fun setPlayerInterfaceStyle(value: com.music.bitchord.data.settings.PlayerInterfaceStyle) = AppSettings.setPlayerInterfaceStyle(value)
        override val playerWaveStyle get() = AppSettings.playerWaveStyle
        override fun setPlayerWaveStyle(value: com.music.bitchord.data.settings.PlayerWaveStyle) = AppSettings.setPlayerWaveStyle(value)
        override val playerElasticConfig get() = AppSettings.playerElasticConfig
        override fun setPlayerElasticConfig(value: com.music.bitchord.data.settings.PlayerElasticConfig) = AppSettings.setPlayerElasticConfig(value)
        override val activeTrackWaveform get() = AppSettings.activeTrackWaveform
        override val liveAudioLevel get() = AppSettings.liveAudioLevel
        override val fullscreenArtworkLyricsStyle get() = AppSettings.fullscreenArtworkLyricsStyle
        override val enableBreathingAura get() = AppSettings.enableBreathingAura
        override val landscapeFontScale get() = AppSettings.landscapeFontScale
        override fun setLandscapeFontScale(value: com.music.bitchord.data.settings.LandscapeFontScale) = AppSettings.setLandscapeFontScale(value)
        override val videoQuality get() = AppSettings.videoQuality
        override fun setVideoQuality(value: com.music.bitchord.data.settings.VideoQuality) = AppSettings.setVideoQuality(value)
        override val videoFullBleed get() = AppSettings.videoFullBleed
        override fun setVideoFullBleed(value: Boolean) = AppSettings.setVideoFullBleed(value)
        override val videoFill get() = AppSettings.videoFill
        override fun setVideoFill(value: Boolean) = AppSettings.setVideoFill(value)

        override fun setLastPlayerScreen(value: LastPlayerScreen) = AppSettings.setLastPlayerScreen(value)
        override fun setLyricsOffsetMs(value: Int) = AppSettings.setLyricsOffsetMs(value)
        override fun setFullscreenArtworkLyricsStyle(value: com.music.bitchord.data.settings.FullscreenArtworkLyricsStyle) =
            AppSettings.setFullscreenArtworkLyricsStyle(value)
        override fun requestTrackWaveform(trackId: String, durationMs: Long) {
            AppSettings.onTrackWaveformRequested?.invoke(trackId, durationMs)
        }
        override fun setAnimatedCanvas(value: Boolean) = AppSettings.setAnimatedCanvas(value)
        override fun setFullBleedArtwork(value: Boolean) = AppSettings.setFullBleedArtwork(value)
        override fun setTapArtworkToToggleFullScreen(value: Boolean) = AppSettings.setTapArtworkToToggleFullScreen(value)
        override fun setSmartFadeEnabled(value: Boolean) {
            AppSettings.setActiveSmartFadeEnabled(value)
            if (value) {
                com.music.bitchord.playback.smart.AutomixModelManager.ensureModelsDownloaded(app)
            }
        }
    }

    override fun cachedCanvas(song: Song): CanvasArtwork? = CanvasRepository.cached(song)

    override suspend fun canvasFor(song: Song): CanvasArtwork? = CanvasRepository.canvasFor(song)

    @Composable
    override fun CanvasVideo(spec: CanvasVideoSpec, modifier: Modifier) = AndroidCanvasVideo(spec, modifier)

    override val musicVideoSupported: Boolean get() = true

    override val pipSupported: Boolean
        get() = pipSupportedProvider?.invoke() ?: false

    override val inPictureInPicture: StateFlow<Boolean> get() = pipState.asStateFlow()

    override fun enterPip(): Boolean =
        onEnterPip?.invoke() ?: false

    companion object {
        var onEnterPip: (() -> Boolean)? = null
        var pipSupportedProvider: (() -> Boolean)? = null
        private val pipState = MutableStateFlow(false)

        fun setInPipMode(active: Boolean) {
            pipState.value = active
        }
    }

    @Composable
    override fun MusicVideo(
        videoId: String,
        fit: Boolean,
        onRenderedChanged: (Boolean) -> Unit,
        modifier: Modifier,
        bottomFade: Float,
    ) = AndroidMusicVideo(videoId, fit, onRenderedChanged, modifier, bottomFade = bottomFade)

    override fun showsMusicVideoPicture(videoId: String): Boolean =
        (!AppSettings.preferMusicOnly.value ||
            MusicVideoEngine.isExplicitVideoOverride(videoId) ||
            com.music.bitchord.data.innertube.StreamResolver.isKnownLive(videoId)) &&
            !MusicVideoEngine.isPictureless(videoId)

    override val musicVideoNoPictureIds: StateFlow<Set<String>>
        get() = MusicVideoEngine.noPictureIds

    override val musicVideoAspect: StateFlow<Float> get() = MusicVideoEngine.aspect

    override val musicVideoQuality: StateFlow<MusicVideoQualityUi> get() = MusicVideoEngine.quality

    override fun setMusicVideoQuality(height: Int?) = MusicVideoEngine.setQuality(height)

    @Composable
    override fun rememberRemoteArtworkUrl(song: Song?): String? =
        com.music.bitchord.ui.components.rememberRemoteArtworkUrl(song)

    // Lazy, all three: the player is not the first thing the process does, and
    // none of these objects should be woken for a launch that never opens it.
    override val volume: SystemVolume by lazy { CastAwareVolume(MusicStreamVolume(app, scope), scope) }

    @Composable
    override fun rememberAudioOutputs(): List<AudioOutputDevice> {
        val outputs = rememberAndroidAudioOutputs()
        val cast by CastController.state.collectAsStateWithLifecycle()
        // While a receiver is the speaker no output of this phone is the one
        // playing, so none of them is lit; the Cast row is.
        return if (cast.casting) outputs.map { it.copy(isActive = false) } else outputs
    }

    @Composable
    override fun rememberOutputPicker(onOpen: () -> Unit): () -> Unit = rememberAndroidOutputPicker(onOpen)

    override fun selectAudioOutput(id: Int?) {
        // Picking an output of this phone is picking this phone: the receiver
        // is let go of, and the music carries on here if it was playing.
        if (CastController.state.value.casting) CastController.disconnect(resumeHere = true)
        AudioRouting.select(id)
    }

    override val outputFormat: StateFlow<OutputFormatUi> by lazy {
        AudioOutputStatus.current
            .map { it.toOutputFormat() }
            .stateIn(scope, SharingStarted.Eagerly, AudioOutputStatus.current.value.toOutputFormat())
    }

    override val party: StateFlow<PartyUi> by lazy {
        ListenTogether.state
            .map { it.toPartyUi() }
            .stateIn(scope, SharingStarted.Eagerly, ListenTogether.state.value.toPartyUi())
    }

    override suspend fun translateLyrics(
        trackId: String,
        lines: List<LyricLine>,
        targetLanguageTag: String,
    ): LyricsTranslationResult =
        when (val result = LyricsTranslation.translate(trackId, lines, targetLanguageTag)) {
            is LyricsTranslation.Result.Translated -> LyricsTranslationResult.Translated(result.lines)
            is LyricsTranslation.Result.SameLanguage -> LyricsTranslationResult.SameLanguage(result.language)
            LyricsTranslation.Result.Unavailable -> LyricsTranslationResult.Unavailable
        }

    override suspend fun romanizeLyrics(
        trackId: String,
        lines: List<LyricLine>,
        targetLanguageTag: String,
    ): LyricsRomanizationResult =
        when (val result = LyricsTranslation.romanize(trackId, lines, targetLanguageTag)) {
            is LyricsTranslation.RomanizationResult.Romanized -> LyricsRomanizationResult.Romanized(result.lines)
            LyricsTranslation.RomanizationResult.AlreadyRomanized -> LyricsRomanizationResult.AlreadyRomanized
            LyricsTranslation.RomanizationResult.Unavailable -> LyricsRomanizationResult.Unavailable
        }

    override fun showMessage(message: String) {
        main.post { Toast.makeText(app, message, Toast.LENGTH_SHORT).show() }
    }

    override val castState: StateFlow<CastUi> by lazy {
        CastController.ensureStarted(app)
        CastController.state
            .map { CastUi(supported = it.supported, connectedName = it.connectedName, connecting = it.connecting) }
            .stateIn(scope, SharingStarted.Eagerly, CastUi())
    }

    @Composable
    override fun CastDialog(hazeState: HazeState, onDismiss: () -> Unit, accentColor: Color?) =
        com.music.bitchord.ui.components.CastDialog(
            hazeState = hazeState,
            onDismiss = onDismiss,
            accentColor = accentColor,
        )

    @Composable
    override fun AudioPipelineDialog(hazeState: HazeState, isPlaying: Boolean, onDismiss: () -> Unit, accentColor: Color?) =
        com.music.bitchord.ui.components.AudioPipelineDialog(
            hazeState = hazeState,
            onDismiss = onDismiss,
            isPlaying = isPlaying,
            accentColor = accentColor,
        )

    // The phone is the platform that can turn picked lyrics into a picture: a
    // bitmap, a canvas, the gallery and the chooser are all Android's here. The
    // desktop has no answer to it, so it never sees the pick mode at all.
    override val lyricsShareAvailable: Boolean get() = true

    @Composable
    override fun LyricsShareSheet(
        hazeState: HazeState,
        request: LyricsShareRequest,
        onDismiss: () -> Unit,
    ) = AndroidLyricsShareSheet(hazeState = hazeState, request = request, onDismiss = onDismiss)
}

private fun AudioOutputStatus.Snapshot.toOutputFormat(): OutputFormatUi {
    val encoding = AudioOutputStatus.encodingLabel(this)
    val rate = actualSampleRateHz?.takeIf { it > 0 }
    val summary = if (rate == null) {
        encoding
    } else {
        "$encoding · ${"%.1f".format(Locale.ROOT, rate / 1000f).removeSuffix(".0")} kHz"
    }
    // What the route is *capable* of, read off the negotiated AudioTrack.
    val carriesHiRes = when (actualEncoding) {
        AudioFormat.ENCODING_PCM_24BIT_PACKED,
        AudioFormat.ENCODING_PCM_32BIT,
        AudioFormat.ENCODING_PCM_FLOAT,
        -> true
        else -> (actualSampleRateHz ?: 0) > 48_000
    }
    return OutputFormatUi(summary = summary, carriesHiRes = carriesHiRes)
}

private fun ListenTogether.State.toPartyUi() = PartyUi(
    inParty = inParty,
    controlsLocked = controlsLocked,
    members = members,
    you = you,
    code = code,
)

/**
 * The volume bar's level: the receiver's while the music is on one, the music
 * stream's otherwise. The two are different numbers on different devices, and
 * the bar must never show one while moving the other.
 */
private class CastAwareVolume(
    private val system: SystemVolume,
    scope: CoroutineScope,
) : SystemVolume {
    override val level: StateFlow<Float> = combine(
        system.level,
        CastController.volume,
        CastController.state.map { it.casting },
    ) { phone, receiver, casting -> if (casting) receiver else phone }
        .stateIn(scope, SharingStarted.Eagerly, system.level.value)

    override fun set(level: Float) {
        if (CastController.state.value.casting) CastController.setVolume(level) else system.set(level)
    }

    override fun refresh() = system.refresh()
}

/**
 * The music stream's volume. Hardware keys and the system panel change it
 * behind the app's back, so Settings is watched and the level follows.
 */
private class MusicStreamVolume(context: Context, scope: CoroutineScope) : SystemVolume {
    private val manager = context.getSystemService(AudioManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private data class FineSetting(val target: LocalVolumeTarget, val routeId: Int?)
    private var fineSetting: FineSetting? = null
    private val _level = MutableStateFlow(readIndex().toFloat() / max())
    override val level: StateFlow<Float> = _level.asStateFlow()

    init {
        context.contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI,
            true,
            object : ContentObserver(main) {
                override fun onChange(selfChange: Boolean) = refresh()
            },
        )
        manager?.registerAudioDeviceCallback(object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>) = refresh()
            override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>) = refresh()
        }, main)
        scope.launch(Dispatchers.Main.immediate) {
            AudioRouting.selectedId.collect { refresh() }
        }
    }

    private fun max() = manager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.coerceAtLeast(1) ?: 15
    private fun readIndex() = manager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0

    private fun route() = manager?.let { audio ->
        AudioRouting.activeOf(audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS), AudioRouting.selectedId.value)
    }

    override fun set(level: Float) {
        val audio = manager ?: return
        if (audio.isVolumeFixed) return
        val route = route()
        val supportsFine = route?.let { AudioRouting.kindOf(it.type) } in setOf(
            AudioRouting.Kind.PHONE, AudioRouting.Kind.BLUETOOTH, AudioRouting.Kind.WIRED,
        )
        val target = localVolumeTarget(
            level, max(), supportsFine,
            min = if (Build.VERSION.SDK_INT >= 28) audio.getStreamMinVolume(AudioManager.STREAM_MUSIC) else 0,
        ) { index ->
            if (Build.VERSION.SDK_INT >= 28 && route != null) {
                runCatching { audio.getStreamVolumeDb(AudioManager.STREAM_MUSIC, index, route.type) }.getOrNull()
            } else null
        }
        val previousIndex = readIndex()
        if (supportsFine) {
            fineSetting = FineSetting(target, route?.id)
            LocalPlaybackVolume.gain = minOf(LocalPlaybackVolume.gain, target.gain)
        } else {
            clearFineSetting()
        }
        if (previousIndex != target.index) {
            runCatching { audio.setStreamVolume(AudioManager.STREAM_MUSIC, target.index, 0) }
        }
        refresh()
    }

    override fun refresh() {
        val index = readIndex()
        val fine = fineSetting?.takeIf { it.target.index == index && it.routeId == route()?.id }
        if (fine == null) clearFineSetting() else LocalPlaybackVolume.gain = fine.target.gain
        _level.value = fine?.target?.displayed ?: index.toFloat() / max()
    }

    private fun clearFineSetting() {
        fineSetting = null
        LocalPlaybackVolume.gain = 1f
    }
}
