/*
 * Copyright (C) 2026 Shivanshu (code4nigel) <https://github.com/code4nigel/ChordPlay>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.music.bitchord.data.taste

import com.music.bitchord.data.DebugLog
import com.music.bitchord.data.Http
import com.music.bitchord.data.innertube.InnertubeParser
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/**
 * 3-tier non-blocking metadata tagging pipeline:
 *
 * 1. **Tier 1 (Source-Tagging):** Immediate 0-cost tagging from YTM `FEmusic_moods_and_genres`
 *    or targeted preference queries.
 * 2. **Tier 2 (On-Device Heuristics):** 0-ms offline Unicode script + label/keyword inference.
 * 3. **Tier 3 (Async Keyless Enrichment):** Background `Dispatchers.IO` lookup using
 *    **iTunes Search API** (fast, keyless, strong Indian regional + global genre coverage) and
 *    **MusicBrainz WS/2** (keyless open community DB for genres + artist region/language).
 *    Deliberately does *not* depend on Last.fm API calls (due to Dec 1, 2026 policy changes).
 */
object TagResolver {

    private const val TAG = "TagResolver"
    private const val USER_AGENT = "ChordPlay/2.2.9 (https://github.com/code4nigel/ChordPlay)"
    private const val ITUNES_URL = "https://itunes.apple.com/search"
    private const val MUSICBRAINZ_URL = "https://musicbrainz.org/ws/2/artist/"
    private const val MUSICBRAINZ_SPACING_MS = 1_150L
    private const val WORKER_IDLE_SPACING_MS = 350L

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private data class EnrichRequest(
        val song: Song,
        val artistKey: String,
    )

    private val inFlight = ConcurrentHashMap.newKeySet<String>()
    private val sessionAttempted = ConcurrentHashMap.newKeySet<String>()
    private val requests = Channel<EnrichRequest>(Channel.UNLIMITED)

    @Volatile
    private var lastMusicBrainzCallAt = 0L

    /** Optional local-only cached artist genre provider (reads already-saved ArtistFacts without network). */
    @Volatile
    var localArtistTagsProvider: ((String) -> List<String>)? = null

    init {
        scope.launch { workerLoop() }
    }

    // ── Tier 1: Source-Tagging ──────────────────────────────────────────────

    fun recordSourceTags(
        items: List<ShelfItem>,
        langMask: Int = 0,
        genreMask: Int = 0,
        themeMask: Int = 0,
    ) {
        if (items.isEmpty() || (langMask == 0 && genreMask == 0 && themeMask == 0)) return
        for (item in items) {
            item.videoId?.takeIf { it.isNotBlank() }?.let { vid ->
                TasteProfileRepository.putMediaTags(
                    entityKey = TasteConstants.songCacheKey(vid),
                    langMask = langMask,
                    genreMask = genreMask,
                    themeMask = themeMask,
                    source = TagSource.SOURCE,
                )
            }
            for (aid in item.artistIds) {
                if (aid.isNotBlank()) {
                    TasteProfileRepository.putMediaTags(
                        entityKey = TasteConstants.artistCacheKey(aid),
                        langMask = langMask,
                        genreMask = genreMask,
                        themeMask = themeMask,
                        source = TagSource.SOURCE,
                    )
                }
            }
            val artistName = InnertubeParser.artistFromSubtitle(item.subtitle)
            if (artistName.isNotBlank()) {
                TasteProfileRepository.putMediaTags(
                    entityKey = TasteConstants.artistCacheKey(artistName),
                    langMask = langMask,
                    genreMask = genreMask,
                    themeMask = themeMask,
                    source = TagSource.SOURCE,
                )
            }
        }
    }

    fun recordSourceTagsForSongs(
        songs: List<Song>,
        langMask: Int = 0,
        genreMask: Int = 0,
        themeMask: Int = 0,
    ) {
        if (songs.isEmpty() || (langMask == 0 && genreMask == 0 && themeMask == 0)) return
        for (song in songs) {
            if (song.videoId.isNotBlank()) {
                TasteProfileRepository.putMediaTags(
                    entityKey = TasteConstants.songCacheKey(song.videoId),
                    langMask = langMask,
                    genreMask = genreMask,
                    themeMask = themeMask,
                    source = TagSource.SOURCE,
                )
            }
            song.artistId?.takeIf { it.isNotBlank() }?.let { aid ->
                TasteProfileRepository.putMediaTags(
                    entityKey = TasteConstants.artistCacheKey(aid),
                    langMask = langMask,
                    genreMask = genreMask,
                    themeMask = themeMask,
                    source = TagSource.SOURCE,
                )
            }
            if (song.artist.isNotBlank()) {
                TasteProfileRepository.putMediaTags(
                    entityKey = TasteConstants.artistCacheKey(song.artist),
                    langMask = langMask,
                    genreMask = genreMask,
                    themeMask = themeMask,
                    source = TagSource.SOURCE,
                )
            }
        }
    }

