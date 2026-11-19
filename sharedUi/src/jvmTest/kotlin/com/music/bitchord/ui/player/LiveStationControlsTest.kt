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
package com.music.bitchord.ui.player

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveStationControlsTest {

    @Test
    fun formatBehindLiveFormatsZeroNegativeAndMultiUnitOffsets() {
        assertEquals("0:00", formatBehindLive(0L))
        assertEquals("-0:05", formatBehindLive(-5_000L))
        assertEquals("-0:05", formatBehindLive(5_000L))
        assertEquals("-1:23", formatBehindLive(-83_000L))
        assertEquals("-1:05:00", formatBehindLive(-3_900_000L))
    }

    @Test
    fun resolveArtworkHighlightStyleAlwaysKeepsWhiteIconTintAndCalmsBrightAccents() {
        val expectedTint = Color.White.copy(alpha = 0.96f)
        val whiteStyle = resolveArtworkHighlightStyle(Color.White, Color.White)
        val yellowStyle = resolveArtworkHighlightStyle(Color(0xFFFFF59D), Color(0xFFFFF176))
        val darkBlueStyle = resolveArtworkHighlightStyle(Color(0xFF1E3A8A), Color(0xFF1D4ED8))
        val nullStyle = resolveArtworkHighlightStyle(null, null)

        assertEquals(expectedTint, whiteStyle.activeIconTint)
        assertEquals(expectedTint, yellowStyle.activeIconTint)
        assertEquals(expectedTint, darkBlueStyle.activeIconTint)
        assertEquals(expectedTint, nullStyle.activeIconTint)

        assertTrue(whiteStyle.fillPrimary.luminance() <= 0.25f)
        assertTrue(yellowStyle.fillPrimary.luminance() <= 0.25f)
        assertTrue(whiteStyle.underlayAlpha > darkBlueStyle.underlayAlpha)
        assertTrue(yellowStyle.underlayAlpha > darkBlueStyle.underlayAlpha)
    }
}
