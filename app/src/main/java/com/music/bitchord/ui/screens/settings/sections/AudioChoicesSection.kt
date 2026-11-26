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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Remove
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.taste.DiscoveryLevel
import com.music.bitchord.data.taste.TasteCategory
import com.music.bitchord.data.taste.TasteConstants
import com.music.bitchord.data.taste.TasteNode
import com.music.bitchord.data.taste.TasteProfileRepository
import com.music.bitchord.data.taste.TasteSnapshot
import com.music.bitchord.data.taste.TasteTaxonomy
import com.music.bitchord.data.taste.TunedSong
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

private enum class AudioChoicesTab {
    LANGUAGES,
    GENRES,
    THEMES,
    ARTISTS,
    SONGS,
    BLOCKED,
}

private enum class TuneFilter {
    ALL,
    BOOSTED,
    LESS,
    BLOCKED,
}

private enum class BlockedSubFilter {
    ALL,
    ARTISTS,
    SONGS,
    AI_ACTS,
    TAGS,
}

private enum class QuickPickChoice {
    NEUTRAL,
    LOVE,
    BLOCK,
}

private sealed class UndoBlockAction {
    data class Node(val node: TasteNode) : UndoBlockAction()
    data class Track(val song: Song) : UndoBlockAction()
}

private const val STEP_DELTA = 10f

