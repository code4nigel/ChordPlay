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

import com.music.bitchord.data.aimusic.AiMusicFilter
import com.music.bitchord.data.innertube.InnertubeParser
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.Song

/**
 * Shared < 1 ms RAM filter & taste scoring used uniformly by Home Feed, Chord Wave,
 * Your Space Dials, ChordPlay Melody, and AutoPlay.
 *
 * Precedence rule:
 * 1. Hard Blocks (blocked songs, blocked artists, blocked languages/genres/themes, AI music when ON)
 *    ALWAYS exclude an item across every surface.
 * 2. Remaining items are scored against the user's [TasteSnapshot] weights.
 */
object TasteFilter {

    fun isBlocked(
        song: Song,
        snapshot: TasteSnapshot = TasteProfileRepository.snapshot.value,
        tagLookup: (Song) -> ResolvedTags = TagResolver::resolveImmediate,
    ): Boolean {
        if (snapshot.isSongBlocked(song.videoId)) return true

        val artistIds = (listOfNotNull(song.artistId) + song.artists.mapNotNull { it.browseId }).distinct()
        if (snapshot.isArtistBlocked(song.artist, artistIds)) return true
        // Also check individual credited artist names (e.g. "Artist A, Artist B")
        for (credit in song.artists) {
            if (snapshot.isArtistBlocked(credit.name)) return true
        }

        if (AiMusicFilter.isAi(song)) return true
        if (snapshot.hideAiMusic && (snapshot.isUserMarkedAi(song.artist, artistIds) || song.artists.any { snapshot.isUserMarkedAi(it.name) })) {
            return true
        }

        if (snapshot.blockedLangMask != 0 || snapshot.blockedGenreMask != 0 || snapshot.blockedThemeMask != 0) {
            val tags = tagLookup(song)
            if ((tags.langMask and snapshot.blockedLangMask) != 0) return true
            if ((tags.genreMask and snapshot.blockedGenreMask) != 0) return true
            if ((tags.themeMask and snapshot.blockedThemeMask) != 0) return true
        }
        return false
    }

    fun isBlocked(
        item: ShelfItem,
        snapshot: TasteSnapshot = TasteProfileRepository.snapshot.value,
        tagLookup: (ShelfItem) -> ResolvedTags = TagResolver::resolveImmediate,
    ): Boolean {
        if (snapshot.isSongBlocked(item.videoId)) return true
        val artistName = InnertubeParser.artistFromSubtitle(item.subtitle)
        val ids = buildList {
            addAll(item.artistIds)
            if (item.videoId == null && item.browseId?.startsWith("UC") == true) {
                add(item.browseId)
            }
        }
        if (snapshot.isArtistBlocked(artistName, ids)) return true
        if (item.videoId == null && item.browseId?.startsWith("UC") == true && snapshot.isArtistBlocked(item.title, listOf(item.browseId))) {
            return true
        }

        if (AiMusicFilter.isAi(item)) return true
        if (snapshot.hideAiMusic && (snapshot.isUserMarkedAi(artistName, ids) || (item.videoId == null && snapshot.isUserMarkedAi(item.title, ids)))) {
            return true
        }

        if (snapshot.blockedLangMask != 0 || snapshot.blockedGenreMask != 0 || snapshot.blockedThemeMask != 0) {
            val tags = tagLookup(item)
            if ((tags.langMask and snapshot.blockedLangMask) != 0) return true
            if ((tags.genreMask and snapshot.blockedGenreMask) != 0) return true
            if ((tags.themeMask and snapshot.blockedThemeMask) != 0) return true
        }
        return false
    }

    fun filterSongs(
        songs: List<Song>,
        snapshot: TasteSnapshot = TasteProfileRepository.snapshot.value,
        keepVideoId: String? = null,
        tagLookup: (Song) -> ResolvedTags = TagResolver::resolveImmediate,
    ): List<Song> {
        if (songs.isEmpty()) return songs
        return songs.filter { song ->
            song.videoId == keepVideoId || !isBlocked(song, snapshot, tagLookup)
        }
    }

    fun filterItems(
        items: List<ShelfItem>,
        snapshot: TasteSnapshot = TasteProfileRepository.snapshot.value,
        tagLookup: (ShelfItem) -> ResolvedTags = TagResolver::resolveImmediate,
    ): List<ShelfItem> {
        if (items.isEmpty()) return items
        return items.filterNot { isBlocked(it, snapshot, tagLookup) }
    }

