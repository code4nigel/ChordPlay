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
import com.music.bitchord.data.aimusic.AiMusicFilter
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.pow

/**
 * Single source of truth for ChordPlay's on-device Taste Engine.
 *
 * Manages 4 compact stores under `<filesDir>/taste/` with strict `< 2 MB` caps:
 *  - `taste_profile.json` (`taste_nodes` + `blocked_songs` + AI artist flags + prefs, < 300 KB)
 *  - `media_tag_cache.json` (hard LRU cap: 2,000 artists + 3,000 songs, ~1 MB)
 *  - `feed_cache.json` (top 10 personalized Home shelves, 45 min TTL, < 300 KB)
 *
 * Exposes an immutable RAM [TasteSnapshot] via [snapshot] for < 0.1 ms lookups across
 * Home Feed, Chord Wave, Your Space Dial, ChordPlay Melody, and AutoPlay.
 */
object TasteProfileRepository {

    private const val TAG = "TasteEngine"
    private const val DIR_NAME = "taste"
    private const val PROFILE_FILE = "taste_profile.json"
    private const val TAG_CACHE_FILE = "media_tag_cache.json"
    private const val FEED_CACHE_FILE = "feed_cache.json"
    private const val SAVE_DEBOUNCE_MS = 350L

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val stateLock = Any()

    @Volatile
    private var tasteDir: File? = null

    @Volatile
    private var nowProvider: () -> Long = { System.currentTimeMillis() }

    private val nodes = LinkedHashMap<String, TasteNode>()
    private val blockedSongs = LinkedHashMap<String, BlockedSong>()
    private val tunedSongs = LinkedHashMap<String, TunedSong>()
    private val userMarkedAiArtists = LinkedHashSet<String>()
    private var hideAiMusic: Boolean = false
    private var discoveryLevel: DiscoveryLevel = DiscoveryLevel.BALANCED
    private var onboardingDismissed: Boolean = false
    private var lastDecayAt: Long = 0L

    private val mediaTags = ConcurrentHashMap<String, MediaTagEntry>()
    private val feedCache = LinkedHashMap<String, FeedCacheEntry>()

    @Volatile
    private var profileDirty = false

    @Volatile
    private var tagCacheDirty = false

    @Volatile
    private var feedCacheDirty = false

    @Volatile
    private var saveScheduled = false

    private val _snapshot = MutableStateFlow(seedDefaultSnapshot())
    val snapshot: StateFlow<TasteSnapshot> = _snapshot.asStateFlow()

    private val _tagRevision = MutableStateFlow(0)
    val tagRevision: StateFlow<Int> = _tagRevision.asStateFlow()

    init {
        seedCoreNodesInto(nodes)
    }

    fun init(filesDir: File, clock: () -> Long = System::currentTimeMillis) {
        synchronized(stateLock) {
            nowProvider = clock
            val dir = File(filesDir, DIR_NAME).apply { mkdirs() }
            tasteDir = dir
            loadFromDiskLocked(dir)
            applyWeeklyDecayIfDueLocked(nowProvider())
            enforceCapsLocked()
            publishSnapshotLocked()
            syncAiFilterLocked()
        }
    }

    /** Resets all state in RAM (optionally backed by [dir]) for deterministic unit tests. */
    fun resetForTesting(dir: File? = null, clock: () -> Long = System::currentTimeMillis) {
        synchronized(stateLock) {
            nowProvider = clock
            tasteDir = dir?.apply { mkdirs() }
            nodes.clear()
            blockedSongs.clear()
            tunedSongs.clear()
            userMarkedAiArtists.clear()
            hideAiMusic = false
            discoveryLevel = DiscoveryLevel.BALANCED
            onboardingDismissed = false
            lastDecayAt = 0L
            mediaTags.clear()
            feedCache.clear()
            seedCoreNodesInto(nodes)
            if (dir != null) {
                loadFromDiskLocked(dir)
            }
            publishSnapshotLocked()
            syncAiFilterLocked()
        }
        _tagRevision.value = 0
    }

    private fun seedDefaultSnapshot(): TasteSnapshot {
        val map = LinkedHashMap<String, TasteNode>()
        seedCoreNodesInto(map)
        return TasteSnapshot(nodesById = map)
    }

    private fun seedCoreNodesInto(target: MutableMap<String, TasteNode>) {
        for (tag in TasteTaxonomy.allCoreTags) {
            val id = TasteTaxonomy.nodeId(tag)
            if (id !in target) {
                target[id] = TasteNode(
                    id = id,
                    category = tag.category,
                    canonicalId = tag.id,
                    key = tag.key,
                    displayName = tag.displayName,
                    weight = TasteConstants.DEFAULT_NEUTRAL_WEIGHT,
                )
            }
        }
    }

    // ── Public Mutations ────────────────────────────────────────────────────

