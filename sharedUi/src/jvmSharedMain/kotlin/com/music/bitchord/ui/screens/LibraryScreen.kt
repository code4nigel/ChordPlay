package com.music.bitchord.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.LibraryPage
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.UiState
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.data.settings.LibrarySort
import com.music.bitchord.data.settings.LibraryViewMode
import com.music.bitchord.sharedui.resources.Res
import com.music.bitchord.sharedui.resources.albums
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
import com.music.bitchord.ui.components.MessageState
import com.music.bitchord.ui.components.PAGE_GUTTER
import com.music.bitchord.ui.components.PullToRefresh
import com.music.bitchord.ui.components.SHELF_CARD_WIDTH
import com.music.bitchord.ui.components.ShelfRow
import com.music.bitchord.ui.components.contextClick
import com.music.bitchord.ui.components.libraryGrid
import com.music.bitchord.ui.components.librarySkeleton
import com.music.bitchord.ui.components.longPressMenuClickable
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
) {
    val pinnedPlaylists by AppUi.host.pinnedPlaylists.collectAsStateWithLifecycle()
    val librarySort by AppUi.host.librarySort.collectAsStateWithLifecycle()
    val libraryViewMode by AppUi.host.libraryViewMode.collectAsStateWithLifecycle()
    val downloadedLabel = stringResource(Res.string.downloaded)
    var selectedFilter by rememberSaveable { mutableStateOf(LibraryFilter.ALL) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(selectedFilter) {
        listState.scrollToItem(0)
    }

    PullToRefresh(
        refreshing = refreshing,
        onRefresh = onRefresh,
        state = pullState,
        modifier = modifier,
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
                    item(key = "title") {
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
                    item(key = "replay") { replay() }
                }

                // 2. Search Bar with real-time text filtering, view mode switcher, and sorting
                item(key = "search_field") {
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
                item(key = "filter_chips") {
                    LibraryFilterChips(
                        selectedFilter = selectedFilter,
                        onFilterSelect = { selectedFilter = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                    )
                }

                if (!signedIn) {
                    when (selectedFilter) {
                        LibraryFilter.DOWNLOADS -> {
                            val downloadItems = (links.map { it.item } + deviceItems)
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
                                    items = downloadItems,
                                    viewMode = libraryViewMode,
                                    pinnedPlaylists = pinnedPlaylists.toSet(),
                                    onShelfItemClick = onShelfItemClick,
                                    onShelfItemLongPress = onShelfItemLongPress,
                                )
                            }
                        }
                        LibraryFilter.PLAYLISTS -> {
                            if (searchQuery.isBlank()) {
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
                            item(key = "sign_in_prompt") {
                                MessageState(
                                    message = stringResource(Res.string.library_sign_in_description),
                                    actionLabel = stringResource(Res.string.sign_in),
                                    onAction = onSignIn,
                                )
                            }
                        }
                        else -> {
                            if (links.isNotEmpty() && selectedFilter == LibraryFilter.ALL) {
                                val filteredLinks = links.map { it.item }
                                    .applyFilterAndSort(searchQuery, librarySort, pinnedPlaylists.toSet())
                                renderLibraryItems(
                                    items = filteredLinks,
                                    viewMode = libraryViewMode,
                                    pinnedPlaylists = pinnedPlaylists.toSet(),
                                    onShelfItemClick = onShelfItemClick,
                                    onShelfItemLongPress = onShelfItemLongPress,
                                )
                            }
                            item(key = "sign_in_prompt") {
                                MessageState(
                                    message = stringResource(Res.string.library_sign_in_description),
                                    actionLabel = stringResource(Res.string.sign_in),
                                    onAction = onSignIn,
                                )
                            }
                        }
                    }
                    return@LazyColumn
                }

                when (state) {
                    is UiState.Loading -> librarySkeleton()
                    is UiState.Error -> item {
                        MessageState(
                            state.message,
                            actionLabel = stringResource(Res.string.retry),
                            onAction = onRetry,
                        )
                    }
                    is UiState.Success -> {
                        val shelves = state.data.shelves

                        when (selectedFilter) {
                            LibraryFilter.ALL -> {
                                // Quick links: Liked Music, Downloads, Local Music
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

                                val quickItems = listOf(likedMusicItem, downloadsItem, localMusicItem)
                                val filteredQuick = if (searchQuery.isBlank()) {
                                    quickItems
                                } else {
                                    val q = searchQuery.trim()
                                    quickItems.filter { it.title.contains(q, ignoreCase = true) }
                                }

                                if (filteredQuick.isNotEmpty()) {
                                    renderLibraryItems(
                                        items = filteredQuick,
                                        viewMode = libraryViewMode,
                                        pinnedPlaylists = pinnedPlaylists.toSet(),
                                        onShelfItemClick = onShelfItemClick,
                                        onShelfItemLongPress = onShelfItemLongPress,
                                    )
                                }

                                // Recent items across all categories
                                val quickKeys = setOfNotNull("VLLM", "LM", "local:downloads", "local:all")
                                val recentItems = shelves.flatMap { shelf ->
                                    if (shelf.title == PLAYLISTS || shelf.title.equals("Playlists", ignoreCase = true)) {
                                        shelf.pinnedFirst(pinnedPlaylists).items
                                    } else {
                                        shelf.items
                                    }
                                }.filter {
                                    it.browseId !in quickKeys &&
                                        !it.title.equals("Liked Music", ignoreCase = true) &&
                                        !it.title.equals("Liked Songs", ignoreCase = true)
                                }.distinctBy { it.browseId ?: it.title }
                                    .applyFilterAndSort(searchQuery, librarySort, pinnedPlaylists.toSet())

                                renderLibraryItems(
                                    items = recentItems,
                                    viewMode = libraryViewMode,
                                    pinnedPlaylists = pinnedPlaylists.toSet(),
                                    onShelfItemClick = onShelfItemClick,
                                    onShelfItemLongPress = onShelfItemLongPress,
                                )
                            }

                            LibraryFilter.PLAYLISTS -> {
                                if (searchQuery.isBlank()) {
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
                                val playlistShelf = shelves.firstOrNull {
                                    it.title == PLAYLISTS || it.title.equals("Playlists", ignoreCase = true)
                                }
                                val playlists = playlistShelf?.pinnedFirst(pinnedPlaylists)?.items.orEmpty()
                                    .applyFilterAndSort(searchQuery, librarySort, pinnedPlaylists.toSet())

                                if (playlists.isEmpty()) {
                                    item(key = "empty_playlists") {
                                        MessageState(
                                            message = if (searchQuery.isNotBlank()) "No playlists match your search" else "No playlists yet",
                                        )
                                    }
                                } else {
                                    renderLibraryItems(
                                        items = playlists,
                                        viewMode = libraryViewMode,
                                        pinnedPlaylists = pinnedPlaylists.toSet(),
                                        onShelfItemClick = onShelfItemClick,
                                        onShelfItemLongPress = onShelfItemLongPress,
                                    )
                                }
                            }

                            LibraryFilter.ALBUMS -> {
                                val albumShelf = shelves.firstOrNull {
                                    it.title.equals("Albums", ignoreCase = true)
                                }
                                val albums = albumShelf?.items.orEmpty()
                                    .applyFilterAndSort(searchQuery, librarySort, pinnedPlaylists.toSet())

                                if (albums.isEmpty()) {
                                    item(key = "empty_albums") {
                                        MessageState(
                                            message = if (searchQuery.isNotBlank()) "No albums match your search" else "No saved albums yet",
                                        )
                                    }
                                } else {
                                    renderLibraryItems(
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
                                val artists = artistShelves.flatMap { it.items }.distinctBy { it.browseId ?: it.title }
                                    .applyFilterAndSort(searchQuery, librarySort, pinnedPlaylists.toSet())

                                if (artists.isEmpty()) {
                                    item(key = "empty_artists") {
                                        MessageState(
                                            message = if (searchQuery.isNotBlank()) "No artists match your search" else "No subscribed artists yet",
                                        )
                                    }
                                } else {
                                    renderLibraryItems(
                                        items = artists,
                                        viewMode = libraryViewMode,
                                        pinnedPlaylists = pinnedPlaylists.toSet(),
                                        onShelfItemClick = onShelfItemClick,
                                        onShelfItemLongPress = onShelfItemLongPress,
                                    )
                                }
                            }

                            LibraryFilter.DOWNLOADS -> {
                                val downloadItems = (links.map { it.item } + deviceItems)
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
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_GUTTER, vertical = 4.dp)
            .height(44.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp),
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
                    text = "Search library",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
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
                    contentDescription = "Clear search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.width(4.dp))
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable { onViewModeToggle() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (viewMode == LibraryViewMode.GRID) BitChordIcons.ListView else BitChordIcons.GridView,
                contentDescription = if (viewMode == LibraryViewMode.GRID) "Switch to list view" else "Switch to grid view",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp),
            )
        }
        Spacer(Modifier.width(2.dp))
        Box {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { sortMenuOpen = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Sort,
                    contentDescription = "Sort library",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(19.dp),
                )
            }
            DropdownMenu(
                expanded = sortMenuOpen,
                onDismissRequest = { sortMenuOpen = false },
            ) {
                LibrarySort.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        trailingIcon = if (option == sortOrder) {
                            {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = "Selected",
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        } else {
                            null
                        },
                        onClick = {
                            onSortOrderChange(option)
                            sortMenuOpen = false
                        },
                    )
                }
            }
        }
    }
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
        LibrarySort.DEFAULT -> {
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

private fun LazyListScope.renderLibraryItems(
    items: List<ShelfItem>,
    viewMode: LibraryViewMode,
    pinnedPlaylists: Set<String>,
    onShelfItemClick: (ShelfItem) -> Unit,
    onShelfItemLongPress: (ShelfItem) -> Unit,
) {
    if (viewMode == LibraryViewMode.GRID) {
        items(
            items.chunked(2),
            key = { pair -> "grid_" + (pair.firstOrNull()?.let { it.browseId ?: it.title } ?: "") },
        ) { rowItems ->
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
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    } else {
        items(items, key = { it.browseId ?: it.title }) { item ->
            val isPinned = item.browseId != null && item.browseId in pinnedPlaylists
            val isArtist = isArtistItem(item)
            LibraryItemRow(
                item = item,
                isPinned = isPinned,
                isArtist = isArtist,
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
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = PAGE_GUTTER, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(LibraryFilter.entries, key = { it.name }) { filter ->
            LibraryFilterChip(
                filter = filter,
                isSelected = filter == selectedFilter,
                onClick = { onFilterSelect(filter) },
            )
        }
    }
}

@Composable
private fun LibraryFilterChip(
    filter: LibraryFilter,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val isLightPrimary = primaryColor.luminance() > 0.5f

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) primaryColor else surfaceVariant.copy(alpha = 0.55f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "libraryFilterBackground",
    )

    val selectedContentColor = if (isLightPrimary) Color(0xFF0F080B) else MaterialTheme.colorScheme.onPrimary
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) selectedContentColor else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "libraryFilterContent",
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            if (isLightPrimary) Color.Black.copy(alpha = 0.15f) else primaryColor.copy(alpha = 0.8f)
        } else {
            Color.White.copy(alpha = 0.08f)
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "libraryFilterBorder",
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .then(
                if (isSelected) {
                    Modifier.shadow(
                        elevation = if (isLightPrimary) 4.dp else 8.dp,
                        shape = CircleShape,
                        spotColor = if (isLightPrimary) Color.Black.copy(alpha = 0.25f) else primaryColor.copy(alpha = 0.5f),
                        ambientColor = if (isLightPrimary) Color.Black.copy(alpha = 0.12f) else primaryColor.copy(alpha = 0.25f),
                    )
                } else {
                    Modifier
                },
            )
            .clip(CircleShape)
            .background(backgroundColor)
            .border(width = 1.dp, color = borderColor, shape = CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        val label = when (filter) {
            LibraryFilter.ALL -> filter.label
            LibraryFilter.PLAYLISTS -> stringResource(Res.string.playlists)
            LibraryFilter.ALBUMS -> stringResource(Res.string.albums)
            LibraryFilter.ARTISTS -> stringResource(Res.string.artists)
            LibraryFilter.DOWNLOADS -> filter.label
        }
        Text(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            letterSpacing = 0.2.sp,
        )
    }
}

/**
 * Full-width vertical list item with thumbnail, title/subtitle, pinned badge, and 3-dot overflow menu.
 */
@Composable
private fun LibraryItemRow(
    item: ShelfItem,
    isPinned: Boolean,
    isArtist: Boolean,
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
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(50.dp)
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

/**
 * Thumbnail content: either dedicated vector icons for standard collections
 * (Liked Songs, Watch Later, Downloads, Local Music) or album/playlist art via AsyncImage.
 */
@Composable
private fun ThumbnailContent(item: ShelfItem) {
    val isLikedSongs = item.browseId == "LM" || item.browseId == "VLLM" ||
        item.title.equals("Liked Music", ignoreCase = true) ||
        item.title.equals("Liked Songs", ignoreCase = true)
    val isWatchLater = item.browseId == "WL" || item.title.equals("Watch Later", ignoreCase = true)
    val isDownloads = item.browseId == "local:downloads" ||
        item.browseId?.startsWith("local:download") == true ||
        item.title.equals("Downloads", ignoreCase = true)
    val isLocalMusic = item.browseId == "local:all" ||
        item.browseId == "local:cache" ||
        item.browseId == "local:webdav" ||
        item.browseId == "local:smb" ||
        item.browseId?.startsWith("local:") == true ||
        item.title.equals("Local Music", ignoreCase = true)

    when {
        isLikedSongs -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF3358F4), Color(0xFF1D2F80)),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.ThumbUp,
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
        isDownloads -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = BitChordIcons.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        isLocalMusic -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                val icon = when (item.browseId) {
                    "local:cache" -> Icons.Rounded.Cached
                    "local:webdav" -> Icons.Rounded.Folder
                    "local:smb" -> Icons.Rounded.Storage
                    else -> Icons.Rounded.Folder
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
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
                    imageVector = Icons.Rounded.LibraryMusic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
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
    val id = item.browseId
    if (id != null && id.startsWith("UC")) return true
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
    val visibleItems = shelf.items.take(5)
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
            leadingCard?.let { card -> item(key = "leading") { card() } }
            items(visibleItems) { item ->
                ShelfCard(
                    item = item,
                    onClick = { onItemClick(item) },
                    onLongPress = { onItemLongPress(item) },
                    isPinned = item.browseId != null && item.browseId in pinnedPlaylists,
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
) {
    val pinnedPlaylists by AppUi.host.pinnedPlaylists.collectAsStateWithLifecycle()
    val librarySort by AppUi.host.librarySort.collectAsStateWithLifecycle()
    val libraryViewMode by AppUi.host.libraryViewMode.collectAsStateWithLifecycle()

    val sortedShelf = shelf.pinnedFirst(pinnedPlaylists).sortedForLibrary(librarySort)
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
                val grid = libraryGrid(maxWidth - PAGE_GUTTER * 2)
                FastScroller(
                    gridState = gridState,
                    contentPadding = contentPadding,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(grid.columns),
                        state = gridState,
                        contentPadding = contentPadding,
                        horizontalArrangement = Arrangement.spacedBy(LIBRARY_GRID_SPACING),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.padding(horizontal = PAGE_GUTTER),
                    ) {
                        if (onNewPlaylist != null) {
                            item(key = "leading") {
                                NewShelfCard(
                                    icon = BitChordIcons.Plus,
                                    label = stringResource(Res.string.new_playlist),
                                    subtitle = stringResource(Res.string.saved_to_youtube_music),
                                    onClick = onNewPlaylist,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                        items(sortedShelf.items, key = { it.browseId ?: it.title }) { item ->
                            ShelfCard(
                                item = item,
                                onClick = { onItemClick(item) },
                                onLongPress = { onItemLongPress(item) },
                                modifier = Modifier.fillMaxWidth(),
                                isPinned = item.browseId != null && item.browseId in pinnedPlaylists,
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
                        item(key = "leading") {
                            NewPlaylistActionRow(onClick = onNewPlaylist)
                        }
                    }
                    items(sortedShelf.items, key = { it.browseId ?: it.title }) { item ->
                        LibraryItemRow(
                            item = item,
                            isPinned = item.browseId != null && item.browseId in pinnedPlaylists,
                            isArtist = isArtistItem(item),
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
    LibrarySort.DEFAULT -> this
    LibrarySort.TITLE_ASC -> copy(items = items.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }))
    LibrarySort.TITLE_DESC -> copy(
        items = items.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.title }),
    )
}

/** The library feed whose cards are the account's own. */
private const val PLAYLISTS = YtMusicRepository.PLAYLISTS_SHELF
