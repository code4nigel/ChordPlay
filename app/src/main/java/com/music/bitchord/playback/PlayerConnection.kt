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
package com.music.bitchord.playback

import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.music.bitchord.data.innertube.StreamResolver
import com.music.bitchord.data.model.ArtistRef
import com.music.bitchord.data.model.NOTIFICATION_ART_PX
import com.music.bitchord.data.model.PlaybackSourceType
import com.music.bitchord.data.model.QueueTier
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.data.sources.SourceRegistry
import com.music.bitchord.data.sources.TrackMatcher
import com.music.bitchord.download.Downloads
import com.music.bitchord.ui.rememberIsForeground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/** Snapshot of playback state, driven by the MediaController. */
data class PlayerState(
    val song: Song? = null,
    val isPlaying: Boolean = false,
    /**
     * The playhead. A field rather than a value: its identity never changes, so
     * carrying it here costs no invalidation — see [PlaybackPosition].
     */
    val position: PlaybackPosition = PlaybackPosition(),
    /**
     * Left here rather than moved alongside the position: it settles once per
     * track, and [mutableStateOf] compares structurally, so the poll writing it
     * back unchanged every tick invalidates nothing.
     */
    val durationMs: Long = 0L,
    val error: String? = null,
    /** True while ExoPlayer is buffering — including our own stream-URL resolution. */
    val isLoading: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val queue: List<Song> = emptyList(),
    val queueIndex: Int = 0,
    /**
     * Whether the queue has somewhere to go either side of the current track.
     * Taken from the player rather than [queueIndex], so the wrap-around of
     * repeat-all is already accounted for.
     */
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false,
    /** True while the current item is the replacement installed by a quality upgrade. */
    val isQualityUpgraded: Boolean = false,
    val isLive: Boolean = false,
    val isSeekable: Boolean = false,
    val liveOffsetMs: Long = 0L,
    val isAtLiveEdge: Boolean = true,
)

/** Binds to [PlaybackService] for the lifetime of the composition. */
@Composable
fun rememberMediaController(): MediaController? {
    val context = LocalContext.current
    var controller by remember { mutableStateOf<MediaController?>(null) }

    DisposableEffect(context) {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            { controller = runCatching { future.get() }.getOrNull() },
            ContextCompat.getMainExecutor(context),
        )
        onDispose {
            MediaController.releaseFuture(future)
            controller = null
        }
    }
    return controller
}

/** Routes the player-screen AutoPlay button through the playback service. */
fun MediaController.toggleAutoplay() {
    sendCustomCommand(
        SessionCommand(ACTION_TOGGLE_AUTOPLAY, Bundle.EMPTY),
        Bundle.EMPTY,
    )
}

/**
 * Routes Shuffle through the playback service so changing its state and
 * reordering the service-owned queue happen as one operation.
 */
fun MediaController.toggleShuffle() {
    sendCustomCommand(
        SessionCommand(ACTION_TOGGLE_SHUFFLE, Bundle.EMPTY),
        Bundle.EMPTY,
    )
}

/** Clears the previous queue's service and restart state before starting radio. */
suspend fun MediaController.beginRadioQueue() {
    sendCustomCommand(
        SessionCommand(ACTION_BEGIN_RADIO_QUEUE, Bundle.EMPTY),
        Bundle.EMPTY,
    ).await()
}

/**
 * Asks the service to go looking for a better copy of the playing track, now,
 * because the listener asked for it.
 *
 * A session command rather than a `replaceMediaItem` from here: the item has to
 * be rebuilt, the track's cached rendition has to be dropped and the automatic
 * path's verdicts about it have to be cleared, and all three live on the
 * service's side of the session — see [PlaybackService]'s `upgradeQualityNow`.
 */
fun MediaController.upgradeQuality() {
    sendCustomCommand(
        SessionCommand(ACTION_UPGRADE_QUALITY, Bundle.EMPTY),
        Bundle.EMPTY,
    )
}

/**
 * Asks the service to revert the playing track to YouTube's original stream.
 */
fun MediaController.revertToOriginal() {
    sendCustomCommand(
        SessionCommand(ACTION_REVERT_TO_ORIGINAL, Bundle.EMPTY),
        Bundle.EMPTY,
    )
}

/**
 * Asks the service to smoothly transition the playing track to another version/rendition.
 */
fun MediaController.swapToVersion(targetSong: Song) {
    sendCustomCommand(
        SessionCommand(ACTION_SWAP_VERSION, Bundle.EMPTY),
        bundleOf(EXTRA_SWAP_MEDIA_ITEM to targetSong.toSongBundle()),
    )
}

/**
 * The credited artists, read back out of the two parallel lists they travel in.
 *
 * A `Song` is a data class and a `MediaItem`'s extras are a `Bundle`, which
 * holds scalars and collections of them — so the credits cross as their names
 * and their ids side by side rather than as the list itself. A blank id is a
 * name YouTube stated without a channel behind it, which is kept so the credit
 * still reads; it simply has no page to open.
 *
 * Shorter of the two lists wins, and they are written together, so a bundle
 * that has been through something which dropped one of them yields what is left
 * rather than a list of names with no ids or ids with no names.
 */
private fun Bundle.artistsFromBundle(): List<ArtistRef> {
    val names = getStringArrayList(ARTISTS_NAMES).orEmpty()
    val ids = getStringArrayList(ARTISTS_IDS).orEmpty()
    if (names.isEmpty() || ids.isEmpty()) return emptyList()
    return names.indices.map { index ->
        ArtistRef(name = names[index], browseId = ids.getOrNull(index)?.ifBlank { null })
    }
}

