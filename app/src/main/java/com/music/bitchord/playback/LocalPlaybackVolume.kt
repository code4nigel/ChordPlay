package com.music.bitchord.playback

import kotlin.math.pow
import kotlin.math.roundToInt

/** Audio output attenuation shared by the local playback and Studio decks. */
internal object LocalPlaybackVolume {
    @Volatile var gain: Float = 1f
}

internal data class LocalVolumeTarget(val index: Int, val gain: Float, val displayed: Float)

internal fun localVolumeTarget(
    level: Float,
    max: Int,
    fineVolume: Boolean,
    min: Int = 0,
    volumeDb: (Int) -> Float? = { null },
): LocalVolumeTarget {
    val percent = (level.coerceIn(0f, 1f) * 100).roundToInt()
    val requested = percent / 100f
    if (!fineVolume) {
        val index = (level.coerceIn(0f, 1f) * max).roundToInt().coerceAtLeast(min)
        return LocalVolumeTarget(index, 1f, index.toFloat() / max)
    }
    if (percent == 0) return LocalVolumeTarget(min, if (min == 0) 1f else 0f, 0f)

    val scaled = percent * max
    val upper = ((scaled + 99) / 100).coerceAtLeast(min)
    val lower = (scaled / 100).takeIf { it >= min } ?: 0
    if (lower == upper) return LocalVolumeTarget(upper, 1f, requested)

    // Interpolate in amplitude, relative to the covering hardware step.
    // The mute endpoint has zero amplitude even if a vendor reports finite dB.
    val upperDb = volumeDb(upper)?.takeIf(Float::isFinite)
    val lowerDb = if (lower > 0 && lower >= min) volumeDb(lower)?.takeIf(Float::isFinite) else null
    val lowerGain = when {
        lower < min || lower == 0 -> 0f
        upperDb != null && lowerDb != null && lowerDb < upperDb ->
            10.0.pow((lowerDb - upperDb) / 20.0).toFloat()
        else -> lower.toFloat() / upper
    }
    val fraction = (scaled - lower * 100) / ((upper - lower) * 100f)
    return LocalVolumeTarget(upper, lowerGain + (1f - lowerGain) * fraction, requested)
}
