/*
 * Copyright (C) 2026 Shivanshu (code4nigel) <https://github.com/code4nigel/ChordPlay>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.music.bitchord.data.settings

/** Style of the expanded Now Playing progress bar wave and transport feel. */
enum class PlayerWaveStyle {
    /** Classic single-track Synesthesia waveform scrubber. */
    OG_WAVE,

    /** Artwork-synced multi-layer Aurora Silk ribbons with zero-seam motion and starlight orb. */
    AURORA_SILK,

    /** Measured track audio amplitude graph styled with ChordPlay Aurora glow and live pulse. */
    AUDIO_GRAPH,
}

/**
 * Normalized whole-track audio amplitude contour for [PlayerWaveStyle.AUDIO_GRAPH].
 * Values in [bars] sit in 0f..1f across the full track timeline; [measuredFraction] indicates
 * how much of the track has real decoded PCM measurements so far (1f = full track).
 */
class TrackAudioWaveform(
    val trackId: String,
    val bars: FloatArray,
    val measuredFraction: Float = 1f,
)

/**
 * Per-control elastic coupling preferences for the expanded Now Playing player deck.
 */
data class PlayerElasticConfig(
    val enabled: Boolean = true,
    val playPillInteraction: Boolean = true,
    val shuffle: Boolean = true,
    val previous: Boolean = true,
    val playPause: Boolean = true,
    val next: Boolean = true,
    val repeat: Boolean = true,
    val lyrics: Boolean = true,
    val qualityPill: Boolean = true,
    val queue: Boolean = true,
)
