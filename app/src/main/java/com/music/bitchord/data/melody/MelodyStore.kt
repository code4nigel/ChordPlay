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
package com.music.bitchord.data.melody

import android.content.Context
import android.content.SharedPreferences
import com.music.bitchord.BuildConfig
import com.music.bitchord.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ChordPlay Melody persistence: mood, tuner, starter picks, bans, skip signals,
 * and day-long snapshot caching.
 *
 * Uses its own preferences file (`bitchord_melody`) so Melody state survives
 * AppSettings imports/exports and never touches the main thread on snapshot writes.
 */
object MelodyStore {

    private lateinit var prefs: SharedPreferences

    internal const val PREFS_FILE = "bitchord_melody"
    private const val KEY_MOOD = "melody_mood"
    private const val KEY_DISCOVERY = "melody_discovery"
    private const val KEY_MEMORY = "melody_memory"
    private const val KEY_EXCLUDED_GENRES = "melody_excluded_genres"
    private const val KEY_SIMILAR_LIKED = "melody_similar_liked"
    private const val KEY_BANNED = "melody_banned"
    private const val KEY_SKIPS = "melody_skips"
    private const val KEY_SEED_CURSOR = "melody_seed_cursor"
    private const val KEY_STARTER_MOODS = "melody_starter_moods"
    private const val KEY_STARTER_ARTISTS = "melody_starter_artists"

    private const val MAX_BANNED = 200
    private const val MAX_SKIPS = 300

    private val _mood = MutableStateFlow(MelodyMood.MELODY)
    val mood: StateFlow<MelodyMood> = _mood.asStateFlow()

    private val _tuner = MutableStateFlow(MelodyTuner())
    val tuner: StateFlow<MelodyTuner> = _tuner.asStateFlow()

    val config: MelodyConfig get() = MelodyConfig(_mood.value, _tuner.value)

    private val _bannedIds = MutableStateFlow<Set<String>>(emptySet())
    val bannedIds: StateFlow<Set<String>> = _bannedIds.asStateFlow()

    private val _skipCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val skipCounts: StateFlow<Map<String, Int>> = _skipCounts.asStateFlow()

    private val _starterMoods = MutableStateFlow<Set<MelodyMood>>(emptySet())
    val starterMoods: StateFlow<Set<MelodyMood>> = _starterMoods.asStateFlow()

    private val _starterArtists = MutableStateFlow<Set<String>>(emptySet())
    val starterArtists: StateFlow<Set<String>> = _starterArtists.asStateFlow()

    /** Rotating radio-seed cursor for endless top-ups. */
    var seedCursor: Int = 0
        private set

    private const val KEY_MELODY_SNAPSHOT = "melody_snapshot"
    private const val KEY_MELODY_BUILT_AT = "melody_built_at"
    private const val KEY_MELODY_SNAPSHOT_VERSION = "melody_snapshot_version"
    private const val KEY_MELODY_SNAPSHOT_APP = "melody_snapshot_app"
    private const val KEY_MELODY_SNAPSHOT_ALGORITHM = "melody_snapshot_algorithm"

    private const val SNAPSHOT_VERSION = 2

    /** A stored mix older than a day rebuilds instead of restoring. */
    const val MELODY_TTL_MS = 24L * 60 * 60 * 1000

    /** A stored mix older than an hour refreshes silently in foreground. */
    const val MELODY_AUTO_REFRESH_MS = 60L * 60 * 1000

    fun snapshotAgeMs(): Long? =
        readSnapshot()?.let { (_, builtAt) -> System.currentTimeMillis() - builtAt }

    fun saveSnapshot(songs: List<Song>) {
        if (!this::prefs.isInitialized || songs.isEmpty()) return
        prefs.edit()
            .putString(KEY_MELODY_SNAPSHOT, encodeMelodySnapshot(songs))
            .putLong(KEY_MELODY_BUILT_AT, System.currentTimeMillis())
            .putInt(KEY_MELODY_SNAPSHOT_VERSION, SNAPSHOT_VERSION)
            .putInt(KEY_MELODY_SNAPSHOT_APP, BuildConfig.VERSION_CODE)
            .putInt(KEY_MELODY_SNAPSHOT_ALGORITHM, MelodyEngine.ALGORITHM_VERSION)
            .apply()
    }

