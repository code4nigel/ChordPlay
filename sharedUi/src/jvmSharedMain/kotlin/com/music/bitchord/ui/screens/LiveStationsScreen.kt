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
package com.music.bitchord.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.music.bitchord.data.innertube.InnertubeParser
import com.music.bitchord.data.model.CARD_ART_PX
import com.music.bitchord.data.model.LiveStationCategory
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.UiState
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.sharedui.resources.Res
import com.music.bitchord.sharedui.resources.live_all_stations
import com.music.bitchord.sharedui.resources.live_search_hint
import com.music.bitchord.sharedui.resources.live_station_247
import com.music.bitchord.sharedui.resources.live_stations_empty
import com.music.bitchord.sharedui.resources.retry
import com.music.bitchord.ui.components.MessageState
import com.music.bitchord.ui.components.PAGE_GUTTER
import com.music.bitchord.ui.components.PullToRefresh
import com.music.bitchord.ui.components.SearchField
import com.music.bitchord.ui.components.SearchPlayingBars
import com.music.bitchord.ui.components.ShimmerBox
import com.music.bitchord.ui.components.SmoothCornerShape
import com.music.bitchord.ui.components.contextClick
import com.music.bitchord.ui.components.longPressMenuClickable
import com.music.bitchord.ui.player.LiveBadge
import org.jetbrains.compose.resources.stringResource

private val LiveCrimson = Color(0xFFFF2D55)

