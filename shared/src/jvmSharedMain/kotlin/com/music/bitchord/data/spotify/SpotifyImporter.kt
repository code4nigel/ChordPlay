package com.music.bitchord.data.spotify

import com.music.bitchord.data.Http
import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.model.SearchFilter
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.sources.TrackMatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import java.util.concurrent.atomic.AtomicInteger

/** Single track metadata extracted from a Spotify playlist. */
data class SpotifyImportTrack(
    val title: String,
    val artist: String,
)

/** The result of fetching and resolving a Spotify playlist's tracks. */
data class SpotifyImportResult(
    val title: String,
    val description: String,
    val thumbnailUrl: String?,
    val totalTracks: Int,
    val resolvedVideoIds: List<String>,
    val unmatchedTracks: List<SpotifyImportTrack>,
)

object SpotifyImporter {

    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /** A Spotify playlist id is 22 base-62 characters. */
    private val PLAYLIST_ID = Regex("""[A-Za-z0-9]{22}""")

    private fun isSpotifyHost(host: String) =
        host == "spotify.com" || host.endsWith(".spotify.com")

    /** Parses a raw user string or link into a Spotify playlist ID if valid. */
    fun extractPlaylistId(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        if (trimmed.startsWith("spotify:playlist:")) {
            val id = trimmed.removePrefix("spotify:playlist:").substringBefore("?").substringBefore("/")
            return id.takeIf { PLAYLIST_ID.matches(it) }
        }

        val uri = runCatching { java.net.URI(trimmed) }.getOrNull() ?: return null
        if (uri.scheme?.lowercase() !in setOf("http", "https")) return null
        if (!isSpotifyHost(uri.host?.lowercase().orEmpty())) return null
        val segments = uri.path.orEmpty().split('/').filter { it.isNotEmpty() }
        val playlistIdx = segments.indexOf("playlist")
        if (playlistIdx == -1 || playlistIdx + 1 >= segments.size) return null
        return segments[playlistIdx + 1].takeIf { PLAYLIST_ID.matches(it) }
    }

