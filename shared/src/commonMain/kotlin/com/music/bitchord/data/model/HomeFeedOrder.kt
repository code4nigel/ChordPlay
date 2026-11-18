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
package com.music.bitchord.data.model

/**
 * Stable synthetic shelf identifiers so ChordPlay's own Home sections
 * (Speed Dial, ChordPlay Melody, Replay) can be reordered alongside
 * real loaded feed shelf titles without colliding with localized titles.
 */
object HomeShelfSlot {
    const val SPEED_DIAL = "__chordplay:speed_dial__"
    const val MELODY = "__chordplay:melody__"
    const val REPLAY = "__chordplay:replay__"
}

fun String.isSyntheticHomeSlot(): Boolean =
    this == HomeShelfSlot.SPEED_DIAL ||
        this == HomeShelfSlot.MELODY ||
        this == HomeShelfSlot.REPLAY

/** One ordered row in Home's vertical feed pipeline. */
sealed interface HomeFeedEntry {
    data class Shelf(val shelf: HomeShelf) : HomeFeedEntry
    data object SpeedDial : HomeFeedEntry
    data object Melody : HomeFeedEntry
    data object Replay : HomeFeedEntry
}

/**
 * Computes the complete ordered list of Home shelf IDs (real loaded feed titles +
 * enabled synthetic placements), preserving any hidden entries in their exact slots
 * so re-enabling restores them in place.
 */
fun orderHomeShelfIds(
    rawShelfTitles: List<String>,
    savedOrder: List<String>,
    includeSpeedDial: Boolean = true,
    includeMelody: Boolean = true,
    includeReplay: Boolean = true,
): List<String> {
    val cleanTitles = rawShelfTitles
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
    val defaultIds = buildList {
        if (includeSpeedDial) add(HomeShelfSlot.SPEED_DIAL)
        addAll(cleanTitles)
        if (includeMelody) add(HomeShelfSlot.MELODY)
        if (includeReplay) add(HomeShelfSlot.REPLAY)
    }
    if (savedOrder.isEmpty()) return defaultIds

    val validSet = defaultIds.toHashSet()
    val orderedKnown = savedOrder.filter { it in validSet }.distinct()
    val knownSet = orderedKnown.toHashSet()
    val missing = defaultIds.filterNot { it in knownSet }
    return orderedKnown + missing
}

/**
 * Sorts raw feed shelves according to the user's saved [savedOrder], keeping
 * unlisted shelves in their natural feed order behind any explicitly ordered ones.
 */
fun orderRawHomeShelves(
    shelves: List<HomeShelf>,
    savedOrder: List<String>,
): List<HomeShelf> {
    if (savedOrder.isEmpty() || shelves.size <= 1) return shelves
    val rank = HashMap<String, Int>(savedOrder.size)
    savedOrder.forEachIndexed { index, id ->
        if (!id.isSyntheticHomeSlot() && id !in rank) rank[id] = index
    }
    if (rank.isEmpty()) return shelves
    return shelves.withIndex()
        .sortedWith(
            compareBy<IndexedValue<HomeShelf>>(
                { rank[it.value.title] ?: Int.MAX_VALUE },
                { it.index },
            ),
        )
        .map { it.value }
}

/**
 * Interleaves real [shelves] with synthetic [HomeFeedEntry] items in exact user order,
 * excluding hidden IDs and unavailable synthetic cards.
 */
fun buildHomeFeedEntries(
    shelves: List<HomeShelf>,
    savedOrder: List<String>,
    hiddenIds: Set<String>,
    showSpeedDial: Boolean,
    showMelody: Boolean,
    showReplay: Boolean,
): List<HomeFeedEntry> {
    val visibleShelves = shelves.filterNot { it.title in hiddenIds }
    val shelfByTitle = LinkedHashMap<String, MutableList<HomeShelf>>(visibleShelves.size)
    visibleShelves.forEach { shelf ->
        shelfByTitle.getOrPut(shelf.title) { mutableListOf() }.add(shelf)
    }

    val allowSpeedDial = showSpeedDial && HomeShelfSlot.SPEED_DIAL !in hiddenIds
    val allowMelody = showMelody && HomeShelfSlot.MELODY !in hiddenIds
    val allowReplay = showReplay && HomeShelfSlot.REPLAY !in hiddenIds

    val orderedIds = orderHomeShelfIds(
        rawShelfTitles = visibleShelves.map { it.title },
        savedOrder = savedOrder,
        includeSpeedDial = allowSpeedDial,
        includeMelody = allowMelody,
        includeReplay = allowReplay,
    )

    return buildList(orderedIds.size) {
        orderedIds.forEach { id ->
            when (id) {
                HomeShelfSlot.SPEED_DIAL -> if (allowSpeedDial) add(HomeFeedEntry.SpeedDial)
                HomeShelfSlot.MELODY -> if (allowMelody) add(HomeFeedEntry.Melody)
                HomeShelfSlot.REPLAY -> if (allowReplay) add(HomeFeedEntry.Replay)
                else -> shelfByTitle[id]?.forEach { add(HomeFeedEntry.Shelf(it)) }
            }
        }
    }
}
