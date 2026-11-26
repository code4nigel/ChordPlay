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

import com.music.bitchord.data.model.ShelfItem

/** Visual border / surface treatment for Your Space artwork tiles. */
enum class SpeedDialTileStyle(val label: String) {
    MINIMAL("Minimal"),
    BEVEL_EDGE("Bevel Edge"),
    FROSTED_GLASS("Frosted Glass"),
}

/** Where unpinned picks for Your Space originate. */
enum class SpeedDialSource(val label: String) {
    SMART_BLEND("Smart"),
    YOUTUBE_MUSIC("YT Music"),
    HISTORY("History"),
    SPOTIFY("Spotify");

    /** Resolves [SPOTIFY] back to [SMART_BLEND] when Spotify is disconnected. */
    fun effective(spotifyConnected: Boolean): SpeedDialSource =
        if (this == SPOTIFY && !spotifyConnected) SMART_BLEND else this

    companion object {
        fun choices(spotifyConnected: Boolean): List<SpeedDialSource> =
            if (spotifyConnected) entries else entries.filterNot { it == SPOTIFY }
    }
}

/** Tabs hosted inside the Your Space multi-dial pager. */
enum class YourSpaceDialTab {
    SPACE,
    MIXES,
    ARTISTS,
}

/** Canonical identifier for matching Speed Dial items across pins, hides, and label overrides. */
fun ShelfItem.speedDialStableId(): String {
    val vId = videoId?.takeIf { it.isNotBlank() }
    if (vId != null) return vId
    val bId = browseId?.takeIf { it.isNotBlank() }?.removePrefix("VL")
    if (bId != null) return bId
    return title.trim()
}

/** Whether a [ShelfItem] represents an artist/channel candidate for the Artist Dial. */
fun ShelfItem.isArtistDialCandidate(): Boolean =
    videoId == null && (browseId?.startsWith("UC") == true || subtitle.contains("artist", ignoreCase = true))

const val SPEED_DIAL_SLOT_BLANK = -1
const val SPEED_DIAL_SLOT_LUCKY = -2

/**
 * Computes slot assignments per page for a 3-column dial grid.
 *
 * Guarantees at least one full page, preserves every pinned item, places [SPEED_DIAL_SLOT_LUCKY]
 * at the bottom-right of Page 1 (`perPage - 1`) when [includeLucky] is true, and fills remaining
 * slots sequentially with `0 until shown` (padding tail slots with [SPEED_DIAL_SLOT_BLANK] only
 * when necessary to keep all pins or when Page 1 has fewer items than slots).
 */
fun computeDialSlots(
    itemCount: Int,
    pinnedCount: Int,
    perPage: Int,
    includeLucky: Boolean,
): IntArray {
    if (perPage <= 0) return IntArray(0)
    val safeCount = itemCount.coerceAtLeast(0)
    val safePinned = pinnedCount.coerceIn(0, safeCount)
    val extras = if (includeLucky) 1 else 0
    val needed = safeCount + extras

    var pages = needed / perPage
    if (pages == 0 || pages * perPage - extras < safePinned) {
        pages = (needed + perPage - 1) / perPage
    }
    pages = pages.coerceAtLeast(1)

    val total = pages * perPage
    val slots = IntArray(total) { SPEED_DIAL_SLOT_BLANK }
    if (includeLucky) {
        slots[perPage - 1] = SPEED_DIAL_SLOT_LUCKY
    }
    val shown = minOf(safeCount, total - extras)
    var next = 0
    for (i in 0 until total) {
        if (slots[i] == SPEED_DIAL_SLOT_BLANK && next < shown) {
            slots[i] = next++
        }
    }
    return slots
}
