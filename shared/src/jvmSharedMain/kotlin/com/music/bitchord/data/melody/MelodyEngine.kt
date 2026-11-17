package com.music.bitchord.data.melody

import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.model.LikeStatus
import com.music.bitchord.data.model.PlaybackSourceType
import com.music.bitchord.data.model.QueueTier
import com.music.bitchord.data.model.Song
import com.music.bitchord.playback.QueueBuilder
import com.music.bitchord.playback.QueueTimeline.asQueueEntry
import kotlin.math.min
import kotlin.random.Random

/**
 * Builds ChordPlay Melody endless personal mix queues from local signals + YouTube radio.
 *
 * Signals (all optional, best-effort):
 * - favorites: liked library tracks (highest weight)
 * - history: recent plays, newest-first (recency weight)
 * - quickPicks: fresh recommendations / Home picks (discovery pool)
 * - localTracks: saved downloads / device music tracks
 * - starterTracks: starter picks when a brand-new user chooses moods or artists
 * - moodPool: Radio tab mood playlists for non-default moods
 * - listens: tracks played recently elsewhere
 * - likeStatuses: session ratings incl. DISLIKE (hard filter)
 * - bannedIds: user-banned tracks, never return
 * - skipCounts: videoId -> skips; >=2 demotes, >=3 excludes
 * - genreOf: artist name -> genres (ArtistFacts on Android), for mood/tuner
 */
object MelodyEngine {

    const val INITIAL_TRACKS = 50
    const val TOPUP_TRACKS = 10
    private const val SKIP_EXCLUDE_AFTER = 3
    private const val SKIP_DEMOTE_AFTER = 2

    /** Bumped when seed/expansion/repeat rules change so stale snapshots rebuild cleanly. */
    const val ALGORITHM_VERSION = 2

    /** Most recent plays kept out of expansion so the mix avoids immediate replays. */
    private const val RECENTLY_PLAYED_LIMIT = 50

    data class SeedPool(
        val favorites: List<Song> = emptyList(),
        val history: List<Song> = emptyList(),
        val quickPicks: List<Song> = emptyList(),
        val moodPool: List<Song> = emptyList(),
        val listens: List<Song> = emptyList(),
        val localTracks: List<Song> = emptyList(),
        val starterTracks: List<Song> = emptyList(),
    )

    /** Score candidates for Melody ordering. Higher = earlier in the mix. */
    fun score(
        candidate: Song,
        favIds: Set<String>,
        historyRank: Map<String, Int>,
        skipCounts: Map<String, Int>,
        tuner: MelodyTuner,
        moodHints: List<String>,
        genreOf: (String) -> List<String> = { emptyList() },
    ): Double {
        var s = 1.0
        if (candidate.videoId in favIds) s += 4.0
        historyRank[candidate.videoId]?.let { rank ->
            val memory = tuner.memory.coerceIn(0f, 1f)
            s += (1.0 - min(rank, 20) / 22.0) * (0.4 + 2.4 * memory)
        }
        val skips = skipCounts[candidate.videoId] ?: 0
        if (skips >= SKIP_DEMOTE_AFTER) s -= 2.0
        val isFresh = candidate.videoId !in favIds && candidate.videoId !in historyRank
        if (isFresh) s += tuner.discovery.coerceIn(0f, 1f) * 2.0 else s += 0.5
        if (moodHints.isNotEmpty()) {
            val genres = genreOf(candidate.artist).map { it.lowercase() }
            if (genres.any { g -> moodHints.any { h -> g.contains(h) } }) s += 1.5
            val hay = "${candidate.title} ${candidate.artist}".lowercase()
            if (moodHints.any { hay.contains(it) }) s += 0.5
        }
        return s
    }

    fun isExcluded(
        candidate: Song,
        bannedIds: Set<String>,
        likeStatuses: Map<String, LikeStatus>,
        skipCounts: Map<String, Int>,
        tuner: MelodyTuner,
        genreOf: (String) -> List<String> = { emptyList() },
    ): Boolean {
        if (candidate.videoId in bannedIds) return true
        if (likeStatuses[candidate.videoId] == LikeStatus.DISLIKE) return true
        if ((skipCounts[candidate.videoId] ?: 0) >= SKIP_EXCLUDE_AFTER) return true
        if (tuner.excludedGenres.isNotEmpty()) {
            val genres = genreOf(candidate.artist).map { it.lowercase() }
            if (genres.any { it in tuner.excludedGenres }) return true
        }
        return false
    }

