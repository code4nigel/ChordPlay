package com.music.bitchord.ui.player

import com.music.bitchord.data.settings.CoverAnimationMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoverMotionTest {
    private val modes = listOf(CoverAnimationMode.DIRECTIONAL_SWEEP, CoverAnimationMode.DISINTEGRATE)
    private val width = 1000f
    private val height = 1000f

    @Test
    fun nextMovesOutgoingLeftAndIncomingFromRight() {
        for (mode in modes) {
            val outMid = computeCoverTransform(mode, CoverChange.NEXT.direction, entering = false, progress = 0.5f, width, height)
            val inMid = computeCoverTransform(mode, CoverChange.NEXT.direction, entering = true, progress = 0.5f, width, height)

            assertTrue(outMid.translationX < 0f, "Expected $mode outgoing NEXT to move left, got ${outMid.translationX}")
            assertTrue(inMid.translationX > 0f, "Expected $mode incoming NEXT to enter from right, got ${inMid.translationX}")
            assertTrue(outMid.rotationY < 0f, "Expected $mode outgoing NEXT negative rotationY")
            assertTrue(inMid.rotationY > 0f, "Expected $mode incoming NEXT positive rotationY")
        }
    }

    @Test
    fun previousMirrorsNextDirection() {
        for (mode in modes) {
            val nextOut = computeCoverTransform(mode, CoverChange.NEXT.direction, entering = false, progress = 0.5f, width, height)
            val prevOut = computeCoverTransform(mode, CoverChange.PREVIOUS.direction, entering = false, progress = 0.5f, width, height)
            val nextIn = computeCoverTransform(mode, CoverChange.NEXT.direction, entering = true, progress = 0.5f, width, height)
            val prevIn = computeCoverTransform(mode, CoverChange.PREVIOUS.direction, entering = true, progress = 0.5f, width, height)

            assertEquals(-nextOut.translationX, prevOut.translationX, 0.001f)
            assertEquals(-nextOut.rotationY, prevOut.rotationY, 0.001f)
            assertEquals(-nextIn.translationX, prevIn.translationX, 0.001f)
            assertEquals(-nextIn.rotationY, prevIn.rotationY, 0.001f)
        }
    }

    @Test
    fun selectionUsesCentered3dDepthWithoutHorizontalSlide() {
        for (mode in modes) {
            val outMid = computeCoverTransform(mode, CoverChange.SELECTION.direction, entering = false, progress = 0.5f, width, height)
            val inMid = computeCoverTransform(mode, CoverChange.SELECTION.direction, entering = true, progress = 0.5f, width, height)

            assertEquals(0f, outMid.translationX, 0.001f)
            assertEquals(0f, inMid.translationX, 0.001f)
            assertEquals(0f, outMid.rotationY, 0.001f)
            assertEquals(0f, inMid.rotationY, 0.001f)
            assertTrue(outMid.rotationX != 0f)
            assertTrue(inMid.rotationX != 0f)
            assertTrue(inMid.scale < 1f)
        }
    }

    @Test
    fun transitionsStartAndSettleCleanlyAtEndpoints() {
        for (mode in modes) {
            for (dir in listOf(-1, 0, 1)) {
                val outStart = computeCoverTransform(mode, dir, entering = false, progress = 0f, width, height)
                val inStart = computeCoverTransform(mode, dir, entering = true, progress = 0f, width, height)
                assertEquals(1f, outStart.alpha, 0.001f)
                assertEquals(0f, outStart.translationX, 0.001f)
                assertEquals(0f, inStart.alpha, 0.001f)

                val outEnd = computeCoverTransform(mode, dir, entering = false, progress = 1f, width, height)
                val inEnd = computeCoverTransform(mode, dir, entering = true, progress = 1f, width, height)
                assertEquals(0f, outEnd.alpha, 0.001f)
                assertEquals(0f, inEnd.translationX, 0.001f)
                assertEquals(0f, inEnd.rotationY, 0.001f)
                assertEquals(0f, inEnd.rotationX, 0.001f)
                assertEquals(1f, inEnd.scale, 0.001f)
                assertEquals(1f, inEnd.alpha, 0.001f)
            }
        }
    }
}