private const val ARTISTS_NAMES = "artistNames"
private const val ARTISTS_IDS = "artistIds"

fun Song.toSongBundle(): Bundle = bundleOf(
    "videoId" to videoId,
    "title" to title,
    "artist" to artist,
    "thumbnailUrl" to thumbnailUrl,
    "durationText" to durationText,
    "artistId" to artistId,
    "albumId" to albumId,
    "albumName" to albumName,
    "isVideo" to isVideo,
    "isVideoOrigin" to isVideoOrigin,
    "setVideoId" to setVideoId,
    "fromAutoplay" to fromAutoplay,
    "radioName" to radioName,
    "localUri" to localUri,
    "downloadFormat" to downloadFormat,
    "localPath" to localPath,
    "sourceQuality" to sourceQuality,
    "playbackSource" to playbackSource,
    "playbackSourceType" to playbackSourceType?.name,
    "playbackSourceId" to playbackSourceId,
    "isExplicit" to (isExplicit ?: false),
    "isLive" to isLive,
    "liveViewersText" to liveViewersText,
    // `bundleOf` has no overload for a list of strings, so the two halves of
    // the credits go in afterwards.
).apply {
    putStringArrayList(ARTISTS_NAMES, ArrayList(artists.map { it.name }))
    putStringArrayList(ARTISTS_IDS, ArrayList(artists.map { it.browseId.orEmpty() }))
}

fun songFromBundle(b: Bundle): Song = Song(
    videoId = b.getString("videoId").orEmpty(),
    title = b.getString("title").orEmpty(),
    artist = b.getString("artist").orEmpty(),
    thumbnailUrl = b.getString("thumbnailUrl"),
    durationText = b.getString("durationText"),
    artistId = b.getString("artistId"),
    artists = b.artistsFromBundle(),
    albumId = b.getString("albumId"),
    albumName = b.getString("albumName"),
    isVideo = b.getBoolean("isVideo"),
    isVideoOrigin = b.getBoolean("isVideoOrigin"),
    setVideoId = b.getString("setVideoId"),
    queueTier = if (b.getBoolean("fromAutoplay")) {
        com.music.bitchord.data.model.QueueTier.AUTOPLAY
    } else {
        com.music.bitchord.data.model.QueueTier.CONTEXT
    },
    radioName = b.getString("radioName"),
    localUri = b.getString("localUri"),
    downloadFormat = b.getString("downloadFormat"),
    localPath = b.getString("localPath"),
    sourceQuality = b.getString("sourceQuality"),
    playbackSource = b.getString("playbackSource"),
    playbackSourceType = b.getString("playbackSourceType")?.let { runCatching { com.music.bitchord.data.model.PlaybackSourceType.valueOf(it) }.getOrNull() },
    playbackSourceId = b.getString("playbackSourceId"),
    isExplicit = if (b.containsKey("isExplicit")) b.getBoolean("isExplicit") else null,
    isLive = b.getBoolean("isLive"),
    liveViewersText = b.getString("liveViewersText"),
)

/** Flushes the current radio queue to disk before reporting that it started. */
suspend fun MediaController.commitRadioQueue() {
    sendCustomCommand(
        SessionCommand(ACTION_COMMIT_RADIO_QUEUE, Bundle.EMPTY),
        Bundle.EMPTY,
    ).await()
}


/**
 * Marks the span of a queue-row drag — see [PartySync.beginQueueDrag]. Each
 * neighbour the row crosses is still its own `moveMediaItem`, sent the moment
 * it happens so the local queue and the on-screen list stay in step; this only
 * tells a jam's party sync to hold its publish until the row is dropped,
 * instead of sending one for every neighbour crossed along the way.
 */
fun MediaController.setQueueDragActive(active: Boolean) {
    sendCustomCommand(
        SessionCommand(ACTION_QUEUE_DRAG, Bundle.EMPTY),
        bundleOf(EXTRA_QUEUE_DRAG_ACTIVE to active),
    )
}

