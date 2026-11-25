/*
 * Copyright (C) 2024-2026 Kushagra Singh and BitChord contributors <https://github.com/kushagrasinghx/BitChord>
 * Copyright (C) 2026 Shivanshu (code4nigel) <https://github.com/code4nigel/ChordPlay>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.music.bitchord.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.ChordPlayPalette
import com.music.bitchord.data.settings.LibrarySort
import com.music.bitchord.data.settings.LibraryViewMode
import com.music.bitchord.data.settings.LibraryViewType
import com.music.bitchord.data.settings.SpeedDialLayoutMode
import com.music.bitchord.data.settings.SpeedDialSource
import com.music.bitchord.data.settings.SpeedDialTileStyle
import com.music.bitchord.download.Downloads
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The shared pages' settings and downloads, answered from the phone's own stores. */
object AndroidAppUiHost : AppUiHost {
    override val appDesignStyle = AppSettings.appDesignStyle
    override val isDynamicTheme: StateFlow<Boolean> = combine(
        AppSettings.colorPalette,
        AppSettings.activeCustomPaletteId,
    ) { palette, customId ->
        customId == null && palette == ChordPlayPalette.DYNAMIC_ARTWORK
    }.stateIn(
        scope = CoroutineScope(Dispatchers.Main.immediate),
        started = SharingStarted.Eagerly,
        initialValue = AppSettings.activeCustomPaletteId.value == null && AppSettings.colorPalette.value == ChordPlayPalette.DYNAMIC_ARTWORK,
    )
    override val swipeToPlayNext = AppSettings.swipeToPlayNext
    override val songSwipeRightAction = AppSettings.songSwipeRightAction
    override val songSwipeLeftAction = AppSettings.songSwipeLeftAction
    override val homeRecentsViewType = AppSettings.homeRecentsViewType
    override fun setHomeRecentsViewType(value: LibraryViewType) = AppSettings.setHomeRecentsViewType(value)
    override val pinnedPlaylists = AppSettings.pinnedPlaylists
    override val pinnedSpeedDial = AppSettings.pinnedSpeedDialItems
    override fun togglePinSpeedDial(item: com.music.bitchord.data.model.ShelfItem) = AppSettings.togglePinSpeedDial(item)
    override val librarySort = AppSettings.librarySort
    override fun setLibrarySort(value: LibrarySort) = AppSettings.setLibrarySort(value)
    override val libraryViewMode = AppSettings.libraryViewMode
    override fun setLibraryViewMode(value: LibraryViewMode) = AppSettings.setLibraryViewMode(value)
    override val speedDialLayoutMode = AppSettings.speedDialLayoutMode
    override fun setSpeedDialLayoutMode(value: SpeedDialLayoutMode) = AppSettings.setSpeedDialLayoutMode(value)
    override val speedDialSource = AppSettings.speedDialSource
    override fun setSpeedDialSource(value: SpeedDialSource) = AppSettings.setSpeedDialSource(value)
    override val speedDialTileStyle = AppSettings.speedDialTileStyle
    override fun setSpeedDialTileStyle(value: SpeedDialTileStyle) = AppSettings.setSpeedDialTileStyle(value)
    override val speedDialShowLabels = AppSettings.speedDialShowLabels
    override fun setSpeedDialShowLabels(value: Boolean) = AppSettings.setSpeedDialShowLabels(value)
    override val speedDialLabelOverrides = AppSettings.speedDialLabelOverrides
    override fun toggleSpeedDialTileLabel(item: com.music.bitchord.data.model.ShelfItem) = AppSettings.toggleSpeedDialTileLabel(item)
    override val speedDialShowPinBadge = AppSettings.speedDialShowPinBadge
    override fun setSpeedDialShowPinBadge(value: Boolean) = AppSettings.setSpeedDialShowPinBadge(value)
    override val speedDialShowMixes = AppSettings.speedDialShowMixes
    override fun setSpeedDialShowMixes(value: Boolean) = AppSettings.setSpeedDialShowMixes(value)
    override val speedDialShowArtists = AppSettings.speedDialShowArtists
    override fun setSpeedDialShowArtists(value: Boolean) = AppSettings.setSpeedDialShowArtists(value)
    override val speedDialShowChordWave = AppSettings.speedDialShowChordWave
    override fun setSpeedDialShowChordWave(value: Boolean) = AppSettings.setSpeedDialShowChordWave(value)
    override val hiddenSpeedDialIds = AppSettings.hiddenSpeedDialIds
    override fun hideFromSpeedDial(item: com.music.bitchord.data.model.ShelfItem) = AppSettings.hideFromSpeedDial(item)
    override fun resetHiddenSpeedDial() = AppSettings.resetHiddenSpeedDial()
    override fun reorderPinnedSpeedDial(reordered: List<com.music.bitchord.data.model.ShelfItem>) =
        AppSettings.reorderPinnedSpeedDial(reordered)
    override val spotifyConnected: StateFlow<Boolean> = AppSettings.spotifySpdcToken
        .map { it.isNotBlank() }
        .stateIn(
            scope = CoroutineScope(Dispatchers.Main.immediate),
            started = SharingStarted.Eagerly,
            initialValue = AppSettings.spotifySpdcToken.value.isNotBlank(),
        )
    override val homeShelfOrder = AppSettings.homeShelfOrder
    override val hiddenHomeShelves = AppSettings.hiddenHomeShelves
    override val homeMelodyVisible = AppSettings.homeMelodyVisible
    override val homeReplayVisible = AppSettings.homeReplayVisible
    override val hideHomeVideos = AppSettings.hideHomeVideos
    override val squareVideoThumbnails = AppSettings.squareVideoThumbnails

    /**
     * [Downloads.saved]'s ids, mirrored once into snapshot state for every row
     * to read.
     *
     * Every track row asks this whether to draw its downloaded mark, and it
     * used to answer with a `collectAsStateWithLifecycle` of its own — so each
     * row on screen set up a coroutine and a lifecycle observer on the same
     * flow, and tore them down again as it scrolled off. One collector here,
     * for the life of the process, writes a state each row merely reads: a
     * row scrolling in costs a snapshot read, and a finished download still
     * redraws exactly the rows that read it.
     *
     * Created on first read, which is always from composition and so on the
     * main thread, where [MainScope] also writes it.
     */
    private val savedIds: State<Set<String>> by lazy(LazyThreadSafetyMode.NONE) {
        val state = mutableStateOf(Downloads.saved.value.keys, referentialEqualityPolicy())
        MainScope().launch { Downloads.saved.collect { state.value = it.keys } }
        state
    }

    @Composable
    override fun downloadedIds(): Set<String> = savedIds.value
}