    /** Order + dedupe a local pool into Melody opening order. */
    fun orderLocalPool(
        pool: List<Song>,
        favIds: Set<String>,
        history: List<Song>,
        skipCounts: Map<String, Int>,
        tuner: MelodyTuner,
        mood: MelodyMood,
        genreOf: (String) -> List<String> = { emptyList() },
        limit: Int = INITIAL_TRACKS,
        random: Random = Random.Default,
    ): List<Song> {
        val historyRank = history.mapIndexed { i, s -> s.videoId to i }.toMap()
        val hints = mood.genreHints()
        val discovery = tuner.discovery.coerceIn(0f, 1f)
        val distinct = pool.distinctBy { it.videoId }
        val gated = when {
            discovery <= 0.15f ->
                distinct.filter { it.videoId in favIds || it.videoId in historyRank }.ifEmpty { distinct }
            discovery >= 0.85f -> {
                val fresh = distinct.filter { it.videoId !in favIds && it.videoId !in historyRank }
                fresh.ifEmpty { distinct }
            }
            else -> distinct
        }
        return gated
            .filterNot { isExcluded(it, emptySet(), emptyMap(), skipCounts, tuner, genreOf) }
            .map { it to score(it, favIds, historyRank, skipCounts, tuner, hints, genreOf) }
            .sortedByDescending { it.second }
            .map { it.first }
            .let { spreadArtists(it, random) }
            .take(limit)
    }

    /**
     * Full initial Melody queue: local ordered pool expanded with YouTube radio.
     * Falls back cleanly to Quick Picks, starter picks, or local tracks when
     * the listener is new or offline.
     */
    suspend fun buildInitialQueue(
        seeds: SeedPool,
        likeStatuses: Map<String, LikeStatus> = emptyMap(),
        bannedIds: Set<String> = emptySet(),
        skipCounts: Map<String, Int> = emptyMap(),
        config: MelodyConfig = MelodyConfig(),
        limit: Int = INITIAL_TRACKS,
        genreOf: (String) -> List<String> = { emptyList() },
        relatedTracks: (suspend (Song) -> List<Song>)? = null,
    ): List<Song> {
        val favIds = seeds.favorites.map { it.videoId }.toSet()
        val historyIds = seeds.history.take(RECENTLY_PLAYED_LIMIT)
            .map { it.videoId }
            .toSet()
        val excluded: (Song) -> Boolean = {
            isExcluded(it, bannedIds, likeStatuses, skipCounts, config.tuner, genreOf)
        }
        val pool = (
            seeds.moodPool +
                seeds.starterTracks +
                seeds.favorites +
                seeds.quickPicks +
                seeds.listens +
                seeds.localTracks
            )
            .distinctBy { it.videoId }
            .filterNot(excluded)
        val localPool = pool.ifEmpty {
            seeds.history.distinctBy { it.videoId }.filterNot(excluded)
        }
        val ordered = orderLocalPool(
            pool = localPool.ifEmpty { seeds.quickPicks },
            favIds = favIds,
            history = seeds.history,
            skipCounts = skipCounts,
            tuner = config.tuner,
            mood = config.mood,
            genreOf = genreOf,
            limit = min(limit, 14),
        )
        if (ordered.isEmpty()) {
            val fallback = (seeds.quickPicks + seeds.starterTracks + seeds.localTracks)
                .distinctBy { it.videoId }
                .filterNot(excluded)
                .take(limit)
            if (fallback.isEmpty() && seeds.favorites.isEmpty() && seeds.history.isEmpty()) {
                return tagMelody(emptyList())
            }
            if (fallback.isNotEmpty()) return tagMelody(fallback)
        }
        val expanded = ordered.toMutableList()
        val radioSeeds = pickRadioSeeds(
            ordered = ordered,
            favorites = seeds.favorites,
            tuner = config.tuner,
            moodPool = seeds.moodPool,
            listens = seeds.listens,
            starterTracks = seeds.starterTracks,
        )
        for (seed in radioSeeds) {
            if (expanded.size >= limit) break
            // Local device files without YouTube IDs cannot seed remote radio directly.
            if (seed.videoId.startsWith("local:") || seed.videoId.isBlank()) continue
            val expandedTracks = if (relatedTracks != null) {
                runCatching { relatedTracks(seed) }.getOrDefault(emptyList())
            } else {
                runCatching { YtMusicRepository.radio(seed.videoId).getOrNull() }
                    .getOrNull().orEmpty()
            }
            val related = expandedTracks
                .filterNot { isExcluded(it, bannedIds, likeStatuses, skipCounts, config.tuner, genreOf) }
                .filterNot { it.videoId in historyIds }
                .filterNot { config.tuner.discovery >= 0.85f && it.videoId in favIds }
            val extra = QueueBuilder.extend(expanded, related, min(8, limit - expanded.size))
            val historyRank = seeds.history.mapIndexed { i, s -> s.videoId to i }.toMap()
            val ranked = extra.sortedByDescending {
                score(it, favIds, historyRank, skipCounts, config.tuner, config.mood.genreHints(), genreOf)
            }
            expanded += ranked.filterNot { c -> expanded.any { QueueBuilder.isSameRecording(it, c) } }
        }
        val distinct = expanded.distinctBy { it.videoId }
        val freshOnly = if (config.tuner.discovery >= 0.85f) {
            distinct.filterNot { it.videoId in favIds }.ifEmpty { distinct }
        } else {
            distinct
        }
        val withoutPlayed = freshOnly.filterNot { it.videoId in historyIds }
        val final = (withoutPlayed.ifEmpty { freshOnly.ifEmpty { distinct } }).take(limit)
        if (final.isEmpty()) return tagMelody(ordered.take(limit))
        return tagMelody(final)
    }

