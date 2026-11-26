package com.music.bitchord.playback.smart

import android.content.Context
import android.media.MediaDataSource
import android.net.Uri
import android.os.Process
import androidx.media3.common.util.UnstableApi
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.PlayerWaveStyle
import com.music.bitchord.data.settings.TrackAudioWaveform
import com.music.bitchord.playback.AudioCache
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.math.pow

/**
 * Produces normalized 96-bin whole-track audio amplitude bars ([TrackAudioWaveform])
 * for [PlayerWaveStyle.AUDIO_GRAPH].
 *
 * Uses existing [TrackAnalyzer] [WaveformPeaks] when already available, and otherwise
 * runs a zero-allocation streaming MediaCodec pass ([AudioDecoder.decodeWaveformBins])
 * that updates progressively (~160 ms) as audio decodes or downloads — avoiding large PCM
 * allocations and keeping both legacy and modern Android devices responsive.
 */
@UnstableApi
internal class TrackWaveformExtractor(
    private val context: Context,
    private val cache: AudioCache,
    private val analyzer: TrackAnalyzer,
) {
    private val resolver by lazy { context.contentResolver }
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "chordplay-wave-extractor").apply {
            isDaemon = true
            priority = Thread.NORM_PRIORITY - 1
        }
    }

    private val memoryCache: MutableMap<String, TrackAudioWaveform> = Collections.synchronizedMap(
        object : LinkedHashMap<String, TrackAudioWaveform>(32, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, TrackAudioWaveform>?): Boolean =
                size > MAX_CACHED_WAVEFORMS
        },
    )
    private val lastAttemptedPrefix = ConcurrentHashMap<String, Long>()

    @Volatile
    private var activeTrackId: String? = null

    @Volatile
    private var inFlightKey: String? = null

    fun setActiveTrack(trackId: String?, uri: Uri?, durationMs: Long) {
        activeTrackId = trackId
        if (trackId.isNullOrBlank()) {
            AppSettings.activeTrackWaveform.value = null
            AppSettings.liveAudioLevel.value = 0f
            return
        }

        val cached = memoryCache[trackId]
        if (cached != null) {
            AppSettings.activeTrackWaveform.value = cached
            if (cached.measuredFraction >= 0.96f) return
        } else {
            AppSettings.activeTrackWaveform.value = null
        }

        if (AppSettings.playerWaveStyle.value != PlayerWaveStyle.AUDIO_GRAPH) return
        request(trackId, uri, durationMs)
    }

    fun request(trackId: String, uri: Uri?, durationMs: Long) {
        if (trackId.isBlank() || uri == null) return
        if (AppSettings.playerWaveStyle.value != PlayerWaveStyle.AUDIO_GRAPH) return

        val existingComplete = memoryCache[trackId]?.takeIf { it.measuredFraction >= 0.96f }
        if (existingComplete != null) {
            if (activeTrackId == trackId) AppSettings.activeTrackWaveform.value = existingComplete
            return
        }

        val existingAnalysis = analyzer.analysisFor(trackId)
        val durationSec = when {
            durationMs > 0L -> durationMs / 1000.0
            existingAnalysis.duration > 0.0 -> existingAnalysis.duration
            else -> 0.0
        }

        val analyzerWave = existingAnalysis.waveform
        if (analyzerWave != null) {
            val effectiveDur = if (durationSec > 0.0) durationSec else analyzerWave.end
            if (effectiveDur > 0.0) {
                val fromAnalyzer = resamplePeaks(trackId, analyzerWave, effectiveDur)
                if (fromAnalyzer != null) {
                    if ((memoryCache[trackId]?.measuredFraction ?: 0f) <= fromAnalyzer.measuredFraction) {
                        memoryCache[trackId] = fromAnalyzer
                    }
                    if (activeTrackId == trackId) {
                        AppSettings.activeTrackWaveform.value = memoryCache[trackId]
                    }
                    if (fromAnalyzer.measuredFraction >= 0.95f) return
                }
            }
        }

        // Skip redundant re-decodes while a partial stream hasn't accumulated meaningful new bytes.
        val isLocal = LocalAudioSource.isLocal(uri)
        val currentPrefix = if (isLocal) {
            Long.MAX_VALUE
        } else {
            val best = bestAvailableRendition(uri) ?: return
            if (best.isComplete) Long.MAX_VALUE else best.cachedPrefix
        }
        val prevAttempt = lastAttemptedPrefix[trackId]
        if (prevAttempt != null && currentPrefix != Long.MAX_VALUE && currentPrefix < prevAttempt + PARTIAL_RETRY_STEP_BYTES) {
            return
        }

        val jobKey = "$trackId:$currentPrefix"
        if (inFlightKey == jobKey) return
        inFlightKey = jobKey

        executor.execute {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            try {
                if (activeTrackId != trackId) return@execute
                extractInternal(trackId, uri, durationSec)
            } finally {
                if (inFlightKey == jobKey) inFlightKey = null
            }
        }
    }

    private fun bestAvailableRendition(uri: Uri): AudioCache.Rendition? {
        val renditions = cache.renditionsOf(uri).filter { it.cachedPrefix > 0L }
        val complete = renditions.firstOrNull { it.isComplete }
        if (complete != null) return complete
        val partial = renditions.maxByOrNull { it.cachedPrefix } ?: return null
        return partial.takeIf { it.cachedPrefix >= MIN_PARTIAL_BYTES }
    }

    private fun extractInternal(trackId: String, uri: Uri, durationHintSec: Double) {
        val local = LocalAudioSource.isLocal(uri)
        val openSource: () -> MediaDataSource?
        val completeContainer: Boolean

        if (local) {
            lastAttemptedPrefix[trackId] = Long.MAX_VALUE
            openSource = { LocalAudioSource.open(resolver, uri) }
            completeContainer = true
        } else {
            val chosen = bestAvailableRendition(uri) ?: return
            lastAttemptedPrefix[trackId] = if (chosen.isComplete) Long.MAX_VALUE else chosen.cachedPrefix
            completeContainer = chosen.isComplete
            openSource = { cache.renditionDataSource(uri, chosen) }
        }

        if (activeTrackId != trackId) return

        val scan = openSource()?.use { source ->
            AudioDecoder.decodeWaveformBins(
                source = source,
                binCount = BAR_COUNT,
                durationHintSeconds = durationHintSec,
                abort = { activeTrackId != trackId },
                onProgress = { bins, hasData, partialFrac, _ ->
                    if (activeTrackId != trackId) return@decodeWaveformBins
                    val normalized = normalizeBars(bins, hasData) ?: return@decodeWaveformBins
                    val existing = memoryCache[trackId]
                    val nextFrac = partialFrac.coerceIn(0.05f, 0.94f)
                    if (existing == null || nextFrac > existing.measuredFraction + 0.04f) {
                        val partialWave = TrackAudioWaveform(
                            trackId = trackId,
                            bars = normalized,
                            measuredFraction = nextFrac,
                        )
                        memoryCache[trackId] = partialWave
                        AppSettings.activeTrackWaveform.value = partialWave
                    }
                },
            )
        } ?: return

        if (activeTrackId != trackId || scan.decodedSeconds < MIN_DECODED_SECONDS) return

        val normalized = normalizeBars(scan.bins, scan.hasData) ?: return
        val fullCoverage = scan.reachedEos && scan.decodedSeconds >= scan.effectiveDurationSeconds * 0.92
        val measuredFraction = if (completeContainer && fullCoverage) {
            1f
        } else {
            ((scan.actualStartSeconds + scan.decodedSeconds) / scan.effectiveDurationSeconds)
                .toFloat()
                .coerceIn(0.05f, 1f)
        }

        val wave = TrackAudioWaveform(
            trackId = trackId,
            bars = normalized,
            measuredFraction = measuredFraction,
        )
        memoryCache[trackId] = wave
        if (activeTrackId == trackId) {
            AppSettings.activeTrackWaveform.value = wave
        }
    }

    fun release() {
        executor.shutdownNow()
    }

    private fun resamplePeaks(trackId: String, peaks: WaveformPeaks, durationSec: Double): TrackAudioWaveform? {
        if (peaks.peaks.isEmpty() || durationSec <= 0.0) return null
        val bars = FloatArray(BAR_COUNT)
        val hitCounts = IntArray(BAR_COUNT)
        for (i in peaks.peaks.indices) {
            val tSec = peaks.start + i * peaks.sampleSeconds
            val frac = (tSec / durationSec).toFloat().coerceIn(0f, 0.9999f)
            val barIdx = (frac * BAR_COUNT).toInt().coerceIn(0, BAR_COUNT - 1)
            val mag = (peaks.peaks[i].toInt() and 0x7F) / 127f
            bars[barIdx] += mag
            hitCounts[barIdx]++
        }
        for (b in 0 until BAR_COUNT) {
            if (hitCounts[b] > 0) bars[b] = bars[b] / hitCounts[b]
        }
        val hasData = BooleanArray(BAR_COUNT) { hitCounts[it] > 0 }
        val normalized = normalizeBars(bars, hasData) ?: return null
        val covered = ((peaks.end - peaks.start) / durationSec).toFloat().coerceIn(0f, 1f)
        return TrackAudioWaveform(trackId = trackId, bars = normalized, measuredFraction = covered)
    }

    private fun normalizeBars(raw: FloatArray, hasData: BooleanArray): FloatArray? {
        val values = raw.indices.filter { hasData[it] }.map { raw[it] }.sorted()
        if (values.isEmpty()) return null
        val p95 = values[((values.size - 1) * 0.95f).toInt()].coerceAtLeast(0.06f)
        val out = FloatArray(BAR_COUNT)
        for (i in 0 until BAR_COUNT) {
            if (!hasData[i]) {
                out[i] = 0f
            } else {
                val norm = (raw[i] / p95).coerceIn(0f, 1.15f).coerceAtMost(1f)
                out[i] = norm.pow(0.62f).coerceIn(0.08f, 1f)
            }
        }
        val smoothed = FloatArray(BAR_COUNT)
        for (i in 0 until BAR_COUNT) {
            if (!hasData[i]) continue
            var acc = out[i] * 0.64f
            var weight = 0.64f
            if (i > 0 && hasData[i - 1]) {
                acc += out[i - 1] * 0.18f
                weight += 0.18f
            }
            if (i + 1 < BAR_COUNT && hasData[i + 1]) {
                acc += out[i + 1] * 0.18f
                weight += 0.18f
            }
            smoothed[i] = (acc / weight).coerceIn(0.08f, 1f)
        }
        return smoothed
    }

    companion object {
        const val BAR_COUNT = 96
        private const val MAX_CACHED_WAVEFORMS = 48
        private const val MIN_PARTIAL_BYTES = 32L * 1024L
        private const val PARTIAL_RETRY_STEP_BYTES = 160L * 1024L
        private const val MIN_DECODED_SECONDS = 0.5
    }
}
