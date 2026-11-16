package com.music.bitchord.playback

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import com.metrolist.innertubex.sabr.SabrChunk
import com.metrolist.innertubex.sabr.findSabrProtocolException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.io.InterruptedIOException
import java.io.RandomAccessFile
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Seekable video stream abstraction shared by [SabrVideoStreamSource] and [HttpRangeVideoStreamSource]
 * so Media3's repeated [DataSource.open]/[DataSource.close] calls during container sniffing and seeking
 * do not discard buffered chunks or restart active streams.
 */
interface SeekableVideoSource {
    val mediaSize: Long?
    val streamFailure: Throwable?

    fun readAtMediaPosition(position: Long, buffer: ByteArray, offset: Int, length: Int): Int
    fun seekToMediaPosition(position: Long): Long
    fun updatePlaybackPosition(positionMs: Long) {}
    fun beginPlaybackSeek(positionMs: Long) = updatePlaybackPosition(positionMs)
    fun endPlaybackSeek(positionMs: Long) = updatePlaybackPosition(positionMs)
    fun closeAsync()
}

@UnstableApi
class SeekableVideoDataSource(
    private val source: SeekableVideoSource,
) : BaseDataSource(true) {
    private var opened = false
    private var uri: Uri = Uri.EMPTY
    private var readPosition = 0L
    private var bytesRemaining = C.LENGTH_UNSET.toLong()

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        transferInitializing(dataSpec)
        source.seekToMediaPosition(dataSpec.position)
        readPosition = dataSpec.position
        val available = source.mediaSize?.minus(dataSpec.position)?.coerceAtLeast(0L)
        bytesRemaining = when {
            dataSpec.length != C.LENGTH_UNSET.toLong() -> available?.let { minOf(it, dataSpec.length) } ?: dataSpec.length
            available != null -> available
            else -> C.LENGTH_UNSET.toLong()
        }
        opened = true
        transferStarted(dataSpec)
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
        val requested = if (bytesRemaining == C.LENGTH_UNSET.toLong()) {
            length
        } else {
            minOf(length.toLong(), bytesRemaining).toInt()
        }
        val count = source.readAtMediaPosition(readPosition, buffer, offset, requested)
        if (count <= 0) return C.RESULT_END_OF_INPUT
        readPosition += count
        if (bytesRemaining != C.LENGTH_UNSET.toLong()) bytesRemaining -= count
        bytesTransferred(count)
        return count
    }

    override fun getUri(): Uri = uri

    override fun close() {
        if (opened) {
            opened = false
            transferEnded()
        }
    }

    class Factory(private val source: SeekableVideoSource) : DataSource.Factory {
        override fun createDataSource(): DataSource = SeekableVideoDataSource(source)
    }
}

/**
 * Streams a SABR/UMP video flow into a temporary file-backed random-access buffer so Media3
 * can sniff MP4/WebM headers, seek instantly within buffered segments, and restart the SABR
 * producer around the playhead on out-of-range seeks.
 *
 * Adapted from Metrolist KMP's `SabrInputStream`.
 */
