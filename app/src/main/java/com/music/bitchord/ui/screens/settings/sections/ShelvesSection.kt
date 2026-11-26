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
package com.music.bitchord.ui.screens.settings.sections

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.rounded.AddToQueue
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.QueuePlayNext
import androidx.compose.material.icons.rounded.SwipeLeft
import androidx.compose.material.icons.rounded.SwipeRight
import com.music.bitchord.data.settings.SongSwipeAction
import com.music.bitchord.ui.components.AlertRule
import com.music.bitchord.ui.components.GlassAlert
import com.music.bitchord.ui.screens.settings.localizedLabel
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import com.music.bitchord.data.settings.SpeedDialLayoutMode
import com.music.bitchord.data.settings.SpeedDialSource
import com.music.bitchord.data.settings.SpeedDialTileStyle
import com.music.bitchord.ui.components.IosSwitch
import com.music.bitchord.ui.components.SmoothCornerShape
import com.music.bitchord.ui.components.YourSpaceCustomizeSheet
import com.music.bitchord.ui.components.speedDialTileEdge
import com.music.bitchord.ui.icons.BitChordIcons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.model.HomeShelfSlot
import com.music.bitchord.data.model.orderHomeShelfIds
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.screens.RowDivider
import com.music.bitchord.ui.screens.SettingsGroup
import com.music.bitchord.ui.screens.SettingsRow
import com.music.bitchord.ui.screens.SettingsSearchEmpty
import com.music.bitchord.ui.screens.SettingsSectionDivider
import com.music.bitchord.ui.screens.SettingsSubPageHeader
import com.music.bitchord.ui.screens.matchesSearch

private fun <T> List<T>.moved(from: Int, to: Int): List<T> {
    if (from !in indices || to !in indices || from == to) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}

private data class ShelfDisplayMeta(
    val title: String,
    val subtitle: String,
    val isHidden: Boolean,
)

@Composable
private fun shelfDisplayMeta(
    id: String,
    hiddenSet: Set<String>,
): ShelfDisplayMeta {
    val isHidden = id in hiddenSet
    return when (id) {
        HomeShelfSlot.SPEED_DIAL -> ShelfDisplayMeta(
            title = stringResource(R.string.shelf_speed_dial_label),
            subtitle = if (isHidden) {
                stringResource(R.string.shelves_hidden_subtitle)
            } else {
                stringResource(R.string.shelf_speed_dial_desc)
            },
            isHidden = isHidden,
        )
        HomeShelfSlot.FIND_YOUR_MOOD -> ShelfDisplayMeta(
            title = stringResource(R.string.find_your_mood),
            subtitle = if (isHidden) {
                stringResource(R.string.shelves_hidden_subtitle)
            } else {
                stringResource(R.string.shelf_find_your_mood_desc)
            },
            isHidden = isHidden,
        )
        HomeShelfSlot.MELODY -> ShelfDisplayMeta(
            title = stringResource(R.string.shelf_melody_label),
            subtitle = if (isHidden) {
                stringResource(R.string.shelves_hidden_subtitle)
            } else {
                stringResource(R.string.shelf_melody_desc)
            },
            isHidden = isHidden,
        )
        HomeShelfSlot.REPLAY -> ShelfDisplayMeta(
            title = stringResource(R.string.shelf_replay_label),
            subtitle = if (isHidden) {
                stringResource(R.string.shelves_hidden_subtitle)
            } else {
                stringResource(R.string.shelf_replay_desc)
            },
            isHidden = isHidden,
        )
        else -> ShelfDisplayMeta(
            title = if (id.equals("Quick picks", ignoreCase = true)) {
                stringResource(R.string.shelf_quick_picks_label)
            } else {
                id
            },
            subtitle = if (isHidden) {
                stringResource(R.string.shelves_hidden_subtitle)
            } else {
                stringResource(R.string.shelves_feed_row_subtitle)
            },
            isHidden = isHidden,
        )
    }
}

private const val SHELF_SWAP_THRESHOLD = 0.6f

