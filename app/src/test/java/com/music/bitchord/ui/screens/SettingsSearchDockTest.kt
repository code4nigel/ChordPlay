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

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsSearchDockTest {
    @Test
    fun shrinksInStepWithTheScroll() {
        assertEquals(0f, settingsSearchDockProgress(scroll = 0f, dockScroll = 300f), 0f)
        assertEquals(0.5f, settingsSearchDockProgress(scroll = 150f, dockScroll = 300f), 0.0001f)
        assertEquals(1f, settingsSearchDockProgress(scroll = 300f, dockScroll = 300f), 0f)
    }

    @Test
    fun holdsOnceDocked() {
        assertEquals(1f, settingsSearchDockProgress(scroll = 900f, dockScroll = 300f), 0f)
        assertEquals(1f, settingsSearchDockProgress(scroll = 0f, dockScroll = 0f), 0f)
    }

    @Test
    fun onlyTheLeftEdgeTravels() {
        assertEquals(48f, settingsSearchDockEdge(rest = 48f, slot = 204f, progress = 0f), 0f)
        assertEquals(126f, settingsSearchDockEdge(rest = 48f, slot = 204f, progress = 0.5f), 0.0001f)
        assertEquals(204f, settingsSearchDockEdge(rest = 48f, slot = 204f, progress = 1f), 0f)
    }
}
