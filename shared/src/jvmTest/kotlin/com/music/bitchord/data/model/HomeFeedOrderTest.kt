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
package com.music.bitchord.data.model

import kotlin.test.Test
import kotlin.test.assertEquals

class HomeFeedOrderTest {
    private fun shelf(title: String) = HomeShelf(title = title, subtitle = "", items = emptyList())

    @Test
    fun defaultOrderPlacesSpeedDialFirstThenFeedThenMelodyAndReplay() {
        val ids = orderHomeShelfIds(
            rawShelfTitles = listOf("Quick picks", "Forgotten favorites", "Trending"),
            savedOrder = emptyList(),
        )
        assertEquals(
            listOf(
                HomeShelfSlot.SPEED_DIAL,
                "Quick picks",
                "Forgotten favorites",
                "Trending",
                HomeShelfSlot.MELODY,
                HomeShelfSlot.REPLAY,
            ),
            ids,
        )
    }

    @Test
    fun savedOrderMovesBottomShelfOrSyntheticCardToTopAndAppendsNewShelves() {
        val ids = orderHomeShelfIds(
            rawShelfTitles = listOf("Quick picks", "Mixed for you", "New continuation"),
            savedOrder = listOf(HomeShelfSlot.MELODY, "Mixed for you", HomeShelfSlot.SPEED_DIAL, "Quick picks"),
        )
        assertEquals(
            listOf(
                HomeShelfSlot.MELODY,
                "Mixed for you",
                HomeShelfSlot.SPEED_DIAL,
                "Quick picks",
                "New continuation",
                HomeShelfSlot.REPLAY,
            ),
            ids,
        )
    }

    @Test
    fun hiddenShelvesRemainInOrderIdsButAreExcludedFromRenderedEntries() {
        val shelves = listOf(shelf("Quick picks"), shelf("Music videos"), shelf("New releases"))
        val savedOrder = listOf(
            HomeShelfSlot.SPEED_DIAL,
            "Music videos",
            HomeShelfSlot.REPLAY,
            "Quick picks",
            HomeShelfSlot.MELODY,
            "New releases",
        )
        val hidden = setOf("Quick picks")

        val settingsIds = orderHomeShelfIds(
            rawShelfTitles = shelves.map { it.title },
            savedOrder = savedOrder,
        )
        assertEquals(savedOrder, settingsIds)

        val entries = buildHomeFeedEntries(
            shelves = shelves,
            savedOrder = savedOrder,
            hiddenIds = hidden,
            showSpeedDial = true,
            showMelody = false,
            showReplay = true,
        )
        assertEquals(
            listOf(
                HomeFeedEntry.SpeedDial,
                HomeFeedEntry.Shelf(shelves[1]),
                HomeFeedEntry.Replay,
                HomeFeedEntry.Shelf(shelves[2]),
            ),
            entries,
        )
    }

    @Test
    fun orderRawHomeShelvesKeepsTieOrderAndRespectsSavedRanks() {
        val a = shelf("Quick picks")
        val b = shelf("Music videos")
        val c = shelf("Charts")
        val ordered = orderRawHomeShelves(
            shelves = listOf(a, b, c),
            savedOrder = listOf(HomeShelfSlot.SPEED_DIAL, "Charts", "Quick picks"),
        )
        assertEquals(listOf("Charts", "Quick picks", "Music videos"), ordered.map { it.title })
    }
}
