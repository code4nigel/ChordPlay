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
package com.music.bitchord.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.model.BrowseType
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.LibraryPage
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.UiState
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.data.settings.LibrarySort
import com.music.bitchord.data.settings.LibraryViewMode
import com.music.bitchord.sharedui.resources.Res
import com.music.bitchord.sharedui.resources.albums
import com.music.bitchord.sharedui.resources.clear_search
import com.music.bitchord.sharedui.resources.library_search_hint
import com.music.bitchord.sharedui.resources.sort_library
import com.music.bitchord.sharedui.resources.sort_default
import com.music.bitchord.sharedui.resources.sort_date_added
import com.music.bitchord.sharedui.resources.sort_date_added_oldest
import com.music.bitchord.sharedui.resources.sort_title_ascending
import com.music.bitchord.sharedui.resources.sort_title_descending
import com.music.bitchord.sharedui.resources.switch_to_grid_view
import com.music.bitchord.sharedui.resources.switch_to_list_view
import com.music.bitchord.sharedui.resources.artists
import com.music.bitchord.sharedui.resources.downloaded
import com.music.bitchord.sharedui.resources.import_spotify
import com.music.bitchord.sharedui.resources.import_spotify_subtitle
import com.music.bitchord.sharedui.resources.library
import com.music.bitchord.sharedui.resources.library_sign_in_description
import com.music.bitchord.sharedui.resources.more
import com.music.bitchord.sharedui.resources.new_playlist
import com.music.bitchord.sharedui.resources.on_device
import com.music.bitchord.sharedui.resources.pinned
import com.music.bitchord.sharedui.resources.playlists
import com.music.bitchord.sharedui.resources.retry
import com.music.bitchord.sharedui.resources.saved_to_youtube_music
import com.music.bitchord.sharedui.resources.sign_in
import com.music.bitchord.sharedui.resources.spotify_logo
import com.music.bitchord.ui.AppUi
import com.music.bitchord.ui.components.FastScroller
import com.music.bitchord.ui.components.LIBRARY_GRID_SPACING
import com.music.bitchord.ui.components.LazyContent
import com.music.bitchord.ui.components.MessageState
import com.music.bitchord.ui.components.PAGE_GUTTER
import com.music.bitchord.ui.components.PillSegmentedControl
import com.music.bitchord.ui.components.PullToRefresh
import com.music.bitchord.ui.components.SHELF_CARD_WIDTH
import com.music.bitchord.ui.components.ShelfRow
import com.music.bitchord.ui.components.SquirclePillShape
import com.music.bitchord.ui.components.contextClick
import com.music.bitchord.ui.components.lazyKey
import com.music.bitchord.ui.components.libraryGrid
import com.music.bitchord.ui.components.librarySkeleton
import com.music.bitchord.ui.components.longPressMenuClickable
import com.music.bitchord.ui.components.rememberShelfKeys
import com.music.bitchord.ui.components.uniqueKeys
import com.music.bitchord.ui.icons.BitChordIcons
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Filter categories for YouTube Music style Library tabs.
 */
