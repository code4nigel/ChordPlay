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

import com.music.bitchord.data.model.LiveStationCategory
import com.music.bitchord.data.model.ShelfItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsPinnedSpeedDialTest {

    @Test
    fun encodeAndDecodePinnedSpeedDialPreservesLiveVideoMetadata() {
        val liveStation = ShelfItem(
            title = "lofi hip hop radio - beats to relax/study to",
            subtitle = "Lofi Girl • 18K watching",
            thumbnailUrl = "https://i.ytimg.com/vi/jfKfPfyJRdk/hqdefault.jpg",
            videoId = "jfKfPfyJRdk",
            browseId = null,
            isVideo = true,
            isLive = true,
            liveViewersText = "18K watching",
            liveCategory = LiveStationCategory.LOFI_CHILL,
        )
        val regularAlbum = ShelfItem(
            title = "Midnight Stories",
            subtitle = "Album • Aria",
            thumbnailUrl = "https://example.com/art.jpg",
            videoId = null,
            browseId = "MPREb_12345",
        )

        val encoded = AppSettings.encodePinnedSpeedDial(listOf(liveStation, regularAlbum))
        val decoded = AppSettings.decodePinnedSpeedDial(encoded)

        assertEquals(2, decoded.size)
        val decodedLive = decoded[0]
        assertEquals(liveStation.title, decodedLive.title)
        assertEquals(liveStation.videoId, decodedLive.videoId)
        assertTrue(decodedLive.isLive)
        assertTrue(decodedLive.isVideo)
        assertEquals("18K watching", decodedLive.liveViewersText)
        assertEquals(LiveStationCategory.LOFI_CHILL, decodedLive.liveCategory)

        val decodedAlbum = decoded[1]
        assertEquals("MPREb_12345", decodedAlbum.browseId)
        assertFalse(decodedAlbum.isLive)
        assertFalse(decodedAlbum.isVideo)
        assertNull(decodedAlbum.liveViewersText)
        assertNull(decodedAlbum.liveCategory)
    }

    @Test
    fun decodePinnedSpeedDialHandlesLegacyJsonWithoutLiveFlags() {
        val legacyJson = """
            [
              {
                "title": "Legacy Radio",
                "subtitle": "Playlist",
                "thumbnailUrl": "",
                "videoId": "",
                "browseId": "VL_old_1"
              }
            ]
        """.trimIndent()

        val decoded = AppSettings.decodePinnedSpeedDial(legacyJson)
        assertEquals(1, decoded.size)
        assertEquals("Legacy Radio", decoded[0].title)
        assertEquals("VL_old_1", decoded[0].browseId)
        assertFalse(decoded[0].isLive)
        assertFalse(decoded[0].isVideo)
        assertNull(decoded[0].thumbnailUrl)
        assertNull(decoded[0].videoId)
    }
}
