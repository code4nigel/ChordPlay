/*
 * Copyright (C) 2024-2026 Kushagra Singh and BitChord contributors <https://github.com/kushagrasinghx/BitChord>
 * Copyright (C) 2026 Shivanshu (code4nigel) <https://github.com/code4nigel/ChordPlay>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.music.bitchord.data.spotify

import android.content.Context
import android.content.SharedPreferences
import com.music.bitchord.data.model.Song
import com.music.bitchord.download.CoalescingWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class LocalPlaylist(
    val id: String,
    val title: String,
    val songs: List<Song>,
) {
    val browseId: String get() = "local:playlist:$id"
}

object LocalPlaylistStore {
    private const val PREF_NAME = "bitchord_local_playlists"
    private const val KEY_PLAYLISTS = "playlists_json"

    private lateinit var prefs: SharedPreferences

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _playlists = MutableStateFlow<List<LocalPlaylist>>(emptyList())
    val playlists = _playlists.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        ioScope.launch { load() }
    }

    private fun load() {
        if (!this::prefs.isInitialized) return
        val raw = prefs.getString(KEY_PLAYLISTS, null) ?: return
        runCatching {
            val list = mutableListOf<LocalPlaylist>()
            val jsonArray = JSONArray(raw)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.getString("id")
                val title = obj.getString("title")
                val songsArray = obj.getJSONArray("songs")
                val songs = mutableListOf<Song>()
                for (j in 0 until songsArray.length()) {
                    val sObj = songsArray.getJSONObject(j)
                    val song = Song(
                        videoId = sObj.optString("videoId", ""),
                        title = sObj.optString("title", ""),
                        artist = sObj.optString("artist", ""),
                        thumbnailUrl = sObj.optString("thumbnailUrl", null).takeIf { !it.isNullOrBlank() && it != "null" },
                        durationText = sObj.optString("durationText", null).takeIf { !it.isNullOrBlank() && it != "null" },
                        artistId = sObj.optString("artistId", null).takeIf { !it.isNullOrBlank() && it != "null" },
                        albumId = sObj.optString("albumId", null).takeIf { !it.isNullOrBlank() && it != "null" },
                        albumName = sObj.optString("albumName", null).takeIf { !it.isNullOrBlank() && it != "null" },
                        isVideo = sObj.optBoolean("isVideo", false),
                    )
                    songs.add(song)
                }
                list.add(LocalPlaylist(id, title, songs))
            }
            // Merge under any edit that somehow landed before the disk read completed.
            _playlists.update { current -> (current + list).distinctBy { it.id } }
        }
    }

    fun savePlaylist(title: String, songs: List<Song>): LocalPlaylist {
        val id = "sp_local_" + System.currentTimeMillis()
        val playlist = LocalPlaylist(id = id, title = title, songs = songs.map { it.copy() })
        _playlists.update { current -> listOf(playlist) + current.filterNot { it.id == id } }
        persist()
        return playlist
    }

    fun deletePlaylist(id: String) {
        _playlists.update { current ->
            current.filterNot { it.id == id || it.browseId == id || it.id == id.removePrefix("local:playlist:") }
        }
        persist()
    }

    fun renamePlaylist(id: String, newTitle: String) {
        val cleanId = id.removePrefix("local:playlist:").removePrefix("VL")
        _playlists.update { current ->
            current.map {
                if (it.id == cleanId || it.id == id || it.browseId == id) {
                    it.copy(title = newTitle)
                } else it
            }
        }
        persist()
    }

    fun getPlaylist(id: String): LocalPlaylist? {
        val cleanId = id.removePrefix("local:playlist:").removePrefix("VL")
        return _playlists.value.firstOrNull { it.id == cleanId || it.id == id || it.browseId == id }
    }

    fun addSong(id: String, song: Song): Boolean {
        return addSongs(id, listOf(song)) > 0
    }

    fun addSongs(id: String, songs: List<Song>): Int {
        val cleanId = id.removePrefix("local:playlist:").removePrefix("VL")
        var addedCount = 0
        _playlists.update { current ->
            addedCount = 0
            current.map { pl ->
                if (pl.id == cleanId || pl.id == id || pl.browseId == id) {
                    val existingIds = pl.songs.map { it.videoId }.toSet()
                    val newSongs = songs.filter { it.videoId.isNotBlank() && it.videoId !in existingIds }
                    if (newSongs.isNotEmpty()) {
                        addedCount += newSongs.size
                        pl.copy(songs = pl.songs + newSongs)
                    } else pl
                } else pl
            }
        }
        if (addedCount > 0) {
            persist()
        }
        return addedCount
    }

    fun removeSong(id: String, videoId: String): Boolean {
        val cleanId = id.removePrefix("local:playlist:").removePrefix("VL")
        var changed = false
        _playlists.update { current ->
            changed = false
            current.map { pl ->
                if (pl.id == cleanId || pl.id == id || pl.browseId == id) {
                    val filtered = pl.songs.filterNot { it.videoId == videoId }
                    if (filtered.size != pl.songs.size) {
                        changed = true
                        pl.copy(songs = filtered)
                    } else pl
                } else pl
            }
        }
        if (changed) {
            persist()
        }
        return changed
    }

    fun removeOccurrence(id: String, song: Song): Boolean {
        var removed = false
        _playlists.update { current ->
            removed = false
            current.map { playlist ->
                if (playlist.id != id) return@map playlist
                val index = playlist.songs.indexOfFirst { it === song }
                if (index < 0) return@map playlist
                removed = true
                playlist.copy(songs = playlist.songs.toMutableList().apply { removeAt(index) })
            }
        }
        if (removed) persist()
        return removed
    }

    /**
     * Encodes and writes off the caller's thread, coalesced to the latest list.
     *
     * Every edit re-serializes every playlist and every song in it, and those
     * edits come from click handlers and ViewModel launches on Main. The edit
     * itself is already in [_playlists] when this is called, so readers never
     * wait on it; the sink always encodes the newest list, so a burst of edits
     * costs one write.
     */
    private val writer = CoalescingWriter { _ -> writeNow(_playlists.value) }

    private fun persist() {
        if (!this::prefs.isInitialized) return
        writer.submit(emptyMap())
    }

    private fun writeNow(list: List<LocalPlaylist>) {
        runCatching {
            val jsonArray = JSONArray()
            for (playlist in list) {
                val pObj = JSONObject()
                pObj.put("id", playlist.id)
                pObj.put("title", playlist.title)
                val songsArray = JSONArray()
                for (song in playlist.songs) {
                    val sObj = JSONObject()
                    sObj.put("videoId", song.videoId)
                    sObj.put("title", song.title)
                    sObj.put("artist", song.artist)
                    sObj.put("thumbnailUrl", song.thumbnailUrl)
                    sObj.put("durationText", song.durationText)
                    sObj.put("artistId", song.artistId)
                    sObj.put("albumId", song.albumId)
                    sObj.put("albumName", song.albumName)
                    sObj.put("isVideo", song.isVideo)
                    songsArray.put(sObj)
                }
                pObj.put("songs", songsArray)
                jsonArray.put(pObj)
            }
            prefs.edit().putString(KEY_PLAYLISTS, jsonArray.toString()).apply()
        }
    }
}
