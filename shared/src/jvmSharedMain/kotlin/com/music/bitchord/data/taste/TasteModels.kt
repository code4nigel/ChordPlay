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

import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.Song
import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
enum class DiscoveryLevel(val budget: Float, val label: String) {
    FOCUSED(0.05f, "Focused"),
    BALANCED(0.15f, "Balanced"),
    EXPLORATORY(0.30f, "Exploratory"),
}

@Serializable
enum class TagSource {
    USER,
    SOURCE,
    HEURISTIC,
    ITUNES,
    MUSICBRAINZ,
    LOCAL_CACHE,
}

@Serializable
data class TasteNode(
    val id: String,
    val category: TasteCategory,
    val canonicalId: Int? = null,
    val key: String,
    val displayName: String,
    val weight: Float = TasteConstants.DEFAULT_NEUTRAL_WEIGHT,
    val manualRank: Int? = null,
    val isBlocked: Boolean = false,
    val isPreferred: Boolean = false,
    val isAutoAdded: Boolean = false,
    val playCount: Int = 0,
    val lastHeardAt: Long = 0L,
)

@Serializable
data class BlockedSong(
    val videoId: String,
    val title: String,
    val artistName: String,
    val blockedAt: Long,
)

@Serializable
data class TunedSong(
    val videoId: String,
    val title: String,
    val artistName: String,
    val weight: Float = TasteConstants.DEFAULT_NEUTRAL_WEIGHT,
    val updatedAt: Long = 0L,
)

@Serializable
data class MediaTagEntry(
    val entityKey: String,
    val langMask: Int = 0,
    val genreMask: Int = 0,
    val themeMask: Int = 0,
    val source: TagSource = TagSource.HEURISTIC,
    val updatedAt: Long = 0L,
    val lastAccessedAt: Long = 0L,
) {
    val isEmpty: Boolean get() = langMask == 0 && genreMask == 0 && themeMask == 0

    fun resolved(): ResolvedTags = ResolvedTags(
        langMask = langMask,
        genreMask = genreMask,
        themeMask = themeMask,
        source = source,
    )
}

@Serializable
data class CachedShelfItem(
    val title: String,
    val subtitle: String = "",
    val thumbnailUrl: String? = null,
    val videoId: String? = null,
    val browseId: String? = null,
    val isVideo: Boolean = false,
    val artistIds: List<String> = emptyList(),
) {
    fun toShelfItem(): ShelfItem = ShelfItem(
        title = title,
        subtitle = subtitle,
        thumbnailUrl = thumbnailUrl,
        videoId = videoId,
        browseId = browseId,
        isVideo = isVideo,
        artistIds = artistIds,
    )

    companion object {
        fun from(item: ShelfItem): CachedShelfItem = CachedShelfItem(
            title = item.title,
            subtitle = item.subtitle,
            thumbnailUrl = item.thumbnailUrl,
            videoId = item.videoId,
            browseId = item.browseId,
            isVideo = item.isVideo,
            artistIds = item.artistIds,
        )
    }
}

@Serializable
data class FeedCacheEntry(
    val shelfKey: String,
    val title: String,
    val subtitle: String = "",
    val items: List<CachedShelfItem>,
    val fetchedAt: Long,
) {
    fun toHomeShelf(): HomeShelf = HomeShelf(
        title = title,
        items = items.map { it.toShelfItem() },
        subtitle = subtitle,
    )
}

data class ResolvedTags(
    val langMask: Int = 0,
    val genreMask: Int = 0,
    val themeMask: Int = 0,
    val source: TagSource = TagSource.HEURISTIC,
) {
    val isEmpty: Boolean get() = langMask == 0 && genreMask == 0 && themeMask == 0

    fun languages(): List<TasteTag> = TagMask.toTags(langMask, TasteTaxonomy.languages)
    fun genres(): List<TasteTag> = TagMask.toTags(genreMask, TasteTaxonomy.genres)
    fun themes(): List<TasteTag> = TagMask.toTags(themeMask, TasteTaxonomy.themes)

    operator fun plus(other: ResolvedTags): ResolvedTags = ResolvedTags(
        langMask = langMask or other.langMask,
        genreMask = genreMask or other.genreMask,
        themeMask = themeMask or other.themeMask,
        source = if (source == TagSource.SOURCE || source == TagSource.USER) source else other.source,
    )
}