/**
 * `Settings -> Audio -> Audio Choices` sub-page.
 *
 * Hosts 6 clean tabs (`Languages | Genres | Themes | Artists | Songs | Blocked`), reliable `-` / `+`
 * priority steppers, 1-tap mute/blocking, category sub-filters for tuned/blocked artists and songs,
 * Quick Setup, Discovery Budget selector, and Reset controls.
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
    var lastBlockedUndo by remember { mutableStateOf<UndoBlockAction?>(null) }

    val scrollState = rememberScrollState()
    val haptic = LocalHapticFeedback.current

    val showQuickSetup = (!snapshot.onboardingDismissed || showQuickSetupOverride) && searchQuery.isBlank()

    val blockedTotalCount = remember(snapshot) {
        val uniqueBlockedArtists = snapshot.blockedNodes(TasteCategory.ARTIST)
            .distinctBy { TasteConstants.normalizeArtistKey(it.displayName) }
            .size
        snapshot.blockedNodes(TasteCategory.LANGUAGE).size +
            snapshot.blockedNodes(TasteCategory.GENRE).size +
            snapshot.blockedNodes(TasteCategory.THEME).size +
            uniqueBlockedArtists +
            snapshot.userMarkedAiArtists.size +
            snapshot.blockedSongs.size
    }

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
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

        // Undo bar after blocking a tag, artist, or song
        AnimatedVisibility(
            visible = lastBlockedUndo != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            lastBlockedUndo?.let { action ->
                val label = when (action) {
                    is UndoBlockAction.Node -> action.node.displayName
                    is UndoBlockAction.Track -> action.song.title
                }
                SettingsGroup(header = null, topSpacing = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = ROW_INSET, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.audio_choices_blocked_notice, label),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = {
                                when (action) {
                                    is UndoBlockAction.Node -> TasteProfileRepository.unblockNode(action.node.id)
                                    is UndoBlockAction.Track -> TasteProfileRepository.unblockSong(action.song.videoId)
                                }
                                lastBlockedUndo = null
                            },
                        ) {
                            Text(stringResource(R.string.undo))
                        }
                    }
                }
            }
        }

        // Scrollable category selector: Languages | Genres | Themes | Artists | Songs | Blocked
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val blockedLabel = if (blockedTotalCount > 0) {
                "${stringResource(R.string.audio_choices_tab_blocked)} ($blockedTotalCount)"
            } else {
                stringResource(R.string.audio_choices_tab_blocked)
            }
            val tabs = listOf(
                AudioChoicesTab.LANGUAGES to stringResource(R.string.audio_choices_tab_languages),
                AudioChoicesTab.GENRES to stringResource(R.string.audio_choices_tab_genres),
                AudioChoicesTab.THEMES to stringResource(R.string.audio_choices_tab_themes),
                AudioChoicesTab.ARTISTS to stringResource(R.string.audio_choices_tab_artists),
                AudioChoicesTab.SONGS to stringResource(R.string.audio_choices_tab_songs),
                AudioChoicesTab.BLOCKED to blockedLabel,
            )
            tabs.forEach { (tab, title) ->
                FilterPill(
                    label = title,
                    selected = activeTab == tab,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        activeTab = tab
                    },
                )
            }
        }

        when (activeTab) {
            AudioChoicesTab.LANGUAGES -> CategoryStepperList(
                category = TasteCategory.LANGUAGE,
                snapshot = snapshot,
                searchQuery = searchQuery,
                onBlockNode = { node ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    TasteProfileRepository.blockNode(node.id)
                    lastBlockedUndo = UndoBlockAction.Node(node)
                },
            )
            AudioChoicesTab.GENRES -> CategoryStepperList(
                category = TasteCategory.GENRE,
                snapshot = snapshot,
                searchQuery = searchQuery,
                onBlockNode = { node ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    TasteProfileRepository.blockNode(node.id)
                    lastBlockedUndo = UndoBlockAction.Node(node)
                },
            )
            AudioChoicesTab.THEMES -> CategoryStepperList(
                category = TasteCategory.THEME,
                snapshot = snapshot,
                searchQuery = searchQuery,
                onBlockNode = { node ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    TasteProfileRepository.blockNode(node.id)
                    lastBlockedUndo = UndoBlockAction.Node(node)
                },
            )
            AudioChoicesTab.ARTISTS -> ArtistsTab(
                snapshot = snapshot,
                searchQuery = searchQuery,
                onBlockNode = { node ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    TasteProfileRepository.blockNode(node.id)
                    lastBlockedUndo = UndoBlockAction.Node(node)
                },
            )
            AudioChoicesTab.SONGS -> SongsTab(
                snapshot = snapshot,
                searchQuery = searchQuery,
                onBlockSong = { song ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    TasteProfileRepository.blockSong(song)
                    lastBlockedUndo = UndoBlockAction.Track(song)
                },
            )
            AudioChoicesTab.BLOCKED -> BlockedItemsTab(
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
private fun CategoryStepperList(
    category: TasteCategory,
    snapshot: TasteSnapshot,
    searchQuery: String,
    onBlockNode: (TasteNode) -> Unit,
) {
    val activeNodes = remember(snapshot, category) { snapshot.activeNodes(category) }
    val visibleNodes = remember(activeNodes, searchQuery) {
        if (searchQuery.isBlank()) {
            activeNodes
        } else {
            activeNodes.filter { matchesSearch(searchQuery, it.displayName, it.key) }
        }
    }
    if (visibleNodes.isEmpty()) {
        SettingsSearchEmpty(query = searchQuery)
        return
    }
    val weightSum = remember(activeNodes) {
        activeNodes.sumOf { it.weight.toDouble() }.toFloat().coerceAtLeast(1f)
    }
    val haptic = LocalHapticFeedback.current

    SettingsGroup(
        header = stringResource(R.string.audio_choices_adjust_hint),
        topSpacing = 6.dp,
    ) {
        visibleNodes.forEachIndexed { idx, node ->
            key(node.id) {
                if (idx > 0) RowDivider()
                val sharePct = ((node.weight / weightSum) * 100f).coerceIn(0f, 100f)
                TasteStepperRow(
                    title = node.displayName,
                    weight = node.weight,
                    sharePercent = sharePct,
                    isPreferred = node.isPreferred || node.weight > TasteConstants.DEFAULT_NEUTRAL_WEIGHT,
                    isPushedLess = node.weight < TasteConstants.DEFAULT_NEUTRAL_WEIGHT,
                    isAutoAdded = node.isAutoAdded && !node.isPreferred,
                    onDecrease = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        TasteProfileRepository.stepNodeWeight(node.id, -STEP_DELTA)
                    },
                    onIncrease = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        TasteProfileRepository.stepNodeWeight(node.id, +STEP_DELTA)
                    },
                    onBlock = { onBlockNode(node) },
                )
            }
        }
    }
}

@Composable
private fun ArtistsTab(
    snapshot: TasteSnapshot,
    searchQuery: String,
    onBlockNode: (TasteNode) -> Unit,
) {
    var filter by remember { mutableStateOf(TuneFilter.ALL) }
    val haptic = LocalHapticFeedback.current

    val activeArtists = remember(snapshot) {
        snapshot.activeNodes(TasteCategory.ARTIST)
            .distinctBy { TasteConstants.normalizeArtistKey(it.displayName) }
    }
    val blockedArtists = remember(snapshot) {
        snapshot.blockedNodes(TasteCategory.ARTIST)
            .distinctBy { TasteConstants.normalizeArtistKey(it.displayName) }
    }
    val markedAi = remember(snapshot) { snapshot.userMarkedAiArtists.sorted() }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                TuneFilter.ALL to stringResource(R.string.audio_choices_filter_all),
                TuneFilter.BOOSTED to stringResource(R.string.audio_choices_boosted),
                TuneFilter.LESS to stringResource(R.string.audio_choices_pushed_less),
                TuneFilter.BLOCKED to stringResource(R.string.audio_choices_tab_blocked),
            ).forEach { (f, label) ->
                FilterPill(
                    label = label,
                    selected = filter == f,
                    compact = true,
                    onClick = { filter = f },
                )
            }
        }

        if (filter == TuneFilter.BLOCKED) {
            val filteredBlocked = remember(blockedArtists, searchQuery) {
                blockedArtists.filter { matchesSearch(searchQuery, it.displayName, it.key) }
            }
            val filteredAi = remember(markedAi, searchQuery) {
                markedAi.filter { matchesSearch(searchQuery, it) }
            }
            if (filteredBlocked.isEmpty() && filteredAi.isEmpty()) {
                SettingsGroup(header = null, topSpacing = 8.dp) {
                    Text(
                        text = stringResource(R.string.audio_choices_blocked_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = ROW_INSET, vertical = 18.dp),
                    )
                }
            } else {
                SettingsGroup(header = stringResource(R.string.audio_choices_blocked_artists_header), topSpacing = 8.dp) {
                    filteredBlocked.forEachIndexed { idx, node ->
                        if (idx > 0) RowDivider()
                        BlockedRow(
                            icon = Icons.Rounded.Person,
                            title = node.displayName,
                            subtitle = stringResource(R.string.audio_choices_blocked_artist_sub),
                            actionLabel = stringResource(R.string.audio_choices_unblock),
                            onAction = { TasteProfileRepository.unblockArtist(node.key) },
                        )
                    }
                    filteredAi.forEachIndexed { idx, key ->
                        if (idx > 0 || filteredBlocked.isNotEmpty()) RowDivider()
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
            return@Column
        }

        val filteredActive = remember(activeArtists, filter, searchQuery) {
            activeArtists
                .filter { node ->
                    when (filter) {
                        TuneFilter.ALL -> true
                        TuneFilter.BOOSTED -> node.weight > TasteConstants.DEFAULT_NEUTRAL_WEIGHT || node.isPreferred
                        TuneFilter.LESS -> node.weight < TasteConstants.DEFAULT_NEUTRAL_WEIGHT
                        TuneFilter.BLOCKED -> false
                    }
                }
                .filter { matchesSearch(searchQuery, it.displayName, it.key) }
        }

        if (filteredActive.isEmpty()) {
            if (searchQuery.isNotBlank()) {
                SettingsSearchEmpty(query = searchQuery)
            } else {
                SettingsGroup(header = null, topSpacing = 8.dp) {
                    Text(
                        text = stringResource(R.string.audio_choices_artists_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = ROW_INSET, vertical = 18.dp),
                    )
                }
            }
        } else {
            val weightSum = remember(activeArtists) {
                activeArtists.sumOf { it.weight.toDouble() }.toFloat().coerceAtLeast(1f)
            }
            SettingsGroup(
                header = stringResource(R.string.audio_choices_adjust_hint),
                topSpacing = 6.dp,
            ) {
                filteredActive.forEachIndexed { idx, node ->
                    key(node.id) {
                        if (idx > 0) RowDivider()
                        val sharePct = ((node.weight / weightSum) * 100f).coerceIn(0f, 100f)
                        TasteStepperRow(
                            title = node.displayName,
                            weight = node.weight,
                            sharePercent = sharePct,
                            isPreferred = node.isPreferred || node.weight > TasteConstants.DEFAULT_NEUTRAL_WEIGHT,
                            isPushedLess = node.weight < TasteConstants.DEFAULT_NEUTRAL_WEIGHT,
                            isAutoAdded = node.isAutoAdded && !node.isPreferred && node.weight >= TasteConstants.DEFAULT_NEUTRAL_WEIGHT,
                            onDecrease = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                TasteProfileRepository.stepNodeWeight(node.id, -STEP_DELTA)
                            },
                            onIncrease = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                TasteProfileRepository.stepNodeWeight(node.id, +STEP_DELTA)
                            },
                            onBlock = { onBlockNode(node) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SongsTab(
    snapshot: TasteSnapshot,
    searchQuery: String,
    onBlockSong: (Song) -> Unit,
) {
    var filter by remember { mutableStateOf(TuneFilter.ALL) }
    val haptic = LocalHapticFeedback.current

    val tunedList = remember(snapshot) {
        snapshot.tunedSongs.values
            .filterNot { it.videoId in snapshot.blockedSongIds }
            .sortedWith(compareByDescending<TunedSong> { it.weight }.thenByDescending { it.updatedAt })
    }
    val blockedList = remember(snapshot) {
        snapshot.blockedSongs.values.sortedByDescending { it.blockedAt }
    }

    val filteredTuned = remember(tunedList, filter, searchQuery) {
        tunedList
            .filter { entry ->
                when (filter) {
                    TuneFilter.ALL -> true
                    TuneFilter.BOOSTED -> entry.weight > TasteConstants.DEFAULT_NEUTRAL_WEIGHT
                    TuneFilter.LESS -> entry.weight < TasteConstants.DEFAULT_NEUTRAL_WEIGHT
                    TuneFilter.BLOCKED -> false
                }
            }
            .filter { matchesSearch(searchQuery, it.title, it.artistName) }
    }
    val filteredBlocked = remember(blockedList, filter, searchQuery) {
        if (filter != TuneFilter.ALL && filter != TuneFilter.BLOCKED) {
            emptyList()
        } else {
            blockedList.filter { matchesSearch(searchQuery, it.title, it.artistName) }
        }
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                TuneFilter.ALL to stringResource(R.string.audio_choices_filter_all),
                TuneFilter.BOOSTED to stringResource(R.string.audio_choices_boosted),
                TuneFilter.LESS to stringResource(R.string.audio_choices_pushed_less),
                TuneFilter.BLOCKED to stringResource(R.string.audio_choices_tab_blocked),
            ).forEach { (f, label) ->
                FilterPill(
                    label = label,
                    selected = filter == f,
                    compact = true,
                    onClick = { filter = f },
                )
            }
        }

        if (filteredTuned.isEmpty() && filteredBlocked.isEmpty()) {
            if (searchQuery.isNotBlank()) {
                SettingsSearchEmpty(query = searchQuery)
            } else {
                SettingsGroup(header = null, topSpacing = 8.dp) {
                    Text(
                        text = stringResource(R.string.audio_choices_songs_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = ROW_INSET, vertical = 18.dp),
                    )
                }
            }
            return@Column
        }

        if (filteredTuned.isNotEmpty()) {
            SettingsGroup(
                header = stringResource(R.string.audio_choices_adjust_hint),
                topSpacing = 6.dp,
            ) {
                filteredTuned.forEachIndexed { idx, entry ->
                    key(entry.videoId) {
                        if (idx > 0) RowDivider()
                        TasteStepperRow(
                            title = entry.title,
                            subtitle = entry.artistName,
                            weight = entry.weight,
                            sharePercent = entry.weight,
                            isPreferred = entry.weight > TasteConstants.DEFAULT_NEUTRAL_WEIGHT,
                            isPushedLess = entry.weight < TasteConstants.DEFAULT_NEUTRAL_WEIGHT,
                            isAutoAdded = false,
                            onDecrease = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                TasteProfileRepository.stepTunedSongWeight(entry.videoId, -STEP_DELTA)
                            },
                            onIncrease = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                TasteProfileRepository.stepTunedSongWeight(entry.videoId, +STEP_DELTA)
                            },
                            onBlock = {
                                onBlockSong(
                                    Song(
                                        videoId = entry.videoId,
                                        title = entry.title,
                                        artist = entry.artistName,
                                        thumbnailUrl = null,
                                    ),
                                )
                            },
                        )
                    }
                }
            }
        }

        if (filteredBlocked.isNotEmpty()) {
            SettingsGroup(
                header = stringResource(R.string.audio_choices_blocked_songs_header),
                topSpacing = 14.dp,
            ) {
                filteredBlocked.forEachIndexed { idx, song ->
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
}

@Composable
private fun TasteStepperRow(
    title: String,
    weight: Float,
    sharePercent: Float,
    isPreferred: Boolean,
    isPushedLess: Boolean,
    isAutoAdded: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onBlock: () -> Unit,
    subtitle: String? = null,
) {
    val canDecrease = weight > TasteConstants.FLOOR_LESS_WEIGHT + 0.1f
    val canIncrease = weight < TasteConstants.MAX_WEIGHT - 0.1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Title, subtitle, status pill, weight bar, %
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 6.dp, end = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (isPushedLess) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.audio_choices_pushed_less),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.14f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                } else if (isPreferred) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.audio_choices_boosted),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                } else if (isAutoAdded) {
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
                    text = String.format(Locale.US, "%.0f%%", sharePercent),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
                        .fillMaxWidth((weight / TasteConstants.MAX_WEIGHT).coerceIn(0.03f, 1f))
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            when {
                                isPushedLess -> MaterialTheme.colorScheme.error.copy(alpha = 0.72f)
                                isPreferred -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                            },
                        ),
                )
            }
        }

        // - stepper
        StepperCircleButton(
            icon = Icons.Rounded.Remove,
            contentDescription = stringResource(R.string.audio_choices_decrease),
            enabled = canDecrease,
            onClick = onDecrease,
        )
        Spacer(Modifier.width(6.dp))
        // + stepper
        StepperCircleButton(
            icon = Icons.Rounded.Add,
            contentDescription = stringResource(R.string.audio_choices_increase),
            enabled = canIncrease,
            onClick = onIncrease,
        )
        // Block
        IconButton(
            onClick = onBlock,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Block,
                contentDescription = stringResource(R.string.audio_choices_block_item),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun StepperCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val bg = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 0.09f else 0.03f)
    val fg = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 0.9f else 0.28f)
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = fg,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    compact: Boolean = false,
) {
    val bg = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    }
    val fg = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.84f)
    }
    val border = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
    }
    Box(
        modifier = Modifier
            .heightIn(min = if (compact) 32.dp else 36.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, border, CircleShape)
            .clickable(onClick = onClick)
            .padding(
                horizontal = if (compact) 12.dp else 14.dp,
                vertical = if (compact) 6.dp else 8.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = if (compact) 12.sp else 13.sp,
            ),
            color = fg,
            maxLines = 1,
        )
    }
}

@Composable
private fun BlockedItemsTab(
    snapshot: TasteSnapshot,
    searchQuery: String,
) {
    var subFilter by remember { mutableStateOf(BlockedSubFilter.ALL) }

    val blockedTags = remember(snapshot, searchQuery) {
        (snapshot.blockedNodes(TasteCategory.LANGUAGE) +
            snapshot.blockedNodes(TasteCategory.GENRE) +
            snapshot.blockedNodes(TasteCategory.THEME))
            .filter { matchesSearch(searchQuery, it.displayName, it.key) }
    }
    val blockedArtists = remember(snapshot, searchQuery) {
        snapshot.blockedNodes(TasteCategory.ARTIST)
            .distinctBy { TasteConstants.normalizeArtistKey(it.displayName) }
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

    val showTags = (subFilter == BlockedSubFilter.ALL || subFilter == BlockedSubFilter.TAGS) && blockedTags.isNotEmpty()
    val showArtists = (subFilter == BlockedSubFilter.ALL || subFilter == BlockedSubFilter.ARTISTS) && blockedArtists.isNotEmpty()
    val showAi = (subFilter == BlockedSubFilter.ALL || subFilter == BlockedSubFilter.AI_ACTS) && markedAiArtists.isNotEmpty()
    val showSongs = (subFilter == BlockedSubFilter.ALL || subFilter == BlockedSubFilter.SONGS) && blockedSongs.isNotEmpty()

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                BlockedSubFilter.ALL to stringResource(R.string.audio_choices_filter_all),
                BlockedSubFilter.ARTISTS to "${stringResource(R.string.audio_choices_tab_artists)} (${blockedArtists.size})",
                BlockedSubFilter.SONGS to "${stringResource(R.string.audio_choices_tab_songs)} (${blockedSongs.size})",
                BlockedSubFilter.AI_ACTS to "${stringResource(R.string.audio_choices_filter_ai_acts)} (${markedAiArtists.size})",
                BlockedSubFilter.TAGS to stringResource(R.string.audio_choices_blocked_tags_header),
            ).forEach { (f, label) ->
                FilterPill(
                    label = label,
                    selected = subFilter == f,
                    compact = true,
                    onClick = { subFilter = f },
                )
            }
        }

        if (!showTags && !showArtists && !showAi && !showSongs) {
            SettingsGroup(header = null, topSpacing = 8.dp) {
                Text(
                    text = stringResource(R.string.audio_choices_blocked_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = ROW_INSET, vertical = 18.dp),
                )
            }
            return@Column
        }

        if (showArtists || showAi) {
            SettingsGroup(header = stringResource(R.string.audio_choices_blocked_artists_header), topSpacing = 8.dp) {
                if (showArtists) {
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
                }
                if (showAi) {
                    markedAiArtists.forEachIndexed { idx, key ->
                        if (idx > 0 || showArtists) RowDivider()
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
        }

        if (showSongs) {
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

        if (showTags) {
            SettingsGroup(header = stringResource(R.string.audio_choices_blocked_tags_header), topSpacing = 14.dp) {
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
