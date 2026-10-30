package com.music.bitchord.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.bitchord.data.model.CARD_ART_PX
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.Song
import com.music.bitchord.ui.screens.CompactTrackRow
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.data.settings.SpeedDialLayoutMode
import com.music.bitchord.ui.icons.BitChordIcons
import kotlinx.coroutines.launch

/**
 * Section header for Speed Dial with title and 3-way layout switcher button.
 */
@Composable
fun SpeedDialSectionHeader(
    layoutMode: SpeedDialLayoutMode,
    onToggleLayoutMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_GUTTER, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Speed Dial",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        IconButton(
            onClick = onToggleLayoutMode,
            modifier = Modifier.size(36.dp),
        ) {
            val icon = when (layoutMode) {
                SpeedDialLayoutMode.TWO_BY_THREE -> BitChordIcons.GridView
                SpeedDialLayoutMode.THREE_BY_THREE -> Icons.Rounded.Apps
                SpeedDialLayoutMode.ONE_BY_FOUR_LIST -> BitChordIcons.ListView
            }
            Icon(
                imageVector = icon,
                contentDescription = "Switch layout: ${layoutMode.label}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Adaptive Speed Dial Grid for ChordPlay Living Home Screen.
 *
 * Supports 3 layout modes:
 * - TWO_BY_THREE: 2-column by 3-row grid per page (Slot 5 on Page 1 is Lucky/Roll a Dice card)
 * - THREE_BY_THREE: 3-column by 3-row grid per page (Slot 8 on Page 1 is Lucky/Roll a Dice card)
 * - COMPACT_CAROUSEL: Horizontal scrolling carousel
 */
@Composable
fun HomeSpeedDial(
    items: List<ShelfItem>,
    pinnedIds: Set<String>,
    currentSong: Song?,
    isPlaying: Boolean,
    onItemClick: (ShelfItem) -> Unit,
    onLuckyDialClick: () -> Unit,
    modifier: Modifier = Modifier,
    layoutMode: SpeedDialLayoutMode = SpeedDialLayoutMode.TWO_BY_THREE,
    onItemLongPress: ((ShelfItem) -> Unit)? = null,
) {
    if (items.isEmpty()) return

    when (layoutMode) {
        SpeedDialLayoutMode.TWO_BY_THREE -> {
            HomeSpeedDialTwoByThree(
                items = items,
                pinnedIds = pinnedIds,
                currentSong = currentSong,
                isPlaying = isPlaying,
                onItemClick = onItemClick,
                onLuckyDialClick = onLuckyDialClick,
                onItemLongPress = onItemLongPress,
                modifier = modifier,
            )
        }
        SpeedDialLayoutMode.THREE_BY_THREE -> {
            HomeSpeedDialThreeByThree(
                items = items,
                pinnedIds = pinnedIds,
                currentSong = currentSong,
                isPlaying = isPlaying,
                onItemClick = onItemClick,
                onLuckyDialClick = onLuckyDialClick,
                onItemLongPress = onItemLongPress,
                modifier = modifier,
            )
        }
        SpeedDialLayoutMode.ONE_BY_FOUR_LIST -> {
            HomeSpeedDialList(
                items = items,
                pinnedIds = pinnedIds,
                currentSong = currentSong,
                isPlaying = isPlaying,
                onItemClick = onItemClick,
                onLuckyDialClick = onLuckyDialClick,
                onItemLongPress = onItemLongPress,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun HomeSpeedDialTwoByThree(
    items: List<ShelfItem>,
    pinnedIds: Set<String>,
    currentSong: Song?,
    isPlaying: Boolean,
    onItemClick: (ShelfItem) -> Unit,
    onLuckyDialClick: () -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    // Page 1 gets up to 5 items + Lucky Dial in slot 5
    val page1Items = items.take(5)
    val remainingItems = items.drop(5)
    val additionalPages = if (remainingItems.isNotEmpty()) remainingItems.chunked(6) else emptyList()
    val totalPages = 1 + additionalPages.size

    val pagerState = rememberPagerState(pageCount = { totalPages })

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_GUTTER, vertical = 6.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
            ) { pageIndex ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (pageIndex == 0) {
                        // Page 1: 5 item slots + Lucky Dial in slot 5 (row 3, col 2)
                        SpeedDialRow(
                            item1 = page1Items.getOrNull(0),
                            item2 = page1Items.getOrNull(1),
                            pinnedIds = pinnedIds,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            onItemClick = onItemClick,
                            onItemLongPress = onItemLongPress,
                        )
                        SpeedDialRow(
                            item1 = page1Items.getOrNull(2),
                            item2 = page1Items.getOrNull(3),
                            pinnedIds = pinnedIds,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            onItemClick = onItemClick,
                            onItemLongPress = onItemLongPress,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val item4 = page1Items.getOrNull(4)
                            if (item4 != null) {
                                SpeedDialTile(
                                    item = item4,
                                    isPinned = (item4.videoId in pinnedIds || item4.browseId in pinnedIds),
                                    isCurrent = isItemCurrent(item4, currentSong),
                                    isPlaying = isPlaying,
                                    isCompact = false,
                                    onItemClick = onItemClick,
                                    onItemLongPress = onItemLongPress,
                                    modifier = Modifier.weight(1f),
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }

                            LuckyDialCard(
                                onClick = onLuckyDialClick,
                                isCompact = false,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        // Subsequent pages: up to 6 items chunked by 2
                        val pageItems = additionalPages.getOrNull(pageIndex - 1).orEmpty()
                        val rows = pageItems.chunked(2)
                        rows.forEach { rowItems ->
                            SpeedDialRow(
                                item1 = rowItems.getOrNull(0),
                                item2 = rowItems.getOrNull(1),
                                pinnedIds = pinnedIds,
                                currentSong = currentSong,
                                isPlaying = isPlaying,
                                onItemClick = onItemClick,
                                onItemLongPress = onItemLongPress,
                            )
                        }
                        repeat(3 - rows.size) {
                            Spacer(modifier = Modifier.height(56.dp))
                        }
                    }
                }
            }

            // Floating Left Quick-Return Button
            androidx.compose.animation.AnimatedVisibility(
                visible = pagerState.currentPage > 0,
                enter = fadeIn(tween(200)) + slideInHorizontally(tween(200)) { -it },
                exit = fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { -it },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 4.dp),
            ) {
                val coroutineScope = rememberCoroutineScope()
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape)
                        .clickable {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(
                                    page = 0,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMediumLow,
                                    ),
                                )
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = BitChordIcons.ChevronLeft,
                        contentDescription = "Return to First Page",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        // Horizontal Page Indicator dots if more than 1 page
        if (totalPages > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(totalPages) { pageIdx ->
                    val isSelected = pagerState.currentPage == pageIdx
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (isSelected) 6.dp else 4.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeSpeedDialThreeByThree(
    items: List<ShelfItem>,
    pinnedIds: Set<String>,
    currentSong: Song?,
    isPlaying: Boolean,
    onItemClick: (ShelfItem) -> Unit,
    onLuckyDialClick: () -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    // Page 1 gets up to 8 items + Lucky Dial in slot 8 (row 3, col 3)
    val page1Items = items.take(8)
    val remainingItems = items.drop(8)
    val additionalPages = if (remainingItems.isNotEmpty()) remainingItems.chunked(9) else emptyList()
    val totalPages = 1 + additionalPages.size

    val pagerState = rememberPagerState(pageCount = { totalPages })

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_GUTTER, vertical = 6.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
            ) { pageIndex ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (pageIndex == 0) {
                        // Row 1: items 0, 1, 2
                        SpeedDialRow3(
                            item1 = page1Items.getOrNull(0),
                            item2 = page1Items.getOrNull(1),
                            item3 = page1Items.getOrNull(2),
                            pinnedIds = pinnedIds,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            onItemClick = onItemClick,
                            onItemLongPress = onItemLongPress,
                        )
                        // Row 2: items 3, 4, 5
                        SpeedDialRow3(
                            item1 = page1Items.getOrNull(3),
                            item2 = page1Items.getOrNull(4),
                            item3 = page1Items.getOrNull(5),
                            pinnedIds = pinnedIds,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            onItemClick = onItemClick,
                            onItemLongPress = onItemLongPress,
                        )
                        // Row 3: items 6, 7 + Lucky Dial in slot 8
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            val item6 = page1Items.getOrNull(6)
                            if (item6 != null) {
                                SpeedDialTile(
                                    item = item6,
                                    isPinned = (item6.videoId in pinnedIds || item6.browseId in pinnedIds),
                                    isCurrent = isItemCurrent(item6, currentSong),
                                    isPlaying = isPlaying,
                                    isCompact = true,
                                    onItemClick = onItemClick,
                                    onItemLongPress = onItemLongPress,
                                    modifier = Modifier.weight(1f),
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }

                            val item7 = page1Items.getOrNull(7)
                            if (item7 != null) {
                                SpeedDialTile(
                                    item = item7,
                                    isPinned = (item7.videoId in pinnedIds || item7.browseId in pinnedIds),
                                    isCurrent = isItemCurrent(item7, currentSong),
                                    isPlaying = isPlaying,
                                    isCompact = true,
                                    onItemClick = onItemClick,
                                    onItemLongPress = onItemLongPress,
                                    modifier = Modifier.weight(1f),
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }

                            LuckyDialCard(
                                onClick = onLuckyDialClick,
                                isCompact = true,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        val pageItems = additionalPages.getOrNull(pageIndex - 1).orEmpty()
                        val rows = pageItems.chunked(3)
                        rows.forEach { rowItems ->
                            SpeedDialRow3(
                                item1 = rowItems.getOrNull(0),
                                item2 = rowItems.getOrNull(1),
                                item3 = rowItems.getOrNull(2),
                                pinnedIds = pinnedIds,
                                currentSong = currentSong,
                                isPlaying = isPlaying,
                                onItemClick = onItemClick,
                                onItemLongPress = onItemLongPress,
                            )
                        }
                        repeat(3 - rows.size) {
                            Spacer(modifier = Modifier.height(52.dp))
                        }
                    }
                }
            }

            // Floating Left Quick-Return Button
            androidx.compose.animation.AnimatedVisibility(
                visible = pagerState.currentPage > 0,
                enter = fadeIn(tween(200)) + slideInHorizontally(tween(200)) { -it },
                exit = fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { -it },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 4.dp),
            ) {
                val coroutineScope = rememberCoroutineScope()
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape)
                        .clickable {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(
                                    page = 0,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMediumLow,
                                    ),
                                )
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = BitChordIcons.ChevronLeft,
                        contentDescription = "Return to First Page",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        // Horizontal Page Indicator dots if more than 1 page
        if (totalPages > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(totalPages) { pageIdx ->
                    val isSelected = pagerState.currentPage == pageIdx
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (isSelected) 6.dp else 4.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeSpeedDialList(
    items: List<ShelfItem>,
    pinnedIds: Set<String>,
    currentSong: Song?,
    isPlaying: Boolean,
    onItemClick: (ShelfItem) -> Unit,
    onLuckyDialClick: () -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val columnWidth = trackColumnWidth(maxWidth)
        ShelfRow(
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val allListItems = items.take(16)
            val columns = allListItems.chunked(4)
            items(columns) { column ->
                Column(Modifier.width(columnWidth)) {
                    column.forEach { item ->
                        val isCurrent = isItemCurrent(item, currentSong)
                        CompactTrackRow(
                            title = item.title,
                            subtitle = cleanTypeSubtitle(item.subtitle, item.detectType()),
                            thumbnailUrl = item.thumbnailUrl,
                            onClick = { onItemClick(item) },
                            onLongPress = onItemLongPress?.let { { it(item) } },
                            isCurrent = isCurrent,
                            isPlaying = isCurrent && isPlaying,
                        )
                    }
                }
            }
            item(key = "speed_dial_list_lucky") {
                Column(
                    modifier = Modifier
                        .width(columnWidth)
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    LuckyDialCard(
                        onClick = onLuckyDialClick,
                        isCompact = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

private fun isItemCurrent(item: ShelfItem, currentSong: Song?): Boolean {
    if (currentSong == null) return false
    if (item.videoId != null && item.videoId == currentSong.videoId) return true
    return currentSong.title.isNotBlank() && item.title == currentSong.title &&
        (item.subtitle == currentSong.artist || item.subtitle.isBlank())
}

@Composable
private fun SpeedDialRow(
    item1: ShelfItem?,
    item2: ShelfItem?,
    pinnedIds: Set<String>,
    currentSong: Song?,
    isPlaying: Boolean,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
) {
    if (item1 == null && item2 == null) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (item1 != null) {
            SpeedDialTile(
                item = item1,
                isPinned = (item1.videoId in pinnedIds || item1.browseId in pinnedIds),
                isCurrent = isItemCurrent(item1, currentSong),
                isPlaying = isPlaying,
                isCompact = false,
                onItemClick = onItemClick,
                onItemLongPress = onItemLongPress,
                modifier = Modifier.weight(1f),
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        if (item2 != null) {
            SpeedDialTile(
                item = item2,
                isPinned = (item2.videoId in pinnedIds || item2.browseId in pinnedIds),
                isCurrent = isItemCurrent(item2, currentSong),
                isPlaying = isPlaying,
                isCompact = false,
                onItemClick = onItemClick,
                onItemLongPress = onItemLongPress,
                modifier = Modifier.weight(1f),
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun SpeedDialRow3(
    item1: ShelfItem?,
    item2: ShelfItem?,
    item3: ShelfItem?,
    pinnedIds: Set<String>,
    currentSong: Song?,
    isPlaying: Boolean,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
) {
    if (item1 == null && item2 == null && item3 == null) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        listOf(item1, item2, item3).forEach { item ->
            if (item != null) {
                SpeedDialTile(
                    item = item,
                    isPinned = (item.videoId in pinnedIds || item.browseId in pinnedIds),
                    isCurrent = isItemCurrent(item, currentSong),
                    isPlaying = isPlaying,
                    isCompact = true,
                    onItemClick = onItemClick,
                    onItemLongPress = onItemLongPress,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

private enum class SpeedDialItemType {
    SONG,
    ALBUM,
    PLAYLIST,
    ARTIST,
}

private fun ShelfItem.detectType(): SpeedDialItemType {
    val bId = browseId
    val vId = videoId
    val sub = subtitle.lowercase()
    return when {
        bId?.startsWith("UC") == true || sub.contains("artist") -> SpeedDialItemType.ARTIST
        bId?.startsWith("VL") == true || bId?.startsWith("PL") == true || bId?.startsWith("RD") == true || sub.contains("playlist") -> SpeedDialItemType.PLAYLIST
        bId?.startsWith("MPREb_") == true || bId?.startsWith("OLAK") == true || sub.contains("album") || sub.contains("single") || sub.contains("ep") -> SpeedDialItemType.ALBUM
        vId != null || sub.startsWith("song") -> SpeedDialItemType.SONG
        bId != null -> SpeedDialItemType.ALBUM
        else -> SpeedDialItemType.SONG
    }
}

private fun cleanTypeSubtitle(subtitle: String, type: SpeedDialItemType): String {
    if (subtitle.isBlank()) return ""
    return when (type) {
        SpeedDialItemType.SONG -> subtitle.removePrefix("Song • ").trim()
        SpeedDialItemType.ALBUM -> subtitle.removePrefix("Album • ").removePrefix("Single • ").removePrefix("EP • ").trim()
        SpeedDialItemType.PLAYLIST -> subtitle.removePrefix("Playlist • ").trim()
        SpeedDialItemType.ARTIST -> subtitle.removePrefix("Artist • ").trim()
    }
}

@Composable
private fun SpeedDialTile(
    item: ShelfItem,
    isPinned: Boolean,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val type = item.detectType()
    val shape = RoundedCornerShape(12.dp)
    val clickModifier = if (onItemLongPress != null) {
        Modifier
            .longPressMenuClickable(
                onClick = { onItemClick(item) },
                onLongClick = { onItemLongPress(item) },
            )
            .contextClick { onItemLongPress(item) }
    } else {
        Modifier.clickable { onItemClick(item) }
    }

    val tileBg = when (type) {
        SpeedDialItemType.PLAYLIST -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        SpeedDialItemType.ALBUM -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.60f)
        SpeedDialItemType.ARTIST -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        SpeedDialItemType.SONG -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
    }

    val tileHeight = if (isCompact) 52.dp else 56.dp
    val artworkContainerSize = if (isCompact) 52.dp else 56.dp

    Box(
        modifier = modifier
            .height(tileHeight)
            .clip(shape)
            .background(tileBg)
            .border(1.dp, Color.White.copy(alpha = if (isCurrent) 0.25f else 0.08f), shape)
            .then(clickModifier),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Artwork container with distinct treatment per type
            Box(
                modifier = Modifier
                    .size(artworkContainerSize)
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                when (type) {
                    SpeedDialItemType.ARTIST -> {
                        // Circular avatar with subtle primary glow ring
                        Box(
                            modifier = Modifier
                                .size(if (isCompact) 40.dp else 46.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.50f), CircleShape),
                        ) {
                            AsyncImage(
                                model = item.thumbnailUrl.artworkAt(CARD_ART_PX),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                    SpeedDialItemType.PLAYLIST -> {
                        // Stacked folder sleeve effect (peek card behind + front card) with playlist micro-badge
                        val frontSize = if (isCompact) 40.dp else 46.dp
                        val backW = if (isCompact) 38.dp else 44.dp
                        val backH = if (isCompact) 42.dp else 48.dp
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            // Subtle stacked back card peek
                            Box(
                                modifier = Modifier
                                    .size(width = backW, height = backH)
                                    .offset(y = (-3).dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.50f), RoundedCornerShape(6.dp)),
                            )
                            // Front card
                            Box(
                                modifier = Modifier
                                    .size(frontSize)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                            ) {
                                AsyncImage(
                                    model = item.thumbnailUrl.artworkAt(CARD_ART_PX),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                // Playlist micro-badge
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color.Black.copy(alpha = 0.70f))
                                        .padding(2.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                        contentDescription = "Playlist",
                                        tint = Color.White.copy(alpha = 0.90f),
                                        modifier = Modifier.size(if (isCompact) 8.dp else 10.dp),
                                    )
                                }
                            }
                        }
                    }
                    SpeedDialItemType.ALBUM -> {
                        // Vinyl disc peeking from sleeve with edge groove highlight
                        val jacketSize = if (isCompact) 40.dp else 46.dp
                        val vinylSize = if (isCompact) 38.dp else 44.dp
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            // Peeking vinyl record disc
                            Box(
                                modifier = Modifier
                                    .padding(start = if (isCompact) 8.dp else 10.dp)
                                    .size(vinylSize)
                                    .clip(CircleShape)
                                    .background(Color(0xFF141414))
                                    .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(if (isCompact) 20.dp else 24.dp)
                                        .border(0.75.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                                )
                                Box(
                                    modifier = Modifier
                                        .size(if (isCompact) 10.dp else 12.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)),
                                )
                            }
                            // Front album jacket
                            Box(
                                modifier = Modifier
                                    .padding(start = 3.dp)
                                    .size(jacketSize)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp)),
                            ) {
                                AsyncImage(
                                    model = item.thumbnailUrl.artworkAt(CARD_ART_PX),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                    SpeedDialItemType.SONG -> {
                        // Clean rounded squircle
                        Box(
                            modifier = Modifier
                                .size(if (isCompact) 40.dp else 46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(10.dp)),
                        ) {
                            AsyncImage(
                                model = item.thumbnailUrl.artworkAt(CARD_ART_PX),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }

            // Title and Subtitle (without text tags)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = if (isCompact) 6.dp else 9.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = item.title,
                    style = if (isCompact) MaterialTheme.typography.titleSmall.copy(fontSize = 12.sp) else MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isCurrent) PlayingAccent else MaterialTheme.colorScheme.onSurface,
                )

                val cleanSubtitle = cleanTypeSubtitle(item.subtitle, type)
                if (cleanSubtitle.isNotBlank()) {
                    Text(
                        text = cleanSubtitle,
                        style = if (isCompact) MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp) else MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (isCurrent && isPlaying) {
                SearchPlayingBars(modifier = Modifier.padding(end = if (isCompact) 6.dp else 10.dp))
            }
        }

        if (isPinned) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.PushPin,
                    contentDescription = "Pinned",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(10.dp),
                )
            }
        }
    }
}

@Composable
private fun LuckyDialCard(
    onClick: () -> Unit,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    val isLightPrimary = MaterialTheme.colorScheme.primary.luminance() > 0.5f
    val contentColor = if (isLightPrimary) Color(0xFF0F080B) else Color.White
    val subtitleColor = if (isLightPrimary) Color(0xFF0F080B).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.85f)
    val iconBoxBg = if (isLightPrimary) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.15f)
    val borderColor = if (isLightPrimary) Color.Black.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.25f)

    Box(
        modifier = modifier
            .height(if (isCompact) 52.dp else 56.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.90f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.80f),
                    ),
                ),
            )
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(if (isCompact) 52.dp else 56.dp)
                    .background(iconBoxBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Casino,
                    contentDescription = "Roll a Dice",
                    tint = contentColor,
                    modifier = Modifier.size(if (isCompact) 20.dp else 24.dp),
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = if (isCompact) 6.dp else 10.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Roll a Dice",
                    style = if (isCompact) MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold) else MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = contentColor,
                )
                Text(
                    text = "Instant discovery mix",
                    style = if (isCompact) MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp) else MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = subtitleColor,
                )
            }
        }
    }
}
