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
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val rowsWithMeta = orderedIds.map { id ->
        id to shelfDisplayMeta(
            id = id,
            hiddenSet = hiddenHomeShelves,
        )
    }

    val filteredRows = rowsWithMeta.filter { (id, meta) ->
        matchesSearch(searchQuery, meta.title, meta.subtitle, id)
    }

    val showLibraryReplayToggle = matchesSearch(
        searchQuery,
        stringResource(R.string.show_replay_in_library),
        stringResource(R.string.show_replay_in_library_subtitle),
        "replay", "library", "stats", "cards",
    )

    val previewItems = rowsWithMeta.filterNot { (_, meta) -> meta.isHidden }.map { (_, meta) -> meta.title }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
    ) {
        SettingsSubPageHeader(
            title = stringResource(R.string.home_shelves),
            onBack = onBack,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
        )

        if (searchQuery.isBlank()) {
            ShelvesMiniPreview(activeTitles = previewItems)
        }

        if (searchQuery.isNotBlank() && !showLibraryReplayToggle && filteredRows.isEmpty()) {
            SettingsSearchEmpty(query = searchQuery)
        } else {
            if (showLibraryReplayToggle) {
                SettingsGroup(header = stringResource(R.string.shelves_visibility_header)) {
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

            if (filteredRows.isNotEmpty()) {
                SettingsGroup(
                    header = stringResource(R.string.arrange_home_shelves),
                    footer = if (homeShelfTitles.isEmpty() && searchQuery.isBlank()) {
                        stringResource(R.string.shelves_empty_feed_hint)
                    } else {
                        null
                    },
                ) {
                    filteredRows.forEachIndexed { rowIdx, (id, meta) ->
                        if (rowIdx > 0) RowDivider()
                        val index = orderedIds.indexOf(id)
                        val canMoveUp = index > 0
                        val canMoveDown = index in 0 until orderedIds.lastIndex
                        val isHidden = meta.isHidden

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
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

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                IconButton(
                                    onClick = { AppSettings.setHomeShelfOrder(orderedIds.moved(index, index - 1)) },
                                    enabled = canMoveUp,
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowUpward,
                                        contentDescription = stringResource(R.string.move_up),
                                        tint = if (canMoveUp) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                        },
                                        modifier = Modifier.size(18.dp),
                                    )
                                }

                                IconButton(
                                    onClick = { AppSettings.setHomeShelfOrder(orderedIds.moved(index, index + 1)) },
                                    enabled = canMoveDown,
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowDownward,
                                        contentDescription = stringResource(R.string.move_down),
                                        tint = if (canMoveDown) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                        },
                                        modifier = Modifier.size(18.dp),
                                    )
                                }

                                Spacer(Modifier.width(6.dp))

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