    fun boostTag(tag: TasteTag, delta: Float = TasteConstants.BOOST_DELTA) {
        synchronized(stateLock) {
            val now = nowProvider()
            val id = TasteTaxonomy.nodeId(tag)
            val cur = nodes[id] ?: nodeForTag(tag)
            val nextWeight = (cur.weight + delta).coerceIn(TasteConstants.MIN_WEIGHT, TasteConstants.MAX_WEIGHT)
            nodes[id] = cur.copy(
                weight = nextWeight,
                isBlocked = false,
                isPreferred = true,
                isAutoAdded = false,
                lastHeardAt = now,
            )
            onboardingDismissed = true
            markProfileChangedLocked()
        }
    }

    fun reduceTag(tag: TasteTag, delta: Float = TasteConstants.REDUCE_DELTA) {
        synchronized(stateLock) {
            val now = nowProvider()
            val id = TasteTaxonomy.nodeId(tag)
            val cur = nodes[id] ?: nodeForTag(tag)
            if (cur.isBlocked) return
            val nextWeight = (cur.weight - delta).coerceIn(TasteConstants.FLOOR_LESS_WEIGHT, TasteConstants.MAX_WEIGHT)
            nodes[id] = cur.copy(
                weight = nextWeight,
                isPreferred = nextWeight >= TasteConstants.DEFAULT_NEUTRAL_WEIGHT && cur.isPreferred,
                lastHeardAt = now,
            )
            markProfileChangedLocked()
        }
    }

    fun blockTag(tag: TasteTag) {
        synchronized(stateLock) {
            val now = nowProvider()
            val id = TasteTaxonomy.nodeId(tag)
            val cur = nodes[id] ?: nodeForTag(tag)
            nodes[id] = cur.copy(
                weight = 0f,
                manualRank = null,
                isBlocked = true,
                isPreferred = false,
                lastHeardAt = now,
            )
            onboardingDismissed = true
            markProfileChangedLocked()
        }
    }

    fun unblockTag(tag: TasteTag) {
        synchronized(stateLock) {
            val id = TasteTaxonomy.nodeId(tag)
            val cur = nodes[id] ?: return
            nodes[id] = cur.copy(
                weight = TasteConstants.DEFAULT_NEUTRAL_WEIGHT,
                isBlocked = false,
                isPreferred = false,
            )
            markProfileChangedLocked()
        }
    }

    fun boostArtist(artistName: String, artistId: String? = null, delta: Float = TasteConstants.BOOST_DELTA) {
        val key = canonicalArtistKey(artistName, artistId) ?: return
        synchronized(stateLock) {
            val now = nowProvider()
            val id = TasteTaxonomy.nodeId(TasteCategory.ARTIST, key)
            val cur = nodes[id] ?: TasteNode(
                id = id,
                category = TasteCategory.ARTIST,
                key = key,
                displayName = artistName.trim().ifBlank { key },
                weight = TasteConstants.DEFAULT_NEUTRAL_WEIGHT,
            )
            nodes[id] = cur.copy(
                displayName = artistName.trim().ifBlank { cur.displayName },
                weight = (cur.weight + delta).coerceIn(TasteConstants.MIN_WEIGHT, TasteConstants.MAX_WEIGHT),
                isBlocked = false,
                isPreferred = true,
                isAutoAdded = false,
                lastHeardAt = now,
            )
            // If name differs from UC channel id, unblock name key too
            val nameKey = TasteConstants.normalizeArtistKey(artistName)
            if (nameKey.isNotEmpty() && nameKey != key) {
                val nameId = TasteTaxonomy.nodeId(TasteCategory.ARTIST, nameKey)
                nodes[nameId]?.takeIf { it.isBlocked }?.let {
                    nodes[nameId] = it.copy(isBlocked = false, weight = TasteConstants.DEFAULT_NEUTRAL_WEIGHT)
                }
            }
            markProfileChangedLocked()
        }
    }

    fun reduceArtist(artistName: String, artistId: String? = null, delta: Float = TasteConstants.REDUCE_DELTA) {
        val key = canonicalArtistKey(artistName, artistId) ?: return
        synchronized(stateLock) {
            val now = nowProvider()
            val id = TasteTaxonomy.nodeId(TasteCategory.ARTIST, key)
            val cur = nodes[id] ?: TasteNode(
                id = id,
                category = TasteCategory.ARTIST,
                key = key,
                displayName = artistName.trim().ifBlank { key },
                weight = TasteConstants.DEFAULT_NEUTRAL_WEIGHT,
            )
            if (cur.isBlocked) return
            nodes[id] = cur.copy(
                displayName = artistName.trim().ifBlank { cur.displayName },
                weight = (cur.weight - delta).coerceIn(TasteConstants.FLOOR_LESS_WEIGHT, TasteConstants.MAX_WEIGHT),
                isPreferred = false,
                lastHeardAt = now,
            )
            markProfileChangedLocked()
        }
    }

    fun blockArtist(artistName: String, artistId: String? = null) {
        val keys = listOfNotNull(
            artistId?.trim()?.takeIf { it.isNotEmpty() }?.let(TasteConstants::normalizeArtistKey),
            artistName.trim().takeIf { it.isNotEmpty() }?.let(TasteConstants::normalizeArtistKey),
        ).distinct()
        if (keys.isEmpty()) return
        synchronized(stateLock) {
            val now = nowProvider()
            val display = artistName.trim().ifBlank { keys.first() }
            for (key in keys) {
                val id = TasteTaxonomy.nodeId(TasteCategory.ARTIST, key)
                val cur = nodes[id]
                nodes[id] = (cur ?: TasteNode(
                    id = id,
                    category = TasteCategory.ARTIST,
                    key = key,
                    displayName = display,
                )).copy(
                    displayName = display,
                    weight = 0f,
                    manualRank = null,
                    isBlocked = true,
                    isPreferred = false,
                    lastHeardAt = now,
                )
            }
            markProfileChangedLocked()
        }
    }