    fun readSnapshot(): Pair<List<Song>, Long>? {
        if (!this::prefs.isInitialized) return null
        val version = prefs.getInt(KEY_MELODY_SNAPSHOT_VERSION, 0)
        if (version != SNAPSHOT_VERSION) {
            clearSnapshot()
            return null
        }
        if (prefs.getInt(KEY_MELODY_SNAPSHOT_ALGORITHM, 0) != MelodyEngine.ALGORITHM_VERSION) {
            clearSnapshot()
            return null
        }
        val text = prefs.getString(KEY_MELODY_SNAPSHOT, null)?.takeIf { it.isNotEmpty() }
            ?: return null
        val songs = decodeMelodySnapshot(text)
        if (songs.isEmpty()) return null
        return songs to prefs.getLong(KEY_MELODY_SNAPSHOT_BUILT_AT_SAFE, 0L)
    }

    // Aliased cleanly to match write key above.
    private const val KEY_MELODY_SNAPSHOT_BUILT_AT_SAFE = KEY_MELODY_BUILT_AT

    fun clearSnapshot() {
        if (!this::prefs.isInitialized) return
        prefs.edit()
            .remove(KEY_MELODY_SNAPSHOT)
            .remove(KEY_MELODY_BUILT_AT)
            .remove(KEY_MELODY_SNAPSHOT_VERSION)
            .remove(KEY_MELODY_SNAPSHOT_APP)
            .remove(KEY_MELODY_SNAPSHOT_ALGORITHM)
            .apply()
    }

    fun init(context: Context) {
        if (this::prefs.isInitialized) return
        prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        _mood.value = runCatching { MelodyMood.valueOf(prefs.getString(KEY_MOOD, null) ?: "MELODY") }
            .getOrDefault(MelodyMood.MELODY)
        _tuner.value = MelodyTuner(
            discovery = prefs.getFloat(KEY_DISCOVERY, 0.35f),
            memory = prefs.getFloat(KEY_MEMORY, 0.5f),
            excludedGenres = prefs.getStringSet(KEY_EXCLUDED_GENRES, emptySet()).orEmpty(),
            similarLiked = prefs.getBoolean(KEY_SIMILAR_LIKED, true),
        )
        _bannedIds.value = prefs.getStringSet(KEY_BANNED, emptySet()).orEmpty().take(MAX_BANNED).toSet()
        _skipCounts.value = readSkips()
        _starterMoods.value = prefs.getStringSet(KEY_STARTER_MOODS, emptySet()).orEmpty()
            .mapNotNull { name -> runCatching { MelodyMood.valueOf(name) }.getOrNull() }
            .filter { it != MelodyMood.MELODY }
            .toSet()
        _starterArtists.value = prefs.getStringSet(KEY_STARTER_ARTISTS, emptySet()).orEmpty()
        seedCursor = prefs.getInt(KEY_SEED_CURSOR, 0)
    }

    fun setMood(mood: MelodyMood) {
        _mood.value = mood
        if (this::prefs.isInitialized) prefs.edit().putString(KEY_MOOD, mood.name).apply()
    }

    fun setDiscovery(value: Float) {
        _tuner.value = _tuner.value.withDiscovery(value)
        if (this::prefs.isInitialized) prefs.edit().putFloat(KEY_DISCOVERY, _tuner.value.discovery).apply()
    }

    fun setMemory(value: Float) {
        _tuner.value = _tuner.value.withMemory(value)
        if (this::prefs.isInitialized) prefs.edit().putFloat(KEY_MEMORY, _tuner.value.memory).apply()
    }

    fun setSimilarLiked(value: Boolean) {
        _tuner.value = _tuner.value.withSimilarLiked(value)
        if (this::prefs.isInitialized) {
            prefs.edit().putBoolean(KEY_SIMILAR_LIKED, _tuner.value.similarLiked).apply()
        }
    }

    fun toggleGenre(genre: String, enabled: Boolean) {
        _tuner.value = _tuner.value.toggleGenre(genre, enabled)
        if (this::prefs.isInitialized) {
            prefs.edit().putStringSet(KEY_EXCLUDED_GENRES, _tuner.value.excludedGenres).apply()
        }
    }

    fun setExcludedGenres(genres: Set<String>) {
        _tuner.value = _tuner.value.copy(excludedGenres = genres.map { it.lowercase() }.toSet())
        if (this::prefs.isInitialized) {
            prefs.edit().putStringSet(KEY_EXCLUDED_GENRES, _tuner.value.excludedGenres).apply()
        }
    }

