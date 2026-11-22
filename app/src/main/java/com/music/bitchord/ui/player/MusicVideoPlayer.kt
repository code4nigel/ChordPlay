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
package com.music.bitchord.ui.player

import android.content.Context
import android.graphics.SurfaceTexture
import android.net.ConnectivityManager
import android.os.Build
import android.view.TextureView
import android.view.ViewGroup
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import com.metrolist.innertubex.extraction.StreamResolveException
import com.metrolist.innertubex.sabr.findSabrProtocolException
import com.music.bitchord.data.Http
import com.music.bitchord.data.TrackLog
import com.music.bitchord.data.innertube.InnerTubeXResolver
import com.music.bitchord.data.innertube.PlayerClient
import com.music.bitchord.data.innertube.StreamResolver
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.playback.HttpRangeVideoStreamSource
import com.music.bitchord.playback.SabrVideoStreamSource
import com.music.bitchord.playback.SeekableVideoDataSource
import com.music.bitchord.playback.SeekableVideoSource
import com.music.bitchord.playback.toSong
import com.music.bitchord.ui.rememberIsForeground
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A music video's picture, the way the web player's videoDeck plays one: a
 * second ExoPlayer, video only and silent, held to the main player's clock.
 *
 * The audio is untouched — it stays in [com.music.bitchord.playback.PlaybackService]
 * with everything that runs on it (the cache, the EQ, crossfade, Listen
 * Together) — and stays the clock. The picture follows it: its rate nudged a
 * few percent for small drift, a seek for a large one. It streams only while a
 * surface wants it, so a music video playing with the screen off or the player
 * shut costs no more data than the song.
 *
 * Surfaces take turns holding the one decoder, the last one mounted in front —
 * the full player over the mini player, the landscape player over either — as
 * only one of them is ever on screen at a time.
 */
@OptIn(UnstableApi::class)
object MusicVideoEngine {

    private const val TAG = "MusicVideo"

    /** Past this the picture jumps to the audio; under it, its rate is nudged. */
    private const val SEEK_DRIFT_MS = 1_500L
    private const val IGNORE_DRIFT_MS = 25L
    private const val SYNC_EVERY_MS = 250L

    /** No second seek inside this, while the first is still landing. */
    private const val SEEK_COOLDOWN_MS = 4_500L

    /** Switch to next client/fallback if ExoPlayer stalls buffering without progressing. */
    private const val STALL_TIMEOUT_MS = 6_000L
    private const val LIVE_STALL_TIMEOUT_MS = 12_000L
    private const val LIVE_SEEK_DRIFT_MS = 2_500L
    private const val LIVE_IGNORE_DRIFT_MS = 150L

    /** How long the picture outlives its last surface: a rotation, a sheet reopened. */
    private const val RELEASE_GRACE_MS = 4_000L

    /** Media3 extractor check interval: 64 KiB matches Metrolist's fast backpressure cadence. */
    private const val CONTINUE_LOADING_CHECK_BYTES = 65_536

    /** A googlevideo URL lives about six hours; a resolved stream is reused well inside that. */
    private const val STREAM_TTL_MS = 60 * 60 * 1000L

    private val scope = MainScope()

    private var controller: Player? = null
    private var player: ExoPlayer? = null
    private var bandwidthMeter: DefaultBandwidthMeter? = null
    private var appContext: Context? = null
    private var activeVideoSource: SeekableVideoSource? = null
    private class Surface(val view: TextureView, val priority: Int, val videoId: String)

    private val surfaces = ArrayList<Surface>()

    /** The surface in front: the highest priority, the latest of equals. */
    private fun topSurface(): Surface? = surfaces.lastOrNull { s -> surfaces.none { it.priority > s.priority } }
    private fun top(): TextureView? = topSurface()?.view

    /** Video explicitly switched to by the user even when `preferMusicOnly` is enabled. */
    @Volatile
    private var explicitVideoOverrideId: String? = null