    fun unblockArtist(artistKey: String) {
        val norm = TasteConstants.normalizeArtistKey(artistKey)
        if (norm.isEmpty()) return
        synchronized(stateLock) {
            val id = TasteTaxonomy.nodeId(TasteCategory.ARTIST, norm)
            val targetNode = nodes[id]
            val targetDisplayNorm = targetNode?.displayName?.let(TasteConstants::normalizeArtistKey)
            var changed = false
            for ((nodeId, node) in nodes) {
                if (node.category != TasteCategory.ARTIST || !node.isBlocked) continue
                val matchesKey = node.id == id || node.key == norm
                val matchesName = targetDisplayNorm != null &&
                    TasteConstants.normalizeArtistKey(node.displayName) == targetDisplayNorm
                if (matchesKey || matchesName) {
                    nodes[nodeId] = node.copy(
                        weight = TasteConstants.DEFAULT_NEUTRAL_WEIGHT,
                        isBlocked = false,
                        isPreferred = false,
                    )
                    changed = true
                }
            }
            if (changed) markProfileChangedLocked()
        }
    }

    /** Manually adjusts any active node (`+` / `-` stepper) and updates priority sorting cleanly. */
    fun stepNodeWeight(nodeId: String, delta: Float = 10f) {
        synchronized(stateLock) {
            val cur = nodes[nodeId] ?: return
            if (cur.isBlocked) return
            val nextWeight = (cur.weight + delta).coerceIn(
                TasteConstants.FLOOR_LESS_WEIGHT,
                TasteConstants.MAX_WEIGHT,
            )
            nodes[nodeId] = cur.copy(
                weight = nextWeight,
                manualRank = null,
                isPreferred = nextWeight > TasteConstants.DEFAULT_NEUTRAL_WEIGHT,
                isAutoAdded = if (nextWeight > TasteConstants.DEFAULT_NEUTRAL_WEIGHT) false else cur.isAutoAdded,
                lastHeardAt = nowProvider(),
            )
            onboardingDismissed = true
            markProfileChangedLocked()
        }
    }

    /** Blocks any node by its [nodeId] (including learned artists, languages, genres, or themes). */
    fun blockNode(nodeId: String): Boolean {
        synchronized(stateLock) {
            val cur = nodes[nodeId] ?: return false
            val tag = cur.canonicalId?.let(TasteTaxonomy::byId)
            if (tag != null) {
                blockTag(tag)
                return true
            }
            if (cur.category == TasteCategory.ARTIST) {
                blockArtist(cur.displayName, cur.key)
                return true
            }
            nodes[nodeId] = cur.copy(
                weight = 0f,
                manualRank = null,
                isBlocked = true,
                isPreferred = false,
                lastHeardAt = nowProvider(),
            )
            markProfileChangedLocked()
            return true
        }
    }

    fun unblockNode(nodeId: String) {
        synchronized(stateLock) {
            val cur = nodes[nodeId] ?: return
            val tag = cur.canonicalId?.let(TasteTaxonomy::byId)
            if (tag != null) {
                unblockTag(tag)
                return
            }
            if (cur.category == TasteCategory.ARTIST) {
                unblockArtist(cur.key)
                return
            }
            nodes[nodeId] = cur.copy(
                weight = TasteConstants.DEFAULT_NEUTRAL_WEIGHT,
                isBlocked = false,
                isPreferred = false,
            )
            markProfileChangedLocked()
        }
    }

    fun boostSong(song: Song, delta: Float = TasteConstants.BOOST_DELTA) {
        stepTunedSongWeight(song = song, delta = delta)
    }

    fun reduceSong(song: Song, delta: Float = TasteConstants.REDUCE_DELTA) {
        stepTunedSongWeight(song = song, delta = -delta)
    }

    fun stepTunedSongWeight(videoId: String, delta: Float) {
        if (videoId.isBlank()) return
        synchronized(stateLock) {
            val cur = tunedSongs[videoId] ?: return
            val next = (cur.weight + delta).coerceIn(
                TasteConstants.FLOOR_LESS_WEIGHT,
                TasteConstants.MAX_WEIGHT,
            )
            tunedSongs[videoId] = cur.copy(weight = next, updatedAt = nowProvider())
            enforceCapsLocked()
            markProfileChangedLocked()
        }
    }

    fun clearTunedSong(videoId: String) {
        synchronized(stateLock) {
            if (tunedSongs.remove(videoId) != null) {
                markProfileChangedLocked()
            }
        }
    }

