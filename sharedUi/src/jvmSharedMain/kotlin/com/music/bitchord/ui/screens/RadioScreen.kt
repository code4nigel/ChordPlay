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

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.bitchord.data.innertube.InnertubeParser
import com.music.bitchord.data.model.CARD_ART_PX
import com.music.bitchord.data.model.HEADER_ART_PX
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.KeyedMix
import com.music.bitchord.data.model.LiveStationCategory
import com.music.bitchord.data.model.MoodGenre
import com.music.bitchord.data.model.MoodGenreSection
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.UiState
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.sharedui.resources.Res
import com.music.bitchord.sharedui.resources.explore
import com.music.bitchord.sharedui.resources.ic_logo
import com.music.bitchord.sharedui.resources.live_cat_all
import com.music.bitchord.sharedui.resources.live_cat_artist_stations
import com.music.bitchord.sharedui.resources.live_cat_electronic
import com.music.bitchord.sharedui.resources.live_cat_hiphop
import com.music.bitchord.sharedui.resources.live_cat_hits
import com.music.bitchord.sharedui.resources.live_cat_jazz
import com.music.bitchord.sharedui.resources.live_cat_lofi_chill
import com.music.bitchord.sharedui.resources.live_cat_rock_pop
import com.music.bitchord.sharedui.resources.live_station_247
import com.music.bitchord.sharedui.resources.live_stations
import com.music.bitchord.sharedui.resources.live_stations_empty
import com.music.bitchord.sharedui.resources.melody
import com.music.bitchord.sharedui.resources.mixes_quick
import com.music.bitchord.sharedui.resources.mixes_your
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
import com.music.bitchord.ui.components.SearchPlayingBars
import com.music.bitchord.ui.components.ShelfRow
import com.music.bitchord.ui.components.ShimmerBox
import com.music.bitchord.ui.components.SmoothCornerShape
import com.music.bitchord.ui.components.contextClick
import com.music.bitchord.ui.components.longPressMenuClickable
import com.music.bitchord.ui.components.thumbnailBorder
import com.music.bitchord.ui.icons.BitChordIcons
import com.music.bitchord.ui.player.LiveBadge
import com.music.bitchord.ui.player.MeshGradientBackground
import com.music.bitchord.ui.player.MeshPalette
import com.music.bitchord.ui.theme.rememberArtworkPalette
import org.jetbrains.compose.resources.painterResource
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
    liveStations: UiState<List<ShelfItem>> = UiState.Success(emptyList()),
    currentSong: Song? = null,
    isPlaying: Boolean = false,
    onLiveStationClick: (List<ShelfItem>, Int) -> Unit = { _, _ -> },
    onRetryLiveStations: () -> Unit = {},
    onSeeAllLiveStations: () -> Unit = {},
    onLiveStationLongPress: ((ShelfItem) -> Unit)? = null,
    mixes: UiState<List<KeyedMix>> = UiState.Success(emptyList()),
    fallbackMoodMixes: List<HomeShelf> = emptyList(),
    onMixClick: ((ShelfItem) -> Unit)? = null,
    onMixLongPress: ((ShelfItem) -> Unit)? = null,
) {
    val showLiveSection = liveStations !is UiState.Success || liveStations.data.isNotEmpty()
    val groupedMixes = remember(mixes) {
        (mixes as? UiState.Success)?.data?.let(::groupMixes)
    }
    val openMix: (ShelfItem) -> Unit = remember(onMixClick, onSeedClick) {
        { item -> (onMixClick ?: onSeedClick)(item) }
    }
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
            if (showLiveSection) {
                item(key = "radio:live_header") {
                    SectionHeader(
                        title = stringResource(Res.string.live_stations),
                        onShowAll = onSeeAllLiveStations,
                    )
                }
                item(key = "radio:live_content") {
                    when (liveStations) {
                        UiState.Loading -> LiveStationsSkeleton()
                        is UiState.Error -> MessageState(
                            liveStations.message,
                            actionLabel = stringResource(Res.string.retry),
                            onAction = onRetryLiveStations,
                        )
                        is UiState.Success -> {
                            val stations = liveStations.data
                            if (stations.isEmpty()) {
                                Text(
                                    text = stringResource(Res.string.live_stations_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = PAGE_GUTTER, vertical = 12.dp),
                                )
                            } else {
                                ShelfRow(
                                    contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                ) {
                                    itemsIndexed(
                                        items = stations,
                                        key = { index, item -> "live:${item.videoId ?: index}:${item.title}" },
                                    ) { index, item ->
                                        val isCurrent = item.matchesCurrentlyPlaying(currentSong)
                                        LiveStationCard(
                                            item = item,
                                            isCurrent = isCurrent,
                                            isPlaying = isCurrent && isPlaying,
                                            onClick = { onLiveStationClick(stations, index) },
                                            onLongPress = onLiveStationLongPress?.let { { it(item) } },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                item(key = "radio:live_gap") { Spacer(Modifier.height(18.dp)) }
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
                        val previewGenres = genres.take(6)
                        item(key = "radio:stations_header") {
                            SectionHeader(
                                title = stringResource(Res.string.radio_stations),
                                onShowAll = if (genres.size > 6) onGenresClick else null,
                            )
                        }
                        item(key = "radio:stations") {
                            StationGrid(genres = previewGenres, onStationClick = onStationClick)
                        }
                        item(key = "radio:stations_gap") { Spacer(Modifier.height(22.dp)) }
                    }
                }
            }
            when {
                mixes is UiState.Loading -> {
                    item(key = "radio:mixes_skeleton") {
                        MixesSkeleton()
                    }
                }
                groupedMixes != null -> {
                    if (groupedMixes.quickMixes.isNotEmpty()) {
                        item(key = "radio:quick_mixes_header") {
                            SectionHeader(stringResource(Res.string.mixes_quick))
                        }
                        item(key = "radio:quick_mixes") {
                            QuickMixGrid(
                                items = groupedMixes.quickMixes,
                                onMixClick = openMix,
                                onMixLongPress = onMixLongPress,
                                modifier = Modifier.padding(bottom = 24.dp),
                            )
                        }
                    }
                    if (groupedMixes.yourMixes.isNotEmpty()) {
                        item(key = "radio:your_mixes") {
                            YourMixesCarousel(
                                title = stringResource(Res.string.mixes_your),
                                items = groupedMixes.yourMixes,
                                onMixClick = openMix,
                                onMixLongPress = onMixLongPress,
                            )
                        }
                    }
                    groupedMixes.moodShelves.forEachIndexed { idx, (moodTitle, items) ->
                        item(key = "radio:mood_mix:$idx:$moodTitle") {
                            Shelf(
                                shelf = HomeShelf(title = moodTitle, items = items),
                                onItemClick = openMix,
                                onItemLongPress = onMixLongPress,
                                currentSong = currentSong,
                                isPlaying = isPlaying,
                                showSubtitle = false,
                            )
                        }
                    }
                }
            }
            val showFallbackMoods = groupedMixes == null || groupedMixes.moodShelves.isEmpty()
            if (showFallbackMoods && fallbackMoodMixes.isNotEmpty()) {
                fallbackMoodMixes.forEachIndexed { idx, shelf ->
                    item(key = "radio:fallback_mood:$idx:${shelf.title}") {
                        Shelf(
                            shelf = shelf,
                            onItemClick = openMix,
                            onItemLongPress = onMixLongPress,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            showSubtitle = false,
                        )
                    }
                }
            }
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
                        0.30f to Color.Black.copy(alpha = 0.15f),
                        0.62f to Color.Black.copy(alpha = 0.58f),
                        1f to Color.Black.copy(alpha = 0.86f),
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

@Composable
fun LiveStationCategory.localizedLabel(): String = stringResource(
    when (this) {
        LiveStationCategory.ALL -> Res.string.live_cat_all
        LiveStationCategory.ARTIST_STATIONS -> Res.string.live_cat_artist_stations
        LiveStationCategory.LOFI_CHILL -> Res.string.live_cat_lofi_chill
        LiveStationCategory.HITS -> Res.string.live_cat_hits
        LiveStationCategory.HIPHOP_RNB -> Res.string.live_cat_hiphop
        LiveStationCategory.ELECTRONIC_EDM -> Res.string.live_cat_electronic
        LiveStationCategory.ROCK_POP -> Res.string.live_cat_rock_pop
        LiveStationCategory.JAZZ_CAFE -> Res.string.live_cat_jazz
    },
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LiveStationCard(
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
    val liveCrimson = Color(0xFFFF2D55)
    val accent = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .width(236.dp)
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
            // Ambient Aura Glow
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                if (isCurrent) accent.copy(alpha = 0.35f)
                                else liveCrimson.copy(alpha = 0.16f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
            AsyncImage(
                model = item.thumbnailUrl.artworkAt(CARD_ART_PX),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
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
            LiveBadge(
                compact = true,
                showSoundwaves = true,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
            )
            if (isCurrent && isPlaying) {
                SearchPlayingBars(Modifier.align(Alignment.Center))
            }
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

@Composable
private fun LiveStationsSkeleton() {
    ShelfRow(
        contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(3) {
            Column(Modifier.width(228.dp)) {
                ShimmerBox(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                    RoundedCornerShape(16.dp),
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

@Composable
private fun QuickMixGrid(
    items: List<ShelfItem>,
    onMixClick: (ShelfItem) -> Unit,
    onMixLongPress: ((ShelfItem) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val columns = (maxWidth / 118.dp).toInt().coerceIn(3, 6)
        Column(
            modifier = Modifier.padding(horizontal = PAGE_GUTTER),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { item ->
                        QuickMixTile(
                            item = item,
                            onClick = { onMixClick(item) },
                            onLongPress = onMixLongPress?.let { { it(item) } },
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickMixTile(
    item: ShelfItem,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .thumbnailBorder(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .longPressMenuClickable(onClick = onClick, onLongClick = onLongPress)
            .contextClick(onLongPress),
    ) {
        AsyncImage(
            model = item.thumbnailUrl.artworkAt(48),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1.5f
                    scaleY = 1.5f
                }
                .blur(24.dp),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.20f)),
        )
        Column(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_logo),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.94f),
                    modifier = Modifier
                        .fillMaxWidth(0.36f)
                        .aspectRatio(1f),
                )
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 9.dp),
            )
        }
    }
}

@Composable
private fun YourMixesCarousel(
    title: String,
    items: List<ShelfItem>,
    onMixClick: (ShelfItem) -> Unit,
    onMixLongPress: ((ShelfItem) -> Unit)?,
) {
    Column(Modifier.padding(bottom = 26.dp)) {
        SectionHeader(title)
        ShelfRow(
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(
                items = items,
                key = { item -> "your_mix:${item.browseId ?: item.title}" },
            ) { item ->
                YourMixCard(
                    item = item,
                    onClick = { onMixClick(item) },
                    onLongPress = onMixLongPress?.let { { it(item) } },
                    modifier = Modifier.width(236.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun YourMixCard(
    item: ShelfItem,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val palette = rememberArtworkPalette(item.thumbnailUrl, artPx = HEADER_ART_PX)
    val shape = SmoothCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .thumbnailBorder(shape)
            .background(palette.wash)
            .longPressMenuClickable(onClick = onClick, onLongClick = onLongPress)
            .contextClick(onLongPress),
    ) {
        AsyncImage(
            model = item.thumbnailUrl.artworkAt(HEADER_ART_PX),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = palette.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = localizeCardSubtitle(item.subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = palette.onBackgroundVariant,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MixesSkeleton() {
    Column(
        modifier = Modifier
            .padding(horizontal = PAGE_GUTTER)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(3) {
                    ShimmerBox(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        RoundedCornerShape(14.dp),
                    )
                }
            }
        }
    }
}

internal data class GroupedMixes(
    val quickMixes: List<ShelfItem>,
    val yourMixes: List<ShelfItem>,
    val moodShelves: List<Pair<String, List<ShelfItem>>>,
) {
    val isEmpty: Boolean get() = quickMixes.isEmpty() && yourMixes.isEmpty() && moodShelves.isEmpty()
}

private val SIGNATURE_MIX_TITLES = listOf(
    "my supermix",
    "discover mix",
    "new release mix",
    "replay mix",
    "archive mix",
    "liked music",
)

private val MOOD_SUPERMIX_REGEX = Regex("""^(.+?)\s+supermix$""", RegexOption.IGNORE_CASE)
private val NUMBERED_MIX_REGEX = Regex("""^(.+?)\s+mix\s*(\d+)$""", RegexOption.IGNORE_CASE)

internal fun groupMixes(mixes: List<KeyedMix>): GroupedMixes {
    val unique = mixes.distinctBy { it.item.browseId ?: it.item.title }
    val quick = mutableListOf<Pair<Int, ShelfItem>>()
    val yours = mutableListOf<ShelfItem>()
    val moods = LinkedHashMap<String, Pair<String, MutableList<Pair<Int, ShelfItem>>>>()
    fun addMood(name: String, order: Int, item: ShelfItem) {
        moods.getOrPut(name.lowercase()) { name to mutableListOf() }.second += order to item
    }
    unique.forEach { mix ->
        val key = mix.englishTitle.trim()
        val rank = SIGNATURE_MIX_TITLES.indexOf(key.lowercase())
        val supermix = MOOD_SUPERMIX_REGEX.matchEntire(key)
        val numbered = NUMBERED_MIX_REGEX.matchEntire(key)
        when {
            rank >= 0 -> quick += rank to mix.item
            supermix != null -> addMood(supermix.groupValues[1], 0, mix.item)
            numbered != null && !numbered.groupValues[1].equals("My", ignoreCase = true) -> {
                val order = numbered.groupValues[2].toIntOrNull() ?: Int.MAX_VALUE
                addMood(numbered.groupValues[1], order, mix.item)
            }
            else -> yours += mix.item
        }
    }
    val englishByItem = unique.associate { it.item to it.englishTitle.trim() }
    return GroupedMixes(
        quickMixes = quick.sortedBy { it.first }.map { it.second },
        yourMixes = yours,
        moodShelves = moods.values.map { (englishName, members) ->
            val sorted = members.sortedBy { it.first }
            resolveMoodHeading(englishName, sorted, englishByItem) to sorted.map { it.second }
        },
    )
}

private fun resolveMoodHeading(
    englishName: String,
    members: List<Pair<Int, ShelfItem>>,
    englishByItem: Map<ShelfItem, String>,
): String {
    val shownInEnglish = members.all { (_, item) -> item.title.trim() == englishByItem[item] }
    if (shownInEnglish) return englishName
    val numbered = members.filter { it.first > 0 }.map { it.second.title.trim() }
    val heading = when {
        numbered.size >= 2 -> numbered.reduce { acc, title -> acc.commonPrefixWith(title) }
        numbered.size == 1 -> numbered.single()
        else -> members.firstOrNull()?.second?.title?.trim().orEmpty()
    }.trimEnd { !it.isLetter() }
    return heading.takeIf { it.length >= 2 }
        ?: members.firstOrNull()?.second?.title?.trim()
        ?: englishName
}