    fun setStarterPicks(moods: Set<MelodyMood>, artists: Set<String>) {
        val cleanMoods = moods.filter { it != MelodyMood.MELODY }.toSet()
        val cleanArtists = artists.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        _starterMoods.value = cleanMoods
        _starterArtists.value = cleanArtists
        if (this::prefs.isInitialized) {
            prefs.edit()
                .putStringSet(KEY_STARTER_MOODS, cleanMoods.map { it.name }.toSet())
                .putStringSet(KEY_STARTER_ARTISTS, cleanArtists)
                .apply()
        }
    }

    fun ban(videoId: String) {
        if (videoId.isBlank()) return
        val next = (_bannedIds.value + videoId).toList().takeLast(MAX_BANNED).toSet()
        _bannedIds.value = next
        if (this::prefs.isInitialized) prefs.edit().putStringSet(KEY_BANNED, next).apply()
    }

    fun unban(videoId: String) {
        _bannedIds.value = _bannedIds.value - videoId
        if (this::prefs.isInitialized) prefs.edit().putStringSet(KEY_BANNED, _bannedIds.value).apply()
    }

    fun isBanned(videoId: String): Boolean = videoId in _bannedIds.value

    fun clearBans() {
        _bannedIds.value = emptySet()
        if (this::prefs.isInitialized) prefs.edit().remove(KEY_BANNED).apply()
    }

    /** Record a skip for Melody training (<30s into track or <40% played). */
    fun recordSkip(videoId: String) {
        if (videoId.isBlank() || videoId in _bannedIds.value) return
        val next = _skipCounts.value.toMutableMap()
        next[videoId] = (next[videoId] ?: 0) + 1
        if (next.size > MAX_SKIPS) {
            next.entries.sortedBy { it.value }.take(next.size - MAX_SKIPS)
                .forEach { next.remove(it.key) }
        }
        _skipCounts.value = next
        if (this::prefs.isInitialized) writeSkips(next)
    }

    /** Full listen forgives one skip. */
    fun recordFullListen(videoId: String) {
        val cur = _skipCounts.value[videoId] ?: return
        val next = if (cur <= 1) {
            _skipCounts.value - videoId
        } else {
            _skipCounts.value + (videoId to cur - 1)
        }
        _skipCounts.value = next
        if (this::prefs.isInitialized) writeSkips(next)
    }

    fun advanceSeedCursor() {
        seedCursor += 1
        if (this::prefs.isInitialized) prefs.edit().putInt(KEY_SEED_CURSOR, seedCursor).apply()
    }

    private fun readSkips(): Map<String, Int> {
        val raw = prefs.getString(KEY_SKIPS, "").orEmpty()
        if (raw.isBlank()) return emptyMap()
        val out = mutableMapOf<String, Int>()
        raw.split(";").forEach { part ->
            val kv = part.split("=")
            if (kv.size != 2 || kv[0].isBlank()) return@forEach
            val count = kv[1].toIntOrNull() ?: return@forEach
            if (count > 0) out[kv[0]] = count
        }
        return out
    }

    private fun writeSkips(map: Map<String, Int>) {
        prefs.edit().putString(KEY_SKIPS, map.entries.joinToString(";") { "${it.key}=${it.value}" }).apply()
    }
}

/** Plain-text Melody snapshot encoder (tested cleanly on JVM without JSON allocations). */
fun encodeMelodySnapshot(songs: List<Song>): String =
    songs.take(200).joinToString("\u001e") { s ->
        listOf(
            s.videoId,
            s.title,
            s.artist,
            s.albumName.orEmpty(),
            s.thumbnailUrl.orEmpty(),
        ).joinToString("\u001f") { it.replace("\u001e", " ").replace("\u001f", " ") }
    }

/** Inverse of [encodeMelodySnapshot]. */
fun decodeMelodySnapshot(text: String): List<Song> {
    if (text.isBlank()) return emptyList()
    return text.split("\u001e").mapNotNull { row ->
        val f = row.split("\u001f")
        val id = f.getOrNull(0)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        Song(
            videoId = id,
            title = f.getOrNull(1).orEmpty(),
            artist = f.getOrNull(2).orEmpty(),
            thumbnailUrl = f.getOrNull(4)?.takeIf { it.isNotBlank() },
            albumName = f.getOrNull(3)?.takeIf { it.isNotBlank() },
        )
    }
}
