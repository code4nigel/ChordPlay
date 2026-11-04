package com.music.bitchord.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.ui.icons.BitChordIcons
import kotlinx.coroutines.yield

private val GLASS_BORDER_COLOR = Color.White.copy(alpha = 0.08f)
private val GLASS_FILL_SELECTED = Color.White.copy(alpha = 0.12f)
private val GLASS_FILL_HOVER = Color.White.copy(alpha = 0.06f)

/**
 * ChordPlay Desktop Navigation Rail (Left Pane).
 *
 * Implements ChordPlay's Liquid Glass styling, clear hierarchy for primary navigation
 * (Home, Explore, Library), instant search, and scrollable playlist collections.
 */
@Composable
internal fun DesktopNavigationRail(
    destination: DesktopDestination,
    settingsOpen: Boolean,
    accountPlaylists: List<ShelfItem>,
    localPlaylists: List<DesktopPlaylist>,
    openedCollectionId: String?,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onDestinationSelected: (DesktopDestination) -> Unit,
    onOpenAccountPlaylist: (ShelfItem) -> Unit,
    onOpenLocalPlaylist: (DesktopPlaylist) -> Unit,
    onOpenSettings: () -> Unit,
    focusSearch: Boolean,
    onSearchFocused: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val searchFocusRequester = remember { FocusRequester() }
    LaunchedEffect(focusSearch) {
        if (focusSearch) {
            runCatching { searchFocusRequester.requestFocus() }
            onSearchFocused()
        }
    }

    val windowInfo = LocalWindowInfo.current
    var searchFocused by remember { mutableStateOf(false) }
    var restoreSearchFocus by remember { mutableStateOf(false) }

    LaunchedEffect(windowInfo.isWindowFocused) {
        if (!windowInfo.isWindowFocused) {
            restoreSearchFocus = searchFocused
        } else if (restoreSearchFocus) {
            yield()
            searchFocusRequester.requestFocus()
            restoreSearchFocus = false
        }
    }

    val titleBarEnabled by DesktopTitleBarSetting.enabled.collectAsState()
    val inlineCaption = DesktopPlatform.drawsOwnWindowFrame && !titleBarEnabled

    Box(
        modifier
            .width(230.dp)
            .fillMaxHeight()
            .desktopWindowGlass(DesktopChromeEdge.END, fade = 0.07f),
    ) {
        Column(Modifier.fillMaxSize()) {
            if (inlineCaption) {
                DesktopTitleBarDragArea(Modifier.fillMaxWidth().height(SIDEBAR_CAPTION_HEIGHT)) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(start = if (DesktopPlatform.isMac) 16.dp else 10.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        DesktopWindowButtons()
                    }
                }
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(
                        start = 12.dp,
                        end = 12.dp,
                        top = if (inlineCaption) 4.dp else 14.dp,
                        bottom = 14.dp,
                    ),
            ) {
                // ChordPlay Branding Header
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "CHORDPLAY",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 1.8.sp,
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DesktopAccent.copy(alpha = 0.20f))
                            .border(1.dp, DesktopAccent.copy(alpha = 0.50f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text = "LOSSLESS",
                            color = DesktopAccent,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp,
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Search Field
                DesktopSearchField(
                    query = query,
                    onQueryChange = onQueryChange,
                    onSearch = onSearch,
                    focusRequester = searchFocusRequester,
                    onFocusChanged = { searchFocused = it },
                )

                Spacer(Modifier.height(14.dp))

                // Primary Navigation Items
                DesktopRailItem(
                    icon = BitChordIcons.Home,
                    label = DesktopStrings["home", "Home"],
                    selected = destination == DesktopDestination.LISTEN_NOW && !settingsOpen,
                    onClick = { onDestinationSelected(DesktopDestination.LISTEN_NOW) },
                )
                DesktopRailItem(
                    icon = BitChordIcons.Explore,
                    label = "Explore",
                    selected = destination == DesktopDestination.EXPLORE && !settingsOpen,
                    onClick = { onDestinationSelected(DesktopDestination.EXPLORE) },
                )
                DesktopRailItem(
                    icon = BitChordIcons.Library,
                    label = "Library",
                    selected = destination == DesktopDestination.LIBRARY && !settingsOpen,
                    onClick = { onDestinationSelected(DesktopDestination.LIBRARY) },
                )
                DesktopRailItem(
                    icon = BitChordIcons.Search,
                    label = "Search",
                    selected = destination == DesktopDestination.SEARCH && !settingsOpen,
                    onClick = { onDestinationSelected(DesktopDestination.SEARCH) },
                )

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = GLASS_BORDER_COLOR)
                Spacer(Modifier.height(8.dp))

                // Collections
                DesktopRailItem(
                    icon = BitChordIcons.Clock,
                    label = "History",
                    selected = destination == DesktopDestination.HISTORY && !settingsOpen,
                    onClick = { onDestinationSelected(DesktopDestination.HISTORY) },
                )

                val queuedDownloads by DesktopDownloadQueue.active.collectAsState()
                DesktopRailItem(
                    icon = BitChordIcons.Download,
                    label = if (queuedDownloads.isEmpty()) "Downloads" else "Downloads (${queuedDownloads.size})",
                    selected = destination == DesktopDestination.DOWNLOADS && !settingsOpen,
                    onClick = { onDestinationSelected(DesktopDestination.DOWNLOADS) },
                )

                DesktopRailItem(
                    icon = BitChordIcons.Library,
                    label = "Local Music",
                    selected = destination == DesktopDestination.LOCAL_MUSIC && !settingsOpen,
                    onClick = { onDestinationSelected(DesktopDestination.LOCAL_MUSIC) },
                )

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = GLASS_BORDER_COLOR)
                Spacer(Modifier.height(8.dp))

                // Playlists Section
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "PLAYLISTS",
                        color = DesktopSecondary.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.weight(1f),
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(bottom = 6.dp),
                ) {
                    items(accountPlaylists, key = { "account:${it.browseId}" }) { playlist ->
                        DesktopRailItem(
                            icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                            label = playlist.title,
                            selected = openedCollectionId == playlist.browseId && !settingsOpen,
                            onClick = { onOpenAccountPlaylist(playlist) },
                        )
                    }
                    items(localPlaylists, key = { "local:${it.id}" }) { playlist ->
                        DesktopRailItem(
                            icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                            label = playlist.title,
                            selected = openedCollectionId == playlist.id && !settingsOpen,
                            onClick = { onOpenLocalPlaylist(playlist) },
                        )
                    }
                    if (accountPlaylists.isEmpty() && localPlaylists.isEmpty()) {
                        item {
                            Text(
                                "Your playlists will appear here",
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
                                color = DesktopSecondary.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }

                HorizontalDivider(color = GLASS_BORDER_COLOR)
                Spacer(Modifier.height(8.dp))

                // Settings
                DesktopRailItem(
                    icon = Icons.Rounded.Settings,
                    label = "Settings",
                    selected = settingsOpen,
                    onClick = onOpenSettings,
                )
            }
        }

        // Tactile Liquid Glass vertical edge
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .width(1.dp)
                .fillMaxHeight()
                .background(GLASS_BORDER_COLOR),
        )
    }
}

@Composable
private fun DesktopRailItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) GLASS_FILL_SELECTED else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (selected) GLASS_BORDER_COLOR else Color.Transparent,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) DesktopAccent else DesktopSecondary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            color = if (selected) Color.White else DesktopSecondary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
