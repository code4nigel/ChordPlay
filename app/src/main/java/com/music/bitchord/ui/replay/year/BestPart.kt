package com.music.bitchord.ui.replay.year

import com.music.bitchord.data.lyrics.LyricLine
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * YouTube's "Most replayed" curve for a video: how much each [segmentMs] stretch
 * of it gets replayed, 0..1, start to end.
 */
data class Heatmap(val segmentMs: Long, val intensity: List<Float>) {
    val durationMs: Long get() = segmentMs * intensity.size
}

/** Where a song's best part starts, and what said so. */
data class BestPartStart(val startMs: Long, val source: Source) {
    enum class Source {
        HEATMAP,
        LYRICS,
        FALLBACK,
    }
}

/**
 * Where a song's best part starts, worked out from YouTube's heat curve and
 * synced lyrics without decoding audio up front.
 */
object BestPart {

    const val SKIP_HEAD_MS = 5_000L
    const val SKIP_TAIL_MS = 30_000L
    const val MIN_SONG_MS = 60_000L

    private const val MIN_MARKERS = 10
    private const val MIN_RISE = 0.08f
    private const val MIN_LIFT = 0.03f
    private const val SMOOTH_HALF_MS = 4_500.0
    private const val TREND_HALF_MS = 45_000.0
    private const val TREND_FLOOR = 0.05f
    private const val FALLBACK_SHARE = 0.30
    private const val FALLBACK_MIN_MS = 20_000L
    private const val FALLBACK_UNKNOWN_MS = 45_000L

    fun parseHeatmap(answer: JsonObject): Heatmap? {
        val entity = findAll(answer, "macroMarkersListEntity")
            .filterIsInstance<JsonObject>()
            .firstOrNull { entity ->
                entity.obj("markersList")?.str("markerType") == "MARKER_TYPE_HEATMAP"
            } ?: return null
        val markers = (entity.obj("markersList")?.get("markers") as? JsonArray)
            ?.mapNotNull { element ->
                val marker = element as? JsonObject ?: return@mapNotNull null
                val start = marker.str("startMillis")?.toLongOrNull() ?: return@mapNotNull null
                val length = marker.str("durationMillis")?.toLongOrNull() ?: return@mapNotNull null
                val heat = marker.str("intensityScoreNormalized")?.toFloatOrNull()
                    ?: return@mapNotNull null
                Triple(start, length, heat.coerceIn(0f, 1f))
            }
            ?.sortedBy { it.first }
            ?: return null
        if (markers.size < MIN_MARKERS) return null
        val segment = markers.map { it.second }.sorted()[markers.size / 2]
        if (segment <= 0) return null
        return Heatmap(segment, markers.map { it.third })
    }

    fun peakMs(heat: Heatmap, durationMs: Long = 0): Long? {
        val duration = durationMs.takeIf { it > 0 } ?: heat.durationMs
        val n = heat.intensity.size
        if (duration < MIN_SONG_MS || n < MIN_MARKERS) return null
        val seg = heat.segmentMs

        val half = (SMOOTH_HALF_MS / seg).roundToInt().coerceAtLeast(1)
        val smooth = FloatArray(n) { i ->
            var sum = 0f
            var count = 0
            for (j in (i - half).coerceAtLeast(0)..(i + half).coerceAtMost(n - 1)) {
                sum += heat.intensity[j]
                count++
            }
            sum / count
        }
        val wide = (TREND_HALF_MS / seg).roundToInt().coerceAtLeast(3)
        val trend = FloatArray(n) { i ->
            val from = (i - wide).coerceAtLeast(1)
            val to = (i + wide).coerceAtMost(n - 1)
            val window = (from..to).map { smooth[it] }.sorted()
            window[window.size / 2]
        }

        fun rise(i: Int) = smooth[i] / trend[i].coerceAtLeast(TREND_FLOOR) - 1f

        val head = maxOf(SKIP_HEAD_MS, 2 * seg)
        val candidates = (0 until n).filter { i -> i * seg >= head && i * seg <= duration - SKIP_TAIL_MS }
        if (candidates.isEmpty()) return null
        val peak = candidates.maxBy { rise(it) }
        if (rise(peak) < MIN_RISE || smooth[peak] - trend[peak] < MIN_LIFT) return null

        val floor = rise(peak) * 0.5f
        var start = peak
        while (start - 1 in candidates && rise(start - 1) >= floor) start--
        var jump = start
        for (i in start..peak) {
            if (heat.intensity[i] / trend[i].coerceAtLeast(TREND_FLOOR) - 1f >= floor) {
                jump = i
                break
            }
        }
        return jump * seg
    }

