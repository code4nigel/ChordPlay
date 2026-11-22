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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.canvas.CanvasArtwork
import com.music.bitchord.data.lyrics.LyricLine
import com.music.bitchord.data.model.LikeStatus
import com.music.bitchord.data.model.Song
import com.music.bitchord.playback.PlaybackPosition
import com.music.bitchord.sharedui.resources.Res
import com.music.bitchord.sharedui.resources.lyrics_not_available
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.Haptics
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import org.jetbrains.compose.resources.stringResource

/**
 * Picture-in-Picture, Fullscreen video, and Tablet/Landscape two-column player
 * layouts. Extracted from [NowPlayingScreen] to keep composition modular.
 */
@Composable
internal fun LandscapeAndVideoNowPlaying(
    inPip: Boolean,
    videoMode: Boolean,
    landscape: Boolean,
    videoRendered: Boolean,
    onVideoRenderedChange: (Boolean) -> Unit,
    art: PlayerArtwork,
    song: Song,
    isPlaying: Boolean,
    isLoading: Boolean,
    position: PlaybackPosition,
    durationMs: Long,
    isLive: Boolean = song.isLive,
    isSeekable: Boolean = true,
    isAtLiveEdge: Boolean = true,
    liveOffsetMs: Long = 0L,
    onJumpToLive: () -> Unit = {},
    hasPrevious: Boolean,
    pastRestartPoint: Boolean,
    hasNext: Boolean,
    controlsLocked: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    shown: () -> Float,
    mixing: Boolean,
    scrub: PlayerScrub,
    volume: PlayerVolume,
    onScrub: (Float) -> Unit,
    onScrubFinished: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onVolumeChangeFinished: () -> Unit,
    onExitFullscreen: () -> Unit,
    onOpenVideoQuality: () -> Unit,
    onOpenPipeline: () -> Unit,
    likeBurstState: LikeBurstState,
    playerHaze: HazeState,
    playerOverlayHaze: HazeState = playerHaze,
    dockHost: Modifier,
    dock: PlayerDock?,
    dockFrame: DockFrame,
    dockT: () -> Float,
    docking: () -> Boolean,
    dockMoving: Boolean,
    dockFade: () -> Float,
    sleeveLayer: GraphicsLayer,
    panels: PlayerPanelState,
    synesthesiaPalette: MeshPalette,
    isWavePlay: Boolean,
    spotifyCanvasPresentation: Boolean,
    canvas: CanvasArtwork?,
    canvasRenderedState: State<Boolean>,
    onCanvasRenderedChange: (Boolean) -> Unit,
    skipSwipeGesture: Modifier,
    swipeSettle: Float,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (browseId: String?, name: String) -> Unit,
    onOpenPlaybackSource: () -> Unit,
    syncedLyricsEnabled: Boolean,
    hideLyricsPreview: Boolean = false,
    lyricsTranslation: LyricsTranslationUi,
    lyrics: List<LyricLine>?,
    lyricsPlayhead: LyricPlayhead,
    lyricsUnavailable: Boolean,
    lyricsLoadingText: String,
    seekToLyric: (Long) -> Unit,
    lyricsShareEnabled: Boolean,
    lyricPicker: LyricsPicker,
    onShowLyricsProviders: () -> Unit,
    onOpenLyricsStyle: () -> Unit,
    queue: List<Song>,
    queueIndex: Int,
    autoplayEnabled: Boolean,
    onJumpTo: (Int) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onMoveInQueue: (Int, Int) -> Unit,
    onClearQueue: () -> Unit,
    onQueueDragActiveChange: (Boolean) -> Unit,
    onPlayNext: (Song) -> Unit,
    signedIn: Boolean,
    likeStatus: LikeStatus,
    showRevertCue: Boolean,
    animatedCanvas: Boolean,
    haptics: Haptics,
    onToggleLike: () -> Unit,
    onAddToPlaylist: (() -> Unit)?,
    onOpenMenu: () -> Unit,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleAutoplay: () -> Unit,
    audioVersionSwitching: Boolean,
    hideVolumeBar: Boolean,
    playerActions: @Composable () -> Unit,
    playerOverlays: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (inPip && videoMode) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            if (!videoRendered) {
                VideoLoadingBackdrop(Modifier.fillMaxSize())
            }
            MusicVideoSurface(
                videoId = song.videoId,
                fit = true,
                onRenderedChanged = onVideoRenderedChange,
                modifier = Modifier.fillMaxSize(),
            )
        }
        return
    }

    if (landscape && videoMode) {
        CompositionLocalProvider(
            LocalLikeBurstState provides likeBurstState,
            LocalPlayerHazeState provides playerHaze,
        ) {
            Box(modifier = modifier.fillMaxSize().then(dockHost)) {
                Box(Modifier.fillMaxSize().hazeSource(playerOverlayHaze)) {
                    FullscreenVideoPlayer(
                    song = song,
                    isPlaying = isPlaying,
                    artRequest = art.request,
                    rendered = videoRendered,
                    onRenderedChanged = onVideoRenderedChange,
                    onExitFullscreen = onExitFullscreen,
                    onOpenQuality = onOpenVideoQuality,
                    isLive = isLive,
                    scrubber = {
                        when {
                            isLive -> LiveStationBar(
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
                            isWavePlay -> SynesthesiaWaveformScrubber(
                                shown = shown,
                                durationMs = durationMs,
                                isPlaying = isPlaying && position.advancing,
                                onScrub = onScrub,
                                onScrubFinished = onScrubFinished,
                                trackKey = song.videoId,
                                palette = synesthesiaPalette,
                            )
                            else -> PlayerScrubber(
                                shown = shown,
                                durationMs = durationMs,
                                loading = isLoading,
                                mixing = mixing && !scrub.scrubbing,
                                transitionWindow = null,
                                onScrub = onScrub,
                                onScrubFinished = onScrubFinished,
                            )
                        }
                    },
                    transport = {
                        val previousEnabled = !controlsLocked && if (isLive) hasPrevious else (hasPrevious || pastRestartPoint)
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
                                isLoading = isLoading,
                                previousEnabled = previousEnabled,
                                nextEnabled = !controlsLocked && hasNext,
                                onPrevious = onPrevious,
                                onPlayPause = onPlayPause,
                                onNext = onNext,
                                compact = true,
                            )
                        }
                    },
                )
                }
                playerOverlays()
            }
        }
        return
    }

    val landscapeArtScale = 1f
    val versionAligning by PlayerSettings.versionAlignmentInProgress.collectAsStateWithLifecycle()
    val transitionWindow by PlayerSettings.smartTransitionWindow.collectAsStateWithLifecycle()
    val panelOpen = panels.lyricsOpen || panels.queueOpen

    fun landscapeDockPose(): LandscapeDockPose? {
        if (dock == null) return null
        val t = dockT()
        val mini = dock.miniArtOnScreen() ?: return null
        val anchor = dockFrame.anchor ?: return null
        val host = dockFrame.host?.takeIf { it.isAttached } ?: return null
        val sleeve = dockFrame.landscapeArt?.takeIf { it.isAttached } ?: return null
        val offset = (dock.offset() ?: return null).toInt().toFloat()
        val travel = dock.sheetTravel.takeIf { it > 0f } ?: return null
        if (sleeve.size.width <= 0) return null
        val open = Rect(
            offset = anchor + host.localPositionOf(sleeve, Offset.Zero),
            size = Size(sleeve.size.width.toFloat(), sleeve.size.height.toFloat()),
        )
        val ride = ((mini.top - open.top) / travel).coerceIn(0f, 1f)
        val rect = lerp(mini, open.translate(0f, offset * ride), t)
        return LandscapeDockPose(
            left = rect.left - anchor.x,
            top = rect.top - anchor.y - offset,
            scale = rect.width / open.width,
        )
    }

    val landscapeArtShape: () -> Shape = {
        val t = dockT()
        if (dock == null || t >= 1f) {
            dockFrame.cornerShape(LANDSCAPE_ART_CORNER)
        } else {
            val scale = landscapeDockPose()?.scale?.takeIf { it > 0f } ?: 1f
            dockFrame.cornerShape(lerp(dock.miniArtCorner(), LANDSCAPE_ART_CORNER, t) / scale)
        }
    }

    CompositionLocalProvider(
        LocalLikeBurstState provides likeBurstState,
        LocalPlayerHazeState provides playerHaze,
    ) {
        Box(modifier = modifier.fillMaxSize().then(dockHost)) {
            val landscapeFontScale by PlayerSettings.landscapeFontScale.collectAsStateWithLifecycle()

            Box(Modifier.fillMaxSize().hazeSource(playerOverlayHaze)) {
            LandscapePlayerLayout(
                pane = when {
                    panels.lyricsOpen -> PlayerPane.Lyrics
                    panels.queueOpen -> PlayerPane.Queue
                    else -> PlayerPane.Main
                },
                onSelectPane = { selectedPane ->
                    when (selectedPane) {
                        PlayerPane.Main -> panels.closePanels()
                        PlayerPane.Lyrics -> panels.openLyrics()
                        PlayerPane.Queue -> {
                            panels.queueOpen = true
                            panels.closeLyrics()
                        }
                    }
                },
                onOpenPipeline = onOpenPipeline,
                queueIcon = playerQueueIcon(
                    repeatMode = repeatMode,
                    shuffleEnabled = shuffleEnabled,
                    autoplayEnabled = autoplayEnabled,
                ),
                palette = synesthesiaPalette,
                background = { backgroundModifier ->
                    Box(modifier = backgroundModifier) {
                        MeshGradientBackground(
                            palette = synesthesiaPalette,
                            trackKey = song.videoId,
                            modifier = Modifier
                                .fillMaxSize()
                                .hazeSource(playerHaze),
                        )
                        if (isWavePlay && !spotifyCanvasPresentation) {
                            SynesthesiaNocturneOverlay(
                                palette = synesthesiaPalette,
                                isPlaying = isPlaying,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                },
                artwork = { artworkModifier ->
                    LandscapeArtwork(
                        artRequest = art.request,
                        artLoaded = art.loaded,
                        onArtState = art::onState,
                        canvas = canvas,
                        canvasRendered = canvasRenderedState.value,
                        isPlaying = isPlaying,
                        onCanvasRenderedChange = onCanvasRenderedChange,
                        positionMs = if (scrub.scrubbing) (scrub.value * durationMs).toLong() else position.positionMs,
                        pausedForTransition = dockMoving,
                        sleeveShape = landscapeArtShape,
                        shadowFraction = dockT,
                        modifier = artworkModifier
                            .then(
                                if (dock != null) {
                                    Modifier.onGloballyPositioned { dockFrame.landscapeArt = it }
                                } else {
                                    Modifier
                                },
                            )
                            .drawWithContent {
                                val pose = if (docking()) landscapeDockPose() else null
                                if (pose != null) {
                                    dockFrame.portalLeft = pose.left
                                    dockFrame.portalTop = pose.top
                                    dockFrame.portalScale = pose.scale
                                    sleeveLayer.record { this@drawWithContent.drawContent() }
                                } else {
                                    if (docking()) {
                                        dockFrame.portalScale = 0f
                                    }
                                    drawContent()
                                }
                            }
                            .then(skipSwipeGesture)
                            .graphicsLayer {
                                val t = dockT()
                                val scale = if (t >= 1f) landscapeArtScale else 1f + (landscapeArtScale - 1f) * t
                                scaleX = scale
                                scaleY = scale
                                translationX = if (t >= 1f) swipeSettle else swipeSettle * t
                            }
                            .clickable(
                                enabled = panelOpen,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                panels.closePanels()
                            },
                    ) {
                        SleeveNerdStats(
                            song = song,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = 16.dp, top = 56.dp)
                                .graphicsLayer { alpha = if (panelOpen) 0f else dockFade() },
                        )
                    }
                },
                artworkActions = {
                    LandscapeArtworkActions(
                        song = song,
                        signedIn = signedIn,
                        likeStatus = likeStatus,
                        showRevertCue = showRevertCue,
                        animatedCanvas = animatedCanvas,
                        onToggleAnimatedCanvas = {
                            haptics.play(Haptic.Select)
                            PlayerSettings.setAnimatedCanvas(!animatedCanvas)
                        },
                        onToggleLike = onToggleLike,
                        onAddToPlaylist = onAddToPlaylist,
                        onOpenMenu = onOpenMenu,
                        palette = synesthesiaPalette,
                        currentPane = when {
                            panels.lyricsOpen -> PlayerPane.Lyrics
                            panels.queueOpen -> PlayerPane.Queue
                            else -> PlayerPane.Main
                        },
                        shuffleEnabled = shuffleEnabled,
                        repeatMode = repeatMode,
                        autoplayEnabled = autoplayEnabled,
                        onToggleShuffle = onToggleShuffle,
                        onCycleRepeat = onCycleRepeat,
                        onToggleAutoplay = onToggleAutoplay,
                    )
                },
                topBar = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .videoDockGlassPill(hazeState = playerHaze)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onOpenPlaybackSource,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "Minimize",
                                tint = Color.White.copy(alpha = 0.90f),
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                },
                actions = playerActions,
                mainPane = { compact ->
                    LandscapeMainPane(
                        compact = compact,
                        credits = {
                            LandscapeCredits(
                                song = song,
                                onOpenAlbum = onOpenAlbum,
                                onOpenArtist = onOpenArtist,
                                fontScale = landscapeFontScale,
                            )
                        },
                        lyricStrip = if (syncedLyricsEnabled && !hideLyricsPreview && !isLive) {
                            {
                                val stripFontSize = (16 * landscapeFontScale.scaleFactor).sp
                                CurrentLyricStrip(
                                    lines = lyricsTranslation.displayedLyrics,
                                    trackKey = song.videoId,
                                    playhead = lyricsPlayhead,
                                    isPlaying = isPlaying && position.advancing,
                                    durationMs = durationMs,
                                    lyricsUnavailable = lyricsUnavailable,
                                    loadingText = lyricsLoadingText,
                                    onClick = panels.openLyrics,
                                    textStyle = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = stripFontSize,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                )
                            }
                        } else {
                            null
                        },
                        scrubber = {
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
                                PlayerScrubber(
                                    shown = shown,
                                    durationMs = durationMs,
                                    loading = versionAligning || audioVersionSwitching,
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
                        },
                        transport = {
                            val previousEnabled = !controlsLocked && if (isLive) hasPrevious else (hasPrevious || pastRestartPoint)
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
                                    compact = compact,
                                )
                            }
                        },
                        volume = if (hideVolumeBar) {
                            null
                        } else {
                            {
                                VolumeRow(
                                    value = { volume.level.value },
                                    onValueChange = onVolumeChange,
                                    onValueChangeFinished = onVolumeChangeFinished,
                                )
                            }
                        },
                    )
                },
                lyricsPane = {
                    LandscapeLyricsPane(
                        palette = synesthesiaPalette,
                        hasLyrics = lyricsTranslation.displayedLyrics.isNotEmpty(),
                        placeholder = if (lyricsUnavailable) {
                            stringResource(Res.string.lyrics_not_available)
                        } else {
                            lyricsLoadingText
                        },
                        status = lyricsTranslation.status,
                        onStatusClick = onShowLyricsProviders,
                        picking = lyricPicker.picking,
                        pickBar = {
                            LyricsPickBar(
                                shareEnabled = lyricPicker.picks.isNotEmpty() && !lyricPicker.overBudget,
                                onCancel = lyricPicker.cancel,
                                onShare = lyricPicker.share,
                            )
                        },
                        showingRomanization = lyricsTranslation.showingRomanization,
                        romanizationState = lyricsTranslation.romanizationState,
                        onToggleRomanization = lyricsTranslation.toggleRomanization,
                        showingTranslation = lyricsTranslation.showingTranslation,
                        translationState = lyricsTranslation.translationState,
                        onToggleTranslation = lyricsTranslation.toggleTranslation,
                        onOpenStyle = onOpenLyricsStyle,
                    ) { panelModifier ->
                        LyricsTranslationMotion(
                            trigger = lyricsTranslation.transition,
                            reduceMotion = lyricsTranslation.reduceMotion,
                            modifier = panelModifier,
                        ) { particleProgress ->
                            LyricsPanel(
                                lines = lyrics.orEmpty(),
                                subLines = lyricsTranslation.subLines,
                                trackKey = song.videoId,
                                playhead = lyricsPlayhead,
                                looking = !lyricsUnavailable,
                                isPlaying = isPlaying && position.advancing,
                                onSeekToLine = seekToLyric,
                                controlsOpen = true,
                                onRevealControls = {},
                                onHideControls = {},
                                translationProgress = particleProgress,
                                canPick = lyricsShareEnabled,
                                picking = lyricPicker.picking,
                                picked = lyricPicker.picks,
                                onPickLine = lyricPicker.pick,
                                onTogglePick = lyricPicker.toggle,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                },
                queuePane = {
                    InlineQueue(
                        queue = queue,
                        currentIndex = queueIndex,
                        autoplayEnabled = autoplayEnabled,
                        controlsLocked = controlsLocked,
                        onJumpTo = onJumpTo,
                        onRemove = onRemoveFromQueue,
                        onMove = onMoveInQueue,
                        onClear = onClearQueue,
                        onDragActiveChange = onQueueDragActiveChange,
                        onPlayNext = onPlayNext,
                        modifier = Modifier.fillMaxSize(),
                    )
                },
            )
            }
            playerOverlays()
        }
    }
}
