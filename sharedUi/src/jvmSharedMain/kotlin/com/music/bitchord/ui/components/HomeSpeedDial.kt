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
package com.music.bitchord.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.music.bitchord.data.model.CARD_ART_PX
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.data.settings.SPEED_DIAL_SLOT_LUCKY
import com.music.bitchord.data.settings.SpeedDialLayoutMode
import com.music.bitchord.data.settings.SpeedDialTileStyle
import com.music.bitchord.data.settings.YourSpaceDialTab
import com.music.bitchord.data.settings.computeDialSlots
import com.music.bitchord.data.settings.speedDialStableId
import com.music.bitchord.sharedui.resources.Res
import com.music.bitchord.sharedui.resources.customize_your_space
import com.music.bitchord.sharedui.resources.your_space
import com.music.bitchord.sharedui.resources.your_space_artists_empty
import com.music.bitchord.sharedui.resources.your_space_mixes_empty
import com.music.bitchord.sharedui.resources.your_space_tab_artists
import com.music.bitchord.sharedui.resources.your_space_tab_mixes
import com.music.bitchord.sharedui.resources.your_space_tab_space
import com.music.bitchord.ui.AppUi
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import com.music.bitchord.ui.icons.BitChordIcons
import com.music.bitchord.ui.player.PlayerPlatform
import com.music.bitchord.ui.screens.CompactTrackRow
import com.music.bitchord.ui.theme.ArtworkPalette
import com.music.bitchord.ui.theme.rememberArtworkPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sin

private const val ARTWORK_COLUMNS = 3
private val ARTWORK_GAP = 8.dp
private val EDGE_PAGE_ZONE = 36.dp

private const val SWIPE_TILT_PER_PAGE_SPEED = 4.5f
private const val SWIPE_TILT_MAX_DEGREES = 10f
private const val SWIPE_TILT_CAMERA_DISTANCE = 16f

private const val ART_WIPE_MS = 850
private const val ART_WIPE_FEATHER = 0.25f

private const val WIGGLE_SHRINK = 0.05f
private const val WIGGLE_DEGREES = 1.4f
private const val WIGGLE_SPEED = 18f
private const val WIGGLE_TAU = 6.2831855f
private const val WIGGLE_DIM = 0.48f

/**
 * Section header for Your Space: title, quick layout switcher, live Customize trigger,
 * and subtle tappable dial-tab pills (`Space` · `Mixes` · optional `Artists`).
 */
