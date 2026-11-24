package com.music.bitchord.data.canvas

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * How Spotify's web player asks for a track's Canvas today: the `canvas` GraphQL query on
 * Pathfinder v2, rather than the `canvaz-cache` protobuf endpoint on spclient.
 *
 * With a valid cookie the Spotify source had stopped finding canvases, even for tracks that show
 * one in Spotify's own apps; asking the way the current web player does finds them again.
 * `canvaz-cache` is kept as the fallback for when this query fails outright. This is the
 * request/response half of the new route, kept free of any HTTP client so Android and desktop
 * share it and it can be tested without a network; each app sends the request with its own client.
 *
 * Like every Pathfinder operation, the query is named by a persisted-query hash that changes when
 * Spotify rebuilds the web player. [KNOWN_CANVAS_HASH] is the last one known to work; the live one
 * is read out of the web player's own scripts by [QueryHashes], so a rebuild doesn't silently take
 * the source down again.
 */
object SpotifyCanvasQuery {

    const val ENDPOINT = "https://api-partner.spotify.com/pathfinder/v2/query"
    const val OPERATION = "canvas"
    const val SEARCH_TRACKS_OPERATION = "searchTracks"

    /** The `canvas` query's hash as of January 2026. */
    const val KNOWN_CANVAS_HASH = "575138ab27cd5c1b3e54da54d0a7cc8d85485402de26340c2145f0f6bb5e7a9f"

    /** Fallback persisted query hash for the web player's `searchTracks` operation. */
    const val KNOWN_SEARCH_TRACKS_HASH = "bc1ca2fcd0ba1013a0fc88e6cc4f190af501851e3dafd3e1ef85840297694428"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** The JSON body for `POST` [ENDPOINT]. */
    fun requestBody(trackUri: String, hash: String): String = buildJsonObject {
        put("operationName", OPERATION)
        putJsonObject("variables") { put("trackUri", trackUri) }
        putJsonObject("extensions") {
            putJsonObject("persistedQuery") {
                put("version", 1)
                put("sha256Hash", hash)
            }
        }
    }.toString()

    /** What an answer to the query said. */
    sealed interface Answer {
        /** The track has a canvas at [url]. */
        data class Found(val url: String, val type: String?) : Answer

        /**
         * Spotify answered the query without a video URL this can play. [detail] says what came
         * back instead (no canvas at all, or one whose shape isn't a plain `.mp4`), for the log.
         */
        data class NoCanvas(val detail: String) : Answer

        /**
         * The query itself didn't work -- unparseable, GraphQL errors and no data (a stale hash
         * reads as "PersistedQueryNotFound"). Callers fall back to the older route; [staleHash]
         * tells them the hash is worth looking up again first.
         */
        data class Failed(val reason: String, val staleHash: Boolean = false) : Answer
    }

    /** Reads `data.trackUnion.canvas.url` out of an answer. */
    fun parse(body: String): Answer {
        val root = runCatching { json.parseToJsonElement(body) as? JsonObject }.getOrNull()
            ?: return Answer.Failed("not JSON")
        val errors = (root["errors"] as? JsonArray)?.mapNotNull { (it as? JsonObject)?.text("message") }.orEmpty()
        val data = root["data"] as? JsonObject
        if (data == null) {
            val reason = errors.joinToString("; ").ifBlank { "no data" }
            return Answer.Failed(reason, staleHash = errors.any { it.contains("PersistedQueryNotFound", ignoreCase = true) })
        }
        val canvas = (data["trackUnion"] as? JsonObject)?.get("canvas") as? JsonObject
            ?: return Answer.NoCanvas("canvas null")
        // The web player itself plays video canvases from `fileId` through its own video player
        // and only reads `url` for images and GIFs, so a video canvas can come back without the
        // plain MP4 URL this client expects. Describe it rather than collapse it into "none".
        val detail = "type=${canvas.text("type")} fileId=${canvas.text("fileId")} url=${canvas.text("url")}"
        val url = canvas.text("url")?.takeIf { it.startsWith("https://") } ?: return Answer.NoCanvas(detail)
        // Some canvases are still images; this plays video only.
        if (!VIDEO_URL.containsMatchIn(url)) return Answer.NoCanvas(detail)
        return Answer.Found(url, canvas.text("type"))
    }

    private val VIDEO_URL = Regex("""\.mp4(?:[?#]|$)""", RegexOption.IGNORE_CASE)

    // ---- The live hash --------------------------------------------------

    /** `"canvas","query","<64 hex>"`, the way the web player's bundle declares its operations. */
    private val OPERATION_DECLARATION =
        Regex("""["'](\w+)["']\s*,\s*["']query["']\s*,\s*["']([0-9a-f]{64})["']""")

    /** [operation]'s hash as declared in a piece of the web player's JavaScript, if it's there. */
    fun findQueryHash(script: String, operation: String = OPERATION): String? =
        OPERATION_DECLARATION.findAll(script).firstOrNull { it.groupValues[1] == operation }?.groupValues?.get(2)

    /** The web player's own script bundles, as linked from open.spotify.com's page. */
    fun webPlayerScripts(html: String): List<String> =
        WEB_PLAYER_SCRIPT.findAll(html).map { it.value }.distinct().toList()

    private val WEB_PLAYER_SCRIPT =
        Regex("""https://open\.spotifycdn\.com/cdn/build/web-player/[A-Za-z0-9._~-]+\.js""")

    /**
     * The current `canvas` hash, read off the web player's scripts and remembered for a while.
     *
     * [fetch] is the app's own GET (URL in, body out, null on failure), so this holds no HTTP
     * client of its own. Falls back to [KNOWN_CANVAS_HASH] whenever the scripts can't be read or
     * no longer declare it where expected, and doesn't retry a failed lookup on every track.
     */
    class QueryHashes(
        private val fetch: (String) -> String?,
        private val now: () -> Long = System::currentTimeMillis,
        private val maxScripts: Int = 8,
    ) {
        private class Entry(var hash: String? = null, var checkedAt: Long = 0L)

        private val entries = mutableMapOf<String, Entry>()

        @Synchronized
        fun canvasHash(forceRefresh: Boolean = false): String =
            hashFor(OPERATION, KNOWN_CANVAS_HASH, forceRefresh)

        @Synchronized
        fun hashFor(operation: String, fallback: String, forceRefresh: Boolean = false): String {
            val state = entries.getOrPut(operation) { Entry() }
            val current = state.hash
            val age = now() - state.checkedAt
            if (!forceRefresh && state.checkedAt != 0L && age < (if (current != null) FOUND_TTL_MS else MISSED_TTL_MS)) {
                return current ?: fallback
            }
            state.checkedAt = now()
            val found = lookUp(operation)
            if (found != null) state.hash = found
            return state.hash ?: fallback
        }

        private fun lookUp(operation: String): String? {
            val html = fetch("https://open.spotify.com/") ?: return null
            for (script in webPlayerScripts(html).take(maxScripts)) {
                val body = fetch(script) ?: continue
                findQueryHash(body, operation)?.let { return it }
            }
            return null
        }

        private companion object {
            const val FOUND_TTL_MS = 12L * 60 * 60 * 1000
            const val MISSED_TTL_MS = 30L * 60 * 1000
        }
    }

    private fun JsonObject.text(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeUnless { it is JsonNull }?.contentOrNull
}
