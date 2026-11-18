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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.music.bitchord.data.model.MoodGenre
import com.music.bitchord.data.model.MoodGenreSection
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.UiState
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.sharedui.resources.Res
import com.music.bitchord.sharedui.resources.explore
import com.music.bitchord.sharedui.resources.melody
import com.music.bitchord.sharedui.resources.radio
import com.music.bitchord.sharedui.resources.radio_featured
import com.music.bitchord.sharedui.resources.radio_featured_subtitle
import com.music.bitchord.sharedui.resources.radio_recent_seeds
import com.music.bitchord.sharedui.resources.radio_start_station
import com.music.bitchord.sharedui.resources.radio_stations
import com.music.bitchord.sharedui.resources.retry
import com.music.bitchord.ui.components.MessageState
import com.music.bitchord.ui.components.PAGE_GUTTER
import com.music.bitchord.ui.components.PullToRefresh
import com.music.bitchord.ui.components.ShelfRow
import com.music.bitchord.ui.components.ShimmerBox
import com.music.bitchord.ui.icons.BitChordIcons
import com.music.bitchord.ui.player.MeshGradientBackground
import com.music.bitchord.ui.player.MeshPalette
import org.jetbrains.compose.resources.stringResource

/**
 * The Radio tab: featured ChordPlay Melody station at the top, followed by
 * "Start a station", "Moods & Genres", recently played seeds, and a wall of
 * mood/genre stations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioScreen(
    state: UiState<List<MoodGenreSection>>,
    listState: LazyListState,
    melodyItem: ShelfItem?,
    onMelodyClick: () -> Unit,
    seeds: List<ShelfItem>,
    onSeedClick: (ShelfItem) -> Unit,
    onStationClick: (MoodGenre) -> Unit,
    onStartStation: () -> Unit,
    onGenresClick: () -> Unit,
    onRetry: () -> Unit,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    pullState: PullToRefreshState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    PullToRefresh(
        refreshing = refreshing,
        onRefresh = onRefresh,
        state = pullState,
        modifier = modifier,
    ) {
        LazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "radio:title") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = PAGE_GUTTER,
                            end = PAGE_GUTTER,
                            top = 8.dp,
                            bottom = 14.dp,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.radio),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioHeaderActionButton(
                            icon = BitChordIcons.Search,
                            contentDescription = stringResource(Res.string.radio_start_station),
                            onClick = onStartStation,
                        )
                        RadioHeaderActionButton(
                            icon = BitChordIcons.TabExplore,
                            contentDescription = stringResource(Res.string.explore),
                            onClick = onGenresClick,
                        )
                    }
                }
            }
            item(key = "radio:featured") {
                RadioFeatureCard(
                    item = melodyItem,
                    onClick = onMelodyClick,
                )
            }
            item(key = "radio:featured_gap") { Spacer(Modifier.height(14.dp)) }
            if (seeds.isNotEmpty()) {
                item(key = "radio:seeds_header") {
                    SectionHeader(stringResource(Res.string.radio_recent_seeds))
                }
                item(key = "radio:seeds") {
                    ShelfRow(
                        contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(
                            items = seeds,
                            key = { seed -> "seed:${seed.videoId ?: seed.browseId ?: seed.title}" },
                        ) { item ->
                            ShelfCard(
                                item = item,
                                onClick = { onSeedClick(item) },
                            )
                        }
                    }
                }
                item(key = "radio:seeds_gap") { Spacer(Modifier.height(18.dp)) }
            }
            when (state) {
                UiState.Loading -> item(key = "radio:skeleton") {
                    RadioSkeleton()
                }
                is UiState.Error -> item(key = "radio:error") {
                    MessageState(
                        state.message,
                        actionLabel = stringResource(Res.string.retry),
                        onAction = onRetry,
                    )
                }
                is UiState.Success -> {
                    val genres = state.data.flatMap(MoodGenreSection::items)
                        .distinctBy { it.browseId to it.params }
                    if (genres.isNotEmpty()) {
                        item(key = "radio:stations_header") {
                            SectionHeader(stringResource(Res.string.radio_stations))
                        }
                        item(key = "radio:stations") {
                            StationGrid(genres = genres, onStationClick = onStationClick)
                        }
                    }
                }
            }
        }
    }
}

/**
 * The personal station card at the top of Radio: uses Melody's current cover
 * when built and an animated ChordPlay Melody mesh gradient when empty.
 */
@Composable
private fun RadioFeatureCard(item: ShelfItem?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val melodyPalette = remember {
        MeshPalette(
            listOf(
                Color(0xFF1F1646),
                Color(0xFF6B32C9),
                Color(0xFFFF5C4D),
                Color(0xFF141E30),
            ),
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_GUTTER)
            .aspectRatio(1.52f)
            .clip(shape)
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        val art = item?.thumbnailUrl
        MeshGradientBackground(
            palette = melodyPalette,
            trackKey = "chordplay:melody:hero",
            continuous = true,
            blurRadius = 24.dp,
            modifier = Modifier.fillMaxSize(),
        )
        if (!art.isNullOrBlank()) {
            AsyncImage(
                model = art.artworkAt(720),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = BitChordIcons.Infinity,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.22f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(96.dp),
            )
        }
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.08f),
                        0.45f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.80f),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 18.dp, end = 18.dp, bottom = 16.dp),
        ) {
            Text(
                text = stringResource(Res.string.radio_featured).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.80f),
                letterSpacing = 1.4.sp,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item?.title ?: stringResource(Res.string.melody),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = BitChordIcons.Infinity,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.90f),
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                text = item?.subtitle ?: stringResource(Res.string.radio_featured_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.84f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp)
                .size(38.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.36f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = BitChordIcons.Play,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun RadioHeaderActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f))
            .border(
                width = 0.75.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(19.dp),
        )
    }
}

/** Square station tiles arranged cleanly by available width. */
@Composable
private fun StationGrid(genres: List<MoodGenre>, onStationClick: (MoodGenre) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = (maxWidth / 118.dp).toInt().coerceIn(3, 5)
        Column(
            modifier = Modifier.padding(horizontal = PAGE_GUTTER),
            verticalArrangement = Arrangement.spacedBy(STATION_SPACING),
        ) {
            genres.chunked(columns).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(STATION_SPACING),
                ) {
                    row.forEach { genre ->
                        StationTile(
                            genre = genre,
                            onClick = { onStationClick(genre) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(columns - row.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun StationTile(genre: MoodGenre, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val wash = remember(genre.stripeColor, genre.title) {
        genre.stripeColor?.let { Color(it.toInt()) } ?: Color(0xFF3A3A3C)
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(wash)
            .clickable(onClick = onClick),
    ) {
        genre.thumbnailUrl?.let { art ->
            AsyncImage(
                model = art.artworkAt(CARD_ART_PX),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to wash.copy(alpha = 0.38f),
                        0.55f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.62f),
                    ),
                ),
        )
        Text(
            text = genre.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 10.dp, vertical = 9.dp),
        )
    }
}

@Composable
private fun RadioSkeleton() {
    Column(
        modifier = Modifier.padding(horizontal = PAGE_GUTTER),
        verticalArrangement = Arrangement.spacedBy(STATION_SPACING),
    ) {
        repeat(4) {
            Row(horizontalArrangement = Arrangement.spacedBy(STATION_SPACING)) {
                repeat(3) {
                    ShimmerBox(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        RoundedCornerShape(16.dp),
                    )
                }
            }
        }
    }
}

private val STATION_SPACING = 10.dp