/**
 * Full-screen directory for Live Music Stations with category chips and in-page keyword search.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveStationsScreen(
    liveStations: UiState<List<ShelfItem>>,
    currentSong: Song?,
    isPlaying: Boolean,
    @Suppress("UNUSED_PARAMETER") onBack: () -> Unit,
    onLiveStationClick: (List<ShelfItem>, Int) -> Unit,
    onRefresh: () -> Unit,
    refreshing: Boolean,
    pullState: PullToRefreshState = rememberPullToRefreshState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onLiveStationLongPress: ((ShelfItem) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(LiveStationCategory.ALL) }

    val filteredStations = remember(liveStations, selectedCategory, searchQuery) {
        val data = (liveStations as? UiState.Success)?.data.orEmpty()
        data.asSequence()
            .filter { item ->
                if (selectedCategory == LiveStationCategory.ALL) true
                else item.liveCategory == selectedCategory
            }
            .filter { item ->
                if (searchQuery.isBlank()) true
                else item.title.contains(searchQuery, ignoreCase = true) ||
                    item.subtitle.contains(searchQuery, ignoreCase = true)
            }
            .sortedByDescending { parseLiveViewerCount(it.liveViewersText) }
            .toList()
    }

    val gridPadding = PaddingValues(
        start = PAGE_GUTTER,
        end = PAGE_GUTTER,
        top = contentPadding.calculateTopPadding() + 4.dp,
        bottom = contentPadding.calculateBottomPadding() + 24.dp,
    )

    PullToRefresh(
        refreshing = refreshing,
        onRefresh = onRefresh,
        state = pullState,
        modifier = modifier,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = gridPadding,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "live:controls_header", span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(Res.string.live_all_stations),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
                    )
                    SearchField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onSubmit = {},
                        placeholder = stringResource(Res.string.live_search_hint),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(LiveStationCategory.entries, key = { it.name }) { category ->
                            val selected = category == selectedCategory
                            LiveCategoryChip(
                                label = category.localizedLabel(),
                                selected = selected,
                                onClick = { selectedCategory = category },
                            )
                        }
                    }
                }
            }

            when (liveStations) {
                UiState.Loading -> {
                    item(key = "live:loading", span = { GridItemSpan(maxLineSpan) }) {
                        LiveStationsGridSkeleton()
                    }
                }
                is UiState.Error -> {
                    item(key = "live:error", span = { GridItemSpan(maxLineSpan) }) {
                        MessageState(
                            liveStations.message,
                            actionLabel = stringResource(Res.string.retry),
                            onAction = onRefresh,
                        )
                    }
                }
                is UiState.Success -> {
                    if (filteredStations.isEmpty()) {
                        item(key = "live:empty", span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(Res.string.live_stations_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = filteredStations,
                            key = { index, item -> "grid:live:${item.videoId ?: index}:${item.title}" },
                        ) { index, item ->
                            val isCurrent = item.matchesCurrentlyPlaying(currentSong)
                            LiveStationOptionACard(
                                item = item,
                                isCurrent = isCurrent,
                                isPlaying = isCurrent && isPlaying,
                                onClick = { onLiveStationClick(filteredStations, index) },
                                onLongPress = onLiveStationLongPress?.let { { it(item) } },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveCategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val bgColor by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.24f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "liveChipBg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.24f)
        else Color.White.copy(alpha = 0.08f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "liveChipBorder",
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onBackground
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "liveChipText",
    )

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bgColor)
            .background(
                Brush.verticalGradient(
                    listOf(
                        if (selected) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f),
                        Color.Transparent,
                    ),
                ),
            )
            .border(width = 1.dp, color = borderColor, shape = CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = textColor,
            maxLines = 1,
        )
    }
}

/**
 * Option A ("Aura Depth & Specular Glow") Live Station Card:
 * - Fluid squircle geometry with smooth 18dp corners.
 * - Sub-pixel top specular highlight gradient.
 * - Dynamic crimson / artwork ambient glow backing.
 * - Sleek soundwave live badge and viewer tally.
 * - No harsh 1px borders.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LiveStationOptionACard(
    item: ShelfItem,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val cardShape = remember { SmoothCornerShape(18.dp) }
    val artistLabel = remember(item.subtitle) {
        InnertubeParser.artistFromSubtitle(item.subtitle).takeIf { it.isNotBlank() }
    } ?: stringResource(Res.string.live_station_247)
    val accent = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .longPressMenuClickable(onClick = onClick, onLongClick = onLongPress)
            .contextClick(onLongPress),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(cardShape)
                .background(Color(0xFF14151C)),
        ) {
            // Ambient Aura Glow (spills soft light behind card)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                if (isCurrent) accent.copy(alpha = 0.35f)
                                else LiveCrimson.copy(alpha = 0.16f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )

            // Station Artwork
            AsyncImage(
                model = item.thumbnailUrl.artworkAt(CARD_ART_PX),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            // Subtle Depth & Vignette Gradient
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.25f),
                            0.45f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.72f),
                        ),
                    ),
            )

            // Sub-pixel Specular Top Edge Highlight
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.White.copy(alpha = 0.18f),
                            0.06f to Color.Transparent,
                            1f to Color.Transparent,
                        ),
                    ),
            )

            // Top-Start Live Soundwave Badge
            LiveBadge(
                compact = true,
                showSoundwaves = true,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
            )

            // Center Playing Equalizer
            if (isCurrent && isPlaying) {
                SearchPlayingBars(Modifier.align(Alignment.Center))
            }

            // Bottom Viewers Tally Pill
            item.liveViewersText?.takeIf { it.isNotBlank() }?.let { viewers ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.14f), Color.Transparent),
                            ),
                        )
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF5252)),
                        )
                        Text(
                            text = viewers,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isCurrent) accent else MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = artistLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Parses YouTube live viewer strings (e.g. "24K watching", "1.2M watching", "850 watching") to a numeric Long for sorting. */
fun parseLiveViewerCount(text: String?): Long {
    if (text.isNullOrBlank()) return 0L
    val clean = text.replace(",", "").trim().lowercase()
    val match = Regex("""([\d.]+)\s*([km])?""").find(clean) ?: return 0L
    val num = match.groupValues[1].toDoubleOrNull() ?: return 0L
    val unit = match.groupValues.getOrNull(2) ?: ""
    return when (unit) {
        "m" -> (num * 1_000_000).toLong()
        "k" -> (num * 1_000).toLong()
        else -> num.toLong()
    }
}

@Composable
private fun LiveStationsGridSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(3) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(2) {
                    Column(Modifier.weight(1f)) {
                        ShimmerBox(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 10f),
                            RoundedCornerShape(18.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        ShimmerBox(
                            Modifier
                                .fillMaxWidth(0.75f)
                                .height(14.dp),
                            RoundedCornerShape(6.dp),
                        )
                        Spacer(Modifier.height(4.dp))
                        ShimmerBox(
                            Modifier
                                .fillMaxWidth(0.45f)
                                .height(12.dp),
                            RoundedCornerShape(6.dp),
                        )
                    }
                }
            }
        }
    }
}
