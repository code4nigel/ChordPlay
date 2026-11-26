package com.music.bitchord.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Light-balanced artwork accent swatches used across the WavePlay scrubber,
 * Shuffle/Repeat indicators, Play/Pause orb, and Audio Graph.
 *
 * Ensures even deep/moody album covers produce vivid, legible light-colored
 * accents against the dark atmospheric player backdrop.
 */
@Immutable
internal data class ChordPlayLiftedAccents(
    val primary: Color,
    val secondary: Color,
    val highlight: Color,
    val filament: Color,
)

/**
 * Lifts raw OKLCH lightness/chroma of [color] so it reads crisply on dark
 * player surfaces while retaining the cover's harmonic hue.
 */
internal fun liftArtworkAccentColor(
    color: Color,
    minLightness: Float = 0.72f,
    maxLightness: Float = 0.87f,
    minChroma: Float = 0.11f,
): Color {
    val oklch = PlayerOklch.fromArgb(color.toArgb())
    val targetL = oklch.l.coerceIn(minLightness, maxLightness)
    val targetC = if (oklch.chroma < 0.025f) {
        // Near-neutral artwork keeps clean pearlescent tint rather than inventing hue
        oklch.chroma
    } else {
        oklch.chroma.coerceIn(minChroma, 0.24f)
    }
    return oklch.copy(l = targetL, chroma = targetC).toColor()
}

internal fun computeLiftedArtworkAccents(palette: MeshPalette): ChordPlayLiftedAccents {
    val rawPrimary = palette.colors.getOrElse(0) { Color(0xFFFF6D5A) }
    val rawSecondary = palette.colors.getOrElse(1) {
        lerp(rawPrimary, Color(0xFFFFB076), 0.35f)
    }
    val rawHighlight = palette.colors.getOrElse(2) {
        lerp(rawSecondary, Color.White, 0.32f)
    }
    val primary = liftArtworkAccentColor(rawPrimary, minLightness = 0.69f, maxLightness = 0.84f)
    val secondary = liftArtworkAccentColor(rawSecondary, minLightness = 0.74f, maxLightness = 0.88f)
    val highlight = lerp(liftArtworkAccentColor(rawHighlight, minLightness = 0.80f, maxLightness = 0.92f), Color.White, 0.22f)
    val filament = lerp(highlight, Color.White, 0.56f)
    return ChordPlayLiftedAccents(
        primary = primary,
        secondary = secondary,
        highlight = highlight,
        filament = filament,
    )
}

@Composable
internal fun rememberLiftedArtworkAccents(palette: MeshPalette): ChordPlayLiftedAccents {
    val target = remember(palette) { computeLiftedArtworkAccents(palette) }
    val primary by animateColorAsState(target.primary, tween(650), label = "liftedAccentPrimary")
    val secondary by animateColorAsState(target.secondary, tween(650), label = "liftedAccentSecondary")
    val highlight by animateColorAsState(target.highlight, tween(650), label = "liftedAccentHighlight")
    val filament by animateColorAsState(target.filament, tween(650), label = "liftedAccentFilament")
    return ChordPlayLiftedAccents(primary, secondary, highlight, filament)
}

private data class PlayerOklch(val l: Float, val chroma: Float, val hue: Float) {
    fun toColor(): Color {
        fun linear(c: Float): FloatArray {
            val radians = hue * (PI / 180.0)
            val a = c * cos(radians).toFloat()
            val b = c * sin(radians).toFloat()
            val lComp = l + 0.3963377774f * a + 0.2158037573f * b
            val mComp = l - 0.1055613458f * a - 0.0638541728f * b
            val sComp = l - 0.0894841775f * a - 1.2914855480f * b
            val l3 = lComp * lComp * lComp
            val m3 = mComp * mComp * mComp
            val s3 = sComp * sComp * sComp
            return floatArrayOf(
                4.0767416621f * l3 - 3.3077115913f * m3 + 0.2309699292f * s3,
                -1.2684380046f * l3 + 2.6097574011f * m3 - 0.3413193965f * s3,
                -0.0041960863f * l3 - 0.7034186147f * m3 + 1.7076147010f * s3,
            )
        }

        fun inGamut(rgb: FloatArray) = rgb.all { it in -0.0001f..1.0001f }
        var c = max(0f, chroma)
        if (!inGamut(linear(c))) {
            var low = 0f
            var high = c
            repeat(16) {
                val mid = (low + high) * 0.5f
                if (inGamut(linear(mid))) low = mid else high = mid
            }
            c = low
        }
        val rgb = linear(c)
        return Color(
            red = linearToSrgb(rgb[0].coerceIn(0f, 1f)),
            green = linearToSrgb(rgb[1].coerceIn(0f, 1f)),
            blue = linearToSrgb(rgb[2].coerceIn(0f, 1f)),
        )
    }

    companion object {
        fun fromArgb(argb: Int): PlayerOklch {
            val r = srgbToLinear(((argb shr 16) and 0xFF) / 255f)
            val g = srgbToLinear(((argb shr 8) and 0xFF) / 255f)
            val b = srgbToLinear((argb and 0xFF) / 255f)

            val l = cbrt(0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b)
            val m = cbrt(0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b)
            val s = cbrt(0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b)

            val lightness = 0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s
            val a = 1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s
            val bb = 0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s
            var hue = atan2(bb.toDouble(), a.toDouble()).toFloat() * (180f / PI.toFloat())
            if (hue < 0f) hue += 360f
            return PlayerOklch(lightness, sqrt(a * a + bb * bb), hue)
        }

        private fun srgbToLinear(c: Float): Float =
            if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)

        private fun cbrt(v: Float): Float =
            if (v < 0f) -(-v).pow(1f / 3f) else v.pow(1f / 3f)
    }
}

private fun linearToSrgb(c: Float): Float =
    if (c <= 0.0031308f) c * 12.92f else 1.055f * c.pow(1f / 2.4f) - 0.055f
