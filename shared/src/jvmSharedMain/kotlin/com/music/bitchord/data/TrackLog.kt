package com.music.bitchord.data

import com.music.bitchord.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

/**
 * The app's own record of how each track came to be playing, as text you can
 * paste somewhere.
 *
 * Diagnosing why a track played from the wrong source, at the wrong bitrate,
 * or not at all has meant plugging the phone in and reading `adb logcat` — and
 * the answer is usually in a stretch lasting a few seconds that has already
 * scrolled past by the time anyone notices something sounded wrong. This keeps
 * that stretch.
 *
 * ### Why not read logcat
 *
 * The obvious implementation shells out to `logcat`, and it works. But from
 * Android 13 an app that does so trips a system consent dialog — *"Allow
 * BitChord to access all device logs?"* — which appears whenever the process
 * happens to spawn, asks for far more than this needs, and puts every other
 * app's output within reach of a paste made from a music player. None of that
 * is a reasonable price for a debug button.
 *
 * So the lines are kept here on the way past instead. Nothing is read back
 * from the system, no permission is involved, no dialog can appear, and what
 * ends up on the clipboard is only ever what this app itself wrote.
 *
 * ### What gets kept
 *
 * Only the paths that decide how a track plays: the resolver, the module
 * sandbox, the source ladder, the cache and the player. Deliberately not the
 * feeds, the artwork, the lyrics or the library — a paste that includes
 * everything is one nobody reads to the end of, and none of it has ever been
 * the answer to "why did this song sound wrong".
 *
 * ### Which lines are whose
 *
 * Every line is filed against the track it is about, and reading the log back
 * is a question about a track rather than about a stretch of time — see
 * [about] and [forTrack]. This app does most of a track's work nowhere near
 * the moment that track is playing: it is resolved while the one before it
 * plays, and the first seconds of every track are spent resolving the *next*
 * one. So "the last thirty seconds of log" is never the same thing as "this
 * song's story", and asking for one by way of the other pastes the wrong
 * song's log almost every time.
 *
 * Call [d], [w] and [e] exactly where `Log.d`/`w`/`e` would go; they forward
 * to logcat as well, so `adb logcat -s BitChord` is unchanged.
 */
object TrackLog {
    /**
     * Where each line is echoed as it is logged — logcat on the phone's debug
     * builds, the desktop's own log. The held lines are recorded regardless.
     */
    @Volatile
    var echo: ((level: Char, tag: String, message: String, error: Throwable?) -> Unit)? = null

    /** The exported header's line on which sources may stand in for YouTube, where there is one. */
    @Volatile
    var sourcesLine: () -> String? = { null }

    /** The platform's own closing lines for an exported log's header — the build, the device. */
    @Volatile
    var headerLines: () -> List<String> = { emptyList() }


    // ── Writing ─────────────────────────────────────────────────────────────

    // logcat is only worth writing to in a debug build — nothing in prod ever
    // reads it (see the class doc), so a release build skips straight to
    // record(), which is what Copy Log actually depends on.

    fun d(tag: String, message: String, about: String? = working.get()) {
        echo?.invoke('D', tag, message, null)
        record('D', message, about)
        toSession('D', tag, message, null, about)
    }

    fun i(tag: String, message: String, about: String? = working.get()) {
        echo?.invoke('I', tag, message, null)
        record('I', message, about)
        toSession('I', tag, message, null, about)
    }

    fun w(tag: String, message: String, about: String? = working.get()) {
        echo?.invoke('W', tag, message, null)
        record('W', message, about)
        toSession('W', tag, message, null, about)
    }

    fun w(tag: String, message: String, error: Throwable, about: String? = working.get()) {
        echo?.invoke('W', tag, message, error)
        record('W', "$message\n${error.stackTraceToString()}", about)
        toSession('W', tag, message, error, about)
    }

    fun e(tag: String, message: String, about: String? = working.get()) {
        echo?.invoke('E', tag, message, null)
        record('E', message, about)
        toSession('E', tag, message, null, about)
    }

    fun e(tag: String, message: String, error: Throwable, about: String? = working.get()) {
        echo?.invoke('E', tag, message, error)
        record('E', "$message\n${error.stackTraceToString()}", about)
        toSession('E', tag, message, error, about)
    }

    // ── Whose line is it ────────────────────────────────────────────────────

    /** The track whose work this thread is doing, if it is doing any. */
    private val working = ThreadLocal<String?>()