    private fun stepTunedSongWeight(song: Song, delta: Float) {
        if (song.videoId.isBlank()) return
        synchronized(stateLock) {
            val now = nowProvider()
            val cur = tunedSongs[song.videoId]
            val baseWeight = cur?.weight ?: TasteConstants.DEFAULT_NEUTRAL_WEIGHT
            val nextWeight = (baseWeight + delta).coerceIn(
                TasteConstants.FLOOR_LESS_WEIGHT,
                TasteConstants.MAX_WEIGHT,
            )
            tunedSongs[song.videoId] = TunedSong(
                videoId = song.videoId,
                title = song.title.ifBlank { cur?.title ?: song.videoId },
                artistName = song.artist.ifBlank { cur?.artistName ?: "" },
                weight = nextWeight,
                updatedAt = now,
            )
            enforceCapsLocked()
            markProfileChangedLocked()
        }
    }

    fun blockSong(song: Song) {
        if (song.videoId.isBlank()) return
        synchronized(stateLock) {
            blockedSongs[song.videoId] = BlockedSong(
                videoId = song.videoId,
                title = song.title,
                artistName = song.artist,
                blockedAt = nowProvider(),
            )
            markProfileChangedLocked()
        }
    }

    fun unblockSong(videoId: String) {
        synchronized(stateLock) {
            if (blockedSongs.remove(videoId) != null) {
                markProfileChangedLocked()
            }
        }
    }

    fun markArtistAsAi(artistName: String, artistId: String? = null) {
        val keys = listOfNotNull(
            artistId?.trim()?.takeIf { it.isNotEmpty() }?.let(TasteConstants::normalizeArtistKey),
            artistName.trim().takeIf { it.isNotEmpty() }?.let(TasteConstants::normalizeArtistKey),
        ).distinct()
        if (keys.isEmpty()) return
        synchronized(stateLock) {
            userMarkedAiArtists.addAll(keys)
            syncAiFilterLocked()
            markProfileChangedLocked()
        }
    }

    fun unmarkArtistAsAi(key: String) {
        val norm = TasteConstants.normalizeArtistKey(key)
        synchronized(stateLock) {
            if (userMarkedAiArtists.remove(norm)) {
                syncAiFilterLocked()
                markProfileChangedLocked()
            }
        }
    }

    fun setHideAiMusic(enabled: Boolean) {
        synchronized(stateLock) {
            if (AiMusicFilter.enabled != enabled) {
                AiMusicFilter.enabled = enabled
            }
            if (hideAiMusic == enabled) return
            hideAiMusic = enabled
            syncAiFilterLocked()
            markProfileChangedLocked()
        }
    }

    fun setDiscoveryLevel(level: DiscoveryLevel) {
        synchronized(stateLock) {
            if (discoveryLevel == level) return
            discoveryLevel = level
            markProfileChangedLocked()
        }
    }

    fun dismissOnboarding() {
        synchronized(stateLock) {
            if (onboardingDismissed) return
            onboardingDismissed = true
            markProfileChangedLocked()
        }
    }

    /**
     * Quick onboarding setup: boosts loved languages/genres to [TasteConstants.PREFERRED_INITIAL_WEIGHT]
     * and blocks disliked ones immediately.
     */
    fun applyQuickSetup(
        preferredLangKeys: Set<String>,
        blockedLangKeys: Set<String>,
        preferredGenreKeys: Set<String>,
        blockedGenreKeys: Set<String>,
    ) {
        synchronized(stateLock) {
            val now = nowProvider()
            for (tag in TasteTaxonomy.languages) {
                val id = TasteTaxonomy.nodeId(tag)
                val cur = nodes[id] ?: nodeForTag(tag)
                nodes[id] = when {
                    tag.key in blockedLangKeys -> cur.copy(weight = 0f, isBlocked = true, isPreferred = false, manualRank = null, lastHeardAt = now)
                    tag.key in preferredLangKeys -> cur.copy(weight = TasteConstants.PREFERRED_INITIAL_WEIGHT, isBlocked = false, isPreferred = true, isAutoAdded = false, lastHeardAt = now)
                    else -> cur.copy(isBlocked = false)
                }
            }
            for (tag in TasteTaxonomy.genres) {
                val id = TasteTaxonomy.nodeId(tag)
                val cur = nodes[id] ?: nodeForTag(tag)
                nodes[id] = when {
                    tag.key in blockedGenreKeys -> cur.copy(weight = 0f, isBlocked = true, isPreferred = false, manualRank = null, lastHeardAt = now)
                    tag.key in preferredGenreKeys -> cur.copy(weight = TasteConstants.PREFERRED_INITIAL_WEIGHT, isBlocked = false, isPreferred = true, isAutoAdded = false, lastHeardAt = now)
                    else -> cur.copy(isBlocked = false)
                }
            }
            onboardingDismissed = true
            markProfileChangedLocked()
        }
    }

