/*
 * Modeled on Orchard's own AudioDecoder (https://github.com/SFG5453/Orchard),
 * scoped down to the platform MediaCodec path — Orchard prefers a native
 * libopus decode with the platform decoder as its documented fallback; this
 * only needs the fallback, since BitChord has no reason to carry a second
 * Opus decoder purely for background analysis.
 *
 * Copyright (C) 2026 Kushagra Singh
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.music.bitchord.playback.smart

import android.media.MediaCodec
import android.media.MediaDataSource
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Decodes a region of a cached track to mono float PCM, for [TrackAnalyzer].
 *
 * Only a region: a transition only ever reads the tail of the outgoing track
 * and the head of the incoming one, not either track in full, and decoding a
 * whole album's worth of audio to analyse thirty seconds of it would cost
 * battery for nothing.
 *
 * Everything here is best-effort. A codec that will not configure, a
 * container Android cannot parse, a region past the end — all return null,
 * and the caller falls back to no analysis, which the transition policy
 * already handles as its bottom rung.
 */
object AudioDecoder {

    private const val TAG = "BitChordAudioDecoder"

    /** Decoded mono PCM at the container's own sample rate; the caller resamples. */
    data class Pcm(val samples: FloatArray, val sampleRate: Double)

    /**
     * Decoded planar stereo PCM at the container's own sample rate.
     *
     * Planar rather than interleaved because the only consumer is the vocal
     * front end, which wants one array per channel; a mono source is widened
     * by sharing the same samples on both sides, which is what the model was
     * trained to see for centre-panned material anyway.
     */
    data class StereoPcm(val left: FloatArray, val right: FloatArray, val sampleRate: Double)