@Composable
fun ShelvesSection(
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    homeShelfTitles: List<String> = emptyList(),
    initialSearchQuery: String = "",
    modifier: Modifier = Modifier,
) {
    val homeShelfOrder by AppSettings.homeShelfOrder.collectAsStateWithLifecycle()
    val hiddenHomeShelves by AppSettings.hiddenHomeShelves.collectAsStateWithLifecycle()
    val libraryReplayVisible by AppSettings.libraryReplayVisible.collectAsStateWithLifecycle()
    val preferMusicOnly by AppSettings.preferMusicOnly.collectAsStateWithLifecycle()
    val hideHomeVideos by AppSettings.hideHomeVideos.collectAsStateWithLifecycle()
    val squareVideoThumbnails by AppSettings.squareVideoThumbnails.collectAsStateWithLifecycle()
    val showLivePreviews by AppSettings.showSettingsLivePreviews.collectAsStateWithLifecycle()
    var searchQuery by remember(initialSearchQuery) { mutableStateOf(initialSearchQuery) }

    val orderedIds = remember(homeShelfTitles, homeShelfOrder) {
        orderHomeShelfIds(
            rawShelfTitles = homeShelfTitles,
            savedOrder = homeShelfOrder,
            includeSpeedDial = true,
            includeMelody = true,
            includeReplay = true,
            includeFindYourMood = true,
        )
    }

    var liveOrder by remember { mutableStateOf(orderedIds) }
    var draggedId by remember { mutableStateOf<String?>(null) }
    var totalDrag by remember { mutableFloatStateOf(0f) }
    var startIndex by remember { mutableIntStateOf(0) }
    var pitchPx by remember { mutableFloatStateOf(0f) }
    var lockedPitchPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(orderedIds, draggedId) {
        if (draggedId == null) {
            liveOrder = orderedIds
        }
    }

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val fallbackPitchPx = with(density) { 64.dp.toPx() }
    val outerScrollState = rememberScrollState()

    val rowsWithMeta = liveOrder.map { id ->
        id to shelfDisplayMeta(
            id = id,
            hiddenSet = hiddenHomeShelves,
        )
    }

    val filteredRows = rowsWithMeta.filter { (id, meta) ->
        matchesSearch(searchQuery, meta.title, meta.subtitle, id)
    }

    val leftSwipeCustomized by AppSettings.songSwipeLeftCustomized.collectAsStateWithLifecycle()
    val songSwipeRightAction by AppSettings.songSwipeRightAction.collectAsStateWithLifecycle()
    val songSwipeLeftAction by AppSettings.songSwipeLeftAction.collectAsStateWithLifecycle()
    var showRightSwipePicker by remember { mutableStateOf(false) }
    var showLeftSwipePicker by remember { mutableStateOf(false) }

    val showYourSpaceSection = matchesSearch(
        searchQuery,
        stringResource(R.string.your_space),
        stringResource(R.string.customize_your_space),
        "your space", "speed dial", "mixes", "chord wave", "tile", "grid",
    )
    var showYourSpaceSheet by remember { mutableStateOf(false) }

    YourSpaceCustomizeSheet(
        visible = showYourSpaceSheet,
        onDismiss = { showYourSpaceSheet = false },
    )

    val showSwipeGestures = matchesSearch(
            searchQuery,
            stringResource(R.string.swipe_gestures),
            stringResource(R.string.right_swipe_action),
            stringResource(R.string.left_swipe_action),
            "swipe", "gestures", "action",
    )
    val showPreferMusicOnly = matchesSearch(
        searchQuery,
        stringResource(R.string.prefer_music_only),
        stringResource(R.string.prefer_music_only_subtitle),
        "music only", "audio", "video", "prefer",
    )
    val showHideVideos = matchesSearch(
        searchQuery,
        stringResource(R.string.hide_home_videos),
        stringResource(R.string.hide_home_videos_subtitle),
        "hide", "videos", "home",
    )
    val showSquareVideos = matchesSearch(
        searchQuery,
        stringResource(R.string.square_video_thumbnails),
        stringResource(R.string.square_video_thumbnails_subtitle),
        "square", "thumbnail", "video", "aspect",
    )
    val showLibraryReplayToggle = matchesSearch(
        searchQuery,
        stringResource(R.string.show_replay_in_library),
        stringResource(R.string.show_replay_in_library_subtitle),
        "replay", "library", "stats", "cards",
    )
    val hasPreferencesToggles = showPreferMusicOnly || showHideVideos || showSquareVideos || showLibraryReplayToggle

    val previewItems = rowsWithMeta.filterNot { (_, meta) -> meta.isHidden }.map { (_, meta) -> meta.title }

    fun advanceDrag(deltaY: Float) {
        val id = draggedId ?: return
        val pitch = lockedPitchPx.takeIf { it > 0f } ?: fallbackPitchPx
        var index = liveOrder.indexOf(id)
        if (index < 0) return

        totalDrag = (totalDrag + deltaY).coerceIn(
            -startIndex * pitch,
            (liveOrder.lastIndex - startIndex) * pitch,
        )

        var swapped = false
        while (true) {
            val travelled = totalDrag / pitch
            val moved = (index - startIndex).toFloat()
            if (travelled > moved + SHELF_SWAP_THRESHOLD && index < liveOrder.lastIndex) {
                liveOrder = liveOrder.moved(index, index + 1)
                index++
                swapped = true
            } else if (travelled < moved - SHELF_SWAP_THRESHOLD && index > 0) {
                liveOrder = liveOrder.moved(index, index - 1)
                index--
                swapped = true
            } else {
                break
            }
        }
        if (swapped) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    if (showRightSwipePicker) {
        SwipeActionPickerDialog(
            title = stringResource(R.string.right_swipe_action),
            currentAction = songSwipeRightAction,
            onSelect = { action ->
                AppSettings.setSongSwipeRightAction(action)
                if (action == SongSwipeAction.PLAY_NEXT) {
                    AppSettings.setSwipeToPlayNext(true)
                } else if (action == SongSwipeAction.ADD_TO_QUEUE) {
                    AppSettings.setSwipeToPlayNext(false)
                }
            },
            onDismiss = { showRightSwipePicker = false },
        )
    }

    if (showLeftSwipePicker) {
        SwipeActionPickerDialog(
            title = stringResource(R.string.left_swipe_action),
            currentAction = songSwipeLeftAction.takeIf { leftSwipeCustomized },
            onReset = AppSettings::resetSongSwipeLeftAction,
            onSelect = { action ->
                AppSettings.setSongSwipeLeftAction(action)
            },
            onDismiss = { showLeftSwipePicker = false },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(outerScrollState, enabled = draggedId == null)
            .padding(contentPadding),
    ) {
        SettingsSubPageHeader(
            title = stringResource(R.string.your_preferences),
            onBack = onBack,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            scrollState = outerScrollState,
        )

        if (searchQuery.isBlank() && showLivePreviews) {
            ShelvesMiniPreview(
                activeTitles = previewItems,
                preferMusicOnly = preferMusicOnly,
                hideHomeVideos = hideHomeVideos,
                squareVideoThumbnails = squareVideoThumbnails,
                libraryReplayVisible = libraryReplayVisible,
                rightSwipeLabel = songSwipeRightAction.localizedLabel(),
                leftSwipeLabel = if (leftSwipeCustomized) {
                    songSwipeLeftAction.localizedLabel()
                } else {
                    stringResource(R.string.swipe_left_default)
                },
            )
        }

        if (searchQuery.isNotBlank() && !showYourSpaceSection && !hasPreferencesToggles && !showSwipeGestures && filteredRows.isEmpty()) {
            SettingsSearchEmpty(query = searchQuery)
        } else {
            // Single unified SettingsGroup containing all Your Preferences controls
            SettingsGroup(
                header = stringResource(R.string.your_preferences),
                footer = when {
                    filteredRows.isEmpty() -> null
                    homeShelfTitles.isEmpty() && searchQuery.isBlank() -> stringResource(R.string.shelves_empty_feed_hint)
                    searchQuery.isBlank() -> stringResource(R.string.drag_to_reorder)
                    else -> null
                },
            ) {
                var hasPreceding = false

                if (showYourSpaceSection) {
                    val sdLayout by AppSettings.speedDialLayoutMode.collectAsStateWithLifecycle()
                    val sdTileStyle by AppSettings.speedDialTileStyle.collectAsStateWithLifecycle()
                    val sdSource by AppSettings.speedDialSource.collectAsStateWithLifecycle()
                    val sdMixes by AppSettings.speedDialShowMixes.collectAsStateWithLifecycle()
                    val sdArtists by AppSettings.speedDialShowArtists.collectAsStateWithLifecycle()
                    val sdChordWave by AppSettings.speedDialShowChordWave.collectAsStateWithLifecycle()
                    val sdLabels by AppSettings.speedDialShowLabels.collectAsStateWithLifecycle()
                    val sdPins by AppSettings.speedDialShowPinBadge.collectAsStateWithLifecycle()
                    val sdSummary = buildString {
                        append(sdLayout.label)
                        append(" • ")
                        append(sdTileStyle.label)
                        append(" • ")
                        append(sdSource.label)
                        if (sdMixes) append(" • Mixes")
                        if (sdArtists) append(" • Artists")
                        if (sdChordWave) append(" • Chord Wave")
                        if (!sdLabels) append(" • Names hidden")
                        if (!sdPins) append(" • Pins hidden")
                    }
                    if (showLivePreviews) {
                        YourSpaceMiniPreviewStrip(
                            layoutMode = sdLayout,
                            tileStyle = sdTileStyle,
                            showMixes = sdMixes,
                            showArtists = sdArtists,
                            showLabels = sdLabels,
                            showPinBadge = sdPins,
                            showChordWave = sdChordWave,
                            onClick = { showYourSpaceSheet = true },
                        )
                        RowDivider()
                    }
                    SettingsRow(
                        icon = Icons.Rounded.Tune,
                        title = stringResource(R.string.customize_your_space),
                        subtitle = sdSummary,
                        onClick = { showYourSpaceSheet = true },
                    )
                    hasPreceding = true
                }

                if (showSwipeGestures) {
                    if (hasPreceding) {
                        SettingsSectionDivider(title = stringResource(R.string.swipe_gestures))
                    }
                    SettingsRow(
                        icon = Icons.Rounded.SwipeRight,
                        title = stringResource(R.string.right_swipe_action),
                        subtitle = songSwipeRightAction.localizedLabel(),
                        onClick = { showRightSwipePicker = true },
                    )
                    RowDivider()
                    SettingsRow(
                        icon = Icons.Rounded.SwipeLeft,
                        title = stringResource(R.string.left_swipe_action),
                        subtitle = if (leftSwipeCustomized) songSwipeLeftAction.localizedLabel()
                            else stringResource(R.string.swipe_left_default),
                        onClick = { showLeftSwipePicker = true },
                    )
                    hasPreceding = true
                }

                if (hasPreferencesToggles) {
                    if (hasPreceding) {
                        SettingsSectionDivider(title = stringResource(R.string.content_preferences_header))
                    }
                    var needsDivider = false
                    if (showPreferMusicOnly) {
                        needsDivider = true
                        SettingsRow(
                            icon = Icons.Rounded.MusicNote,
                            title = stringResource(R.string.prefer_music_only),
                            subtitle = stringResource(R.string.prefer_music_only_subtitle),
                            trailing = {
                                IosSwitch(
                                    checked = preferMusicOnly,
                                    onCheckedChange = AppSettings::setPreferMusicOnly,
                                )
                            },
                            onClick = { AppSettings.setPreferMusicOnly(!preferMusicOnly) },
                        )
                    }
                    if (showHideVideos) {
                        if (needsDivider) RowDivider()
                        needsDivider = true
                        SettingsRow(
                            icon = Icons.Rounded.VideocamOff,
                            title = stringResource(R.string.hide_home_videos),
                            subtitle = stringResource(R.string.hide_home_videos_subtitle),
                            trailing = {
                                IosSwitch(
                                    checked = hideHomeVideos,
                                    onCheckedChange = AppSettings::setHideHomeVideos,
                                )
                            },
                            onClick = { AppSettings.setHideHomeVideos(!hideHomeVideos) },
                        )
                    }
                    if (showSquareVideos) {
                        if (needsDivider) RowDivider()
                        needsDivider = true
                        SettingsRow(
                            icon = Icons.Rounded.AspectRatio,
                            title = stringResource(R.string.square_video_thumbnails),
                            subtitle = stringResource(R.string.square_video_thumbnails_subtitle),
                            trailing = {
                                IosSwitch(
                                    checked = squareVideoThumbnails,
                                    onCheckedChange = AppSettings::setSquareVideoThumbnails,
                                )
                            },
                            onClick = { AppSettings.setSquareVideoThumbnails(!squareVideoThumbnails) },
                        )
                    }
                    if (showLibraryReplayToggle) {
                        if (needsDivider) RowDivider()
                        SettingsRow(
                            icon = Icons.Rounded.BarChart,
                            title = stringResource(R.string.show_replay_in_library),
                            subtitle = stringResource(R.string.show_replay_in_library_subtitle),
                            trailing = {
                                IosSwitch(
                                    checked = libraryReplayVisible,
                                    onCheckedChange = AppSettings::setLibraryReplayVisible,
                                )
                            },
                            onClick = { AppSettings.setLibraryReplayVisible(!libraryReplayVisible) },
                        )
                    }
                    hasPreceding = true
                }

                if (filteredRows.isNotEmpty()) {
                    if (hasPreceding) {
                        SettingsSectionDivider(title = stringResource(R.string.arrange_home_shelves))
                    }
                    Column(modifier = Modifier.fillMaxWidth()) {
                        filteredRows.forEachIndexed { rowIdx, (id, meta) ->
                            key(id) {
                                val isHidden = meta.isHidden
                                val isDragging = draggedId == id
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .zIndex(if (isDragging) 1f else 0f)
                                        .onSizeChanged { if (it.height > 0 && draggedId == null) pitchPx = it.height.toFloat() }
                                        .graphicsLayer {
                                            if (isDragging) {
                                                val currentIndex = liveOrder.indexOf(id).coerceAtLeast(0)
                                                val pitch = lockedPitchPx.takeIf { it > 0f } ?: fallbackPitchPx
                                                translationY = totalDrag - (currentIndex - startIndex) * pitch
                                                scaleX = 1.015f
                                                scaleY = 1.015f
                                            }
                                        }
                                        .background(
                                            if (isDragging) {
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
                                            } else {
                                                Color.Transparent
                                            },
                                        ),
                                ) {
                                    if (rowIdx > 0 && !isDragging) RowDivider()
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 8.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        if (searchQuery.isBlank()) {
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 40.dp, height = 42.dp)
                                                    .pointerInput(id) {
                                                        detectDragGestures(
                                                            onDragStart = {
                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                draggedId = id
                                                                startIndex = liveOrder.indexOf(id).coerceAtLeast(0)
                                                                totalDrag = 0f
                                                                lockedPitchPx = pitchPx.takeIf { it > 0f } ?: fallbackPitchPx
                                                            },
                                                            onDrag = { change, dragAmount ->
                                                                change.consume()
                                                                advanceDrag(dragAmount.y)
                                                            },
                                                            onDragEnd = {
                                                                AppSettings.setHomeShelfOrder(liveOrder)
                                                                draggedId = null
                                                                totalDrag = 0f
                                                            },
                                                            onDragCancel = {
                                                                draggedId = null
                                                                totalDrag = 0f
                                                                liveOrder = orderedIds
                                                            },
                                                        )
                                                    },
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.DragHandle,
                                                    contentDescription = stringResource(R.string.drag_to_reorder),
                                                    tint = if (isDragging) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                                    },
                                                    modifier = Modifier.size(20.dp),
                                                )
                                            }
                                        } else {
                                            Spacer(Modifier.width(8.dp))
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = meta.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = if (!isHidden) {
                                                    MaterialTheme.colorScheme.onSurface
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                },
                                            )
                                            Text(
                                                text = meta.subtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                    alpha = if (!isHidden) 1f else 0.55f,
                                                ),
                                            )
                                        }

                                        Spacer(Modifier.width(8.dp))

                                        IosSwitch(
                                            checked = !isHidden,
                                            onCheckedChange = { enabled -> AppSettings.setHomeShelfHidden(id, !enabled) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (searchQuery.isBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (hiddenHomeShelves.isNotEmpty()) {
                    TextButton(onClick = { AppSettings.showAllHomeShelves() }) {
                        Icon(
                            imageVector = Icons.Rounded.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.shelves_show_all))
                    }
                }
                OutlinedButton(
                    onClick = { AppSettings.resetHomeShelves() },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.reset_to_default))
                }
            }
        }
    }
}