    fun chorusMs(lines: List<LyricLine>, durationMs: Long): Long? {
        if (durationMs in 1 until MIN_SONG_MS) return null
        val timed = lines.filter { it.timeMs >= 0 && !it.isGap && it.text.isNotBlank() }
        if (timed.size < 4) return null
        val keys = timed.map { normalize(it.text) }
        val counts = keys.filter { it.length >= 4 }.groupingBy { it }.eachCount()
        val most = counts.values.maxOrNull() ?: return null
        if (most < 2) return null
        val limit = if (durationMs > 0) durationMs - SKIP_TAIL_MS else Long.MAX_VALUE
        val first = keys.indices.firstOrNull { i ->
            counts[keys[i]] == most && timed[i].timeMs >= SKIP_HEAD_MS && timed[i].timeMs <= limit
        } ?: return null
        var start = first
        while (start - 1 >= 0) {
            val count = counts[keys[start - 1]] ?: 0
            if (count < 2 || count < most - 1) break
            if (timed[start - 1].timeMs < SKIP_HEAD_MS || first - start >= 4) break
            start--
        }
        return timed[start].timeMs
    }

    fun snap(targetMs: Long, lineStarts: List<Long>): Long {
        lineStarts
            .filter { it in (targetMs - 4_000)..(targetMs + 1_500) }
            .minByOrNull { abs(it - targetMs) + if (it > targetMs) 1_500 else 0 }
            ?.let { return (it - 250).coerceAtLeast(0) }
        return (targetMs - 1_000).coerceAtLeast(0)
    }

    fun fallbackMs(durationMs: Long): Long = when {
        durationMs <= 0 -> FALLBACK_UNKNOWN_MS
        durationMs < MIN_SONG_MS -> 0L
        else -> maxOf(FALLBACK_MIN_MS, (durationMs * FALLBACK_SHARE).toLong())
            .coerceAtMost(durationMs - SKIP_TAIL_MS)
    }

    fun pick(heat: Heatmap?, lines: List<LyricLine>, durationMs: Long): BestPartStart {
        val lineStarts = lines.filter { it.timeMs >= 0 && !it.isGap }.map { it.timeMs }
        heat?.let { peakMs(it, durationMs) }?.let {
            return BestPartStart(snap(it, lineStarts), BestPartStart.Source.HEATMAP)
        }
        chorusMs(lines, durationMs)?.let {
            return BestPartStart(snap(it, lineStarts), BestPartStart.Source.LYRICS)
        }
        return BestPartStart(fallbackMs(durationMs), BestPartStart.Source.FALLBACK)
    }

    private fun normalize(text: String) = text.lowercase().filter { it.isLetterOrDigit() }

    private fun findAll(element: JsonElement, key: String): List<JsonElement> = buildList {
        fun walk(e: JsonElement) {
            when (e) {
                is JsonObject -> e.forEach { (k, v) -> if (k == key) add(v) else walk(v) }
                is JsonArray -> e.forEach(::walk)
                else -> Unit
            }
        }
        walk(element)
    }

    private fun JsonObject.obj(key: String): JsonObject? = get(key) as? JsonObject
    private fun JsonObject.str(key: String): String? = (get(key) as? JsonPrimitive)?.contentOrNull
}
