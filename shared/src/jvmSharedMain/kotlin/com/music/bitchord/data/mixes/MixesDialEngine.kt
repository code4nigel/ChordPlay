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
package com.music.bitchord.data.mixes

import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.settings.isArtistDialCandidate
import com.music.bitchord.data.settings.speedDialStableId
import com.music.bitchord.data.spotify.SPOTIFY_PAGE_PREFIX
import com.music.bitchord.data.spotify.SpotifyLibrary
import com.music.bitchord.data.spotify.SpotifyPlaylist
import java.time.LocalTime
import kotlin.random.Random

/**
 * Curates the Mixes Dial and Artist Dial items across YouTube Music and Spotify.
 * Pure domain logic so deduplication, interleaving, and sleep-time rules are unit-testable.
 */
object MixesDialEngine {

    const val DEFAULT_LIMIT = 18

    private val NOT_A_WORD = Regex("[^\\p{L}\\p{N}]+")

    private val SPOTIFY_PERSONAL = setOf(
        "discover weekly",
        "release radar",
        "on repeat",
        "repeat rewind",
        "daylist",
        "your top songs",
        "time capsule",
    )

    /**
     * Normalized mix family key: strips punctuation, case, and trailing digits so variants like
     * "Chill Mix 01" and "Chill Mix 02" or "My Mix 1" and "My Mix 4" do not duplicate on a dial.
     */
    fun mixType(title: String): String {
        val words = title.lowercase().split(NOT_A_WORD).filter { it.isNotEmpty() }
        return words.dropLastWhile { word -> word.all(Char::isDigit) }
            .joinToString("")
            .ifEmpty { words.joinToString("").ifEmpty { title.trim().lowercase() } }
    }

    /** Sleep mixes appear during late-night listening hours (21:30–04:00 local time). */
    fun isSleepTime(hour: Int, minute: Int): Boolean =
        (hour > 21 || (hour == 21 && minute >= 30)) || (hour < 4 || (hour == 4 && minute == 0))

    fun isSleepTime(time: LocalTime = LocalTime.now()): Boolean =
        isSleepTime(time.hour, time.minute)

    fun isSleepMix(item: ShelfItem): Boolean =
        item.title.contains("sleep", ignoreCase = true)

    fun isYouTubeMix(item: ShelfItem): Boolean {
        if (item.videoId != null) return false
        val id = item.browseId?.removePrefix("VL") ?: return false
        if (id.startsWith("RDTMAK")) return true
        val playlist = id.startsWith("RD") || id.startsWith("PL") || id.startsWith("OLAK")
        return playlist && (
            item.title.contains("mix", ignoreCase = true) ||
                item.subtitle.contains("mix", ignoreCase = true) ||
                item.subtitle.contains("radio", ignoreCase = true)
            )
    }

    fun isSpotifyPersonalMix(playlist: SpotifyPlaylist): Boolean {
        if (playlist.id == SpotifyLibrary.LIKED_ID) return false
        if (!playlist.owner.equals("Spotify", ignoreCase = true)) return false
        val name = playlist.name.trim().lowercase()
        return "mix" in name || name in SPOTIFY_PERSONAL || SPOTIFY_PERSONAL.any { name.startsWith(it) }
    }

    fun spotifyPlaylistToTile(playlist: SpotifyPlaylist): ShelfItem = ShelfItem(
        title = playlist.name,
        subtitle = playlist.owner ?: "Spotify",
        thumbnailUrl = playlist.imageUrl,
        videoId = null,
        browseId = SPOTIFY_PAGE_PREFIX + playlist.id,
    )

    /**
     * Blends YouTube Music and Spotify mixes alternately, removing hidden items, daytime sleep
     * mixes, duplicate IDs, and duplicate normalized [mixType]s.
     */
    fun pickMixes(
        youTube: List<ShelfItem>,
        spotify: List<ShelfItem>,
        hiddenIds: Set<String> = emptySet(),
        sleepTime: Boolean = isSleepTime(),
        limit: Int = DEFAULT_LIMIT,
        random: Random? = null,
    ): List<ShelfItem> {
        val allowed: (ShelfItem) -> Boolean = { item ->
            val id = item.speedDialStableId()
            id.isNotBlank() && id !in hiddenIds && (sleepTime || !isSleepMix(item))
        }
        val ytClean = youTube.filter(allowed).distinctBy { it.speedDialStableId() }
            .let { if (random != null) it.shuffled(random) else it }
        val spClean = spotify.filter(allowed).distinctBy { it.speedDialStableId() }
            .let { if (random != null) it.shuffled(random) else it }

        val result = ArrayList<ShelfItem>(limit)
        val seenIds = HashSet<String>()
        val seenTypes = HashSet<String>()

        fun tryAdd(item: ShelfItem): Boolean {
            val id = item.speedDialStableId()
            val kind = mixType(item.title)
            if (id in seenIds || kind in seenTypes) return false
            seenIds += id
            seenTypes += kind
            result += item
            return true
        }

        val leadFirst = if (random?.nextBoolean() != false) listOf(ytClean, spClean) else listOf(spClean, ytClean)
        val maxLen = maxOf(ytClean.size, spClean.size)
        var i = 0
        while (result.size < limit && i < maxLen) {
            for (pool in leadFirst) {
                if (result.size >= limit) break
                val candidate = pool.getOrNull(i) ?: continue
                tryAdd(candidate)
            }
            i++
        }
        return result
    }

    /**
     * Extracts artist tiles for the optional Artist Dial: pinned artists in pin order first,
     * followed by artist candidates from Home shelves, excluding [hiddenIds].
     */
    fun extractArtistDialItems(
        shelves: List<HomeShelf>,
        pinnedSpeedDial: List<ShelfItem>,
        hiddenIds: Set<String> = emptySet(),
        limit: Int = DEFAULT_LIMIT,
    ): List<ShelfItem> {
        val pinnedArtists = pinnedSpeedDial.filter {
            it.isArtistDialCandidate() && it.speedDialStableId() !in hiddenIds
        }
        val shelfArtists = shelves.asSequence()
            .flatMap { it.items.asSequence() }
            .filter { it.isArtistDialCandidate() && it.speedDialStableId() !in hiddenIds }

        return (pinnedArtists.asSequence() + shelfArtists)
            .distinctBy { it.speedDialStableId() }
            .take(limit)
            .toList()
    }
}
