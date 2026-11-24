package com.music.bitchord.ui.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoverPlaybackTest {
    @Test
    fun confirmedChangesUseTwoTimingsAndMirrorSkipDirections() {
        assertEquals(220, CoverChange.NEXT.durationMs)
        assertEquals(CoverChange.NEXT.animationSpec, CoverChange.PREVIOUS.animationSpec)
        assertEquals(CoverChange.NEXT.animationSpec, CoverChange.SELECTION.animationSpec)
        assertEquals(420, CoverChange.AUTOMATIC.durationMs)

        assertEquals(1, CoverChange.NEXT.direction)
        assertEquals(-1, CoverChange.PREVIOUS.direction)
        assertEquals(CoverChange.NEXT.direction, CoverChange.AUTOMATIC.direction)
        assertEquals(0, CoverChange.SELECTION.direction)
        assertTrue(CoverChange.NEXT.easing.transform(0.5f) > CoverChange.AUTOMATIC.easing.transform(0.5f))
    }
}
