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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.model.Song
import com.music.bitchord.playback.PlaybackPosition
import dev.chrisbanes.haze.HazeState

/**
 * The foot of the portrait player: the lyric strip, scrubber (WavePlay or
 * standard), transport (ChordPlay or standard), volume bar, and action row.
 */
@Composable
internal fun PortraitControlDeck(
    panels: PlayerPanelState,
    spotifyCanvasPresentation: Boolean,
    onSpotifyDeckHeight: (Dp) -> Unit,
    syncedLyricsEnabled: Boolean,
    hideLyricsPreview: Boolean = false,
    lyricsTranslation: LyricsTranslationUi,
    hasLyrics: Boolean,
    song: Song,
    lyricsPlayhead: LyricPlayhead,
    isPlaying: Boolean,
    position: PlaybackPosition,
    durationMs: Long,
    isLive: Boolean = song.isLive,
    isSeekable: Boolean = true,
    isAtLiveEdge: Boolean = true,
    liveOffsetMs: Long = 0L,
    onJumpToLive: () -> Unit = {},
    lyricsUnavailable: Boolean,
    lyricsLoadingText: String,
    pickingLyrics: Boolean,
    onShowLyricsProviders: () -> Unit,
    onOpenLyricsStyle: () -> Unit,
    isWavePlay: Boolean,
    synesthesiaPalette: MeshPalette,
    shown: () -> Float,
    versionSwitching: Boolean,
    mixing: Boolean,
    scrub: PlayerScrub,
    onScrub: (Float) -> Unit,
    onScrubFinished: () -> Unit,
    isLoading: Boolean,
    controlSpread: Dp,
    audioVersionSwitching: Boolean,
    controlsLocked: Boolean,
    hasPrevious: Boolean,
    pastRestartPoint: Boolean,
    hasNext: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    playerHaze: HazeState,
    hideVolumeBar: Boolean,
    volume: PlayerVolume,
    onVolumeChange: (Float) -> Unit,
    onVolumeChangeFinished: () -> Unit,
    playerActions: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.onSizeChanged { size ->
            if (spotifyCanvasPresentation) {
                onSpotifyDeckHeight(with(density) { size.height.toDp() })
            }
        },
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = PLAYER_MAX_WIDTH)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val showInlineLyricStrip = syncedLyricsEnabled && !hideLyricsPreview
            val lyricOffsetY = if (isWavePlay) 2.dp else 6.dp
            if (!panels.lyricsOpen && showInlineLyricStrip) {
                if (isLive) {
                    Text(
                        text = "\u00A0",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = lyricOffsetY)
                            .padding(vertical = 4.dp),
                    )
                } else {
                    CurrentLyricStrip(
                        lines = lyricsTranslation.displayedLyrics,
                        trackKey = song.videoId,
                        playhead = lyricsPlayhead,
                        isPlaying = isPlaying && position.advancing,
                        durationMs = durationMs,
                        lyricsUnavailable = lyricsUnavailable,
                        loadingText = lyricsLoadingText,
                        onClick = panels.openLyrics,
                    )
                }
            }
            if (!panels.lyricsOpen && !showInlineLyricStrip) {
                Text(
                    text = "\u00A0",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = lyricOffsetY)
                        .padding(vertical = 4.dp),
                )
            }
            if (isWavePlay && !panels.lyricsOpen) {
                Spacer(Modifier.height(2.dp))
            }
            if (panels.lyricsOpen) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = 6.dp)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LyricsStatusWithChange(
                        status = lyricsTranslation.status,
                        onStatusClick = onShowLyricsProviders,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                    )
                    if (!pickingLyrics && hasLyrics) {
                        GlassyLyricsActions(
                            showingRomanization = lyricsTranslation.showingRomanization,
                            romanizationState = lyricsTranslation.romanizationState,
                            onToggleRomanization = lyricsTranslation.toggleRomanization,
                            showingTranslation = lyricsTranslation.showingTranslation,
                            translationState = lyricsTranslation.translationState,
                            onToggleTranslation = lyricsTranslation.toggleTranslation,
                            onOpenStyle = onOpenLyricsStyle,
                            palette = synesthesiaPalette,
                            enabled = panels.lyricsControlsOpen,
                        )
                    }
                }
            }
            if (isLive) {
                LiveStationBar(
                    shown = shown,
                    durationMs = durationMs,
                    isPlaying = isPlaying && position.advancing,
                    isSeekable = isSeekable,
                    isAtLiveEdge = isAtLiveEdge,
                    liveOffsetMs = liveOffsetMs,
                    liveViewersText = song.liveViewersText,
                    onScrub = onScrub,
                    onScrubFinished = onScrubFinished,
                    onJumpToLive = onJumpToLive,
                    trackKey = song.videoId,
                    palette = synesthesiaPalette,
                )
            } else if (isWavePlay) {
                SynesthesiaWaveformScrubber(
                    shown = shown,
                    durationMs = durationMs,
                    isPlaying = isPlaying && position.advancing,
                    onScrub = onScrub,
                    onScrubFinished = onScrubFinished,
                    trackKey = song.videoId,
                    palette = synesthesiaPalette,
                )
            } else {
                val transitionWindow by PlayerSettings.smartTransitionWindow.collectAsStateWithLifecycle()
                PlayerScrubber(
                    shown = shown,
                    durationMs = durationMs,
                    loading = versionSwitching,
                    mixing = mixing && !scrub.scrubbing,
                    transitionWindow = transitionWindow
                        ?.takeIf { !scrub.scrubbing && it.end > it.start }
                        ?.let { it.start..it.end },
                    onScrub = onScrub,
                    onScrubFinished = onScrubFinished,
                ) {
                    PlaybackQualityLabel(
                        song = song,
                        isLoading = isLoading,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 8.dp),
                    )
                }
            }

            Spacer(Modifier.height(if (isWavePlay) (4.dp + controlSpread * 0.12f) else (8.dp + controlSpread / 2)))

            val previousEnabled = !controlsLocked && if (isLive) hasPrevious else (hasPrevious || pastRestartPoint)
            val deckScope = rememberCoroutineScope()
            val reduceAnimation by PlayerSettings.reduceAnimation.collectAsStateWithLifecycle()
            val deckCoupling = remember(deckScope) { DeckPhysicsCoupling(deckScope) { reduceAnimation } }
            CompositionLocalProvider(LocalDeckPhysicsCoupling provides if (isWavePlay) deckCoupling else null) {
                if (isWavePlay) {
                    ChordPlayTransportDeck(
                        isPlaying = isPlaying,
                        isLoading = isLoading || audioVersionSwitching,
                        previousEnabled = previousEnabled,
                        nextEnabled = !controlsLocked && hasNext,
                        onPrevious = onPrevious,
                        onPlayPause = onPlayPause,
                        onNext = onNext,
                        shuffleEnabled = shuffleEnabled,
                        repeatMode = repeatMode,
                        onToggleShuffle = onToggleShuffle,
                        onCycleRepeat = onCycleRepeat,
                        palette = synesthesiaPalette,
                        hazeState = playerHaze,
                        queueOpen = panels.queueOpen,
                        isLive = isLive,
                    )
                } else {
                    TransportRow(
                        isPlaying = isPlaying,
                        isLoading = isLoading || audioVersionSwitching,
                        previousEnabled = previousEnabled,
                        nextEnabled = !controlsLocked && hasNext,
                        onPrevious = onPrevious,
                        onPlayPause = onPlayPause,
                        onNext = onNext,
                    )
                }

                if (!isWavePlay) {
                    Spacer(Modifier.height(12.dp + controlSpread / 2))

                    if (!hideVolumeBar) {
                        VolumeRow(
                            value = { volume.level.value },
                            onValueChange = onVolumeChange,
                            onValueChangeFinished = onVolumeChangeFinished,
                        )
                    } else {
                        Spacer(Modifier.height(VOLUME_ROW_HEIGHT))
                    }

                    Spacer(Modifier.height(6.dp))
                } else {
                    Spacer(Modifier.height(10.dp + controlSpread * 0.25f))
                }

                playerActions()
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}