    /**
     * Drag-to-Reorder <-> Probability Sync:
     * Recomputes weights along a smooth monotonic curve (`95f` down to `15f`) and stores `manualRank`.
     */
    fun reorderCategory(category: TasteCategory, orderedNodeIds: List<String>) {
        if (orderedNodeIds.isEmpty()) return
        synchronized(stateLock) {
            val activeIds = orderedNodeIds.filter { id ->
                val n = nodes[id]
                n != null && n.category == category && !n.isBlocked
            }
            val count = activeIds.size
            if (count == 0) return
            val now = nowProvider()
            activeIds.forEachIndexed { idx, id ->
                val cur = nodes[id] ?: return@forEachIndexed
                val t = if (count <= 1) 0f else idx.toFloat() / (count - 1).toFloat()
                val newWeight = (
                    TasteConstants.MIN_MANUAL_WEIGHT +
                        (TasteConstants.MAX_MANUAL_WEIGHT - TasteConstants.MIN_MANUAL_WEIGHT) *
                        (1f - t).toDouble().pow(1.25).toFloat()
                    ).coerceIn(TasteConstants.MIN_MANUAL_WEIGHT, TasteConstants.MAX_MANUAL_WEIGHT)
                nodes[id] = cur.copy(
                    weight = newWeight,
                    manualRank = idx,
                    isPreferred = idx < 3 || cur.isPreferred,
                    isAutoAdded = false,
                    lastHeardAt = now,
                )
            }
            onboardingDismissed = true
            markProfileChangedLocked()
        }
    }

