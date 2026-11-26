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

import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.model.SearchFilter
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

/**
 * Weighted Chord Wave sampler + 5-song rolling buffer + 6-hour cooldown ring buffer.
 *
 * Guarantees 0-ms track picks when buffered, instant RAM purging on `Less` / `Never Again`,
 * 85-95% taste-guided picks, and 5-30% discovery budget (per [DiscoveryLevel]).
 */
object ChordWaveEngine {

    const val BUFFER_SIZE = 5
    private const val MAX_COOLDOWN_ENTRIES = 160

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val bufferLock = Any()
    private val refillMutex = Mutex()

    private val rollingBuffer = ArrayDeque<Song>(BUFFER_SIZE + 2)
    private val cooldownPlayedAt = LinkedHashMap<String, Long>()

    @Volatile
    private var lastFallbackPool: List<Song> = emptyList()

    @Volatile
    private var refillJob: Job? = null

    @Volatile
    private var nowProvider: () -> Long = System::currentTimeMillis

    private val _currentWaveSong = MutableStateFlow<Song?>(null)
    val currentWaveSong: StateFlow<Song?> = _currentWaveSong.asStateFlow()

    private val _bufferSnapshot = MutableStateFlow<List<Song>>(emptyList())
    val bufferSnapshot: StateFlow<List<Song>> = _bufferSnapshot.asStateFlow()

    fun resetForTesting(clock: () -> Long = System::currentTimeMillis) {
        synchronized(bufferLock) {
            nowProvider = clock
            rollingBuffer.clear()
            cooldownPlayedAt.clear()
            lastFallbackPool = emptyList()
            _currentWaveSong.value = null
            _bufferSnapshot.value = emptyList()
        }
    }

    /** Seeds the fallback pool (e.g. from Home / History / Library) and warms the buffer asynchronously. */
    fun warmUp(fallbackPool: List<Song>) {
        if (fallbackPool.isNotEmpty()) {
            lastFallbackPool = fallbackPool
        }
        val needRefill = synchronized(bufferLock) { rollingBuffer.size < BUFFER_SIZE }
        if (needRefill) {
            triggerAsyncRefill()
        }
    }

    /**
     * Picks the next Chord Wave track in 0 ms when the rolling buffer is warm,
     * falling back cleanly to weighted sampling over [fallbackPoolProvider].
     */
    suspend fun rollNext(
        snapshot: TasteSnapshot = TasteProfileRepository.snapshot.value,
        fallbackPoolProvider: suspend () -> List<Song> = { emptyList() },
        random: Random = Random.Default,
    ): Song? {
        val buffered = popCleanBufferedLocked(snapshot)
        if (buffered != null) {
            recordPlayed(buffered)
            triggerAsyncRefill()
            return buffered
        }

        val fallback = fallbackPoolProvider().also {
            if (it.isNotEmpty()) lastFallbackPool = it
        }
        val picked = pickWeightedCandidate(
            candidates = fallback,
            snapshot = snapshot,
            random = random,
        ) ?: fetchSingleRemoteCandidate(snapshot, random)

        if (picked != null) {
            recordPlayed(picked)
            triggerAsyncRefill()
        }
        return picked
    }

    /**
     * Immediately removes any tracks in the 5-song rolling buffer matching [predicate] or blocked
     * by the latest [snapshot], and refills asynchronously.
     */
    fun purgeBuffer(
        snapshot: TasteSnapshot = TasteProfileRepository.snapshot.value,
        extraPredicate: (Song) -> Boolean = { false },
    ) {
        var removedAny = false
        synchronized(bufferLock) {
            val iterator = rollingBuffer.iterator()
            while (iterator.hasNext()) {
                val song = iterator.next()
                if (TasteFilter.isBlocked(song, snapshot) || extraPredicate(song)) {
                    iterator.remove()
                    removedAny = true
                }
            }
            if (removedAny) {
                _bufferSnapshot.value = rollingBuffer.toList()
            }
        }
        triggerAsyncRefill()
    }

    /**
     * When the user taps "More Song" on a track, immediately pulls `RDAMVM<videoId>`
     * seeds into the front of the rolling buffer.
     */
    fun enqueueSimilarSeeds(song: Song) {
        if (song.videoId.isBlank()) return
        scope.launch {
            val snapshot = TasteProfileRepository.snapshot.value
            val related = YtMusicRepository.radio(song.videoId).getOrNull().orEmpty()
            val tags = TagResolver.resolveImmediate(song)
            if (!tags.isEmpty) {
                TagResolver.recordSourceTagsForSongs(related, tags.langMask, tags.genreMask, tags.themeMask)
            }
            val clean = TasteFilter.filterSongs(related, snapshot)
                .filterNot { it.videoId == song.videoId || isInCooldown(it.videoId) }
                .take(3)
            if (clean.isEmpty()) return@launch
            synchronized(bufferLock) {
                for (item in clean.asReversed()) {
                    if (rollingBuffer.none { it.videoId == item.videoId }) {
                        rollingBuffer.addFirst(item)
                    }
                }
                while (rollingBuffer.size > BUFFER_SIZE + 2) {
                    rollingBuffer.removeLast()
                }
                _bufferSnapshot.value = rollingBuffer.toList()
            }
        }
    }