    /**
     * Fetches public Spotify playlist details from Spotify's embed endpoint.
     */
    suspend fun fetchPlaylistTracks(playlistId: String): Pair<String, List<SpotifyImportTrack>> =
        withContext(Dispatchers.IO) {
            // 1. Direct GraphQL query via official SpotifyLibrary if authenticated
            if (SpotifyLibrary.auth != null) {
                val directResult = runCatching {
                    val title = SpotifyLibrary.playlistTitle(playlistId)?.takeIf { it.isNotBlank() }
                        ?: "Spotify Playlist"
                    val tracks = SpotifyLibrary.tracks(playlistId)
                    if (tracks.isNotEmpty()) {
                        Pair(title, tracks.map { SpotifyImportTrack(title = it.title, artist = it.artist) })
                    } else null
                }.getOrNull()
                if (directResult != null) return@withContext directResult
            }

            // 2. Fallback: Parse embed page HTML
            val url = "https://open.spotify.com/embed/playlist/$playlistId"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            val html = Http.client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful || body.isBlank()) {
                    error("Failed to load Spotify playlist (HTTP ${response.code})")
                }
                body
            }

            val parsed = parseEmbedHtml(html, playlistId)
            if (parsed != null && parsed.second.isNotEmpty()) {
                return@withContext parsed
            }

            error("Could not parse Spotify playlist metadata. Make sure the playlist is public.")
        }

    private fun parseEmbedHtml(html: String, playlistId: String): Pair<String, List<SpotifyImportTrack>>? {
        // A. Try __NEXT_DATA__
        val nextDataRegex = Regex("""<script[^>]*id=["']__NEXT_DATA__["'][^>]*>(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
        val nextDataMatch = nextDataRegex.find(html)
        if (nextDataMatch != null) {
            val jsonString = nextDataMatch.groupValues[1]
            val parsed = runCatching { parseNextDataJson(jsonString, playlistId) }.getOrNull()
            if (parsed != null && parsed.second.isNotEmpty()) return parsed
        }

        // B. Try application/ld+json scripts
        val ldJsonRegex = Regex("""<script[^>]*type=["']application/ld\+json["'][^>]*>(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
        for (match in ldJsonRegex.findAll(html)) {
            val ldString = match.groupValues[1]
            val parsed = runCatching { parseLdJson(ldString) }.getOrNull()
            if (parsed != null && parsed.second.isNotEmpty()) return parsed
        }

        // C. Try generic application/json scripts
        val anyJsonRegex = Regex("""<script[^>]*type=["']application/json["'][^>]*>(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
        for (match in anyJsonRegex.findAll(html)) {
            val matchContent = match.groupValues[1]
            if (matchContent.contains("trackList") || matchContent.contains("tracks")) {
                val parsed = runCatching { parseGenericJson(matchContent) }.getOrNull()
                if (parsed != null && parsed.second.isNotEmpty()) return parsed
            }
        }

        return null
    }

    private fun parseNextDataJson(jsonString: String, playlistId: String): Pair<String, List<SpotifyImportTrack>> {
        val root = json.parseToJsonElement(jsonString).jsonObject
        val props = root["props"]?.jsonObject?.get("pageProps")?.jsonObject
        val stateData = props?.get("state")?.jsonObject
            ?.get("data")?.jsonObject
            ?.get("entity")?.jsonObject
            ?: props?.get("entity")?.jsonObject
            ?: root["state"]?.jsonObject?.get("data")?.jsonObject?.get("entity")?.jsonObject

        val playlistTitle = stateData?.get("name")?.jsonPrimitive?.contentOrNull
            ?: stateData?.get("title")?.jsonPrimitive?.contentOrNull
            ?: "Imported Spotify Playlist"

        val trackListJson = stateData?.get("trackList")?.jsonArray
            ?: stateData?.get("tracks")?.jsonArray
            ?: JsonArray(emptyList())

        val tracks = mutableListOf<SpotifyImportTrack>()
        for (element in trackListJson) {
            val obj = element.jsonObject
            val title = obj["title"]?.jsonPrimitive?.contentOrNull
                ?: obj["name"]?.jsonPrimitive?.contentOrNull
                ?: continue
            val subtitle = obj["subtitle"]?.jsonPrimitive?.contentOrNull
                ?: obj["artists"]?.jsonArray?.joinToString(", ") {
                    it.jsonObject["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
                }
                ?: ""
            if (title.isNotBlank()) {
                tracks.add(SpotifyImportTrack(title = title.trim(), artist = subtitle.trim()))
            }
        }

        if (tracks.size >= EMBED_TRACK_LIMIT) {
            val token = props?.get("state")?.jsonObject
                ?.get("settings")?.jsonObject
                ?.get("session")?.jsonObject
                ?.get("accessToken")?.jsonPrimitive?.contentOrNull
            if (token != null) {
                runCatching { fetchRemainingTracks(playlistId, token, tracks.size) }
                    .getOrNull()
                    ?.let { tracks.addAll(it) }
            }
        }

        return Pair(playlistTitle, tracks)
    }

    private fun parseLdJson(jsonString: String): Pair<String, List<SpotifyImportTrack>>? {
        val root = json.parseToJsonElement(jsonString).jsonObject
        val type = root["@type"]?.jsonPrimitive?.contentOrNull
        if (type != null && !type.contains("Playlist", ignoreCase = true)) return null
        val playlistTitle = root["name"]?.jsonPrimitive?.contentOrNull ?: "Spotify Playlist"
        val trackList = root["track"]?.jsonArray ?: root["tracks"]?.jsonArray ?: return null
        val tracks = mutableListOf<SpotifyImportTrack>()
        for (item in trackList) {
            val obj = item.jsonObject
            val title = obj["name"]?.jsonPrimitive?.contentOrNull ?: continue
            val artist = when (val byArtist = obj["byArtist"]) {
                is JsonArray -> byArtist.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.contentOrNull }.joinToString(", ")
                is JsonObject -> byArtist["name"]?.jsonPrimitive?.contentOrNull
                else -> null
            } ?: ""
            if (title.isNotBlank()) {
                tracks.add(SpotifyImportTrack(title = title.trim(), artist = artist.trim()))
            }
        }
        return if (tracks.isNotEmpty()) Pair(playlistTitle, tracks) else null
    }

    private fun parseGenericJson(jsonString: String): Pair<String, List<SpotifyImportTrack>>? {
        val root = json.parseToJsonElement(jsonString).jsonObject
        val entity = root["entity"]?.jsonObject ?: root["data"]?.jsonObject ?: root
        val title = entity["name"]?.jsonPrimitive?.contentOrNull
            ?: entity["title"]?.jsonPrimitive?.contentOrNull
            ?: "Spotify Playlist"
        val trackList = entity["trackList"]?.jsonArray ?: entity["tracks"]?.jsonArray ?: return null
        val tracks = mutableListOf<SpotifyImportTrack>()
        for (element in trackList) {
            val obj = element.jsonObject
            val trackTitle = obj["title"]?.jsonPrimitive?.contentOrNull
                ?: obj["name"]?.jsonPrimitive?.contentOrNull
                ?: continue
            val artist = obj["subtitle"]?.jsonPrimitive?.contentOrNull
                ?: obj["artist"]?.jsonPrimitive?.contentOrNull
                ?: ""
            if (trackTitle.isNotBlank()) {
                tracks.add(SpotifyImportTrack(title = trackTitle.trim(), artist = artist.trim()))
            }
        }
        return if (tracks.isNotEmpty()) Pair(title, tracks) else null
    }

    /**
     * The YouTube Music song that is [track], or null if there is none: the
     * best candidate by title, artist, length and album, and failing that the
     * search's first hit.
     */
    suspend fun matchTrack(track: SpotifyTrack): Song? {
        val query = listOf(track.title, track.artist).filter { it.isNotBlank() }.joinToString(" ")
        val candidates = YtMusicRepository.search(query, SearchFilter.SONGS).getOrNull()
            ?.filterIsInstance<SearchResult.Track>()
            ?.map { it.song }
            .orEmpty()
        return TrackMatcher.best(
            candidates,
            TrackMatcher.Target(
                title = track.title,
                artist = track.artist,
                durationSec = track.durationMs.takeIf { it > 0 }?.div(1000),
                album = track.album,
            ),
        ) ?: candidates.firstOrNull()
    }

    /** Tracks the embed page lists before it cuts off. */
    private const val EMBED_TRACK_LIMIT = 100
    private const val PAGE_SIZE = 100
    private const val PATHFINDER_URL = "https://api-partner.spotify.com/pathfinder/v1/query"
    private const val FETCH_PLAYLIST_HASH = "19ff1327c29e99c208c86d7a9d8f1929cfdf3d3202a0ff4253c821f1901aa94d"

    /** Tracks from [start] to the end of the playlist, a page at a time. */
    private fun fetchRemainingTracks(
        playlistId: String,
        token: String,
        start: Int,
    ): List<SpotifyImportTrack> {
        val out = mutableListOf<SpotifyImportTrack>()
        var offset = start
        while (true) {
            val variables = """{"uri":"spotify:playlist:$playlistId","offset":$offset,"limit":$PAGE_SIZE,"enableWatchFeedEntrypoint":false}"""
            val extensions = """{"persistedQuery":{"version":1,"sha256Hash":"$FETCH_PLAYLIST_HASH"}}"""
            val url = PATHFINDER_URL.toHttpUrl().newBuilder()
                .addQueryParameter("operationName", "fetchPlaylist")
                .addQueryParameter("variables", variables)
                .addQueryParameter("extensions", extensions)
                .build()
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Authorization", "Bearer $token")
                .header("App-platform", "WebPlayer")
                .header("Accept", "application/json")
                .build()
            val body = Http.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}")
                response.body?.string().orEmpty()
            }
            val content = json.parseToJsonElement(body).jsonObject["data"]?.jsonObject
                ?.get("playlistV2")?.jsonObject?.get("content")?.jsonObject
                ?: break
            val items = content["items"]?.jsonArray ?: break
            for (item in items) {
                val data = item.jsonObject["itemV2"]?.jsonObject?.get("data")?.jsonObject ?: continue
                val title = data["name"]?.jsonPrimitive?.content?.trim().orEmpty()
                if (title.isEmpty()) continue
                val artist = data["artists"]?.jsonObject?.get("items")?.jsonArray
                    ?.mapNotNull { it.jsonObject["profile"]?.jsonObject?.get("name")?.jsonPrimitive?.content }
                    ?.joinToString(", ")
                    .orEmpty()
                out.add(SpotifyImportTrack(title = title, artist = artist))
            }
            val total = content["totalCount"]?.jsonPrimitive?.content?.toIntOrNull() ?: break
            offset += items.size
            if (items.isEmpty() || offset >= total) break
        }
        return out
    }

    /**
     * Resolves a list of Spotify tracks to full Song objects using Innertube search.
     * Reports real-time progress via [onProgress].
     */
    suspend fun resolveToSongs(
        tracks: List<SpotifyImportTrack>,
        onProgress: (completed: Int, total: Int) -> Unit,
    ): Pair<List<Song>, List<SpotifyImportTrack>> = coroutineScope {
        val total = tracks.size
        val completedCount = AtomicInteger(0)
        val semaphore = Semaphore(4) // Bounded concurrency for search queries

        val deferredResults = tracks.map { track ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    val query = "${track.title} ${track.artist}".trim()
                    val searchResult = YtMusicRepository.search(query, SearchFilter.SONGS).getOrNull()
                    val matchedSong = searchResult?.filterIsInstance<SearchResult.Track>()
                        ?.firstOrNull()?.song

                    val done = completedCount.incrementAndGet()
                    onProgress(done, total)

                    if (matchedSong != null) {
                        Pair(matchedSong, null)
                    } else {
                        Pair(null, track)
                    }
                }
            }
        }

        val results = deferredResults.awaitAll()
        val songs = results.mapNotNull { it.first }
        val unmatched = results.mapNotNull { it.second }

        Pair(songs, unmatched)
    }
}
