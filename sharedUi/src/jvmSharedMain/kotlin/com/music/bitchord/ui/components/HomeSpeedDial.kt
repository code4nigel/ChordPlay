package com.music.bitchord.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.music.bitchord.sharedui.resources.Res
import com.music.bitchord.sharedui.resources.*
import com.music.bitchord.ui.theme.ArtworkPalette
import com.music.bitchord.ui.theme.rememberArtworkPalette
import org.jetbrains.compose.resources.stringResource
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlinx.coroutines.delay
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
            text = stringResource(Res.string.your_space),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
            ),
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
    onPlayMixClick: ((ShelfItem) -> Unit)? = null,
) {
    if (items.isEmpty()) return

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val dynamicPalette = rememberArtworkPalette(
        imageUrl = currentSong?.thumbnailUrl,
        dark = isDark,
    )

    when (layoutMode) {
        SpeedDialLayoutMode.TWO_BY_THREE -> {
            HomeSpeedDialTwoByThree(
                items = items,
                pinnedIds = pinnedIds,
                currentSong = currentSong,
                isPlaying = isPlaying,
                palette = dynamicPalette,
                onItemClick = onItemClick,
                onLuckyDialClick = onLuckyDialClick,
                onItemLongPress = onItemLongPress,
                onPlayMixClick = onPlayMixClick,
                modifier = modifier,
            )
        }
        SpeedDialLayoutMode.THREE_BY_THREE -> {
            HomeSpeedDialThreeByThree(
                items = items,
                pinnedIds = pinnedIds,
                currentSong = currentSong,
                isPlaying = isPlaying,
                palette = dynamicPalette,
                onItemClick = onItemClick,
                onLuckyDialClick = onLuckyDialClick,
                onItemLongPress = onItemLongPress,
                onPlayMixClick = onPlayMixClick,
                modifier = modifier,
            )
        }
        SpeedDialLayoutMode.ONE_BY_FOUR_LIST -> {
            HomeSpeedDialList(
                items = items,
                pinnedIds = pinnedIds,
                currentSong = currentSong,
                isPlaying = isPlaying,
                palette = dynamicPalette,
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
    palette: ArtworkPalette,
    onItemClick: (ShelfItem) -> Unit,
    onLuckyDialClick: () -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
    modifier: Modifier = Modifier,
    onPlayMixClick: ((ShelfItem) -> Unit)? = null,
) {
    // Page 1 gets up to 5 items + Lucky Dial in slot 5
    val page1Items = items.take(5)
    val remainingItems = items.drop(5)
    val additionalPages = if (remainingItems.isNotEmpty()) remainingItems.chunked(6) else emptyList()
    val totalPages = 1 + additionalPages.size

    val pagerState = rememberPagerState(pageCount = { totalPages })

    var showIndicators by remember { mutableStateOf(false) }
    LaunchedEffect(pagerState.isScrollInProgress) {
        if (pagerState.isScrollInProgress) {
            showIndicators = true
        } else {
            delay(2000L)
            showIndicators = false
        }
    }

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
                            palette = palette,
                            onItemClick = onItemClick,
                            onItemLongPress = onItemLongPress,
                            onPlayMixClick = onPlayMixClick,
                        )
                        SpeedDialRow(
                            item1 = page1Items.getOrNull(2),
                            item2 = page1Items.getOrNull(3),
                            pinnedIds = pinnedIds,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            palette = palette,
                            onItemClick = onItemClick,
                            onItemLongPress = onItemLongPress,
                            onPlayMixClick = onPlayMixClick,
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
                                    palette = palette,
                                    isCompact = false,
                                    onItemClick = onItemClick,
                                    onItemLongPress = onItemLongPress,
                                    onPlayMixClick = onPlayMixClick,
                                    modifier = Modifier.weight(1f),
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }

                            LuckyDialCard(
                                palette = palette,
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
                                palette = palette,
                                onItemClick = onItemClick,
                                onItemLongPress = onItemLongPress,
                                onPlayMixClick = onPlayMixClick,
                            )
                        }
                        repeat(3 - rows.size) {
                            Spacer(modifier = Modifier.height(64.dp))
                        }
                    }
                }
            }

            // Floating Left Quick-Return Button
            androidx.compose.animation.AnimatedVisibility(
                visible = showIndicators && pagerState.currentPage > 0,
                enter = fadeIn(tween(250)) + slideInHorizontally(tween(250)) { -it },
                exit = fadeOut(tween(350)) + slideOutHorizontally(tween(350)) { -it },
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
            val indicatorAlpha by animateFloatAsState(
                targetValue = if (showIndicators) 1f else 0f,
                animationSpec = tween(durationMillis = 300),
                label = "indicatorAlpha",
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp), // Fixed reserved height prevents layout shifts!
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier.graphicsLayer { alpha = indicatorAlpha },
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
}