/** Mirrors the controller into Compose state, polling position while playing. */
@Composable
fun rememberPlayerState(controller: MediaController?): PlayerState {
    val position = remember { PlaybackPosition() }
    var state by remember { mutableStateOf(PlayerState(position = position)) }

    DisposableEffect(controller) {
        val player = controller ?: return@DisposableEffect onDispose {}

        // A MediaController reads each item across the session boundary. Keep
        // the converted queue until its timeline actually changes; playback,
        // buffering, repeat and metadata events do not require another O(n)
        // walk over a large playlist.
        var queueSnapshot = emptyList<Song>()

        // Set when the timeline change was the *contents* changing, which is the
        // only kind this has to convert again.
        //
        // `EVENT_TIMELINE_CHANGED` on its own is not that question. It also
        // fires for `TIMELINE_CHANGE_REASON_SOURCE_UPDATE`, which every track
        // raises as its source is prepared and reports its real duration — the
        // running order is identical, only the window it describes has filled
        // in. Rebuilding on those meant a thousand-track queue converted a
        // thousand songs out of the session for every track it loaded, on the
        // main thread, for as long as the queue kept playing.
        var queueChanged = false

        fun sync(
            error: String? = null,
            rebuildQueue: Boolean = false,
            refreshCurrentQueueItem: Boolean = false,
        ) {
            val item = player.currentMediaItem
            if (rebuildQueue) {
                queueSnapshot = (0 until player.mediaItemCount)
                    .map { player.getMediaItemAt(it).toSong() }
            } else if (refreshCurrentQueueItem && item != null) {
                val index = player.currentMediaItemIndex
                if (index in queueSnapshot.indices) {
                    queueSnapshot = queueSnapshot.toMutableList().also { it[index] = item.toSong() }
                }
            }
            val song = item?.toSong()
            val liveSnap = readLiveSnapshot(player, song)
            // Synced here too, so seeking while paused or buffering still moves
            // the scrubber (the poll loop only runs on play).
            position.report(player.currentPosition.coerceAtLeast(0L))
            state = state.copy(
                song = song,
                isPlaying = player.isPlaying,
                durationMs = liveSnap.durationMs,
                error = error,
                isLoading = player.playbackState == Player.STATE_BUFFERING,
                repeatMode = player.repeatMode,
                queue = queueSnapshot,
                queueIndex = player.currentMediaItemIndex,
                hasPrevious = player.hasPreviousMediaItem(),
                hasNext = player.hasNextMediaItem(),
                isQualityUpgraded = item?.mediaMetadata?.extras
                    ?.getBoolean(EXTRA_QUALITY_UPGRADED) == true,
                isLive = liveSnap.isLive,
                isSeekable = liveSnap.isSeekable,
                liveOffsetMs = liveSnap.liveOffsetMs,
                isAtLiveEdge = liveSnap.isAtLiveEdge,
            )
        }

        val listener = object : Player.Listener {
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                // The count guard is belt and braces: a playlist change is the
                // only reason the queue can be a different length, so if it is,
                // this must convert it again whatever the reason claims.
                if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED ||
                    timeline.windowCount != queueSnapshot.size
                ) {
                    queueChanged = true
                }
            }
            // Every jump the player makes — a seek, a skip, a repeat starting
            // over, a stretch of silence skipped — is announced here, ahead of
            // the `onEvents` that reports where it landed. It is the only thing
            // the lyrics accept as a reason to go backwards; see PlaybackPosition.
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                position.seeks++
            }
            override fun onEvents(p: Player, events: Player.Events) = sync(
                error = state.error,
                rebuildQueue = queueChanged.also { queueChanged = false },
                refreshCurrentQueueItem = events.contains(Player.EVENT_MEDIA_METADATA_CHANGED),
            )
            override fun onPlayerErrorChanged(error: androidx.media3.common.PlaybackException?) {
                sync(error?.let { "Playback failed: ${it.errorCodeName}" })
            }
        }
        player.addListener(listener)
        sync(rebuildQueue = true)
        onDispose { player.removeListener(listener) }
    }

    // Only while the app is on screen. The poll exists to move a scrubber, and
    // a scrubber behind a locked screen is not being read — but the loop is a
    // plain `delay`, so without this it went on making two binder round-trips a
    // second to the media session for the whole time the phone was in a pocket.
    // Nothing is lost by stopping: `sync` above runs on the controller's own
    // events, and the first thing that happens on the way back is a fresh read.
    val foreground = rememberIsForeground()
    LaunchedEffect(controller, state.isPlaying, state.isLive, foreground) {
        if (controller == null || !foreground || (!state.isPlaying && !state.isLive)) return@LaunchedEffect
        val interval = if (state.isPlaying) 500L else 1_000L
        while (true) {
            position.report(controller.currentPosition.coerceAtLeast(0L))
            val snap = readLiveSnapshot(controller, state.song)
            if (snap.durationMs != state.durationMs ||
                snap.isLive != state.isLive ||
                snap.isSeekable != state.isSeekable ||
                snap.liveOffsetMs != state.liveOffsetMs ||
                snap.isAtLiveEdge != state.isAtLiveEdge
            ) {
                state = state.copy(
                    durationMs = snap.durationMs,
                    isLive = snap.isLive,
                    isSeekable = snap.isSeekable,
                    liveOffsetMs = snap.liveOffsetMs,
                    isAtLiveEdge = snap.isAtLiveEdge,
                )
            }
            delay(interval)
        }
    }
    return state
}

private data class LiveSnapshot(
    val durationMs: Long,
    val isLive: Boolean,
    val isSeekable: Boolean,
    val liveOffsetMs: Long,
    val isAtLiveEdge: Boolean,
)

private fun readLiveSnapshot(player: Player, song: Song?): LiveSnapshot {
    val live = song?.isLive == true ||
        player.isCurrentMediaItemLive ||
        StreamResolver.isKnownLive(song?.videoId)
    val seekable = player.isCurrentMediaItemSeekable
    val rawDuration = player.duration.takeIf { it > 0L && it != C.TIME_UNSET } ?: 0L
    val duration = if (live && !seekable) 0L else rawDuration
    val offset = if (live) {
        player.currentLiveOffset.takeIf { it != C.TIME_UNSET && it >= 0L }
            ?: if (seekable && rawDuration > 0L) {
                (rawDuration - player.currentPosition.coerceAtLeast(0L)).coerceAtLeast(0L)
            } else {
                0L
            }
    } else {
        0L
    }
    return LiveSnapshot(
        durationMs = duration,
        isLive = live,
        isSeekable = seekable,
        liveOffsetMs = offset,
        isAtLiveEdge = !live || offset < LIVE_EDGE_TOLERANCE_MS,
    )
}

