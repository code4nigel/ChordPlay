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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.music.bitchord.data.lyrics.LyricsSource
import com.music.bitchord.ui.LyricsProviderState
import dev.chrisbanes.haze.HazeState

/**
 * Which of the drawers and dialogs the player raises over itself are up.
 */
@Stable
internal class PlayerOverlayState {
    var audioOutput by mutableStateOf(false)
    var audioPipeline by mutableStateOf(false)
    var cast by mutableStateOf(false)
    var lyricsProviders by mutableStateOf(false)
    var listenTogetherMembers by mutableStateOf(false)
    var lyricsStyle by mutableStateOf(false)
    var videoQuality by mutableStateOf(false)
    var lyricsShare by mutableStateOf<LyricsShareRequest?>(null)

    val openListenTogetherMembers: () -> Unit = { listenTogetherMembers = true }
}

/**
 * The drawers and dialogs the player raises over itself. Mounted by whichever
 * layout is on screen — they are overlays over the player, not part of either
 * shape of it.
 */
@Composable
internal fun PlayerOverlays(
    overlays: PlayerOverlayState,
    playerHaze: HazeState,
    accountName: String?,
    accentColor: Color?,
    isVideo: Boolean,
    isPlaying: Boolean,
    onListenTogether: () -> Unit,
    lyricsSource: LyricsSource?,
    lyricsProviderStates: Map<LyricsSource, LyricsProviderState>,
    onSelectLyricsProvider: (LyricsSource) -> Unit,
    lyricsOffsetOpen: Boolean,
    onDismissLyricsOffset: () -> Unit,
) {
    if (overlays.audioOutput) {
        AudioOutputSheet(
            hazeState = playerHaze,
            accountName = accountName,
            onDismiss = { overlays.audioOutput = false },
            onOpenCast = { overlays.cast = true },
            accentColor = accentColor,
        )
    }
    if (overlays.lyricsProviders) {
        LyricsProviderSheet(
            hazeState = playerHaze,
            currentSource = lyricsSource,
            states = lyricsProviderStates,
            onSelect = onSelectLyricsProvider,
            onDismiss = { overlays.lyricsProviders = false },
        )
    }
    if (overlays.lyricsStyle) {
        LyricsStyleSheet(
            hazeState = playerHaze,
            onDismiss = { overlays.lyricsStyle = false },
            accentColor = accentColor,
        )
    }
    if (overlays.audioPipeline) {
        AudioPipelineDialog(
            hazeState = playerHaze,
            isPlaying = isPlaying,
            onDismiss = { overlays.audioPipeline = false },
            accentColor = accentColor,
        )
    }
    if (overlays.videoQuality) {
        VideoQualitySheet(
            hazeState = playerHaze,
            onDismiss = { overlays.videoQuality = false },
            accentColor = accentColor,
        )
    }
    if (overlays.cast) {
        CastDialog(
            hazeState = playerHaze,
            onDismiss = { overlays.cast = false },
            accentColor = accentColor,
        )
    }
    if (overlays.listenTogetherMembers) {
        ListenTogetherMembersSheet(
            hazeState = playerHaze,
            onDismiss = { overlays.listenTogetherMembers = false },
            onManage = {
                overlays.listenTogetherMembers = false
                onListenTogether()
            },
        )
    }
    if (lyricsOffsetOpen) {
        LyricsOffsetSheet(
            hazeState = playerHaze,
            onDismiss = onDismissLyricsOffset,
        )
    }
    overlays.lyricsShare?.let { request ->
        PlayerPlatform.host.LyricsShareSheet(
            hazeState = playerHaze,
            request = request,
            onDismiss = { overlays.lyricsShare = null },
        )
    }
}

/**
 * Back, for everything the player raises over itself. Registered bottom layer
 * first: the panels, then the drawers and dialogs drawn over them.
 */
@Composable
internal fun PlayerBackHandlers(
    sideways: Boolean,
    videoMode: Boolean,
    fullscreenRequested: Boolean,
    onExitFullscreen: () -> Unit,
    panels: PlayerPanelState,
    overlays: PlayerOverlayState,
    lyricsOffsetOpen: Boolean,
    onDismissLyricsOffset: () -> Unit,
    lyricPicker: LyricsPicker,
) {
    PlayerBackHandler(enabled = sideways && (videoMode || fullscreenRequested), onBack = onExitFullscreen)
    PlayerBackHandler(enabled = panels.lyricsOpen, onBack = panels.closeLyrics)
    PlayerBackHandler(enabled = panels.queueOpen) { panels.queueOpen = false }
    PlayerBackHandler(enabled = overlays.audioOutput) { overlays.audioOutput = false }
    PlayerBackHandler(enabled = overlays.lyricsProviders) { overlays.lyricsProviders = false }
    PlayerBackHandler(enabled = overlays.lyricsStyle) { overlays.lyricsStyle = false }
    PlayerBackHandler(enabled = overlays.listenTogetherMembers) { overlays.listenTogetherMembers = false }
    PlayerBackHandler(enabled = overlays.audioPipeline) { overlays.audioPipeline = false }
    PlayerBackHandler(enabled = overlays.videoQuality) { overlays.videoQuality = false }
    PlayerBackHandler(enabled = overlays.cast) { overlays.cast = false }
    PlayerBackHandler(enabled = lyricsOffsetOpen, onBack = onDismissLyricsOffset)
    PlayerBackHandler(enabled = overlays.lyricsShare != null) { overlays.lyricsShare = null }
    PlayerBackHandler(enabled = lyricPicker.picking) { lyricPicker.cancel() }
}
