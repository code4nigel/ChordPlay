package com.music.bitchord.ui.player

import com.music.bitchord.data.settings.CoverAnimationMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoverPlaybackTest {
    @Test
    fun confirmedChangesUseTwoTimingsAndMirrorSkipDirections() {
        assertEquals(380, CoverChange.NEXT.durationMs)
        assertEquals(CoverChange.NEXT.animationSpec, CoverChange.PREVIOUS.animationSpec)
        assertEquals(CoverChange.NEXT.animationSpec, CoverChange.SELECTION.animationSpec)
        assertEquals(560, CoverChange.AUTOMATIC.durationMs)

        assertEquals(1, CoverChange.NEXT.direction)
        assertEquals(-1, CoverChange.PREVIOUS.direction)
        assertEquals(CoverChange.NEXT.direction, CoverChange.AUTOMATIC.direction)
        assertEquals(0, CoverChange.SELECTION.direction)
        assertTrue(CoverChange.NEXT.easing.transform(0.5f) > CoverChange.AUTOMATIC.easing.transform(0.5f))
    }

    @Test
    fun modeDurationsScaleByAnimationStyle() {
        for (change in CoverChange.entries) {
            assertEquals(0, change.durationMsFor(CoverAnimationMode.NONE))
            assertEquals(600, change.durationMsFor(CoverAnimationMode.DIRECTIONAL_SWEEP))
            assertEquals(680, change.durationMsFor(CoverAnimationMode.DISINTEGRATE))
        }

        for (mode in listOf(CoverAnimationMode.DIRECTIONAL_SWEEP, CoverAnimationMode.DISINTEGRATE)) {
            assertEquals(CoverChange.AUTOMATIC.animationSpecFor(mode), CoverChange.NEXT.animationSpecFor(mode))
            assertEquals(CoverChange.AUTOMATIC.animationSpecFor(mode), CoverChange.PREVIOUS.animationSpecFor(mode))
            assertEquals(CoverChange.AUTOMATIC.animationSpecFor(mode), CoverChange.SELECTION.animationSpecFor(mode))
        }
    }
}