@Composable
private fun HomeSpeedDialThreeByThree(
    items: List<ShelfItem>,
    pinnedIds: Set<String>,
    currentSong: Song?,
    isPlaying: Boolean,
    palette: ArtworkPalette,
    onItemClick: (ShelfItem) -> Unit,
    onLuckyDialClick: () -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
    modifier: Modifier = Modifier,
    onPlayMixClick: ((ShelfItem) -> Unit)? = null,
) {
    // Page 1 gets up to 8 items + Lucky Dial in slot 8 (row 3, col 3)
    val page1Items = items.take(8)
    val remainingItems = items.drop(8)
    val additionalPages = if (remainingItems.isNotEmpty()) remainingItems.chunked(9) else emptyList()
    val totalPages = 1 + additionalPages.size

    val pagerState = rememberPagerState(pageCount = { totalPages })

    var showIndicators by remember { mutableStateOf(false) }
    LaunchedEffect(pagerState.isScrollInProgress) {
        if (pagerState.isScrollInProgress) {
            showIndicators = true
        } else {
            delay(2000L)
            showIndicators = false
        }
    }

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
                            palette = palette,
                            onItemClick = onItemClick,
                            onItemLongPress = onItemLongPress,
                            onPlayMixClick = onPlayMixClick,
                        )
                        // Row 2: items 3, 4, 5
                        SpeedDialRow3(
                            item1 = page1Items.getOrNull(3),
                            item2 = page1Items.getOrNull(4),
                            item3 = page1Items.getOrNull(5),
                            pinnedIds = pinnedIds,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            palette = palette,
                            onItemClick = onItemClick,
                            onItemLongPress = onItemLongPress,
                            onPlayMixClick = onPlayMixClick,
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
                                    palette = palette,
                                    isCompact = true,
                                    onItemClick = onItemClick,
                                    onItemLongPress = onItemLongPress,
                                    onPlayMixClick = onPlayMixClick,
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
                                    palette = palette,
                                    isCompact = true,
                                    onItemClick = onItemClick,
                                    onItemLongPress = onItemLongPress,
                                    onPlayMixClick = onPlayMixClick,
                                    modifier = Modifier.weight(1f),
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }

                            LuckyDialCard(
                                palette = palette,
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
                                palette = palette,
                                onItemClick = onItemClick,
                                onItemLongPress = onItemLongPress,
                                onPlayMixClick = onPlayMixClick,
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
                visible = showIndicators && pagerState.currentPage > 0,
                enter = fadeIn(tween(250)) + slideInHorizontally(tween(250)) { -it },
                exit = fadeOut(tween(350)) + slideOutHorizontally(tween(350)) { -it },
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
            val indicatorAlpha by animateFloatAsState(
                targetValue = if (showIndicators) 1f else 0f,
                animationSpec = tween(durationMillis = 300),
                label = "indicatorAlpha",
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp), // Fixed reserved height prevents layout shifts!
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier.graphicsLayer { alpha = indicatorAlpha },
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
}

