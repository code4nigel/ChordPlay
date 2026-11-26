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