private const val LIVE_EDGE_TOLERANCE_MS = 15_000L

/**
 * Per-field snapshot view over [PlayerState]. A composable that reads
 * [song] or [isPlaying] subscribes only to that derived field instead of
 * recomposing on every unrelated [PlayerState] copy (buffering, duration, queue).
 */
@Stable
class PlayerReads internal constructor(
    private val source: State<PlayerState>,
    /** The playhead. Its identity never changes — see [PlaybackPosition]. */
    val position: PlaybackPosition,
) {
    private fun <T> derived(read: (PlayerState) -> T): State<T> =
        derivedStateOf(structuralEqualityPolicy()) { read(source.value) }

    val snapshot: PlayerState get() = source.value

    val song: Song? by derived { it.song }
    val isPlaying: Boolean by derived { it.isPlaying }
    val durationMs: Long by derived { it.durationMs }
    val error: String? by derived { it.error }
    val isLoading: Boolean by derived { it.isLoading }
    val repeatMode: Int by derived { it.repeatMode }
    val queue: List<Song> by derived { it.queue }
    val queueIndex: Int by derived { it.queueIndex }
    val hasPrevious: Boolean by derived { it.hasPrevious }
    val hasNext: Boolean by derived { it.hasNext }
    val isQualityUpgraded: Boolean by derived { it.isQualityUpgraded }
    val isLive: Boolean by derived { it.isLive }
    val isSeekable: Boolean by derived { it.isSeekable }
    val liveOffsetMs: Long by derived { it.liveOffsetMs }
    val isAtLiveEdge: Boolean by derived { it.isAtLiveEdge }
}

/**
 * Mirrors the controller into Compose state, polling position while playing,
 * and hands it out as [PlayerReads].
 */
@Composable
fun rememberPlayerReads(controller: MediaController?): PlayerReads {
    val position = remember { PlaybackPosition() }
    val stateHolder = remember { mutableStateOf(PlayerState(position = position)) }
    var state by stateHolder
    val reads = remember { PlayerReads(stateHolder, position) }

    DisposableEffect(controller) {
        val player = controller ?: return@DisposableEffect onDispose {}
        var queueSnapshot = emptyList<Song>()
        var queueChanged = false

        fun sync(
            error: String? = null,
            rebuildQueue: Boolean = false,
            refreshCurrentQueueItem: Boolean = false,
        ) {
            val item = player.currentMediaItem
            if (rebuildQueue) {
                queueSnapshot = (0 until player.mediaItemCount)
                    .map { player.getMediaItemAt(it).toSong() }
            } else if (refreshCurrentQueueItem && item != null) {
                val index = player.currentMediaItemIndex
                val refreshed = item.toSong()
                if (index in queueSnapshot.indices && queueSnapshot[index] != refreshed) {
                    queueSnapshot = queueSnapshot.toMutableList().also { it[index] = refreshed }
                }
            }
            val song = item?.toSong()
            val liveSnap = readLiveSnapshot(player, song)
            position.report(player.currentPosition.coerceAtLeast(0L))
            state = state.copy(
                song = song,
                isPlaying = player.isPlaying,
                durationMs = liveSnap.durationMs,
                error = error,
                isLoading = player.playbackState == Player.STATE_BUFFERING,
                repeatMode = player.repeatMode,
                queue = queueSnapshot,
                queueIndex = player.currentMediaItemIndex,
                hasPrevious = player.hasPreviousMediaItem(),
                hasNext = player.hasNextMediaItem(),
                isQualityUpgraded = item?.mediaMetadata?.extras
                    ?.getBoolean(EXTRA_QUALITY_UPGRADED) == true,
                isLive = liveSnap.isLive,
                isSeekable = liveSnap.isSeekable,
                liveOffsetMs = liveSnap.liveOffsetMs,
                isAtLiveEdge = liveSnap.isAtLiveEdge,
            )
        }

        val listener = object : Player.Listener {
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED ||
                    timeline.windowCount != queueSnapshot.size
                ) {
                    queueChanged = true
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                position.seeks++
            }

            override fun onEvents(p: Player, events: Player.Events) = sync(
                error = state.error,
                rebuildQueue = queueChanged.also { queueChanged = false },
                refreshCurrentQueueItem = events.contains(Player.EVENT_MEDIA_METADATA_CHANGED),
            )

            override fun onPlayerErrorChanged(error: androidx.media3.common.PlaybackException?) {
                sync(error?.let { "Playback failed: ${it.errorCodeName}" })
            }
        }
        player.addListener(listener)
        sync(rebuildQueue = true)
        onDispose { player.removeListener(listener) }
    }

    val foreground = rememberIsForeground()
    LaunchedEffect(controller, foreground) {
        if (controller == null || !foreground) return@LaunchedEffect
        snapshotFlow { state.isPlaying to state.isLive }.collectLatest { (playing, live) ->
            if (!playing && !live) return@collectLatest
            val interval = if (playing) 500L else 1_000L
            while (true) {
                position.report(controller.currentPosition.coerceAtLeast(0L))
                val snap = readLiveSnapshot(controller, state.song)
                if (snap.durationMs != state.durationMs ||
                    snap.isLive != state.isLive ||
                    snap.isSeekable != state.isSeekable ||
                    snap.liveOffsetMs != state.liveOffsetMs ||
                    snap.isAtLiveEdge != state.isAtLiveEdge
                ) {
                    state = state.copy(
                        durationMs = snap.durationMs,
                        isLive = snap.isLive,
                        isSeekable = snap.isSeekable,
                        liveOffsetMs = snap.liveOffsetMs,
                        isAtLiveEdge = snap.isAtLiveEdge,
                    )
                }
                delay(interval)
            }
        }
    }
    return reads
}