    fun filterShelves(
        shelves: List<HomeShelf>,
        snapshot: TasteSnapshot = TasteProfileRepository.snapshot.value,
        tagLookup: (ShelfItem) -> ResolvedTags = TagResolver::resolveImmediate,
    ): List<HomeShelf> {
        if (shelves.isEmpty()) return shelves
        return shelves.mapNotNull { shelf ->
            if (shelf.items.isEmpty()) return@mapNotNull shelf
            val kept = shelf.items.filterNot { isBlocked(it, snapshot, tagLookup) }
            when {
                kept.isEmpty() -> null
                kept.size == shelf.items.size -> shelf
                else -> shelf.copy(items = kept)
            }
        }
    }

    /**
     * Scores a track against [snapshot] in [0f..200f] (`-1f` if hard-blocked).
     * Neutral tracks hover around `45f`; preferred language/genre/artist matches push upward.
     */
    fun scoreSong(
        song: Song,
        snapshot: TasteSnapshot = TasteProfileRepository.snapshot.value,
        tagLookup: (Song) -> ResolvedTags = TagResolver::resolveImmediate,
    ): Float {
        if (isBlocked(song, snapshot, tagLookup)) return -1f
        val tags = tagLookup(song)
        var score = TasteConstants.DEFAULT_NEUTRAL_WEIGHT

        val langs = tags.languages()
        if (langs.isNotEmpty()) {
            val bestLang = langs.maxOf { snapshot.weightForTag(it) }
            score += (bestLang - TasteConstants.DEFAULT_NEUTRAL_WEIGHT) * 0.70f
        }
        val genres = tags.genres()
        if (genres.isNotEmpty()) {
            val bestGenre = genres.maxOf { snapshot.weightForTag(it) }
            score += (bestGenre - TasteConstants.DEFAULT_NEUTRAL_WEIGHT) * 0.65f
        }
        val themes = tags.themes()
        if (themes.isNotEmpty()) {
            val bestTheme = themes.maxOf { snapshot.weightForTag(it) }
            score += (bestTheme - TasteConstants.DEFAULT_NEUTRAL_WEIGHT) * 0.40f
        }

        val artistKey = song.artistId?.takeIf { it.isNotBlank() }?.let(TasteConstants::normalizeArtistKey)
            ?: TasteConstants.normalizeArtistKey(song.artist)
        if (artistKey.isNotEmpty()) {
            val artistWeight = snapshot.weightFor(TasteCategory.ARTIST, artistKey)
            score += (artistWeight - TasteConstants.DEFAULT_NEUTRAL_WEIGHT) * 0.75f
        }
        return score.coerceAtLeast(1f)
    }

    fun scoreItem(
        item: ShelfItem,
        snapshot: TasteSnapshot = TasteProfileRepository.snapshot.value,
        tagLookup: (ShelfItem) -> ResolvedTags = TagResolver::resolveImmediate,
    ): Float {
        if (isBlocked(item, snapshot, tagLookup)) return -1f
        val tags = tagLookup(item)
        var score = TasteConstants.DEFAULT_NEUTRAL_WEIGHT
        val langs = tags.languages()
        if (langs.isNotEmpty()) {
            score += (langs.maxOf { snapshot.weightForTag(it) } - TasteConstants.DEFAULT_NEUTRAL_WEIGHT) * 0.70f
        }
        val genres = tags.genres()
        if (genres.isNotEmpty()) {
            score += (genres.maxOf { snapshot.weightForTag(it) } - TasteConstants.DEFAULT_NEUTRAL_WEIGHT) * 0.65f
        }
        val themes = tags.themes()
        if (themes.isNotEmpty()) {
            score += (themes.maxOf { snapshot.weightForTag(it) } - TasteConstants.DEFAULT_NEUTRAL_WEIGHT) * 0.40f
        }
        val artistKey = item.artistIds.firstOrNull()?.let(TasteConstants::normalizeArtistKey)
            ?: TasteConstants.normalizeArtistKey(InnertubeParser.artistFromSubtitle(item.subtitle))
        if (artistKey.isNotEmpty()) {
            val artistWeight = snapshot.weightFor(TasteCategory.ARTIST, artistKey)
            score += (artistWeight - TasteConstants.DEFAULT_NEUTRAL_WEIGHT) * 0.75f
        }
        return score.coerceAtLeast(1f)
    }
}