    /**
     * A coroutine context that files everything logged inside it against [id].
     *
     *     scope.async(Dispatchers.IO + TrackLog.about(videoId)) { … }
     *
     * The alternative is passing an id down to every call that logs, and the
     * lines worth having are exactly the ones furthest from anyone who knows
     * which track they are for: a fetch inside a QuickJS export inside a module
     * search inside a source ladder. None of those layers has any other use for
     * a track id, and threading one through all of them to serve a debug button
     * would be a worse trade than the button is worth.
     *
     * Carried as a [kotlinx.coroutines.ThreadContextElement] rather than a bare
     * thread local because that work hops threads constantly —
     * `withContext(IO)` for a fetch, `Dispatchers.Default` for the JS engine —
     * and this follows it, including into every child coroutine.
     */
    fun about(id: String?): CoroutineContext = working.asContextElement(id)

    private class Line(val at: Long, val level: Char, val text: String, val track: String?)

    private val lines = ArrayDeque<Line>()

    /** Total characters held, so the buffer is bounded by size rather than by count. */
    private var held = 0

    /**
     * Bounded by bytes rather than by line count: one `callExport result` line
     * carrying a search response is worth several hundred ordinary lines, and a
     * limit that counts them the same either wastes memory or throws away the
     * history that matters.
     */
    private fun record(level: Char, message: String, about: String?) {
        val text = if (message.length > MAX_LINE_CHARS) {
            message.take(MAX_LINE_CHARS) + "…(${message.length - MAX_LINE_CHARS} more)"
        } else {
            message
        }
        synchronized(lines) {
            lines.addLast(Line(System.currentTimeMillis(), level, text, about))
            held += text.length
            while (held > MAX_HELD_CHARS && lines.isNotEmpty()) {
                held -= lines.removeFirst().text.length
            }
        }
    }

    // ── Recording a session ─────────────────────────────────────────────────

    private val sessionLock = Any()
    private var session: java.io.Writer? = null
    private var sessionFile: java.io.File? = null
    private var sessionLines = 0

    private val _recording = MutableStateFlow(false)

    /** Whether a session is being written right now. */
    val recording: StateFlow<Boolean> = _recording.asStateFlow()

    /**
     * Starts writing every logged line to [file], after [header]. A session
     * already running is ended first, its file kept.
     */
    fun startSession(file: java.io.File, header: List<String> = emptyList(), append: Boolean = false) {
        synchronized(sessionLock) {
            closeSession()
            file.parentFile?.mkdirs()
            val carriedOn = append && file.length() > 0L
            val writer = java.io.FileOutputStream(file, carriedOn).bufferedWriter()
            if (carriedOn) writer.write("\n")
            header.forEach { writer.write(it); writer.write("\n") }
            writer.write("${if (carriedOn) "carried on" else "started"}: ${STAMP.format(Date())}\n\n")
            writer.flush()
            session = writer
            sessionFile = file
            sessionLines = 0
            _recording.value = true
        }
    }

    /** Ends the session, returning its file — or null when none was running. */
    fun stopSession(): java.io.File? = synchronized(sessionLock) {
        val file = sessionFile ?: return@synchronized null
        session?.runCatching {
            write("\nstopped: ${STAMP.format(Date())} ($sessionLines lines)\n")
        }
        closeSession()
        file
    }

    private fun closeSession() {
        session?.runCatching { close() }
        session = null
        sessionFile = null
        _recording.value = false
    }

    /**
     * One line into the running session, if there is one. No-op when [_recording] is false.
     */
    fun toSession(level: Char, tag: String, message: String, error: Throwable? = null, about: String? = null) {
        if (!_recording.value) return
        val at = System.currentTimeMillis()
        val body = buildString {
            append(' ').append(level).append('/').append(tag)
            if (about != null) append(" {").append(about).append('}')
            append(": ").append(message)
            if (error != null) append('\n').append(error.stackTraceToString())
            append('\n')
        }
        synchronized(sessionLock) {
            val writer = session ?: return
            try {
                writer.write(SESSION_CLOCK.format(Date(at)))
                writer.write(body)
                writer.flush()
                sessionLines++
            } catch (_: java.io.IOException) {
                closeSession()
            }
        }
    }

    // ── Reading ─────────────────────────────────────────────────────────────

    /**
     * Wall-clock times at which each track became the current one.
     *
     * Only a floor for tracks with no lines of their own, now that lines say
     * which track they are about: a track served whole from the disk cache is
     * resolved by nobody and would otherwise have no start at all.
     */
    private val startedAt = ConcurrentHashMap<String, Long>()

