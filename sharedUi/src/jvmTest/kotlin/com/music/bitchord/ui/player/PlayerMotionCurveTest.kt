package com.music.bitchord.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerMotionCurveTest {
    @Test
    fun dreamyEaseOutHasQuickStartAndSmoothMonotonicTail() {
        val t15 = DreamyEaseOutEasing.transform(0.15f)
        val t40 = DreamyEaseOutEasing.transform(0.40f)
        val t75 = DreamyEaseOutEasing.transform(0.75f)
        val t90 = DreamyEaseOutEasing.transform(0.90f)

        // Quick start: significantly past linear progress early on.
        assertTrue("Expected quick takeoff at t=0.15f, got $t15", t15 > 0.45f)
        // Monotonic glide toward target without overshoot.
        assertTrue(t15 < t40 && t40 < t75 && t75 < t90 && t90 < 1f)
        // Smooth ease-out deceleration tail.
        val lateDelta = 1f - t75
        val earlyDelta = t40 - 0f
        assertTrue("Expected gentler late slope than early surge", lateDelta < earlyDelta * 0.25f)
        assertEquals(0f, DreamyEaseOutEasing.transform(0f), 0.0001f)
        assertEquals(1f, DreamyEaseOutEasing.transform(1f), 0.0001f)
    }

    @Test
    fun smoothStep01IsBoundedSymmetricAndSmoothAtEndpoints() {
        assertEquals(0f, smoothStep01(-0.5f), 0.0001f)
        assertEquals(0.5f, smoothStep01(0.5f), 0.0001f)
        assertEquals(1f, smoothStep01(1.5f), 0.0001f)
        assertTrue(smoothStep01(0.1f) < 0.05f)
        assertTrue(smoothStep01(0.9f) > 0.95f)
    }
}
