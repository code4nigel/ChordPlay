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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.taste.DiscoveryLevel
import com.music.bitchord.data.taste.TasteCategory
import com.music.bitchord.data.taste.TasteConstants
import com.music.bitchord.data.taste.TasteNode
import com.music.bitchord.data.taste.TasteProfileRepository
import com.music.bitchord.data.taste.TasteSnapshot
import com.music.bitchord.data.taste.TasteTag
import com.music.bitchord.data.taste.TasteTaxonomy
import com.music.bitchord.ui.components.AlertAction
import com.music.bitchord.ui.components.AlertRule
import com.music.bitchord.ui.components.GlassAlert
import com.music.bitchord.ui.screens.DestructiveRow
import com.music.bitchord.ui.screens.ROW_INSET
import com.music.bitchord.ui.screens.RowDivider
import com.music.bitchord.ui.screens.SegmentedControl
import com.music.bitchord.ui.screens.SettingsGroup
import com.music.bitchord.ui.screens.SettingsRow
import com.music.bitchord.ui.screens.SettingsSearchEmpty
import com.music.bitchord.ui.screens.SettingsSubPageHeader
import com.music.bitchord.ui.screens.matchesSearch
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt

private enum class AudioChoicesTab(val category: TasteCategory?) {
    LANGUAGES(TasteCategory.LANGUAGE),
    GENRES(TasteCategory.GENRE),
    THEMES(TasteCategory.THEME),
    BLOCKED(null),
}

private enum class QuickPickChoice {
    NEUTRAL,
    LOVE,
    BLOCK,
}

private const val TASTE_SWAP_THRESHOLD = 0.58f

private fun <T> List<T>.movedItem(from: Int, to: Int): List<T> {
    if (from !in indices || to !in indices || from == to) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}

/**
 * `Settings -> Audio -> Audio Choices` sub-page.
 *
 * Hosts segmented tabs (`Languages | Genres | Themes | Blocked`), an optional first-run
 * quick setup card, drag-to-reorder priority ranking with live probability percentages,
 * 1-tap mute/blocking, Discovery Budget slider/segments, and Reset controls.
 */