internal suspend fun pollWhilePlaying(isPlaying: () -> Boolean, intervalMs: Long, tick: () -> Unit) {
    snapshotFlow(isPlaying).collectLatest { playing ->
        if (!playing) return@collectLatest
        while (true) {
            tick()
            delay(intervalMs)
        }
    }
}

/**
 * The inverse of [toMediaItem], as far as a MediaItem can carry a [Song].
 *
 * It has to round-trip the fields used by the live queue UI and playback
 * service. A field dropped here disappears as soon as a Song passes through
 * Media3, because controllers and service helpers read it back through this.
 */
fun MediaItem.toSong() = Song(
    videoId = mediaId,
    title = mediaMetadata.title?.toString().orEmpty(),
    artist = mediaMetadata.artist?.toString().orEmpty(),
    thumbnailUrl = mediaMetadata.artworkUri?.toString(),
    durationText = mediaMetadata.extras?.getString(EXTRA_DURATION),
    artistId = mediaMetadata.extras?.getString(EXTRA_ARTIST_ID),
    artists = mediaMetadata.extras?.artistsFromBundle() ?: emptyList(),
    albumId = mediaMetadata.extras?.getString(EXTRA_ALBUM_ID),
    albumName = mediaMetadata.albumTitle?.toString(),
    isExplicit = mediaMetadata.extras?.takeIf { it.containsKey(EXTRA_EXPLICIT) }
        ?.getBoolean(EXTRA_EXPLICIT),
    isVideo = mediaMetadata.extras?.getBoolean(EXTRA_IS_VIDEO) == true,
    isVideoOrigin = mediaMetadata.extras?.getBoolean(EXTRA_VIDEO_ORIGIN) == true ||
        mediaMetadata.extras?.getBoolean(EXTRA_IS_VIDEO) == true,
    setVideoId = mediaMetadata.extras?.getString(EXTRA_SET_VIDEO_ID),
    queueTier = this.queueTier,
    queueEntryId = this.queueEntryId,
    radioName = mediaMetadata.extras?.getString(EXTRA_RADIO_NAME),
    playbackSource = mediaMetadata.extras?.getString(EXTRA_PLAYBACK_SOURCE),
    playbackSourceType = mediaMetadata.extras?.getString(EXTRA_PLAYBACK_SOURCE_TYPE)
        ?.let { runCatching { PlaybackSourceType.valueOf(it) }.getOrNull() },
    playbackSourceId = mediaMetadata.extras?.getString(EXTRA_PLAYBACK_SOURCE_ID),
    localUri = mediaMetadata.extras?.getString(EXTRA_LOCAL_URI),
    localPath = mediaMetadata.extras?.getString(EXTRA_LOCAL_PATH),
    isLive = mediaMetadata.extras?.getBoolean(EXTRA_IS_LIVE) == true ||
        localConfiguration?.uri?.getQueryParameter("live") == "1",
    liveViewersText = mediaMetadata.extras?.getString(EXTRA_LIVE_VIEWERS),
)

/** @see Song.fromAutoplay */
val MediaItem.fromAutoplay: Boolean
    get() = queueTier == QueueTier.AUTOPLAY

/** @see Song.queueTier */
val MediaItem.queueTier: QueueTier
    get() {
        return when (mediaMetadata.extras?.getString(EXTRA_QUEUE_TIER)) {
            "USER_QUEUE" -> QueueTier.USER_QUEUE
            "CONTEXT" -> QueueTier.CONTEXT
            "AUTOPLAY" -> QueueTier.AUTOPLAY
            else -> if (mediaMetadata.extras?.getBoolean(EXTRA_FROM_AUTOPLAY) == true) {
                QueueTier.AUTOPLAY
            } else {
                QueueTier.CONTEXT
            }
        }
    }

/** @see Song.queueEntryId */
val MediaItem.queueEntryId: String?
    get() = mediaMetadata.extras?.getString(EXTRA_QUEUE_ENTRY_ID)

/** Public metadata keys for queue categorization and immutable queue entry identity. */
const val EXTRA_QUEUE_TIER = "bitchord.queueTier"
const val EXTRA_QUEUE_ENTRY_ID = "bitchord.queueEntryId"

/**
 * Marks a queue entry as AutoPlay's rather than the user's. Carried on the
 * MediaItem so it survives the trip through the session — the queue belongs to
 * the player, and the UI only ever sees it back through a MediaController.
 */
private const val EXTRA_FROM_AUTOPLAY = "bitchord.fromAutoplay"

/** @see Song.radioName */
private const val EXTRA_RADIO_NAME = "bitchord.radioName"

/** @see Song.playbackSource */
private const val EXTRA_PLAYBACK_SOURCE = "bitchord.playbackSource"
private const val EXTRA_PLAYBACK_SOURCE_TYPE = "bitchord.playbackSourceType"
private const val EXTRA_PLAYBACK_SOURCE_ID = "bitchord.playbackSourceId"