@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
class SabrVideoStreamSource(
    override val mediaSize: Long?,
    private val streamFactory: (initialPlayerTimeMs: Long, playbackPositionMs: () -> Long) -> Flow<SabrChunk>,
    private val expectedDurationMs: Long? = null,
    private val startupTimeoutMs: Long = DEFAULT_STARTUP_TIMEOUT_MS,
    private val readTimeoutMs: Long = DEFAULT_READ_TIMEOUT_MS,
    private val seekPrerollMs: Long = DEFAULT_SEEK_PREROLL_MS,
    tempDirectory: File,
) : SeekableVideoSource {
    private val stateLock = Object()
    private val playbackPositionLock = Object()
    private val cacheCommitLock = Any()
    private val backing = CacheBacking(tempDirectory)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var playbackPositionMs = 0L
    private var playbackSeekInProgress = false

    @Volatile
    private var closed = false

    @Volatile
    override var streamFailure: Throwable? = null
        private set

    private val cachedRanges = mutableListOf<LongRange>()
    private val cachedChunkEndTimes = mutableMapOf<Long, Long>()
    private var readPosition = 0L
    private var completed = false
    private var receivedMedia = false

    @Volatile
    private var producerGeneration = 0L
    private var producer: Job? = null
    private var producerInitialPlayerTimeMs = 0L
    private var startupDeadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(startupTimeoutMs)

    init {
        startProducer(0L)
    }

    private fun startIfNeeded(position: Long) {
        if (producer != null || completed || closed || streamFailure != null) return
        val target = synchronized(playbackPositionLock) { (playbackPositionMs - seekPrerollMs).coerceAtLeast(0L) }
        val prefixEnd = cachedEndAt(0L)
        startProducer(if (position == 0L && prefixEnd == null) 0L else maxOf(target, cachedChunkEndTimes[prefixEnd] ?: 0L))
    }

    private fun startProducer(initialPlayerTimeMs: Long) {
        val generation = synchronized(stateLock) {
            synchronized(cacheCommitLock) {
                producerGeneration++
                streamFailure = null
                completed = false
                producerInitialPlayerTimeMs = initialPlayerTimeMs
                if (!receivedMedia) {
                    startupDeadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(startupTimeoutMs)
                }
                producerGeneration
            }
        }
        producer?.cancel()
        producer = scope.launch {
            try {
                streamFactory(initialPlayerTimeMs) { synchronized(playbackPositionLock) { playbackPositionMs } }
                    .collect { append(generation, it) }
                synchronized(stateLock) {
                    if (closed || producerGeneration != generation) return@launch
                    val lastMediaEndMs = cachedChunkEndTimes.values.maxOrNull()
                    val minimumCompleteEndMs = expectedDurationMs?.minus(EARLY_END_TOLERANCE_MS)?.coerceAtLeast(0L)
                    if (minimumCompleteEndMs != null && lastMediaEndMs != null && lastMediaEndMs < minimumCompleteEndMs) {
                        streamFailure = IOException(
                            "SABR stream ended at $lastMediaEndMs ms before expected duration $expectedDurationMs ms",
                        )
                    }
                    completed = true
                    stateLock.notifyAll()
                }
            } catch (cancelled: CancellationException) {
                recordFailure(generation, cancelled)
            } catch (error: Throwable) {
                recordFailure(generation, error)
            }
        }
    }

    override fun readAtMediaPosition(position: Long, buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        mediaSize?.let { if (position >= it) return -1 }

        val count: Int
        synchronized(stateLock) {
            startIfNeeded(position)
            var availableEnd = cachedEndAt(position)
            var readDeadlineNanos = if (receivedMedia) {
                System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(readTimeoutMs)
            } else {
                startupDeadlineNanos
            }
            while (!closed && streamFailure == null && !completed && availableEnd == null) {
                try {
                    val remainingNanos = readDeadlineNanos - System.nanoTime()
                    if (remainingNanos <= 0L) {
                        streamFailure = SocketTimeoutException(
                            if (receivedMedia) {
                                "SABR video stream stalled waiting for media"
                            } else {
                                "SABR video stream produced no media before startup deadline"
                            },
                        )
                        completed = true
                        break
                    }
                    val remainingMillis = TimeUnit.NANOSECONDS.toMillis(remainingNanos).coerceAtLeast(1L)
                    stateLock.wait(remainingMillis)
                } catch (error: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw InterruptedIOException("SABR video read interrupted").apply { initCause(error) }
                }
                availableEnd = cachedEndAt(position)
            }
            if (availableEnd == null) {
                if (!closed && completed && streamFailure == null && (mediaSize != null || cachedRanges.any { it.first > position })) {
                    streamFailure = IOException("SABR video stream ended before media byte $position")
                }
                streamFailure?.let { throw IOException("SABR video stream failed", it) }
            }
            if (closed || availableEnd == null) return -1
            count = minOf(length.toLong(), availableEnd - position).toInt()
        }

        synchronized(backing) {
            if (closed) return -1
            if (Thread.currentThread().isInterrupted) throw InterruptedIOException("SABR video read interrupted")
            backing.data.seek(position)
            backing.data.readFully(buffer, offset, count)
        }
        return count
    }

    override fun seekToMediaPosition(position: Long): Long {
        val bounded = mediaSize?.let { position.coerceIn(0L, it) } ?: position.coerceAtLeast(0L)
        synchronized(stateLock) {
            if (closed) return bounded
            readPosition = bounded
            if (streamFailure?.findSabrProtocolException() != null) return bounded
            if (producer == null) {
                if (bounded < (mediaSize ?: Long.MAX_VALUE)) startIfNeeded(bounded)
                return bounded
            }
            if (bounded < (mediaSize ?: Long.MAX_VALUE) && !isFullyCached()) {
                val uncached = cachedEndAt(bounded) == null
                val targetTimeMs = synchronized(playbackPositionLock) {
                    if (bounded == 0L && uncached) 0L else (playbackPositionMs - seekPrerollMs).coerceAtLeast(0L)
                }
                val shouldRestart = (
                    targetTimeMs < producerInitialPlayerTimeMs &&
                        (cachedChunkEndTimes[cachedEndAt(bounded)] ?: Long.MIN_VALUE) < producerInitialPlayerTimeMs
                    ) || (
                    uncached && (
                        completed || producer?.isActive != true ||
                            (producerInitialPlayerTimeMs == 0L && targetTimeMs > 0L) ||
                            (targetTimeMs - producerInitialPlayerTimeMs) >= SEEK_RESTART_THRESHOLD_MS
                        )
                    )
                if (shouldRestart) {
                    startProducer(targetTimeMs)
                }
            }
        }
        return bounded
    }

    override fun updatePlaybackPosition(positionMs: Long) {
        synchronized(playbackPositionLock) {
            if (!playbackSeekInProgress) {
                playbackPositionMs = positionMs.coerceAtLeast(0L)
            }
        }
    }

    override fun beginPlaybackSeek(positionMs: Long) {
        synchronized(playbackPositionLock) {
            playbackSeekInProgress = true
            playbackPositionMs = positionMs.coerceAtLeast(0L)
        }
    }

    override fun endPlaybackSeek(positionMs: Long) {
        synchronized(playbackPositionLock) {
            playbackPositionMs = positionMs.coerceAtLeast(0L)
            playbackSeekInProgress = false
        }
    }

    override fun closeAsync() {
        val wasOpen = synchronized(stateLock) {
            if (closed) {
                false
            } else {
                closed = true
                stateLock.notifyAll()
                true
            }
        }
        if (wasOpen) {
            scope.cancel()
            CoroutineScope(Dispatchers.IO).launch { backing.release() }
        }
    }

    private fun append(generation: Long, chunk: SabrChunk) {
        if (chunk.data.isEmpty()) return
        val endRangeExclusive = chunk.endRangeExclusive
        if (mediaSize != null && endRangeExclusive > mediaSize) {
            throw IOException("SABR video stream exceeded declared media size")
        }
        synchronized(cacheCommitLock) {
            synchronized(backing) {
                if (closed || producerGeneration != generation) return
                currentCoroutineContextCheck()
                backing.data.seek(chunk.startRange)
                backing.data.write(chunk.data)
            }
        }
        synchronized(stateLock) {
            if (closed || producerGeneration != generation) return
            receivedMedia = true
            addCachedRange(chunk.startRange until endRangeExclusive)
            val startMs = chunk.startMs
            val durationMs = chunk.durationMs
            if (!chunk.isInitialization && startMs != null && durationMs != null &&
                startMs >= 0L && durationMs > 0L && startMs <= Long.MAX_VALUE - durationMs
            ) {
                cachedChunkEndTimes[endRangeExclusive] = startMs + durationMs
            }
            cachedChunkEndTimes.keys.retainAll(cachedRanges.map { it.last + 1L }.toSet())
            if (mediaSize != null && isFullyCached()) {
                completed = true
            }
            stateLock.notifyAll()
        }
    }

    private fun currentCoroutineContextCheck() {
        if (Thread.currentThread().isInterrupted) throw InterruptedIOException("SABR write interrupted")
    }

    private fun isFullyCached(): Boolean {
        val size = mediaSize ?: return false
        return (cachedEndAt(0L) ?: 0L) >= size
    }

    private fun cachedEndAt(position: Long): Long? =
        cachedRanges.firstOrNull { position in it }?.let { it.last + 1L }

    private fun addCachedRange(range: LongRange) {
        if (range.isEmpty()) return
        cachedRanges += range
        cachedRanges.sortBy(LongRange::first)
        val merged = mutableListOf<LongRange>()
        cachedRanges.forEach { current ->
            val previous = merged.lastOrNull()
            if (previous != null && current.first <= previous.last + 1L) {
                merged[merged.lastIndex] = previous.first..maxOf(previous.last, current.last)
            } else {
                merged += current
            }
        }
        cachedRanges.clear()
        cachedRanges += merged
    }

    private fun recordFailure(generation: Long, error: Throwable) {
        synchronized(stateLock) {
            if (closed || producerGeneration != generation) return
            streamFailure = error
            completed = true
            stateLock.notifyAll()
        }
    }

    private class CacheBacking(private val directory: File) {
        private var cache: Pair<File, RandomAccessFile>? = null
        val data: RandomAccessFile get() = open().second

        private fun open(): Pair<File, RandomAccessFile> {
            cache?.let { return it }
            val file = File.createTempFile("chordplay-sabr-vid-", ".media", directory)
            return try {
                val raf = RandomAccessFile(file, "rw")
                runCatching { file.delete() }
                (file to raf).also { cache = it }
            } catch (t: Throwable) {
                runCatching { file.delete() }
                throw t
            }
        }

        fun release() {
            synchronized(this) {
                cache?.let { (file, raf) ->
                    runCatching { raf.close() }
                    runCatching { file.delete() }
                }
                cache = null
            }
        }
    }

    companion object {
        const val DEFAULT_SEEK_PREROLL_MS = 10_000L
        private const val SEEK_RESTART_THRESHOLD_MS = 15_000L
        private const val DEFAULT_STARTUP_TIMEOUT_MS = 12_000L
        private const val DEFAULT_READ_TIMEOUT_MS = 30_000L
        private const val EARLY_END_TOLERANCE_MS = 2_000L
    }
}