    /** Rotating seed for endless top-ups: least-recently-seeded favorite first. */
    fun nextSeed(
        queue: List<Song>,
        favorites: List<Song>,
        seedCursor: Int,
    ): Song? {
        if (favorites.isNotEmpty()) return favorites[seedCursor % favorites.size]
        return queue.lastOrNull()
    }

    private fun pickRadioSeeds(
        ordered: List<Song>,
        favorites: List<Song>,
        tuner: MelodyTuner,
        moodPool: List<Song>,
        listens: List<Song>,
        starterTracks: List<Song>,
    ): List<Song> {
        val liked = favorites.map { it.videoId }.toSet()
        val seeds = mutableListOf<Song>()
        seeds += moodPool.take(2)
        seeds += starterTracks.take(2)
        if (tuner.similarLiked) seeds += favorites.take(2) else seeds += listens.take(2)
        val candidates = if (tuner.similarLiked) ordered else ordered.filter { it.videoId !in liked }
        seeds += if (tuner.discovery > 0.6f) candidates.take(4) else candidates.take(2)
        return seeds.distinctBy { it.videoId }.take(4)
    }

    private fun spreadArtists(tracks: List<Song>, random: Random): List<Song> {
        if (tracks.size < 4) return tracks
        val out = mutableListOf<Song>()
        val buckets = tracks.groupBy { it.artist.lowercase() }.mapValues { it.value.toMutableList() }
        val keys = buckets.keys.shuffled(random).toMutableList()
        var ki = 0
        var guard = tracks.size * 3
        while (out.size < tracks.size && guard-- > 0) {
            if (keys.isEmpty()) break
            val k = keys[ki % keys.size]
            val bucket = buckets[k]
            if (bucket == null) {
                ki++
                continue
            }
            if (bucket.isNotEmpty()) out += bucket.removeAt(0)
            if (bucket.isEmpty()) keys.remove(k) else ki++
        }
        buckets.values.forEach { out += it }
        return out.distinctBy { it.videoId }
    }

    /** Tag tracks as ChordPlay Melody origin so Now Playing + queue name the mix. */
    fun tagMelody(tracks: List<Song>): List<Song> = tracks.map {
        it.copy(
            radioName = MelodyRules.MELODY_RADIO_NAME,
            playbackSource = MelodyRules.MELODY_RADIO_NAME,
            playbackSourceType = PlaybackSourceType.EXPLORE,
            playbackSourceId = MelodyRules.MELODY_BROWSE_ID,
        ).asQueueEntry(QueueTier.CONTEXT)
    }
}