/**
 * The artist and album pages this track hangs under, when they are known.
 *
 * Carried so they survive the round trip through the session: the player's own
 * menu backfills them with a lookup when they are missing (see MainActivity's
 * `links`), but a queue restored after a restart, or a track read back by the
 * service, has only what the item carries.
 */
private const val EXTRA_ARTIST_ID = "bitchord.artistId"
private const val EXTRA_ALBUM_ID = "bitchord.albumId"

/** @see Song.setVideoId */
private const val EXTRA_SET_VIDEO_ID = "bitchord.setVideoId"

/** @see Song.localUri */
internal const val EXTRA_LOCAL_URI = "bitchord.localUri"

/** @see Song.localPath */
internal const val EXTRA_LOCAL_PATH = "bitchord.localPath"

/**
 * How long the track runs, as the row that queued it said.
 *
 * On the item rather than left to [MediaMetadata.durationMs] because that field
 * is the *player's* to state, and the player takes its own figure from the
 * decoder. This one is the claim a cross-source match is made on — see
 * [TrackMatcher] — and the two disagree often enough that overwriting either
 * with the other loses information. Carried so that [toSong] can give it back
 * to the live queue and matching code.
 */
private const val EXTRA_DURATION = "bitchord.durationText"
private const val EXTRA_EXPLICIT = "bitchord.explicit"
private const val EXTRA_IS_VIDEO = "bitchord.isVideo"
private const val EXTRA_VIDEO_ORIGIN = "bitchord.isVideoOrigin"
internal const val EXTRA_IS_LIVE = "bitchord.isLive"
internal const val EXTRA_LIVE_VIEWERS = "bitchord.liveViewers"

/** Set only on the replacement item produced by [QualityUpgrade]. */
const val EXTRA_QUALITY_UPGRADED = "bitchord.qualityUpgraded"

/** Whether this item was selected from a music-video row, even after conversion. */
val MediaItem.isVideoOrigin: Boolean
    get() = mediaMetadata.extras?.getBoolean(EXTRA_VIDEO_ORIGIN) == true ||
        mediaMetadata.extras?.getBoolean(EXTRA_IS_VIDEO) == true

/** Whether this item represents an active/24/7 live broadcast stream. */
val MediaItem.isLiveItem: Boolean
    get() = mediaMetadata.extras?.getBoolean(EXTRA_IS_LIVE) == true ||
        localConfiguration?.uri?.getQueryParameter("live") == "1" ||
        StreamResolver.isKnownLive(mediaId)

fun MediaController.autoplaySectionStart(): Int = autoplaySectionStart(
    fromAutoplay = (0 until mediaItemCount).map { getMediaItemAt(it).fromAutoplay },
    currentIndex = currentMediaItemIndex,
)

/**
 * Custom scheme; PlaybackService resolves the real stream URL at play time.
 *
 * A video-tagged [Song] plays using the video's own audio by default. The
 * player can explicitly replace just the current item with a catalogue match.
 */
/**
 * MP4-family containers (m4a/aac/amr/wma/...) store their header or trailing
 * metadata in a way that needs backward seeking to parse, which the
 * content:// route (ContentDataSource) doesn't reliably support — the same
 * bytes read fine as a plain file. Formats like flac/mp3/ogg/webm already
 * seek correctly through content:// and are left alone.
 */
private val DIRECT_FILE_URI_EXTENSIONS = setOf(
    "m4a", "m4b", "m4p", "mp4", "aac", "3ga", "3gp", "3gpp",
    "alac", "amr", "awb", "wma", "aif", "aiff", "ac3", "dts",
    // DSF keeps its tags at the end, so the extractor reads the tail first and
    // seeks back to the audio; DFF walks every chunk to the end before it
    // starts, and seeks back the same way.
    "dsf", "dff",
)

private fun resolvePlaybackUri(uriString: String, localPath: String?): String {
    if (localPath.isNullOrBlank() || !uriString.startsWith("content://")) return uriString
    val ext = localPath.substringAfterLast('.', "").lowercase(Locale.ROOT)
    if (ext !in DIRECT_FILE_URI_EXTENSIONS) return uriString
    val file = File(localPath)
    return if (file.exists() && file.canRead()) Uri.fromFile(file).toString() else uriString
}

/**
 * The `&n=&a=&d=` tail every playback URI carries: what this track is, in the
 * terms [com.music.bitchord.data.sources.TrackMatcher] compares recordings on.
 *
 * The runtime is the one of the three that can rule a candidate *out* on its
 * own, and it is only ever a hint here — a row that never carried a duration
 * simply omits it and the match is made on title and artist alone, as it was
 * before.
 */
private fun encodeQueryValue(value: String): String =
    Uri.encode(value) ?: java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20")

private fun Song.matchQuery(): String = buildString {
    append("&n=").append(encodeQueryValue(title))
    append("&a=").append(encodeQueryValue(artist))
    TrackMatcher.secondsOf(durationText)?.let { append("&d=").append(it) }
    albumName?.takeIf { it.isNotBlank() }?.let { append("&l=").append(encodeQueryValue(it)) }
    isExplicit?.let { append("&e=").append(if (it) "1" else "0") }
    // `m` controls source routing, not player policy. A converted catalogue
    // track keeps [isVideoOrigin] for AutoMix, but is intentionally audio here
    // so the normal source-quality upgrade path can improve it.
    if (isVideo || isLive) append("&m=1")
    if (isLive) append("&live=1")
}

