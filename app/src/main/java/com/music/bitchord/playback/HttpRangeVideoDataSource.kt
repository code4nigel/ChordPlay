package com.music.bitchord.playback

import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import com.music.bitchord.data.Http
import com.music.bitchord.data.TrackLog
import com.music.bitchord.data.innertube.PlayerClient
import com.music.bitchord.data.innertube.StreamResolver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Non-blocking HTTP byte-range video source modeled after Metrolist KMP's `HttpRangeInputStream`,
 * combined with BitChord's `StreamResolver.onPlaybackRefused` attribution and Prism's transient
 * 403 single retry:
 * - Starts with a 64 KiB probe chunk so MP4 `ftyp`/`moov` headers resolve in milliseconds.
 * - Doubles demand chunk size up to [chunkSize] (clamped by [PlayerClient.rangeBytesFor]).
 * - Retains an LRU RAM cache of 4 chunks across Media3 `open()`/`close()`/`seek()` boundaries.
 * - Prefetches up to 3 chunks ahead concurrently so decoder reads never stall on range boundaries.
 */
@UnstableApi
class HttpRangeVideoStreamSource(
    private val url: String,
    private val requestHeaders: Map<String, String>,
    mediaSize: Long? = null,
    private val chunkSize: Int = DEFAULT_CHUNK_SIZE,
    private val startupRequestTimeoutMs: Long = DEFAULT_STARTUP_TIMEOUT_MS,
    private val requestTimeoutMs: Long = DEFAULT_REQUEST_TIMEOUT_MS,
) : SeekableVideoSource {

    private val prefetchScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val chunkSemaphore = Semaphore(MAX_CONCURRENT_CHUNK_REQUESTS)
    private val chunkCache = LinkedHashMap<Long, ByteArray>(MAX_CACHED_CHUNKS, 0.75f, true)
    private val inFlightChunks = mutableMapOf<Long, Deferred<ByteArray>>()
    private val activeCalls = mutableSetOf<Call>()

    private var position = 0L
    private var activeChunkStart = -1L
    private var activeChunkBytes = byteArrayOf()
    private var nextDemandChunkSize = minOf(STARTUP_PROBE_CHUNK_SIZE, chunkSize)
    private var hasFetchedChunk = false
    private var prefetchJob: Job? = null

    @Volatile
    private var closed = false

    @Volatile
    override var streamFailure: Throwable? = null
        private set

    @Volatile
    override var mediaSize: Long? = mediaSize?.takeIf { it > 0L }
        ?: url.toHttpUrlOrNull()?.queryParameter("clen")?.toLongOrNull()?.takeIf { it > 0L }
        private set

    override fun readAtMediaPosition(position: Long, buffer: ByteArray, offset: Int, length: Int): Int {
        if (closed) return -1
        if (length == 0) return 0
        val total = mediaSize
        if (total != null && position >= total) return -1

        synchronized(this) {
            val cacheOffset = (position - activeChunkStart).toInt()
            if (position in activeChunkStart until activeChunkStart + activeChunkBytes.size) {
                val count = minOf(
                    length.toLong(),
                    (activeChunkBytes.size - cacheOffset).toLong(),
                    if (total != null) total - position else Long.MAX_VALUE,
                ).toInt()
                if (count > 0) {
                    activeChunkBytes.copyInto(buffer, offset, cacheOffset, cacheOffset + count)
                    return count
                }
            }
        }

        val (start, bytes) = fetchChunk(position)

        synchronized(this) {
            if (closed) return -1
            activeChunkStart = start
            activeChunkBytes = bytes
            if (!closed) {
                triggerPrefetchLoop(start, bytes.size.toLong())
            }
            val currentTotal = mediaSize
            val cacheOffset = (position - activeChunkStart).toInt()
            val count = minOf(
                length.toLong(),
                (activeChunkBytes.size - cacheOffset).toLong(),
                if (currentTotal != null) currentTotal - position else Long.MAX_VALUE,
            ).toInt()
            if (count <= 0) return -1
            activeChunkBytes.copyInto(buffer, offset, cacheOffset, cacheOffset + count)
            return count
        }
    }

    override fun seekToMediaPosition(position: Long): Long {
        val total = mediaSize
        val bounded = if (total != null) position.coerceIn(0L, total) else position.coerceAtLeast(0L)
        synchronized(this) {
            this.position = bounded
            if (bounded < activeChunkStart || bounded >= activeChunkStart + activeChunkBytes.size) {
                prefetchJob?.cancel()
                activeChunkBytes = byteArrayOf()
                activeChunkStart = -1L
                nextDemandChunkSize = minOf(STARTUP_PROBE_CHUNK_SIZE, chunkSize)
            }
        }
        return bounded
    }

    override fun closeAsync() {
        synchronized(this) {
            if (closed) return
            closed = true
        }
        prefetchScope.cancel()
        synchronized(activeCalls) { activeCalls.toList() }.forEach(Call::cancel)
        synchronized(inFlightChunks) {
            inFlightChunks.values.toList().forEach { it.cancel() }
            inFlightChunks.clear()
        }
        synchronized(this) {
            synchronized(chunkCache) { chunkCache.clear() }
            prefetchJob?.cancel()
            activeChunkBytes = byteArrayOf()
            activeChunkStart = -1L
        }
    }

    private fun cachedChunkAt(position: Long): Pair<Long, ByteArray>? =
        synchronized(chunkCache) {
            val start = chunkCache.entries
                .firstOrNull { (start, bytes) -> position >= start && position - start < bytes.size }
                ?.key
                ?: return@synchronized null
            start to checkNotNull(chunkCache[start])
        }

    private fun fetchChunk(requestedPosition: Long): Pair<Long, ByteArray> {
        cachedChunkAt(requestedPosition)?.let { return it }

        val effectiveSize = synchronized(this) {
            val sequential = activeChunkStart >= 0L && requestedPosition == activeChunkStart + activeChunkBytes.size
            if (!sequential && hasFetchedChunk) {
                prefetchJob?.cancel()
                nextDemandChunkSize = minOf(STARTUP_PROBE_CHUNK_SIZE, chunkSize)
            }
            hasFetchedChunk = true
            nextDemandChunkSize = minOf(nextDemandChunkSize, chunkSize)
            nextDemandChunkSize
        }

        val bytes = try {
            runBlocking { getOrDownloadChunk(requestedPosition, effectiveSize, acquirePermit = false) }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw InterruptedIOException("Video chunk fetch interrupted").apply { initCause(e) }
        }

        synchronized(this) {
            if (nextDemandChunkSize == effectiveSize) {
                nextDemandChunkSize = minOf(chunkSize.toLong(), effectiveSize.toLong() * 2L).toInt()
            }
        }
        return requestedPosition to bytes
    }

    private fun triggerPrefetchLoop(currentStart: Long, currentChunkLen: Long) {
        prefetchJob?.cancel()
        val total = mediaSize
        val prefetchStarts = mutableListOf<Long>()
        var nextStart = currentStart + currentChunkLen
        for (i in 0 until PREFETCH_CHUNKS_AHEAD) {
            if (total != null && nextStart >= total) break
            val isCached = synchronized(chunkCache) { chunkCache.containsKey(nextStart) }
            if (!isCached) prefetchStarts.add(nextStart)
            nextStart += chunkSize.toLong()
        }
        if (prefetchStarts.isNotEmpty()) {
            prefetchJob = prefetchScope.launch {
                for (start in prefetchStarts) {
                    if (closed) break
                    try {
                        getOrDownloadChunk(start, chunkSize, acquirePermit = true)
                    } catch (_: CancellationException) {
                        break
                    } catch (_: Exception) {
                        break
                    }
                }
            }
        }
    }

    private suspend fun getOrDownloadChunk(
        start: Long,
        sizeToFetch: Int,
        acquirePermit: Boolean,
    ): ByteArray = coroutineScope {
        synchronized(chunkCache) { chunkCache[start] }?.let { return@coroutineScope it }

        val candidate = async(Dispatchers.IO, start = CoroutineStart.LAZY) {
            downloadChunkSuspend(start, sizeToFetch, acquirePermit).also { putChunk(start, it) }
        }
        val deferred = synchronized(inFlightChunks) {
            inFlightChunks[start]?.takeUnless { it.isCancelled } ?: candidate.also { pending ->
                inFlightChunks[start] = pending
                pending.invokeOnCompletion {
                    synchronized(inFlightChunks) {
                        if (inFlightChunks[start] === pending) inFlightChunks.remove(start)
                    }
                }
            }
        }
        if (deferred !== candidate) candidate.cancel()
        deferred.start()
        try {
            deferred.await()
        } catch (cancelled: CancellationException) {
            if (closed || !coroutineContext.isActive || deferred === candidate) throw cancelled
            getOrDownloadChunk(start, sizeToFetch, acquirePermit)
        }
    }

    private fun putChunk(start: Long, bytes: ByteArray) {
        synchronized(chunkCache) {
            if (closed) return
            chunkCache[start] = bytes
            while (chunkCache.size > MAX_CACHED_CHUNKS) {
                val iterator = chunkCache.entries.iterator()
                iterator.next()
                iterator.remove()
            }
        }
    }

    private suspend fun downloadChunkSuspend(
        start: Long,
        sizeToFetch: Int,
        acquirePermit: Boolean,
    ): ByteArray {
        val totalKnown = mediaSize
        val end = if (totalKnown != null) {
            minOf(start + sizeToFetch.toLong() - 1L, totalKnown - 1L)
        } else {
            start + sizeToFetch.toLong() - 1L
        }
        val timeoutMs = if (activeChunkStart < 0L) startupRequestTimeoutMs else requestTimeoutMs
        var lastError: Throwable? = null

        for (attempt in 0..1) {
            if (closed) break
            try {
                val bytes = withContext(Dispatchers.IO) {
                    if (acquirePermit) chunkSemaphore.acquire()
                    try {
                        val request = Request.Builder()
                            .url(url)
                            .apply { requestHeaders.forEach { (name, value) -> header(name, value) } }
                            .header("Accept", "*/*")
                            .header("Accept-Encoding", "identity")
                            .header("Range", "bytes=$start-$end")
                            .build()
                        val call = Http.client.newCall(request)
                        call.timeout().timeout(timeoutMs, TimeUnit.MILLISECONDS)
                        synchronized(activeCalls) {
                            check(!closed) { "Direct video stream is closed" }
                            activeCalls += call
                        }
                        try {
                            call.executeCancellable { response ->
                                val code = response.code
                                if (code != 200 && code != 206) {
                                    if (code in REFUSAL_CODES) {
                                        StreamResolver.onPlaybackRefused(url, code)
                                    }
                                    throw HttpDataSource.InvalidResponseCodeException(
                                        code,
                                        response.message,
                                        IOException("HTTP $code for range $start-$end"),
                                        response.headers.toMultimap(),
                                        DataSpec.Builder().setUri(url).setPosition(start).build(),
                                        ByteArray(0),
                                    )
                                }
                                if (code == 206) {
                                    val rangeHeader = response.header("Content-Range")
                                    val match = rangeHeader?.let {
                                        Regex("bytes ([0-9]+)-([0-9]+)/([0-9]+|\\*)").matchEntire(it.trim())
                                    }?.groupValues
                                    val total = match?.get(3)?.toLongOrNull()
                                    if (total != null && total > 0L) {
                                        mediaSize = total
                                    }
                                }
                                val bodyBytes = response.body.bytes()
                                if (bodyBytes.isEmpty()) {
                                    throw IOException("Empty video range response for $start-$end")
                                }
                                if (mediaSize == null && (code == 200 || bodyBytes.size < sizeToFetch)) {
                                    mediaSize = start + bodyBytes.size
                                }
                                bodyBytes
                            }
                        } finally {
                            synchronized(activeCalls) { activeCalls -= call }
                        }
                    } finally {
                        if (acquirePermit) chunkSemaphore.release()
                    }
                }
                streamFailure = null
                return bytes
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                TrackLog.w(
                    TAG,
                    "video range $start-$end error (attempt $attempt): ${e::class.simpleName}: ${e.message}",
                )
                if (closed) break
                val httpCode = (e as? HttpDataSource.InvalidResponseCodeException)?.responseCode
                if (httpCode != null && httpCode in 400..499 && httpCode != 403 && httpCode != 408) {
                    streamFailure = e
                    throw e
                }
                if (attempt < 1 && !closed) {
                    delay(250L)
                }
            }
        }

        val finalError = (lastError as? IOException) ?: IOException("Video range $start-$end failed", lastError)
        streamFailure = finalError
        throw finalError
    }

    private suspend fun Call.executeCancellable(readResponse: (Response) -> ByteArray): ByteArray =
        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { cancel() }
            try {
                val bytes = execute().use(readResponse)
                if (continuation.isActive) continuation.resume(bytes)
            } catch (error: Throwable) {
                if (continuation.isActive) continuation.resumeWithException(error)
            }
        }

    companion object {
        private const val TAG = "MusicVideo"
        private const val STARTUP_PROBE_CHUNK_SIZE = 65_536
        private const val DEFAULT_CHUNK_SIZE = 2 * 1024 * 1024
        private const val DEFAULT_STARTUP_TIMEOUT_MS = 12_000L
        private const val DEFAULT_REQUEST_TIMEOUT_MS = 20_000L
        private const val MAX_CONCURRENT_CHUNK_REQUESTS = 3
        private const val MAX_CACHED_CHUNKS = 4
        private const val PREFETCH_CHUNKS_AHEAD = 3
        private val REFUSAL_CODES = setOf(403, 404, 410)

        /** Chooses chunk size ~4 seconds of video, clamped by client range ceiling (up to 4 MiB). */
        fun chunkSizeFor(
            url: String,
            bitrate: Int?,
            rangeChunkSizeBytes: Long,
        ): Int {
            val clientLimit = minOf(
                rangeChunkSizeBytes.coerceIn(256L * 1024L, 4L * 1024L * 1024L),
                PlayerClient.rangeBytesFor(url).coerceAtLeast(512L * 1024L),
            )
            val fourSeconds = bitrate?.takeIf { it > 0 }?.let { (it.toLong() / 8L) * 4L }
            return minOf(clientLimit, fourSeconds?.coerceAtLeast(512L * 1024L) ?: clientLimit).toInt()
        }
    }
}