@Composable
fun SpeedDialSectionHeader(
    layoutMode: SpeedDialLayoutMode,
    onToggleLayoutMode: () -> Unit,
    modifier: Modifier = Modifier,
    tabs: List<YourSpaceDialTab> = listOf(YourSpaceDialTab.SPACE),
    activeTab: YourSpaceDialTab = YourSpaceDialTab.SPACE,
    onTabSelect: (YourSpaceDialTab) -> Unit = {},
    onOpenCustomize: (() -> Unit)? = null,
) {
    val haptics = rememberHaptics()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_GUTTER, vertical = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        haptics.play(Haptic.Tap)
                        onToggleLayoutMode()
                    },
                    modifier = Modifier.size(48.dp),
                ) {
                    val icon = when (layoutMode) {
                        SpeedDialLayoutMode.TWO_BY_THREE -> BitChordIcons.GridView
                        SpeedDialLayoutMode.THREE_BY_THREE,
                        SpeedDialLayoutMode.THREE_BY_FOUR,
                        -> Icons.Rounded.Apps
                        SpeedDialLayoutMode.ONE_BY_FOUR_LIST -> BitChordIcons.ListView
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = "Switch layout: ${layoutMode.label}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
                if (onOpenCustomize != null) {
                    val customizeLabel = stringResource(Res.string.customize_your_space)
                    IconButton(
                        onClick = {
                            haptics.play(Haptic.Tap)
                            onOpenCustomize()
                        },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = customizeLabel,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }

        if (tabs.size > 1) {
            Spacer(Modifier.height(2.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEach { tab ->
                    val selected = tab == activeTab
                    val label = when (tab) {
                        YourSpaceDialTab.SPACE -> stringResource(Res.string.your_space_tab_space)
                        YourSpaceDialTab.MIXES -> stringResource(Res.string.your_space_tab_mixes)
                        YourSpaceDialTab.ARTISTS -> stringResource(Res.string.your_space_tab_artists)
                    }
                    val pillBg = when {
                        selected -> MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.22f else 0.14f)
                        isDark -> Color.White.copy(alpha = 0.06f)
                        else -> Color.Black.copy(alpha = 0.05f)
                    }
                    val pillBorder = when {
                        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                        isDark -> Color.White.copy(alpha = 0.08f)
                        else -> Color.Black.copy(alpha = 0.06f)
                    }
                    val textColor = if (selected) {
                        if (isDark) Color.White else MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Box(
                        modifier = Modifier
                            .heightIn(min = 34.dp)
                            .clip(RoundedCornerShape(50))
                            .background(pillBg)
                            .border(0.75.dp, pillBorder, RoundedCornerShape(50))
                            .clickable(role = Role.Tab) {
                                if (!selected) {
                                    haptics.play(Haptic.Select)
                                    onTabSelect(tab)
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                letterSpacing = 0.1.sp,
                            ),
                            color = textColor,
                            maxLines = 1,
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

/**
 * Adaptive Speed Dial / Your Space host supporting:
 * - Multi-dial horizontal swipe + header pill switching (`Space`, `Mixes`, optional `Artists`)
 * - Full-bleed 3-column square artwork grids (`3×3` and `3×4`) with selectable edge treatments,
 *   drag-to-reorder pins, velocity-reactive swipe tilt, diagonal staggered entrance, smooth art
 *   wipe, and ChordPlay's original **Chord Wave** discovery animation
 * - Classic `2×3` compact cards and `1×4` horizontal track list modes
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
    layoutMode: SpeedDialLayoutMode = SpeedDialLayoutMode.THREE_BY_THREE,
    onItemLongPress: ((ShelfItem) -> Unit)? = null,
    onPlayMixClick: ((ShelfItem) -> Unit)? = null,
    mixesItems: List<ShelfItem>? = null,
    artistItems: List<ShelfItem> = emptyList(),
    activeTab: YourSpaceDialTab = YourSpaceDialTab.SPACE,
    onTabChange: (YourSpaceDialTab) -> Unit = {},
    onLuckyDialRoll: ((onLanded: () -> Unit) -> Unit)? = null,
    onReorderPinned: ((List<ShelfItem>) -> Unit)? = null,
) {
    val showMixes by AppUi.host.speedDialShowMixes.collectAsStateWithLifecycle()
    val showArtists by AppUi.host.speedDialShowArtists.collectAsStateWithLifecycle()
    val showChordWave by AppUi.host.speedDialShowChordWave.collectAsStateWithLifecycle()
    val tileStyle by AppUi.host.speedDialTileStyle.collectAsStateWithLifecycle()
    val showLabels by AppUi.host.speedDialShowLabels.collectAsStateWithLifecycle()
    val labelOverrides by AppUi.host.speedDialLabelOverrides.collectAsStateWithLifecycle()
    val showPinBadge by AppUi.host.speedDialShowPinBadge.collectAsStateWithLifecycle()
    val reduceAnimation by PlayerPlatform.host.settings.reduceAnimation.collectAsStateWithLifecycle()

    val tabs = remember(showMixes, showArtists) {
        buildList {
            add(YourSpaceDialTab.SPACE)
            if (showMixes) add(YourSpaceDialTab.MIXES)
            if (showArtists) add(YourSpaceDialTab.ARTISTS)
        }
    }

    if (items.isEmpty() && !showMixes && !showArtists) return

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val isDynamicTheme by AppUi.host.isDynamicTheme.collectAsStateWithLifecycle()
    val colorScheme = MaterialTheme.colorScheme
    val themePalette = remember(colorScheme) {
        ArtworkPalette(
            background = colorScheme.background,
            wash = colorScheme.surface,
            elevated = colorScheme.surfaceVariant,
            accent = colorScheme.primary,
            onBackground = colorScheme.onBackground,
            onBackgroundVariant = colorScheme.onSurfaceVariant,
            divider = colorScheme.outline,
        )
    }
    val activePalette = if (isDynamicTheme) {
        rememberArtworkPalette(
            imageUrl = currentSong?.thumbnailUrl,
            dark = isDark,
        )
    } else {
        themePalette
    }

    val rollHandler: (onLanded: () -> Unit) -> Unit = remember(onLuckyDialRoll, onLuckyDialClick) {
        { onLanded ->
            if (onLuckyDialRoll != null) {
                onLuckyDialRoll(onLanded)
            } else {
                onLuckyDialClick()
                onLanded()
            }
        }
    }

    val spacePinnedIds = remember(pinnedIds) {
        pinnedIds.mapTo(HashSet()) { it.removePrefix("VL") } + pinnedIds
    }

    if (tabs.size == 1) {
        YourSpaceTabContent(
            tab = YourSpaceDialTab.SPACE,
            spaceItems = items,
            mixesItems = mixesItems,
            artistItems = artistItems,
            pinnedIds = spacePinnedIds,
            currentSong = currentSong,
            isPlaying = isPlaying,
            palette = activePalette,
            layoutMode = layoutMode,
            tileStyle = tileStyle,
            showLabels = showLabels,
            labelOverrides = labelOverrides,
            showPinBadge = showPinBadge,
            showChordWave = showChordWave,
            reduceAnimation = reduceAnimation,
            onItemClick = onItemClick,
            onItemLongPress = onItemLongPress,
            onPlayMixClick = onPlayMixClick,
            onLuckyDialRoll = rollHandler,
            onReorderPinned = onReorderPinned,
            modifier = modifier,
        )
        return
    }

    val outerPagerState = rememberPagerState(
        initialPage = tabs.indexOf(activeTab).coerceAtLeast(0),
        pageCount = { tabs.size },
    )

    LaunchedEffect(activeTab, tabs) {
        val target = tabs.indexOf(activeTab)
        if (target >= 0 && outerPagerState.currentPage != target && !outerPagerState.isScrollInProgress) {
            outerPagerState.animateScrollToPage(
                target,
                animationSpec = if (reduceAnimation) spring(stiffness = Spring.StiffnessHigh) else spring(stiffness = Spring.StiffnessMediumLow),
            )
        }
    }

    LaunchedEffect(outerPagerState, tabs) {
        snapshotFlow { outerPagerState.settledPage }.collect { pageIdx ->
            tabs.getOrNull(pageIdx)?.let { onTabChange(it) }
        }
    }

    val minFlingPx = with(LocalDensity.current) { 400.dp.toPx() }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val handoff = remember(outerPagerState, minFlingPx, rtl) {
        DialTabFlingHandoff(outerPagerState, minFlingPx, rtl)
    }

    HorizontalPager(
        state = outerPagerState,
        verticalAlignment = Alignment.Top,
        key = { tabs[it].name },
        beyondViewportPageCount = 1,
        modifier = modifier.fillMaxWidth(),
    ) { pageIndex ->
        val tab = tabs[pageIndex]
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .nestedScroll(handoff),
        ) {
            YourSpaceTabContent(
                tab = tab,
                spaceItems = items,
                mixesItems = mixesItems,
                artistItems = artistItems,
                pinnedIds = spacePinnedIds,
                currentSong = currentSong,
                isPlaying = isPlaying,
                palette = activePalette,
                layoutMode = layoutMode,
                tileStyle = tileStyle,
                showLabels = showLabels,
                labelOverrides = labelOverrides,
                showPinBadge = showPinBadge,
                showChordWave = showChordWave,
                reduceAnimation = reduceAnimation,
                onItemClick = onItemClick,
                onItemLongPress = onItemLongPress,
                onPlayMixClick = onPlayMixClick,
                onLuckyDialRoll = rollHandler,
                onReorderPinned = if (tab == YourSpaceDialTab.SPACE) onReorderPinned else null,
            )
        }
    }
}

@Composable
private fun YourSpaceTabContent(
    tab: YourSpaceDialTab,
    spaceItems: List<ShelfItem>,
    mixesItems: List<ShelfItem>?,
    artistItems: List<ShelfItem>,
    pinnedIds: Set<String>,
    currentSong: Song?,
    isPlaying: Boolean,
    palette: ArtworkPalette,
    layoutMode: SpeedDialLayoutMode,
    tileStyle: SpeedDialTileStyle,
    showLabels: Boolean,
    labelOverrides: Set<String>,
    showPinBadge: Boolean,
    showChordWave: Boolean,
    reduceAnimation: Boolean,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
    onPlayMixClick: ((ShelfItem) -> Unit)?,
    onLuckyDialRoll: (onLanded: () -> Unit) -> Unit,
    onReorderPinned: ((List<ShelfItem>) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val artworkRows = layoutMode.gridRows
    when (tab) {
        YourSpaceDialTab.SPACE -> {
            if (spaceItems.isEmpty()) return
            when (layoutMode) {
                SpeedDialLayoutMode.THREE_BY_THREE,
                SpeedDialLayoutMode.THREE_BY_FOUR,
                -> HomeSpeedDialArtworkGrid(
                    items = spaceItems,
                    pinnedIds = pinnedIds,
                    rows = artworkRows,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    palette = palette,
                    tileStyle = tileStyle,
                    showLabels = showLabels,
                    labelOverrides = labelOverrides,
                    showPinBadge = showPinBadge,
                    includeLucky = showChordWave,
                    showSourceBadge = false,
                    isArtistDial = false,
                    reduceAnimation = reduceAnimation,
                    onItemClick = onItemClick,
                    onItemLongPress = onItemLongPress,
                    onLuckyDialRoll = onLuckyDialRoll,
                    onReorderPinned = onReorderPinned,
                    modifier = modifier,
                )

                SpeedDialLayoutMode.TWO_BY_THREE -> HomeSpeedDialTwoByThree(
                    items = spaceItems,
                    pinnedIds = pinnedIds,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    palette = palette,
                    showPinBadge = showPinBadge,
                    showChordWave = showChordWave,
                    reduceAnimation = reduceAnimation,
                    onItemClick = onItemClick,
                    onLuckyDialRoll = onLuckyDialRoll,
                    onItemLongPress = onItemLongPress,
                    onPlayMixClick = onPlayMixClick,
                    modifier = modifier,
                )

                SpeedDialLayoutMode.ONE_BY_FOUR_LIST -> HomeSpeedDialList(
                    items = spaceItems,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    palette = palette,
                    showChordWave = showChordWave,
                    reduceAnimation = reduceAnimation,
                    onItemClick = onItemClick,
                    onLuckyDialRoll = onLuckyDialRoll,
                    onItemLongPress = onItemLongPress,
                    modifier = modifier,
                )
            }
        }

        YourSpaceDialTab.MIXES -> {
            when {
                mixesItems == null -> ArtworkDialSkeletonGrid(rows = artworkRows, modifier = modifier)
                mixesItems.isEmpty() -> EmptyDialNotice(
                    message = stringResource(Res.string.your_space_mixes_empty),
                    modifier = modifier,
                )
                else -> HomeSpeedDialArtworkGrid(
                    items = mixesItems,
                    pinnedIds = pinnedIds,
                    rows = artworkRows,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    palette = palette,
                    tileStyle = tileStyle,
                    showLabels = showLabels,
                    labelOverrides = labelOverrides,
                    showPinBadge = showPinBadge,
                    includeLucky = false,
                    showSourceBadge = true,
                    isArtistDial = false,
                    reduceAnimation = reduceAnimation,
                    onItemClick = onItemClick,
                    onItemLongPress = onItemLongPress,
                    onLuckyDialRoll = onLuckyDialRoll,
                    onReorderPinned = null,
                    modifier = modifier,
                )
            }
        }

        YourSpaceDialTab.ARTISTS -> {
            if (artistItems.isEmpty()) {
                EmptyDialNotice(
                    message = stringResource(Res.string.your_space_artists_empty),
                    modifier = modifier,
                )
            } else {
                HomeSpeedDialArtworkGrid(
                    items = artistItems,
                    pinnedIds = pinnedIds,
                    rows = artworkRows,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    palette = palette,
                    tileStyle = tileStyle,
                    showLabels = showLabels,
                    labelOverrides = labelOverrides,
                    showPinBadge = showPinBadge,
                    includeLucky = false,
                    showSourceBadge = false,
                    isArtistDial = true,
                    reduceAnimation = reduceAnimation,
                    onItemClick = onItemClick,
                    onItemLongPress = onItemLongPress,
                    onLuckyDialRoll = onLuckyDialRoll,
                    onReorderPinned = null,
                    modifier = modifier,
                )
            }
        }
    }
}

private class DialTabFlingHandoff(
    private val state: PagerState,
    private val minFlingPx: Float,
    private val reversed: Boolean,
) : NestedScrollConnection {
    override suspend fun onPreFling(available: Velocity): Velocity {
        val offset = state.currentPageOffsetFraction
        if (abs(offset) < 0.001f) return Velocity.Zero
        val position = state.currentPage + offset
        val velocity = if (reversed) -available.x else available.x
        val target = when {
            velocity <= -minFlingPx -> ceil(position)
            velocity >= minFlingPx -> floor(position)
            else -> round(position)
        }.toInt().coerceIn(0, (state.pageCount - 1).coerceAtLeast(0))
        state.animateScrollToPage(target, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
        return available
    }
}

@Composable
private fun EmptyDialNotice(
    message: String,
    modifier: Modifier = Modifier,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_GUTTER, vertical = 8.dp)
            .height(176.dp)
            .clip(SmoothCornerShape(18.dp))
            .background(if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.04f))
            .border(
                0.75.dp,
                if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                SmoothCornerShape(18.dp),
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ArtworkDialSkeletonGrid(
    rows: Int,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_GUTTER, vertical = 6.dp),
    ) {
        val tileSide = (maxWidth - ARTWORK_GAP * (ARTWORK_COLUMNS - 1)) / ARTWORK_COLUMNS
        Column(verticalArrangement = Arrangement.spacedBy(ARTWORK_GAP)) {
            repeat(rows) {
                Row(horizontalArrangement = Arrangement.spacedBy(ARTWORK_GAP)) {
                    repeat(ARTWORK_COLUMNS) {
                        ShimmerBox(
                            modifier = Modifier.size(tileSide),
                            shape = SmoothCornerShape(14.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Full-bleed 3-column square artwork grid for `3×3` and `3×4` modes, Mixes Dial, and Artist Dial.
 * Includes in-grid drag-to-reorder for pinned items, velocity-reactive swipe tilt, staggered
 * diagonal entrance, and smooth diagonal cover wipes.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeSpeedDialArtworkGrid(
    items: List<ShelfItem>,
    pinnedIds: Set<String>,
    rows: Int,
    currentSong: Song?,
    isPlaying: Boolean,
    palette: ArtworkPalette,
    tileStyle: SpeedDialTileStyle,
    showLabels: Boolean,
    labelOverrides: Set<String>,
    showPinBadge: Boolean,
    includeLucky: Boolean,
    showSourceBadge: Boolean,
    isArtistDial: Boolean,
    reduceAnimation: Boolean,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
    onLuckyDialRoll: (onLanded: () -> Unit) -> Unit,
    onReorderPinned: ((List<ShelfItem>) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val perPage = ARTWORK_COLUMNS * rows
    var order by remember(items) { mutableStateOf(items) }
    val pinnedCount = remember(order, pinnedIds) {
        order.takeWhile { it.speedDialStableId() in pinnedIds }.size
    }
    val slots = remember(order.size, pinnedCount, perPage, includeLucky) {
        computeDialSlots(order.size, pinnedCount, perPage, includeLucky)
    }
    val pageCount = (slots.size / perPage).coerceAtLeast(1)

    val pagerState = rememberPagerState(pageCount = { pageCount })
    val swipeTilt = rememberSwipeTilt(pagerState, reduceAnimation)

    val latestSlots by rememberUpdatedState(slots)
    val latestOrder by rememberUpdatedState(order)
    val latestItems by rememberUpdatedState(items)
    val latestPinnedCount by rememberUpdatedState(pinnedCount)
    val latestPageCount by rememberUpdatedState(pageCount)
    val latestReorder by rememberUpdatedState(onReorderPinned)
    val latestMenu by rememberUpdatedState(onItemLongPress)

    var dragId by remember { mutableStateOf<String?>(null) }
    var dragPos by remember { mutableStateOf(Offset.Zero) }
    var grab by remember { mutableStateOf(Offset.Zero) }
    var edge by remember { mutableIntStateOf(0) }

    val isReordering = dragId != null
    val wiggleLevel by animateFloatAsState(
        targetValue = if (isReordering && !reduceAnimation) 1f else 0f,
        animationSpec = tween(200),
        label = "artworkWiggle",
    )
    var wiggleClock by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isReordering, reduceAnimation) {
        if (!isReordering || reduceAnimation) return@LaunchedEffect
        val start = withFrameMillis { it }
        while (true) {
            withFrameMillis { wiggleClock = (it - start) / 1000f }
        }
    }

    LaunchedEffect(edge, dragId) {
        if (dragId == null || edge == 0) return@LaunchedEffect
        while (true) {
            delay(550L)
            val target = (pagerState.currentPage + edge).coerceIn(0, latestPageCount - 1)
            if (target != pagerState.currentPage) {
                pagerState.animateScrollToPage(target)
            }
            delay(380L)
        }
    }

    val enteredIds = remember { HashSet<String>() }
    val artLoaded = remember(items) { mutableStateMapOf<String, Boolean>() }
    val density = LocalDensity.current
    val haptics = rememberHaptics()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val contentWidth = maxWidth - PAGE_GUTTER * 2
            val tileSide = ((contentWidth - ARTWORK_GAP * (ARTWORK_COLUMNS - 1)) / ARTWORK_COLUMNS).coerceAtLeast(64.dp)
            val gridHeight = tileSide * rows + ARTWORK_GAP * (rows - 1)

            val tilePx = with(density) { tileSide.toPx() }
            val gapPx = with(density) { ARTWORK_GAP.toPx() }
            val gutterPx = with(density) { PAGE_GUTTER.toPx() }
            val widthPx = with(density) { maxWidth.toPx() }
            val edgePx = with(density) { EDGE_PAGE_ZONE.toPx() }

            fun cellOrigin(cell: Int) = Offset(
                (cell % ARTWORK_COLUMNS) * (tilePx + gapPx),
                (cell / ARTWORK_COLUMNS) * (tilePx + gapPx),
            )

            fun toOrderIndex(shownIndex: Int): Int? =
                latestSlots.getOrNull(shownIndex)?.takeIf { it >= 0 }

            fun orderTargetAt(shownIndex: Int): Int =
                toOrderIndex(shownIndex)
                    ?: (0 until shownIndex.coerceAtMost(latestSlots.size)).count { latestSlots[it] >= 0 }

            fun hitCell(pos: Offset, page: Int): Int? {
                val x = pos.x - gutterPx
                val col = floor(x / (tilePx + gapPx)).toInt()
                val row = floor(pos.y / (tilePx + gapPx)).toInt()
                if (col !in 0 until ARTWORK_COLUMNS || row !in 0 until rows) return null
                if (x - col * (tilePx + gapPx) > tilePx || pos.y - row * (tilePx + gapPx) > tilePx) return null
                return (page * perPage + row * ARTWORK_COLUMNS + col).takeIf {
                    it < latestSlots.size && latestSlots[it] >= 0
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(rows, tilePx, gapPx, gutterPx) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            val endedEarly = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                var done = false
                                while (!done) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                    done = change == null || change.changedToUp() ||
                                        event.changes.size > 1 ||
                                        (change.position - down.position).getDistance() > viewConfiguration.touchSlop
                                }
                                true
                            }
                            if (endedEarly != null) return@awaitEachGesture

                            val page = pagerState.currentPage
                            val shownIdx = hitCell(down.position, page) ?: return@awaitEachGesture
                            val orderIdx = toOrderIndex(shownIdx) ?: return@awaitEachGesture
                            val reorderCallback = latestReorder ?: return@awaitEachGesture
                            if (orderIdx >= latestPinnedCount) return@awaitEachGesture

                            val heldItem = latestOrder.getOrNull(orderIdx) ?: return@awaitEachGesture
                            val heldId = heldItem.speedDialStableId()
                            haptics.play(Haptic.Select)
                            dragId = heldId
                            dragPos = down.position
                            grab = down.position - cellOrigin(shownIdx - page * perPage) - Offset(gutterPx, 0f)

                            var moved = false
                            var committed = false
                            try {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    val lifted = !change.pressed
                                    change.consume()
                                    if (lifted) {
                                        committed = true
                                        if (moved) {
                                            haptics.play(Haptic.Select)
                                            reorderCallback(latestOrder.take(latestPinnedCount))
                                        } else {
                                            latestMenu?.invoke(heldItem)
                                        }
                                        break
                                    }
                                    dragPos = change.position
                                    if (!moved && (change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                        moved = true
                                    }
                                    if (moved && !pagerState.isScrollInProgress) {
                                        val x = dragPos.x - gutterPx
                                        val col = floor(x / (tilePx + gapPx)).toInt().coerceIn(0, ARTWORK_COLUMNS - 1)
                                        val row = floor(dragPos.y / (tilePx + gapPx)).toInt().coerceIn(0, rows - 1)
                                        val targetShown = pagerState.currentPage * perPage + row * ARTWORK_COLUMNS + col
                                        val targetOrder = orderTargetAt(targetShown)
                                            .coerceIn(0, (latestPinnedCount - 1).coerceAtLeast(0))
                                        val currentIdx = latestOrder.indexOfFirst { it.speedDialStableId() == heldId }
                                        if (currentIdx >= 0 && targetOrder != currentIdx) {
                                            order = latestOrder.toMutableList().apply { add(targetOrder, removeAt(currentIdx)) }
                                            haptics.play(Haptic.Tick)
                                        }
                                    }
                                    edge = when {
                                        !moved -> 0
                                        dragPos.x < edgePx -> -1
                                        dragPos.x > widthPx - edgePx -> 1
                                        else -> 0
                                    }
                                }
                            } finally {
                                if (!committed) order = latestItems
                                dragId = null
                                edge = 0
                            }
                        }
                    },
            ) {
                HorizontalPager(
                    state = pagerState,
                    pageSpacing = 12.dp,
                    contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                    verticalAlignment = Alignment.Top,
                    userScrollEnabled = dragId == null,
                    modifier = Modifier.fillMaxWidth(),
                ) { page ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(gridHeight),
                    ) {
                        for (cell in 0 until perPage) {
                            val slotIdx = page * perPage + cell
                            if (slotIdx >= slots.size) continue
                            val slotVal = slots[slotIdx]
                            if (slotVal == -1) continue

                            if (slotVal == SPEED_DIAL_SLOT_LUCKY) {
                                val origin = cellOrigin(cell)
                                val entering = remember("lucky_wave") {
                                    enteredIds.add("lucky_wave") && page == pagerState.currentPage && !reduceAnimation
                                }
                                val appear = remember { Animatable(if (entering) 0f else 1f) }
                                LaunchedEffect(appear) {
                                    if (appear.value >= 1f) return@LaunchedEffect
                                    delay((cell / ARTWORK_COLUMNS + cell % ARTWORK_COLUMNS) * 40L)
                                    appear.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
                                }
                                ChordWaveArtworkTile(
                                    palette = palette,
                                    tileStyle = tileStyle,
                                    showLabel = showLabels,
                                    reduceAnimation = reduceAnimation,
                                    onRoll = onLuckyDialRoll,
                                    dim = { if (isReordering) WIGGLE_DIM * wiggleLevel else 0f },
                                    modifier = Modifier
                                        .offset { IntOffset(origin.x.roundToInt(), origin.y.roundToInt()) }
                                        .size(tileSide)
                                        .graphicsLayer {
                                            val s = if (entering) 0.93f + 0.07f * appear.value else 1f
                                            scaleX = s
                                            scaleY = s
                                            alpha = appear.value.coerceIn(0f, 1f)
                                            rotationY = swipeTilt.value
                                            cameraDistance = SWIPE_TILT_CAMERA_DISTANCE * this.density
                                        },
                                )
                                continue
                            }

                            val item = order.getOrNull(slotVal) ?: continue
                            val id = item.speedDialStableId()
                            key(id) {
                                val targetOrigin = cellOrigin(cell).let { IntOffset(it.x.roundToInt(), it.y.roundToInt()) }
                                val animatedOffset by animateIntOffsetAsState(
                                    targetValue = targetOrigin,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                    label = "dialArtworkOffset",
                                )

                                val entering = remember(id) {
                                    enteredIds.add(id) && page == pagerState.currentPage && !reduceAnimation
                                }
                                val appear = remember(id) { Animatable(if (entering) 0f else 1f) }
                                LaunchedEffect(appear) {
                                    if (appear.value >= 1f) return@LaunchedEffect
                                    withTimeoutOrNull(450L) {
                                        snapshotFlow { artLoaded[id] == true }.first { it }
                                    }
                                    delay((cell / ARTWORK_COLUMNS + cell % ARTWORK_COLUMNS) * 40L)
                                    appear.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
                                }

                                val isPinned = id in pinnedIds
                                val phase = remember(id) { (id.hashCode() and 0xFFFF) / 65535f * WIGGLE_TAU }

                                ArtworkSpeedDialTile(
                                    item = item,
                                    palette = palette,
                                    tileStyle = tileStyle,
                                    isPinned = isPinned && showPinBadge,
                                    isCurrent = isItemCurrent(item, currentSong),
                                    isPlaying = isPlaying,
                                    showLabel = showLabels != (id in labelOverrides),
                                    showSourceBadge = showSourceBadge,
                                    isArtistTile = isArtistDial || item.detectType() == SpeedDialItemType.ARTIST,
                                    reduceAnimation = reduceAnimation,
                                    onArtLoaded = { artLoaded[id] = true },
                                    dim = { if (isReordering && !isPinned) WIGGLE_DIM * wiggleLevel else 0f },
                                    onClick = { onItemClick(item) },
                                    onLongPress = if (onReorderPinned != null && isPinned) {
                                        null
                                    } else {
                                        onItemLongPress?.let { { it(item) } }
                                    },
                                    modifier = Modifier
                                        .offset { animatedOffset }
                                        .size(tileSide)
                                        .graphicsLayer {
                                            val shown = appear.value
                                            var s = if (entering) 0.93f + 0.07f * shown else 1f
                                            val w = wiggleLevel
                                            if (w > 0f && isPinned) {
                                                s *= 1f - WIGGLE_SHRINK * w
                                                rotationZ = sin(wiggleClock * WIGGLE_SPEED + phase) * WIGGLE_DEGREES * w
                                            } else {
                                                rotationZ = 0f
                                            }
                                            scaleX = s
                                            scaleY = s
                                            alpha = if (dragId == id) 0f else shown.coerceIn(0f, 1f)
                                            rotationY = swipeTilt.value
                                            cameraDistance = SWIPE_TILT_CAMERA_DISTANCE * this.density
                                        },
                                )
                            }
                        }
                    }
                }

                // Floating lifted tile following pointer while dragging a pinned item
                val liftedId = dragId
                if (liftedId != null) {
                    order.firstOrNull { it.speedDialStableId() == liftedId }?.let { draggedItem ->
                        ArtworkSpeedDialTile(
                            item = draggedItem,
                            palette = palette,
                            tileStyle = tileStyle,
                            isPinned = showPinBadge,
                            isCurrent = false,
                            isPlaying = false,
                            showLabel = showLabels != (liftedId in labelOverrides),
                            showSourceBadge = showSourceBadge,
                            isArtistTile = isArtistDial || draggedItem.detectType() == SpeedDialItemType.ARTIST,
                            reduceAnimation = reduceAnimation,
                            onClick = {},
                            onLongPress = null,
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        (dragPos.x - grab.x).roundToInt(),
                                        (dragPos.y - grab.y).roundToInt(),
                                    )
                                }
                                .size(tileSide)
                                .graphicsLayer {
                                    scaleX = 1.07f
                                    scaleY = 1.07f
                                }
                                .shadow(14.dp, SmoothCornerShape(14.dp)),
                        )
                    }
                }
            }
        }

        // Page indicator dots
        if (pageCount > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(pageCount) { pageIdx ->
                    val selected = pagerState.currentPage == pageIdx
                    Box(
                        modifier = Modifier
                            .size(width = if (selected) 16.dp else 5.dp, height = 5.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) palette.accent
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtworkSpeedDialTile(
    item: ShelfItem,
    palette: ArtworkPalette,
    tileStyle: SpeedDialTileStyle,
    isPinned: Boolean,
    isCurrent: Boolean,
    isPlaying: Boolean,
    showLabel: Boolean,
    showSourceBadge: Boolean,
    isArtistTile: Boolean,
    reduceAnimation: Boolean,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)?,
    modifier: Modifier = Modifier,
    dim: () -> Float = { 0f },
    onArtLoaded: (() -> Unit)? = null,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val shape = SmoothCornerShape(14.dp)
    val type = item.detectType()
    val placeholderBg = mutedSpeedDialSurface(
        palette = palette,
        isDark = isDark,
        isCurrent = isCurrent,
        fallbackSurface = MaterialTheme.colorScheme.surfaceVariant,
    )
    val placeholderTint = (if (isDark) Color.White else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.45f)
    val candidateUrl = item.thumbnailUrl?.artworkAt(CARD_ART_PX) ?: item.thumbnailUrl

    val clickModifier = if (onLongPress != null) {
        Modifier
            .longPressMenuClickable(onClick = onClick, onLongClick = onLongPress)
            .contextClick(onLongPress)
    } else {
        Modifier.clickable(onClick = onClick)
    }

    Box(
        modifier = modifier
            .press3D(enabled = !reduceAnimation)
            .clip(shape)
            .background(placeholderBg)
            .speedDialTileEdge(
                style = tileStyle,
                shape = shape,
                accent = palette.accent,
                isDark = isDark,
                isCurrent = isCurrent,
            )
            .semantics { contentDescription = item.title }
            .then(clickModifier),
    ) {
        // Fallback icon behind artwork
        Icon(
            imageVector = when {
                isArtistTile -> Icons.Rounded.Person
                type == SpeedDialItemType.LIVE -> Icons.Rounded.Sensors
                type == SpeedDialItemType.ALBUM -> Icons.Rounded.Album
                type == SpeedDialItemType.MIX -> Icons.Rounded.Casino
                type == SpeedDialItemType.PLAYLIST -> BitChordIcons.Playlist
                else -> Icons.Rounded.LibraryMusic
            },
            contentDescription = null,
            tint = placeholderTint,
            modifier = Modifier
                .align(Alignment.Center)
                .size(26.dp),
        )

        if (!candidateUrl.isNullOrBlank()) {
            SpeedDialArtworkImage(
                url = candidateUrl,
                contentDescription = null,
                reduceAnimation = reduceAnimation,
                onLoaded = onArtLoaded,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LaunchedEffect(Unit) { onArtLoaded?.invoke() }
        }

        // Bottom vertical scrim + title
        if (showLabel && item.title.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.72f),
                            ),
                        ),
                    ),
            )
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.15).sp,
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }

        // Top-start tag: Mix Source Badge (Mixes Dial) or LIVE pill
        if (showSourceBadge) {
            MixSourceBadge(
                source = mixBadgeSourceOf(item),
                artworkUrl = candidateUrl,
                modifier = Modifier.align(Alignment.TopStart),
            )
        } else if (type == SpeedDialItemType.LIVE) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.Black.copy(alpha = 0.74f))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF3B30)),
                )
                Text(
                    text = "LIVE",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 9.sp,
                )
            }
        }

        // Top-end subtle pin badge
        if (isPinned) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(19.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.56f))
                    .border(0.6.dp, Color.White.copy(alpha = 0.22f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = BitChordIcons.Pin,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(10.5.dp),
                )
            }
        }

        // Centered equalizer bars when playing
        if (isCurrent && isPlaying) {
            SearchPlayingBars(modifier = Modifier.align(Alignment.Center))
        }

        // Dim overlay for unpinned tiles while reordering
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val d = dim()
                    if (d > 0f) drawRect(Color.Black.copy(alpha = d))
                },
        )
    }
}

/**
 * Tile artwork renderer with smooth diagonal wipe when dynamic mix covers update, avoiding
 * jarring grey flashes. Falls back to an instant cut when [reduceAnimation] is enabled.
 */
@Composable
private fun SpeedDialArtworkImage(
    url: String,
    contentDescription: String?,
    reduceAnimation: Boolean,
    onLoaded: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalPlatformContext.current
    var shownUrl by remember { mutableStateOf(url) }
    var settled by remember { mutableStateOf(true) }

    Box(modifier) {
        val currentReq = remember(shownUrl, context) {
            ImageRequest.Builder(context)
                .data(shownUrl)
                .size(CARD_ART_PX)
                .build()
        }
        AsyncImage(
            model = currentReq,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            onSuccess = {
                settled = true
                onLoaded?.invoke()
            },
            onError = {
                settled = true
                onLoaded?.invoke()
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (url != shownUrl || !settled) {
            val incomingReq = remember(url, context) {
                ImageRequest.Builder(context)
                    .data(url)
                    .size(CARD_ART_PX)
                    .build()
            }
            var loaded by remember(url) { mutableStateOf(false) }
            val wipe = remember(url) { Animatable(0f) }

            LaunchedEffect(url, loaded, reduceAnimation) {
                if (!loaded) return@LaunchedEffect
                if (reduceAnimation) {
                    wipe.snapTo(1f)
                } else {
                    wipe.animateTo(1f, tween(ART_WIPE_MS, easing = FastOutSlowInEasing))
                }
                settled = false
                shownUrl = url
            }

            AsyncImage(
                model = incomingReq,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onSuccess = { loaded = true },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        val edge = wipe.value * (1f + ART_WIPE_FEATHER) - ART_WIPE_FEATHER
                        val w = size.width
                        val h = size.height
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(Color.Black, Color.Transparent),
                                start = Offset(w * edge, h * edge),
                                end = Offset(w * (edge + ART_WIPE_FEATHER), h * (edge + ART_WIPE_FEATHER)),
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    },
            )
        }
    }
}

@Composable
private fun rememberSwipeTilt(
    pagerState: PagerState,
    reduceAnimation: Boolean,
): Animatable<Float, AnimationVector1D> {
    val tilt = remember { Animatable(0f) }
    LaunchedEffect(pagerState, reduceAnimation) {
        if (reduceAnimation) {
            tilt.snapTo(0f)
            return@LaunchedEffect
        }
        var lastPos = Float.NaN
        var lastNanos = 0L
        snapshotFlow { pagerState.currentPage + pagerState.currentPageOffsetFraction }
            .collectLatest { pos ->
                val now = System.nanoTime()
                val dt = (now - lastNanos) / 1_000_000_000f
                val velocity = if (lastPos.isNaN() || dt <= 0f || dt > 0.1f) 0f else (pos - lastPos) / dt
                lastPos = pos
                lastNanos = now
                val target = (-velocity * SWIPE_TILT_PER_PAGE_SPEED)
                    .coerceIn(-SWIPE_TILT_MAX_DEGREES, SWIPE_TILT_MAX_DEGREES)
                tilt.animateTo(target, spring(stiffness = Spring.StiffnessMedium))
                tilt.animateTo(
                    0f,
                    spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow),
                )
            }
    }
    return tilt
}

@Composable
private fun HomeSpeedDialTwoByThree(
    items: List<ShelfItem>,
    pinnedIds: Set<String>,
    currentSong: Song?,
    isPlaying: Boolean,
    palette: ArtworkPalette,
    showPinBadge: Boolean,
    showChordWave: Boolean,
    reduceAnimation: Boolean,
    onItemClick: (ShelfItem) -> Unit,
    onLuckyDialRoll: (onLanded: () -> Unit) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
    modifier: Modifier = Modifier,
    onPlayMixClick: ((ShelfItem) -> Unit)? = null,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val page1Limit = if (showChordWave) 5 else 6
    val page1Items = items.take(page1Limit)
    val remainingItems = items.drop(page1Limit)
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
                        SpeedDialRow(
                            item1 = page1Items.getOrNull(0),
                            item2 = page1Items.getOrNull(1),
                            pinnedIds = pinnedIds,
                            showPinBadge = showPinBadge,
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
                            showPinBadge = showPinBadge,
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
                                val id4 = item4.speedDialStableId()
                                SpeedDialTile(
                                    item = item4,
                                    isPinned = showPinBadge && (id4 in pinnedIds || item4.videoId in pinnedIds || item4.browseId in pinnedIds),
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

                            if (showChordWave) {
                                ChordWaveCompactCard(
                                    palette = palette,
                                    isDark = isDark,
                                    reduceAnimation = reduceAnimation,
                                    onRoll = onLuckyDialRoll,
                                    isCompact = false,
                                    modifier = Modifier.weight(1f),
                                )
                            } else {
                                val item5 = page1Items.getOrNull(5)
                                if (item5 != null) {
                                    val id5 = item5.speedDialStableId()
                                    SpeedDialTile(
                                        item = item5,
                                        isPinned = showPinBadge && (id5 in pinnedIds || item5.videoId in pinnedIds || item5.browseId in pinnedIds),
                                        isCurrent = isItemCurrent(item5, currentSong),
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
                    } else {
                        val pageItems = additionalPages.getOrNull(pageIndex - 1).orEmpty()
                        val rows = pageItems.chunked(2)
                        rows.forEach { rowItems ->
                            SpeedDialRow(
                                item1 = rowItems.getOrNull(0),
                                item2 = rowItems.getOrNull(1),
                                pinnedIds = pinnedIds,
                                showPinBadge = showPinBadge,
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

        if (totalPages > 1) {
            val indicatorAlpha by animateFloatAsState(
                targetValue = if (showIndicators) 1f else 0f,
                animationSpec = tween(durationMillis = 300),
                label = "indicatorAlpha",
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
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
    currentSong: Song?,
    isPlaying: Boolean,
    palette: ArtworkPalette,
    showChordWave: Boolean,
    reduceAnimation: Boolean,
    onItemClick: (ShelfItem) -> Unit,
    onLuckyDialRoll: (onLanded: () -> Unit) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
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
            if (showChordWave) {
                item(key = "speed_dial_list_lucky") {
                    Column(
                        modifier = Modifier
                            .width(columnWidth)
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        ChordWaveCompactCard(
                            palette = palette,
                            isDark = isDark,
                            reduceAnimation = reduceAnimation,
                            onRoll = onLuckyDialRoll,
                            isCompact = false,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
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
    showPinBadge: Boolean,
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
            val id1 = item1.speedDialStableId()
            SpeedDialTile(
                item = item1,
                isPinned = showPinBadge && (id1 in pinnedIds || item1.videoId in pinnedIds || item1.browseId in pinnedIds),
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
            val id2 = item2.speedDialStableId()
            SpeedDialTile(
                item = item2,
                isPinned = showPinBadge && (id2 in pinnedIds || item2.videoId in pinnedIds || item2.browseId in pinnedIds),
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

private enum class SpeedDialItemType {
    LIVE,
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
        isLive -> SpeedDialItemType.LIVE
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
    if (subtitle.isBlank()) return if (type == SpeedDialItemType.LIVE) "Live Station" else ""
    return when (type) {
        SpeedDialItemType.LIVE -> subtitle.removePrefix("Live • ").removePrefix("LIVE • ").trim()
        SpeedDialItemType.MIX -> subtitle.removePrefix("Mix • ").removePrefix("Radio • ").trim()
        SpeedDialItemType.SONG -> subtitle.removePrefix("Song • ").trim()
        SpeedDialItemType.ALBUM -> subtitle.removePrefix("Album • ").removePrefix("Single • ").removePrefix("EP • ").trim()
        SpeedDialItemType.PLAYLIST -> subtitle.removePrefix("Playlist • ").trim()
        SpeedDialItemType.ARTIST -> subtitle.removePrefix("Artist • ").trim()
    }
}

private fun mutedSpeedDialSurface(
    palette: ArtworkPalette,
    isDark: Boolean,
    isCurrent: Boolean,
    fallbackSurface: Color,
): Color {
    val neutralBase = if (isDark) Color(0xFF1A1B1E) else fallbackSurface
    val tintWeight = when {
        isDark && isCurrent -> 0.24f
        isDark -> 0.14f
        isCurrent -> 0.18f
        else -> 0.10f
    }
    return lerp(neutralBase, palette.elevated, tintWeight).copy(alpha = if (isDark) 0.88f else 0.92f)
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
    isCurrent: Boolean,
    isPlaying: Boolean,
    onPlayMixClick: ((ShelfItem) -> Unit)?,
    onItemClick: (ShelfItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val artContainerSize = if (isCompact) 47.dp else 53.dp
    val placeholderBg = mutedSpeedDialSurface(
        palette = palette,
        isDark = isDark,
        isCurrent = isCurrent,
        fallbackSurface = MaterialTheme.colorScheme.surfaceVariant,
    )
    val placeholderTint = (if (isDark) Color.White else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.45f)

    Box(
        modifier = modifier.size(artContainerSize),
        contentAlignment = Alignment.Center,
    ) {
        when (type) {
            SpeedDialItemType.LIVE -> {
                val liveSize = if (isCompact) 44.dp else 50.dp
                val liveShape = RoundedCornerShape(if (isCompact) 11.dp else 13.dp)
                val liveBorderBrush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFF453A).copy(alpha = 0.85f),
                        palette.accent.copy(alpha = 0.72f),
                    ),
                )
                Box(
                    modifier = Modifier
                        .size(liveSize)
                        .clip(liveShape)
                        .background(placeholderBg)
                        .border(1.3.dp, liveBorderBrush, liveShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Sensors,
                        contentDescription = null,
                        tint = placeholderTint,
                        modifier = Modifier.size(if (isCompact) 20.dp else 22.dp),
                    )
                    if (!candidateArtUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = candidateArtUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(2.5.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.74f))
                            .padding(horizontal = 3.5.dp, vertical = 1.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF3B30)),
                        )
                        Text(
                            text = "LIVE",
                            color = Color.White,
                            fontSize = 6.5.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 7.sp,
                        )
                    }
                    val broadcastAlignment = if (isPinned) Alignment.TopEnd else Alignment.BottomEnd
                    val broadcastBadgeSize = if (isCompact) 15.dp else 17.dp
                    Box(
                        modifier = Modifier
                            .align(broadcastAlignment)
                            .padding(2.5.dp)
                            .size(broadcastBadgeSize)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.76f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.20f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Sensors,
                            contentDescription = "Live station",
                            tint = Color(0xFFFF6961),
                            modifier = Modifier.size(if (isCompact) 9.5.dp else 11.dp),
                        )
                    }
                }
            }

            SpeedDialItemType.ARTIST -> {
                val avatarSize = if (isCompact) 43.dp else 49.dp
                Box(
                    modifier = Modifier
                        .size(avatarSize)
                        .clip(CircleShape)
                        .background(placeholderBg)
                        .border(1.25.dp, palette.accent.copy(alpha = 0.50f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = placeholderTint,
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
                    Box(
                        modifier = Modifier
                            .padding(start = if (isCompact) 8.dp else 9.dp)
                            .size(vinylSize)
                            .clip(CircleShape)
                            .background(Color(0xFF121417))
                            .border(0.8.dp, Color.White.copy(alpha = 0.22f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isCompact) 20.dp else 23.dp)
                                .border(0.6.dp, Color.White.copy(alpha = 0.14f), CircleShape),
                        )
                        Box(
                            modifier = Modifier
                                .size(if (isCompact) 9.dp else 11.dp)
                                .clip(CircleShape)
                                .background(palette.accent.copy(alpha = 0.90f)),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(jacketSize)
                            .clip(RoundedCornerShape(if (isCompact) 8.dp else 10.dp))
                            .background(placeholderBg)
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(if (isCompact) 8.dp else 10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Album,
                            contentDescription = null,
                            tint = placeholderTint,
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
                    Box(
                        modifier = Modifier
                            .size(width = backW, height = backH)
                            .offset(y = (-2.5).dp)
                            .clip(RoundedCornerShape(if (isCompact) 8.dp else 10.dp))
                            .background(palette.accent.copy(alpha = 0.30f))
                            .border(1.dp, palette.accent.copy(alpha = 0.45f), RoundedCornerShape(if (isCompact) 8.dp else 10.dp)),
                    )

                    Box(
                        modifier = Modifier
                            .size(frontSize)
                            .clip(RoundedCornerShape(if (isCompact) 10.dp else 12.dp))
                            .background(placeholderBg)
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(if (isCompact) 10.dp else 12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = BitChordIcons.Playlist,
                            contentDescription = null,
                            tint = placeholderTint,
                            modifier = Modifier.size(if (isCompact) 22.dp else 26.dp),
                        )
                        if (!candidateArtUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = candidateArtUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        if (!isPinned) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(3.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(Color.Black.copy(alpha = 0.72f))
                                    .padding(horizontal = 3.5.dp, vertical = 2.dp),
                            ) {
                                Icon(
                                    imageVector = BitChordIcons.Playlist,
                                    contentDescription = "Playlist",
                                    tint = Color.White.copy(alpha = 0.92f),
                                    modifier = Modifier.size(if (isCompact) 12.dp else 14.dp),
                                )
                            }
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
                        .background(placeholderBg)
                        .border(1.4.dp, palette.accent.copy(alpha = 0.85f), RoundedCornerShape(if (isCompact) 10.dp else 12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Casino,
                        contentDescription = null,
                        tint = placeholderTint,
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
                        .background(placeholderBg)
                        .border(1.dp, Color.White.copy(alpha = 0.10f), artworkShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LibraryMusic,
                        contentDescription = null,
                        tint = placeholderTint,
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

        if (isCurrent && isPlaying) {
            SearchPlayingBars(modifier = Modifier.align(Alignment.Center))
        }

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
                    tint = Color(0xFFD6B27D),
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

    val tileHeight = if (isCompact) 56.dp else 64.dp
    val outerShape = RoundedCornerShape(if (isCompact) 18.dp else 22.dp)
    val cardPadding = if (isCompact) 4.5.dp else 5.5.dp

    val cardBg = mutedSpeedDialSurface(
        palette = palette,
        isDark = isDark,
        isCurrent = isCurrent,
        fallbackSurface = MaterialTheme.colorScheme.surfaceVariant,
    )
    val cardBorder = when {
        isCurrent -> (if (isDark) palette.accent else PlayingAccent).copy(alpha = 0.42f)
        isDark -> Color.White.copy(alpha = 0.08f)
        else -> Color.Black.copy(alpha = 0.08f)
    }

    val titleColor = when {
        isCurrent -> if (isDark) palette.accent else PlayingAccent
        else -> MaterialTheme.colorScheme.onSurface
    }
    val subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)

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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(tileHeight)
            .clip(outerShape)
            .background(cardBg)
            .border(0.5.dp, cardBorder, outerShape)
            .then(clickModifier),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = cardPadding, end = if (isCompact) 8.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 7.dp else 9.dp),
        ) {
            SpeedDialArtwork(
                item = item,
                type = type,
                candidateArtUrl = candidateArtUrl,
                palette = palette,
                isDark = isDark,
                isCompact = isCompact,
                isPinned = isPinned,
                isCurrent = isCurrent,
                isPlaying = isPlaying,
                onPlayMixClick = onPlayMixClick,
                onItemClick = onItemClick,
            )

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
        }
    }
}