    fun onTrackStarted(videoId: String) {
        if (startedAt.size >= MAX_REMEMBERED) startedAt.clear()
        startedAt[videoId] = System.currentTimeMillis()
    }

    /**
     * The log for [song]: the lines about that track, plus the ones about
     * nothing in particular, from where its own story starts.
     *
     * Both halves of that are the fix for the same bug, and a plain time window
     * gets both of them wrong:
     *
     *  - **Where it starts.** A track is resolved while the track *before* it
     *    is still playing — that is what read-ahead is — so the resolve that
     *    decides its source, its bitrate and whether it plays at all sits
     *    minutes earlier than the moment the queue reached it. No window
     *    measured back from the selection reaches that.
     *  - **What is in it.** The first seconds of every track are spent
     *    resolving the next one, so a window running from the selection to now
     *    is largely the *following* song's story: its ladder, its client walk,
     *    its read-ahead. That is what a paste taken a few seconds into a track
     *    was almost entirely made of.
     *
     * Falling back to everything held is still the right way to be wrong for a
     * track nothing was ever filed against — one served whole from the disk
     * cache, or the track a cold start resumes on.
     */
    suspend fun forTrack(song: Song, stats: NerdStats.Snapshot?): String = withContext(Dispatchers.Default) {
        val held = synchronized(lines) { lines.toList() }
        val from = listOfNotNull(
            held.firstOrNull { it.track == song.videoId }?.at,
            startedAt[song.videoId]?.minus(LEAD_IN_MS),
        ).minOrNull()
        val since = held.filter { from == null || it.at >= from }
        val window = since.filter { it.track == null || it.track == song.videoId }
        header(song, stats, from, window.size, since.size - window.size) + "\n" +
            window.joinToString("\n") { "${CLOCK.format(Date(it.at))} ${it.level} ${it.text}" } +
            "\n"
    }

    // ── The part that isn't the log ─────────────────────────────────────────

    /**
     * What the lines alone can't say: which build produced them, on what, and
     * what the player believed it was playing when the log was taken.
     *
     * @param elsewhere how many lines in the same stretch belonged to another
     *   track and were left out. Stated rather than silently dropped: it is the
     *   difference between "nothing happened" and "nothing happened *to this
     *   track*", and the two send a reader looking in opposite places.
     */
    private fun header(
        song: Song,
        stats: NerdStats.Snapshot?,
        from: Long?,
        count: Int,
        elsewhere: Int,
    ) = buildString {
        appendLine("BitChord log — ${song.title} — ${song.artist}")
        appendLine("id=${song.videoId} duration=${song.durationText ?: "?"} album=${song.albumName ?: "?"}")
        appendLine("playing: ${stats.describe()}")
        sourcesLine()?.let(::appendLine)
        appendLine(
            "window: ${from?.let { CLOCK.format(Date(it)) } ?: "everything held"} → " +
                "${CLOCK.format(Date())} ($count lines" +
                (if (elsewhere > 0) ", $elsewhere for other tracks left out)" else ")"),
        )
        headerLines().forEach(::appendLine)
    }

    private fun NerdStats.Snapshot?.describe(): String {
        if (this == null) return "nothing reported"
        val measured = listOfNotNull(
            mimeType,
            bitDepth?.let { "$it-bit" },
            bitrateKbps?.let { "$it kbps" },
            sampleRateHz?.let { "$it Hz" },
            channels?.let { "${it}ch" },
        ).joinToString(" · ").ifEmpty { "nothing reported" }
        val promised = claimed?.summary?.let { " (source said: $it)" }.orEmpty()
        val tier = when {
            isHiRes -> " [Hi-Res Lossless]"
            isLossless -> " [Lossless]"
            isHiQuality -> " [Hi-Quality]"
            else -> ""
        }
        return measured + promised + tier
    }

    private val CLOCK = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private val SESSION_CLOCK = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private val STAMP = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US)

    /**
     * How far back of a track's selection to reach when nothing was ever filed
     * against it — see [startedAt].
     *
     * Only a fallback now. It used to be the whole of the window, on the
     * reasoning that the resolve runs a moment before the player reports the
     * item as current; what it actually reaches back into is the *previous*
     * track's playback, and what the track being asked about spent it doing is
     * usually nothing.
     */
    private const val LEAD_IN_MS = 20_000L

    /** Roughly the last few tracks' worth, and small enough to hold without thinking about it. */
    private const val MAX_HELD_CHARS = 512_000

    /** Enough for a stack trace or a search response's opening; not a whole catalogue page. */
    private const val MAX_LINE_CHARS = 2_000

    private const val MAX_REMEMBERED = 32
}
