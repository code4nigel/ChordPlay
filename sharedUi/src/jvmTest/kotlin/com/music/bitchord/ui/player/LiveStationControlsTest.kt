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

import org.junit.Assert.assertEquals
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
}
