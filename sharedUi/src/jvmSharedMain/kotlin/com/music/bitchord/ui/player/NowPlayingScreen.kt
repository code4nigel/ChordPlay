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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.canvas.CanvasSource
import com.music.bitchord.data.lyrics.LyricLine
import com.music.bitchord.data.lyrics.LyricsSource
import com.music.bitchord.data.model.LikeStatus
import com.music.bitchord.data.model.PLAYER_ART_PX
import com.music.bitchord.data.model.PlaybackSourceType
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.playerArtPx
import com.music.bitchord.data.settings.AppDesignStyle
import com.music.bitchord.data.settings.LastPlayerScreen
import com.music.bitchord.data.settings.PlayerInterfaceStyle
import com.music.bitchord.data.settings.PlayerLayoutMode
import com.music.bitchord.playback.BACK_RESTARTS_AFTER_MS
import com.music.bitchord.playback.PlaybackPosition
import com.music.bitchord.sharedui.resources.Res
import com.music.bitchord.sharedui.resources.lyrics_loading_lines
import com.music.bitchord.ui.LyricsProviderState
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import com.music.bitchord.ui.theme.StatusBarIcons
import com.music.bitchord.ui.theme.rememberArtworkTopBandLuminance
import com.music.bitchord.ui.theme.rememberScreenOrientation
import com.music.bitchord.ui.theme.topBandScrimAlpha
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringArrayResource
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

val LocalPlayerHazeState = staticCompositionLocalOf<HazeState?> { null }

/** Maximum artwork decode width shared with [com.music.bitchord.data.model.PLAYER_ART_PX]. */
internal const val ART_PX = PLAYER_ART_PX

/** How many attempts the cover art decode gets before giving up. */
internal const val ART_RETRIES = 3

/** @see ART_RETRIES */
internal const val ART_RETRY_DELAY_MS = 1_500L

/** Time the player lets an album title settle before showing an empty subtitle. */
internal const val ALBUM_SETTLE_MS = 700L

private const val REVERT_CUE_MS = 2_600L

/** Distance in ms before a pending seek is considered settled. */
internal const val SEEK_SETTLE_TOLERANCE_MS = 1_500L

/** Fallback timeout for pending seeks. */
internal const val SEEK_SETTLE_TIMEOUT_MS = 4_000L

private val THUMB_SIZE = 54.dp
private val HEADER_HEIGHT = 60.dp
private val ART_TITLE_GAP = 20.dp
private const val QUEUE_TRAVEL_MS = 655
private val DISMISS_STRIP_HEIGHT = 32.dp
private val ART_BOX_TOP_PAD = 8.dp
private const val HERO_FADE_FRACTION = 0.42f
private const val SUBVIEW_STATUS_SCRIM_MIN_ALPHA = 0.40f
private val BACKDROP_LIFT = 48.dp
private val MESH_SEAM_NUDGE = 16.dp

/** Live blur ON: capture frames frequently (~8fps) to smoothly animate the wash. */
private const val LIVE_REFRESH_LIVE_MS = 125L

/** Live blur OFF: sample slowly once every 3s for subtle palette drift. */
private const val LIVE_REFRESH_MS = 3_000L

/** Reduce animation mode: low-cadence palette updates. */
private const val LIVE_REFRESH_REDUCED_MS = 1_200L

private val TABLET_PLAYER_MIN_WIDTH = 700.dp
private val LANDSCAPE_PLAYER_MIN_WIDTH = 560.dp
private val CONTROL_GAP_SPREAD_MAX = 24.dp
private var lastControlSpread: Dp = 0.dp
private var lastHeroSettled = false
private const val DOCK_FADE_LEAD = 0.15f

fun fullBleedArtworkAvailable(windowWidth: Dp): Boolean =
    playerFillsWindow(windowWidth) || tabletSizedPlayer(windowWidth)

private fun playerFillsWindow(windowWidth: Dp): Boolean =
    windowWidth <= PLAYER_MAX_WIDTH + PLAYER_GUTTER * 2

private fun tabletSizedPlayer(windowWidth: Dp): Boolean =
    windowWidth >= TABLET_PLAYER_MIN_WIDTH

fun landscapePlayerAvailable(windowWidth: Dp, windowHeight: Dp): Boolean =
    windowWidth > windowHeight && windowWidth >= LANDSCAPE_PLAYER_MIN_WIDTH

/**
 * Full-screen / in-panel expanded audio & video player for ChordPlay.
 */