@Composable
private fun HomeSpeedDialList(
    items: List<ShelfItem>,
    pinnedIds: Set<String>,
    currentSong: Song?,
    isPlaying: Boolean,
    palette: ArtworkPalette,
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
                        palette = palette,
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
    palette: ArtworkPalette,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
    onPlayMixClick: ((ShelfItem) -> Unit)? = null,
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
                palette = palette,
                isCompact = false,
                onItemClick = onItemClick,
                onItemLongPress = onItemLongPress,
                onPlayMixClick = onPlayMixClick,
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
                palette = palette,
                isCompact = false,
                onItemClick = onItemClick,
                onItemLongPress = onItemLongPress,
                onPlayMixClick = onPlayMixClick,
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
    palette: ArtworkPalette,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
    onPlayMixClick: ((ShelfItem) -> Unit)? = null,
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
                    palette = palette,
                    isCompact = true,
                    onItemClick = onItemClick,
                    onItemLongPress = onItemLongPress,
                    onPlayMixClick = onPlayMixClick,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

private enum class SpeedDialItemType {
    MIX,
    SONG,
    ALBUM,
    PLAYLIST,
    ARTIST,
}

private fun ShelfItem.detectType(): SpeedDialItemType {
    val bId = browseId
    val vId = videoId
    val sub = subtitle.lowercase()
    val t = title.lowercase()
    return when {
        bId?.startsWith("RD") == true || bId?.startsWith("VLRD") == true ||
            sub.contains("mix") || sub.contains("radio") || sub.contains("station") ||
            t.contains("supermix") || t.contains("my mix") || t.endsWith(" mix") || t.endsWith(" radio") -> SpeedDialItemType.MIX
        bId?.startsWith("UC") == true || sub.contains("artist") -> SpeedDialItemType.ARTIST
        bId?.startsWith("VL") == true || bId?.startsWith("PL") == true || bId?.contains("playlist") == true ||
            sub.contains("playlist") || sub.contains("track") || (sub.contains("song") && sub.contains("•")) ||
            title.startsWith("♫") -> SpeedDialItemType.PLAYLIST
        bId?.startsWith("MPREb_") == true || bId?.startsWith("OLAK") == true || sub.contains("album") || sub.contains("single") || sub.contains("ep") -> SpeedDialItemType.ALBUM
        vId != null || sub.startsWith("song") -> SpeedDialItemType.SONG
        bId != null -> SpeedDialItemType.ALBUM
        else -> SpeedDialItemType.SONG
    }
}

private fun cleanTypeSubtitle(subtitle: String, type: SpeedDialItemType): String {
    if (subtitle.isBlank()) return ""
    return when (type) {
        SpeedDialItemType.MIX -> subtitle.removePrefix("Mix • ").removePrefix("Radio • ").trim()
        SpeedDialItemType.SONG -> subtitle.removePrefix("Song • ").trim()
        SpeedDialItemType.ALBUM -> subtitle.removePrefix("Album • ").removePrefix("Single • ").removePrefix("EP • ").trim()
        SpeedDialItemType.PLAYLIST -> subtitle.removePrefix("Playlist • ").trim()
        SpeedDialItemType.ARTIST -> subtitle.removePrefix("Artist • ").trim()
    }
}

@Composable
private fun SpeedDialArtwork(
    item: ShelfItem,
    type: SpeedDialItemType,
    candidateArtUrl: String?,
    palette: ArtworkPalette,
    isDark: Boolean,
    isCompact: Boolean,
    isPinned: Boolean,
    onPlayMixClick: ((ShelfItem) -> Unit)?,
    onItemClick: (ShelfItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val artContainerSize = if (isCompact) 47.dp else 53.dp

    Box(
        modifier = modifier.size(artContainerSize),
        contentAlignment = Alignment.Center,
    ) {
        when (type) {
            SpeedDialItemType.ARTIST -> {
                val avatarSize = if (isCompact) 43.dp else 49.dp
                Box(
                    modifier = Modifier
                        .size(avatarSize)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF1E2834) else MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.5.dp, palette.accent.copy(alpha = 0.55f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = (if (isDark) Color.White else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.45f),
                        modifier = Modifier.size(if (isCompact) 20.dp else 24.dp),
                    )
                    if (!candidateArtUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = candidateArtUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            SpeedDialItemType.ALBUM -> {
                val jacketSize = if (isCompact) 39.dp else 45.dp
                val vinylSize = if (isCompact) 38.dp else 44.dp
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    // Peeking vinyl record disc
                    Box(
                        modifier = Modifier
                            .padding(start = if (isCompact) 8.dp else 9.dp)
                            .size(vinylSize)
                            .clip(CircleShape)
                            .background(Color(0xFF121417))
                            .border(0.8.dp, Color.White.copy(alpha = 0.22f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        // Groove line
                        Box(
                            modifier = Modifier
                                .size(if (isCompact) 20.dp else 23.dp)
                                .border(0.6.dp, Color.White.copy(alpha = 0.14f), CircleShape),
                        )
                        // Center spindle label
                        Box(
                            modifier = Modifier
                                .size(if (isCompact) 9.dp else 11.dp)
                                .clip(CircleShape)
                                .background(palette.accent.copy(alpha = 0.90f)),
                        )
                    }

                    // Front album jacket
                    Box(
                        modifier = Modifier
                            .size(jacketSize)
                            .clip(RoundedCornerShape(if (isCompact) 8.dp else 10.dp))
                            .background(if (isDark) Color(0xFF1E2834) else MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(if (isCompact) 8.dp else 10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Album,
                            contentDescription = null,
                            tint = (if (isDark) Color.White else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.45f),
                            modifier = Modifier.size(if (isCompact) 18.dp else 22.dp),
                        )
                        if (!candidateArtUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = candidateArtUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }

            SpeedDialItemType.PLAYLIST -> {
                val backW = if (isCompact) 41.dp else 47.dp
                val backH = if (isCompact) 43.dp else 49.dp
                val frontSize = if (isCompact) 41.dp else 47.dp

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    // Subtle stacked back card peek
                    Box(
                        modifier = Modifier
                            .size(width = backW, height = backH)
                            .offset(y = (-2.5).dp)
                            .clip(RoundedCornerShape(if (isCompact) 8.dp else 10.dp))
                            .background(palette.accent.copy(alpha = 0.35f))
                            .border(1.dp, palette.accent.copy(alpha = 0.50f), RoundedCornerShape(if (isCompact) 8.dp else 10.dp)),
                    )

                    // Front playlist card
                    Box(
                        modifier = Modifier
                            .size(frontSize)
                            .clip(RoundedCornerShape(if (isCompact) 10.dp else 12.dp))
                            .background(if (isDark) Color(0xFF1E2834) else MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(if (isCompact) 10.dp else 12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                            contentDescription = null,
                            tint = (if (isDark) Color.White else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.45f),
                            modifier = Modifier.size(if (isCompact) 18.dp else 22.dp),
                        )
                        if (!candidateArtUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = candidateArtUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        // Playlist micro-badge at bottom-end
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.Black.copy(alpha = 0.70f))
                                .padding(horizontal = 2.dp, vertical = 1.dp),
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

            SpeedDialItemType.MIX -> {
                val mixSize = if (isCompact) 44.dp else 50.dp
                val isLightAccent = palette.accent.luminance() > 0.5f
                val mixBadgeText = if (isLightAccent) Color(0xFF0F080B) else Color.White

                Box(
                    modifier = Modifier
                        .size(mixSize)
                        .clip(RoundedCornerShape(if (isCompact) 10.dp else 12.dp))
                        .background(if (isDark) Color(0xFF1E2834) else MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.5.dp, palette.accent, RoundedCornerShape(if (isCompact) 10.dp else 12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Casino,
                        contentDescription = null,
                        tint = (if (isDark) Color.White else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.45f),
                        modifier = Modifier.size(if (isCompact) 18.dp else 22.dp),
                    )
                    if (!candidateArtUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = candidateArtUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    // "MIX" micro-badge at Alignment.TopStart
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(palette.accent)
                            .padding(horizontal = 3.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text = "MIX",
                            color = mixBadgeText,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 8.sp,
                        )
                    }

                    // Quick Play button at Alignment.BottomEnd (if not pinned)
                    if (!isPinned) {
                        val playBtnSize = if (isCompact) 17.dp else 20.dp
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(2.dp)
                                .size(playBtnSize)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.75f))
                                .clickable { onPlayMixClick?.invoke(item) ?: onItemClick(item) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Play Mix",
                                tint = Color.White,
                                modifier = Modifier.size(if (isCompact) 11.dp else 13.dp),
                            )
                        }
                    }
                }
            }

            SpeedDialItemType.SONG -> {
                val songSize = artContainerSize
                val artworkShape = RoundedCornerShape(if (isCompact) 13.dp else 15.dp)

                Box(
                    modifier = Modifier
                        .size(songSize)
                        .clip(artworkShape)
                        .background(if (isDark) Color(0xFF1E2834) else MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, Color.White.copy(alpha = 0.10f), artworkShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LibraryMusic,
                        contentDescription = null,
                        tint = (if (isDark) Color.White else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.45f),
                        modifier = Modifier.size(if (isCompact) 20.dp else 24.dp),
                    )
                    if (!candidateArtUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = candidateArtUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }

        // Overlapping Location Pin badge at bottom-right of artwork container
        if (isPinned) {
            val badgeSize = if (isCompact) 17.dp else 20.dp
            val pinIconSize = if (isCompact) 10.dp else 12.dp
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size(badgeSize)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF121921) else MaterialTheme.colorScheme.surface)
                    .border(1.dp, if (isDark) Color(0xFF2B3A4A) else Color.Black.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = BitChordIcons.LocationPin,
                    contentDescription = "Pinned to Your Space",
                    tint = Color(0xFFD6B27D), // warm golden/tan location pin
                    modifier = Modifier.size(pinIconSize),
                )
            }
        }
    }
}

@Composable
private fun SpeedDialTile(
    item: ShelfItem,
    isPinned: Boolean,
    isCurrent: Boolean,
    isPlaying: Boolean,
    palette: ArtworkPalette,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier,
    onPlayMixClick: ((ShelfItem) -> Unit)? = null,
) {
    val type = item.detectType()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Card dimensions
    val tileHeight = if (isCompact) 56.dp else 64.dp
    val outerShape = RoundedCornerShape(if (isCompact) 18.dp else 22.dp)
    val cardPadding = if (isCompact) 4.5.dp else 5.5.dp

    // Dynamic background derived from the currently playing song's artwork
    val cardBg = if (isDark) {
        palette.elevated.copy(alpha = 0.70f)
    } else {
        palette.elevated.copy(alpha = 0.88f)
    }

    val titleColor = when {
        isCurrent -> if (isDark) palette.accent else PlayingAccent
        isDark -> palette.onBackground
        else -> MaterialTheme.colorScheme.onSurface
    }

    val subtitleColor = if (isDark) {
        palette.onBackgroundVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

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

    val candidateArtUrl = item.thumbnailUrl?.artworkAt(CARD_ART_PX) ?: item.thumbnailUrl

    // Unified card capsule container (borderless clean elevated surface)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(tileHeight)
            .clip(outerShape)
            .background(cardBg)
            .then(clickModifier),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = cardPadding, end = if (isCompact) 8.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 7.dp else 9.dp),
        ) {
            // Artwork container with distinct type treatment
            SpeedDialArtwork(
                item = item,
                type = type,
                candidateArtUrl = candidateArtUrl,
                palette = palette,
                isDark = isDark,
                isCompact = isCompact,
                isPinned = isPinned,
                onPlayMixClick = onPlayMixClick,
                onItemClick = onItemClick,
            )

            // Title & Subtitle column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = item.title,
                    style = if (isCompact) {
                        MaterialTheme.typography.titleSmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.15).sp,
                        )
                    } else {
                        MaterialTheme.typography.titleMedium.copy(
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp,
                        )
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = titleColor,
                )

                val cleanSubtitle = cleanTypeSubtitle(item.subtitle, type)
                if (cleanSubtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(1.5.dp))
                    Text(
                        text = cleanSubtitle,
                        style = if (isCompact) {
                            MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        } else {
                            MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = subtitleColor,
                    )
                }
            }

            // Playing animation indicator
            if (isCurrent && isPlaying) {
                SearchPlayingBars(
                    modifier = Modifier.padding(end = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun LuckyDialCard(
    palette: ArtworkPalette,
    onClick: () -> Unit,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val tileHeight = if (isCompact) 56.dp else 64.dp
    val outerShape = RoundedCornerShape(if (isCompact) 18.dp else 22.dp)
    val cardPadding = if (isCompact) 4.5.dp else 5.5.dp
    val artSize = tileHeight - (cardPadding * 2)
    val artworkShape = RoundedCornerShape(if (isCompact) 13.dp else 15.dp)

    // Dynamic accent color for dice box derived from current song's artwork
    val diceBoxBg = if (isDark) {
        palette.accent.copy(alpha = 0.85f)
    } else {
        palette.accent.copy(alpha = 0.90f)
    }
    val isLightAccent = diceBoxBg.luminance() > 0.5f
    val diceIconTint = if (isLightAccent) Color(0xFF0F080B) else Color.White

    // Card background matching other cards + subtle accent glow
    val cardBg = if (isDark) {
        palette.elevated.copy(alpha = 0.70f)
    } else {
        palette.elevated.copy(alpha = 0.88f)
    }
    val accentTint = palette.accent.copy(alpha = if (isDark) 0.15f else 0.10f)

    val titleColor = if (isDark) palette.onBackground else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (isDark) palette.onBackgroundVariant else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(tileHeight)
            .clip(outerShape)
            .background(cardBg)
            .background(accentTint)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = cardPadding, end = if (isCompact) 8.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 7.dp else 9.dp),
        ) {
            // Dice squircle container on the left
            Box(
                modifier = Modifier
                    .size(artSize)
                    .clip(artworkShape)
                    .background(diceBoxBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Casino,
                    contentDescription = stringResource(Res.string.roll_a_dice),
                    tint = diceIconTint,
                    modifier = Modifier.size(if (isCompact) 22.dp else 26.dp),
                )
            }

            // Title & Subtitle column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(Res.string.roll_a_dice),
                    style = if (isCompact) {
                        MaterialTheme.typography.titleSmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.15).sp,
                        )
                    } else {
                        MaterialTheme.typography.titleMedium.copy(
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp,
                        )
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = titleColor,
                )
                Spacer(modifier = Modifier.height(1.5.dp))
                Text(
                    text = "- " + stringResource(Res.string.instant_discovery_mix),
                    style = if (isCompact) {
                        MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    } else {
                        MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = subtitleColor,
                )
            }
        }
    }
}