    fun setCurrentWaveSong(song: Song?) {
        _currentWaveSong.value = song
    }

    /** Weighted random choice over [candidates], stripping blocks and 6h cooldown tracks. */
    fun pickWeightedCandidate(
        candidates: List<Song>,
        snapshot: TasteSnapshot,
        random: Random = Random.Default,
        excludeIds: Set<String> = emptySet(),
    ): Song? {
        val clean = TasteFilter.filterSongs(candidates, snapshot)
            .distinctBy { it.videoId }
            .filterNot { it.videoId in excludeIds }
        if (clean.isEmpty()) return null

        val fresh = clean.filterNot { isInCooldown(it.videoId) }.ifEmpty { clean }
        val explorationRoll = random.nextFloat()
        if (explorationRoll < snapshot.discoveryLevel.budget) {
            // Discovery slice: prefer tracks that aren't from high-play top artists or pick uniformly among clean tracks
            return fresh[random.nextInt(fresh.size)]
        }

        val scored = fresh.map { song ->
            val score = TasteFilter.scoreSong(song, snapshot).coerceAtLeast(1f)
            song to score
        }
        val totalWeight = scored.sumOf { it.second.toDouble() }
        if (totalWeight <= 0.0) return fresh[random.nextInt(fresh.size)]

        var target = random.nextDouble() * totalWeight
        for ((song, weight) in scored) {
            target -= weight
            if (target <= 0.0) return song
        }
        return scored.last().first
    }

    /** Chooses a target `(Language, Genre/Theme)` or exploratory query for Chord Wave bucket sampling. */
    fun sampleBucketQuery(
        snapshot: TasteSnapshot,
        random: Random = Random.Default,
    ): Triple<String, TasteTag?, TasteTag?> {
        val activeLangs = snapshot.activeNodes(TasteCategory.LANGUAGE)
            .mapNotNull { node -> node.canonicalId?.let(TasteTaxonomy::byId)?.let { it to node.weight } }
        val activeGenres = snapshot.activeNodes(TasteCategory.GENRE)
            .mapNotNull { node -> node.canonicalId?.let(TasteTaxonomy::byId)?.let { it to node.weight } }
        val activeThemes = snapshot.activeNodes(TasteCategory.THEME)
            .mapNotNull { node -> node.canonicalId?.let(TasteTaxonomy::byId)?.let { it to node.weight } }

        val isDiscovery = random.nextFloat() < snapshot.discoveryLevel.budget
        val lang = if (isDiscovery) {
            val neutralLangs = activeLangs.filter { it.second in 35f..60f }
            (neutralLangs.ifEmpty { activeLangs }).randomOrNull(random)?.first
        } else {
            weightedPick(activeLangs.filter { it.second >= 50f }.ifEmpty { activeLangs }, random)
        }

        val genre = if (isDiscovery) {
            val adjacent = activeGenres.filter { it.second in 35f..60f }
            (adjacent.ifEmpty { activeGenres }).randomOrNull(random)?.first
        } else {
            weightedPick(activeGenres.filter { it.second >= 50f }.ifEmpty { activeGenres }, random)
        }

        val theme = if (genre == null || random.nextFloat() < 0.35f) {
            weightedPick(activeThemes.filter { it.second >= 50f }, random)
        } else {
            null
        }

        val query = TasteTaxonomy.buildQuery(lang, genre, theme)
        return Triple(query, lang, genre)
    }

    private fun weightedPick(pairs: List<Pair<TasteTag, Float>>, random: Random): TasteTag? {
        if (pairs.isEmpty()) return null
        val total = pairs.sumOf { it.second.coerceAtLeast(1f).toDouble() }
        var target = random.nextDouble() * total
        for ((tag, w) in pairs) {
            target -= w.coerceAtLeast(1f)
            if (target <= 0.0) return tag
        }
        return pairs.last().first
    }

    private fun popCleanBufferedLocked(snapshot: TasteSnapshot): Song? = synchronized(bufferLock) {
        var found: Song? = null
        while (rollingBuffer.isNotEmpty()) {
            val candidate = rollingBuffer.removeFirst()
            if (!TasteFilter.isBlocked(candidate, snapshot) && !isInCooldown(candidate.videoId)) {
                found = candidate
                break
            }
        }
        _bufferSnapshot.value = rollingBuffer.toList()
        found
    }