@Composable
fun NowPlayingScreen(
    song: Song,
    playedBy: String? = null,
    isPlaying: Boolean,
    isLoading: Boolean,
    position: PlaybackPosition,
    durationMs: Long,
    isLive: Boolean = song.isLive,
    isSeekable: Boolean = true,
    isAtLiveEdge: Boolean = true,
    liveOffsetMs: Long = 0L,
    onJumpToLive: () -> Unit = {},
    audioVersionSwitching: Boolean,
    qualityUpgraded: Boolean,
    queue: List<Song>,
    queueIndex: Int,
    hasPrevious: Boolean,
    hasNext: Boolean,
    repeatMode: Int,
    shuffleEnabled: Boolean,
    autoplayEnabled: Boolean,
    signedIn: Boolean,
    accountName: String?,
    likeStatus: LikeStatus,
    onToggleLike: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onBlockedControl: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekFraction: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleAutoplay: () -> Unit,
    onJumpTo: (Int) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onMoveInQueue: (Int, Int) -> Unit,
    onPlayNext: ((Song) -> Unit)? = null,
    queueTools: NowPlayingQueueTools? = null,
    onQueueDragActiveChange: (Boolean) -> Unit = {},
    onClearQueue: () -> Unit,
    onOpenMenu: () -> Unit,
    onAddToPlaylist: (() -> Unit)? = null,
    onShareSong: (() -> Unit)? = null,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (browseId: String?, name: String) -> Unit,
    onOpenPlaybackSource: () -> Unit,
    onListenTogether: () -> Unit,
    lyrics: List<LyricLine>?,
    lyricsSource: LyricsSource?,
    lyricsProviderStates: Map<LyricsSource, LyricsProviderState>,
    onSelectLyricsProvider: (LyricsSource) -> Unit,
    lyricsUnavailable: Boolean,
    lyricsOffsetOpen: Boolean,
    onDismissLyricsOffset: () -> Unit,
    windowWidth: Dp,
    windowHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val haptics = rememberHaptics()
    val artPx = remember(windowWidth, density.density) {
        playerArtPx(windowWidth.value, density.density)
    }

    val remoteArt = rememberRemoteArtworkUrl(song)
    val artTopLuminance = rememberArtworkTopBandLuminance(remoteArt, artPx)
    val artworkStatusScrimAlpha = topBandScrimAlpha(artTopLuminance)
    val isTopBright = (artTopLuminance ?: 0f) > 0.45f

    val playerHaze = remember { HazeState() }
    val overlays = remember { PlayerOverlayState() }
    val openAudioOutput = rememberOutputPicker { overlays.audioOutput = true }
    var automixStudioOpen by remember { mutableStateOf(false) }
    val queueActions = queueTools?.let { QueueActions(it.onSave) { overlays.queueSearch = true } }
    val controlsLocked = rememberControlsLocked()
    val likeBurstState = remember { LikeBurstState() }
    LaunchedEffect(song.videoId) { likeBurstState.clear() }
    DisposableEffect(likeBurstState) {
        onDispose { likeBurstState.clear() }
    }

    val syncedLyricsEnabled by PlayerSettings.syncedLyrics.collectAsStateWithLifecycle()
    val hideLyricsPreview by PlayerSettings.hideLyricsPreview.collectAsStateWithLifecycle()
    val lyricsOffsetMs by PlayerSettings.lyricsOffsetMs.collectAsStateWithLifecycle()
    val lyricsPlayhead = rememberLyricPlayhead(position)
    val seekToLyric: (Long) -> Unit = { lineTimeMs ->
        onSeek(adjustedLyricsSeekTarget(lineTimeMs, lyricsOffsetMs))
    }
    val hideVolumeBar by PlayerSettings.hideVolumeBar.collectAsStateWithLifecycle()
    val hideSongStatus by PlayerSettings.hideSongStatus.collectAsStateWithLifecycle()
    val appDesignStyle by PlayerSettings.appDesignStyle.collectAsStateWithLifecycle()
    val playerLayoutMode by PlayerSettings.playerLayoutMode.collectAsStateWithLifecycle()
    val enableBreathingAura by PlayerSettings.enableBreathingAura.collectAsStateWithLifecycle()
    val fullscreenLyricsStyle by PlayerSettings.fullscreenArtworkLyricsStyle.collectAsStateWithLifecycle()
    val isChordPlay = appDesignStyle == AppDesignStyle.CHORDPLAY
    val isModernAmbient = isChordPlay && playerLayoutMode == PlayerLayoutMode.MODERN_AMBIENT
    val isVinylRecord = isChordPlay && playerLayoutMode == PlayerLayoutMode.VINYL_RECORD
    // Both supported ChordPlay layouts (Modern Ambient and Vinyl Turntable) use
    // ChordPlay's WavePlay deck so the glowing Play/Pause orb is never lost.
    val isModernOrVinyl = isChordPlay
    val playerInterfaceStyle by PlayerSettings.playerInterfaceStyle.collectAsStateWithLifecycle()
    val playerWaveStyle by PlayerSettings.playerWaveStyle.collectAsStateWithLifecycle()
    val isWavePlay = isChordPlay && playerInterfaceStyle == PlayerInterfaceStyle.WAVEPLAY
    val effectiveIsLive = isLive || song.isLive

    LaunchedEffect(song.videoId, playerWaveStyle, durationMs, effectiveIsLive) {
        if (isWavePlay && !effectiveIsLive &&
            playerWaveStyle == com.music.bitchord.data.settings.PlayerWaveStyle.AUDIO_GRAPH &&
            song.videoId.isNotBlank()
        ) {
            PlayerSettings.requestTrackWaveform(song.videoId, durationMs)
        }
    }

    val spotifyCanvasAutoHide by PlayerSettings.spotifyCanvasAutoHide.collectAsStateWithLifecycle()
    val animatedCanvas by PlayerSettings.animatedCanvas.collectAsStateWithLifecycle()
    val mixing by PlayerSettings.smartMixInProgress.collectAsStateWithLifecycle()
    val noPictureIds by PlayerPlatform.host.musicVideoNoPictureIds.collectAsStateWithLifecycle()
    val videoMode = showsMusicVideo(song) && song.videoId !in noPictureIds
    val videoFullBleed by PlayerSettings.videoFullBleed.collectAsStateWithLifecycle()
    val videoFill by PlayerSettings.videoFill.collectAsStateWithLifecycle()
    var videoControlsVisible by remember { mutableStateOf(true) }
    val videoControlsAlpha by animateFloatAsState(
        targetValue = if (videoControlsVisible) 1f else 0f,
        animationSpec = tween(180),
        label = "videoControlsAlpha",
    )
    LaunchedEffect(videoControlsVisible, isPlaying) {
        if (videoControlsVisible && isPlaying) {
            delay(3500)
            videoControlsVisible = false
        }
    }
    var videoRendered by remember(song.videoId) { mutableStateOf(false) }
    val inPip by PlayerPlatform.host.inPictureInPicture.collectAsStateWithLifecycle()
    val videoAspectState = PlayerPlatform.host.musicVideoAspect.collectAsStateWithLifecycle()
    val screenOrientation = rememberScreenOrientation()
    var fullscreenRequested by remember { mutableStateOf(false) }
    val sideways = landscapePlayerAvailable(windowWidth, windowHeight)
    LaunchedEffect(sideways) { if (!sideways) fullscreenRequested = false }
    val exitFullscreen = {
        fullscreenRequested = false
        screenOrientation.portrait()
    }

    val canvas = rememberCanvasArtwork(song, enabled = !videoMode)?.takeUnless { videoMode }
    val clipState = remember(canvas?.url) { CanvasClipState() }
    val synesthesiaPalette = rememberPlayerArtworkPalette(remoteArt, clipState)

    val legacyMesh by PlayerSettings.legacyMeshGradient.collectAsStateWithLifecycle()
    val reduceAnimation by PlayerSettings.reduceAnimation.collectAsStateWithLifecycle()
    val disableInfiniteLoops by PlayerSettings.disableInfiniteLoops.collectAsStateWithLifecycle()
    val reduceDynamicBlur by PlayerSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val liveBlur by PlayerSettings.liveBlur.collectAsStateWithLifecycle()
    val liveRefreshMs = when {
        reduceAnimation || disableInfiniteLoops -> LIVE_REFRESH_REDUCED_MS
        liveBlur -> LIVE_REFRESH_LIVE_MS
        else -> LIVE_REFRESH_MS
    }

    val scrub = rememberPlayerScrub(song.videoId, position, durationMs)
    val shown: () -> Float = {
        if (effectiveIsLive) {
            when {
                scrub.scrubbing -> scrub.value
                scrub.pendingSeek != null -> scrub.pendingSeek!!
                isAtLiveEdge || !isSeekable || durationMs <= 0L -> 1f
                else -> (position.positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
            }
        } else {
            scrub.shown(position.positionMs, durationMs)
        }
    }
    val handleJumpToLive: () -> Unit = {
        scrub.pendingSeek = null
        onJumpToLive()
    }
    val pastRestartPoint by remember(position) {
        derivedStateOf { position.positionMs > BACK_RESTARTS_AFTER_MS }
    }

    val restoredPlayerScreen = remember { PlayerSettings.lastPlayerScreen.value }
    val panels = remember { PlayerPanelState(restoredPlayerScreen) }
    var localLyricsOffsetOpen by remember { mutableStateOf(false) }
    val effectiveLyricsOffsetOpen = lyricsOffsetOpen || localLyricsOffsetOpen
    val dismissLyricsOffset = {
        localLyricsOffsetOpen = false
        onDismissLyricsOffset()
    }

    val dock = LocalPlayerDock.current
    if (dock != null) {
        DisposableEffect(dock) {
            onDispose { dock.attached = false }
        }
    }
    var playerPrewarmStage by remember(song.videoId) { mutableIntStateOf(0) }
    val currentDock by rememberUpdatedState(dock)
    LaunchedEffect(song.videoId) {
        playerPrewarmStage = 1
        snapshotFlow { currentDock?.settled ?: true }.first { it }
        delay(650)
        playerPrewarmStage = 2
        delay(650)
        playerPrewarmStage = 3
    }

    LaunchedEffect(panels.lyricsOpen) { if (!panels.lyricsOpen) panels.lyricsScrolling = false }
    LaunchedEffect(panels.queueOpen) { if (!panels.queueOpen) panels.queueScrolling = false }
    val panelScrolling = panels.lyricsScrolling || panels.queueScrolling
    val playerDeckReveal = remember { Animatable(1f) }
    val playerDeckSettledOpen by remember(playerDeckReveal) {
        derivedStateOf { playerDeckReveal.value >= 0.999f }
    }
    LaunchedEffect(panels.lyricsOpen, panels.queueOpen) {
        PlayerSettings.setLastPlayerScreen(
            when {
                panels.lyricsOpen -> LastPlayerScreen.LYRICS
                panels.queueOpen -> LastPlayerScreen.QUEUE
                else -> LastPlayerScreen.MAIN
            },
        )
        if (panels.queueOpen) panels.queueControlsOpen = true
    }

    val lyricsLoadingLines = stringArrayResource(Res.array.lyrics_loading_lines)
    val lyricsLoadingText = remember(song.videoId) { lyricsLoadingLines.random() }
    val lyricsTranslation = rememberLyricsTranslation(
        trackId = song.videoId,
        lyrics = lyrics,
        lyricsSource = lyricsSource,
        lyricsUnavailable = lyricsUnavailable,
        loadingText = lyricsLoadingText,
        haptics = haptics,
    )
    val lyricsShareEnabled = lyricsShareAvailable
    val lyricPicker = rememberLyricsPicker(
        song = song,
        lines = lyrics,
        subLines = lyricsTranslation.subLines,
        artworkUrl = remoteArt,
        haptics = haptics,
        onCard = { overlays.lyricsShare = it },
    )

    var showRevertCue by remember(song.videoId) { mutableStateOf(false) }
    LaunchedEffect(song.videoId, qualityUpgraded) {
        if (!qualityUpgraded) {
            showRevertCue = false
            return@LaunchedEffect
        }
        showRevertCue = true
        delay(REVERT_CUE_MS)
        showRevertCue = false
    }

    KeepScreenOn(enabled = panels.lyricsOpen)

    PlayerBackHandlers(
        sideways = sideways,
        videoMode = videoMode,
        fullscreenRequested = fullscreenRequested,
        onExitFullscreen = exitFullscreen,
        panels = panels,
        overlays = overlays,
        lyricsOffsetOpen = effectiveLyricsOffsetOpen,
        onDismissLyricsOffset = dismissLyricsOffset,
        lyricPicker = lyricPicker,
    )

    val playerAnimationSpeed by PlayerSettings.playerAnimationSpeed.collectAsStateWithLifecycle()
    val animSpeed = playerAnimationSpeed.coerceIn(0.5f, 2.0f)

    LaunchedEffect(panels.queueOpen, panels.queueDragging, panels.queueReleased, animSpeed) {
        if (panels.queueDragging) return@LaunchedEffect
        val target = if (panels.queueOpen) 1f else 0f
        val from = panels.queueSlide.floatValue
        if (from == target) return@LaunchedEffect
        animate(
            initialValue = from,
            targetValue = target,
            animationSpec = tween(
                durationMillis = ((QUEUE_TRAVEL_MS * abs(target - from)) / animSpeed)
                    .roundToInt()
                    .coerceIn(160, 1500),
                easing = DreamyEaseOutEasing,
            ),
        ) { value, _ -> panels.queueSlide.floatValue = value }
    }

    val swipeThreshold = with(density) { 72.dp.toPx() }
    val swipeSettle = remember { Animatable(0f) }
    val swipeTargets = remember { Channel<Float>(Channel.CONFLATED) }
    LaunchedEffect(swipeTargets) {
        for (target in swipeTargets) {
            val newest = swipeTargets.tryReceive().getOrNull() ?: target
            launch {
                if (newest != swipeSettle.targetValue) {
                    swipeSettle.animateTo(newest, spring(stiffness = Spring.StiffnessMediumLow))
                }
            }
        }
    }
    val setSwipeOffset: (Float) -> Unit = { swipeTargets.trySend(it) }
    val swipeHintShown by remember(swipeThreshold) {
        derivedStateOf { abs(swipeSettle.value) / swipeThreshold > 0.01f }
    }
    val swipeHintNext by remember { derivedStateOf { swipeSettle.value < 0f } }

    val artScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.86f,
        animationSpec = spring(
            dampingRatio = 0.88f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "artScale",
    )

    val volume = rememberPlayerVolume()
    LyricsControlsIdleEffect(
        panels = panels,
        scrub = scrub,
        volume = volume,
        idleMs = LYRICS_CONTROLS_IDLE_MS,
    )
    QueueControlsIdleEffect(
        panels = panels,
        suspended = scrub.scrubbing || volume.dragging || panels.queueDragging || panels.queueReordering ||
            overlays.queueSearch || queueTools?.saveOpen == true || automixStudioOpen || overlays.audioOutput,
        idleMs = LYRICS_CONTROLS_IDLE_MS,
    )

    val animatedCollapse = animateFloatAsState(
        targetValue = if (panels.lyricsOpen || panels.queueOpen) 1f else 0f,
        animationSpec = tween(durationMillis = (655f / animSpeed).roundToInt().coerceIn(160, 1500), easing = DreamyEaseOutEasing),
        label = "sleeveCollapse",
    )
    val collapse = remember(panels, animatedCollapse) { PlayerCollapse(panels, animatedCollapse) }
    val p = collapse.p

    val panelFade = remember {
        Animatable(if (Snapshot.withoutReadObservation { collapse.done }) 1f else 0f)
    }
    LaunchedEffect(collapse, animSpeed) {
        snapshotFlow { collapse.done }.collectLatest { settled ->
            panelFade.animateTo(
                if (settled) 1f else 0f,
                tween(durationMillis = (340f / animSpeed).roundToInt().coerceIn(80, 780), easing = DreamyEaseOutEasing),
            )
        }
    }

    val fullBleedArt by PlayerSettings.fullBleedArtwork.collectAsStateWithLifecycle()
    val tapArtworkToToggleFullScreen by PlayerSettings.tapArtworkToToggleFullScreen.collectAsStateWithLifecycle()
    val spotifyCanvasOnPhone = canvas?.source == CanvasSource.SPOTIFY && playerFillsWindow(windowWidth)
    val art = rememberPlayerArtwork(remoteArt, artPx)
    val coverPresentation = rememberCoverPresentation(
        song = song,
        currentArt = art,
        artPx = artPx,
        enabled = !reduceAnimation && !disableInfiniteLoops && !videoMode && !effectiveIsLive,
        upcoming = queue.getOrNull(queueIndex + 1).takeIf { hasNext },
        paletteColors = synesthesiaPalette.colors,
    )
    var stableCanvasHero by remember { mutableStateOf(spotifyCanvasOnPhone) }
    if (coverPresentation == null) SideEffect { stableCanvasHero = spotifyCanvasOnPhone }
    val canvasHero = if (coverPresentation == null) spotifyCanvasOnPhone else stableCanvasHero
    val videoHeroMode = videoMode && playerFillsWindow(windowWidth)
    val heroMode = videoHeroMode || (!videoMode && (
        canvasHero || (fullBleedArt && playerFillsWindow(windowWidth))
        ))

    var heroSettled by remember { mutableStateOf(lastHeroSettled) }
    LaunchedEffect(art, clipState.rendered) {
        if (heroSettled) return@LaunchedEffect
        snapshotFlow { art.loaded || clipState.rendered.value }.first { it }
        heroSettled = true
        lastHeroSettled = true
    }

    val heroClip = canvas?.takeIf { heroMode && !collapse.pastHalf && coverPresentation == null }
    val spotifyCanvasFullscreen = spotifyCanvasOnPhone && heroClip?.source == CanvasSource.SPOTIFY
    val spotifyCanvasPresentation = spotifyCanvasFullscreen && clipState.rendered.value
    val canvasActiveOnPlayer = spotifyCanvasPresentation || (heroClip != null && clipState.rendered.value) || (!heroMode && coverPresentation == null && canvas != null && clipState.rendered.value)
    val canvasFirstPortrait = !spotifyCanvasFullscreen &&
        heroClip != null && clipState.aspect > 0f && clipState.aspect < 1f
    val heroClipMounted = canvas?.takeIf {
        heroMode && coverPresentation == null && (!(panels.lyricsOpen || panels.queueOpen) || !collapse.done)
    }
    val clipFullscreen = spotifyCanvasOnPhone && heroClipMounted?.source == CanvasSource.SPOTIFY
    val clipPortrait = !clipFullscreen &&
        heroClipMounted != null && clipState.aspect > 0f && clipState.aspect < 1f

    val canvasAutoHideBlocked = !isPlaying || scrub.scrubbing || volume.dragging ||
        panels.lyricsOpen || panels.queueOpen || effectiveLyricsOffsetOpen || overlays.lyricsStyle ||
        overlays.audioPipeline || overlays.videoQuality || overlays.cast || overlays.audioOutput ||
        overlays.lyricsProviders || mixing
    LaunchedEffect(
        spotifyCanvasPresentation,
        clipState.spotifyControlsOpen,
        spotifyCanvasAutoHide,
        canvasAutoHideBlocked,
    ) {
        if (!spotifyCanvasPresentation || !clipState.spotifyControlsOpen ||
            !spotifyCanvasAutoHide || canvasAutoHideBlocked
        ) return@LaunchedEffect
        delay(SPOTIFY_CANVAS_CONTROLS_IDLE_MS)
        clipState.spotifyControlsOpen = false
    }
    LaunchedEffect(spotifyCanvasPresentation, panels.lyricsOpen, panels.queueOpen) {
        if (spotifyCanvasPresentation && !panels.lyricsOpen && !panels.queueOpen) {
            clipState.spotifyControlsOpen = true
        }
    }
    LaunchedEffect(spotifyCanvasPresentation, mixing) {
        if (spotifyCanvasPresentation && mixing) clipState.spotifyControlsOpen = true
    }

    val heroT = animateFloatAsState(
        targetValue = if (
            heroMode && (heroSettled || art.loaded || clipState.rendered.value || videoRendered)
        ) 1f else 0f,
        animationSpec = tween(durationMillis = 440, easing = DreamyEaseOutEasing),
        label = "heroCanvas",
    )
    val spotifyChromeAlpha by animateFloatAsState(
        targetValue = if (!spotifyCanvasPresentation || clipState.spotifyControlsOpen) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = DreamyEaseOutEasing),
        label = "spotifyCanvasChrome",
    )

    val measures = remember { PlayerMeasures() }
    val darkTopBarIcons by remember(isTopBright) {
        derivedStateOf { isTopBright && heroT.value > 0.5f && !panels.lyricsOpen && !panels.queueOpen }
    }
    val topBarGlyphTint by animateColorAsState(
        targetValue = if (isTopBright && heroMode && !panels.lyricsOpen && !panels.queueOpen) {
            Color(0xFF161616).copy(alpha = 0.88f)
        } else {
            Color.White.copy(alpha = 0.85f)
        },
        animationSpec = tween(durationMillis = 360, easing = DreamyEaseOutEasing),
        label = "topBarGlyphTint",
    )
    StatusBarIcons(dark = darkTopBarIcons)
    fun canvasBottom(portrait: Boolean): Dp =
        if (portrait && measures.playerBounds.width > 0 && measures.playerBounds.height > 0) {
            val viewAspect = measures.playerBounds.width.toFloat() / measures.playerBounds.height
            val videoHeight = if (clipState.aspect >= viewAspect) measures.playerBounds.width / clipState.aspect
            else measures.playerBounds.height.toFloat()
            with(density) { videoHeight.toDp() }
        } else 0.dp
    val renderedCanvasBottom = canvasBottom(canvasFirstPortrait)
    val clipBottom = canvasBottom(clipPortrait)
    val clipFadeFraction = if (clipBottom > 0.dp) {
        (measures.heroHeight.value * HERO_FADE_FRACTION / clipBottom.value).coerceIn(0f, 1f)
    } else 0f
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val topStrip = DISMISS_STRIP_HEIGHT

    val openPlaybackOrigin: () -> Unit = {
        if (playedBy != null) {
            onListenTogether()
        } else if (song.playbackSourceType == PlaybackSourceType.QUEUE) {
            panels.queueOpen = true
            panels.closeLyrics()
        } else {
            onOpenPlaybackSource()
        }
    }

    val skipSwipeGesture = Modifier.skipSwipeGesture(
        pipelineOpen = overlays.audioPipeline,
        castOpen = overlays.cast,
        panelScrolling = panelScrolling,
        controlsLocked = controlsLocked,
        swipeThreshold = swipeThreshold,
        haptics = haptics,
        onBlockedControl = onBlockedControl,
        onNext = onNext,
        onPrevious = onPrevious,
        setSwipeOffset = setSwipeOffset,
    )

    val onScrub: (Float) -> Unit = scrub::drag
    val onScrubFinished: () -> Unit = {
        haptics.play(Haptic.Select)
        scrub.release(onSeekFraction)
    }
    val onVolumeChange: (Float) -> Unit = volume::drag
    val onVolumeChangeFinished: () -> Unit = volume::release

    val playerActions: @Composable () -> Unit = {
        PlayerActionRow(
            lyricsOpen = panels.lyricsOpen,
            queueOpen = panels.queueOpen,
            shuffleEnabled = shuffleEnabled,
            repeatMode = repeatMode,
            autoplayEnabled = autoplayEnabled,
            onToggleLyrics = panels.toggleLyrics,
            onLyricsOffset = { localLyricsOffsetOpen = true },
            onToggleQueue = panels.toggleQueue,
            onToggleShuffle = onToggleShuffle,
            onCycleRepeat = onCycleRepeat,
            onToggleAutoplay = onToggleAutoplay,
            onOpenOutput = openAudioOutput,
            onListenTogether = onListenTogether,
            onOpenListenTogetherMembers = overlays.openListenTogetherMembers,
            song = song,
            isLoading = isLoading,
            isModernOrVinyl = isWavePlay,
            onOpenPipeline = {
                if (song.isVideo || effectiveIsLive) {
                    overlays.videoQuality = true
                } else {
                    overlays.audioPipeline = true
                }
            },
            palette = synesthesiaPalette,
            animSpeed = animSpeed,
        )
        if (!isWavePlay) {
            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(20.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                OutputCaption(
                    accountName = accountName,
                    onOpenOutput = openAudioOutput,
                    onOpenMembers = overlays.openListenTogetherMembers,
                )
            }
        }
    }

    val openAutomixStudio: () -> Unit = { automixStudioOpen = true }
    val playerOverlays: @Composable () -> Unit = {
        LikeBurstOverlay(
            state = likeBurstState,
            modifier = Modifier.fillMaxSize(),
        )
        PlayerOverlays(
            overlays = overlays,
            playerHaze = playerHaze,
            accountName = accountName,
            accentColor = synesthesiaPalette.colors.firstOrNull(),
            isVideo = song.isVideo,
            isPlaying = isPlaying,
            onListenTogether = onListenTogether,
            lyricsSource = lyricsSource,
            lyricsProviderStates = lyricsProviderStates,
            onSelectLyricsProvider = onSelectLyricsProvider,
            lyricsOffsetOpen = effectiveLyricsOffsetOpen,
            onDismissLyricsOffset = dismissLyricsOffset,
        )
        if (automixStudioOpen) {
            AutomixStudioSheet(playerHaze, synesthesiaPalette.colors.firstOrNull()) { automixStudioOpen = false }
        }
        if (queueTools != null) {
            QueueToolDialogs(queueTools, overlays, synesthesiaPalette.colors.firstOrNull(), controlsLocked)
        }
    }

    val spotifyCanvasOpaque = spotifyCanvasPresentation && clipState.cover.floatValue >= 0.99f
    val landscape = landscapePlayerAvailable(windowWidth, windowHeight)
    val tabletArtworkBackdrop = tabletSizedPlayer(windowWidth)
    val legacyMeshBackdrop = !tabletArtworkBackdrop && !spotifyCanvasOpaque &&
        legacyMesh && !canvasFirstPortrait
    val fullCoverBackdropVisible = landscape ||
        (!legacyMeshBackdrop && (tabletArtworkBackdrop || panels.lyricsOpen || panels.queueOpen))
    val meshLiveFrames = if (
        spotifyCanvasPresentation || fullCoverBackdropVisible
    ) {
        null
    } else {
        clipState.frames.frames
    }
    val sheetSettled = dock?.settled ?: true
    val fullArtworkBlurImage = rememberFullArtworkBlurImage(
        imageUrl = remoteArt,
        artPx = artPx,
        prepare = sheetSettled || landscape || tabletArtworkBackdrop || panels.lyricsOpen || panels.queueOpen ||
            playerPrewarmStage >= 1,
    )
    val currentFullArtworkBlurImage = rememberUpdatedState(fullArtworkBlurImage)
    val currentLiveBlurFrames = rememberUpdatedState(
        if (fullCoverBackdropVisible) clipState.frames.frames else null,
    )
    val fullArtworkBlurContent = remember {
        movableContentOf<Modifier> { backdropModifier ->
            FullArtworkBlurBackdrop(
                image = currentFullArtworkBlurImage.value,
                liveFrames = currentLiveBlurFrames.value,
                modifier = backdropModifier,
            )
        }
    }

    val dockFrame = remember { DockFrame() }
    LaunchedEffect(landscape) {
        dockFrame.landscapeArt = null
        dockFrame.miniInBox = null
        dockFrame.anchor = null
        dockFrame.anchorFor = IntSize.Zero
        dockFrame.portalLeft = 0f
        dockFrame.portalTop = 0f
        dockFrame.portalScale = 1f
        measures.heroHeight = 0.dp
        measures.cardBottom = 0.dp
        measures.playerBounds = IntSize.Zero
        measures.spotifyDeckHeight = 0.dp
        measures.spotifyExpandedTopHeight = 0.dp
        lastControlSpread = 0.dp
    }
    val dockT: () -> Float = {
        if (dock == null) {
            1f
        } else {
            val open = dock.openFraction()
            if (dockFrame.anchor != null && dock.hasMiniArt()) open else 1f
        }
    }
    val docking: () -> Boolean = { dockT() < 1f }
    val dockMoving by remember { derivedStateOf { docking() } }
    LaunchedEffect(dockMoving) {
        if (dockMoving) likeBurstState.clear()
    }
    val dockFade: () -> Float = { smoothStep01((dockT() - DOCK_FADE_LEAD) / (1f - DOCK_FADE_LEAD)) }
    val sleeveLayer = rememberGraphicsLayer()

    val dockHost = modifier.playerDockHost(
        dock = dock,
        dockFrame = dockFrame,
        docking = docking,
        dockFade = dockFade,
        sleeveLayer = sleeveLayer,
    )

    if ((inPip && videoMode) || landscape) {
        CompositionLocalProvider(LocalOpenAutomixStudio provides openAutomixStudio, LocalQueueActions provides queueActions) {
            LandscapeAndVideoNowPlaying(
                inPip = inPip,
                videoMode = videoMode,
                landscape = landscape,
                videoRendered = videoRendered,
                onVideoRenderedChange = { videoRendered = it },
                art = art,
                coverPresentation = coverPresentation,
                song = song,
                isPlaying = isPlaying,
                isLoading = isLoading,
                position = position,
                durationMs = durationMs,
                isLive = effectiveIsLive,
                isSeekable = isSeekable,
                isAtLiveEdge = isAtLiveEdge,
                liveOffsetMs = liveOffsetMs,
                onJumpToLive = handleJumpToLive,
                hasPrevious = hasPrevious,
                pastRestartPoint = pastRestartPoint,
                hasNext = hasNext,
                controlsLocked = controlsLocked,
                onPrevious = onPrevious,
                onPlayPause = onPlayPause,
                onNext = onNext,
                shown = shown,
                mixing = mixing,
                scrub = scrub,
                volume = volume,
                onScrub = onScrub,
                onScrubFinished = onScrubFinished,
                onVolumeChange = onVolumeChange,
                onVolumeChangeFinished = onVolumeChangeFinished,
                onExitFullscreen = exitFullscreen,
                onOpenVideoQuality = { overlays.videoQuality = true },
                onOpenPipeline = {
                    if (song.isVideo || effectiveIsLive) overlays.videoQuality = true else overlays.audioPipeline = true
                },
                likeBurstState = likeBurstState,
                playerHaze = playerHaze,
                dockHost = dockHost,
                dock = dock,
                dockFrame = dockFrame,
                dockT = dockT,
                docking = docking,
                dockMoving = dockMoving,
                dockFade = dockFade,
                sleeveLayer = sleeveLayer,
                panels = panels,
                synesthesiaPalette = synesthesiaPalette,
                isWavePlay = isWavePlay,
                spotifyCanvasPresentation = spotifyCanvasPresentation,
                canvas = canvas,
                canvasRenderedState = clipState.rendered,
                onCanvasRenderedChange = { clipState.rendered.value = it },
                skipSwipeGesture = skipSwipeGesture,
                swipeSettle = swipeSettle.value,
                onOpenAlbum = onOpenAlbum,
                onOpenArtist = onOpenArtist,
                onOpenPlaybackSource = onOpenPlaybackSource,
                syncedLyricsEnabled = syncedLyricsEnabled,
                hideLyricsPreview = hideLyricsPreview,
                lyricsTranslation = lyricsTranslation,
                lyrics = lyrics,
                lyricsPlayhead = lyricsPlayhead,
                lyricsUnavailable = lyricsUnavailable,
                lyricsLoadingText = lyricsLoadingText,
                seekToLyric = seekToLyric,
                lyricsShareEnabled = lyricsShareEnabled,
                lyricPicker = lyricPicker,
                onShowLyricsProviders = { overlays.lyricsProviders = true },
                onOpenLyricsStyle = { overlays.lyricsStyle = true },
                queue = queue,
                queueIndex = queueIndex,
                autoplayEnabled = autoplayEnabled,
                onJumpTo = onJumpTo,
                onRemoveFromQueue = onRemoveFromQueue,
                onMoveInQueue = onMoveInQueue,
                onClearQueue = onClearQueue,
                onQueueDragActiveChange = onQueueDragActiveChange,
                onPlayNext = { onPlayNext?.invoke(it) },
                signedIn = signedIn,
                likeStatus = likeStatus,
                showRevertCue = showRevertCue,
                animatedCanvas = animatedCanvas,
                haptics = haptics,
                onToggleLike = onToggleLike,
                onAddToPlaylist = onAddToPlaylist,
                onOpenMenu = onOpenMenu,
                shuffleEnabled = shuffleEnabled,
                repeatMode = repeatMode,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeat = onCycleRepeat,
                onToggleAutoplay = onToggleAutoplay,
                audioVersionSwitching = audioVersionSwitching,
                hideVolumeBar = hideVolumeBar,
                playerActions = playerActions,
                playerOverlays = playerOverlays,
                modifier = Modifier,
            )
        }
        return
    }

    CompositionLocalProvider(
        LocalLikeBurstState provides likeBurstState,
        LocalPlayerHazeState provides playerHaze,
        LocalOpenAutomixStudio provides openAutomixStudio,
        LocalQueueActions provides queueActions,
    ) {
        Box(
            modifier = dockHost
                .fillMaxSize()
                .onSizeChanged { measures.playerBounds = it }
                .background(Color.Black),
        ) {
            PortraitBackdrop(
                legacyMesh = legacyMeshBackdrop,
                tablet = tabletArtworkBackdrop,
                panelOpen = panels.lyricsOpen || panels.queueOpen,
                collapseP = p,
                canvasFirstPortrait = canvasFirstPortrait,
                spotifyCanvasPresentation = spotifyCanvasOpaque,
                isWavePlay = isWavePlay,
                isPlaying = isPlaying,
                synesthesiaPalette = synesthesiaPalette,
                trackKey = song.videoId,
                imageUrl = remoteArt,
                artPx = artPx,
                meshLiveFrames = meshLiveFrames,
                seam = {
                    if (canvasFirstPortrait) {
                        renderedCanvasBottom
                    } else {
                        lerp(0.dp, measures.heroHeight + MESH_SEAM_NUDGE, heroT.value)
                    }
                },
                lift = { BACKDROP_LIFT * (1f - heroT.value) },
                liftFrom = { measures.cardBottom },
                fullArtworkBlur = fullArtworkBlurContent,
                playerHaze = playerHaze,
            )

            if (videoHeroMode && measures.heroHeight > 0.dp && !collapse.pastHalf) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(measures.heroHeight)
                        .align(Alignment.TopStart)
                        .then(if (reduceDynamicBlur) Modifier else Modifier.hazeSource(playerHaze))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { videoControlsVisible = !videoControlsVisible },
                ) {
                    if (!videoRendered) {
                        VideoLoadingBackdrop(
                            Modifier
                                .fillMaxSize()
                                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                .drawWithContent {
                                    drawContent()
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(Color.Black, Color.Transparent),
                                            startY = size.height * (1f - HERO_FADE_FRACTION),
                                            endY = size.height,
                                        ),
                                        blendMode = BlendMode.DstIn,
                                    )
                                },
                        )
                    }
                    MusicVideoSurface(
                        videoId = song.videoId,
                        fit = !videoFill,
                        bottomFade = HERO_FADE_FRACTION,
                        onRenderedChanged = { videoRendered = it },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            if (heroMode && measures.heroHeight > 0.dp) {
                heroClipMounted?.let { clip ->
                    CanvasArtworkPlayer(
                        canvas = clip,
                        isPlaying = isPlaying,
                        positionMs = if (scrub.scrubbing) (scrub.value * durationMs).toLong() else position.positionMs,
                        pausedForTransition = panels.lyricsOpen || panels.queueOpen || panels.queueDragging ||
                            collapse.started || dockMoving || !sheetSettled,
                        contentMode = if (clipFullscreen) {
                            CanvasContentMode.CROP
                        } else {
                            CanvasContentMode.FIT_PORTRAIT
                        },
                        alignPortraitTop = clipPortrait,
                        onAspectRatioChanged = { clipState.aspect = it },
                        portraitRevealBounds = measures.playerBounds,
                        presentationAlpha = { (1f - 2f * p()).coerceIn(0f, 1f) * dockFade() },
                        onRenderedChanged = { clipState.rendered.value = it },
                        onFrameCaptured = { frame ->
                            if (!clipFullscreen && !tabletArtworkBackdrop &&
                                !panels.lyricsOpen && !panels.queueOpen && !panels.queueDragging
                            ) {
                                clipState.frames.offer(frame)
                            }
                        },
                        refreshFrameEveryMs = if (
                            song.isVideo || clipFullscreen || tabletArtworkBackdrop ||
                            panels.lyricsOpen || panels.queueOpen || panels.queueDragging ||
                            dockMoving || !sheetSettled
                        ) {
                            null
                        } else {
                            liveRefreshMs
                        },
                        onCoverChanged = { clipState.cover.floatValue = it },
                        underStillCover = true,
                        bottomFade = when {
                            clipFullscreen -> 0f
                            clipPortrait -> clipFadeFraction
                            else -> HERO_FADE_FRACTION
                        },
                        bottomFadeEndPx = if (clipPortrait) with(density) { clipBottom.toPx() } else null,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .then(
                                if (clipFullscreen || clipPortrait) Modifier.fillMaxSize()
                                else Modifier.fillMaxWidth().height(measures.heroHeight),
                            )
                            .then(if (reduceDynamicBlur) Modifier else Modifier.hazeSource(playerHaze)),
                    )
                }
            }

            val playerSubviewOpen = panels.lyricsOpen || panels.queueOpen || effectiveLyricsOffsetOpen ||
                overlays.audioPipeline || overlays.videoQuality || overlays.cast || overlays.audioOutput ||
                overlays.lyricsProviders || overlays.lyricsShare != null
            val heroScrimAlpha = artworkStatusScrimAlpha * heroT.value
            val targetTopGradientAlpha = if (playerSubviewOpen) {
                maxOf(heroScrimAlpha, SUBVIEW_STATUS_SCRIM_MIN_ALPHA)
            } else {
                heroScrimAlpha
            }
            val topGradientAlpha by animateFloatAsState(
                targetValue = targetTopGradientAlpha,
                animationSpec = tween(durationMillis = 360, easing = DreamyEaseOutEasing),
                label = "topGradientAlpha",
            )
            val topScrimBrush = remember(topGradientAlpha) {
                val steps = 8
                Brush.verticalGradient(
                    List(steps) { index ->
                        val progress = index / (steps - 1).toFloat()
                        val factor = (1f - progress).toDouble().pow(1.5).toFloat()
                        Color.Black.copy(alpha = topGradientAlpha * factor)
                    },
                )
            }

            if (canvasFirstPortrait && clipState.rendered.value) {
                Box(
                    Modifier.matchParentSize()
                        .graphicsLayer { alpha = clipState.cover.floatValue }
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.40f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.70f),
                            ),
                        ),
                )
            }

            SpotifyCanvasDeckScrim(visible = spotifyCanvasPresentation && clipState.spotifyControlsOpen)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .toggleSpotifyCanvasControlsOnTap(
                        enabled = spotifyCanvasPresentation && !mixing,
                        onToggle = { clipState.spotifyControlsOpen = !clipState.spotifyControlsOpen },
                    )
                    .then(skipSwipeGesture),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                NowPlayingTopBar(
                    topStrip = topStrip,
                    statusBarTop = statusBarTop,
                    topScrimBrush = topScrimBrush,
                    isWavePlay = isWavePlay,
                    spotifyChromeAlpha = spotifyChromeAlpha,
                    p = p,
                    collapseAlmostDone = collapse.almostDone,
                    onOpenPlaybackSource = onOpenPlaybackSource,
                    topBarGlyphTint = topBarGlyphTint,
                    hideSongStatus = hideSongStatus,
                    song = song,
                    playedBy = playedBy,
                    openPlaybackOrigin = openPlaybackOrigin,
                    animatedCanvas = animatedCanvas,
                    isTopBright = darkTopBarIcons,
                    onToggleAnimatedCanvas = {
                        haptics.play(Haptic.Select)
                        PlayerSettings.setAnimatedCanvas(!animatedCanvas)
                    },
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .onGloballyPositioned { measures.dismissBandSpace = it }
                        .pointerInput(overlays.audioPipeline, overlays.cast, panelScrolling) {
                            if (overlays.audioPipeline || overlays.cast || panelScrolling) return@pointerInput
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val space = measures.dismissBandSpace
                                val y = space?.localToRoot(down.position)?.y ?: down.position.y
                                val panelUp = panels.queueOpen || panels.lyricsOpen || panels.queueSlide.floatValue > 0.01f
                                val bandTop: Float
                                val bandBottom: Float
                                if (panelUp) {
                                    bandTop = space?.positionInRoot()?.y ?: 0f
                                    bandBottom = bandTop + (ART_BOX_TOP_PAD + HEADER_HEIGHT).toPx()
                                } else {
                                    bandTop = measures.dismissBandTop
                                    bandBottom = measures.dismissBandBottom
                                }
                                if (y >= bandTop && y <= bandBottom) {
                                    if (!panelUp) {
                                        dragQueueIn(
                                            down = down,
                                            travel = bandBottom - bandTop - HEADER_HEIGHT.toPx(),
                                            slide = panels.queueSlide,
                                            onHold = { panels.queueDragging = it },
                                            onSettle = { open ->
                                                if (open != panels.queueOpen) {
                                                    haptics.play(if (open) Haptic.Expand else Haptic.Tap)
                                                    if (open) panels.queueControlsOpen = true
                                                    panels.queueOpen = open
                                                }
                                                panels.queueReleased++
                                            },
                                        )
                                    }
                                    return@awaitEachGesture
                                }
                                if (down.isConsumed) return@awaitEachGesture
                                val drag = awaitVerticalTouchSlopOrCancellation(down.id) { change, _ ->
                                    change.consume()
                                }
                                if (drag != null) verticalDrag(drag.id) { it.consume() }
                            }
                        }
                        .padding(horizontal = PLAYER_GUTTER),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val versionAligning by PlayerSettings.versionAlignmentInProgress.collectAsStateWithLifecycle()
                    val versionSwitching = versionAligning || audioVersionSwitching
                    var controlSpread by remember { mutableStateOf(lastControlSpread) }

                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(max = PLAYER_MAX_WIDTH)
                            .fillMaxWidth()
                            .padding(top = ART_BOX_TOP_PAD, bottom = 18.dp),
                    ) {
                        if (spotifyCanvasPresentation && playerDeckSettledOpen &&
                            maxHeight != measures.spotifyExpandedTopHeight
                        ) {
                            SideEffect { measures.spotifyExpandedTopHeight = maxHeight }
                        }
                        val roomy = maxHeight + controlSpread
                        val wantArt = minOf(maxWidth, roomy - ART_TITLE_GAP - HEADER_HEIGHT)
                        val fullArt = minOf(wantArt, maxHeight - ART_TITLE_GAP - HEADER_HEIGHT)
                            .coerceAtLeast(THUMB_SIZE)
                        val slack = (roomy - wantArt - ART_TITLE_GAP - HEADER_HEIGHT).coerceAtLeast(0.dp)
                        val controlDeckIsMeasured = !spotifyCanvasPresentation || playerDeckSettledOpen
                        if (!panels.lyricsOpen && !panels.queueOpen && !panels.queueDragging && collapse.atRest && controlDeckIsMeasured) {
                            val target = with(density) {
                                val half = slack.coerceAtMost(CONTROL_GAP_SPREAD_MAX * 2).toPx().div(2f).roundToInt()
                                (half * 2).toDp()
                            }
                            val granted = with(density) {
                                val steppedPx = (controlSpread.toPx() + (target.toPx() - controlSpread.toPx()) * 0.4f).roundToInt()
                                steppedPx.toDp()
                            }
                            if (granted != controlSpread) {
                                SideEffect {
                                    controlSpread = granted
                                    lastControlSpread = granted
                                }
                            }
                        }
                        val groupTop = (maxHeight - fullArt - ART_TITLE_GAP - HEADER_HEIGHT).coerceAtLeast(0.dp) / 2
                        fun artSize(): Dp = lerp(fullArt, THUMB_SIZE, p())
                        fun artTop(): Dp = lerp(groupTop, 0.dp, p())
                        fun artStart(): Dp = lerp((maxWidth - fullArt) / 2, 0.dp, p())
                        fun regularTitleTop(): Dp = lerp(groupTop + fullArt + ART_TITLE_GAP, 0.dp, p())
                        val lyricsBottomLift = if (fullscreenLyricsStyle == com.music.bitchord.data.settings.FullscreenArtworkLyricsStyle.PLAYER_STRIP && syncedLyricsEnabled && !hideLyricsPreview) {
                            56.dp + maxOf(navBarBottom, 20.dp)
                        } else {
                            maxOf(navBarBottom, 12.dp)
                        }
                        val spotifyTitleRange = if (spotifyCanvasPresentation) {
                            val targetFullHeight = if (measures.spotifyDeckHeight > 0.dp && measures.spotifyExpandedTopHeight > 0.dp) {
                                maxOf(maxHeight, measures.spotifyExpandedTopHeight + measures.spotifyDeckHeight)
                            } else {
                                maxHeight
                            }
                            (targetFullHeight - HEADER_HEIGHT - lyricsBottomLift).coerceAtLeast(0.dp)
                        } else {
                            null
                        }
                        fun currentTitleTop(): Dp {
                            if (spotifyCanvasPresentation && playerDeckReveal.value <= 0.001f) {
                                return (maxHeight - HEADER_HEIGHT - lyricsBottomLift).coerceAtLeast(0.dp)
                            }
                            return spotifyTitleRange?.let { collapsedTitleTop ->
                                lerp(collapsedTitleTop, regularTitleTop(), playerDeckReveal.value)
                            } ?: regularTitleTop()
                        }
                        val thumbWidth = if (videoMode) THUMB_SIZE * (16f / 9f) else THUMB_SIZE
                        fun titleStart(): Dp = lerp(0.dp, thumbWidth + 12.dp, p())
                        val bannerBottom = statusBarTop + topStrip + ART_BOX_TOP_PAD + groupTop + fullArt + ART_TITLE_GAP / 2
                        val cardBottom = statusBarTop + topStrip + ART_BOX_TOP_PAD + groupTop + fullArt
                        val bannerSettled = !panels.lyricsOpen || measures.heroHeight == 0.dp
                        if (bannerSettled && (bannerBottom != measures.heroHeight || cardBottom != measures.cardBottom)) {
                            SideEffect {
                                measures.heroHeight = bannerBottom
                                measures.cardBottom = cardBottom
                            }
                        }

                        val effectiveHeroHeight = if (measures.heroHeight > 0.dp) measures.heroHeight else bannerBottom
                        val effectivePlayerBounds = if (measures.playerBounds.width > 0 && measures.playerBounds.height > 0) {
                            measures.playerBounds
                        } else {
                            with(density) {
                                IntSize(
                                    windowWidth.roundToPx().coerceAtLeast(1),
                                    windowHeight.roundToPx().coerceAtLeast(1),
                                )
                            }
                        }
                        val bannerTopPx = with(density) {
                            -(statusBarTop.roundToPx() + topStrip.roundToPx() + ART_BOX_TOP_PAD.roundToPx())
                        }
                        val boxWidthPx = constraints.maxWidth
                        fun bannerShare(): Float =
                            if (effectiveHeroHeight > 0.dp && effectivePlayerBounds.width > 0) heroT.value else 0f
                        fun bannerShown(): Float = bannerShare() * (1f - p())
                        fun cardScale(): Float = if (videoMode) 1f else artScale + (1f - artScale) * p()

                        PortraitSleeve(
                            song = song,
                            art = art,
                            coverPresentation = coverPresentation,
                            clipState = clipState,
                            canvas = canvas,
                            heroClip = heroClip,
                            heroMode = heroMode,
                            tabletArtworkBackdrop = tabletArtworkBackdrop,
                            panels = panels,
                            collapse = collapse,
                            isPlaying = isPlaying,
                            position = position,
                            durationMs = durationMs,
                            scrub = scrub,
                            dock = dock,
                            dockFrame = dockFrame,
                            dockT = dockT,
                            docking = docking,
                            dockMoving = dockMoving,
                            sleeveLayer = sleeveLayer,
                            playerHaze = playerHaze,
                            density = density,
                            playerBounds = effectivePlayerBounds,
                            heroHeight = effectiveHeroHeight,
                            boxWidthPx = boxWidthPx,
                            bannerTopPx = bannerTopPx,
                            maxWidth = maxWidth,
                            maxHeight = maxHeight,
                            fullArt = fullArt,
                            groupTop = groupTop,
                            thumbSize = THUMB_SIZE,
                            headerHeight = HEADER_HEIGHT,
                            artTitleGap = ART_TITLE_GAP,
                            thumbWidth = thumbWidth,
                            bannerShare = ::bannerShare,
                            bannerShown = ::bannerShown,
                            cardScale = ::cardScale,
                            artSize = ::artSize,
                            artTop = ::artTop,
                            artStart = ::artStart,
                            currentTitleTop = ::currentTitleTop,
                            titleStart = ::titleStart,
                            isModernAmbient = isModernAmbient,
                            enableBreathingAura = enableBreathingAura,
                            isVinylRecord = isVinylRecord,
                            videoMode = videoMode,
                            videoHeroMode = videoHeroMode,
                            videoRendered = videoRendered,
                            onVideoRenderedChange = { videoRendered = it },
                            videoAspect = videoAspectState.value,
                            videoFill = videoFill,
                            videoFullBleed = videoFullBleed,
                            videoControlsVisible = videoControlsVisible,
                            videoControlsAlpha = videoControlsAlpha,
                            onToggleVideoControls = { videoControlsVisible = !videoControlsVisible },
                            onToggleFullscreen = {
                                fullscreenRequested = true
                                screenOrientation.landscape()
                            },
                            onOpenVideoQuality = { overlays.videoQuality = true },
                            swipeSettle = swipeSettle.value,
                            swipeThreshold = swipeThreshold,
                            swipeHintShown = swipeHintShown,
                            swipeHintNext = swipeHintNext,
                            hasPrevious = hasPrevious,
                            hasNext = hasNext,
                            spotifyCanvasPresentation = spotifyCanvasPresentation,
                            spotifyChromeAlpha = spotifyChromeAlpha,
                            syncedLyricsEnabled = syncedLyricsEnabled,
                            hideLyricsPreview = hideLyricsPreview,
                            fullscreenLyricsStyle = fullscreenLyricsStyle,
                            lyricsTranslation = lyricsTranslation,
                            lyricsPlayhead = lyricsPlayhead,
                            lyricsUnavailable = lyricsUnavailable,
                            lyricsLoadingText = lyricsLoadingText,
                            deckRevealValue = { playerDeckReveal.value },
                            meshRefreshMs = if (song.isVideo) null else liveRefreshMs,
                            onMeshFrameCaptured = { clipState.frames.offer(it) },
                            onDismissBandTop = { measures.dismissBandTop = it },
                            heroFadeFraction = HERO_FADE_FRACTION,
                            onToggleFullBleedArtwork = if (!canvasActiveOnPlayer && !videoMode && tapArtworkToToggleFullScreen && !panels.lyricsOpen && !panels.queueOpen) {
                                {
                                    haptics.play(Haptic.Tap)
                                    PlayerSettings.setFullBleedArtwork(!fullBleedArt)
                                }
                            } else null,
                        )

                        PortraitCredits(
                            song = song,
                            isWavePlay = isWavePlay,
                            isModernOrVinyl = isModernOrVinyl,
                            signedIn = signedIn,
                            likeStatus = likeStatus,
                            showRevertCue = showRevertCue,
                            currentTitleTop = ::currentTitleTop,
                            titleStart = ::titleStart,
                            p = p,
                            scrolls = {
                                !(panels.lyricsOpen || panels.queueOpen || panels.queueDragging) && !collapse.pastSettling
                            },
                            headerHeight = HEADER_HEIGHT,
                            thumbSize = THUMB_SIZE,
                            playerHaze = playerHaze,
                            haptics = haptics,
                            likeBurstState = likeBurstState,
                            onToggleLike = onToggleLike,
                            onAddToPlaylist = onAddToPlaylist,
                            onOpenMenu = onOpenMenu,
                            onOpenAlbum = onOpenAlbum,
                            onOpenArtist = onOpenArtist,
                            onDismissBandBottom = { measures.dismissBandBottom = it },
                        )

                        val lyricsPanelVisible = panels.lyricsOpen && collapse.done
                        if (lyricsPanelVisible || playerPrewarmStage >= 2) {
                            PortraitLyricsPane(
                                visible = lyricsPanelVisible,
                                fade = { panelFade.value },
                                panels = panels,
                                lyrics = lyrics,
                                lyricsTranslation = lyricsTranslation,
                                song = song,
                                lyricsPlayhead = lyricsPlayhead,
                                lyricsUnavailable = lyricsUnavailable,
                                isPlaying = isPlaying,
                                position = position,
                                onSeekToLine = seekToLyric,
                                canPick = lyricsShareEnabled,
                                lyricPicker = lyricPicker,
                            )
                        }

                        val queuePanelVisible = !panels.lyricsOpen &&
                            (panels.queueDragging || (panels.queueOpen && collapse.queueShowing && collapse.done))
                        if (queuePanelVisible || playerPrewarmStage >= 3) {
                            PortraitQueuePane(
                                visible = queuePanelVisible,
                                fade = { panelFade.value },
                                panels = panels,
                                queue = queue,
                                queueIndex = queueIndex,
                                autoplayEnabled = autoplayEnabled,
                                controlsLocked = controlsLocked,
                                onJumpTo = onJumpTo,
                                onRemoveFromQueue = onRemoveFromQueue,
                                onMoveInQueue = onMoveInQueue,
                                onClearQueue = onClearQueue,
                                onQueueDragActiveChange = onQueueDragActiveChange,
                                onPlayNext = { onPlayNext?.invoke(it) },
                            )
                        }
                    }

                    SlidingPlayerDeck(
                        visible = !lyricPicker.picking &&
                            (!panels.lyricsOpen || panels.lyricsControlsOpen) &&
                            (!panels.queueOpen || panels.queueControlsOpen) &&
                            (!spotifyCanvasPresentation || clipState.spotifyControlsOpen || mixing),
                        reveal = playerDeckReveal,
                        modifier = if (panels.queueOpen) Modifier.pointerInput(panels) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false, pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                                panels.queueTransportActivity++
                            }
                        } else Modifier,
                        fadeIn = { !panels.lyricsOpen && !panels.queueOpen && p() > 0f },
                    ) {
                        PortraitControlDeck(
                            panels = panels,
                            spotifyCanvasPresentation = spotifyCanvasPresentation,
                            onSpotifyDeckHeight = {
                                if (it != measures.spotifyDeckHeight) measures.spotifyDeckHeight = it
                            },
                            syncedLyricsEnabled = syncedLyricsEnabled,
                            hideLyricsPreview = hideLyricsPreview,
                            lyricsTranslation = lyricsTranslation,
                            hasLyrics = !lyrics.isNullOrEmpty(),
                            song = song,
                            lyricsPlayhead = lyricsPlayhead,
                            isPlaying = isPlaying,
                            position = position,
                            durationMs = durationMs,
                            isLive = effectiveIsLive,
                            isSeekable = isSeekable,
                            isAtLiveEdge = isAtLiveEdge,
                            liveOffsetMs = liveOffsetMs,
                            onJumpToLive = handleJumpToLive,
                            lyricsUnavailable = lyricsUnavailable,
                            lyricsLoadingText = lyricsLoadingText,
                            pickingLyrics = lyricPicker.picking,
                            onShowLyricsProviders = { overlays.lyricsProviders = true },
                            onOpenLyricsStyle = { overlays.lyricsStyle = true },
                            isWavePlay = isWavePlay,
                            synesthesiaPalette = synesthesiaPalette,
                            shown = shown,
                            versionSwitching = versionSwitching,
                            mixing = mixing,
                            scrub = scrub,
                            onScrub = onScrub,
                            onScrubFinished = onScrubFinished,
                            isLoading = isLoading,
                            controlSpread = controlSpread,
                            audioVersionSwitching = audioVersionSwitching,
                            controlsLocked = controlsLocked,
                            hasPrevious = hasPrevious,
                            pastRestartPoint = pastRestartPoint,
                            hasNext = hasNext,
                            onPrevious = onPrevious,
                            onPlayPause = onPlayPause,
                            onNext = onNext,
                            shuffleEnabled = shuffleEnabled,
                            repeatMode = repeatMode,
                            onToggleShuffle = onToggleShuffle,
                            onCycleRepeat = onCycleRepeat,
                            playerHaze = playerHaze,
                            hideVolumeBar = hideVolumeBar,
                            volume = volume,
                            onVolumeChange = onVolumeChange,
                            onVolumeChangeFinished = onVolumeChangeFinished,
                            playerActions = playerActions,
                        )
                    }
                }
            }

            if (videoHeroMode && measures.heroHeight > 0.dp && !collapse.pastHalf &&
                !panels.lyricsOpen && !panels.queueOpen && (videoControlsVisible || videoControlsAlpha > 0f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(measures.heroHeight)
                        .align(Alignment.TopStart),
                ) {
                    VideoControlsDock(
                        videoFill = videoFill,
                        onToggleVideoFill = { PlayerSettings.setVideoFill(!videoFill) },
                        videoFullBleed = videoFullBleed,
                        onToggleVideoFullBleed = { PlayerSettings.setVideoFullBleed(!videoFullBleed) },
                        fullscreen = false,
                        onToggleFullscreen = {
                            fullscreenRequested = true
                            screenOrientation.landscape()
                        },
                        onOpenQuality = { overlays.videoQuality = true },
                        onEnterPip = { PlayerPlatform.host.enterPip() },
                        pipSupported = PlayerPlatform.host.pipSupported,
                        showFullBleedToggle = true,
                        hazeState = null,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .graphicsLayer { alpha = videoControlsAlpha }
                            .padding(end = 16.dp, bottom = 14.dp),
                    )
                }
            }
            playerOverlays()
        }
    }
}

@Composable
private fun rememberPlayerArtworkPalette(remoteArt: String?, clipState: CanvasClipState): MeshPalette {
    // Live blur frames can arrive more often than the artwork palette needs to change.
    var paletteFrame by remember(clipState) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(clipState) {
        var lastAt = 0L
        clipState.frames.frames.collect { frame ->
            if (frame == null) return@collect
            val now = System.nanoTime()
            if (lastAt == 0L || now - lastAt >= LIVE_REFRESH_MS * 1_000_000L) {
                lastAt = now
                paletteFrame = frame
            }
        }
    }
    return rememberArtworkColors(remoteArt, paletteFrame)
}
