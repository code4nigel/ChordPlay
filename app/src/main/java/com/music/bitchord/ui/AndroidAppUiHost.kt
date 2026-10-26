package com.music.bitchord.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.LibraryViewMode
import com.music.bitchord.data.settings.LibraryViewType
import com.music.bitchord.download.Downloads

/** The shared pages' settings and downloads, answered from the phone's own stores. */
object AndroidAppUiHost : AppUiHost {
    override val appDesignStyle = AppSettings.appDesignStyle
    override val swipeToPlayNext = AppSettings.swipeToPlayNext
    override val songSwipeRightAction = AppSettings.songSwipeRightAction
    override val songSwipeLeftAction = AppSettings.songSwipeLeftAction
    override val homeRecentsViewType = AppSettings.homeRecentsViewType
    override fun setHomeRecentsViewType(value: LibraryViewType) = AppSettings.setHomeRecentsViewType(value)
    override val pinnedPlaylists = AppSettings.pinnedPlaylists
    override val pinnedSpeedDial = AppSettings.pinnedSpeedDialItems
    override fun togglePinSpeedDial(item: com.music.bitchord.data.model.ShelfItem) = AppSettings.togglePinSpeedDial(item)
    override val librarySort = AppSettings.librarySort
    override val libraryViewMode = AppSettings.libraryViewMode
    override fun setLibraryViewMode(value: LibraryViewMode) = AppSettings.setLibraryViewMode(value)

    @Composable
    override fun downloadedIds(): Set<String> {
        val saved by Downloads.saved.collectAsStateWithLifecycle()
        return saved.keys
    }
}
