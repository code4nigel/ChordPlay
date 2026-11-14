package com.music.bitchord.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.ChordPlayPalette
import com.music.bitchord.data.settings.LibrarySort
import com.music.bitchord.data.settings.LibraryViewMode
import com.music.bitchord.data.settings.LibraryViewType
import com.music.bitchord.data.settings.SpeedDialLayoutMode
import com.music.bitchord.download.Downloads
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

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
    override val homeShelfOrder = AppSettings.homeShelfOrder
    override val disabledHomeShelves = AppSettings.disabledHomeShelves

    @Composable
    override fun downloadedIds(): Set<String> {
        val saved by Downloads.saved.collectAsStateWithLifecycle()
        return saved.keys
    }
}