    fun resetLearnedWeights(includeBlocks: Boolean = false) {
        synchronized(stateLock) {
            if (includeBlocks) {
                nodes.clear()
                blockedSongs.clear()
                tunedSongs.clear()
                userMarkedAiArtists.clear()
                onboardingDismissed = false
                seedCoreNodesInto(nodes)
            } else {
                tunedSongs.clear()
                val iterator = nodes.entries.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    val n = entry.value
                    if (n.category == TasteCategory.ARTIST && !n.isBlocked) {
                        iterator.remove()
                    } else if (!n.isBlocked) {
                        entry.setValue(
                            n.copy(
                                weight = TasteConstants.DEFAULT_NEUTRAL_WEIGHT,
                                manualRank = null,
                                isPreferred = false,
                                isAutoAdded = false,
                                playCount = 0,
                            ),
                        )
                    }
                }
            }
            syncAiFilterLocked()
            markProfileChangedLocked()
        }
    }

    // ── Passive Listening & Decay ───────────────────────────────────────────

    /**
     * Called once a track passes the listen threshold (>30s or >50%).
     * Nudges resolved Language / Genre / Theme / Artist weights within `0f..100f`.
     */
    fun recordPassiveListen(song: Song, resolved: ResolvedTags) {
        synchronized(stateLock) {
            val now = nowProvider()
            applyWeeklyDecayIfDueLocked(now)

            fun nudgeTag(tag: TasteTag) {
                val id = TasteTaxonomy.nodeId(tag)
                val cur = nodes[id]
                if (cur?.isBlocked == true) return
                if (cur == null) {
                    nodes[id] = nodeForTag(tag).copy(
                        weight = TasteConstants.DEFAULT_NEUTRAL_WEIGHT + TasteConstants.PASSIVE_NUDGE,
                        isAutoAdded = true,
                        playCount = 1,
                        lastHeardAt = now,
                    )
                } else {
                    val isNewDiscovery = cur.playCount == 0 && !cur.isPreferred && cur.manualRank == null
                    val delta = if (cur.manualRank != null) TasteConstants.PASSIVE_MANUAL_NUDGE else TasteConstants.PASSIVE_NUDGE
                    val nextWeight = (cur.weight + delta).coerceIn(TasteConstants.MIN_WEIGHT, TasteConstants.MAX_WEIGHT)
                    nodes[id] = cur.copy(
                        weight = nextWeight,
                        isAutoAdded = if (cur.isPreferred || cur.manualRank != null) cur.isAutoAdded else (cur.isAutoAdded || isNewDiscovery),
                        playCount = cur.playCount + 1,
                        lastHeardAt = now,
                    )
                }
            }

            resolved.languages().forEach(::nudgeTag)
            resolved.genres().forEach(::nudgeTag)
            resolved.themes().forEach(::nudgeTag)

            val artistKey = canonicalArtistKey(song.artist, song.artistId)
            if (artistKey != null) {
                val aId = TasteTaxonomy.nodeId(TasteCategory.ARTIST, artistKey)
                val curArtist = nodes[aId]
                if (curArtist?.isBlocked != true) {
                    val delta = if (curArtist?.manualRank != null) TasteConstants.PASSIVE_MANUAL_NUDGE else TasteConstants.PASSIVE_NUDGE
                    nodes[aId] = (curArtist ?: TasteNode(
                        id = aId,
                        category = TasteCategory.ARTIST,
                        key = artistKey,
                        displayName = song.artist.trim().ifBlank { artistKey },
                        weight = TasteConstants.DEFAULT_NEUTRAL_WEIGHT,
                        isAutoAdded = true,
                    )).copy(
                        displayName = song.artist.trim().ifBlank { curArtist?.displayName ?: artistKey },
                        weight = ((curArtist?.weight ?: TasteConstants.DEFAULT_NEUTRAL_WEIGHT) + delta)
                            .coerceIn(TasteConstants.MIN_WEIGHT, TasteConstants.MAX_WEIGHT),
                        playCount = (curArtist?.playCount ?: 0) + 1,
                        lastHeardAt = now,
                    )
                }
            }

            enforceCapsLocked()
            markProfileChangedLocked()
        }
    }

    /** Gentle negative nudge when a track is quickly skipped (<20s). */
    fun recordQuickSkip(song: Song, resolved: ResolvedTags) {
        synchronized(stateLock) {
            val now = nowProvider()
            fun nudgeDown(tag: TasteTag) {
                val id = TasteTaxonomy.nodeId(tag)
                val cur = nodes[id] ?: return
                if (cur.isBlocked || cur.manualRank != null || cur.isPreferred) return
                nodes[id] = cur.copy(
                    weight = (cur.weight - TasteConstants.QUICK_SKIP_NUDGE)
                        .coerceIn(TasteConstants.FLOOR_LESS_WEIGHT, TasteConstants.MAX_WEIGHT),
                    lastHeardAt = now,
                )
            }
            resolved.genres().forEach(::nudgeDown)
            resolved.themes().forEach(::nudgeDown)
            canonicalArtistKey(song.artist, song.artistId)?.let { artistKey ->
                val aId = TasteTaxonomy.nodeId(TasteCategory.ARTIST, artistKey)
                val curArtist = nodes[aId]
                if (curArtist != null && !curArtist.isBlocked && curArtist.manualRank == null && !curArtist.isPreferred) {
                    nodes[aId] = curArtist.copy(
                        weight = (curArtist.weight - TasteConstants.QUICK_SKIP_NUDGE)
                            .coerceIn(TasteConstants.FLOOR_LESS_WEIGHT, TasteConstants.MAX_WEIGHT),
                        lastHeardAt = now,
                    )
                }
            }
            markProfileChangedLocked()
        }
    }

    fun applyWeeklyDecayIfDue(): Boolean {
        synchronized(stateLock) {
            val changed = applyWeeklyDecayIfDueLocked(nowProvider())
            if (changed) markProfileChangedLocked()
            return changed
        }
    }

    private fun applyWeeklyDecayIfDueLocked(now: Long): Boolean {
        if (lastDecayAt == 0L) {
            lastDecayAt = now
            return false
        }
        if (now - lastDecayAt < TasteConstants.DECAY_INTERVAL_MS) return false
        lastDecayAt = now
        var changed = false
        for ((id, node) in nodes) {
            if (node.isBlocked) continue
            // Inactive tags drift gently toward neutral (weight * 0.97 + 45 * 0.03)
            if (now - node.lastHeardAt >= TasteConstants.DECAY_INTERVAL_MS) {
                val decayed = (node.weight * TasteConstants.WEEKLY_DECAY_KEEP +
                    TasteConstants.DEFAULT_NEUTRAL_WEIGHT * (1f - TasteConstants.WEEKLY_DECAY_KEEP))
                    .coerceIn(TasteConstants.FLOOR_LESS_WEIGHT, TasteConstants.MAX_WEIGHT)
                if (decayed != node.weight) {
                    nodes[id] = node.copy(weight = decayed)
                    changed = true
                }
            }
        }
        return changed
    }

    // ── Media Tag Cache (2,000 artists + 3,000 songs LRU cap) ────────────────

    fun getMediaTags(entityKey: String): MediaTagEntry? {
        val entry = mediaTags[entityKey] ?: return null
        val now = nowProvider()
        if (now - entry.lastAccessedAt > 60_000L) {
            mediaTags[entityKey] = entry.copy(lastAccessedAt = now)
            tagCacheDirty = true
            scheduleSave()
        }
        return entry
    }

    fun putMediaTags(
        entityKey: String,
        langMask: Int,
        genreMask: Int,
        themeMask: Int,
        source: TagSource,
    ) {
        if (entityKey.isBlank()) return
        val now = nowProvider()
        val existing = mediaTags[entityKey]
        val mergedLang = (existing?.langMask ?: 0) or langMask
        val mergedGenre = (existing?.genreMask ?: 0) or genreMask
        val mergedTheme = (existing?.themeMask ?: 0) or themeMask
        val effectiveSource = when {
            existing?.source == TagSource.USER || source == TagSource.USER -> TagSource.USER
            existing?.source == TagSource.SOURCE || source == TagSource.SOURCE -> TagSource.SOURCE
            else -> source
        }
        mediaTags[entityKey] = MediaTagEntry(
            entityKey = entityKey,
            langMask = mergedLang,
            genreMask = mergedGenre,
            themeMask = mergedTheme,
            source = effectiveSource,
            updatedAt = now,
            lastAccessedAt = now,
        )
        enforceTagCacheCaps()
        tagCacheDirty = true
        _tagRevision.value = _tagRevision.value + 1
        scheduleSave()
    }

    fun sweepLruAndCaps() {
        synchronized(stateLock) {
            enforceCapsLocked()
            enforceTagCacheCaps()
            markProfileChangedLocked()
        }
    }

    // ── Feed Cache (30-60 min TTL, < 300 KB) ────────────────────────────────

    fun saveFeedCache(shelves: List<HomeShelf>) {
        if (shelves.isEmpty()) return
        val now = nowProvider()
        synchronized(stateLock) {
            feedCache.clear()
            shelves.take(TasteConstants.MAX_FEED_SHELVES).forEachIndexed { idx, shelf ->
                val key = "${idx}_${shelf.title.lowercase().replace(Regex("[^a-z0-9]+"), "_")}"
                feedCache[key] = FeedCacheEntry(
                    shelfKey = key,
                    title = shelf.title,
                    subtitle = shelf.subtitle,
                    items = shelf.items.take(20).map(CachedShelfItem::from),
                    fetchedAt = now,
                )
            }
            feedCacheDirty = true
            scheduleSave()
        }
    }

    fun readFeedCache(allowStale: Boolean = false): List<HomeShelf> {
        val now = nowProvider()
        synchronized(stateLock) {
            val entries = feedCache.values.toList()
            if (entries.isEmpty()) return emptyList()
            if (!allowStale && entries.all { now - it.fetchedAt > TasteConstants.FEED_CACHE_TTL_MS }) {
                return emptyList()
            }
            return entries.map { it.toHomeShelf() }
        }
    }

    fun isFeedCacheStale(): Boolean {
        val now = nowProvider()
        synchronized(stateLock) {
            if (feedCache.isEmpty()) return true
            val newest = feedCache.values.maxOfOrNull { it.fetchedAt } ?: return true
            return now - newest > TasteConstants.FEED_CACHE_TTL_MS
        }
    }

    fun clearFeedCache() {
        synchronized(stateLock) {
            if (feedCache.isEmpty()) return
            feedCache.clear()
            feedCacheDirty = true
            scheduleSave()
        }
    }

    // ── Backup Export / Import ──────────────────────────────────────────────

    fun exportProfileJson(): String = synchronized(stateLock) {
        json.encodeToString(TasteProfileDisk.serializer(), currentProfileDiskLocked())
    }

    fun importProfileJson(raw: String): Boolean {
        val parsed = runCatching { json.decodeFromString(TasteProfileDisk.serializer(), raw) }.getOrNull()
            ?: return false
        synchronized(stateLock) {
            applyProfileDiskLocked(parsed)
            publishSnapshotLocked()
            syncAiFilterLocked()
            profileDirty = true
            scheduleSave()
        }
        return true
    }

    // ── Internals ───────────────────────────────────────────────────────────

    private fun canonicalArtistKey(artistName: String, artistId: String?): String? {
        val idKey = artistId?.trim()?.takeIf { it.isNotEmpty() }?.let(TasteConstants::normalizeArtistKey)
        if (idKey != null) return idKey
        return artistName.trim().takeIf { it.isNotEmpty() }?.let(TasteConstants::normalizeArtistKey)
    }

    private fun nodeForTag(tag: TasteTag): TasteNode = TasteNode(
        id = TasteTaxonomy.nodeId(tag),
        category = tag.category,
        canonicalId = tag.id,
        key = tag.key,
        displayName = tag.displayName,
        weight = TasteConstants.DEFAULT_NEUTRAL_WEIGHT,
    )

    private fun enforceCapsLocked() {
        val passiveArtists = nodes.values.filter {
            it.category == TasteCategory.ARTIST && !it.isBlocked && !it.isPreferred && it.manualRank == null
        }
        if (passiveArtists.size > TasteConstants.MAX_PASSIVE_ARTIST_NODES) {
            val dropCount = passiveArtists.size - TasteConstants.MAX_PASSIVE_ARTIST_NODES
            passiveArtists
                .sortedWith(compareBy<TasteNode> { it.weight }.thenBy { it.lastHeardAt })
                .take(dropCount)
                .forEach { nodes.remove(it.id) }
        }
        if (tunedSongs.size > TasteConstants.MAX_TUNED_SONGS) {
            val dropSongs = tunedSongs.size - TasteConstants.MAX_TUNED_SONGS
            tunedSongs.values
                .sortedBy { it.updatedAt }
                .take(dropSongs)
                .forEach { tunedSongs.remove(it.videoId) }
        }
    }

    private fun enforceTagCacheCaps() {
        val artists = mediaTags.values.filter { it.entityKey.startsWith("A:") }
        if (artists.size > TasteConstants.MAX_CACHED_ARTISTS) {
            val drop = artists.size - TasteConstants.MAX_CACHED_ARTISTS
            artists.sortedBy { it.lastAccessedAt }.take(drop).forEach { mediaTags.remove(it.entityKey) }
            tagCacheDirty = true
        }
        val songs = mediaTags.values.filter { it.entityKey.startsWith("S:") }
        if (songs.size > TasteConstants.MAX_CACHED_SONGS) {
            val drop = songs.size - TasteConstants.MAX_CACHED_SONGS
            songs.sortedBy { it.lastAccessedAt }.take(drop).forEach { mediaTags.remove(it.entityKey) }
            tagCacheDirty = true
        }
    }

    private fun markProfileChangedLocked() {
        publishSnapshotLocked()
        profileDirty = true
        scheduleSave()
    }

    private fun publishSnapshotLocked() {
        _snapshot.value = TasteSnapshot(
            nodesById = HashMap(nodes),
            blockedSongs = HashMap(blockedSongs),
            tunedSongs = HashMap(tunedSongs),
            userMarkedAiArtists = HashSet(userMarkedAiArtists),
            hideAiMusic = hideAiMusic,
            discoveryLevel = discoveryLevel,
            onboardingDismissed = onboardingDismissed,
            lastDecayAt = lastDecayAt,
        )
    }

    private fun syncAiFilterLocked() {
        AiMusicFilter.setUserMarkedArtists(HashSet(userMarkedAiArtists))
    }

    private fun currentProfileDiskLocked(): TasteProfileDisk = TasteProfileDisk(
        version = 1,
        nodes = nodes.values.toList(),
        blockedSongs = blockedSongs.values.toList(),
        tunedSongs = tunedSongs.values.toList(),
        userMarkedAiArtists = userMarkedAiArtists.toList(),
        hideAiMusic = hideAiMusic,
        discoveryLevel = discoveryLevel,
        onboardingDismissed = onboardingDismissed,
        lastDecayAt = lastDecayAt,
    )

    private fun applyProfileDiskLocked(disk: TasteProfileDisk) {
        nodes.clear()
        seedCoreNodesInto(nodes)
        for (node in disk.nodes) {
            nodes[node.id] = node.copy(weight = node.weight.coerceIn(0f, TasteConstants.MAX_WEIGHT))
        }
        blockedSongs.clear()
        for (song in disk.blockedSongs) {
            blockedSongs[song.videoId] = song
        }
        tunedSongs.clear()
        for (song in disk.tunedSongs) {
            tunedSongs[song.videoId] = song.copy(
                weight = song.weight.coerceIn(TasteConstants.FLOOR_LESS_WEIGHT, TasteConstants.MAX_WEIGHT),
            )
        }
        userMarkedAiArtists.clear()
        userMarkedAiArtists.addAll(disk.userMarkedAiArtists)
        hideAiMusic = disk.hideAiMusic
        discoveryLevel = disk.discoveryLevel
        onboardingDismissed = disk.onboardingDismissed
        lastDecayAt = disk.lastDecayAt
    }

    private fun loadFromDiskLocked(dir: File) {
        val profileFile = File(dir, PROFILE_FILE)
        if (profileFile.isFile) {
            runCatching {
                json.decodeFromString(TasteProfileDisk.serializer(), profileFile.readText())
            }.onSuccess { applyProfileDiskLocked(it) }
                .onFailure { DebugLog.w(TAG, "Resetting unreadable taste profile: ${it.message}") }
        }
        val tagFile = File(dir, TAG_CACHE_FILE)
        if (tagFile.isFile) {
            runCatching {
                json.decodeFromString(MediaTagCacheDisk.serializer(), tagFile.readText())
            }.onSuccess { disk ->
                mediaTags.clear()
                for (entry in disk.entries) mediaTags[entry.entityKey] = entry
            }.onFailure { DebugLog.w(TAG, "Resetting unreadable tag cache: ${it.message}") }
        }
        val feedFile = File(dir, FEED_CACHE_FILE)
        if (feedFile.isFile) {
            runCatching {
                json.decodeFromString(FeedCacheDisk.serializer(), feedFile.readText())
            }.onSuccess { disk ->
                feedCache.clear()
                for (entry in disk.shelves) feedCache[entry.shelfKey] = entry
            }.onFailure { DebugLog.w(TAG, "Resetting unreadable feed cache: ${it.message}") }
        }
    }

    private fun scheduleSave() {
        if (tasteDir == null) return
        if (saveScheduled) return
        saveScheduled = true
        ioScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            flushNow()
        }
    }

    fun flushNow() {
        val dir = tasteDir ?: return
        var profilePayload: String? = null
        var tagPayload: String? = null
        var feedPayload: String? = null

        synchronized(stateLock) {
            saveScheduled = false
            if (profileDirty) {
                profileDirty = false
                profilePayload = json.encodeToString(TasteProfileDisk.serializer(), currentProfileDiskLocked())
            }
            if (tagCacheDirty) {
                tagCacheDirty = false
                tagPayload = json.encodeToString(
                    MediaTagCacheDisk.serializer(),
                    MediaTagCacheDisk(entries = mediaTags.values.toList()),
                )
            }
            if (feedCacheDirty) {
                feedCacheDirty = false
                feedPayload = json.encodeToString(
                    FeedCacheDisk.serializer(),
                    FeedCacheDisk(shelves = feedCache.values.toList()),
                )
            }
        }

        profilePayload?.let { File(dir, PROFILE_FILE).writeAtomically(it) }
        tagPayload?.let { File(dir, TAG_CACHE_FILE).writeAtomically(it) }
        feedPayload?.let { File(dir, FEED_CACHE_FILE).writeAtomically(it) }
    }

    private fun File.writeAtomically(text: String) {
        runCatching {
            val tmp = File(parentFile, "$name.tmp")
            tmp.writeText(text)
            if (!tmp.renameTo(this)) {
                delete()
                tmp.renameTo(this)
            }
        }.onFailure {
            DebugLog.w(TAG, "Failed writing $name: ${it.message}")
        }
    }
}