data class SongTagDetails(
    val song: Song,
    val artistName: String,
    val artistKey: String,
    val artistId: String?,
    val language: TasteTag?,
    val genre: TasteTag?,
    val theme: TasteTag?,
    val isDetecting: Boolean,
)

/**
 * Immutable RAM snapshot of the listener's taste state, exposed via `StateFlow`.
 * All runtime filtering & scoring reads directly from this snapshot in < 0.1 ms.
 */
data class TasteSnapshot(
    val nodesById: Map<String, TasteNode> = emptyMap(),
    val blockedSongs: Map<String, BlockedSong> = emptyMap(),
    val tunedSongs: Map<String, TunedSong> = emptyMap(),
    val userMarkedAiArtists: Set<String> = emptySet(),
    val hideAiMusic: Boolean = false,
    val discoveryLevel: DiscoveryLevel = DiscoveryLevel.BALANCED,
    val onboardingDismissed: Boolean = false,
    val lastDecayAt: Long = 0L,
) {
    val blockedSongIds: Set<String> = blockedSongs.keys

    val blockedArtistKeys: Set<String> = nodesById.values
        .asSequence()
        .filter { it.category == TasteCategory.ARTIST && it.isBlocked }
        .mapTo(HashSet()) { it.key }

    val blockedLangMask: Int = nodesById.values
        .asSequence()
        .filter { it.category == TasteCategory.LANGUAGE && it.isBlocked }
        .mapNotNull { it.canonicalId?.let(TasteTaxonomy::byId)?.bitMask }
        .fold(0) { acc, bit -> acc or bit }

    val blockedGenreMask: Int = nodesById.values
        .asSequence()
        .filter { it.category == TasteCategory.GENRE && it.isBlocked }
        .mapNotNull { it.canonicalId?.let(TasteTaxonomy::byId)?.bitMask }
        .fold(0) { acc, bit -> acc or bit }

    val blockedThemeMask: Int = nodesById.values
        .asSequence()
        .filter { it.category == TasteCategory.THEME && it.isBlocked }
        .mapNotNull { it.canonicalId?.let(TasteTaxonomy::byId)?.bitMask }
        .fold(0) { acc, bit -> acc or bit }

    val hasPreferences: Boolean = tunedSongs.isNotEmpty() || nodesById.values.any {
        it.isPreferred || it.isBlocked || it.manualRank != null || it.weight != TasteConstants.DEFAULT_NEUTRAL_WEIGHT
    }

    fun activeNodes(category: TasteCategory): List<TasteNode> =
        nodesById.values
            .filter { it.category == category && !it.isBlocked }
            .sortedWith(
                compareByDescending<TasteNode> { it.weight }
                    .thenByDescending { it.isPreferred }
                    .thenBy { it.manualRank ?: Int.MAX_VALUE }
                    .thenBy { it.canonicalId ?: Int.MAX_VALUE }
                    .thenBy { it.displayName.lowercase(Locale.ROOT) },
            )

    fun blockedNodes(category: TasteCategory): List<TasteNode> =
        nodesById.values
            .filter { it.category == category && it.isBlocked }
            .sortedBy { it.displayName }

    fun activeWeightSum(category: TasteCategory): Float =
        nodesById.values
            .asSequence()
            .filter { it.category == category && !it.isBlocked }
            .sumOf { it.weight.toDouble() }
            .toFloat()
            .coerceAtLeast(1f)

    fun sharePercent(node: TasteNode): Float {
        if (node.isBlocked || node.weight <= 0f) return 0f
        return (node.weight / activeWeightSum(node.category)) * 100f
    }

    fun weightFor(category: TasteCategory, key: String): Float {
        val id = TasteTaxonomy.nodeId(category, key)
        return nodesById[id]?.weight ?: TasteConstants.DEFAULT_NEUTRAL_WEIGHT
    }

    fun weightForTag(tag: TasteTag): Float =
        nodesById[TasteTaxonomy.nodeId(tag)]?.weight ?: TasteConstants.DEFAULT_NEUTRAL_WEIGHT

    fun isTagBlocked(tag: TasteTag): Boolean = when (tag.category) {
        TasteCategory.LANGUAGE -> (blockedLangMask and tag.bitMask) != 0
        TasteCategory.GENRE -> (blockedGenreMask and tag.bitMask) != 0
        TasteCategory.THEME -> (blockedThemeMask and tag.bitMask) != 0
        TasteCategory.ARTIST -> tag.key in blockedArtistKeys
    }

    fun isSongBlocked(videoId: String?): Boolean =
        !videoId.isNullOrBlank() && videoId in blockedSongIds

    fun isArtistBlocked(artistName: String?, artistIds: List<String> = emptyList()): Boolean {
        for (id in artistIds) {
            val k = TasteConstants.normalizeArtistKey(id)
            if (k.isNotEmpty() && k in blockedArtistKeys) return true
        }
        val nameKey = artistName?.let(TasteConstants::normalizeArtistKey).orEmpty()
        return nameKey.isNotEmpty() && nameKey in blockedArtistKeys
    }

    fun isUserMarkedAi(artistName: String?, artistIds: List<String> = emptyList()): Boolean {
        if (userMarkedAiArtists.isEmpty()) return false
        for (id in artistIds) {
            val k = TasteConstants.normalizeArtistKey(id)
            if (k.isNotEmpty() && k in userMarkedAiArtists) return true
        }
        val nameKey = artistName?.let(TasteConstants::normalizeArtistKey).orEmpty()
        return nameKey.isNotEmpty() && nameKey in userMarkedAiArtists
    }

    fun topPreferredOrWeighted(category: TasteCategory, minWeight: Float = 55f, limit: Int = 3): List<TasteTag> =
        activeNodes(category)
            .filter { it.isPreferred || it.weight >= minWeight }
            .mapNotNull { it.canonicalId?.let(TasteTaxonomy::byId) }
            .take(limit)
}