    fun recordCategoryTitle(items: List<ShelfItem>, categoryTitle: String) {
        val masks = TagNormalizer.normalizeTag(categoryTitle)
        if (!masks.isEmpty) {
            recordSourceTags(items, masks.langMask, masks.genreMask, masks.themeMask)
        }
    }

    // ── Tier 2 + Cache Immediate Resolution ─────────────────────────────────

    fun resolveImmediate(song: Song): ResolvedTags {
        val songCached = song.videoId.takeIf { it.isNotBlank() }
            ?.let { TasteProfileRepository.getMediaTags(TasteConstants.songCacheKey(it)) }
            ?.resolved()
            ?: ResolvedTags()

        val artistIdCached = song.artistId?.takeIf { it.isNotBlank() }
            ?.let { TasteProfileRepository.getMediaTags(TasteConstants.artistCacheKey(it)) }
            ?.resolved()
            ?: ResolvedTags()

        val artistNameCached = song.artist.takeIf { it.isNotBlank() }
            ?.let { TasteProfileRepository.getMediaTags(TasteConstants.artistCacheKey(it)) }
            ?.resolved()
            ?: ResolvedTags()

        val localCached = localArtistTagsProvider?.invoke(song.artist)
            ?.takeIf { it.isNotEmpty() }
            ?.let { TagNormalizer.normalizeAll(it) }
            ?.let { ResolvedTags(it.langMask, it.genreMask, it.themeMask, TagSource.LOCAL_CACHE) }
            ?: ResolvedTags()

        val heuristic = TagHeuristics.infer(
            title = song.title,
            artist = song.artist,
            albumName = song.albumName,
        )

        return songCached + artistIdCached + artistNameCached + localCached + heuristic
    }

    fun resolveImmediate(item: ShelfItem): ResolvedTags {
        val songCached = item.videoId?.takeIf { it.isNotBlank() }
            ?.let { TasteProfileRepository.getMediaTags(TasteConstants.songCacheKey(it)) }
            ?.resolved()
            ?: ResolvedTags()

        var artistCached = ResolvedTags()
        for (aid in item.artistIds) {
            TasteProfileRepository.getMediaTags(TasteConstants.artistCacheKey(aid))?.let {
                artistCached += it.resolved()
            }
        }
        val artistName = InnertubeParser.artistFromSubtitle(item.subtitle)
        if (artistName.isNotBlank()) {
            TasteProfileRepository.getMediaTags(TasteConstants.artistCacheKey(artistName))?.let {
                artistCached += it.resolved()
            }
        }

        val heuristic = TagHeuristics.infer(
            title = item.title,
            artist = artistName,
            subtitle = item.subtitle,
        )

        return songCached + artistCached + heuristic
    }

    /**
     * Produces structured display labels for the Chord Wave Tune Sheet.
     * Triggers async Tier-3 enrichment if Genre or Language is still missing.
     */
    fun songDetails(song: Song, snapshot: TasteSnapshot): SongTagDetails {
        val tags = resolveImmediate(song)
        val artistName = song.artist.ifBlank { "Unknown Artist" }
        val artistKey = song.artistId?.takeIf { it.isNotBlank() }?.let(TasteConstants::normalizeArtistKey)
            ?: TasteConstants.normalizeArtistKey(artistName)

        val languages = tags.languages()
        val genres = tags.genres()
        val themes = tags.themes()

        val bestLang = languages.maxByOrNull { snapshot.weightForTag(it) }
        val bestGenre = genres.maxByOrNull { snapshot.weightForTag(it) }
        val bestTheme = themes.maxByOrNull { snapshot.weightForTag(it) }

        val needsEnrichment = bestLang == null || bestGenre == null
        val cacheKey = TasteConstants.artistCacheKey(song.artistId?.takeIf { it.isNotBlank() } ?: artistName)
        val cachedEntry = TasteProfileRepository.getMediaTags(cacheKey)
        val isStale = cachedEntry == null || System.currentTimeMillis() - cachedEntry.updatedAt > TasteConstants.TAG_STALE_MS
        val resolving = needsEnrichment && (cacheKey in inFlight || (isStale && cacheKey !in sessionAttempted))
        if (needsEnrichment && isStale) {
            enqueueEnrichment(song)
        }

        return SongTagDetails(
            song = song,
            artistName = artistName,
            artistKey = artistKey,
            artistId = song.artistId,
            language = bestLang,
            genre = bestGenre,
            theme = bestTheme,
            isDetecting = resolving,
        )
    }