    fun setExplicitVideoOverride(videoId: String?) {
        if (explicitVideoOverrideId == videoId) return
        explicitVideoOverrideId = videoId
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            refresh()
        } else {
            scope.launch { refresh() }
        }
    }

    fun isExplicitVideoOverride(videoId: String?): Boolean =
        !videoId.isNullOrBlank() && explicitVideoOverrideId == videoId

    /** The video loaded (or loading) into [player]. */
    private var loadedId: String? = null
    private var loadJob: Job? = null
    private var syncJob: Job? = null
    private var releaseJob: Job? = null
    private var lastSeekAt = 0L
    private var seekStartedAt = 0L
    private var bufferingStartedAt = 0L
    private var lastBufferedPos = 0L

    /** How far ahead of the audio a seek is aimed: what the last one took to land. */
    private var seekLeadMs = 250L

    /** Resolved streams by video: the stream, the height it was asked at, and when. */
    private val streams = LinkedHashMap<String, Triple<InnerTubeXResolver.VideoStream, Int, Long>>()

    /**
     * Tracks with no picture to show, so a track change back to one doesn't ask
     * again — until the session changes, which can open an age gate.
     */
    private val noPicture = HashSet<String>()
    private val _noPictureIds = MutableStateFlow<Set<String>>(emptySet())
    val noPictureIds: StateFlow<Set<String>> = _noPictureIds.asStateFlow()
    private val fallbackUsed = HashSet<String>()
    private var loadedStream: InnerTubeXResolver.VideoStream? = null
    private var isCurrentStreamLive: Boolean = false

    private fun isLiveTrack(videoId: String): Boolean {
        val song = controller?.currentMediaItem?.toSong()?.takeIf { it.videoId == videoId }
        if (song?.isLive == true) return true
        if (controller?.isCurrentMediaItemLive == true && controller?.currentMediaItem?.mediaId == videoId) {
            return true
        }
        return StreamResolver.isKnownLive(videoId)
    }

    private fun applyVideoTrackSelection(exo: ExoPlayer, capHeight: Int?, exactPin: Boolean = false) {
        val builder = exo.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
        if (builder is DefaultTrackSelector.Parameters.Builder) {
            builder.setExceedVideoConstraintsIfNecessary(true)
        }
        builder.clearOverridesOfType(C.TRACK_TYPE_VIDEO)
        if (capHeight != null && capHeight > 0 && capHeight < Int.MAX_VALUE) {
            var matchedOverride: TrackSelectionOverride? = null
            if (exactPin) {
                outer@ for (group in exo.currentTracks.groups) {
                    if (group.type != C.TRACK_TYPE_VIDEO) continue
                    for (i in 0 until group.length) {
                        if (group.getTrackFormat(i).height == capHeight && group.isTrackSupported(i)) {
                            matchedOverride = TrackSelectionOverride(group.mediaTrackGroup, i)
                            break@outer
                        }
                    }
                }
            }
            if (matchedOverride != null) {
                builder.addOverride(matchedOverride)
                builder.clearVideoSizeConstraints()
            } else {
                builder.setMaxVideoSize(Int.MAX_VALUE, capHeight)
            }
        } else {
            builder.clearVideoSizeConstraints()
        }
        exo.trackSelectionParameters = builder.build()
    }

    fun isPictureless(videoId: String): Boolean = videoId in _noPictureIds.value

    private fun markNoPicture(videoId: String) {
        if (noPicture.add(videoId)) {
            _noPictureIds.value = noPicture.toSet()
        }
    }

    private fun clearNoPicture(videoId: String) {
        if (noPicture.remove(videoId)) {
            _noPictureIds.value = noPicture.toSet()
        }
    }

    private fun clearAllNoPicture() {
        if (noPicture.isNotEmpty()) {
            noPicture.clear()
            _noPictureIds.value = emptySet()
        }
    }

    /** Videos the age gate stopped, asked as age-restricted from the start. */
    private val ageGated = HashSet<String>()

    /** Clients whose picture googlevideo refused mid-playback, by video. */
    private val refused = HashMap<String, MutableSet<String>>()

    /** The client that minted the loaded picture, for [refused]. */
    private var loadedProfile: String? = null

    init {
        // Signing in is what opens an age gate, so every "no picture" verdict
        // and every URL minted for the old session is owed a fresh ask.
        scope.launch {
            InnerTubeXResolver.sessionGeneration.drop(1).collect {
                clearAllNoPicture()
                fallbackUsed.clear()
                streams.clear()
                refused.clear()
                refresh()
            }
        }
        scope.launch {
            AppSettings.preferMusicOnly.drop(1).collect { refresh() }
        }
        scope.launch {
            AppSettings.videoQuality.drop(1).collect {
                prefs?.edit()?.remove(KEY_QUALITY)?.apply()
                _quality.value = _quality.value.copy(held = null)
                val id = loadedId ?: return@collect
                if (loadedStream?.isHls == true) {
                    val cap = liveTrackCapFor(id)
                    loadedCap = cap ?: 0
                    player?.let { applyVideoTrackSelection(it, cap, exactPin = _quality.value.held != null) }
                    TrackLog.d(TAG, "video: switched HLS preference cap to ${cap?.let { "${it}p" } ?: "Auto"} in-place", about = id)
                    return@collect
                }
                streams.remove(id)
                load(id)
            }
        }
    }

    private val _shownVideoId = MutableStateFlow<String?>(null)

    /** The video whose frames are on screen now, once its first one has landed. */
    val shownVideoId: StateFlow<String?> = _shownVideoId.asStateFlow()

    private val _frameOn = MutableStateFlow<TextureView?>(null)

    /** The surface the last frame was drawn to. */
    internal val frameOn: StateFlow<TextureView?> = _frameOn.asStateFlow()

    private val _quality = MutableStateFlow(MusicVideoQualityUi())

    /** The heights the loaded video offers, the held one, and the one on screen. */
    val quality: StateFlow<MusicVideoQualityUi> = _quality.asStateFlow()

    private var prefs: android.content.SharedPreferences? = null

    /** What the loaded stream was asked for, so a failed high one can step down once. */
    private var loadedCap = 0

    /** Videos whose held height wouldn't decode here, played at [AUTO_HEIGHT] instead. */
    private val stepDown = HashSet<String>()

    private val _playing = MutableStateFlow(false)

    /** Whether the audio the picture follows is playing — what keeps the screen awake. */
    val playing: StateFlow<Boolean> = _playing.asStateFlow()

    private val _aspect = MutableStateFlow(16f / 9f)

    /** The picture's width over its height. */
    val aspect: StateFlow<Float> = _aspect.asStateFlow()

    private val controllerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.mediaId?.let { id ->
                clearNoPicture(id)
                fallbackUsed.remove(id)
            }
            refresh()
        }
        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) = refresh()
        override fun onTimelineChanged(timeline: Timeline, reason: Int) = refresh()
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playing.value = isPlaying
            sync()
        }
        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) = sync()
        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            // A seek on the audio: follow it at once rather than on the next tick.
            lastSeekAt = 0L
            sync()
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onRenderedFirstFrame() {
            TrackLog.d(TAG, "video: rendered first frame", about = loadedId)
            _frameOn.value = top()
            _shownVideoId.value = loadedId
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            val width = videoSize.width * videoSize.pixelWidthHeightRatio
            if (width > 0f && videoSize.height > 0) {
                TrackLog.d(TAG, "video: size ${width.roundToInt()}x${videoSize.height}", about = loadedId)
                _aspect.value = width / videoSize.height
                _quality.value = _quality.value.copy(activeHeight = videoSize.height)
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val stateName = when (playbackState) {
                Player.STATE_IDLE -> "IDLE"
                Player.STATE_BUFFERING -> "BUFFERING"
                Player.STATE_READY -> "READY"
                Player.STATE_ENDED -> "ENDED"
                else -> playbackState.toString()
            }
            TrackLog.d(TAG, "video: state=$stateName (playWhenReady=${player?.playWhenReady})", about = loadedId)
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    if (bufferingStartedAt == 0L) bufferingStartedAt = System.currentTimeMillis()
                }
                Player.STATE_READY -> {
                    bufferingStartedAt = 0L
                    lastBufferedPos = 0L
                    if (seekStartedAt > 0L) {
                        val took = System.currentTimeMillis() - seekStartedAt
                        seekLeadMs = ((seekLeadMs + took) / 2).coerceIn(0L, 1_500L)
                        seekStartedAt = 0L
                    }
                }
                else -> {
                    bufferingStartedAt = 0L
                    lastBufferedPos = 0L
                }
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            if (!isCurrentStreamLive && loadedStream?.isHls != true) return
            val heights = mutableSetOf<Int>()
            var activeH: Int? = null
            for (group in tracks.groups) {
                if (group.type != C.TRACK_TYPE_VIDEO) continue
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    if (format.height > 0) {
                        heights += format.height
                        if (group.isTrackSelected(i)) {
                            activeH = format.height
                        }
                    }
                }
            }
            if (heights.isNotEmpty()) {
                _quality.value = _quality.value.copy(
                    heights = heights.sortedDescending(),
                    activeHeight = activeH ?: _quality.value.activeHeight,
                )
            }
            // If user picked a specific height before manifest tracks finished loading,
            // promote the cap to an exact TrackSelectionOverride once that height appears.
            val heldHeight = _quality.value.held
            val exo = player
            if (heldHeight != null && exo != null && loadedStream?.isHls == true) {
                val hasOverride = exo.trackSelectionParameters.overrides.values.any {
                    it.type == C.TRACK_TYPE_VIDEO
                }
                if (!hasOverride && heldHeight in heights && activeH != heldHeight) {
                    applyVideoTrackSelection(exo, heldHeight, exactPin = true)
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val id = loadedId ?: return
            if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                TrackLog.w(TAG, "video: behind live window for $id; jumping to live edge", about = id)
                player?.seekToDefaultPosition()
                player?.prepare()
                return
            }
            TrackLog.w(TAG, "video: playback failed at ${loadedCap}p via ${loadedProfile ?: "?"} (${error.errorCodeName}: ${error.message})", about = id)
            streams.remove(id)
            if (isCurrentStreamLive) {
                val httpCode = generateSequence<Throwable>(error) { it.cause }
                    .filterIsInstance<HttpDataSource.InvalidResponseCodeException>()
                    .firstOrNull()
                    ?.responseCode
                if (httpCode != null) {
                    StreamResolver.onLivePlaybackRefused(id, loadedStream?.url, httpCode)
                } else {
                    StreamResolver.invalidate(id)
                }
            }
            // If NewPipe provided a 360p MP4 fallback URL, try that before dropping clients.
            val fallbackUrl = loadedStream?.fallbackUrl
            if (fallbackUrl != null && fallbackUsed.add("$id:fallback")) {
                TrackLog.w(TAG, "video: retrying playback via 360p MP4 fallbackUrl", about = id)
                playStream(
                    id = id,
                    stream = InnerTubeXResolver.VideoStream(
                        videoId = id,
                        url = fallbackUrl,
                        width = 640,
                        height = 360,
                        mimeType = "video/mp4",
                        headers = loadedStream?.headers.orEmpty(),
                        heights = loadedStream?.heights.orEmpty(),
                        profileId = loadedProfile ?: "fallback",
                    ),
                )
                return
            }
            // googlevideo/SABR turned this client away: the next client in
            // InnerTubeX's catalog gets a turn.
            val profile = loadedProfile
            if (profile != null && profile != "newpipe" && profile != "live-hls" &&
                (isRefusal(error) || error.findSabrProtocolException() != null)
            ) {
                val skip = refused.getOrPut(id) { HashSet() }
                if (skip.add(profile) && skip.size <= MAX_REFUSED_CLIENTS) {
                    TrackLog.w(TAG, "video: $profile refused/failed; asking the next client", about = id)
                    InnerTubeXResolver.onVideoRefused()
                    load(id)
                    return
                }
            }
            if (profile != "newpipe" && fallbackUsed.add("$id:newpipe-error")) {
                TrackLog.w(TAG, "video: retrying via NewPipe/HLS fallback after player error", about = id)
                load(id)
                return
            }
            // Above 1080p YouTube serves VP9 and AV1, which not every phone
            // decodes: one more try on the H.264 ladder before giving up.
            if (loadedCap > AUTO_HEIGHT && stepDown.add(id)) {
                load(id)
                return
            }
            markNoPicture(id)
            unload()
        }
    }

    /** The app's MediaController, whose position the picture follows. Main thread. */
    fun attach(controller: Player?) {
        if (this.controller === controller) return
        this.controller?.removeListener(controllerListener)
        this.controller = controller
        controller?.addListener(controllerListener)
        _playing.value = controller?.isPlaying == true
        refresh()
    }

    /**
     * Puts the picture on [view] until the returned release is called, unless
     * a surface of higher [priority] holds it — the full player's outranks the
     * mini player's, whichever was mounted last. Main thread.
     */
    internal fun claim(view: TextureView, priority: Int, videoId: String): () -> Unit {
        releaseJob?.cancel()
        val wantedSong = controller?.currentMediaItem?.toSong()
        TrackLog.d(
            TAG,
            "video: surface claim priority=$priority surfaceVideoId=$videoId controllerSong=${wantedSong?.videoId} isVideo=${wantedSong?.isVideo}",
            about = videoId.ifBlank { wantedSong?.videoId },
        )
        if (videoId.isNotBlank()) {
            clearNoPicture(videoId)
        }
        if (wantedSong?.isVideo == true || wantedSong?.isLive == true) {
            clearNoPicture(wantedSong.videoId)
            if (wantedSong.isLive) StreamResolver.markKnownLive(wantedSong.videoId)
        }
        surfaces.removeAll { it.view === view }
        surfaces += Surface(view, priority, videoId)
        val app = view.context.applicationContext
        if (prefs == null) {
            prefs = app.getSharedPreferences("music_video", Context.MODE_PRIVATE).also { stored ->
                val held = stored.getInt(KEY_QUALITY, 0).takeIf { it > 0 }
                _quality.value = _quality.value.copy(held = held)
            }
        }
        val exo = ensurePlayer(app)
        if (top() === view) {
            _frameOn.value = null
            attachSurface(view)
            repaint()
        }
        refresh()
        return release@{
            val wasTop = top() === view
            surfaces.removeAll { it.view === view }
            if (!wasTop) return@release
            if (_frameOn.value === view) _frameOn.value = null
            val next = top()
            if (next != null) {
                attachSurface(next)
                repaint()
            } else {
                exo.clearVideoTextureView(view)
                releaseJob = scope.launch {
                    delay(RELEASE_GRACE_MS)
                    if (surfaces.isEmpty()) unload()
                }
            }
        }
    }

    private fun attachSurface(view: TextureView) {
        val exo = player ?: return
        exo.setVideoTextureView(view)
        val delegate = view.surfaceTextureListener
        if (delegate is GuardedSurfaceListener) return
        view.surfaceTextureListener = GuardedSurfaceListener(view, delegate)
    }

    private class GuardedSurfaceListener(
        private val view: TextureView,
        private val delegate: TextureView.SurfaceTextureListener?,
    ) : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            delegate?.onSurfaceTextureAvailable(surface, width, height)
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            delegate?.onSurfaceTextureSizeChanged(surface, width, height)
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            if (_frameOn.value === view) _frameOn.value = null
            return delegate?.onSurfaceTextureDestroyed(surface) ?: true
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            delegate?.onSurfaceTextureUpdated(surface)
            val exo = player
            val currentId = loadedId
            if (top() === view && currentId != null && exo?.playbackState == Player.STATE_READY) {
                if (_frameOn.value !== view) _frameOn.value = view
                if (_shownVideoId.value != currentId) _shownVideoId.value = currentId
            }
        }
    }

    private fun ensurePlayer(context: Context): ExoPlayer {
        val app = context.applicationContext
        appContext = app
        val meter = bandwidthMeter ?: DefaultBandwidthMeter.Builder(app).build().also { bandwidthMeter = it }
        return player ?: ExoPlayer.Builder(app)
            .setBandwidthMeter(meter)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(10_000, 30_000, 1_000, 2_000)
                    .setTargetBufferBytes(16 * 1024 * 1024)
                    .setPrioritizeTimeOverSizeThresholds(true)
                    .build(),
            )
            .build()
            .apply {
                volume = 0f
                trackSelectionParameters = trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                    .build()
                addListener(playerListener)
            }
            .also { player = it }
    }

    /** A paused decoder handed a new surface draws nothing until asked to. */
    private fun repaint() {
        val exo = player ?: return
        if (!exo.playWhenReady && exo.playbackState != Player.STATE_IDLE) {
            exo.seekTo(exo.currentPosition)
        }
    }

    private var lastIgnoredAudioId: String? = null

    /** The videoId the picture should be of: the controller's or the active surface's. */
    private fun wantedVideoId(): String? {
        val surfaceId = topSurface()?.videoId?.takeIf { it.isNotBlank() }
        val song = controller?.currentMediaItem?.toSong()
        val live = song?.isLive == true || (surfaceId != null && StreamResolver.isKnownLive(surfaceId))
        val explicitOverride = isExplicitVideoOverride(surfaceId) || isExplicitVideoOverride(song?.videoId)
        if (AppSettings.preferMusicOnly.value && !live && !explicitOverride) {
            val skipId = surfaceId ?: song?.videoId
            TrackLog.d(TAG, "video: skipped $skipId (preferMusicOnly=true)", about = skipId)
            return null
        }
        val id = when {
            song?.isVideo == true || song?.isLive == true -> song.videoId
            surfaceId != null -> surfaceId
            else -> {
                if (song != null && surfaces.isNotEmpty() && lastIgnoredAudioId != song.videoId) {
                    lastIgnoredAudioId = song.videoId
                    TrackLog.d(TAG, "video: wanted=null (${song.videoId} isVideo=false in controller and no surface videoId)", about = song.videoId)
                } else if (song == null) {
                    TrackLog.d(TAG, "video: wanted=null (no active MediaItem, hasController=${controller != null})")
                }
                return null
            }
        }
        if (id in noPicture) {
            TrackLog.d(TAG, "video: skipped $id (in noPicture)", about = id)
            return null
        }
        return id
    }

    private fun refresh() {
        val videoId = if (surfaces.isEmpty()) null else wantedVideoId()
        if (videoId == null) {
            // The picture is kept through the grace period after its last
            // surface goes; a track change to one with none drops it now.
            if (surfaces.isNotEmpty() || wantedVideoId() != loadedId) unload()
            return
        }
        if (videoId == loadedId) {
            startSync()
            return
        }
        load(videoId)
    }

    /**
     * Holds the picture at [height] from now on, or lets it choose with null —
     * the web player's quality box. The playing video reloads at once, at the
     * moment the audio is at.
     */
    fun setQuality(height: Int?) {
        prefs?.edit()?.apply {
            if (height == null) remove(KEY_QUALITY) else putInt(KEY_QUALITY, height)
        }?.apply()
        _quality.value = _quality.value.copy(held = height)
        val id = loadedId ?: return
        if (loadedStream?.isHls == true) {
            val cap = liveTrackCapFor(id)
            loadedCap = cap ?: 0
            player?.let { applyVideoTrackSelection(it, cap, exactPin = height != null) }
            TrackLog.d(TAG, "video: switched HLS quality cap to ${cap?.let { "${it}p" } ?: "Auto"} in-place", about = id)
            return
        }
        streams.remove(id)
        stepDown.remove(id)
        load(id)
    }

    private fun estimatedDownstreamBps(): Long {
        val meterBps = bandwidthMeter?.bitrateEstimate?.takeIf { it > 0L && it != Long.MAX_VALUE }
        if (meterBps != null) return meterBps
        val ctx = appContext ?: return -1L
        return runCatching {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val caps = cm?.activeNetwork?.let { cm.getNetworkCapabilities(it) }
            val kbps = caps?.linkDownstreamBandwidthKbps ?: 0
            if (kbps > 0) kbps * 1000L else -1L
        }.getOrDefault(-1L)
    }

    private fun autoNetworkHeightCap(): Int {
        if (AppSettings.meteredConnection.value == true) return METERED_HEIGHT
        val bps = estimatedDownstreamBps()
        return when {
            bps <= 0L -> AUTO_HEIGHT
            bps >= 6_000_000L -> AUTO_HEIGHT
            bps >= 2_500_000L -> 720
            bps >= 1_100_000L -> 480
            else -> 360
        }
    }

    /** The height asked for: the held one, else network-aware Auto / active VideoQuality cap (clamped on metered). */
    private fun capFor(videoId: String): Int {
        val metered = AppSettings.meteredConnection.value == true
        val pref = AppSettings.videoQuality.value
        val prefCap = if (pref.isAuto) autoNetworkHeightCap() else pref.maxHeight
        val auto = if (metered) minOf(prefCap, METERED_HEIGHT) else prefCap
        val held = _quality.value.held ?: return auto
        return if (videoId in stepDown) minOf(held, AUTO_HEIGHT) else held
    }

    /**
     * Track-selection ceiling for adaptive HLS streams: null removes height
     * constraints so ExoPlayer ABR adapts freely to network speed when Auto is active.
     */
    private fun liveTrackCapFor(videoId: String): Int? {
        if (_quality.value.held != null) return capFor(videoId)
        val pref = AppSettings.videoQuality.value
        if (pref.isAuto) {
            return if (AppSettings.meteredConnection.value == true) METERED_HEIGHT else null
        }
        return capFor(videoId)
    }

    /** A refusal from googlevideo itself, as opposed to a decoder or network failure. */
    private fun isRefusal(error: PlaybackException): Boolean =
        generateSequence<Throwable>(error) { it.cause }
            .filterIsInstance<HttpDataSource.InvalidResponseCodeException>()
            .any { it.responseCode in REFUSAL_CODES }

    private fun StreamResolver.ResolvedVideoStream.toVideoStream(videoId: String): InnerTubeXResolver.VideoStream {
        val assumedWidth = if (height > 0) (height * 16f / 9f).roundToInt() else 0
        return InnerTubeXResolver.VideoStream(
            videoId = videoId,
            url = url,
            width = assumedWidth,
            height = height,
            mimeType = mimeType,
            headers = StreamResolver.mediaHeadersFor(url),
            heights = heights.ifEmpty { listOfNotNull(height.takeIf { it > 0 }) },
            profileId = if (isLive) "live-hls" else "newpipe",
            isHls = isHls,
            fallbackUrl = fallbackUrl,
            isLive = isLive,
        )
    }

    private fun isSquareOrPortraitArt(stream: InnerTubeXResolver.VideoStream): Boolean =
        stream.width > 0 && stream.height > 0 && stream.aspect < MIN_VIDEO_ASPECT

    /**
     * [videoId]'s picture: resolves live streams directly via HLS, and VOD tracks
     * through InnerTubeX (SABR or direct), age-restricted InnerTubeX, NewPipe, or HLS.
     */
    private suspend fun resolve(videoId: String, cap: Int): InnerTubeXResolver.VideoStream? {
        val skip = refused[videoId].orEmpty().toMutableSet()
        var lastError: StreamResolveException? = null
        val forceFallback = "$videoId:newpipe-error" in fallbackUsed
        val liveTrack = isLiveTrack(videoId)

        suspend fun tryInnerTubeX(
            ageRestricted: Boolean,
            allowHls: Boolean,
            allowSabr: Boolean,
            isLive: Boolean = liveTrack,
        ): InnerTubeXResolver.VideoStream? {
            val candidate = try {
                withTimeoutOrNull(10_000L) {
                    withContext(Dispatchers.IO + TrackLog.about(videoId)) {
                        InnerTubeXResolver.extractVideo(
                            videoId = videoId,
                            maxHeight = cap,
                            skipClients = skip,
                            ageRestricted = ageRestricted,
                            allowHls = allowHls,
                            allowSabr = allowSabr,
                            isLive = isLive,
                        )
                    }
                }
            } catch (e: StreamResolveException) {
                lastError = e
                return null
            } ?: return null

            return candidate
        }

        if (liveTrack) {
            val sharedLive = StreamResolver.resolveLiveVideoStream(videoId, forceRefresh = forceFallback)
                ?.toVideoStream(videoId)
            if (sharedLive != null) {
                TrackLog.d(
                    TAG,
                    "video: resolved live stream via shared live HLS (heights=${sharedLive.heights})",
                    about = videoId,
                )
                return sharedLive
            }
            TrackLog.d(TAG, "video: resolving live stream via NewPipe HLS fallback (cap=${cap}p)", about = videoId)
            val newPipeLive = runCatching {
                StreamResolver.resolveVideoStream(videoId, cap, isLive = true)?.toVideoStream(videoId)
            }.onFailure { err ->
                TrackLog.w(TAG, "video: NewPipe live fallback threw: ${err.message}", about = videoId)
            }.getOrNull()
            if (newPipeLive != null) return newPipeLive
            if (lastError != null && lastError!!.reason in ESCALATED_REASONS) throw lastError!!
            return null
        }

        if (!forceFallback) {
            if (videoId !in ageGated) {
                val plain = tryInnerTubeX(ageRestricted = false, allowHls = false, allowSabr = true)
                if (plain != null) return plain
                TrackLog.d(TAG, "video: plain InnerTubeX exhausted; trying age-restricted", about = videoId)
            }
            val restricted = tryInnerTubeX(ageRestricted = true, allowHls = false, allowSabr = true)
                ?.also { ageGated += videoId }
            if (restricted != null) return restricted
        }

        TrackLog.d(TAG, "video: falling back to NewPipe video extractor (cap=${cap}p)", about = videoId)
        val newPipe = runCatching {
            StreamResolver.resolveVideoStream(videoId, cap)?.toVideoStream(videoId)
        }.onFailure { err ->
            TrackLog.w(TAG, "video: NewPipe fallback threw: ${err.message}", about = videoId)
        }.getOrNull()
        if (newPipe != null) {
            val useFallbackUrl = "$videoId:newpipe-primary-error" in fallbackUsed
            val fbUrl = newPipe.fallbackUrl
            if (useFallbackUrl && fbUrl != null) {
                TrackLog.d(TAG, "video: retrying via NewPipe 360p fallback", about = videoId)
                return InnerTubeXResolver.VideoStream(
                    videoId = videoId,
                    url = fbUrl,
                    width = 640,
                    height = 360,
                    mimeType = "video/mp4",
                    headers = PlayerClient.forStreamUrl(fbUrl).mediaHeaders(),
                    heights = newPipe.heights,
                    profileId = "newpipe",
                )
            }
            TrackLog.d(TAG, "video: resolved via NewPipe (${newPipe.height}p ${newPipe.mimeType})", about = videoId)
            return newPipe
        }

        TrackLog.d(TAG, "video: falling back to InnerTubeX HLS (cap=${cap}p)", about = videoId)
        val hls = tryInnerTubeX(ageRestricted = videoId in ageGated, allowHls = true, allowSabr = false)
        if (hls != null) return hls
        if (lastError != null && lastError!!.reason in ESCALATED_REASONS) throw lastError!!
        return null
    }

    private fun playStream(id: String, stream: InnerTubeXResolver.VideoStream) {
        val targetView = top()
        val exo = player ?: targetView?.let { target ->
            ensurePlayer(target.context.applicationContext).also { attachSurface(target) }
        } ?: run {
            TrackLog.w(TAG, "video: playStream skipped (no ExoPlayer/surface)", about = id)
            return
        }
        activeVideoSource?.closeAsync()
        activeVideoSource = null
        loadedStream = stream
        loadedProfile = stream.profileId
        val isLive = stream.isLive || isLiveTrack(id)
        isCurrentStreamLive = isLive
        if (isLive) StreamResolver.markKnownLive(id)
        applyVideoTrackSelection(
            exo,
            if (stream.isHls) liveTrackCapFor(id) else null,
            exactPin = _quality.value.held != null,
        )

        val start = if (isLive) 0L else controller?.currentPosition?.coerceAtLeast(0L) ?: 0L
        val initialPos = if (isLive || start < 300L) 0L else start + seekLeadMs

        val mediaItem = MediaItem.Builder()
            .setUri(stream.url)
            .apply {
                val baseMime = stream.mimeType.substringBefore(';').trim()
                if (baseMime.isNotBlank()) setMimeType(baseMime)
            }
            .build()

        val sabrBootstrap = stream.sabrBootstrap
        val source = when {
            stream.isHls -> {
                val hlsHeaders = StreamResolver.mediaHeadersFor(stream.url) + stream.headers
                val upstream = OkHttpDataSource.Factory(Http.client)
                    .setDefaultRequestProperties(hlsHeaders)
                HlsMediaSource.Factory(upstream).createMediaSource(mediaItem)
            }
            sabrBootstrap != null -> {
                val cacheDir = (targetView?.context?.applicationContext ?: appContext)?.cacheDir
                if (cacheDir == null) {
                    TrackLog.w(TAG, "video: missing cacheDir for SABR video source", about = id)
                    return
                }
                val sabrSource = SabrVideoStreamSource(
                    mediaSize = stream.contentLengthBytes,
                    streamFactory = { initialPlayerTimeMs, playbackPositionMs ->
                        InnerTubeXResolver.openSabrVideoChunks(
                            bootstrap = sabrBootstrap,
                            initialPlayerTimeMs = initialPlayerTimeMs,
                            playbackPositionMs = playbackPositionMs,
                        )
                    },
                    expectedDurationMs = sabrBootstrap.durationMs ?: controller?.duration?.takeIf { it > 0L },
                    tempDirectory = cacheDir,
                )
                sabrSource.updatePlaybackPosition(initialPos)
                activeVideoSource = sabrSource
                ProgressiveMediaSource.Factory(SeekableVideoDataSource.Factory(sabrSource))
                    .setContinueLoadingCheckIntervalBytes(CONTINUE_LOADING_CHECK_BYTES)
                    .createMediaSource(mediaItem)
            }
            else -> {
                val rangeSource = HttpRangeVideoStreamSource(
                    url = stream.url,
                    requestHeaders = stream.headers,
                    mediaSize = stream.contentLengthBytes,
                    chunkSize = HttpRangeVideoStreamSource.chunkSizeFor(
                        url = stream.url,
                        bitrate = stream.bitrate,
                        rangeChunkSizeBytes = stream.rangeChunkSizeBytes,
                    ),
                )
                activeVideoSource = rangeSource
                ProgressiveMediaSource.Factory(SeekableVideoDataSource.Factory(rangeSource))
                    .setContinueLoadingCheckIntervalBytes(CONTINUE_LOADING_CHECK_BYTES)
                    .createMediaSource(mediaItem)
            }
        }

        if (stream.aspect > 0f) _aspect.value = stream.aspect
        _quality.value = _quality.value.copy(heights = stream.heights, activeHeight = stream.height.takeIf { it > 0 })
        TrackLog.d(
            TAG,
            "video: playStream profile=${stream.profileId} ${stream.width}x${stream.height} mime=${stream.mimeType} sabr=${stream.isSabr} hls=${stream.isHls} live=$isLive start=${if (isLive) "LIVE" else "${start}ms"}",
            about = id,
        )
        bufferingStartedAt = System.currentTimeMillis()
        lastBufferedPos = if (isLive) 0L else initialPos
        if (isLive) {
            exo.setMediaSource(source)
        } else {
            exo.setMediaSource(source, initialPos)
        }
        exo.playWhenReady = controller?.isPlaying == true
        exo.prepare()
        lastSeekAt = System.currentTimeMillis()
        seekStartedAt = lastSeekAt
        startSync()
    }

    private fun load(videoId: String) {
        unload()
        loadedId = videoId
        val cap = capFor(videoId)
        loadedCap = cap
        TrackLog.d(TAG, "video: loading picture (cap=${cap}p)", about = videoId)
        loadJob = scope.launch(TrackLog.about(videoId)) {
            val stream = try {
                cachedStream(videoId, cap)
                    ?: resolve(videoId, cap)?.also { streams[videoId] = Triple(it, cap, System.currentTimeMillis()) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: StreamResolveException) {
                TrackLog.w(TAG, "video: no picture (${e.reason}): ${e.message}", about = videoId)
                if (loadedId != videoId) return@launch
                loadedId = null
                // Settled until something changes it: the age gate by a sign-in
                // (which clears [noPicture]), the rest not at all. A network
                // failure is asked again on the next surface or track change.
                if (e.reason != StreamResolveException.Reason.NETWORK) markNoPicture(videoId)
                return@launch
            } catch (e: Exception) {
                // Cleared on next surface claim or track transition so future
                // attempts still retry, while dropping the spinner right now.
                TrackLog.w(TAG, "video: couldn't resolve the picture: ${e.message}", about = videoId)
                markNoPicture(videoId)
                if (loadedId == videoId) loadedId = null
                return@launch
            }
            if (loadedId != videoId) return@launch
            if (stream == null) {
                TrackLog.w(TAG, "video: no stream returned by InnerTubeX or NewPipe", about = videoId)
                markNoPicture(videoId)
                loadedId = null
                return@launch
            }
            // A catalogue track's "video" is its cover held still; nothing to show.
            if (isSquareOrPortraitArt(stream)) {
                TrackLog.w(TAG, "video: rejected non-widescreen dimensions ${stream.width}x${stream.height}", about = videoId)
                markNoPicture(videoId)
                loadedId = null
                return@launch
            }
            playStream(videoId, stream)
        }
    }

    private fun cachedStream(videoId: String, cap: Int): InnerTubeXResolver.VideoStream? {
        val (stream, askedFor, at) = streams[videoId] ?: return null
        if (askedFor != cap || System.currentTimeMillis() - at > STREAM_TTL_MS) {
            streams.remove(videoId)
            return null
        }
        return stream
    }

    private fun unload() {
        loadJob?.cancel()
        loadJob = null
        syncJob?.cancel()
        syncJob = null
        loadedId = null
        loadedStream = null
        loadedProfile = null
        isCurrentStreamLive = false
        bufferingStartedAt = 0L
        lastBufferedPos = 0L
        activeVideoSource?.closeAsync()
        activeVideoSource = null
        _shownVideoId.value = null
        _frameOn.value = null
        _quality.value = _quality.value.copy(heights = emptyList(), activeHeight = null)
        player?.let {
            it.stop()
            it.clearMediaItems()
        }
        while (streams.size > 8) streams.remove(streams.keys.first())
    }

    private fun startSync() {
        if (syncJob?.isActive == true) return
        syncJob = scope.launch {
            while (isActive) {
                sync()
                delay(SYNC_EVERY_MS)
            }
        }
    }

    /** Holds the picture to the audio: same play state, same rate, the same moment. */
    private fun sync() {
        val audio = controller ?: return
        val exo = player ?: return
        if (loadedId == null || exo.playbackState == Player.STATE_IDLE || exo.playbackState == Player.STATE_ENDED) return
        if (isCurrentStreamLive || audio.isCurrentMediaItemLive || exo.isCurrentMediaItemLive) {
            isCurrentStreamLive = true
            syncLive(audio, exo)
            return
        }
        val speed = audio.playbackParameters.speed
        val audioPos = audio.currentPosition
        activeVideoSource?.updatePlaybackPosition(audioPos)
        val now = System.currentTimeMillis()
        if (!audio.isPlaying) {
            exo.playWhenReady = false
            setSpeed(exo, speed)
            // Paused: the exact frame, so a scrub shows where it lands.
            if (abs(exo.currentPosition - audioPos) > 40L && now - lastSeekAt > 120L) seek(exo, audioPos)
            return
        }
        exo.playWhenReady = true
        if (exo.playbackState == Player.STATE_BUFFERING) {
            val buffered = exo.bufferedPosition
            if (buffered > lastBufferedPos + 64L) {
                lastBufferedPos = buffered
                bufferingStartedAt = now
            } else if (bufferingStartedAt == 0L) {
                bufferingStartedAt = now
            } else if (now - bufferingStartedAt > STALL_TIMEOUT_MS) {
                val id = loadedId ?: return
                val profile = loadedProfile
                TrackLog.w(TAG, "video: stalled buffering for ${now - bufferingStartedAt}ms via ${profile ?: "?"}; failing over", about = id)
                streams.remove(id)
                if (profile != null && profile != "newpipe") {
                    val skip = refused.getOrPut(id) { HashSet() }
                    if (skip.add(profile) && skip.size <= MAX_REFUSED_CLIENTS) {
                        InnerTubeXResolver.onVideoRefused()
                        load(id)
                        return
                    }
                }
                if (profile != "newpipe" && fallbackUsed.add("$id:newpipe-error")) {
                    load(id)
                    return
                }
                if (loadedCap > AUTO_HEIGHT && stepDown.add(id)) {
                    load(id)
                    return
                }
                bufferingStartedAt = now
            }
            return
        }
        val drift = exo.currentPosition - audioPos
        if (abs(drift) > SEEK_DRIFT_MS) {
            if (now - lastSeekAt > SEEK_COOLDOWN_MS) seek(exo, audioPos + seekLeadMs)
            return
        }
        // Catch up (or wait) smoothly by speed nudging instead of jarring re-seeks.
        val nudge = if (abs(drift) > IGNORE_DRIFT_MS) (-drift / 1000f).coerceIn(-0.18f, 0.18f) else 0f
        setSpeed(exo, speed * (1f + nudge))
    }

    /**
     * Synchronizes a live HLS video stream with the audio live stream using
     * absolute window timestamps (or live-edge offsets) rather than raw VOD positions.
     */
    private fun syncLive(audio: Player, exo: ExoPlayer) {
        val speed = audio.playbackParameters.speed
        val now = System.currentTimeMillis()
        val driftMs = computeLiveDriftMs(audio, exo)

        if (!audio.isPlaying) {
            exo.playWhenReady = false
            setSpeed(exo, speed)
            if (audio.playbackState == Player.STATE_READY &&
                driftMs != null && abs(driftMs) > 300L && now - lastSeekAt > 350L && exo.isCurrentMediaItemSeekable
            ) {
                val target = (exo.currentPosition + driftMs).coerceIn(0L, exo.duration.coerceAtLeast(0L))
                lastSeekAt = now
                exo.seekTo(target)
            }
            return
        }

        exo.playWhenReady = true
        if (exo.playbackState == Player.STATE_BUFFERING) {
            val buffered = exo.bufferedPosition
            if (buffered > lastBufferedPos + 64L) {
                lastBufferedPos = buffered
                bufferingStartedAt = now
            } else if (bufferingStartedAt == 0L) {
                bufferingStartedAt = now
            } else if (now - bufferingStartedAt > LIVE_STALL_TIMEOUT_MS) {
                val id = loadedId ?: return
                TrackLog.w(TAG, "video: live stream stalled for ${now - bufferingStartedAt}ms; resetting to live edge", about = id)
                bufferingStartedAt = now
                if ("$id:live-resync" !in fallbackUsed && fallbackUsed.add("$id:live-resync")) {
                    exo.seekToDefaultPosition()
                    exo.prepare()
                    return
                }
                streams.remove(id)
                load(id)
                return
            }
            return
        }

        if (driftMs == null) {
            setSpeed(exo, speed)
            return
        }

        // Positive driftMs means video is behind audio and needs to jump/speed up.
        if (abs(driftMs) > LIVE_SEEK_DRIFT_MS) {
            if (now - lastSeekAt > 2_000L) {
                lastSeekAt = now
                val audioOffset = audio.currentLiveOffset
                if (audioOffset != C.TIME_UNSET && audioOffset < 5_000L) {
                    exo.seekToDefaultPosition()
                } else if (exo.isCurrentMediaItemSeekable) {
                    val target = (exo.currentPosition + driftMs).coerceIn(0L, exo.duration.coerceAtLeast(0L))
                    exo.seekTo(target)
                }
            }
            return
        }

        val nudge = if (abs(driftMs) > LIVE_IGNORE_DRIFT_MS) {
            (driftMs / 4_000f).coerceIn(-0.06f, 0.06f)
        } else {
            0f
        }
        setSpeed(exo, speed * (1f + nudge))
    }

    /**
     * Positive result means video is behind audio by N ms; negative means ahead.
     */
    private fun computeLiveDriftMs(audio: Player, exo: ExoPlayer): Long? {
        val audioWindow = Timeline.Window()
        val videoWindow = Timeline.Window()
        val hasAudioWindow = !audio.currentTimeline.isEmpty &&
            audio.currentMediaItemIndex in 0 until audio.currentTimeline.windowCount
        val hasVideoWindow = !exo.currentTimeline.isEmpty &&
            exo.currentMediaItemIndex in 0 until exo.currentTimeline.windowCount

        if (hasAudioWindow && hasVideoWindow) {
            audio.currentTimeline.getWindow(audio.currentMediaItemIndex, audioWindow)
            exo.currentTimeline.getWindow(exo.currentMediaItemIndex, videoWindow)
            if (audioWindow.windowStartTimeMs != C.TIME_UNSET &&
                videoWindow.windowStartTimeMs != C.TIME_UNSET
            ) {
                val audioAbs = audioWindow.windowStartTimeMs + audio.currentPosition
                val videoAbs = videoWindow.windowStartTimeMs + exo.currentPosition
                return audioAbs - videoAbs
            }
        }
        val audioOffset = audio.currentLiveOffset
        val videoOffset = exo.currentLiveOffset
        if (audioOffset != C.TIME_UNSET && videoOffset != C.TIME_UNSET) {
            return videoOffset - audioOffset
        }
        return null
    }

    private fun seek(exo: ExoPlayer, positionMs: Long) {
        val targetMs = positionMs.coerceAtLeast(0L)
        lastSeekAt = System.currentTimeMillis()
        seekStartedAt = lastSeekAt
        activeVideoSource?.beginPlaybackSeek(targetMs)
        exo.seekTo(targetMs)
        activeVideoSource?.endPlaybackSeek(targetMs)
    }

    private fun setSpeed(exo: ExoPlayer, speed: Float) {
        if (abs(exo.playbackParameters.speed - speed) > 0.005f) {
            exo.playbackParameters = PlaybackParameters(speed)
        }
    }

    /** Narrower than this is a cover held still, not a picture worth showing. */
    private const val MIN_VIDEO_ASPECT = 1.2f

    /** Where Auto opens, as on the web player: the top of YouTube's H.264 ladder. */
    private const val AUTO_HEIGHT = 1080
    private const val METERED_HEIGHT = 480
    private const val KEY_QUALITY = "quality"

    /** The same as the audio's: googlevideo's ways of refusing a URL. */
    private val REFUSAL_CODES = setOf(403, 404, 410)

    /** How many refused clients a picture is worth before it's given up — the audio's three. */
    private const val MAX_REFUSED_CLIENTS = 3

    /**
     * What the plain ask fails with when the age gate is why: InnerTubeX names
     * the gate only when no client answered at all, and otherwise reports the
     * clients that answered without a picture as no playable stream.
     */
    private val ESCALATED_REASONS = setOf(
        StreamResolveException.Reason.AGE_RESTRICTED,
        StreamResolveException.Reason.NO_PLAYABLE_STREAM,
    )
}

