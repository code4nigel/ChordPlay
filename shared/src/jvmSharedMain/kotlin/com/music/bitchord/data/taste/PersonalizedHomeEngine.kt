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
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.MoodGenre
import com.music.bitchord.data.model.SearchFilter
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.Song
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Preference-first Personalized Home Feed builder (Plan Section 5).
 *
 * 1. Returns cached shelves immediately from `feed_cache` (stale-while-revalidate).
 * 2. Fetches the user's top 2-3 weighted (Language, Genre/Theme) buckets + 1 For You Mix
 *    shelf in parallel via YTM `FEmusic_moods_and_genres` and targeted queries.
 * 3. Applies [TasteFilter] in RAM (< 1 ms) and enforces at most 1 extra continuation/supplement
 *    fetch per shelf when filtered below [MIN_SHELF_ITEMS] (no infinite loops).
 */
object PersonalizedHomeEngine {

    const val MIN_SHELF_ITEMS = 4
    private const val MAX_BUCKET_SHELVES = 3

    data class ShelfBucketSpec(
        val title: String,
        val query: String,
        val language: TasteTag?,
        val genre: TasteTag?,
        val theme: TasteTag?,
    ) {
        val langMask: Int get() = TagMask.ofTag(language)
        val genreMask: Int get() = TagMask.ofTag(genre)
        val themeMask: Int get() = TagMask.ofTag(theme)
    }

    /** Immediate startup shelves from `feed_cache`, filtered through the current snapshot. */
    fun cachedStartupShelves(
        snapshot: TasteSnapshot = TasteProfileRepository.snapshot.value,
        allowStale: Boolean = true,
    ): List<HomeShelf> {
        val cached = TasteProfileRepository.readFeedCache(allowStale = allowStale)
        if (cached.isEmpty()) return emptyList()
        return TasteFilter.filterShelves(cached, snapshot)
    }

    /** Builds up to 3 preference-driven bucket specs + 1 discovery mix spec. */
    fun planBucketSpecs(snapshot: TasteSnapshot): List<ShelfBucketSpec> {
        val topLangs = snapshot.topPreferredOrWeighted(TasteCategory.LANGUAGE, minWeight = 55f, limit = 3)
        val topGenres = snapshot.topPreferredOrWeighted(TasteCategory.GENRE, minWeight = 55f, limit = 3)
        val topThemes = snapshot.topPreferredOrWeighted(TasteCategory.THEME, minWeight = 60f, limit = 2)

        if (topLangs.isEmpty() && topGenres.isEmpty() && topThemes.isEmpty()) {
            return emptyList()
        }

        val specs = mutableListOf<ShelfBucketSpec>()
        val usedTitles = HashSet<String>()

        fun addSpec(lang: TasteTag?, genre: TasteTag?, theme: TasteTag?, prefix: String = "") {
            if (specs.size >= MAX_BUCKET_SHELVES) return
            val labelParts = listOfNotNull(
                lang?.displayName,
                genre?.displayName,
                theme?.displayName,
            )
            if (labelParts.isEmpty()) return
            val title = (if (prefix.isNotBlank()) "$prefix " else "") + labelParts.joinToString(" • ")
            if (!usedTitles.add(title.lowercase())) return
            specs += ShelfBucketSpec(
                title = title,
                query = TasteTaxonomy.buildQuery(lang, genre, theme),
                language = lang,
                genre = genre,
                theme = theme,
            )
        }

        // Primary pair: #1 Language + #1 Genre
        if (topLangs.isNotEmpty() && topGenres.isNotEmpty()) {
            addSpec(topLangs[0], topGenres[0], null)
            if (topGenres.size > 1) {
                addSpec(topLangs[0], topGenres[1], null)
            } else if (topLangs.size > 1) {
                addSpec(topLangs[1], topGenres[0], null)
            }
            if (specs.size < MAX_BUCKET_SHELVES && topThemes.isNotEmpty()) {
                addSpec(topLangs[0], null, topThemes[0])
            } else if (specs.size < MAX_BUCKET_SHELVES && topLangs.size > 1) {
                addSpec(topLangs[1], topGenres.getOrNull(1) ?: topGenres[0], null)
            }
        } else if (topLangs.isNotEmpty()) {
            topLangs.forEach { lang -> addSpec(lang, null, topThemes.firstOrNull(), prefix = "Top") }
        } else if (topGenres.isNotEmpty()) {
            topGenres.forEach { genre -> addSpec(null, genre, topThemes.firstOrNull()) }
        } else {
            topThemes.forEach { theme -> addSpec(null, null, theme) }
        }

        return specs.take(MAX_BUCKET_SHELVES)
    }

    /**
     * Fetches preference-first shelves in parallel. Each shelf runs at most 1 continuation
     * or fallback search if filtering drops it below [MIN_SHELF_ITEMS].
     */
    suspend fun fetchPreferredShelves(
        snapshot: TasteSnapshot = TasteProfileRepository.snapshot.value,
        moodCategories: List<MoodGenre> = emptyList(),
    ): List<HomeShelf> = coroutineScope {
        val specs = planBucketSpecs(snapshot)
        if (specs.isEmpty()) return@coroutineScope emptyList()

        val bucketJobs = specs.map { spec ->
            async { fetchSingleBucketShelf(spec, snapshot, moodCategories) }
        }
        val mixJob = async { fetchForYouMixShelf(specs.firstOrNull(), snapshot) }

        val result = (bucketJobs.awaitAll() + listOf(mixJob.await()))
            .filterNotNull()
            .filter { it.items.isNotEmpty() }

        if (result.isNotEmpty()) {
            TasteProfileRepository.saveFeedCache(result)
        }
        result
    }