internal fun Song.playbackUriString(offlineUri: String? = null): String {
    val effectiveOfflineUri = if (isLive) null else offlineUri
    if (effectiveOfflineUri != null) return effectiveOfflineUri
    val sourceTrack = SourceRegistry.parseTrackKey(videoId)
    return when {
        videoId.startsWith("content://") || videoId.startsWith("file://") -> videoId
        isLive -> "bitchord://watch?v=$videoId${matchQuery()}"
        sourceTrack != null -> SourceRegistry.trackUri(sourceTrack.first, sourceTrack.second)
            .let { "$it${matchQuery()}" }
        OriginalVersion.isPinned(videoId) -> directYouTubeUri()
        else -> "bitchord://watch?v=$videoId${matchQuery()}"
    }
}

internal fun Song.playbackMimeType(offlineUri: String? = null): String? {
    val effectiveOfflineUri = if (isLive) null else offlineUri
    return if (isLive && effectiveOfflineUri == null) MimeTypes.APPLICATION_M3U8 else null
}

fun Song.toMediaItem(): MediaItem {
    val offlineUri = if (isLive) {
        null
    } else {
        localUri?.takeUnless(Downloads::isMissingLocalFile)
            ?: Downloads.verifiedSavedUri(videoId)
    }
    val uriString = playbackUriString(offlineUri)
    val mimeType = playbackMimeType(offlineUri)
    return MediaItem.Builder()
        .setMediaId(videoId)
        .setUri(resolvePlaybackUri(uriString, localPath))
        .apply {
            if (mimeType != null) {
                setMimeType(mimeType)
            }
        }
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            // The release this track came off, when whoever queued it knew.
            //
            // A native field rather than an extra because Media3 bundles this
            // one across the session on its own, and because the lock screen and
            // Android Auto both draw it — a track queued from an album page had
            // the name in hand all along and was arriving at those surfaces
            // without it. It is also what the Replay's album chart is counted
            // on: read back off the player, a track with no album here is a
            // track that cannot be filed under one.
            .setAlbumTitle(albumName)
            // Sized here rather than left as stored: this is what the lock
            // screen, the notification and Android Auto draw, all of them
            // large, and none of them go back for a better copy later.
            .setArtworkUri(artworkAt(NOTIFICATION_ART_PX)?.toUri())
            // System media surfaces (One UI's Now Bar, Android Auto, Assistant)
            // classify a session by its media type; untyped sessions get treated
            // as generic audio and lose the music-specific card.
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .setIsPlayable(true)
            .setIsBrowsable(false)
            // What a queue entry has to carry about itself: which section of
            // the queue it belongs to, whether it is playing off the device,
            // and how long the row that queued it said it runs. The uri two
            // lines up answers the second question but does not survive the
            // trip back out — Media3 leaves a MediaItem's localConfiguration
            // out of the bundle it sends to a MediaController — so without this
            // a track playing from a file reaches the UI looking like any other
            // YouTube track, and the player's menu offers to rate, download and
            // share it.
            //
            // Set for every track rather than only the local and AutoPlay ones,
            // because the runtime applies to all of them: gated on those two, a
            // plain YouTube track carries no extras at all, so [toSong] reads
            // back a null duration and later matching loses the `&d=` it
            // depends on.
            .apply {
                if (queueTier != QueueTier.CONTEXT || queueEntryId != null || fromAutoplay ||
                    offlineUri != null || durationText != null ||
                    artistId != null || albumId != null || setVideoId != null ||
                    isExplicit != null || isVideo || isVideoOrigin || isLive ||
                    liveViewersText != null || radioName != null ||
                    playbackSource != null || playbackSourceType != null || playbackSourceId != null
                ) {
                    setExtras(
                        bundleOf(
                            EXTRA_QUEUE_TIER to queueTier.name,
                            EXTRA_QUEUE_ENTRY_ID to queueEntryId,
                            EXTRA_FROM_AUTOPLAY to fromAutoplay,
                            EXTRA_RADIO_NAME to radioName,
                            EXTRA_PLAYBACK_SOURCE to playbackSource,
                            EXTRA_PLAYBACK_SOURCE_TYPE to playbackSourceType?.name,
                            EXTRA_PLAYBACK_SOURCE_ID to playbackSourceId,
                            EXTRA_LOCAL_URI to offlineUri,
                            EXTRA_LOCAL_PATH to localPath,
                            EXTRA_DURATION to durationText,
                            EXTRA_ARTIST_ID to artistId,
                            EXTRA_ALBUM_ID to albumId,
                            EXTRA_SET_VIDEO_ID to setVideoId,
                            EXTRA_EXPLICIT to isExplicit,
                            EXTRA_IS_VIDEO to isVideo,
                            EXTRA_VIDEO_ORIGIN to isVideoOrigin,
                            EXTRA_IS_LIVE to isLive,
                            EXTRA_LIVE_VIEWERS to liveViewersText,
                        ),
                    )
                }
            }
            .build(),
    )
    .build()
}

/**
 * The current song reopened through YouTube alone, bypassing every substitute
 * and quality-upgrade path.  The separate rendition tag is essential: the
 * base cache key may currently contain JioSaavn or module bytes, and resuming
 * that entry as though it were a YouTube WebM corrupts the stream.
 */
fun Song.toDirectYouTubeMediaItem(): MediaItem =
    toMediaItem().buildUpon()
        .setUri(directYouTubeUri())
        .build()