@Composable
fun AudioChoicesSection(
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    initialSearchQuery: String = "",
    modifier: Modifier = Modifier,
) {
    val snapshot by TasteProfileRepository.snapshot.collectAsStateWithLifecycle()
    var searchQuery by remember(initialSearchQuery) { mutableStateOf(initialSearchQuery) }
    var activeTab by remember { mutableStateOf(AudioChoicesTab.LANGUAGES) }
    var showQuickSetupOverride by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var lastBlockedUndo by remember { mutableStateOf<TasteTag?>(null) }

    val scrollState = rememberScrollState()
    val haptic = LocalHapticFeedback.current

    val showQuickSetup = (!snapshot.onboardingDismissed || showQuickSetupOverride) && searchQuery.isBlank()

    if (showResetDialog) {
        GlassAlert(
            title = stringResource(R.string.audio_choices_reset_title),
            onDismiss = { showResetDialog = false },
            footerActions = {
                AlertAction(
                    label = stringResource(R.string.audio_choices_reset_learned),
                    emphasised = true,
                    onClick = {
                        TasteProfileRepository.resetLearnedWeights(includeBlocks = false)
                        showResetDialog = false
                    },
                )
                AlertRule()
                AlertAction(
                    label = stringResource(R.string.audio_choices_reset_all),
                    emphasised = false,
                    destructive = true,
                    onClick = {
                        TasteProfileRepository.resetLearnedWeights(includeBlocks = true)
                        showResetDialog = false
                    },
                )
                AlertRule()
                AlertAction(
                    label = stringResource(R.string.cancel),
                    emphasised = false,
                    onClick = { showResetDialog = false },
                )
            },
        ) {
            Text(
                text = stringResource(R.string.audio_choices_reset_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    var draggedId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState, enabled = draggedId == null)
            .padding(contentPadding),
    ) {
        SettingsSubPageHeader(
            title = stringResource(R.string.audio_choices),
            onBack = onBack,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            scrollState = scrollState,
        )

        AnimatedVisibility(
            visible = showQuickSetup,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            QuickSetupCard(
                snapshot = snapshot,
                onApply = { prefLangs, blockLangs, prefGenres, blockGenres ->
                    TasteProfileRepository.applyQuickSetup(
                        preferredLangKeys = prefLangs,
                        blockedLangKeys = blockLangs,
                        preferredGenreKeys = prefGenres,
                        blockedGenreKeys = blockGenres,
                    )
                    showQuickSetupOverride = false
                },
                onDismiss = {
                    TasteProfileRepository.dismissOnboarding()
                    showQuickSetupOverride = false
                },
            )
        }

        // Undo bar after blocking a tag
        AnimatedVisibility(
            visible = lastBlockedUndo != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            lastBlockedUndo?.let { tag ->
                SettingsGroup(header = null, topSpacing = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = ROW_INSET, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.audio_choices_blocked_notice, tag.displayName),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = {
                                TasteProfileRepository.unblockTag(tag)
                                lastBlockedUndo = null
                            },
                        ) {
                            Text(stringResource(R.string.undo))
                        }
                    }
                }
            }
        }

        // Segmented tabs: Languages | Genres | Themes | Blocked
        val tabLabels = listOf(
            stringResource(R.string.audio_choices_tab_languages),
            stringResource(R.string.audio_choices_tab_genres),
            stringResource(R.string.audio_choices_tab_themes),
            stringResource(R.string.audio_choices_tab_blocked),
        )
        SegmentedControl(
            options = tabLabels,
            selectedIndex = AudioChoicesTab.entries.indexOf(activeTab),
            onSelect = { idx ->
                draggedId = null
                activeTab = AudioChoicesTab.entries[idx]
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )

        val activeCategory = activeTab.category
        if (activeCategory != null) {
            ReorderableCategoryList(
                category = activeCategory,
                snapshot = snapshot,
                searchQuery = searchQuery,
                draggedId = draggedId,
                onDraggedIdChange = { draggedId = it },
                onBlockTag = { tag ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    TasteProfileRepository.blockTag(tag)
                    lastBlockedUndo = tag
                },
            )
        } else {
            BlockedItemsTab(
                snapshot = snapshot,
                searchQuery = searchQuery,
            )
        }

        // Discovery Budget + Reset + Quick Setup trigger
        val showDiscovery = matchesSearch(
            searchQuery,
            stringResource(R.string.audio_choices_discovery),
            stringResource(R.string.audio_choices_discovery_subtitle),
            "discovery", "focused", "balanced", "exploratory", "budget",
        )
        val showReset = matchesSearch(
            searchQuery,
            stringResource(R.string.audio_choices_reset_button),
            "reset", "clear", "weights",
        )
        if (showDiscovery || showReset) {
            SettingsGroup(
                header = stringResource(R.string.audio_choices_controls_header),
                topSpacing = 18.dp,
            ) {
                if (showDiscovery) {
                    SettingsRow(
                        icon = Icons.Rounded.Explore,
                        title = stringResource(R.string.audio_choices_discovery),
                        subtitle = stringResource(R.string.audio_choices_discovery_subtitle),
                    )
                    val discoveryOptions = listOf(
                        stringResource(R.string.audio_choices_discovery_focused),
                        stringResource(R.string.audio_choices_discovery_balanced),
                        stringResource(R.string.audio_choices_discovery_exploratory),
                    )
                    SegmentedControl(
                        options = discoveryOptions,
                        selectedIndex = DiscoveryLevel.entries.indexOf(snapshot.discoveryLevel),
                        onSelect = { idx ->
                            TasteProfileRepository.setDiscoveryLevel(DiscoveryLevel.entries[idx])
                        },
                        modifier = Modifier.padding(start = ROW_INSET, end = ROW_INSET, bottom = 14.dp),
                    )
                }
                if (showDiscovery && showReset) RowDivider()
                if (showReset) {
                    if (!showQuickSetup && searchQuery.isBlank()) {
                        SettingsRow(
                            icon = Icons.Rounded.Tune,
                            title = stringResource(R.string.audio_choices_quick_setup_title),
                            subtitle = stringResource(R.string.audio_choices_quick_setup_reopen),
                            onClick = { showQuickSetupOverride = true },
                        )
                        RowDivider()
                    }
                    DestructiveRow(
                        label = stringResource(R.string.audio_choices_reset_button),
                        onClick = { showResetDialog = true },
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ReorderableCategoryList(
    category: TasteCategory,
    snapshot: TasteSnapshot,
    searchQuery: String,
    draggedId: String?,
    onDraggedIdChange: (String?) -> Unit,
    onBlockTag: (TasteTag) -> Unit,
) {
    val activeNodes = remember(snapshot, category) { snapshot.activeNodes(category) }
    var liveOrder by remember(category) { mutableStateOf(activeNodes.map { it.id }) }
    var totalDrag by remember { mutableFloatStateOf(0f) }
    var startIndex by remember { mutableIntStateOf(0) }
    var pitchPx by remember { mutableFloatStateOf(0f) }
    var lockedPitchPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(activeNodes, draggedId) {
        if (draggedId == null) {
            liveOrder = activeNodes.map { it.id }
        }
    }

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val fallbackPitchPx = with(density) { 64.dp.toPx() }

    val nodeById = remember(activeNodes) { activeNodes.associateBy { it.id } }

    // Live weights preview during drag so probability % shifts in real time
    val previewWeights = remember(liveOrder, draggedId, nodeById) {
        if (draggedId == null) {
            nodeById.mapValues { it.value.weight }
        } else {
            val count = liveOrder.size
            liveOrder.mapIndexed { idx, id ->
                val t = if (count <= 1) 0f else idx.toFloat() / (count - 1).toFloat()
                val w = (
                    TasteConstants.MIN_MANUAL_WEIGHT +
                        (TasteConstants.MAX_MANUAL_WEIGHT - TasteConstants.MIN_MANUAL_WEIGHT) *
                        (1f - t).toDouble().pow(1.25).toFloat()
                    ).coerceIn(TasteConstants.MIN_MANUAL_WEIGHT, TasteConstants.MAX_MANUAL_WEIGHT)
                id to w
            }.toMap()
        }
    }
    val weightSum = remember(previewWeights) {
        previewWeights.values.sum().coerceAtLeast(1f)
    }

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
            if (travelled > moved + TASTE_SWAP_THRESHOLD && index < liveOrder.lastIndex) {
                liveOrder = liveOrder.movedItem(index, index + 1)
                index++
                swapped = true
            } else if (travelled < moved - TASTE_SWAP_THRESHOLD && index > 0) {
                liveOrder = liveOrder.movedItem(index, index - 1)
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

    val visibleIds = remember(liveOrder, searchQuery, nodeById) {
        if (searchQuery.isBlank()) {
            liveOrder
        } else {
            liveOrder.filter { id ->
                val node = nodeById[id] ?: return@filter false
                matchesSearch(searchQuery, node.displayName, node.key)
            }
        }
    }

    if (visibleIds.isEmpty()) {
        SettingsSearchEmpty(query = searchQuery)
        return
    }

    SettingsGroup(
        header = stringResource(R.string.audio_choices_drag_hint),
        topSpacing = 6.dp,
    ) {
        visibleIds.forEachIndexed { idx, id ->
            val node = nodeById[id] ?: return@forEachIndexed
            val isDragging = draggedId == id
            val currentIdx = liveOrder.indexOf(id)
            val liveWeight = previewWeights[id] ?: node.weight
            val sharePct = ((liveWeight / weightSum) * 100f).coerceIn(0f, 100f)

            key(id) {
                if (idx > 0) RowDivider()
                val tag = node.canonicalId?.let(TasteTaxonomy::byId)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (isDragging) 10f else 0f)
                        .graphicsLayer {
                            if (isDragging) {
                                val pitch = lockedPitchPx.takeIf { it > 0f } ?: fallbackPitchPx
                                translationY = totalDrag - (currentIdx - startIndex) * pitch
                                scaleX = 1.02f
                                scaleY = 1.02f
                            }
                        }
                        .onSizeChanged { size ->
                            if (!isDragging && size.height > 0) {
                                pitchPx = size.height.toFloat()
                            }
                        }
                        .padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Drag handle (≡)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .then(
                                if (searchQuery.isBlank()) {
                                    Modifier.pointerInput(id) {
                                        detectDragGestures(
                                            onDragStart = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                startIndex = liveOrder.indexOf(id).coerceAtLeast(0)
                                                lockedPitchPx = pitchPx.takeIf { it > 0f } ?: fallbackPitchPx
                                                totalDrag = 0f
                                                onDraggedIdChange(id)
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                advanceDrag(dragAmount.y)
                                            },
                                            onDragEnd = {
                                                onDraggedIdChange(null)
                                                TasteProfileRepository.reorderCategory(category, liveOrder)
                                            },
                                            onDragCancel = {
                                                onDraggedIdChange(null)
                                                liveOrder = activeNodes.map { it.id }
                                            },
                                        )
                                    }
                                } else {
                                    Modifier
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DragHandle,
                            contentDescription = stringResource(R.string.audio_choices_drag_handle),
                            tint = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    // Tag name + probability bar + %
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = node.displayName,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (node.isAutoAdded && !node.isPreferred) {
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.audio_choices_auto_added),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = String.format(Locale.US, "%.1f%%", sharePct),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(5.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth((liveWeight / TasteConstants.MAX_WEIGHT).coerceIn(0.03f, 1f))
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(
                                        if (node.isPreferred || node.manualRank != null) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                        },
                                    ),
                            )
                        }
                    }

                    // Block / Mute button
                    if (tag != null) {
                        IconButton(
                            onClick = { onBlockTag(tag) },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Block,
                                contentDescription = stringResource(R.string.audio_choices_block_item),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                                modifier = Modifier.size(19.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockedItemsTab(
    snapshot: TasteSnapshot,
    searchQuery: String,
) {
    val blockedTags = remember(snapshot, searchQuery) {
        (snapshot.blockedNodes(TasteCategory.LANGUAGE) +
            snapshot.blockedNodes(TasteCategory.GENRE) +
            snapshot.blockedNodes(TasteCategory.THEME))
            .filter { matchesSearch(searchQuery, it.displayName, it.key) }
    }
    val blockedArtists = remember(snapshot, searchQuery) {
        snapshot.blockedNodes(TasteCategory.ARTIST)
            .filter { matchesSearch(searchQuery, it.displayName, it.key) }
    }
    val markedAiArtists = remember(snapshot, searchQuery) {
        snapshot.userMarkedAiArtists
            .filter { matchesSearch(searchQuery, it) }
            .sorted()
    }
    val blockedSongs = remember(snapshot, searchQuery) {
        snapshot.blockedSongs.values
            .sortedByDescending { it.blockedAt }
            .filter { matchesSearch(searchQuery, it.title, it.artistName) }
    }

    if (blockedTags.isEmpty() && blockedArtists.isEmpty() && markedAiArtists.isEmpty() && blockedSongs.isEmpty()) {
        SettingsGroup(header = null, topSpacing = 8.dp) {
            Text(
                text = stringResource(R.string.audio_choices_blocked_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = ROW_INSET, vertical = 18.dp),
            )
        }
        return
    }

    if (blockedTags.isNotEmpty()) {
        SettingsGroup(header = stringResource(R.string.audio_choices_blocked_tags_header), topSpacing = 8.dp) {
            blockedTags.forEachIndexed { idx, node ->
                if (idx > 0) RowDivider()
                val tag = node.canonicalId?.let(TasteTaxonomy::byId)
                BlockedRow(
                    icon = Icons.Rounded.Block,
                    title = node.displayName,
                    subtitle = node.category.name.lowercase().replaceFirstChar { it.uppercase() },
                    actionLabel = stringResource(R.string.audio_choices_unblock),
                    onAction = {
                        if (tag != null) TasteProfileRepository.unblockTag(tag)
                    },
                )
            }
        }
    }

    if (blockedArtists.isNotEmpty() || markedAiArtists.isNotEmpty()) {
        SettingsGroup(header = stringResource(R.string.audio_choices_blocked_artists_header), topSpacing = 14.dp) {
            blockedArtists.forEachIndexed { idx, node ->
                if (idx > 0) RowDivider()
                BlockedRow(
                    icon = Icons.Rounded.Person,
                    title = node.displayName,
                    subtitle = stringResource(R.string.audio_choices_blocked_artist_sub),
                    actionLabel = stringResource(R.string.audio_choices_unblock),
                    onAction = { TasteProfileRepository.unblockArtist(node.key) },
                )
            }
            markedAiArtists.forEachIndexed { idx, key ->
                if (idx > 0 || blockedArtists.isNotEmpty()) RowDivider()
                BlockedRow(
                    icon = Icons.Rounded.SmartToy,
                    title = key,
                    subtitle = stringResource(R.string.audio_choices_marked_ai_sub),
                    actionLabel = stringResource(R.string.audio_choices_unblock),
                    onAction = { TasteProfileRepository.unmarkArtistAsAi(key) },
                )
            }
        }
    }

    if (blockedSongs.isNotEmpty()) {
        SettingsGroup(header = stringResource(R.string.audio_choices_blocked_songs_header), topSpacing = 14.dp) {
            blockedSongs.forEachIndexed { idx, song ->
                if (idx > 0) RowDivider()
                BlockedRow(
                    icon = Icons.Rounded.MusicNote,
                    title = song.title,
                    subtitle = song.artistName,
                    actionLabel = stringResource(R.string.audio_choices_restore),
                    onAction = { TasteProfileRepository.unblockSong(song.videoId) },
                )
            }
        }
    }
}

@Composable
private fun BlockedRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(horizontal = ROW_INSET, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        TextButton(onClick = onAction) {
            Text(actionLabel)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickSetupCard(
    snapshot: TasteSnapshot,
    onApply: (Set<String>, Set<String>, Set<String>, Set<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val langChoices = remember { TasteTaxonomy.languages.take(16) }
    val genreChoices = remember { TasteTaxonomy.genres.take(14) }
    val langStates = remember(snapshot) {
        mutableStateMapOf<String, QuickPickChoice>().apply {
            for (tag in langChoices) {
                val node = snapshot.nodesById[TasteTaxonomy.nodeId(tag)]
                put(
                    tag.key,
                    when {
                        node?.isBlocked == true -> QuickPickChoice.BLOCK
                        node?.isPreferred == true -> QuickPickChoice.LOVE
                        else -> QuickPickChoice.NEUTRAL
                    },
                )
            }
        }
    }
    val genreStates = remember(snapshot) {
        mutableStateMapOf<String, QuickPickChoice>().apply {
            for (tag in genreChoices) {
                val node = snapshot.nodesById[TasteTaxonomy.nodeId(tag)]
                put(
                    tag.key,
                    when {
                        node?.isBlocked == true -> QuickPickChoice.BLOCK
                        node?.isPreferred == true -> QuickPickChoice.LOVE
                        else -> QuickPickChoice.NEUTRAL
                    },
                )
            }
        }
    }

    SettingsGroup(header = null, topSpacing = 8.dp) {
        Column(modifier = Modifier.padding(horizontal = ROW_INSET, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.audio_choices_quick_setup_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.cancel),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.audio_choices_quick_setup_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.audio_choices_tab_languages),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                langChoices.forEach { tag ->
                    val state = langStates[tag.key] ?: QuickPickChoice.NEUTRAL
                    QuickSetupChip(
                        label = tag.displayName,
                        choice = state,
                        onClick = {
                            langStates[tag.key] = when (state) {
                                QuickPickChoice.NEUTRAL -> QuickPickChoice.LOVE
                                QuickPickChoice.LOVE -> QuickPickChoice.BLOCK
                                QuickPickChoice.BLOCK -> QuickPickChoice.NEUTRAL
                            }
                        },
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.audio_choices_tab_genres),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                genreChoices.forEach { tag ->
                    val state = genreStates[tag.key] ?: QuickPickChoice.NEUTRAL
                    QuickSetupChip(
                        label = tag.displayName,
                        choice = state,
                        onClick = {
                            genreStates[tag.key] = when (state) {
                                QuickPickChoice.NEUTRAL -> QuickPickChoice.LOVE
                                QuickPickChoice.LOVE -> QuickPickChoice.BLOCK
                                QuickPickChoice.BLOCK -> QuickPickChoice.NEUTRAL
                            }
                        },
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.audio_choices_skip))
                }
                Button(
                    onClick = {
                        onApply(
                            langStates.filterValues { it == QuickPickChoice.LOVE }.keys,
                            langStates.filterValues { it == QuickPickChoice.BLOCK }.keys,
                            genreStates.filterValues { it == QuickPickChoice.LOVE }.keys,
                            genreStates.filterValues { it == QuickPickChoice.BLOCK }.keys,
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.audio_choices_apply))
                }
            }
        }
    }
}

@Composable
private fun QuickSetupChip(
    label: String,
    choice: QuickPickChoice,
    onClick: () -> Unit,
) {
    val bg = when (choice) {
        QuickPickChoice.NEUTRAL -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
        QuickPickChoice.LOVE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        QuickPickChoice.BLOCK -> MaterialTheme.colorScheme.error.copy(alpha = 0.16f)
    }
    val fg = when (choice) {
        QuickPickChoice.NEUTRAL -> MaterialTheme.colorScheme.onSurface
        QuickPickChoice.LOVE -> MaterialTheme.colorScheme.primary
        QuickPickChoice.BLOCK -> MaterialTheme.colorScheme.error
    }
    val borderColor = when (choice) {
        QuickPickChoice.NEUTRAL -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        QuickPickChoice.LOVE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        QuickPickChoice.BLOCK -> MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
    }
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, borderColor, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (choice) {
            QuickPickChoice.LOVE -> {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
            }
            QuickPickChoice.BLOCK -> {
                Icon(Icons.Rounded.Block, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
            }
            QuickPickChoice.NEUTRAL -> Unit
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
            color = fg,
        )
    }
}
