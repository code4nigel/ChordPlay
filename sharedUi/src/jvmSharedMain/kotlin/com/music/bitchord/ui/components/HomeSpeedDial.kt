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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.bitchord.data.model.CARD_ART_PX
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.ui.icons.BitChordIcons
import kotlinx.coroutines.launch

/**
 * 6-Tile Speed Dial Grid for ChordPlay Living Home Screen.
 *
 * Inspired by YouTube Music & Metrolist:
 * - 2-column by 3-row grid per page
 * - Slot 5 (bottom-right tile of Page 1) is permanently dedicated to the "Lucky Radio" discovery card
 * - Supports horizontal paging across multiple pages when more than 5 items are pinned or available
 * - Pinned items render an active PushPin badge
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
    onItemLongPress: ((ShelfItem) -> Unit)? = null,
) {
    if (items.isEmpty()) return

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
                                    onItemClick = onItemClick,
                                    onItemLongPress = onItemLongPress,
                                    modifier = Modifier.weight(1f),
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }

                            LuckyDialCard(
                                onClick = onLuckyDialClick,
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
                onItemClick = onItemClick,
                onItemLongPress = onItemLongPress,
                modifier = Modifier.weight(1f),
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

private enum class SpeedDialItemType(val label: String) {
    SONG("SONG"),
    ALBUM("ALBUM"),
    PLAYLIST("PLAYLIST"),
    ARTIST("ARTIST"),
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

    Box(
        modifier = modifier
            .height(56.dp)
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
                    .size(56.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                when (type) {
                    SpeedDialItemType.ARTIST -> {
                        // Circular avatar with subtle glowing ring
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), CircleShape),
                        ) {
                            AsyncImage(
                                model = item.thumbnailUrl.artworkAt(CARD_ART_PX),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                    SpeedDialItemType.ALBUM -> {
                        // Vinyl album jacket with peeking record rim & vinyl micro-badge
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = item.thumbnailUrl.artworkAt(CARD_ART_PX),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                            // Right spine / vinyl edge shadow
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .fillMaxHeight()
                                    .width(3.dp)
                                    .background(Color.Black.copy(alpha = 0.35f)),
                            )
                            // Micro badge pill at bottom-left
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(3.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .padding(horizontal = 3.dp, vertical = 2.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Album,
                                    contentDescription = "Album",
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(9.dp),
                                )
                            }
                        }
                    }
                    SpeedDialItemType.PLAYLIST -> {
                        // Playlist card with playlist micro-badge
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = item.thumbnailUrl.artworkAt(CARD_ART_PX),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                            // Upper stack accent line
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.50f)),
                            )
                            // Micro badge pill at bottom-left
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(3.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .padding(horizontal = 3.dp, vertical = 2.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                    contentDescription = "Playlist",
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(9.dp),
                                )
                            }
                        }
                    }
                    SpeedDialItemType.SONG -> {
                        // Standard square artwork
                        AsyncImage(
                            model = item.thumbnailUrl.artworkAt(CARD_ART_PX),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            // Title and Subtitle with Type Badges
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 9.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isCurrent) PlayingAccent else MaterialTheme.colorScheme.onSurface,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (type != SpeedDialItemType.SONG) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                                .border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(3.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                        ) {
                            Text(
                                text = type.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.4.sp,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    val cleanSubtitle = cleanTypeSubtitle(item.subtitle, type)
                    if (cleanSubtitle.isNotBlank()) {
                        Text(
                            text = cleanSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (isCurrent && isPlaying) {
                SearchPlayingBars(modifier = Modifier.padding(end = 10.dp))
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
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.90f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.80f),
                    ),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.25f), shape)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = "Lucky Radio",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Lucky Radio",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White,
                )
                Text(
                    text = "Fresh discovery mix",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }
}