    /**
     * Reads the audio duration a fully-cached container advertises, without
     * decoding it. Queue metadata isn't always trustworthy, so this is the
     * fallback [TrackAnalyzer] reaches for when a track's own duration is
     * missing or non-finite.
     */
    fun containerDurationSeconds(source: MediaDataSource): Double? {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(source)
            (0 until extractor.trackCount)
                .mapNotNull { index ->
                    val format = extractor.getTrackFormat(index)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: return@mapNotNull null
                    if (!mime.startsWith("audio/") || !format.containsKey(MediaFormat.KEY_DURATION)) {
                        return@mapNotNull null
                    }
                    format.getLong(MediaFormat.KEY_DURATION).takeIf { it > 0 }?.div(1_000_000.0)
                }
                .maxOrNull()
                ?.takeIf { it.isFinite() && it > 0 }
        } catch (error: Exception) {
            Log.w(TAG, "Could not read duration from cached media", error)
            null
        } finally {
            runCatching { extractor.release() }
        }
    }

    /**
     * Decodes [startSeconds] to [endSeconds] of [source], downmixed to mono at
     * the container's native rate.
     *
     * The extractor seeks to the closest sync sample at or before the
     * requested start, so a little more audio than asked for may come back at
     * the front; the caller is given the real start via the returned offset
     * so frame indices still map to true track times.
     *
     * [abort] is polled once per pass of the decode loop; answering true
     * abandons the decode as if it had failed. A whole-track decode is the
     * longest single thing analysis does, and it holds tens of megabytes of
     * PCM while it runs, so the caller that no longer wants the answer wants
     * it stopped rather than merely ignored.
     */
    private class PcmScratch {
        var shorts = ShortArray(8192)

        fun load(buffer: ByteBuffer, info: MediaCodec.BufferInfo): Int {
            val byteCount = info.size.coerceAtLeast(0)
            val shortCount = byteCount / 2
            if (shortCount <= 0) return 0
            if (shorts.size < shortCount) {
                shorts = ShortArray(max(shortCount, shorts.size * 2))
            }
            val view = buffer.duplicate().apply {
                order(ByteOrder.LITTLE_ENDIAN)
                position(info.offset)
                limit(info.offset + shortCount * 2)
            }.asShortBuffer()
            view.get(shorts, 0, shortCount)
            return shortCount
        }
    }

    /** Result of a zero-allocation streaming whole-track bar amplitude scan. */
    data class WaveformBinResult(
        val bins: FloatArray,
        val hasData: BooleanArray,
        val decodedSeconds: Double,
        val actualStartSeconds: Double,
        val effectiveDurationSeconds: Double,
        val reachedEos: Boolean,
    )

    fun decodeRegion(
        source: MediaDataSource,
        startSeconds: Double,
        endSeconds: Double,
        abort: () -> Boolean = { false },
    ): Pair<Pcm, Double>? {
        val scratch = PcmScratch()
        val chunks = ArrayList<FloatArray>()
        val decoded = decodeRaw(source, startSeconds, endSeconds, abort) { buffer, info, channels ->
            chunks += toMono(buffer, info, channels, scratch)
        } ?: return null
        return Pcm(flatten(chunks), decoded.first) to decoded.second
    }

    /**
     * As [decodeRegion], but keeping the two channels apart.
     *
     * Only the vocal front end needs this: open-unmix was trained on stereo,
     * and handing it a duplicated mono mix throws away the very stereo
     * information it uses to tell a centred vocal from the instruments
     * around it.
     */
    fun decodeRegionStereo(
        source: MediaDataSource,
        startSeconds: Double,
        endSeconds: Double,
        abort: () -> Boolean = { false },
    ): Pair<StereoPcm, Double>? {
        val scratch = PcmScratch()
        val left = ArrayList<FloatArray>()
        val right = ArrayList<FloatArray>()
        val decoded = decodeRaw(source, startSeconds, endSeconds, abort) { buffer, info, channels ->
            toStereo(buffer, info, channels, left, right, scratch)
        } ?: return null
        return StereoPcm(flatten(left), flatten(right), decoded.first) to decoded.second
    }

    /**
     * Streaming whole-track waveform extractor that reduces PCM buffers directly into
     * [binCount] RMS/Peak amplitude bins in-place (< 32 KB heap, zero 40 MB PCM arrays).
     *
     * Invokes [onProgress] periodically (~160 ms) so partial waveforms can appear smoothly
     * during decode on slow devices or large lossless files.
     */
    fun decodeWaveformBins(
        source: MediaDataSource,
        binCount: Int,
        durationHintSeconds: Double,
        abort: () -> Boolean = { false },
        onProgress: ((bins: FloatArray, hasData: BooleanArray, measuredFraction: Float, effectiveDurationSec: Double) -> Unit)? = null,
    ): WaveformBinResult? {
        if (binCount <= 0) return null
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(source)
            val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return null
            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            val containerDurSec = format.longOrNull(MediaFormat.KEY_DURATION)
                ?.takeIf { it > 0L }
                ?.div(1_000_000.0)
            val effectiveDuration = when {
                containerDurSec != null && containerDurSec.isFinite() && containerDurSec > 0.0 -> containerDurSec
                durationHintSeconds.isFinite() && durationHintSeconds > 0.0 -> durationHintSeconds
                else -> return null
            }

            extractor.seekTo(0L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            codec = runCatching { MediaCodec.createDecoderByType(mime) }
                .onFailure { Log.w(TAG, "No decoder for $mime", it) }
                .getOrNull() ?: return null
            codec.configure(format, null, null, 0)
            codec.start()

            val scratch = PcmScratch()
            val binSumSq = DoubleArray(binCount)
            val binPeak = FloatArray(binCount)
            val binCountSamples = IntArray(binCount)
            val binDuration = effectiveDuration / binCount

            val bufferInfo = MediaCodec.BufferInfo()
            var outputChannels = max(1, format.intOrNull(MediaFormat.KEY_CHANNEL_COUNT) ?: 1)
            var outputRate = format.intOrNull(MediaFormat.KEY_SAMPLE_RATE) ?: 0
            var actualStartSeconds = 0.0
            var sawFirstSample = false
            var decodedFrames = 0L
            var lastEmittedMs = System.currentTimeMillis()
            var inputDone = false
            var outputDone = false
            var reachedEosFlag = false

            while (!outputDone) {
                if (abort()) return null
                var madeProgress = false

                var fed = 0
                while (!inputDone && fed < 4) {
                    val inputIndex = codec.dequeueInputBuffer(0L)
                    if (inputIndex < 0) break
                    madeProgress = true
                    fed++
                    val inputBuffer = codec.getInputBuffer(inputIndex) ?: continue
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    val sampleTimeUs = extractor.sampleTime
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(inputIndex, 0, sampleSize, sampleTimeUs, 0)
                        extractor.advance()
                    }
                }

                var firstOutputWait = if (madeProgress) 0L else IDLE_TIMEOUT_US
                while (!outputDone) {
                    if (abort()) return null
                    val outputIndex = codec.dequeueOutputBuffer(bufferInfo, firstOutputWait)
                    firstOutputWait = 0L
                    when {
                        outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            madeProgress = true
                            val newFormat = codec.outputFormat
                            outputRate = newFormat.intOrNull(MediaFormat.KEY_SAMPLE_RATE) ?: outputRate
                            outputChannels = max(1, newFormat.intOrNull(MediaFormat.KEY_CHANNEL_COUNT) ?: outputChannels)
                        }
                        outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER ||
                            outputIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> break
                        outputIndex >= 0 -> {
                            madeProgress = true
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                outputDone = true
                                reachedEosFlag = true
                            }
                            if (bufferInfo.size > 0 && outputRate > 0) {
                                if (!sawFirstSample) {
                                    actualStartSeconds = max(0.0, bufferInfo.presentationTimeUs / 1_000_000.0)
                                    sawFirstSample = true
                                }
                                codec.getOutputBuffer(outputIndex)?.let { output ->
                                    val shortCount = scratch.load(output, bufferInfo)
                                    val frames = shortCount / outputChannels
                                    if (frames > 0) {
                                        val shorts = scratch.shorts
                                        // Stride down within each decoded buffer so CPU cost stays tiny
                                        // (~10-25x faster on full songs) while accurately capturing envelope.
                                        val stride = max(1, frames / 24)
                                        val invScale = 1f / (32768f * outputChannels)
                                        val bufStartSec = actualStartSeconds + decodedFrames.toDouble() / outputRate
                                        var f = 0
                                        while (f < frames) {
                                            val sampleSec = bufStartSec + f.toDouble() / outputRate
                                            val binIdx = (sampleSec / binDuration).toInt()
                                            if (binIdx in 0 until binCount) {
                                                val base = f * outputChannels
                                                var chSum = 0
                                                for (c in 0 until outputChannels) {
                                                    chSum += shorts[base + c]
                                                }
                                                val a = abs(chSum * invScale)
                                                binSumSq[binIdx] += (a * a).toDouble()
                                                if (a > binPeak[binIdx]) binPeak[binIdx] = a
                                                binCountSamples[binIdx]++
                                            }
                                            f += stride
                                        }
                                        decodedFrames += frames
                                    }
                                }
                            }
                            codec.releaseOutputBuffer(outputIndex, false)
                        }
                    }
                }

                if (onProgress != null && sawFirstSample && outputRate > 0) {
                    val now = System.currentTimeMillis()
                    if (now - lastEmittedMs >= 160L) {
                        lastEmittedMs = now
                        val decodedEndSec = actualStartSeconds + decodedFrames.toDouble() / outputRate
                        val frac = (decodedEndSec / effectiveDuration).toFloat().coerceIn(0f, 1f)
                        val snapBins = FloatArray(binCount)
                        val snapHas = BooleanArray(binCount)
                        for (b in 0 until binCount) {
                            val cnt = binCountSamples[b]
                            if (cnt > 0) {
                                val rms = sqrt(binSumSq[b] / cnt).toFloat()
                                snapBins[b] = 0.62f * rms + 0.38f * binPeak[b]
                                snapHas[b] = true
                            }
                        }
                        onProgress(snapBins, snapHas, frac, effectiveDuration)
                    }
                }
            }

            if (!sawFirstSample || outputRate <= 0 || decodedFrames <= 0L) return null
            val bins = FloatArray(binCount)
            val hasData = BooleanArray(binCount)
            for (b in 0 until binCount) {
                val cnt = binCountSamples[b]
                if (cnt > 0) {
                    val rms = sqrt(binSumSq[b] / cnt).toFloat()
                    bins[b] = 0.62f * rms + 0.38f * binPeak[b]
                    hasData[b] = true
                }
            }
            return WaveformBinResult(
                bins = bins,
                hasData = hasData,
                decodedSeconds = decodedFrames.toDouble() / outputRate,
                actualStartSeconds = actualStartSeconds,
                effectiveDurationSeconds = effectiveDuration,
                reachedEos = reachedEosFlag,
            )
        } catch (error: Exception) {
            Log.w(TAG, "Streaming waveform decode failed", error)
            return null
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }

    /**
     * Streaming zero-allocation [WaveformPeaks] extractor for [TrackAnalyzer] and Automix Studio.
     * Decodes straight from [MediaCodec] output buffers into quantized byte peaks (~40 Hz capped at
     * [MAX_PEAK_BINS]) with minimal RAM and no ONNX/DSP blocking.
     */
    fun decodeWaveformPeaks(
        source: MediaDataSource,
        durationHintSeconds: Double,
        abort: () -> Boolean = { false },
    ): WaveformPeaks? {
        val maxBins = MAX_PEAK_BINS
        val hintedBinCount = if (durationHintSeconds.isFinite() && durationHintSeconds > 0.0) {
            min(maxBins, max(160, (durationHintSeconds * 40.0).roundToInt()))
        } else {
            maxBins
        }
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(source)
            val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return null
            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            val containerDurSec = format.longOrNull(MediaFormat.KEY_DURATION)
                ?.takeIf { it > 0L }
                ?.div(1_000_000.0)
            val effectiveDuration = when {
                containerDurSec != null && containerDurSec.isFinite() && containerDurSec > 0.0 -> containerDurSec
                durationHintSeconds.isFinite() && durationHintSeconds > 0.0 -> durationHintSeconds
                else -> 0.0
            }
            val targetSampleSeconds = if (effectiveDuration > 0.0) {
                max(0.025, effectiveDuration / maxBins)
            } else {
                0.025
            }
            val initialCapacity = if (effectiveDuration > 0.0) {
                min(maxBins, max(hintedBinCount, (effectiveDuration / targetSampleSeconds).roundToInt() + 16))
            } else {
                maxBins
            }
            val peakBins = FloatArray(initialCapacity)
            var maxWrittenBin = -1

            extractor.seekTo(0L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            codec = runCatching { MediaCodec.createDecoderByType(mime) }
                .onFailure { Log.w(TAG, "No decoder for $mime", it) }
                .getOrNull() ?: return null
            codec.configure(format, null, null, 0)
            codec.start()

            val scratch = PcmScratch()
            val bufferInfo = MediaCodec.BufferInfo()
            var outputChannels = max(1, format.intOrNull(MediaFormat.KEY_CHANNEL_COUNT) ?: 1)
            var outputRate = format.intOrNull(MediaFormat.KEY_SAMPLE_RATE) ?: 0
            var actualStartSeconds = 0.0
            var sawFirstSample = false
            var decodedSeconds = 0.0
            var inputDone = false
            var outputDone = false

            while (!outputDone) {
                if (abort()) return null
                var madeProgress = false

                var fed = 0
                while (!inputDone && fed < 4) {
                    val inputIndex = codec.dequeueInputBuffer(0L)
                    if (inputIndex < 0) break
                    madeProgress = true
                    fed++
                    val inputBuffer = codec.getInputBuffer(inputIndex) ?: continue
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    val sampleTimeUs = extractor.sampleTime
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(inputIndex, 0, sampleSize, sampleTimeUs, 0)
                        extractor.advance()
                    }
                }

                var firstOutputWait = if (madeProgress) 0L else IDLE_TIMEOUT_US
                while (!outputDone) {
                    if (abort()) return null
                    val outputIndex = codec.dequeueOutputBuffer(bufferInfo, firstOutputWait)
                    firstOutputWait = 0L
                    when {
                        outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            madeProgress = true
                            val newFormat = codec.outputFormat
                            outputRate = newFormat.intOrNull(MediaFormat.KEY_SAMPLE_RATE) ?: outputRate
                            outputChannels = max(1, newFormat.intOrNull(MediaFormat.KEY_CHANNEL_COUNT) ?: outputChannels)
                        }
                        outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER ||
                            outputIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> break
                        outputIndex >= 0 -> {
                            madeProgress = true
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                outputDone = true
                            }
                            if (bufferInfo.size > 0 && outputRate > 0) {
                                if (!sawFirstSample) {
                                    actualStartSeconds = max(0.0, bufferInfo.presentationTimeUs / 1_000_000.0)
                                    sawFirstSample = true
                                }
                                codec.getOutputBuffer(outputIndex)?.let { output ->
                                    val shortCount = scratch.load(output, bufferInfo)
                                    val frames = shortCount / outputChannels
                                    if (frames > 0) {
                                        val shorts = scratch.shorts
                                        val framesPerTargetBin = max(1, (outputRate * targetSampleSeconds).roundToInt())
                                        val stride = max(1, framesPerTargetBin / 12)
                                        val invScale = 1f / (32768f * outputChannels)
                                        var f = 0
                                        while (f < frames) {
                                            val sampleOffsetSec = decodedSeconds + f.toDouble() / outputRate
                                            val binIdx = (sampleOffsetSec / targetSampleSeconds).toInt()
                                            if (binIdx in peakBins.indices) {
                                                val base = f * outputChannels
                                                var chSum = 0
                                                for (c in 0 until outputChannels) {
                                                    chSum += shorts[base + c]
                                                }
                                                val a = abs(chSum * invScale)
                                                if (a > peakBins[binIdx]) peakBins[binIdx] = a
                                                if (binIdx > maxWrittenBin) maxWrittenBin = binIdx
                                            }
                                            f += stride
                                        }
                                        decodedSeconds += frames.toDouble() / outputRate
                                    }
                                }
                            }
                            codec.releaseOutputBuffer(outputIndex, false)
                        }
                    }
                }
            }

            if (!sawFirstSample || outputRate <= 0 || maxWrittenBin < 0) return null
            val count = maxWrittenBin + 1
            val peaks = ByteArray(count) { i ->
                (peakBins[i].coerceIn(0f, 1f) * 127f).roundToInt().toByte()
            }
            return WaveformPeaks(actualStartSeconds, targetSampleSeconds, peaks)
        } catch (error: Exception) {
            Log.w(TAG, "Streaming peaks decode failed", error)
            return null
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }

    /** Concatenates the decoded chunks into one contiguous buffer. */
    private fun flatten(chunks: List<FloatArray>): FloatArray {
        val samples = FloatArray(chunks.sumOf { it.size })
        var offset = 0
        for (chunk in chunks) {
            chunk.copyInto(samples, offset)
            offset += chunk.size
        }
        return samples
    }

    /**
     * Runs the decode loop, handing each output buffer to [onBuffer], and
     * returns the output sample rate paired with the region's real start.
     *
     * Shared by the mono and stereo entry points so there is one dequeue loop
     * to get right rather than two that can drift apart; all that differs
     * between them is how a buffer is reduced, which is what [onBuffer] owns.
     */
    private fun decodeRaw(
        source: MediaDataSource,
        startSeconds: Double,
        endSeconds: Double,
        abort: () -> Boolean,
        onBuffer: (ByteBuffer, MediaCodec.BufferInfo, Int) -> Unit,
    ): Pair<Double, Double>? {
        if (endSeconds <= startSeconds) return null
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(source)
            val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return null
            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null

            val startUs = (startSeconds * 1_000_000).toLong()
            val endUs = (endSeconds * 1_000_000).toLong()
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            codec = runCatching { MediaCodec.createDecoderByType(mime) }
                .onFailure { Log.w(TAG, "No decoder for $mime", it) }
                .getOrNull() ?: return null
            codec.configure(format, null, null, 0)
            codec.start()

            val bufferInfo = MediaCodec.BufferInfo()
            var outputChannels = format.intOrNull(MediaFormat.KEY_CHANNEL_COUNT) ?: 1
            var outputRate = format.intOrNull(MediaFormat.KEY_SAMPLE_RATE) ?: 0
            var actualStartSeconds = -1.0
            var sawFirstSample = false
            var inputDone = false
            var outputDone = false

            while (!outputDone) {
                if (abort()) return null
                var madeProgress = false

                var fed = 0
                while (!inputDone && fed < 4) {
                    val inputIndex = codec.dequeueInputBuffer(0L)
                    if (inputIndex < 0) break
                    madeProgress = true
                    fed++
                    val inputBuffer = codec.getInputBuffer(inputIndex) ?: continue
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    val sampleTimeUs = extractor.sampleTime
                    if (sampleSize < 0 || (sampleTimeUs in 0..Long.MAX_VALUE && sampleTimeUs > endUs)) {
                        codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(inputIndex, 0, sampleSize, sampleTimeUs, 0)
                        extractor.advance()
                    }
                }

                var firstOutputWait = if (madeProgress) 0L else IDLE_TIMEOUT_US
                while (!outputDone) {
                    if (abort()) return null
                    val outputIndex = codec.dequeueOutputBuffer(bufferInfo, firstOutputWait)
                    firstOutputWait = 0L
                    when {
                        outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            madeProgress = true
                            val newFormat = codec.outputFormat
                            outputRate = newFormat.intOrNull(MediaFormat.KEY_SAMPLE_RATE) ?: outputRate
                            outputChannels = newFormat.intOrNull(MediaFormat.KEY_CHANNEL_COUNT) ?: outputChannels
                        }
                        outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER ||
                            outputIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> break
                        outputIndex >= 0 -> {
                            madeProgress = true
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                outputDone = true
                            }
                            if (bufferInfo.size > 0) {
                                if (!sawFirstSample) {
                                    actualStartSeconds = bufferInfo.presentationTimeUs / 1_000_000.0
                                    sawFirstSample = true
                                }
                                codec.getOutputBuffer(outputIndex)?.let { output ->
                                    onBuffer(output, bufferInfo, outputChannels)
                                }
                                if (bufferInfo.presentationTimeUs > endUs) outputDone = true
                            }
                            codec.releaseOutputBuffer(outputIndex, false)
                        }
                    }
                }
            }

            if (!sawFirstSample || outputRate <= 0) return null
            return outputRate.toDouble() to actualStartSeconds
        } catch (error: Exception) {
            Log.w(TAG, "Region decode failed", error)
            return null
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }

    /** Downmixes one 16-bit PCM output buffer to mono float in [-1, 1]. */
    private fun toMono(
        buffer: ByteBuffer,
        info: MediaCodec.BufferInfo,
        channels: Int,
        scratch: PcmScratch,
    ): FloatArray {
        val safeChannels = max(1, channels)
        val shortCount = scratch.load(buffer, info)
        val frames = shortCount / safeChannels
        val mono = FloatArray(frames)
        val shorts = scratch.shorts
        val invScale = 1f / (32768f * safeChannels)
        for (index in 0 until frames) {
            val base = index * safeChannels
            var sum = 0
            for (c in 0 until safeChannels) sum += shorts[base + c]
            mono[index] = sum * invScale
        }
        return mono
    }

    private fun toStereo(
        buffer: ByteBuffer,
        info: MediaCodec.BufferInfo,
        channels: Int,
        left: MutableList<FloatArray>,
        right: MutableList<FloatArray>,
        scratch: PcmScratch,
    ) {
        val safeChannels = max(1, channels)
        val shortCount = scratch.load(buffer, info)
        val frames = shortCount / safeChannels
        val leftChunk = FloatArray(frames)
        val rightChunk = FloatArray(frames)
        val shorts = scratch.shorts
        for (index in 0 until frames) {
            val base = index * safeChannels
            leftChunk[index] = shorts[base] / 32768f
            rightChunk[index] = (if (safeChannels > 1) shorts[base + 1] else shorts[base]) / 32768f
        }
        left += leftChunk
        right += rightChunk
    }

    private fun MediaFormat.intOrNull(key: String): Int? = if (containsKey(key)) getInteger(key) else null
    private fun MediaFormat.longOrNull(key: String): Long? = if (containsKey(key)) getLong(key) else null

    private const val IDLE_TIMEOUT_US = 2_000L
    private const val MAX_PEAK_BINS = 12_000
}
