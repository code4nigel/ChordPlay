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

import com.music.bitchord.data.model.LikeStatus
import com.music.bitchord.data.model.PlaybackSourceType
import com.music.bitchord.data.model.Song
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MelodyEngineTest {

    private fun song(id: String, artist: String = "Artist $id", title: String = "Track $id") =
        Song(videoId = id, title = title, artist = artist, thumbnailUrl = null)

    @Test
    fun `unlocks without a 16-favorite gate when any signal exists`() {
        assertFalse(MelodyRules.isUnlocked())
        assertTrue(MelodyRules.isUnlocked(favTrackCount = 1))
        assertTrue(MelodyRules.isUnlocked(historyCount = 1))
        assertTrue(MelodyRules.isUnlocked(localTrackCount = 1))
        assertTrue(MelodyRules.isUnlocked(hasHomePicks = true))
        assertTrue(MelodyRules.isUnlocked(hasStarterPicks = true))
        assertTrue(MelodyRules.isUnlocked(spotifyConnected = true))
    }

    @Test
    fun `isExcluded filters banned disliked 3-skips and excluded genres`() {
        val tuner = MelodyTuner().toggleGenre("Metal", enabled = false)
        val genreLookup: (String) -> List<String> = { artist ->
            if (artist == "Heavy Band") listOf("Metal") else listOf("Pop")
        }

        assertTrue(
            MelodyEngine.isExcluded(
                song("ban"),
                bannedIds = setOf("ban"),
                likeStatuses = emptyMap(),
                skipCounts = emptyMap(),
                tuner = tuner,
                genreOf = genreLookup,
            ),
        )
        assertTrue(
            MelodyEngine.isExcluded(
                song("bad"),
                bannedIds = emptySet(),
                likeStatuses = mapOf("bad" to LikeStatus.DISLIKE),
                skipCounts = emptyMap(),
                tuner = tuner,
                genreOf = genreLookup,
            ),
        )
        assertTrue(
            MelodyEngine.isExcluded(
                song("skip3"),
                bannedIds = emptySet(),
                likeStatuses = emptyMap(),
                skipCounts = mapOf("skip3" to 3),
                tuner = tuner,
                genreOf = genreLookup,
            ),
        )
        assertTrue(
            MelodyEngine.isExcluded(
                song("m1", artist = "Heavy Band"),
                bannedIds = emptySet(),
                likeStatuses = emptyMap(),
                skipCounts = emptyMap(),
                tuner = tuner,
                genreOf = genreLookup,
            ),
        )
        assertFalse(
            MelodyEngine.isExcluded(
                song("ok", artist = "Pop Star"),
                bannedIds = emptySet(),
                likeStatuses = mapOf("ok" to LikeStatus.LIKE),
                skipCounts = mapOf("ok" to 2),
                tuner = tuner,
                genreOf = genreLookup,
            ),
        )
    }

    @Test
    fun `buildInitialQueue expands starter and quick picks and tags Melody origin`() = runBlocking {
        val seeds = MelodyEngine.SeedPool(
            quickPicks = listOf(song("qp1"), song("qp2")),
            starterTracks = listOf(song("st1", artist = "Coldplay")),
        )
        val expanded = MelodyEngine.buildInitialQueue(
            seeds = seeds,
            limit = 10,
            relatedTracks = { seed ->
                listOf(
                    song("${seed.videoId}_r1", artist = "Fresh 1"),
                    song("${seed.videoId}_r2", artist = "Fresh 2"),
                )
            },
        )

        assertTrue(expanded.size >= 4)
        assertTrue(expanded.all { it.radioName == MelodyRules.MELODY_RADIO_NAME })
        assertTrue(expanded.all { it.playbackSource == MelodyRules.MELODY_RADIO_NAME })
        assertEquals(PlaybackSourceType.EXPLORE, expanded.first().playbackSourceType)
        assertEquals(MelodyRules.MELODY_BROWSE_ID, expanded.first().playbackSourceId)
    }

    @Test
    fun `high discovery omits favorites when fresh candidates exist`() = runBlocking {
        val fav = song("fav1", artist = "Fav Artist")
        val seeds = MelodyEngine.SeedPool(
            favorites = listOf(fav),
            quickPicks = listOf(song("fresh1", artist = "New Artist")),
        )
        val queue = MelodyEngine.buildInitialQueue(
            seeds = seeds,
            config = MelodyConfig(tuner = MelodyTuner(discovery = 0.95f)),
            limit = 10,
            relatedTracks = { listOf(song("fresh2", artist = "Discovery Artist")) },
        )

        assertTrue(queue.isNotEmpty())
        assertTrue(queue.none { it.videoId == "fav1" })
    }
}
