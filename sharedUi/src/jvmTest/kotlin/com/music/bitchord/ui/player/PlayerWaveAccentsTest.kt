package com.music.bitchord.ui.player

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.test.Test
import kotlin.test.assertTrue

class PlayerWaveAccentsTest {
    @Test
    fun darkArtworkAccentIsLiftedIntoReadableLightRange() {
        val darkCrimson = Color(0xFF3E0A08)
        val deepNavy = Color(0xFF0A142E)
        val accents = computeLiftedArtworkAccents(MeshPalette(listOf(darkCrimson, deepNavy)))

        assertTrue(accents.primary.luminance() > darkCrimson.luminance() + 0.15f)
        assertTrue(accents.secondary.luminance() > deepNavy.luminance() + 0.20f)
        assertTrue(accents.highlight.luminance() >= accents.primary.luminance())
        assertTrue(accents.filament.luminance() >= accents.highlight.luminance())
    }

    @Test
    fun brightArtworkRetainsVividLightAccent() {
        val brightCyan = Color(0xFF42D9F5)
        val brightPink = Color(0xFFFF7EB3)
        val accents = computeLiftedArtworkAccents(MeshPalette(listOf(brightCyan, brightPink)))

        assertTrue(accents.primary.luminance() in 0.25f..0.90f)
        assertTrue(accents.highlight.luminance() >= 0.45f)
    }

    @Test
    fun monochromePaletteAvoidsSpuriousSaturation() {
        val charcoal = Color(0xFF1E1E1E)
        val accents = computeLiftedArtworkAccents(MeshPalette(listOf(charcoal)))

        val spread = maxOf(accents.primary.red, accents.primary.green, accents.primary.blue) -
            minOf(accents.primary.red, accents.primary.green, accents.primary.blue)
        assertTrue(spread < 0.08f, "Expected near-neutral lifted gray/pearl, spread was $spread")
    }
}
