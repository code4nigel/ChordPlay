package com.music.bitchord.ui.player

import androidx.compose.ui.unit.IntSize
import com.music.bitchord.data.settings.CanvasProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CanvasCrossfadeTest {

    @Test
    fun underStillCoverMaintainsFullCombinedOpacityAcrossReveal() {
        for (step in 0..20) {
            val t = step / 20f
            val alphas = computeCanvasRevealAlpha(
                reveal = t,
                presentationAlpha = 1f,
                underStillCover = true,
            )
            val topAlpha = 1f - alphas.coverAlpha
            val bottomAlpha = alphas.videoAlpha
            val combined = topAlpha + bottomAlpha * (1f - topAlpha)
            assertEquals(
                1f,
                combined,
                1e-5f,
                "Combined SrcOver opacity dipped at t=$t (video=$bottomAlpha, cover=${alphas.coverAlpha})",
            )
        }
    }

    @Test
    fun overlayCanvasScalesLinearlyWhenNotUnderStillCover() {
        val alphas = computeCanvasRevealAlpha(
            reveal = 0.6f,
            presentationAlpha = 0.5f,
            underStillCover = false,
        )
        assertEquals(0.3f, alphas.videoAlpha, 1e-5f)
        assertEquals(0.3f, alphas.coverAlpha, 1e-5f)
    }

    @Test
    fun canvasProviderDefaultParseSerializeAndDeduplication() {
        assertEquals(CanvasProvider.SPOTIFY, CanvasProvider.defaultOrder.first())
        assertEquals(CanvasProvider.defaultOrder, CanvasProvider.parse(null))
        assertEquals(CanvasProvider.defaultOrder, CanvasProvider.parse("  "))
        assertEquals(CanvasProvider.defaultOrder, CanvasProvider.parse("UNKNOWN,FOO"))

        val custom = listOf(
            CanvasProvider.TIDAL,
            CanvasProvider.SPOTIFY,
            CanvasProvider.COMMUNITY,
            CanvasProvider.APPLE_MUSIC,
        )
        val encoded = CanvasProvider.serialize(custom)
        assertEquals(custom, CanvasProvider.parse(encoded))

        // Partial / duplicate stored values get normalized cleanly with all remaining providers appended.
        assertEquals(
            listOf(
                CanvasProvider.APPLE_MUSIC,
                CanvasProvider.SPOTIFY,
                CanvasProvider.TIDAL,
                CanvasProvider.COMMUNITY,
            ),
            CanvasProvider.parse("APPLE_MUSIC,APPLE_MUSIC,UNKNOWN,SPOTIFY"),
        )
    }

    @Test
    fun isCanvasFrameReadyRejectsStaleFramesBeforeFirstRenderedFrame() {
        val bounds = IntSize(1080, 2400)
        assertFalse(
            isCanvasFrameReady(
                rendered = false,
                awaitingFirstFrame = true,
                awaitingSurfaceRepaint = false,
                surfaceAvailable = true,
                viewSize = bounds,
                clipAspect = 9f / 16f,
                contentMode = CanvasContentMode.CROP,
                portraitRevealBounds = bounds,
            ),
        )
        assertTrue(
            isCanvasFrameReady(
                rendered = false,
                awaitingFirstFrame = false,
                awaitingSurfaceRepaint = false,
                surfaceAvailable = true,
                viewSize = bounds,
                clipAspect = 9f / 16f,
                contentMode = CanvasContentMode.CROP,
                portraitRevealBounds = bounds,
            ),
        )
    }

    @Test
    fun isCanvasFrameReadyGuardsSurfaceAndPortraitLayoutBounds() {
        val bounds = IntSize(1080, 2400)
        assertFalse(
            isCanvasFrameReady(
                rendered = false,
                awaitingFirstFrame = false,
                awaitingSurfaceRepaint = true,
                surfaceAvailable = true,
                viewSize = bounds,
                clipAspect = 0.75f,
                contentMode = CanvasContentMode.FIT_PORTRAIT,
                portraitRevealBounds = bounds,
            ),
        )
        assertFalse(
            isCanvasFrameReady(
                rendered = false,
                awaitingFirstFrame = false,
                awaitingSurfaceRepaint = false,
                surfaceAvailable = false,
                viewSize = bounds,
                clipAspect = 0.75f,
                contentMode = CanvasContentMode.FIT_PORTRAIT,
                portraitRevealBounds = bounds,
            ),
        )
        assertFalse(
            isCanvasFrameReady(
                rendered = false,
                awaitingFirstFrame = false,
                awaitingSurfaceRepaint = false,
                surfaceAvailable = true,
                viewSize = IntSize.Zero,
                clipAspect = 0.75f,
                contentMode = CanvasContentMode.CROP,
                portraitRevealBounds = bounds,
            ),
        )
        assertFalse(
            isCanvasFrameReady(
                rendered = false,
                awaitingFirstFrame = false,
                awaitingSurfaceRepaint = false,
                surfaceAvailable = true,
                viewSize = IntSize(500, 500),
                clipAspect = 0.75f,
                contentMode = CanvasContentMode.FIT_PORTRAIT,
                portraitRevealBounds = bounds,
            ),
        )
        assertTrue(
            isCanvasFrameReady(
                rendered = false,
                awaitingFirstFrame = false,
                awaitingSurfaceRepaint = false,
                surfaceAvailable = true,
                viewSize = bounds,
                clipAspect = 0.75f,
                contentMode = CanvasContentMode.FIT_PORTRAIT,
                portraitRevealBounds = bounds,
            ),
        )
    }
}
