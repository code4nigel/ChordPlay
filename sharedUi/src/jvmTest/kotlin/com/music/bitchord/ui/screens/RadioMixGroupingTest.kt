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
package com.music.bitchord.ui.screens

import com.music.bitchord.data.model.KeyedMix
import com.music.bitchord.data.model.ShelfItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RadioMixGroupingTest {

    private fun mix(id: String, localizedTitle: String, englishTitle: String = localizedTitle): KeyedMix =
        KeyedMix(
            item = ShelfItem(
                title = localizedTitle,
                subtitle = "Playlist",
                thumbnailUrl = null,
                videoId = null,
                browseId = id,
            ),
            englishTitle = englishTitle,
        )

    @Test
    fun groupMixesSplitsQuickMixesYourMixesAndSortedMoodShelves() {
        val input = listOf(
            mix("VL_chill2", "Chill-Mix 2", "Chill Mix 2"),
            mix("VL_discover", "Discover-Mix", "Discover Mix"),
            mix("VL_my1", "My Mix 1", "My Mix 1"),
            mix("VL_super", "Mein Supermix", "My Supermix"),
            mix("VL_chill_super", "Chill Supermix", "Chill Supermix"),
            mix("VL_custom", "Late Night R&B", "Late Night R&B"),
            mix("VL_chill1", "Chill-Mix 1", "Chill Mix 1"),
            mix("VL_liked", "Liked Music", "Liked Music"),
        )

        val grouped = groupMixes(input)

        // Quick Mixes ordered strictly by SIGNATURE_MIX_TITLES rank: My Supermix -> Discover Mix -> Liked Music
        assertEquals(
            listOf("VL_super", "VL_discover", "VL_liked"),
            grouped.quickMixes.map { it.browseId },
        )

        // Your Mixes captures My Mix * and personal curated titles
        assertEquals(
            listOf("VL_my1", "VL_custom"),
            grouped.yourMixes.map { it.browseId },
        )

        // Mood shelf groups Chill Supermix ahead of Chill Mix 1 and Chill Mix 2
        assertEquals(1, grouped.moodShelves.size)
        val (heading, chillItems) = grouped.moodShelves.single()
        assertTrue(heading.isNotBlank())
        assertEquals(
            listOf("VL_chill_super", "VL_chill1", "VL_chill2"),
            chillItems.map { it.browseId },
        )
    }
}
