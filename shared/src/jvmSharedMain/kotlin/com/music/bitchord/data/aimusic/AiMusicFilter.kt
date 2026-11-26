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
package com.music.bitchord.data.aimusic

import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * "Hide AI music": takes songs and artists of known AI music acts out of what
 * YouTube Music and Spotify suggest — search, Home shelves, mixes, radio, and
 * recommendations. What the listener saved themselves (their library, their
 * own playlists, their history) is not filtered here, and neither are device files.
 */
object AiMusicFilter {

    /** The switch in Settings. The lists are only read while it is on. */
    @Volatile
    var enabled: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            _changes.update { it + 1 }
        }

    private val _changes = MutableStateFlow(0)

    /**
     * Increments whenever the filter's active output may have changed: toggled on/off
     * or refreshed lists loaded. Cached feeds re-apply when this changes.
     */
    val changes: StateFlow<Int> = _changes.asStateFlow()

    @Volatile
    private var youtubeChannels: Set<String> = emptySet()

    @Volatile
    private var spotifyArtists: Set<String> = emptySet()

    /** Puts the lists in use: YouTube Music channel ids (`UC…`) and Spotify artist ids. */
    fun setLists(youtube: Set<String>, spotify: Set<String>) {
        if (youtube == youtubeChannels && spotify == spotifyArtists) return
        youtubeChannels = youtube
        spotifyArtists = spotify
        _changes.update { it + 1 }
    }

    /** How many YouTube Music channels and Spotify artists the lists in use hold. */
    val listSizes: Pair<Int, Int> get() = youtubeChannels.size to spotifyArtists.size

    /** On, and with a list to test against. */
    val active: Boolean get() = enabled && (youtubeChannels.isNotEmpty() || spotifyArtists.isNotEmpty())

    /** Whether [channelId] is the YouTube Music channel of a listed AI act. */
    fun isAiChannel(channelId: String?): Boolean =
        active && channelId != null && channelId in youtubeChannels

    /** Whether [artistId] (bare, or a `spotify:artist:` uri) is a listed AI act on Spotify. */
    fun isAiSpotifyArtist(artistId: String?): Boolean =
        active && artistId != null && artistId.removePrefix(SPOTIFY_ARTIST_URI) in spotifyArtists

    /** Whether any of [artistIds] is a listed AI act on Spotify. */
    fun anyAiSpotifyArtist(artistIds: List<String>): Boolean =
        active && artistIds.any(::isAiSpotifyArtist)

    /** Whether one of this song's credited artists is a listed AI act. A device file never is. */
    fun isAi(song: Song): Boolean {
        if (!active || song.localUri != null) return false
        return isAiChannel(song.artistId) || song.artists.any { isAiChannel(it.browseId) }
    }

    /** Whether this card is a listed AI act's page, or a song or release credited to one. */
    fun isAi(item: ShelfItem): Boolean {
        if (!active) return false
        return isAiChannel(item.browseId) || item.artistIds.any(::isAiChannel)
    }

    /**
     * [songs] without AI music. [keepVideoId] is the song the listener chose to
     * play: a mix or radio built from it keeps it, so that a tap always plays
     * what was tapped.
     */
    fun songs(songs: List<Song>, keepVideoId: String? = null): List<Song> {
        if (!active) return songs
        return songs.filter { it.videoId == keepVideoId || !isAi(it) }
    }

    /**
     * [shelves] without AI cards. A shelf that had only AI cards goes; a shelf
     * that came with no cards stays as it was.
     */
    fun shelves(shelves: List<HomeShelf>): List<HomeShelf> {
        if (!active) return shelves
        return shelves.mapNotNull { shelf ->
            if (shelf.items.isEmpty()) return@mapNotNull shelf
            val kept = shelf.items.filterNot(::isAi)
            when {
                kept.isEmpty() -> null
                kept.size == shelf.items.size -> shelf
                else -> shelf.copy(items = kept)
            }
        }
    }

    /** [items] without AI cards. */
    fun items(items: List<ShelfItem>): List<ShelfItem> = if (active) items.filterNot(::isAi) else items

    /** Search rows without AI songs and AI artists. */
    fun searchRows(rows: List<SearchResult>): List<SearchResult> {
        if (!active) return rows
        return rows.filterNot { row ->
            when (row) {
                is SearchResult.TopTrack -> isAi(row.song)
                is SearchResult.Track -> isAi(row.song)
                is SearchResult.Browse -> isAiChannel(row.item.browseId)
            }
        }
    }

    private const val SPOTIFY_ARTIST_URI = "spotify:artist:"
}
