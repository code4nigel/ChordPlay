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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
            title = id,
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
    var searchQuery by remember(initialSearchQuery) { mutableStateOf(initialSearchQuery) }

    val orderedIds = remember(homeShelfTitles, homeShelfOrder) {
        orderHomeShelfIds(
            rawShelfTitles = homeShelfTitles,
            savedOrder = homeShelfOrder,
            includeSpeedDial = true,
            includeMelody = true,
            includeReplay = true,
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
        )

        if (searchQuery.isBlank()) {
            ShelvesMiniPreview(activeTitles = previewItems)
        }

        if (searchQuery.isNotBlank() && !hasPreferencesToggles && filteredRows.isEmpty()) {
            SettingsSearchEmpty(query = searchQuery)
        } else {
            if (hasPreferencesToggles) {
                SettingsGroup(header = stringResource(R.string.content_preferences_header)) {
                    var needsDivider = false
                    if (showPreferMusicOnly) {
                        needsDivider = true
                        SettingsRow(
                            icon = Icons.Rounded.MusicNote,
                            title = stringResource(R.string.prefer_music_only),
                            subtitle = stringResource(R.string.prefer_music_only_subtitle),
                            trailing = {
                                Switch(
                                    checked = preferMusicOnly,
                                    onCheckedChange = AppSettings::setPreferMusicOnly,
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                                    ),
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
                                Switch(
                                    checked = hideHomeVideos,
                                    onCheckedChange = AppSettings::setHideHomeVideos,
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                                    ),
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
                                Switch(
                                    checked = squareVideoThumbnails,
                                    onCheckedChange = AppSettings::setSquareVideoThumbnails,
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                                    ),
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
                                Switch(
                                    checked = libraryReplayVisible,
                                    onCheckedChange = AppSettings::setLibraryReplayVisible,
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            },
                            onClick = { AppSettings.setLibraryReplayVisible(!libraryReplayVisible) },
                        )
                    }
                }
            }

            if (filteredRows.isNotEmpty()) {
                SettingsGroup(
                    header = stringResource(R.string.arrange_home_shelves),
                    footer = if (homeShelfTitles.isEmpty() && searchQuery.isBlank()) {
                        stringResource(R.string.shelves_empty_feed_hint)
                    } else if (searchQuery.isBlank()) {
                        stringResource(R.string.drag_to_reorder)
                    } else {
                        null
                    },
                ) {
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

                                        Switch(
                                            checked = !isHidden,
                                            onCheckedChange = { enabled -> AppSettings.setHomeShelfHidden(id, !enabled) },
                                            colors = SwitchDefaults.colors(
                                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                                checkedBorderColor = MaterialTheme.colorScheme.primary,
                                            ),
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
private fun ShelvesMiniPreview(
    activeTitles: List<String>,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                .height(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
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
        }
    }
}
