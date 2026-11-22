package com.music.bitchord.ui.replay.year

import android.content.Context
import android.util.Log
import com.music.bitchord.data.innertube.Innertube
import com.music.bitchord.data.lyrics.EmbeddedLyrics
import com.music.bitchord.data.lyrics.LyricLine
import com.music.bitchord.data.lyrics.LyricsRepository
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.durationMillis
import com.music.bitchord.data.settings.AppSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Songs' best parts ([BestPart]) for the Replay year story: the heat curves
 * fetched and kept on disk, and the starts worked out from them.
 */
object BestParts {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val lock = Mutex()
    private var file: File? = null
    private val stored = ConcurrentHashMap<String, StoredHeat>()
    private val starts = ConcurrentHashMap<String, BestPartStart>()

    fun known(videoId: String): BestPartStart? = starts[videoId]

    suspend fun startFor(context: Context, song: Song): BestPartStart {
        starts[song.videoId]?.let { return it }
        ensureLoaded(context)
        val heat = heatmapFor(song)
        val durationMs = song.durationText.durationMillis().takeIf { it > 0 } ?: heat?.durationMs ?: 0L
        val lines = syncedLines(context, song, durationMs).orEmpty()
        val result = BestPart.pick(heat, lines, durationMs)
        if (result.source != BestPartStart.Source.FALLBACK) starts[song.videoId] = result
        return result
    }

    private suspend fun heatmapFor(song: Song): Heatmap? {
        val id = song.videoId
        if (song.localUri != null || !isYouTubeId(id)) return null
        val now = System.currentTimeMillis()
        stored[id]?.let { kept ->
            val heat = kept.heatmap()
            val fresh = now - kept.fetchedAt < if (heat != null) MONTH_MS else WEEK_MS
            if (fresh) return heat
        }
        val answer = withTimeoutOrNull(HEATMAP_TIMEOUT_MS) {
            try {
                Innertube.webNext(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.d(TAG, "no heatmap answer for $id: ${e.message}")
                null
            }
        } ?: return stored[id]?.heatmap()
        val heat = BestPart.parseHeatmap(answer)
        stored[id] = StoredHeat(heat?.segmentMs ?: 0L, heat?.intensity.orEmpty(), now)
        save()
        return heat
    }

    private suspend fun syncedLines(context: Context, song: Song, durationMs: Long): List<LyricLine>? {
        if (!AppSettings.syncedLyrics.value) return null
        return withTimeoutOrNull(LYRICS_TIMEOUT_MS) {
            try {
                song.localUri
                    ?.let { EmbeddedLyrics.forUri(context, it) }
                    ?.takeIf(EmbeddedLyrics::isSynced)
                    ?: if (durationMs > 0) {
                        LyricsRepository.lyrics(
                            videoId = song.videoId,
                            title = song.title,
                            artist = song.artist,
                            durationMs = durationMs,
                            album = song.albumName,
                            sources = AppSettings.lyricsSources.value,
                            order = AppSettings.lyricsSourceOrder.value,
                        )?.lines?.takeIf(EmbeddedLyrics::isSynced)
                    } else {
                        null
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.d(TAG, "no lyrics for ${song.videoId}: ${e.message}")
                null
            }
        }
    }

    fun isYouTubeId(id: String): Boolean = YOUTUBE_ID.matches(id)

    private val YOUTUBE_ID = Regex("[A-Za-z0-9_-]{11}")

    private suspend fun ensureLoaded(context: Context) = lock.withLock {
        if (file != null) return@withLock
        val target = File(context.applicationContext.filesDir, FILE_NAME)
        file = target
        withContext(Dispatchers.IO) {
            if (!target.exists()) return@withContext
            runCatching { json.decodeFromString(Stored.serializer(), target.readText()) }
                .onSuccess { kept -> stored.putAll(kept.heats) }
                .onFailure {
                    Log.w(TAG, "Discarding unreadable heatmap cache", it)
                    target.delete()
                }
        }
    }

    private suspend fun save() = lock.withLock {
        val target = file ?: return@withLock
        withContext(Dispatchers.IO) {
            runCatching {
                val kept = stored.entries
                    .sortedByDescending { it.value.fetchedAt }
                    .take(MAX_KEPT)
                    .associate { it.key to it.value }
                target.writeText(json.encodeToString(Stored.serializer(), Stored(heats = kept)))
            }.onFailure { Log.w(TAG, "Could not write heatmap cache", it) }
        }
    }

    @Serializable
    private data class Stored(val version: Int = 1, val heats: Map<String, StoredHeat> = emptyMap())

    @Serializable
    private data class StoredHeat(val segmentMs: Long, val intensity: List<Float>, val fetchedAt: Long) {
        fun heatmap(): Heatmap? = if (segmentMs > 0 && intensity.isNotEmpty()) Heatmap(segmentMs, intensity) else null
    }

    private const val TAG = "ChordPlayBestPart"
    private const val FILE_NAME = "replay_heatmaps.json"
    private const val MAX_KEPT = 500
    private const val HEATMAP_TIMEOUT_MS = 8_000L
    private const val LYRICS_TIMEOUT_MS = 6_000L
    private const val MONTH_MS = 30L * 24 * 3600 * 1000
    private const val WEEK_MS = 7L * 24 * 3600 * 1000
}