private fun Song.directYouTubeUri(): String =
    "bitchord://watch?v=$videoId${matchQuery()}&$DIRECT_YOUTUBE_PARAMETER=1&q=original"

/**
 * Whether there is a YouTube upload behind this song to go back *to* — the
 * question "Revert to original" only means something for.
 *
 * Offered for any track that has one rather than only for a track a quality
 * upgrade has visibly swapped, because the swap is not the only way to end up
 * on a copy that is wrong. A source ranked above YouTube gets first refusal on
 * every track — see [SourceResolver.substituteForYouTube] — so a song can be
 * playing JioSaavn's or a module's idea of it from its very first second, with
 * nothing on screen having changed and nothing to undo. Those are precisely the
 * ones worth doubting: the match is made on title, artist and runtime, and a
 * live version, a remaster or a different mix agreeing on all three is a
 * catalogue's ordinary business. The listener is the only one who can hear that
 * it is the wrong recording, and until this was always available they had no
 * way to say so.
 *
 * The three exclusions are all "there is no such upload", not "reverting would
 * be unwise":
 *
 *  - a track queued from a module's own catalogue has no YouTube id at all,
 *    only a source-and-track key that would build a nonsense URI;
 *  - a file on the device is identified by its own `content://` or `file://`
 *    URI, for the same reason;
 *  - a music video *is* the YouTube upload, so there is nowhere for it to go.
 *    Its catalogue match is a separate control with its own way back — see
 *    `VideoAudioVersionButton`.
 */
fun Song.hasYouTubeOriginal(): Boolean =
    videoId.isNotBlank() &&
        !videoId.startsWith("content://") &&
        !videoId.startsWith("file://") &&
        !isVideo &&
        SourceRegistry.parseTrackKey(videoId) == null

/** A playback URI carrying this is explicitly requested YouTube, never a substitute. */
const val DIRECT_YOUTUBE_PARAMETER = "direct_youtube"

/**
 * Which track a playback URI is for, as a media id — the inverse of the URI
 * [toMediaItem] builds, as far as the identity goes.
 *
 * Needed because most of what this app does to a track happens somewhere that
 * has only the URI: the resolver runs on ExoPlayer's loader thread with a
 * DataSpec in hand, and read-ahead means the track being fetched is usually not
 * the one playing. That is what makes it the answer to "whose log line is this"
 * — see [com.music.bitchord.data.TrackLog.about].
 *
 * Deliberately not the cache key, which looks similar and is not the same
 * thing: that one splits a track's renditions apart on purpose and spells a
 * source-backed track differently again, so filing lines under it would scatter
 * one song's story across several names.
 */
fun mediaIdIn(uri: Uri): String? = if (uri.authority == "source") {
    val configId = uri.getQueryParameter("s")
    val trackId = uri.getQueryParameter("t")
    if (configId != null && trackId != null) SourceRegistry.trackKey(configId, trackId) else null
} else {
    uri.getQueryParameter("v")
}

/**
 * Replaces the queue with [songs] and starts it.
 *
 * Suspending because of the mapping: [toMediaItem] is not the cheap struct copy
 * it looks like — it builds a URI and a metadata bundle per track, and asks
 * [Downloads] whether the file it has on record for the track is still on disk,
 * which is a `stat` per downloaded song. One track's worth of that is nothing.
 * A playlist's worth is hundreds of milliseconds of it, and run inline it lands
 * on the frame that handles the tap, so the page freezes before the music
 * starts. Built off the main thread and handed to the player back on it, which
 * is where Media3 requires the call to be made.
 */
suspend fun MediaController.playSongs(songs: List<Song>, startIndex: Int) {
    if (songs.isEmpty()) return
    // A queue started while shuffle is on goes in shuffled rather than being
    // played out of order — see [QueueShuffle]. The track the user picked still
    // leads, so it ends up at the top instead of at [startIndex].
    val shuffled = QueueShuffle.enabled.value
    val items = withContext(Dispatchers.Default) {
        val queue = if (shuffled) {
            QueueShuffle.startingOrder(songs, startIndex.coerceIn(songs.indices))
        } else {
            songs
        }
        queue.map { it.toMediaItem() }
    }
    val startPos = if (songs.getOrNull(startIndex)?.isLive == true) C.TIME_UNSET else 0L
    setMediaItems(items, queueStartIndex(startIndex, items.size, shuffled), startPos)
    prepare()
    play()
}

/**
 * Replaces the queue with 24/7 Live Stations without shuffling so Next/Previous
 * surf sequentially between live channels and playback starts directly at the live edge.
 */
suspend fun MediaController.playLiveStations(stations: List<Song>, startIndex: Int) {
    if (stations.isEmpty()) return
    val clamped = startIndex.coerceIn(stations.indices)
    val items = withContext(Dispatchers.Default) {
        stations.map { station ->
            station.copy(isLive = true, isVideo = true).toMediaItem()
        }
    }
    setMediaItems(items, clamped, C.TIME_UNSET)
    prepare()
    play()
}

/**
 * The selected track is moved to the head when a new queue is shuffled, so
 * playback must begin there rather than at its index in the unshuffled list.
 */
internal fun queueStartIndex(requestedIndex: Int, itemCount: Int, shuffled: Boolean): Int =
    if (shuffled) 0 else requestedIndex.coerceIn(0, itemCount - 1)
