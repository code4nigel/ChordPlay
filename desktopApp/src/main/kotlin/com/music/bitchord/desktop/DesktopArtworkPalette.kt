package com.music.bitchord.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.music.bitchord.ui.graphics.ColorUtils
import com.music.bitchord.ui.graphics.paletteSwatches
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * ChordPlay Dynamic Artwork Palette for Desktop.
 *
 * Extracts vibrant accents, atmospheric ambient glows, and adaptive liquid glass surfaces
 * directly from song artwork, killing dull gray/brown pixel averages.
 */
@Immutable
data class DesktopArtworkPalette(
    val primary: Color,
    val secondary: Color,
    val primaryGlow: Color = primary,
    val ambientAura: Color = secondary.copy(alpha = 0.45f),
    val surfaceGlass: Color = Color(0x22FFFFFF),
) {
    companion object {
        // ChordPlay signature Crimson (#FF375F / #E50914) & Aura Blue/Violet (#4E6DFF)
        val Default = DesktopArtworkPalette(
            primary = Color(0xFFFF375F),
            secondary = Color(0xFF4E6DFF),
            primaryGlow = Color(0xFFFF375F),
            ambientAura = Color(0x334E6DFF),
            surfaceGlass = Color(0x1AFFFFFF),
        )

        fun from(bitmap: ImageBitmap?): DesktopArtworkPalette {
            if (bitmap == null || bitmap.width == 0 || bitmap.height == 0) return Default

            // 1. Swatch clustering & candidate filtering
            val allSwatches = runCatching { paletteSwatches(bitmap, maxColors = 24, clearFilters = true) }
                .getOrDefault(emptyList())

            val hsl = FloatArray(3)
            // Discard near-black (luminance < 0.12), near-white (luminance > 0.90), and low-saturation (sat < 0.15)
            val filteredSwatches = allSwatches.filter { swatch ->
                ColorUtils.colorToHSL(swatch.rgb, hsl)
                val sat = hsl[1]
                val lum = hsl[2]
                lum in 0.12f..0.90f && sat >= 0.15f
            }

            val candidatePool = filteredSwatches.ifEmpty { allSwatches }
            if (candidatePool.isEmpty()) return Default

            // Score candidate swatches by saturation * sqrt(population) with mid-luminance weighting
            val vibrantSwatch = candidatePool.maxByOrNull { swatch ->
                ColorUtils.colorToHSL(swatch.rgb, hsl)
                val sat = hsl[1]
                val lum = hsl[2]
                val lumWeight = if (lum in 0.25f..0.78f) 1.25f else 0.5f
                sat * sqrt(swatch.population.toFloat()) * lumWeight
            } ?: candidatePool.first()

            val dominantSwatch = allSwatches.maxByOrNull { it.population } ?: vibrantSwatch

            // Primary: Vibrant dominant color for play buttons and primary accents
            val vibrantHsl = FloatArray(3).also { ColorUtils.colorToHSL(vibrantSwatch.rgb, it) }
            val primaryHsl = floatArrayOf(
                vibrantHsl[0],
                vibrantHsl[1].coerceIn(0.55f, 1.0f),
                vibrantHsl[2].coerceIn(0.45f, 0.68f),
            )
            val primary = Color(ColorUtils.HSLToColor(primaryHsl))

            // Primary Glow: High-luminance saturated accent for ambient glows and scrubbers
            val glowHsl = floatArrayOf(
                vibrantHsl[0],
                vibrantHsl[1].coerceIn(0.70f, 1.0f),
                vibrantHsl[2].coerceIn(0.52f, 0.75f),
            )
            val primaryGlow = Color(ColorUtils.HSLToColor(glowHsl))

            // Secondary: Harmonized accent for sub-labels and secondary badges
            val dominantHsl = FloatArray(3).also { ColorUtils.colorToHSL(dominantSwatch.rgb, it) }
            val secondaryHue = if (dominantHsl[1] > 0.20f && abs(dominantHsl[0] - vibrantHsl[0]) > 25f) {
                dominantHsl[0]
            } else {
                (vibrantHsl[0] + 30f) % 360f
            }
            val secondaryHsl = floatArrayOf(
                secondaryHue,
                dominantHsl[1].coerceIn(0.40f, 0.85f),
                dominantHsl[2].coerceIn(0.40f, 0.65f),
            )
            val secondary = Color(ColorUtils.HSLToColor(secondaryHsl))

            // Ambient Aura: Low-frequency softened color for radial background illumination
            val auraHsl = floatArrayOf(
                secondaryHue,
                dominantHsl[1].coerceIn(0.45f, 0.90f),
                dominantHsl[2].coerceIn(0.14f, 0.26f),
            )
            val ambientAura = Color(ColorUtils.HSLToColor(auraHsl)).copy(alpha = 0.50f)

            // Surface Glass: Frosted translucent card background with contrast adaptation
            val surfaceGlass = Color(
                red = (primary.red * 0.16f + 0.08f).coerceIn(0.06f, 0.22f),
                green = (primary.green * 0.16f + 0.08f).coerceIn(0.06f, 0.22f),
                blue = (primary.blue * 0.16f + 0.10f).coerceIn(0.08f, 0.25f),
                alpha = 0.20f,
            )

            return DesktopArtworkPalette(
                primary = primary,
                secondary = secondary,
                primaryGlow = primaryGlow,
                ambientAura = ambientAura,
                surfaceGlass = surfaceGlass,
            )
        }
    }
}

@Composable
fun rememberDesktopArtworkPalette(url: String?): DesktopArtworkPalette {
    val palette by produceState(DesktopArtworkPalette.Default, key1 = url) {
        value = DesktopArtworkPalette.Default
        value = withContext(Dispatchers.IO) { DesktopArtworkPalette.from(DesktopArtworkCache.load(url)) }
    }
    return palette
}