@Serializable
data class TasteProfileDisk(
    val version: Int = 1,
    val nodes: List<TasteNode> = emptyList(),
    val blockedSongs: List<BlockedSong> = emptyList(),
    val tunedSongs: List<TunedSong> = emptyList(),
    val userMarkedAiArtists: List<String> = emptyList(),
    val hideAiMusic: Boolean = false,
    val discoveryLevel: DiscoveryLevel = DiscoveryLevel.BALANCED,
    val onboardingDismissed: Boolean = false,
    val lastDecayAt: Long = 0L,
)

@Serializable
data class MediaTagCacheDisk(
    val version: Int = 1,
    val entries: List<MediaTagEntry> = emptyList(),
)

@Serializable
data class FeedCacheDisk(
    val version: Int = 1,
    val shelves: List<FeedCacheEntry> = emptyList(),
)

object TasteConstants {
    const val MIN_WEIGHT = 0f
    const val FLOOR_LESS_WEIGHT = 5f
    const val MIN_MANUAL_WEIGHT = 15f
    const val DEFAULT_NEUTRAL_WEIGHT = 45f
    const val PREFERRED_INITIAL_WEIGHT = 85f
    const val MAX_MANUAL_WEIGHT = 95f
    const val MAX_WEIGHT = 100f

    const val BOOST_DELTA = 15f
    const val REDUCE_DELTA = 20f
    const val PASSIVE_NUDGE = 1.5f
    const val PASSIVE_MANUAL_NUDGE = 0.5f
    const val QUICK_SKIP_NUDGE = 1.0f

    const val WEEKLY_DECAY_KEEP = 0.97f
    const val DECAY_INTERVAL_MS = 7L * 24 * 60 * 60 * 1000
    const val TAG_STALE_MS = 30L * 24 * 60 * 60 * 1000
    const val FEED_CACHE_TTL_MS = 45L * 60 * 1000
    const val WAVE_COOLDOWN_MS = 6L * 60 * 60 * 1000

    const val MAX_PASSIVE_ARTIST_NODES = 500
    const val MAX_TUNED_SONGS = 200
    const val MAX_CACHED_ARTISTS = 2_000
    const val MAX_CACHED_SONGS = 3_000
    const val MAX_FEED_SHELVES = 10

    fun normalizeArtistKey(raw: String): String = raw
        .trim()
        .lowercase(Locale.ROOT)
        .replace(Regex("\\s+"), " ")

    fun artistCacheKey(artistIdOrName: String): String =
        "A:${normalizeArtistKey(artistIdOrName)}"

    fun songCacheKey(videoId: String): String =
        "S:${videoId.trim()}"
}