    // ── Tier 3: Async Keyless Enrichment ────────────────────────────────────

    fun enqueueEnrichment(song: Song) {
        if (song.artist.isBlank() && song.title.isBlank()) return
        val keySource = song.artistId?.takeIf { it.isNotBlank() } ?: song.artist.takeIf { it.isNotBlank() } ?: return
        val cacheKey = TasteConstants.artistCacheKey(keySource)
        val existing = TasteProfileRepository.getMediaTags(cacheKey)
        if (existing != null &&
            existing.langMask != 0 &&
            existing.genreMask != 0 &&
            System.currentTimeMillis() - existing.updatedAt <= TasteConstants.TAG_STALE_MS
        ) {
            return
        }
        if (!sessionAttempted.add(cacheKey)) return
        if (!inFlight.add(cacheKey)) return
        requests.trySend(EnrichRequest(song = song, artistKey = cacheKey))
    }

    private suspend fun workerLoop() {
        for (req in requests) {
            try {
                enrichBlocking(req)
            } catch (t: Throwable) {
                DebugLog.w(TAG, "Tag enrichment error for ${req.song.artist}: ${t.message}")
            } finally {
                inFlight.remove(req.artistKey)
            }
            delay(WORKER_IDLE_SPACING_MS)
        }
    }

    private fun enrichBlocking(req: EnrichRequest) {
        val song = req.song
        val primaryArtist = primaryArtistName(song.artist)
        var acc = TagNormalizer.NormalizedMasks()
        var source = TagSource.HEURISTIC

        // 1. Check any previously cached local ArtistFacts without hitting Last.fm
        localArtistTagsProvider?.invoke(primaryArtist)?.takeIf { it.isNotEmpty() }?.let { cached ->
            acc += TagNormalizer.normalizeAll(cached)
            if (!acc.isEmpty) source = TagSource.LOCAL_CACHE
        }

        // 2. iTunes Search API (fast, keyless, strong Indian & global genres)
        if (acc.genreMask == 0 || acc.langMask == 0) {
            val itunesMasks = fetchItunesMasks(primaryArtist, song.title)
            if (!itunesMasks.isEmpty) {
                acc += itunesMasks
                source = TagSource.ITUNES
            }
        }

        // 3. MusicBrainz WS/2 (keyless open community DB for genres, tags, and area/country)
        if (acc.genreMask == 0 || acc.langMask == 0) {
            val now = System.currentTimeMillis()
            val wait = MUSICBRAINZ_SPACING_MS - (now - lastMusicBrainzCallAt)
            if (wait > 0L) Thread.sleep(wait)
            lastMusicBrainzCallAt = System.currentTimeMillis()
            val mbMasks = fetchMusicBrainzMasks(primaryArtist)
            if (!mbMasks.isEmpty) {
                acc += mbMasks
                if (source == TagSource.HEURISTIC) source = TagSource.MUSICBRAINZ
            }
        }

        // 4. Always merge Tier-2 heuristics so script/keyword info isn't lost
        val heuristic = TagHeuristics.infer(song.title, song.artist, albumName = song.albumName)
        val langMask = acc.langMask or heuristic.langMask
        val genreMask = acc.genreMask or heuristic.genreMask
        val themeMask = acc.themeMask or heuristic.themeMask

        TasteProfileRepository.putMediaTags(
            entityKey = req.artistKey,
            langMask = langMask,
            genreMask = genreMask,
            themeMask = themeMask,
            source = source,
        )
        if (song.artist.isNotBlank()) {
            val nameKey = TasteConstants.artistCacheKey(song.artist)
            if (nameKey != req.artistKey) {
                TasteProfileRepository.putMediaTags(nameKey, langMask, genreMask, themeMask, source)
            }
        }
        if (song.videoId.isNotBlank()) {
            TasteProfileRepository.putMediaTags(
                entityKey = TasteConstants.songCacheKey(song.videoId),
                langMask = langMask,
                genreMask = genreMask,
                themeMask = themeMask,
                source = source,
            )
        }
    }