@Composable
private fun YourSpaceMiniPreviewStrip(
    layoutMode: SpeedDialLayoutMode,
    tileStyle: SpeedDialTileStyle,
    showMixes: Boolean,
    showArtists: Boolean,
    showLabels: Boolean,
    showPinBadge: Boolean,
    showChordWave: Boolean,
    onClick: () -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val accent = MaterialTheme.colorScheme.primary
    val tileShape = SmoothCornerShape(12.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.your_space),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = accent,
            )
            Text(
                text = buildString {
                    append(layoutMode.label)
                    if (showMixes) append(" • Mixes")
                    if (showArtists) append(" • Artists")
                },
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Tile 1: Sample Pinned Artwork Tile
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(74.dp)
                    .clip(tileShape)
                    .background(
                        Brush.linearGradient(
                            listOf(accent.copy(alpha = 0.55f), Color(0xFF28224A)),
                        ),
                    )
                    .speedDialTileEdge(
                        style = tileStyle,
                        shape = tileShape,
                        accent = accent,
                        isDark = isDark,
                        isCurrent = false,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.72f),
                    modifier = Modifier.align(Alignment.Center).size(22.dp),
                )
                if (showPinBadge) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(5.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = BitChordIcons.Pin,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(9.dp),
                        )
                    }
                }
                if (showLabels) {
                    Text(
                        text = "Pinned",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.BottomStart).padding(6.dp),
                    )
                }
            }

            // Tile 2: Mixes when enabled, else regular track tile
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(74.dp)
                    .clip(tileShape)
                    .background(
                        if (showMixes) {
                            Brush.linearGradient(listOf(Color(0xFF00B8D4).copy(alpha = 0.65f), Color(0xFF1E293B)))
                        } else {
                            Brush.linearGradient(listOf(Color(0xFF334155), Color(0xFF1E293B)))
                        },
                    )
                    .speedDialTileEdge(
                        style = tileStyle,
                        shape = tileShape,
                        accent = accent,
                        isDark = isDark,
                        isCurrent = false,
                    ),
            ) {
                if (showMixes) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .size(18.dp)
                            .clip(RoundedCornerShape(bottomEnd = 8.dp))
                            .background(Color(0xFF102A33)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "M",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.72f),
                        modifier = Modifier.align(Alignment.Center).size(22.dp),
                    )
                }
                if (showLabels) {
                    Text(
                        text = if (showMixes) "Mixes" else "Recent",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.BottomStart).padding(6.dp),
                    )
                }
            }

            // Tile 3: Chord Wave when enabled, else Artist card when `showArtists`, else track tile
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(74.dp)
                    .clip(if (showArtists && !showChordWave) CircleShape else tileShape)
                    .background(
                        when {
                            showChordWave -> Brush.linearGradient(
                                listOf(Color(0xFF7C4DFF).copy(alpha = 0.60f), Color(0xFF00E5FF).copy(alpha = 0.45f)),
                            )
                            showArtists -> Brush.linearGradient(
                                listOf(accent.copy(alpha = 0.65f), Color(0xFF2E1E3E)),
                            )
                            else -> Brush.linearGradient(
                                listOf(Color(0xFF3A3D4D), Color(0xFF1B1D26)),
                            )
                        },
                    )
                    .speedDialTileEdge(
                        style = tileStyle,
                        shape = if (showArtists && !showChordWave) CircleShape else tileShape,
                        accent = accent,
                        isDark = isDark,
                        isCurrent = showChordWave,
                    ),
            ) {
                Icon(
                    imageVector = if (showChordWave) Icons.Rounded.GraphicEq else Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.align(Alignment.Center).size(22.dp),
                )
                if (showLabels) {
                    Text(
                        text = when {
                            showChordWave -> stringResource(R.string.chord_wave)
                            showArtists -> "Artist"
                            else -> "Quick Pick"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(if (showArtists && !showChordWave) Alignment.BottomCenter else Alignment.BottomStart)
                            .padding(6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ShelvesMiniPreview(
    activeTitles: List<String>,
    preferMusicOnly: Boolean,
    hideHomeVideos: Boolean,
    squareVideoThumbnails: Boolean,
    libraryReplayVisible: Boolean,
    rightSwipeLabel: String,
    leftSwipeLabel: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.live_layout_preview),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.shelves_active_count, activeTitles.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (activeTitles.isEmpty()) {
                Text(
                    text = stringResource(R.string.all_shelves_disabled),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    activeTitles.take(4).forEachIndexed { index, label ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
                                .padding(4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "${index + 1}. $label",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                textAlign = TextAlign.Center,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            // Real Home video aspect & content-preference summary row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.60f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (!hideHomeVideos) {
                    Box(
                        modifier = Modifier
                            .size(
                                width = if (squareVideoThumbnails) 34.dp else 54.dp,
                                height = 34.dp,
                            )
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f),
                                    ),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (squareVideoThumbnails) "1:1" else "16:9",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                            color = Color.White,
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = buildString {
                            append(if (hideHomeVideos) "Videos hidden" else if (squareVideoThumbnails) "Square thumbnails" else "Widescreen videos")
                            if (preferMusicOnly) append(" • Music only")
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = buildString {
                            append("R: $rightSwipeLabel • L: $leftSwipeLabel")
                            if (libraryReplayVisible) append(" • Replay on")
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun SwipeActionPickerDialog(
    title: String,
    currentAction: SongSwipeAction?,
    onSelect: (SongSwipeAction) -> Unit,
    onDismiss: () -> Unit,
    onReset: (() -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current
    GlassAlert(title = title, onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (onReset != null) {
                val isDefaultSelected = currentAction == null
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onReset()
                            onDismiss()
                        }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = null,
                        tint = if (isDefaultSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.swipe_left_default),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isDefaultSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isDefaultSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (isDefaultSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
                AlertRule()
            }
            SongSwipeAction.entries.forEachIndexed { index, action ->
                if (index > 0) AlertRule()
                val isSelected = action == currentAction
                val actionIcon = when (action) {
                    SongSwipeAction.PLAY_NEXT -> Icons.Rounded.QueuePlayNext
                    SongSwipeAction.ADD_TO_QUEUE -> Icons.Rounded.AddToQueue
                    SongSwipeAction.TOGGLE_LIKE -> Icons.Rounded.Favorite
                    SongSwipeAction.DOWNLOAD -> Icons.Rounded.Download
                    SongSwipeAction.REMOVE_FROM_PLAYLIST -> Icons.Rounded.Delete
                    SongSwipeAction.OPEN_MENU -> Icons.Rounded.MoreHoriz
                    SongSwipeAction.NONE -> Icons.Rounded.Block
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect(action)
                            onDismiss()
                        }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = action.localizedLabel(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Curated color palette carousel with rich live checkmarks and atmospheric swatches.
 */