enum class LibraryFilter(val label: String) {
    ALL("All"),
    PLAYLISTS("Playlists"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    DOWNLOADS("Downloads"),
}

/**
 * The signed-in library redesigned to YouTube Music's vertical stack UI.
 * Features top search bar, sorting, grid/list view switcher, category filter selector tabs,
 * and full-width vertical list item rows or 2-column grid cards.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    signedIn: Boolean,
    state: UiState<LibraryPage>,
    listState: LazyListState,
    onShelfItemClick: (ShelfItem) -> Unit,
    onShelfItemLongPress: (ShelfItem) -> Unit,
    onNewPlaylist: () -> Unit,
    onImportSpotifyPlaylist: (() -> Unit)? = null,
    onOpenSpotify: (() -> Unit)? = null,
    /** A shelf's "Show all" handler. */
    onShowAll: (HomeShelf) -> Unit,
    /** Replay header cards at the head of the library. */
    replay: @Composable () -> Unit,
    onSignIn: () -> Unit,
    onRetry: () -> Unit,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    pullState: PullToRefreshState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    /** The device's folders: downloads, local files, remote sources. */
    links: List<LibraryLink>,
    /** On-device releases / downloaded items. */
    deviceItems: List<ShelfItem>,
    /** Heading visibility. */
    showTitle: Boolean = true,
    richLibraryCards: Boolean = false,
) = BoxWithConstraints(modifier.fillMaxSize()) {
    val columns = if (richLibraryCards) {
        compactLibraryColumns(maxWidth - PAGE_GUTTER * 2, LocalDensity.current.fontScale)
    } else 2
    val pinnedPlaylists by AppUi.host.pinnedPlaylists.collectAsStateWithLifecycle()
    val librarySort by AppUi.host.librarySort.collectAsStateWithLifecycle()
    val libraryViewMode by AppUi.host.libraryViewMode.collectAsStateWithLifecycle()
    val downloadedLabel = stringResource(Res.string.downloaded)
    var selectedFilter by rememberSaveable { mutableStateOf(LibraryFilter.ALL) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val orderedDeviceItems = remember(deviceItems, librarySort) {
        deviceItems.withDeviceDateOrder(librarySort)
    }

    LaunchedEffect(selectedFilter) {
        listState.scrollToItem(0)
    }

    PullToRefresh(
        refreshing = refreshing,
        onRefresh = onRefresh,
        state = pullState,
        modifier = Modifier.fillMaxSize(),
    ) {
        FastScroller(
            listState = listState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
            ) {
                if (showTitle) {
                    item(key = "title", contentType = LazyContent.HEADER) {
                        Text(
                            text = stringResource(Res.string.library),
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
                        )
                    }
                }

                // 1. Preserved Replay / Stats header card on top (when not actively searching)
                if (searchQuery.isBlank()) {
                    item(key = "replay", contentType = LazyContent.HEADER) { replay() }
                }

                // 2. Search Bar with real-time text filtering, view mode switcher, and sorting
                item(key = "search_field", contentType = LazyContent.HEADER) {
                    LibrarySearchField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        sortOrder = librarySort,
                        onSortOrderChange = { AppUi.host.setLibrarySort(it) },
                        viewMode = libraryViewMode,
                        onViewModeToggle = {
                            AppUi.host.setLibraryViewMode(
                                if (libraryViewMode == LibraryViewMode.GRID) LibraryViewMode.LIST else LibraryViewMode.GRID,
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                    )
                }

                // 3. Top Selector Tabs / Filter Chips
                item(key = "filter_chips", contentType = LazyContent.HEADER) {
                    LibraryFilterChips(
                        selectedFilter = selectedFilter,
                        onFilterSelect = { selectedFilter = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                    )
                }

                // Always render local downloads regardless of online connection or sign-in state
                if (selectedFilter == LibraryFilter.DOWNLOADS) {
                    val downloadsItem = links.firstOrNull { it.item.browseId == "local:downloads" }?.item
                        ?: ShelfItem(
                            title = downloadedLabel,
                            subtitle = "",
                            thumbnailUrl = null,
                            videoId = null,
                            browseId = "local:downloads",
                        )
                    val downloadItems = (listOf(downloadsItem) + orderedDeviceItems)
                        .distinctBy { it.browseId ?: it.title }
                        .applyFilterAndSort(searchQuery, librarySort, pinnedPlaylists.toSet())
                    if (downloadItems.isEmpty()) {
                        item(key = "empty_downloads", contentType = LazyContent.MESSAGE) {
                            MessageState(
                                message = if (searchQuery.isNotBlank()) "No downloads match your search" else "No downloaded music on this device",
                            )
                        }
                    } else {
                        renderLibraryItems(
                            columns = columns,
                            richArtwork = richLibraryCards,
                            items = downloadItems,
                            viewMode = libraryViewMode,
                            pinnedPlaylists = pinnedPlaylists.toSet(),
                            onShelfItemClick = onShelfItemClick,
                            onShelfItemLongPress = onShelfItemLongPress,
                            keyPrefix = "dl:",
                        )
                    }
                    return@LazyColumn
                }

                if (!signedIn) {
                    when (selectedFilter) {
                        LibraryFilter.PLAYLISTS -> {
                            val offlinePlaylists = orderedDeviceItems.filter {
                                it.browseId?.startsWith("local:playlist") == true ||
                                    it.subtitle.contains("playlist", ignoreCase = true) ||
                                    it.title.contains("playlist", ignoreCase = true)
                            }.applyFilterAndSort(searchQuery, librarySort, pinnedPlaylists.toSet())

                            renderPlaylistTabContent(
                                columns = columns,
                                richArtwork = richLibraryCards,
                                items = offlinePlaylists,
                                viewMode = libraryViewMode,
                                pinnedPlaylists = pinnedPlaylists.toSet(),
                                showActions = searchQuery.isBlank(),
                                onNewPlaylist = onNewPlaylist,
                                onImportSpotifyPlaylist = onImportSpotifyPlaylist,
                                onOpenSpotify = onOpenSpotify,
                                onShelfItemClick = onShelfItemClick,
                                onShelfItemLongPress = onShelfItemLongPress,
                                emptyContent = {
                                    item(key = "sign_in_prompt") {
                                        MessageState(
                                            message = stringResource(Res.string.library_sign_in_description),
                                            actionLabel = stringResource(Res.string.sign_in),
                                            onAction = onSignIn,
                                        )
                                    }
                                },
                            )
                        }
                        LibraryFilter.ALBUMS -> {
                            val offlineAlbums = orderedDeviceItems.filter {
                                val id = it.browseId
                                !id.isNullOrBlank() &&
                                    (id.startsWith("local:album") || (!id.startsWith("local:playlist") && !it.subtitle.contains("playlist", ignoreCase = true)))
                            }.applyFilterAndSort(searchQuery, librarySort, pinnedPlaylists.toSet())

                            if (offlineAlbums.isNotEmpty()) {
                                renderLibraryItems(
                                    columns = columns,
                                    richArtwork = richLibraryCards,
                                    items = offlineAlbums,
                                    viewMode = libraryViewMode,
                                    pinnedPlaylists = pinnedPlaylists.toSet(),
                                    onShelfItemClick = onShelfItemClick,
                                    onShelfItemLongPress = onShelfItemLongPress,
                                )
                            } else {
                                item(key = "sign_in_prompt") {
                                    MessageState(
                                        message = stringResource(Res.string.library_sign_in_description),
                                        actionLabel = stringResource(Res.string.sign_in),
                                        onAction = onSignIn,
                                    )
                                }
                            }
                        }
                        else -> {
                            if (links.isNotEmpty() && selectedFilter == LibraryFilter.ALL) {
                                val filteredLinks = (links.map { it.item } + orderedDeviceItems)
                                    .distinctBy { it.browseId ?: it.title }
                                    .applyFilterAndSort(searchQuery, librarySort, pinnedPlaylists.toSet())
                                renderLibraryItems(
                                    columns = columns,
                                    richArtwork = richLibraryCards,
                                    items = filteredLinks,
                                    viewMode = libraryViewMode,
                                    pinnedPlaylists = pinnedPlaylists.toSet(),
                                    onShelfItemClick = onShelfItemClick,
                                    onShelfItemLongPress = onShelfItemLongPress,
                                )
                            } else {
                                item(key = "sign_in_prompt") {
                                    MessageState(
                                        message = stringResource(Res.string.library_sign_in_description),
                                        actionLabel = stringResource(Res.string.sign_in),
                                        onAction = onSignIn,
                                    )
                                }
                            }
                        }
                    }
                    return@LazyColumn
                }

                when (state) {
                    is UiState.Loading -> librarySkeleton()
                    is UiState.Error -> {
                        when (selectedFilter) {
                            LibraryFilter.PLAYLISTS -> {
                                val offlinePlaylists = orderedDeviceItems.filter {
                                    it.browseId?.startsWith("local:playlist") == true ||
                                        it.subtitle.contains("playlist", ignoreCase = true) ||
                                        it.title.contains("playlist", ignoreCase = true)
                                }.applyFilterAndSort(searchQuery, librarySort, pinnedPlaylists.toSet())

                                renderPlaylistTabContent(
                                    columns = columns,
                                    richArtwork = richLibraryCards,
                                    items = offlinePlaylists,
                                    viewMode = libraryViewMode,
                                    pinnedPlaylists = pinnedPlaylists.toSet(),
                                    showActions = searchQuery.isBlank(),
                                    onNewPlaylist = onNewPlaylist,
                                    onImportSpotifyPlaylist = onImportSpotifyPlaylist,
                                    onOpenSpotify = onOpenSpotify,
                                    onShelfItemClick = onShelfItemClick,
                                    onShelfItemLongPress = onShelfItemLongPress,
                                    emptyContent = {
                                        item(key = "empty_playlists") {
                                            MessageState(
                                                state.message,
                                                actionLabel = stringResource(Res.string.retry),
                                                onAction = onRetry,
                                            )
                                        }
                                    },
                                )
                            }
                            LibraryFilter.ALBUMS -> {
                                val offlineAlbums = orderedDeviceItems.filter {
                                    val id = it.browseId
                                    !id.isNullOrBlank() &&
                                        (id.startsWith("local:album") || (!id.startsWith("local:playlist") && !it.subtitle.contains("playlist", ignoreCase = true)))
                                }.applyFilterAndSort(searchQuery, librarySort, pinnedPlaylists.toSet())

                                if (offlineAlbums.isNotEmpty()) {
                                    renderLibraryItems(
                                        columns = columns,
                                        richArtwork = richLibraryCards,
                                        items = offlineAlbums,
                                        viewMode = libraryViewMode,
                                        pinnedPlaylists = pinnedPlaylists.toSet(),
                                        onShelfItemClick = onShelfItemClick,
                                        onShelfItemLongPress = onShelfItemLongPress,
                                    )
                                } else {
                                    item(key = "empty_albums") {
                                        MessageState(
                                            state.message,
                                            actionLabel = stringResource(Res.string.retry),
                                            onAction = onRetry,
                                        )
                                    }
                                }
                            }
                            LibraryFilter.ALL -> {
                                val offlineItems = (links.map { it.item } + orderedDeviceItems)
                                    .distinctBy { it.browseId ?: it.title }
                                    .applyFilterAndSort(searchQuery, librarySort, pinnedPlaylists.toSet())
                                if (offlineItems.isNotEmpty()) {
                                    renderLibraryItems(
                                        columns = columns,
                                        richArtwork = richLibraryCards,
                                        items = offlineItems,
                                        viewMode = libraryViewMode,
                                        pinnedPlaylists = pinnedPlaylists.toSet(),
                                        onShelfItemClick = onShelfItemClick,
                                        onShelfItemLongPress = onShelfItemLongPress,
                                    )
                                } else {
                                    item(key = "offline_empty") {
                                        MessageState(
                                            state.message,
                                            actionLabel = stringResource(Res.string.retry),
                                            onAction = onRetry,
                                        )
                                    }
                                }
                            }
                            else -> {
                                item(key = "offline_artists") {
                                    MessageState(
                                        state.message,
                                        actionLabel = stringResource(Res.string.retry),
                                        onAction = onRetry,
                                    )
                                }
                            }
                        }
                    }
                    is UiState.Success -> {
                        val shelves = state.data.shelves

                        when (selectedFilter) {
                            LibraryFilter.ALL -> {
                                // Quick links also include the cache when its Storage switch is on.
                                val likedMusicItem = shelves.flatMap { it.items }.firstOrNull {
                                    it.browseId == "VLLM" || it.browseId == "LM" ||
                                        it.title.equals("Liked Music", ignoreCase = true) ||
                                        it.title.equals("Liked Songs", ignoreCase = true)
                                } ?: ShelfItem(
                                    title = "Liked Music",
                                    subtitle = "Auto playlist",
                                    thumbnailUrl = null,
                                    videoId = null,
                                    browseId = YtMusicRepository.LIKED_MUSIC,
                                )

                                val downloadsItem = links.firstOrNull { it.item.browseId == "local:downloads" }?.item
                                    ?: ShelfItem(
                                        title = downloadedLabel,
                                        subtitle = "",
                                        thumbnailUrl = null,
                                        videoId = null,
                                        browseId = "local:downloads",
                                    )

                                val localMusicItem = links.firstOrNull { it.item.browseId == "local:all" }?.item
                                    ?: ShelfItem(
                                        title = "Local Music",
                                        subtitle = "",
                                        thumbnailUrl = null,
                                        videoId = null,
                                        browseId = "local:all",
                                    )

                                val cachedSongsItem = links.firstOrNull { it.item.browseId == "local:cache" }?.item
                                val quickItems = listOfNotNull(likedMusicItem, downloadsItem, localMusicItem, cachedSongsItem)
                                val filteredQuick = if (searchQuery.isBlank()) {
                                    quickItems
                                } else {
                                    val q = searchQuery.trim()
                                    quickItems.filter { it.title.contains(q, ignoreCase = true) }
                                }

                                if (filteredQuick.isNotEmpty()) {
                                    renderLibraryItems(
                                        columns = columns,
                                        richArtwork = richLibraryCards,
                                        items = filteredQuick,
                                        viewMode = libraryViewMode,
                                        pinnedPlaylists = pinnedPlaylists.toSet(),
                                        onShelfItemClick = onShelfItemClick,
                                        onShelfItemLongPress = onShelfItemLongPress,
                                        keyPrefix = "quick:",
                                    )
                                }

                                // Recent items across all categories
                                val quickKeys = quickItems.mapNotNullTo(mutableSetOf("VLLM", "LM")) { it.browseId }
                                val recentItems = shelves.flatMap { shelf ->
                                    if (shelf.title == PLAYLISTS || shelf.title.equals("Playlists", ignoreCase = true)) {
                                        shelf.copy(items = shelf.items.withCloudDateOrder(librarySort)).pinnedFirst(pinnedPlaylists).items
                                    } else {
                                        shelf.items.withCloudDateOrder(librarySort)
                                    }
                                }.filter {
                                    it.browseId !in quickKeys &&
                                        !it.title.equals("Liked Music", ignoreCase = true) &&
                                        !it.title.equals("Liked Songs", ignoreCase = true)
                                }.distinctBy { it.browseId ?: it.title }
                                    .applyFilterAndSort(searchQuery, librarySort, pinnedPlaylists.toSet())

                                renderLibraryItems(
                                    columns = columns,
                                    richArtwork = richLibraryCards,
                                    items = recentItems,
                                    viewMode = libraryViewMode,
                                    pinnedPlaylists = pinnedPlaylists.toSet(),
                                    onShelfItemClick = onShelfItemClick,
                                    onShelfItemLongPress = onShelfItemLongPress,
                                    keyPrefix = "recent:",
                                )
                            }

                            LibraryFilter.PLAYLISTS -> {
                                val localAndDownloadedPlaylists = orderedDeviceItems.filter {
                                    it.browseId?.startsWith("local:playlist") == true ||
                                        it.subtitle.contains("playlist", ignoreCase = true) ||
                                        it.title.contains("playlist", ignoreCase = true)
                                }
                                val playlistShelf = shelves.firstOrNull {
                                    it.title == PLAYLISTS || it.title.equals("Playlists", ignoreCase = true)
                                }
                                val remotePlaylists = playlistShelf?.let { shelf ->
                                    shelf.copy(items = shelf.items.withCloudDateOrder(librarySort)).pinnedFirst(pinnedPlaylists).items
                                }.orEmpty()
                                val playlists = (localAndDownloadedPlaylists + remotePlaylists)
                                    .distinctBy { it.browseId ?: it.title }
                                    .applyFilterAndSort(searchQuery, librarySort, pinnedPlaylists.toSet())

                                renderPlaylistTabContent(
                                    columns = columns,
                                    richArtwork = richLibraryCards,
                                    items = playlists,
                                    viewMode = libraryViewMode,
                                    pinnedPlaylists = pinnedPlaylists.toSet(),
                                    showActions = searchQuery.isBlank(),
                                    onNewPlaylist = onNewPlaylist,
                                    onImportSpotifyPlaylist = onImportSpotifyPlaylist,
                                    onOpenSpotify = onOpenSpotify,
                                    onShelfItemClick = onShelfItemClick,
                                    onShelfItemLongPress = onShelfItemLongPress,
                                    emptyContent = {
                                        item(key = "empty_playlists") {
                                            MessageState(
                                                message = if (searchQuery.isNotBlank()) "No playlists match your search" else "No playlists yet",
                                            )
                                        }
                                    },
                                )
                            }

                            LibraryFilter.ALBUMS -> {
                                val localAndDownloadedAlbums = orderedDeviceItems.filter {
                                    val id = it.browseId
                                    !id.isNullOrBlank() &&
                                        (id.startsWith("local:album") || (!id.startsWith("local:playlist") && !it.subtitle.contains("playlist", ignoreCase = true)))
                                }
                                val albumShelf = shelves.firstOrNull {
                                    it.title.equals("Albums", ignoreCase = true)
                                }
                                val remoteAlbums = albumShelf?.items.orEmpty().withCloudDateOrder(librarySort)
                                val albums = (localAndDownloadedAlbums + remoteAlbums)
                                    .distinctBy { it.browseId ?: it.title }
                                    .applyFilterAndSort(searchQuery, librarySort, pinnedPlaylists.toSet())

                                if (albums.isEmpty()) {
                                    item(key = "empty_albums") {
                                        MessageState(
                                            message = if (searchQuery.isNotBlank()) "No albums match your search" else "No saved albums yet",
                                        )
                                    }
                                } else {
                                    renderLibraryItems(
                                        columns = columns,
                                        richArtwork = richLibraryCards,
                                        items = albums,
                                        viewMode = libraryViewMode,
                                        pinnedPlaylists = pinnedPlaylists.toSet(),
                                        onShelfItemClick = onShelfItemClick,
                                        onShelfItemLongPress = onShelfItemLongPress,
                                    )
                                }
                            }

                            LibraryFilter.ARTISTS -> {
                                val artistShelves = shelves.filter {
                                    it.title.equals("Artists", ignoreCase = true) ||
                                        it.title.equals("Subscriptions", ignoreCase = true)
                                }
                                val artists = artistShelves.flatMap { it.items.withCloudDateOrder(librarySort) }.distinctBy { it.browseId ?: it.title }
                                    .applyFilterAndSort(searchQuery, librarySort, pinnedPlaylists.toSet())

                                if (artists.isEmpty()) {
                                    item(key = "empty_artists") {
                                        MessageState(
                                            message = if (searchQuery.isNotBlank()) "No artists match your search" else "No subscribed artists yet",
                                        )
                                    }
                                } else {
                                    renderLibraryItems(
                                        columns = columns,
                                        richArtwork = richLibraryCards,
                                        items = artists,
                                        viewMode = libraryViewMode,
                                        pinnedPlaylists = pinnedPlaylists.toSet(),
                                        onShelfItemClick = onShelfItemClick,
                                        onShelfItemLongPress = onShelfItemLongPress,
                                    )
                                }
                            }

                            LibraryFilter.DOWNLOADS -> {
                                val downloadItems = (links.map { it.item } + orderedDeviceItems)
                                    .distinctBy { it.browseId ?: it.title }
                                    .applyFilterAndSort(searchQuery, librarySort, pinnedPlaylists.toSet())

                                if (downloadItems.isEmpty()) {
                                    item(key = "empty_downloads") {
                                        MessageState(
                                            message = if (searchQuery.isNotBlank()) "No downloads match your search" else "No downloaded music on this device",
                                        )
                                    }
                                } else {
                                    renderLibraryItems(
                                        columns = columns,
                                        richArtwork = richLibraryCards,
                                        items = downloadItems,
                                        viewMode = libraryViewMode,
                                        pinnedPlaylists = pinnedPlaylists.toSet(),
                                        onShelfItemClick = onShelfItemClick,
                                        onShelfItemLongPress = onShelfItemLongPress,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Real-time search field matching LocalMusicScreen with clear button, view switcher (Grid/List),
 * and sort dropdown menu.
 */
@Composable
private fun LibrarySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    sortOrder: LibrarySort,
    onSortOrderChange: (LibrarySort) -> Unit,
    viewMode: LibraryViewMode,
    onViewModeToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var sortMenuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = PAGE_GUTTER, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, remember { SquirclePillShape() })
                .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.library_search_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable { onQueryChange("") },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = stringResource(Res.string.clear_search),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(Modifier.width(4.dp))
            }
        }
        Row(
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, remember { SquirclePillShape() }),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onViewModeToggle, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = if (viewMode == LibraryViewMode.GRID) BitChordIcons.ListView else BitChordIcons.GridView,
                    contentDescription = stringResource(
                        if (viewMode == LibraryViewMode.GRID) Res.string.switch_to_list_view else Res.string.switch_to_grid_view,
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(19.dp),
                )
            }
            Box {
                IconButton(onClick = { sortMenuOpen = true }, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Rounded.Sort,
                        contentDescription = stringResource(Res.string.sort_library),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp),
                    )
                }
                LibrarySortMenu(
                    expanded = sortMenuOpen,
                    selected = sortOrder,
                    onSelect = onSortOrderChange,
                    onDismiss = { sortMenuOpen = false },
                )
            }
        }
    }
}

@Composable
fun LibrarySortMenu(
    expanded: Boolean,
    selected: LibrarySort,
    onSelect: (LibrarySort) -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = lerp(scheme.surface, scheme.surfaceVariant, 0.72f),
        tonalElevation = 0.dp,
        modifier = Modifier.widthIn(min = 184.dp, max = 300.dp),
    ) {
        LibrarySort.entries.forEach { option ->
            val label = stringResource(when (option) {
                LibrarySort.DEFAULT -> Res.string.sort_default
                LibrarySort.DATE_NEWEST -> Res.string.sort_date_added
                LibrarySort.DATE_OLDEST -> Res.string.sort_date_added_oldest
                LibrarySort.TITLE_ASC -> Res.string.sort_title_ascending
                LibrarySort.TITLE_DESC -> Res.string.sort_title_descending
            })
            DropdownMenuItem(
                text = { Text(label, style = MaterialTheme.typography.bodyLarge) },
                trailingIcon = if (option == selected) {
                    { Icon(Icons.Rounded.Check, contentDescription = null, tint = scheme.primary) }
                } else null,
                onClick = {
                    onSelect(option)
                    onDismiss()
                },
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
    }
}

// YouTube Music returns separate shelves without item-level timestamps.
private fun List<ShelfItem>.withCloudDateOrder(sort: LibrarySort): List<ShelfItem> =
    if (sort == LibrarySort.DATE_OLDEST) asReversed() else this

private fun List<ShelfItem>.withDeviceDateOrder(sort: LibrarySort): List<ShelfItem> {
    val oldestFirst = compareBy<ShelfItem> { it.libraryAddedAt }
    val byDate = when (sort) {
        LibrarySort.DATE_NEWEST -> oldestFirst.reversed()
        LibrarySort.DATE_OLDEST -> oldestFirst
        else -> return this
    }
    return sortedWith(compareBy<ShelfItem> { it.libraryAddedAt == null }.then(byDate))
}

private fun List<ShelfItem>.applyFilterAndSort(
    query: String,
    sort: LibrarySort,
    pinned: Set<String>,
): List<ShelfItem> {
    val filtered = if (query.isBlank()) {
        this
    } else {
        val q = query.trim()
        filter {
            it.title.contains(q, ignoreCase = true) ||
                it.subtitle.contains(q, ignoreCase = true)
        }
    }
    return when (sort) {
        LibrarySort.DEFAULT, LibrarySort.DATE_NEWEST, LibrarySort.DATE_OLDEST -> {
            filtered.sortedByDescending { it.browseId != null && it.browseId in pinned }
        }
        LibrarySort.TITLE_ASC -> {
            filtered.sortedWith(
                compareByDescending<ShelfItem> { it.browseId != null && it.browseId in pinned }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title },
            )
        }
        LibrarySort.TITLE_DESC -> {
            filtered.sortedWith(
                compareByDescending<ShelfItem> { it.browseId != null && it.browseId in pinned }
                    .thenByDescending(String.CASE_INSENSITIVE_ORDER) { it.title },
            )
        }
    }
}

private sealed interface PlaylistGridSlot {
    val stableKey: String

    data class Action(
        override val stableKey: String,
        val content: @Composable (Modifier) -> Unit,
    ) : PlaylistGridSlot

    data class Item(
        override val stableKey: String,
        val item: ShelfItem,
    ) : PlaylistGridSlot
}

private fun LazyListScope.renderPlaylistTabContent(
    columns: Int,
    richArtwork: Boolean,
    items: List<ShelfItem>,
    viewMode: LibraryViewMode,
    pinnedPlaylists: Set<String>,
    showActions: Boolean,
    onNewPlaylist: () -> Unit,
    onImportSpotifyPlaylist: (() -> Unit)?,
    onOpenSpotify: (() -> Unit)?,
    onShelfItemClick: (ShelfItem) -> Unit,
    onShelfItemLongPress: (ShelfItem) -> Unit,
    emptyContent: LazyListScope.() -> Unit,
) {
    if (viewMode == LibraryViewMode.LIST) {
        if (showActions) {
            item(key = "new_playlist") {
                NewPlaylistActionRow(onClick = onNewPlaylist)
            }
            if (onImportSpotifyPlaylist != null) {
                item(key = "import_spotify") {
                    ImportSpotifyActionRow(onClick = onImportSpotifyPlaylist)
                }
            }
            if (onOpenSpotify != null) {
                item(key = "spotify_playlists") {
                    SpotifyPlaylistsActionRow(onClick = onOpenSpotify)
                }
            }
        }
        if (items.isEmpty()) {
            emptyContent()
        } else {
            renderLibraryItems(
                columns = columns,
                richArtwork = richArtwork,
                items = items,
                viewMode = viewMode,
                pinnedPlaylists = pinnedPlaylists,
                onShelfItemClick = onShelfItemClick,
                onShelfItemLongPress = onShelfItemLongPress,
            )
        }
        return
    }

    val actionSlots = if (!showActions) emptyList() else buildList {
        add(PlaylistGridSlot.Action("action:new_playlist") { mod ->
            NewShelfCard(
                icon = BitChordIcons.Plus,
                label = stringResource(Res.string.new_playlist),
                subtitle = stringResource(Res.string.saved_to_youtube_music),
                onClick = onNewPlaylist,
                modifier = mod,
            )
        })
        if (onImportSpotifyPlaylist != null) {
            add(PlaylistGridSlot.Action("action:import_spotify") { mod ->
                NewShelfCard(
                    icon = BitChordIcons.Plus,
                    logo = Res.drawable.spotify_logo,
                    label = stringResource(Res.string.import_spotify),
                    subtitle = stringResource(Res.string.import_spotify_subtitle),
                    onClick = onImportSpotifyPlaylist,
                    modifier = mod,
                )
            })
        }
        if (onOpenSpotify != null) {
            add(PlaylistGridSlot.Action("action:spotify_playlists") { mod ->
                val isConnected = remember {
                    runCatching { com.music.bitchord.data.spotify.SpotifyLibrary.isConnected() }.getOrDefault(false)
                }
                NewShelfCard(
                    icon = BitChordIcons.Plus,
                    logo = Res.drawable.spotify_logo,
                    label = "Spotify Playlists",
                    subtitle = if (isConnected) "Your connected Spotify playlists" else "Connect Spotify",
                    onClick = onOpenSpotify,
                    modifier = mod,
                )
            })
        }
    }

    val itemKeys = uniqueKeys(items) { "grid:" + it.lazyKey() }
    val itemSlots = items.mapIndexed { index, item ->
        PlaylistGridSlot.Item(itemKeys[index], item)
    }
    val slots = actionSlots + itemSlots
    if (slots.isNotEmpty()) {
        val rows = slots.chunked(columns)
        itemsIndexed(
            rows,
            key = { _, row -> "playlists:" + row.joinToString("|") { it.stableKey } },
            contentType = { _, _ -> LazyContent.CARD },
        ) { _, rowSlots ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PAGE_GUTTER, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowSlots.forEach { slot ->
                    when (slot) {
                        is PlaylistGridSlot.Action -> slot.content(Modifier.weight(1f))
                        is PlaylistGridSlot.Item -> {
                            val item = slot.item
                            val isPinned = item.browseId != null && item.browseId in pinnedPlaylists
                            ShelfCard(
                                item = item,
                                isPinned = isPinned,
                                onClick = { onShelfItemClick(item) },
                                onLongPress = { onShelfItemLongPress(item) },
                                modifier = Modifier.weight(1f),
                                artwork = libraryCardArtwork(item, richArtwork),
                            )
                        }
                    }
                }
                repeat(columns - rowSlots.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
    if (items.isEmpty()) {
        emptyContent()
    }
}

private fun LazyListScope.renderLibraryItems(
    columns: Int,
    richArtwork: Boolean,
    items: List<ShelfItem>,
    viewMode: LibraryViewMode,
    pinnedPlaylists: Set<String>,
    onShelfItemClick: (ShelfItem) -> Unit,
    onShelfItemLongPress: (ShelfItem) -> Unit,
    keyPrefix: String = "",
) {
    if (viewMode == LibraryViewMode.GRID) {
        val rows = items.chunked(columns)
        val pairKeys = uniqueKeys(rows) { pair ->
            keyPrefix + "grid:" + (pair.firstOrNull()?.lazyKey() ?: "")
        }
        itemsIndexed(
            rows,
            key = { index, _ -> pairKeys[index] },
            contentType = { _, _ -> LazyContent.CARD },
        ) { _, rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PAGE_GUTTER, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowItems.forEach { item ->
                    val isPinned = item.browseId != null && item.browseId in pinnedPlaylists
                    ShelfCard(
                        item = item,
                        isPinned = isPinned,
                        onClick = { onShelfItemClick(item) },
                        onLongPress = { onShelfItemLongPress(item) },
                        modifier = Modifier.weight(1f),
                        artwork = libraryCardArtwork(item, richArtwork),
                    )
                }
                repeat(columns - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    } else {
        val keys = uniqueKeys(items) { keyPrefix + it.lazyKey() }
        itemsIndexed(
            items,
            key = { index, _ -> keys[index] },
            contentType = { _, _ -> LazyContent.BROWSE_ROW },
        ) { _, item ->
            val isPinned = item.browseId != null && item.browseId in pinnedPlaylists
            val isArtist = isArtistItem(item)
            LibraryItemRow(
                item = item,
                isPinned = isPinned,
                isArtist = isArtist,
                albumSleeve = richArtwork && libraryArtworkType(item) == BrowseType.ALBUM,
                onClick = { onShelfItemClick(item) },
                onLongPress = { onShelfItemLongPress(item) },
            )
        }
    }
}

/**
 * Filter chip row right below the Library title with YouTube Music styling and contrast protection.
 */
@Composable
private fun LibraryFilterChips(
    selectedFilter: LibraryFilter,
    onFilterSelect: (LibraryFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val labels = listOf(
        LibraryFilter.ALL.label,
        stringResource(Res.string.playlists),
        stringResource(Res.string.albums),
        stringResource(Res.string.artists),
        LibraryFilter.DOWNLOADS.label,
    )
    PillSegmentedControl(
        options = labels,
        selectedIndex = LibraryFilter.entries.indexOf(selectedFilter).coerceAtLeast(0),
        onSelect = { index -> onFilterSelect(LibraryFilter.entries[index]) },
        modifier = modifier.padding(horizontal = PAGE_GUTTER, vertical = 4.dp),
    )
}

/**
 * Full-width vertical list item with thumbnail, title/subtitle, pinned badge, and 3-dot overflow menu.
 */
@Composable
private fun LibraryItemRow(
    item: ShelfItem,
    isPinned: Boolean,
    isArtist: Boolean,
    albumSleeve: Boolean = false,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .longPressMenuClickable(onClick = onClick, onLongClick = onLongPress)
            .contextClick(onLongPress)
            .padding(horizontal = PAGE_GUTTER, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isArtist) {
            ArtistAvatarThumbnail(item = item)
        } else if (albumSleeve) {
            Box(Modifier.width(84.dp), contentAlignment = Alignment.Center) {
                AlbumSleeveArtwork(item, Modifier.size(56.dp))
            }
        } else {
            LandscapeFolderThumbnail(item = item)
        }

        Spacer(Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val displaySubtitle = localizeCardSubtitle(item.subtitle)
            if (displaySubtitle.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = displaySubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (isPinned) {
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = BitChordIcons.Pin,
                contentDescription = stringResource(Res.string.pinned),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }

        IconButton(
            onClick = onLongPress,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = stringResource(Res.string.more),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Landscape folder thumbnail (~84.dp wide × ~50.dp high, rounded 10.dp) featuring
 * the YouTube-style "folder peek" (subtle stacked card border peeking behind).
 */
@Composable
private fun LandscapeFolderThumbnail(
    item: ShelfItem,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(width = 84.dp, height = 54.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Stacked top card peek (folder peek)
        Box(
            modifier = Modifier
                .width(72.dp)
                .height(46.dp)
                .align(Alignment.TopCenter)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                .border(
                    width = 0.5.dp,
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                ),
        )

        // Main landscape thumbnail in front
        Box(
            modifier = Modifier
                .size(width = 84.dp, height = 50.dp)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = 0.5.dp,
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            ThumbnailContent(item = item)
        }
    }
}

/**
 * Circular avatar thumbnail for subscribed and saved artists.
 */
@Composable
private fun ArtistAvatarThumbnail(
    item: ShelfItem,
    modifier: Modifier = Modifier.size(50.dp),
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.15f),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (!item.thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = item.thumbnailUrl.artworkAt(200),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

private fun compactLibraryColumns(available: androidx.compose.ui.unit.Dp, fontScale: Float): Int =
    if (fontScale >= 1.5f || (available - LIBRARY_GRID_SPACING * 2) / 3 < 96.dp) 2 else 3

private fun libraryArtworkType(item: ShelfItem): BrowseType? {
    item.browseType?.takeUnless { it == BrowseType.OTHER }?.let { return it }
    val id = item.browseId ?: return null
    return when {
        id.startsWith("UC") || id.startsWith("MPLA") -> BrowseType.ARTIST
        id.startsWith("MPRE") || id.removePrefix("VL").startsWith("OLAK") ||
            id.startsWith("local:album:") -> BrowseType.ALBUM
        id.startsWith("VL") || id.startsWith("PL") ||
            id.startsWith("local:playlist:") -> BrowseType.PLAYLIST
        else -> null
    }
}

private fun libraryCardArtwork(item: ShelfItem, enabled: Boolean): (@Composable () -> Unit)? {
    if (!enabled) return null
    return when {
        isArtistItem(item) -> {
            { ArtistAvatarThumbnail(item, Modifier.fillMaxWidth().aspectRatio(1f)) }
        }
        libraryArtworkType(item) == BrowseType.ALBUM -> {
            { AlbumSleeveArtwork(item, Modifier.fillMaxWidth().aspectRatio(1f)) }
        }
        else -> null
    }
}

@Composable
private fun AlbumSleeveArtwork(item: ShelfItem, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    BoxWithConstraints(modifier, contentAlignment = Alignment.BottomCenter) {
        val sleeve = minOf(maxWidth, maxHeight) * 0.86f
        Canvas(Modifier.align(Alignment.TopCenter).size(sleeve * 0.89f)) {
            val radius = size.minDimension / 2
            drawCircle(scheme.inverseSurface, radius)
            for (fraction in listOf(0.80f, 0.88f, 0.95f)) {
                drawCircle(
                    scheme.inverseOnSurface.copy(alpha = 0.28f),
                    radius * fraction,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.5.dp.toPx()),
                )
            }
        }
        val shape = RoundedCornerShape(3.dp)
        Box(
            Modifier.size(sleeve).clip(shape).background(scheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (item.thumbnailUrl.isNullOrBlank()) {
                Icon(Icons.Rounded.Album, contentDescription = null, tint = scheme.onSurfaceVariant)
            } else {
                AsyncImage(
                    item.thumbnailUrl.artworkAt(320), contentDescription = null,
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

/**
 * Thumbnail content: either dedicated vector icons for standard collections
 * (Liked Songs, Watch Later, Downloads, Local Music) or album/playlist art via AsyncImage.
 */
@Composable
private fun ThumbnailContent(item: ShelfItem) {
    val specialTile = libraryTileStyle(item)
    val isWatchLater = item.browseId == "WL" || item.title.equals("Watch Later", ignoreCase = true)
    val isPlaylist = item.browseId?.startsWith("local:playlist") == true ||
        item.browseId?.startsWith("VL") == true ||
        item.browseId?.startsWith("PL") == true ||
        item.subtitle.contains("playlist", ignoreCase = true)

    when {
        specialTile != null -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(specialTile.colors)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = specialTile.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        isWatchLater -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF4A4E69), Color(0xFF22223B)),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = BitChordIcons.Clock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        !item.thumbnailUrl.isNullOrBlank() -> {
            AsyncImage(
                model = item.thumbnailUrl.artworkAt(240),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        else -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPlaylist) BitChordIcons.Playlist else Icons.Rounded.LibraryMusic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}

/**
 * Action row for creating a new playlist with YouTube-style folder peek styling.
 */
@Composable
private fun NewPlaylistActionRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = PAGE_GUTTER, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(width = 84.dp, height = 54.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .height(46.dp)
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                    .border(
                        width = 0.5.dp,
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                    ),
            )
            Box(
                modifier = Modifier
                    .size(width = 84.dp, height = 50.dp)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = 0.5.dp,
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = BitChordIcons.Plus,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(Res.string.new_playlist),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = stringResource(Res.string.saved_to_youtube_music),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Action row for Spotify playlist import.
 */
@Composable
private fun ImportSpotifyActionRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = PAGE_GUTTER, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(width = 84.dp, height = 54.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .height(46.dp)
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                    .border(
                        width = 0.5.dp,
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                    ),
            )
            Box(
                modifier = Modifier
                    .size(width = 84.dp, height = 50.dp)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = 0.5.dp,
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.spotify_logo),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(Res.string.import_spotify),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = stringResource(Res.string.import_spotify_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Direct entry row for connected Spotify playlists.
 */
@Composable
private fun SpotifyPlaylistsActionRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isConnected = runCatching { com.music.bitchord.data.spotify.SpotifyLibrary.isConnected() }.getOrDefault(false)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = PAGE_GUTTER, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(width = 84.dp, height = 54.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .height(46.dp)
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                    .border(
                        width = 0.5.dp,
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                    ),
            )
            Box(
                modifier = Modifier
                    .size(width = 84.dp, height = 50.dp)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = 0.5.dp,
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.spotify_logo),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Spotify Playlists",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = if (isConnected) "Your connected Spotify playlists" else "Connect Spotify",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Determines whether a shelf item represents an artist or channel. */
private fun isArtistItem(item: ShelfItem): Boolean {
    val type = libraryArtworkType(item)
    if (type != null && type != BrowseType.OTHER) return type == BrowseType.ARTIST
    val sub = item.subtitle
    return sub.equals("Artist", ignoreCase = true) ||
        sub.contains("subscriber", ignoreCase = true) ||
        sub.contains("Subscribers", ignoreCase = true)
}

/** One of the Library's folder rows: the page [item] opens, behind [icon]. */
/** [logo], when set, is drawn in place of [icon] — a service's own mark. */
data class LibraryLink(val item: ShelfItem, val icon: ImageVector, val logo: DrawableResource? = null)

/**
 * A Library shelf: a sideways-scrolling row of [SHELF_CARD_WIDTH] cards, retained for backwards
 * compatibility with any callers requiring shelf layout.
 */
@Composable
internal fun LibraryGridShelf(
    shelf: HomeShelf,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: (ShelfItem) -> Unit,
    onShowAll: () -> Unit,
    leadingCard: (@Composable () -> Unit)? = null,
    pinnedPlaylists: List<String> = emptyList(),
) {
    val visibleItems = remember(shelf.items) { shelf.items.take(5) }
    val keys = rememberShelfKeys(visibleItems)
    val pinned = remember(pinnedPlaylists) { pinnedPlaylists.toHashSet() }
    Column(Modifier.padding(bottom = 26.dp)) {
        SectionHeader(
            title = shelf.title,
            subtitle = shelf.subtitle,
            onShowAll = onShowAll.takeIf { shelf.items.size > 5 },
        )
        ShelfRow(
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(LIBRARY_GRID_SPACING),
        ) {
            leadingCard?.let { card ->
                item(key = "leading", contentType = LazyContent.LEADING_CARD) { card() }
            }
            itemsIndexed(
                visibleItems,
                key = { index, _ -> keys[index] },
                contentType = { _, _ -> LazyContent.CARD },
            ) { _, item ->
                ShelfCard(
                    item = item,
                    onClick = { onItemClick(item) },
                    onLongPress = { onItemLongPress(item) },
                    isPinned = item.browseId != null && item.browseId in pinned,
                )
            }
        }
    }
}

/**
 * Everything a Library shelf's "Show all" opens onto — supports grid and vertical list view modes.
 */
@Composable
fun LibraryGridPage(
    shelf: HomeShelf,
    gridState: LazyGridState,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: (ShelfItem) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onNewPlaylist: (() -> Unit)? = null,
    richLibraryCards: Boolean = false,
) {
    val pinnedPlaylists by AppUi.host.pinnedPlaylists.collectAsStateWithLifecycle()
    val librarySort by AppUi.host.librarySort.collectAsStateWithLifecycle()
    val libraryViewMode by AppUi.host.libraryViewMode.collectAsStateWithLifecycle()

    val sortedShelf = remember(shelf, pinnedPlaylists, librarySort) {
        shelf.copy(items = shelf.items.withCloudDateOrder(librarySort))
            .sortedForLibrary(librarySort).pinnedFirst(pinnedPlaylists)
    }
    val pinned = remember(pinnedPlaylists) { pinnedPlaylists.toHashSet() }
    val keys = rememberShelfKeys(sortedShelf.items)
    val listState = rememberLazyListState()

    LaunchedEffect(libraryViewMode) {
        val totalCount = sortedShelf.items.size + if (onNewPlaylist != null) 1 else 0
        if (totalCount > 0) {
            if (libraryViewMode == LibraryViewMode.LIST) {
                val target = gridState.firstVisibleItemIndex.coerceIn(0, totalCount - 1)
                listState.scrollToItem(target)
            } else {
                val target = listState.firstVisibleItemIndex.coerceIn(0, totalCount - 1)
                gridState.scrollToItem(target)
            }
        }
    }

    when (libraryViewMode) {
        LibraryViewMode.GRID -> {
            BoxWithConstraints(modifier.fillMaxSize()) {
                val available = maxWidth - PAGE_GUTTER * 2
                val columns = if (richLibraryCards) compactLibraryColumns(available, LocalDensity.current.fontScale)
                    else libraryGrid(available).columns
                FastScroller(
                    gridState = gridState,
                    contentPadding = contentPadding,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        state = gridState,
                        contentPadding = contentPadding,
                        horizontalArrangement = Arrangement.spacedBy(LIBRARY_GRID_SPACING),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.padding(horizontal = PAGE_GUTTER),
                    ) {
                        if (onNewPlaylist != null) {
                            item(key = "leading", contentType = LazyContent.LEADING_CARD) {
                                NewShelfCard(
                                    icon = BitChordIcons.Plus,
                                    label = stringResource(Res.string.new_playlist),
                                    subtitle = stringResource(Res.string.saved_to_youtube_music),
                                    onClick = onNewPlaylist,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                        gridItemsIndexed(
                            sortedShelf.items,
                            key = { index, _ -> keys[index] },
                            contentType = { _, _ -> LazyContent.CARD },
                        ) { _, item ->
                            ShelfCard(
                                item = item,
                                onClick = { onItemClick(item) },
                                onLongPress = { onItemLongPress(item) },
                                modifier = Modifier.fillMaxWidth(),
                                isPinned = item.browseId != null && item.browseId in pinned,
                                artwork = libraryCardArtwork(item, richLibraryCards),
                            )
                        }
                    }
                }
            }
        }
        LibraryViewMode.LIST -> {
            FastScroller(
                listState = listState,
                contentPadding = contentPadding,
                modifier = modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    state = listState,
                    contentPadding = contentPadding,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (onNewPlaylist != null) {
                        item(key = "leading", contentType = LazyContent.LEADING_CARD) {
                            NewPlaylistActionRow(onClick = onNewPlaylist)
                        }
                    }
                    itemsIndexed(
                        sortedShelf.items,
                        key = { index, _ -> keys[index] },
                        contentType = { _, _ -> LazyContent.BROWSE_ROW },
                    ) { _, item ->
                        LibraryItemRow(
                            item = item,
                            isPinned = item.browseId != null && item.browseId in pinned,
                            isArtist = isArtistItem(item),
                            albumSleeve = richLibraryCards && libraryArtworkType(item) == BrowseType.ALBUM,
                            onClick = { onItemClick(item) },
                            onLongPress = { onItemLongPress(item) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Moves whichever of this shelf's cards are in [pinned] to the front, in the order they were pinned.
 */
private fun HomeShelf.pinnedFirst(pinned: List<String>): HomeShelf {
    if (pinned.isEmpty()) return this
    val byId = items.filter { it.browseId != null }.associateBy { it.browseId }
    val pinnedItems = pinned.mapNotNull { byId[it] }
    if (pinnedItems.isEmpty()) return this
    val pinnedSet = pinnedItems.toSet()
    return copy(items = pinnedItems + items.filter { it !in pinnedSet })
}

/**
 * Sorts shelf items alphabetically when requested.
 */
private fun HomeShelf.sortedForLibrary(sort: LibrarySort): HomeShelf = when (sort) {
    LibrarySort.DEFAULT, LibrarySort.DATE_NEWEST, LibrarySort.DATE_OLDEST -> this
    LibrarySort.TITLE_ASC -> copy(items = items.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }))
    LibrarySort.TITLE_DESC -> copy(
        items = items.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.title }),
    )
}

/** The library feed whose cards are the account's own. */
private const val PLAYLISTS = YtMusicRepository.PLAYLISTS_SHELF