    internal fun parseItunesResponse(body: String): TagNormalizer.NormalizedMasks = runCatching {
        val root = json.parseToJsonElement(body) as? JsonObject ?: return TagNormalizer.NormalizedMasks()
        val results = root["results"] as? JsonArray ?: return TagNormalizer.NormalizedMasks()
        var acc = TagNormalizer.NormalizedMasks()
        for (el in results.take(3)) {
            val obj = el as? JsonObject ?: continue
            val genre = (obj["primaryGenreName"] as? JsonPrimitive)?.contentOrNull
            if (!genre.isNullOrBlank()) {
                acc += TagNormalizer.normalizeTag(genre)
            }
            val collection = (obj["collectionName"] as? JsonPrimitive)?.contentOrNull
            if (!collection.isNullOrBlank()) {
                acc += TagNormalizer.normalizeTag(collection)
            }
        }
        acc
    }.getOrDefault(TagNormalizer.NormalizedMasks())

    internal fun parseMusicBrainzResponse(body: String): TagNormalizer.NormalizedMasks = runCatching {
        val root = json.parseToJsonElement(body) as? JsonObject ?: return TagNormalizer.NormalizedMasks()
        val artists = root["artists"] as? JsonArray ?: return TagNormalizer.NormalizedMasks()
        val first = artists.firstOrNull() as? JsonObject ?: return TagNormalizer.NormalizedMasks()

        val rawTags = mutableListOf<String>()
        (first["genres"] as? JsonArray)?.forEach { g ->
            (g as? JsonObject)?.get("name")?.let { (it as? JsonPrimitive)?.contentOrNull }?.let(rawTags::add)
        }
        (first["tags"] as? JsonArray)?.forEach { t ->
            (t as? JsonObject)?.get("name")?.let { (it as? JsonPrimitive)?.contentOrNull }?.let(rawTags::add)
        }
        var acc = TagNormalizer.normalizeAll(rawTags)

        val areaName = (first["area"] as? JsonObject)?.get("name")?.let { (it as? JsonPrimitive)?.contentOrNull }
        val beginAreaName = (first["begin-area"] as? JsonObject)?.get("name")?.let { (it as? JsonPrimitive)?.contentOrNull }
        val country = (first["country"] as? JsonPrimitive)?.contentOrNull

        val areaMask = TagNormalizer.normalizeArea(beginAreaName)
            .let { if (it != 0) it else TagNormalizer.normalizeArea(areaName) }
            .let { if (it != 0) it else TagNormalizer.normalizeCountryCode(country) }

        if (areaMask != 0 && acc.langMask == 0) {
            acc = acc.copy(langMask = areaMask)
        }
        acc
    }.getOrDefault(TagNormalizer.NormalizedMasks())

    private fun fetchItunesMasks(artist: String, title: String): TagNormalizer.NormalizedMasks {
        val term = listOf(artist, title).filter { it.isNotBlank() }.joinToString(" ").trim()
        if (term.isBlank()) return TagNormalizer.NormalizedMasks()
        val encoded = URLEncoder.encode(term, "UTF-8")
        val url = "$ITUNES_URL?term=$encoded&entity=song&limit=3"
        val body = httpGet(url) ?: return TagNormalizer.NormalizedMasks()
        return parseItunesResponse(body)
    }

    private fun fetchMusicBrainzMasks(artist: String): TagNormalizer.NormalizedMasks {
        if (artist.isBlank()) return TagNormalizer.NormalizedMasks()
        val encoded = URLEncoder.encode("artist:\"$artist\"", "UTF-8")
        val url = "$MUSICBRAINZ_URL?query=$encoded&fmt=json&limit=2"
        val body = httpGet(url) ?: return TagNormalizer.NormalizedMasks()
        return parseMusicBrainzResponse(body)
    }

    private fun httpGet(url: String): String? = runCatching {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()
        Http.client.newCall(request).execute().use { resp ->
            if (resp.isSuccessful) resp.body?.string() else null
        }
    }.getOrNull()

    private fun primaryArtistName(artist: String): String =
        artist.split(",", "&", " feat.", " ft.", " x ", "×", "•")
            .firstOrNull()
            ?.trim()
            .orEmpty()
}