    private fun recordPlayed(song: Song) {
        val now = nowProvider()
        synchronized(bufferLock) {
            cooldownPlayedAt[song.videoId] = now
            pruneCooldownLocked(now)
            _currentWaveSong.value = song
        }
    }

    private fun isInCooldown(videoId: String): Boolean {
        val ts = synchronized(bufferLock) { cooldownPlayedAt[videoId] } ?: return false
        return nowProvider() - ts < TasteConstants.WAVE_COOLDOWN_MS
    }

    private fun pruneCooldownLocked(now: Long) {
        val it = cooldownPlayedAt.entries.iterator()
        while (it.hasNext()) {
            val entry = it.next()
            if (now - entry.value >= TasteConstants.WAVE_COOLDOWN_MS) {
                it.remove()
            }
        }
        while (cooldownPlayedAt.size > MAX_COOLDOWN_ENTRIES) {
            val eldest = cooldownPlayedAt.keys.firstOrNull() ?: break
            cooldownPlayedAt.remove(eldest)
        }
    }

    private fun triggerAsyncRefill() {
        if (refillJob?.isActive == true) return
        refillJob = scope.launch {
            refillMutex.withLock {
                fillBuffer()
            }
        }
    }

    private suspend fun fillBuffer() {
        val snapshot = TasteProfileRepository.snapshot.value
        val needed = synchronized(bufferLock) { BUFFER_SIZE - rollingBuffer.size }
        if (needed <= 0) return

        val alreadyBufferedIds = synchronized(bufferLock) {
            rollingBuffer.mapTo(HashSet()) { it.videoId }
        }
        val gathered = mutableListOf<Song>()

        // Try taste-guided remote query when user has preferences or fallback pool is thin
        if (snapshot.hasPreferences || lastFallbackPool.size < BUFFER_SIZE) {
            val (query, lang, genre) = sampleBucketQuery(snapshot)
            val rows = runCatching {
                YtMusicRepository.search(query, SearchFilter.SONGS).getOrNull()
            }.getOrNull().orEmpty()
            val songs = rows.mapNotNull {
                (it as? SearchResult.TopTrack)?.song ?: (it as? SearchResult.Track)?.song
            }
            if (songs.isNotEmpty()) {
                TagResolver.recordSourceTagsForSongs(
                    songs,
                    langMask = TagMask.ofTag(lang),
                    genreMask = TagMask.ofTag(genre),
                )
                gathered += TasteFilter.filterSongs(songs, snapshot)
            }
        }

        // Also pull RDAMVM radio from a top fallback / current wave song for continuity
        val seedId = _currentWaveSong.value?.videoId?.takeIf { it.length == 11 }
            ?: lastFallbackPool.firstOrNull { it.videoId.length == 11 }?.videoId
        if (gathered.size < needed && seedId != null) {
            val radioSongs = runCatching { YtMusicRepository.radio(seedId).getOrNull() }.getOrNull().orEmpty()
            gathered += TasteFilter.filterSongs(radioSongs, snapshot)
        }

        // Always keep local/home fallback available
        if (lastFallbackPool.isNotEmpty()) {
            gathered += TasteFilter.filterSongs(lastFallbackPool, snapshot)
        }

        val additions = mutableListOf<Song>()
        val excludeIds = alreadyBufferedIds.toMutableSet()
        repeat(needed) {
            val pick = pickWeightedCandidate(gathered, snapshot, excludeIds = excludeIds) ?: return@repeat
            excludeIds += pick.videoId
            additions += pick
        }

        if (additions.isNotEmpty()) {
            synchronized(bufferLock) {
                for (song in additions) {
                    if (rollingBuffer.size < BUFFER_SIZE && rollingBuffer.none { it.videoId == song.videoId }) {
                        rollingBuffer.addLast(song)
                    }
                }
                _bufferSnapshot.value = rollingBuffer.toList()
            }
        }
    }

    private suspend fun fetchSingleRemoteCandidate(snapshot: TasteSnapshot, random: Random): Song? {
        val (query, lang, genre) = sampleBucketQuery(snapshot, random)
        val searchSongs = runCatching {
            YtMusicRepository.search(query, SearchFilter.SONGS).getOrNull()
        }.getOrNull().orEmpty().mapNotNull {
            (it as? SearchResult.TopTrack)?.song ?: (it as? SearchResult.Track)?.song
        }
        if (searchSongs.isNotEmpty()) {
            TagResolver.recordSourceTagsForSongs(searchSongs, TagMask.ofTag(lang), TagMask.ofTag(genre))
            pickWeightedCandidate(searchSongs, snapshot, random)?.let { return it }
        }
        val charts = runCatching {
            YtMusicRepository.browseSongs("FEmusic_charts").getOrNull()?.songs
        }.getOrNull().orEmpty()
        return pickWeightedCandidate(charts, snapshot, random)
    }
}