    private suspend fun fetchSingleBucketShelf(
        spec: ShelfBucketSpec,
        snapshot: TasteSnapshot,
        moodCategories: List<MoodGenre>,
    ): HomeShelf? {
        val collected = LinkedHashMap<String, ShelfItem>()

        // Try matching YTM Moods & Genres category first if supplied
        val matchingCategory = findMatchingCategory(spec, moodCategories)
        if (matchingCategory != null) {
            val catShelves = YtMusicRepository.moodGenreShelves(
                matchingCategory.browseId,
                matchingCategory.params,
            ).getOrNull().orEmpty()
            for (shelf in catShelves) {
                TagResolver.recordSourceTags(shelf.items, spec.langMask, spec.genreMask, spec.themeMask)
                for (item in TasteFilter.filterItems(shelf.items, snapshot)) {
                    val key = item.videoId ?: item.browseId ?: continue
                    collected.putIfAbsent(key, item)
                }
            }
        }

        // Targeted YTM song search (e.g. "Hindi Rap")
        var continuationToken: String? = null
        if (collected.size < MIN_SHELF_ITEMS) {
            val page = YtMusicRepository.searchPage(spec.query, SearchFilter.SONGS).getOrNull()
            continuationToken = page?.continuation
            val songs = page?.rows.orEmpty().mapNotNull { row ->
                (row as? SearchResult.TopTrack)?.song ?: (row as? SearchResult.Track)?.song
            }
            TagResolver.recordSourceTagsForSongs(songs, spec.langMask, spec.genreMask, spec.themeMask)
            for (song in TasteFilter.filterSongs(songs, snapshot)) {
                collected.putIfAbsent(song.videoId, song.toShelfItem())
            }
        }

        // At most 1 continuation fetch if still below MIN_SHELF_ITEMS
        if (collected.size < MIN_SHELF_ITEMS && continuationToken != null) {
            val page2 = YtMusicRepository.searchContinuation(continuationToken, SearchFilter.SONGS).getOrNull()
            val songs2 = page2?.rows.orEmpty().mapNotNull { row ->
                (row as? SearchResult.TopTrack)?.song ?: (row as? SearchResult.Track)?.song
            }
            TagResolver.recordSourceTagsForSongs(songs2, spec.langMask, spec.genreMask, spec.themeMask)
            for (song in TasteFilter.filterSongs(songs2, snapshot)) {
                collected.putIfAbsent(song.videoId, song.toShelfItem())
            }
        }

        val items = collected.values.take(15)
        if (items.size < MIN_SHELF_ITEMS && items.isEmpty()) return null
        return HomeShelf(
            title = spec.title,
            subtitle = "Tailored from Audio Choices",
            items = items,
        )
    }

    private suspend fun fetchForYouMixShelf(
        primarySpec: ShelfBucketSpec?,
        snapshot: TasteSnapshot,
    ): HomeShelf? {
        val mixes = YtMusicRepository.mixedForYou().getOrNull().orEmpty()
        val filteredMixes = TasteFilter.filterItems(mixes, snapshot)
        if (filteredMixes.size >= MIN_SHELF_ITEMS) {
            return HomeShelf(
                title = "For You Mix",
                subtitle = "Guided by your taste",
                items = filteredMixes.take(12),
            )
        }
        val query = primarySpec?.let { "${it.query} mix" } ?: return null
        val fallbackSongs = YtMusicRepository.searchPage(query, SearchFilter.SONGS)
            .getOrNull()
            ?.rows
            .orEmpty()
            .mapNotNull { (it as? SearchResult.TopTrack)?.song ?: (it as? SearchResult.Track)?.song }
        TagResolver.recordSourceTagsForSongs(
            fallbackSongs,
            primarySpec.langMask,
            primarySpec.genreMask,
            primarySpec.themeMask,
        )
        val combined = (filteredMixes + TasteFilter.filterSongs(fallbackSongs, snapshot).map { it.toShelfItem() })
            .distinctBy { it.videoId ?: it.browseId ?: it.title }
            .take(12)
        if (combined.isEmpty()) return null
        return HomeShelf(
            title = "For You Mix",
            subtitle = "Guided by your taste",
            items = combined,
        )
    }

    private fun findMatchingCategory(spec: ShelfBucketSpec, categories: List<MoodGenre>): MoodGenre? {
        if (categories.isEmpty()) return null
        val hints = buildList {
            spec.genre?.let { addAll(it.ytmCategoryHints); add(it.displayName.lowercase()) }
            spec.language?.let { addAll(it.ytmCategoryHints); add(it.displayName.lowercase()) }
            spec.theme?.let { addAll(it.ytmCategoryHints); add(it.displayName.lowercase()) }
        }
        return categories.firstOrNull { cat ->
            val title = cat.title.lowercase()
            hints.any { hint -> title == hint || title.contains(hint) }
        }
    }

    private fun Song.toShelfItem(): ShelfItem = ShelfItem(
        title = title,
        subtitle = artist,
        thumbnailUrl = thumbnailUrl,
        videoId = videoId,
        browseId = null,
        isVideo = isVideo,
        artistIds = (listOfNotNull(artistId) + artists.mapNotNull { it.browseId }).distinct(),
    )
}
