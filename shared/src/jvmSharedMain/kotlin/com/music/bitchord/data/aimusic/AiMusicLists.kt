/*
 * Adapted from Dial (https://github.com/steeben/Dial) by steeben.
 * Copyright (C) 2026 Shivanshu (code4nigel) <https://github.com/code4nigel/ChordPlay>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.music.bitchord.data.aimusic

import com.music.bitchord.data.DebugLog as Log
import com.music.bitchord.data.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.Base64

/**
 * The community lists of AI music acts behind [AiMusicFilter], and the cached copies
 * kept on device:
 *
 * - YouTube Music, by channel id:
 *   - Flow AI music list (Soul Over AI, CC BY 4.0, and Spotify AI Blocker, MIT).
 *   - aimusic-blocklist (MIT): channels scored as AI from release patterns.
 * - Spotify, by artist id: Spotify AI Blocker (MIT).
 *
 * Refreshed at most once a day and only while the toggle is on.
 */
object AiMusicLists {

    private const val TAG = "AiMusic"

    const val FLOW_BASE = "https://ailist.flow-tube.org/v1/aimusic/"
    const val CRAWLER_URL = "https://raw.githubusercontent.com/mankidev/aimusic-blocklist/main/data/blocklist.json"
    const val SPOTIFY_URL = "https://raw.githubusercontent.com/CennoxX/spotify-ai-blocker/main/SpotifyAiArtists.csv"

    /** How old a kept copy may be before it is asked for again. */
    const val REFRESH_MS = 24L * 60 * 60 * 1000

    private const val DIR = "ai-music"
    private const val FLOW_FILE = "flow-aimusic.json"
    private const val CRAWLER_FILE = "aimusic-blocklist.json"
    private const val SPOTIFY_FILE = "spotify-ai-artists.csv"

    private val CHANNEL_ID = Regex("^UC[A-Za-z0-9_-]{22}$")
    private val SPOTIFY_ID = Regex("^[0-9A-Za-z]{22}$")

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val mutex = Mutex()

    /** Where the Flow manifest says its full list is, and the SHA-256 that list must have. */
    data class SnapshotRef(val path: String, val sha256: String?)

    /**
     * The snapshot named in a Flow `manifest.json`. Its payload is base64 JSON;
     * the signature beside it is not checked here, the hash of the snapshot is.
     */
    fun flowSnapshotRef(manifest: String): SnapshotRef? = runCatching<SnapshotRef?> {
        val envelope = json.parseToJsonElement(manifest) as JsonObject
        val payload = String(Base64.getDecoder().decode(envelope.text("payload")!!), Charsets.UTF_8)
        val root = json.parseToJsonElement(payload) as JsonObject
        if (root.text("list") != "aimusic") return@runCatching null
        val snapshot = root["snapshot"] as JsonObject
        val path = snapshot.text("path")?.takeIf { it.isNotBlank() && ".." !in it && "://" !in it }
            ?: return@runCatching null
        SnapshotRef(path, snapshot.text("sha256"))
    }.getOrNull()

    /** The blocked channel ids of a Flow `snapshot/<version>.json`. */
    fun flowChannels(snapshot: String): Set<String> = runCatching<Set<String>> {
        val root = json.parseToJsonElement(snapshot) as JsonObject
        val ids = (root["block"] as? JsonObject)?.get("ids") as? JsonArray ?: return@runCatching emptySet()
        ids.mapNotNullTo(HashSet()) { (it as? JsonPrimitive)?.contentOrNull?.takeIf(CHANNEL_ID::matches) }
    }.getOrDefault(emptySet())

    /** The channel ids of aimusic-blocklist's `blocklist.json` (`artists[].channelId`). */
    fun crawlerChannels(body: String): Set<String> = runCatching<Set<String>> {
        val root = json.parseToJsonElement(body) as JsonObject
        val artists = root["artists"] as? JsonArray ?: return@runCatching emptySet()
        artists.mapNotNullTo(HashSet()) { (it as? JsonObject)?.text("channelId")?.takeIf(CHANNEL_ID::matches) }
    }.getOrDefault(emptySet())

    /** The artist ids of Spotify AI Blocker's CSV: a header `artist,id`, then one artist per line. */
    fun spotifyArtists(csv: String): Set<String> =
        csv.lineSequence()
            .mapNotNull { line -> line.trim().substringAfterLast(',').trim().trim('"').takeIf(SPOTIFY_ID::matches) }
            .toHashSet()

    /**
     * Reads the kept lists into [AiMusicFilter], first fetching any that is
     * missing or older than [REFRESH_MS] (all of them when [force]).
     */
    suspend fun refresh(filesDir: File, force: Boolean = false) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val dir = File(filesDir, DIR).apply { mkdirs() }
            val flow = File(dir, FLOW_FILE)
            val crawler = File(dir, CRAWLER_FILE)
            val spotify = File(dir, SPOTIFY_FILE)
            if (force || stale(flow)) fetchFlow()?.let { flow.writeAtomically(it) }
            if (force || stale(crawler)) get(CRAWLER_URL)?.takeIf { crawlerChannels(it).isNotEmpty() }
                ?.let { crawler.writeAtomically(it) }
            if (force || stale(spotify)) get(SPOTIFY_URL)?.takeIf { spotifyArtists(it).isNotEmpty() }
                ?.let { spotify.writeAtomically(it) }

            val youtube = HashSet<String>()
            flow.readOrNull()?.let { youtube += flowChannels(it) }
            crawler.readOrNull()?.let { youtube += crawlerChannels(it) }
            val spotifyIds = spotify.readOrNull()?.let(::spotifyArtists).orEmpty()
            AiMusicFilter.setLists(youtube, spotifyIds)
            Log.d(TAG, "lists in use: ${youtube.size} YouTube Music channels, ${spotifyIds.size} Spotify artists")
        }
    }

    /** Takes the lists out of use; the kept copies stay for the next time it is switched on. */
    fun clear() = AiMusicFilter.setLists(emptySet(), emptySet())

    private fun fetchFlow(): String? {
        val ref = get(FLOW_BASE + "manifest.json")?.let(::flowSnapshotRef) ?: return null
        val body = get(FLOW_BASE + ref.path) ?: return null
        if (ref.sha256 != null && !sha256(body).equals(ref.sha256, ignoreCase = true)) {
            Log.w(TAG, "Flow snapshot hash does not match its manifest; kept the last copy")
            return null
        }
        return body.takeIf { flowChannels(it).isNotEmpty() }
    }

    private fun get(url: String): String? = runCatching {
        Http.client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (response.isSuccessful) response.body?.string() else {
                Log.w(TAG, "GET $url: http ${response.code}")
                null
            }
        }
    }.onFailure { Log.w(TAG, "GET $url failed: ${it.message}") }.getOrNull()

    private fun stale(file: File): Boolean =
        !file.isFile || System.currentTimeMillis() - file.lastModified() > REFRESH_MS

    private fun File.readOrNull(): String? = runCatching { takeIf { it.isFile }?.readText() }.getOrNull()

    private fun File.writeAtomically(text: String) {
        val temp = File(parentFile, "$name.tmp")
        temp.writeText(text)
        if (!temp.renameTo(this)) {
            delete()
            temp.renameTo(this)
        }
    }

    private fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
}
