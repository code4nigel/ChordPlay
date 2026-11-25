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
package com.music.bitchord.data.settings

import com.music.bitchord.data.model.ShelfItem
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class SpeedDialConfigTest {

    @Test
    fun computeDialSlotsSinglePageWithLuckyPlacesLuckyAtBottomRight() {
        val slots = computeDialSlots(
            itemCount = 12,
            pinnedCount = 2,
            perPage = 9,
            includeLucky = true,
        )
        assertEquals(9, slots.size)
        assertContentEquals(intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, SPEED_DIAL_SLOT_LUCKY), slots)
    }

    @Test
    fun computeDialSlotsPadsShortSinglePageWithBlanksAndPreservesLucky() {
        val slots = computeDialSlots(
            itemCount = 4,
            pinnedCount = 2,
            perPage = 9,
            includeLucky = true,
        )
        assertEquals(9, slots.size)
        assertContentEquals(
            intArrayOf(
                0, 1, 2, 3,
                SPEED_DIAL_SLOT_BLANK, SPEED_DIAL_SLOT_BLANK,
                SPEED_DIAL_SLOT_BLANK, SPEED_DIAL_SLOT_BLANK,
                SPEED_DIAL_SLOT_LUCKY,
            ),
            slots,
        )
    }

    @Test
    fun computeDialSlotsMultiPagePreservesAllPinsAndFullPages() {
        val slots = computeDialSlots(
            itemCount = 12,
            pinnedCount = 10,
            perPage = 9,
            includeLucky = true,
        )
        assertEquals(18, slots.size)
        assertEquals(SPEED_DIAL_SLOT_LUCKY, slots[8])
        assertEquals(9, slots[10])
        assertEquals(11, slots[12])
        assertEquals(SPEED_DIAL_SLOT_BLANK, slots[13])
    }

    @Test
    fun speedDialSourceEffectiveAndChoicesRespectSpotifyConnection() {
        assertEquals(SpeedDialSource.SMART_BLEND, SpeedDialSource.SPOTIFY.effective(spotifyConnected = false))
        assertEquals(SpeedDialSource.SPOTIFY, SpeedDialSource.SPOTIFY.effective(spotifyConnected = true))

        val withoutSpotify = SpeedDialSource.choices(spotifyConnected = false)
        assertEquals(listOf(SpeedDialSource.SMART_BLEND, SpeedDialSource.YOUTUBE_MUSIC, SpeedDialSource.HISTORY), withoutSpotify)

        val withSpotify = SpeedDialSource.choices(spotifyConnected = true)
        assertEquals(SpeedDialSource.entries, withSpotify)
    }

    @Test
    fun speedDialStableIdNormalizesVlPrefix() {
        val itemWithVl = ShelfItem("Mix", "YouTube", null, videoId = null, browseId = "VLRDTMAK123")
        val itemWithoutVl = ShelfItem("Mix", "YouTube", null, videoId = null, browseId = "RDTMAK123")
        assertEquals("RDTMAK123", itemWithVl.speedDialStableId())
        assertEquals(itemWithoutVl.speedDialStableId(), itemWithVl.speedDialStableId())

        val songItem = ShelfItem("Track", "Artist", null, videoId = "vid_99", browseId = "VL_ignored")
        assertEquals("vid_99", songItem.speedDialStableId())
    }
}