/**
 * Where the playing music video shows: [videoId]'s picture, fitted into or
 * cropped to these bounds. Shows nothing until a frame of that video has
 * landed here, so a caller keeps its still artwork underneath and the picture
 * fades in over it. Holds the picture only while the app is on screen.
 */
@OptIn(UnstableApi::class)
@Composable
internal fun AndroidMusicVideo(
    videoId: String,
    fit: Boolean,
    onRenderedChanged: (Boolean) -> Unit,
    modifier: Modifier,
    /** Which surface wins when two are mounted: the full player's, over the mini player's. */
    priority: Int = 1,
    bottomFade: Float = 0f,
) {
    val inPip by PlayerPlatform.host.inPictureInPicture.collectAsStateWithLifecycle()
    val foreground = rememberIsForeground(allowStarted = true || inPip)
    var view by remember { mutableStateOf<TextureView?>(null) }
    var bounds by remember { mutableStateOf(IntSize.Zero) }
    DisposableEffect(view, foreground, priority, videoId) {
        val target = view
        val release = if (target != null && foreground) {
            MusicVideoEngine.claim(target, priority, videoId)
        } else {
            null
        }
        onDispose { release?.invoke() }
    }

    val shown by MusicVideoEngine.shownVideoId.collectAsStateWithLifecycle()
    val frameOn by MusicVideoEngine.frameOn.collectAsStateWithLifecycle()
    val aspect by MusicVideoEngine.aspect.collectAsStateWithLifecycle()
    val rendered = view != null && frameOn === view && shown == videoId
    val playing by MusicVideoEngine.playing.collectAsStateWithLifecycle()

    // A video being watched keeps the screen awake, as any video player does —
    // only while it plays, and only on the surface its frames are reaching.
    val awake = rendered && playing
    LaunchedEffect(view, awake) { view?.keepScreenOn = awake }

    val report by rememberUpdatedState(onRenderedChanged)
    LaunchedEffect(rendered) { report(rendered) }
    DisposableEffect(Unit) { onDispose { report(false) } }

    val alpha by animateFloatAsState(
        targetValue = if (rendered) 1f else 0f,
        animationSpec = tween(durationMillis = 320),
        label = "musicVideoAlpha",
    )

    Box(modifier.clipToBounds(), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { context ->
                TextureView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    // Blend rather than punch a hole: the still art stays
                    // visible underneath for the length of the fade.
                    isOpaque = false
                    this.alpha = 0f
                    view = this
                }
            },
            update = { texture ->
                if (view !== texture) view = texture
                texture.alpha = alpha
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val effectiveBounds = if (bounds.width > 0 && bounds.height > 0) {
                        bounds
                    } else {
                        IntSize(texture.width, texture.height)
                    }
                    if (bottomFade > 0.001f && effectiveBounds.width > 0 && effectiveBounds.height > 0) {
                        texture.setBottomFade(bottomFade, effectiveBounds, null)
                    } else {
                        texture.setRenderEffect(null)
                    }
                }
            },
            onRelease = { texture ->
                if (view === texture) view = null
            },
            // The picture at its own shape: letterboxed into the bounds, or
            // overflowing them and cropped by the clip above.
            modifier = Modifier
                .onSizeChanged { bounds = it }
                .layout { measurable, constraints ->
                val width = constraints.maxWidth
                val height = if (constraints.hasBoundedHeight) {
                    constraints.maxHeight
                } else {
                    (width / aspect).roundToInt()
                }
                val boundsAspect = width.toFloat() / height.coerceAtLeast(1)
                val wider = boundsAspect > aspect
                val (w, h) = if (wider == fit) {
                    (height * aspect).roundToInt() to height
                } else {
                    width to (width / aspect).roundToInt()
                }
                val placeable = measurable.measure(Constraints.fixed(w.coerceAtLeast(1), h.coerceAtLeast(1)))
                layout(width, height) {
                    placeable.place((width - w) / 2, (height - h) / 2)
                }
            },
        )
    }
}
