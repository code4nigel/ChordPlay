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

/**
 * Endless personalized mix for ChordPlay ("ChordPlay Melody"), combining
 * listening history, favorites, local library tracks, starter picks, skips,
 * and bans with fresh YouTube Music recommendations.
 *
 * Pure-Kotlin models so Android, Desktop, and JVM unit tests share one set of rules.
 */
object MelodyRules {
    const val MELODY_RADIO_NAME = "ChordPlay Melody"
    const val MELODY_SHORT_NAME = "Melody"

    /** Synthetic browse id used by the Home / Radio Melody entry card click path. */
    const val MELODY_BROWSE_ID = "bitchord:melody"

    /** Synthetic shelf title for Home / Radio entry points. */
    const val MELODY_SHELF_TITLE = "ChordPlay Melody — Your personal mix"

    /**
     * ChordPlay Melody has no hard 16-favorite lock wall: any personal or
     * catalog signal unlocks immediately, and brand-new users can pick
     * starter moods/artists or seed directly from Quick Picks.
     */
    fun isUnlocked(
        favTrackCount: Int = 0,
        favArtistCount: Int = 0,
        historyCount: Int = 0,
        localTrackCount: Int = 0,
        hasHomePicks: Boolean = false,
        hasStarterPicks: Boolean = false,
        spotifyConnected: Boolean = false,
    ): Boolean =
        favTrackCount > 0 ||
            favArtistCount > 0 ||
            historyCount > 0 ||
            localTrackCount > 0 ||
            hasHomePicks ||
            hasStarterPicks ||
            spotifyConnected
}

/** Filter ChordPlay Melody by mood. [MELODY] = unfiltered endless mix. */
enum class MelodyMood(val label: String) {
    MELODY("Melody"),
    LOVE("Love"),
    WORKOUT("Workout"),
    CHILL("Chill"),
    SAD("Sad"),
    FOCUS("Focus"),
    PARTY("Party"),
    ;

    /**
     * Station names the Radio tab's collections answer to for this mood.
     */
    val stationNames: List<String>
        get() = when (this) {
            MELODY -> emptyList()
            LOVE -> listOf("Love", "Romance", "Feel good")
            WORKOUT -> listOf("Workout", "Energize", "Exercise")
            CHILL -> listOf("Chill", "Sleep", "Commute")
            SAD -> listOf("Sad", "Melancholy")
            FOCUS -> listOf("Focus", "Brain power", "Study")
            PARTY -> listOf("Party", "Dance", "Club")
        }
}

/**
 * Melody Tuner: steer discovery freshness, recency memory, similar-liked
 * seeding, and excluded genres with a Draft-until-Apply workflow.
 */
data class MelodyTuner(
    val discovery: Float = 0.35f,
    val memory: Float = 0.5f,
    val excludedGenres: Set<String> = emptySet(),
    val similarLiked: Boolean = true,
) {
    fun withDiscovery(value: Float) = copy(discovery = value.coerceIn(0f, 1f))
    fun withMemory(value: Float) = copy(memory = value.coerceIn(0f, 1f))
    fun withSimilarLiked(value: Boolean) = copy(similarLiked = value)
    fun toggleGenre(genre: String, enabled: Boolean): MelodyTuner =
        if (enabled) copy(excludedGenres = excludedGenres - genre.lowercase())
        else copy(excludedGenres = excludedGenres + genre.lowercase())

    fun isGenreEnabled(genre: String): Boolean = genre.lowercase() !in excludedGenres
}

data class MelodyConfig(
    val mood: MelodyMood = MelodyMood.MELODY,
    val tuner: MelodyTuner = MelodyTuner(),
)

data class MelodyStatus(
    val unlocked: Boolean,
    val favTrackCount: Int = 0,
    val favArtistCount: Int = 0,
    val historyCount: Int = 0,
    val localTrackCount: Int = 0,
    val hasStarterPicks: Boolean = false,
)

/** Curated genre list for the Tuner toggles (matches ArtistFacts vocabulary). */
val MELODY_TUNER_GENRES = listOf(
    "Pop", "Hip-Hop", "Rock", "Electronic", "Dance", "R&B",
    "Latin", "K-Pop", "Country", "Jazz", "Classical", "Metal",
    "Indie", "Folk", "Soul", "Reggae", "Punk", "Blues",
    "Soundtrack", "Ambient",
)

/** Starter artists offered to brand-new listeners with empty history/library. */
val MELODY_STARTER_ARTISTS = listOf(
    "The Weeknd",
    "Arijit Singh",
    "Taylor Swift",
    "Coldplay",
    "Kendrick Lamar",
    "Daft Punk",
    "Tame Impala",
    "Billie Eilish",
    "Fred again..",
    "Lana Del Rey",
)

/** Mood -> genre keywords used to boost matching seeds/candidates. */
fun MelodyMood.genreHints(): List<String> = when (this) {
    MelodyMood.MELODY -> emptyList()
    MelodyMood.LOVE -> listOf("r&b", "soul", "pop", "acoustic")
    MelodyMood.WORKOUT -> listOf("hip-hop", "electronic", "dance", "edm", "metal")
    MelodyMood.CHILL -> listOf("ambient", "chill", "acoustic", "jazz", "lo-fi")
    MelodyMood.SAD -> listOf("acoustic", "indie", "soul", "folk", "piano")
    MelodyMood.FOCUS -> listOf("classical", "ambient", "jazz", "lo-fi", "soundtrack")
    MelodyMood.PARTY -> listOf("dance", "pop", "latin", "hip-hop", "edm")
}
