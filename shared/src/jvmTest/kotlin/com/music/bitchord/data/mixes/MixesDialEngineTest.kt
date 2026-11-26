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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MixesDialEngineTest {

    @Test
    fun mixTypeDeduplicatesNumberedVariants() {
        assertEquals(MixesDialEngine.mixType("Chill Mix 01"), MixesDialEngine.mixType("Chill Mix 02"))
        assertEquals(MixesDialEngine.mixType("My Mix 1"), MixesDialEngine.mixType("My Mix 4"))
        assertEquals(MixesDialEngine.mixType("Your Top Songs 2023"), MixesDialEngine.mixType("Your Top Songs 2024"))
    }

    @Test
    fun isSleepTimeRespectsLateNightWindow() {
        assertFalse(MixesDialEngine.isSleepTime(21, 29))
        assertTrue(MixesDialEngine.isSleepTime(21, 30))
        assertTrue(MixesDialEngine.isSleepTime(2, 15))
        assertTrue(MixesDialEngine.isSleepTime(4, 0))
        assertFalse(MixesDialEngine.isSleepTime(4, 1))
        assertFalse(MixesDialEngine.isSleepTime(14, 0))
    }

    @Test
    fun pickMixesAlternatesSourcesDedupesTypesAndFiltersSleepAndHidden() {
        val yt = listOf(
            ShelfItem("Chill Mix 01", "YT", null, null, "VLRDTMAK_chill1"),
            ShelfItem("Deep Sleep Mix", "YT", null, null, "RDTMAK_sleep"),
            ShelfItem("Hidden Mix", "YT", null, null, "RDTMAK_hidden"),
            ShelfItem("Energy Mix", "YT", null, null, "RDTMAK_energy"),
        )
        val sp = listOf(
            ShelfItem("Chill Mix 02", "Spotify", null, null, "spotify:playlist:sp_chill2"),
            ShelfItem("Daily Mix 1", "Spotify", null, null, "spotify:playlist:sp_daily1"),
            ShelfItem("Focus Mix", "Spotify", null, null, "spotify:playlist:sp_focus"),
        )

        val daytime = MixesDialEngine.pickMixes(
            youTube = yt,
            spotify = sp,
            hiddenIds = setOf("RDTMAK_hidden"),
            sleepTime = false,
            limit = 10,
        )
        val titles = daytime.map { it.title }
        assertFalse("Deep Sleep Mix" in titles)
        assertFalse("Hidden Mix" in titles)
        assertFalse("Chill Mix 02" in titles) // deduped against Chill Mix 01
        assertEquals(listOf("Chill Mix 01", "Energy Mix", "Daily Mix 1", "Focus Mix"), titles)

        val night = MixesDialEngine.pickMixes(
            youTube = yt,
            spotify = emptyList(),
            hiddenIds = emptySet(),
            sleepTime = true,
            limit = 10,
        )
        assertTrue(night.any { it.title == "Deep Sleep Mix" })
    }

    @Test
    fun extractArtistDialItemsPrioritizesPinnedAndExcludesHidden() {
        val pinnedArtist = ShelfItem("Pinned Artist", "Artist", null, null, "UC_pinned")
        val shelfArtist1 = ShelfItem("Hidden Artist", "Artist", null, null, "UC_hidden")
        val shelfArtist2 = ShelfItem("Top Artist", "Artist", null, null, "UC_top")
        val nonArtist = ShelfItem("Some Song", "Track", null, "vid_1", null)

        val artists = MixesDialEngine.extractArtistDialItems(
            shelves = listOf(HomeShelf("Artists", listOf(shelfArtist1, shelfArtist2, nonArtist, pinnedArtist))),
            pinnedSpeedDial = listOf(pinnedArtist),
            hiddenIds = setOf("UC_hidden"),
        )
        assertEquals(listOf("UC_pinned", "UC_top"), artists.map { it.browseId })
    }
}
