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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.R
import com.music.bitchord.data.melody.MELODY_STARTER_ARTISTS
import com.music.bitchord.data.melody.MELODY_TUNER_GENRES
import com.music.bitchord.data.melody.MelodyMood
import com.music.bitchord.data.melody.MelodyStatus
import com.music.bitchord.data.melody.MelodyTuner
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.UiState
import com.music.bitchord.data.model.isSameTrackAs
import com.music.bitchord.ui.components.MessageState
import com.music.bitchord.ui.components.PAGE_GUTTER
import com.music.bitchord.ui.components.PillSegmentedControl
import com.music.bitchord.ui.components.PlayingAccent
import com.music.bitchord.ui.components.ROW_DIVIDER_INSET
import com.music.bitchord.ui.components.SongRow
import com.music.bitchord.ui.components.songListSkeleton
import com.music.bitchord.ui.icons.BitChordIcons

/**
 * ChordPlay Melody: endless personalized mix with Draft-until-Apply Tuner,
 * Mood strip, skip & ban training, and starter mood/artist onboarding so
 * brand-new users can start immediately without a 16-favorite lock wall.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MelodyScreen(
    tracksState: UiState<List<Song>>,
    mood: MelodyMood,
    tuner: MelodyTuner,
    status: MelodyStatus,
    bannedIds: Set<String>,
    starterMoods: Set<MelodyMood>,
    starterArtists: Set<String>,
    isSaved: Boolean,
    isGrowing: Boolean,
    rebuilding: Boolean,
    listState: LazyListState,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onSave: () -> Unit,
    onNewMix: () -> Unit,
    onToggleLike: () -> Unit,
    currentLiked: Boolean,
    onApply: (MelodyMood, Float, Float, Set<String>, Boolean) -> Unit,
    onApplyStarters: (Set<MelodyMood>, Set<String>) -> Unit,
    onSeedQuickPicks: () -> Unit,
    onUnban: (String) -> Unit,
    onBan: (Song) -> Unit,
    onSongClick: (List<Song>, Int) -> Unit,
    onSongLongPress: (Song) -> Unit,
    onRetry: () -> Unit,
    contentPadding: PaddingValues,
    currentSong: Song? = null,
    isPlaying: Boolean = false,
    modifier: Modifier = Modifier,
) {
    // Drafts reset whenever the committed config changes (after Apply).
    var draftMood by remember(mood) { mutableStateOf(mood) }
    var draftDiscovery by remember(tuner.discovery) { mutableFloatStateOf(tuner.discovery) }
    var draftMemory by remember(tuner.memory) { mutableFloatStateOf(tuner.memory) }
    var draftExcluded by remember(tuner.excludedGenres) { mutableStateOf(tuner.excludedGenres) }
    var draftSimilarLiked by remember(tuner.similarLiked) { mutableStateOf(tuner.similarLiked) }
    var showTuner by remember { mutableStateOf(false) }

    val dirty = draftMood != mood ||
        draftDiscovery != tuner.discovery ||
        draftMemory != tuner.memory ||
        draftExcluded != tuner.excludedGenres ||
        draftSimilarLiked != tuner.similarLiked
    val songs = (tracksState as? UiState.Success)?.data.orEmpty()

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        item(key = "melody:hero") {
            MelodyHero(
                mood = draftMood,
                trackCount = songs.size,
                isSaved = isSaved,
                isGrowing = isGrowing,
                rebuilding = rebuilding,
                tunerDirty = dirty,
                tunerOpen = showTuner,
                currentLiked = currentLiked,
                onPlay = onPlay,
                onShuffle = onShuffle,
                onSave = onSave,
                onToggleLike = onToggleLike,
                onTune = { showTuner = !showTuner },
            )
        }
        item(key = "melody:moods") {
            MelodyMoodStrip(
                mood = draftMood,
                onMood = { draftMood = it },
            )
        }
        if (dirty && !showTuner) {
            item(key = "melody:quick-apply") {
                Button(
                    onClick = {
                        onApply(draftMood, draftDiscovery, draftMemory, draftExcluded, draftSimilarLiked)
                    },
                    enabled = !rebuilding,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PAGE_GUTTER, vertical = 10.dp),
                ) {
                    if (rebuilding) {
                        CircularProgressIndicator(
                            Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        stringResource(R.string.melody_apply),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
        if (showTuner) {
            item(key = "melody:tuner") {
                val reveal = remember { MutableTransitionState(false) }
                LaunchedEffect(Unit) { reveal.targetState = true }
                AnimatedVisibility(
                    visibleState = reveal,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    MelodyTunerGroup(
                        tuner = tuner.copy(
                            discovery = draftDiscovery,
                            memory = draftMemory,
                            excludedGenres = draftExcluded,
                            similarLiked = draftSimilarLiked,
                        ),
                        onDiscovery = { draftDiscovery = it },
                        onMemory = { draftMemory = it },
                        onSimilarLiked = { draftSimilarLiked = it },
                        onToggleGenre = { genre, enabled ->
                            draftExcluded = if (enabled) draftExcluded - genre.lowercase()
                            else draftExcluded + genre.lowercase()
                        },
                        dirty = dirty,
                        rebuilding = rebuilding,
                        onApply = {
                            onApply(draftMood, draftDiscovery, draftMemory, draftExcluded, draftSimilarLiked)
                        },
                    )
                }
            }
        }
        if (songs.isNotEmpty()) {
            item(key = "melody:tracks-header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.melody_mix_title, songs.size),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onNewMix, enabled = !rebuilding) {
                        Icon(
                            Icons.Rounded.Refresh,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.melody_new_mix),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
        when (tracksState) {
            is UiState.Loading -> songListSkeleton(count = 8, keyPrefix = "skeleton:melody")
            is UiState.Error -> item(key = "melody:message") {
                MessageState(
                    message = tracksState.message,
                    actionLabel = stringResource(R.string.try_again),
                    onAction = onRetry,
                )
            }
            is UiState.Success -> {
                if (songs.isEmpty()) {
                    item(key = "melody:starter") {
                        MelodyStarterCard(
                            status = status,
                            initialMoods = starterMoods,
                            initialArtists = starterArtists,
                            rebuilding = rebuilding,
                            onBuildFromStarters = onApplyStarters,
                            onSeedQuickPicks = onSeedQuickPicks,
                        )
                    }
                } else {
                    items(
                        count = songs.size,
                        key = { "melody:${songs[it].videoId}:$it" },
                    ) { index ->
                        val song = songs[index]
                        Box(Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)) {
                            SongRow(
                                song = song,
                                onClick = { onSongClick(songs, index) },
                                onLongPress = { onSongLongPress(song) },
                                onSwipeToQueue = { onBan(song) },
                                isCurrent = song.isSameTrackAs(currentSong),
                                isPlaying = song.isSameTrackAs(currentSong) && isPlaying,
                                searchPlayingStyle = true,
                                activeTint = PlayingAccent,
                            )
                            if (index < songs.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = ROW_DIVIDER_INSET),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                )
                            }
                        }
                    }
                }
            }
        }
        if (bannedIds.isNotEmpty()) {
            item(key = "melody:banned") {
                MelodyBans(bannedIds = bannedIds, onUnban = onUnban)
            }
        }
        item(key = "melody:spacer") { Spacer(Modifier.height(24.dp)) }
    }
}

private const val MELODY_GENRES_COLLAPSED = 10

@Composable
private fun MelodyHero(
    mood: MelodyMood,
    trackCount: Int,
    isSaved: Boolean,
    isGrowing: Boolean,
    rebuilding: Boolean,
    tunerDirty: Boolean,
    tunerOpen: Boolean,
    currentLiked: Boolean,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onSave: () -> Unit,
    onToggleLike: () -> Unit,
    onTune: () -> Unit,
) {
    val fg = MaterialTheme.colorScheme.onBackground
    val dim = MaterialTheme.colorScheme.onSurfaceVariant
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val appear by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 320),
        label = "melody-hero-in",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 14.dp)
            .graphicsLayer {
                alpha = appear
                translationY = (1f - appear) * 18f
            },
        horizontalAlignment = Alignment.Start,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = PAGE_GUTTER),
        ) {
            Text(
                text = stringResource(R.string.melody),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = fg,
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Rounded.AllInclusive,
                contentDescription = null,
                tint = dim,
                modifier = Modifier.size(30.dp),
            )
        }
        AnimatedContent(
            targetState = if (mood == MelodyMood.MELODY) {
                stringResource(R.string.melody_full_name) + " · " + stringResource(R.string.melody_subtitle)
            } else {
                mood.localizedLabel() + " · " + stringResource(R.string.melody_full_name)
            },
            transitionSpec = {
                (fadeIn(animationSpec = tween(200)) + slideInVertically { it / 3 }) togetherWith
                    fadeOut(animationSpec = tween(120))
            },
            label = "melody-subtitle",
        ) { subtitle ->
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = dim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = PAGE_GUTTER),
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = PAGE_GUTTER),
        ) {
            AnimatedContent(
                targetState = when {
                    isSaved -> stringResource(R.string.saved)
                    trackCount > 0 -> stringResource(R.string.melody_temp_meta, trackCount)
                    else -> stringResource(R.string.melody_temp_playlist)
                },
                transitionSpec = {
                    (fadeIn(animationSpec = tween(180)) + slideInVertically { it / 2 }) togetherWith
                        fadeOut(animationSpec = tween(120))
                },
                label = "melody-meta",
            ) { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.7.sp),
                    color = dim,
                )
            }
            if (isGrowing && !isSaved && trackCount > 0) {
                Box(Modifier.size(5.dp).clip(CircleShape).background(dim))
                Text(
                    text = stringResource(R.string.melody_growing),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.7.sp),
                    color = dim,
                )
            }
            if (rebuilding) {
                CircularProgressIndicator(
                    Modifier.size(12.dp),
                    strokeWidth = 1.5.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                MelodyGlassCircle(
                    icon = Icons.Rounded.Tune,
                    description = stringResource(R.string.melody_tuner),
                    onClick = onTune,
                    selected = tunerOpen,
                )
                if (tunerDirty) {
                    Box(
                        Modifier
                            .padding(top = 2.dp, end = 2.dp)
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                    )
                }
            }
            MelodyGlassCircle(
                icon = BitChordIcons.Shuffle,
                description = stringResource(R.string.shuffle),
                onClick = onShuffle,
            )
            val press = remember { MutableInteractionSource() }
            val pressed by press.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (pressed) 0.94f else 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "melody-play-press",
            )
            Button(
                onClick = onPlay,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background,
                ),
                contentPadding = PaddingValues(0.dp),
                interactionSource = press,
                modifier = Modifier
                    .size(54.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
            ) {
                Icon(
                    BitChordIcons.Play,
                    contentDescription = stringResource(R.string.play),
                    modifier = Modifier.size(26.dp),
                )
            }
            MelodyGlassCircle(
                icon = if (isSaved) Icons.Rounded.Bookmark else BitChordIcons.PlaylistAdd,
                description = stringResource(if (isSaved) R.string.saved else R.string.melody_save),
                onClick = onSave,
                enabled = trackCount > 0,
                selected = isSaved,
            )
            MelodyGlassCircle(
                icon = if (currentLiked) BitChordIcons.HeartFilled else BitChordIcons.Heart,
                description = stringResource(R.string.like),
                onClick = onToggleLike,
                filled = currentLiked,
            )
        }
    }
}

@Composable
private fun MelodyGlassCircle(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    selected: Boolean = false,
    filled: Boolean = false,
    size: Dp = 50.dp,
) {
    val tint = when {
        filled -> Color(0xFFFF5B6E)
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onBackground
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            )
            .border(
                width = 0.75.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f),
                shape = CircleShape,
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            description,
            tint = tint.copy(alpha = if (enabled) 1f else 0.4f),
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
fun MelodyMood.localizedLabel(): String = when (this) {
    MelodyMood.MELODY -> stringResource(R.string.melody_mood_all)
    MelodyMood.LOVE -> stringResource(R.string.melody_mood_love)
    MelodyMood.WORKOUT -> stringResource(R.string.melody_mood_workout)
    MelodyMood.CHILL -> stringResource(R.string.melody_mood_chill)
    MelodyMood.SAD -> stringResource(R.string.melody_mood_sad)
    MelodyMood.FOCUS -> stringResource(R.string.melody_mood_focus)
    MelodyMood.PARTY -> stringResource(R.string.melody_mood_party)
}

@Composable
private fun MelodyMoodStrip(
    mood: MelodyMood,
    onMood: (MelodyMood) -> Unit,
) {
    val moodOptions = MelodyMood.entries.map { it.localizedLabel() }
    PillSegmentedControl(
        options = moodOptions,
        selectedIndex = MelodyMood.entries.indexOf(mood).coerceAtLeast(0),
        onSelect = { index -> onMood(MelodyMood.entries[index]) },
        modifier = Modifier.padding(horizontal = PAGE_GUTTER, vertical = 4.dp),
    )
}

@Composable
private fun MelodyTunerGroup(
    tuner: MelodyTuner,
    onDiscovery: (Float) -> Unit,
    onMemory: (Float) -> Unit,
    onSimilarLiked: (Boolean) -> Unit,
    onToggleGenre: (String, Boolean) -> Unit,
    dirty: Boolean,
    rebuilding: Boolean,
    onApply: () -> Unit,
) {
    SettingsGroup(
        header = stringResource(R.string.melody_tuner),
        topSpacing = 8.dp,
    ) {
        MelodyTunerSlider(
            icon = Icons.Rounded.Explore,
            title = stringResource(R.string.melody_discovery),
            startLabel = stringResource(R.string.melody_personal),
            endLabel = stringResource(R.string.melody_adventurous),
            value = tuner.discovery,
            onValue = onDiscovery,
            subtitle = stringResource(R.string.melody_discovery_subtitle),
        )
        RowDivider()
        MelodyTunerSlider(
            icon = Icons.Rounded.History,
            title = stringResource(R.string.melody_memory),
            startLabel = stringResource(R.string.melody_alltime),
            endLabel = stringResource(R.string.melody_recent),
            value = tuner.memory,
            onValue = onMemory,
            subtitle = stringResource(R.string.melody_memory_subtitle),
        )
        RowDivider()
        SettingsRow(
            icon = Icons.Rounded.Favorite,
            title = stringResource(R.string.melody_similar_liked),
            subtitle = stringResource(R.string.melody_similar_liked_subtitle),
            trailing = {
                Switch(
                    checked = tuner.similarLiked,
                    onCheckedChange = onSimilarLiked,
                )
            },
        )
        RowDivider()
        var showAllGenres by remember { mutableStateOf(false) }
        val shownGenres = if (showAllGenres) MELODY_TUNER_GENRES
        else MELODY_TUNER_GENRES.take(MELODY_GENRES_COLLAPSED)
        Column(
            Modifier
                .padding(start = ROW_INSET, end = ROW_INSET, top = 14.dp, bottom = 12.dp)
                .animateContentSize(),
        ) {
            Text(
                text = stringResource(R.string.melody_genres),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(10.dp))
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                shownGenres.forEach { genre ->
                    FilterChip(
                        selected = tuner.isGenreEnabled(genre),
                        onClick = { onToggleGenre(genre, !tuner.isGenreEnabled(genre)) },
                        label = { Text(genre) },
                    )
                }
            }
            if (MELODY_TUNER_GENRES.size > MELODY_GENRES_COLLAPSED) {
                val hidden = MELODY_TUNER_GENRES.size - MELODY_GENRES_COLLAPSED
                TextButton(
                    onClick = { showAllGenres = !showAllGenres },
                    modifier = Modifier.padding(top = 6.dp),
                ) {
                    Text(
                        if (showAllGenres) {
                            stringResource(R.string.melody_genres_fewer)
                        } else {
                            stringResource(R.string.melody_genres_more, hidden)
                        },
                    )
                }
            }
        }
        if (dirty) {
            RowDivider()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ROW_INSET, vertical = 12.dp),
            ) {
                Button(
                    onClick = onApply,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !rebuilding,
                ) {
                    if (rebuilding) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(stringResource(R.string.melody_apply))
                }
            }
        }
    }
}

@Composable
private fun MelodyTunerSlider(
    icon: ImageVector,
    title: String,
    startLabel: String,
    endLabel: String,
    value: Float,
    onValue: (Float) -> Unit,
    subtitle: String,
) {
    Column(Modifier.padding(start = ROW_INSET, end = ROW_INSET, top = 14.dp, bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(ICON_SIZE),
            )
            Spacer(Modifier.width(ICON_GAP))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = ICON_SIZE + ICON_GAP, top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = startLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = endLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        MelodySlider(
            value = value,
            onValueChange = onValue,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = ICON_SIZE + ICON_GAP),
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = ICON_SIZE + ICON_GAP, top = 2.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MelodySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.24f)
    val activeColor = MaterialTheme.colorScheme.primary
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        thumb = {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .shadow(elevation = 2.dp, shape = CircleShape, clip = false)
                        .background(activeColor, CircleShape),
                )
            }
        },
        track = { state ->
            val start = state.valueRange.start
            val end = state.valueRange.endInclusive
            val span = end - start
            val fraction = if (span == 0f) 0f else ((state.value.coerceIn(start, end) - start) / span).coerceIn(0f, 1f)
            Canvas(modifier = Modifier.fillMaxWidth().height(6.dp)) {
                val radius = size.height / 2f
                drawRoundRect(
                    color = inactiveColor,
                    cornerRadius = CornerRadius(radius, radius),
                )
                val activeWidth = size.width * fraction
                if (activeWidth > 0f) {
                    val left = if (layoutDirection == LayoutDirection.Rtl) size.width - activeWidth else 0f
                    drawRoundRect(
                        color = activeColor,
                        topLeft = Offset(left, 0f),
                        size = Size(activeWidth, size.height),
                        cornerRadius = CornerRadius(radius, radius),
                    )
                }
            }
        },
    )
}

/**
 * Starter onboarding card shown when no tracks have been built yet (for
 * brand-new accounts or empty local state). Replaces any hard lock wall so
 * Melody always works out of the box.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MelodyStarterCard(
    status: MelodyStatus,
    initialMoods: Set<MelodyMood>,
    initialArtists: Set<String>,
    rebuilding: Boolean,
    onBuildFromStarters: (Set<MelodyMood>, Set<String>) -> Unit,
    onSeedQuickPicks: () -> Unit,
) {
    var pickedMoods by remember(initialMoods) { mutableStateOf(initialMoods) }
    var pickedArtists by remember(initialArtists) { mutableStateOf(initialArtists) }
    val selectableMoods = remember { MelodyMood.entries.filter { it != MelodyMood.MELODY } }
    val hasPicks = pickedMoods.isNotEmpty() || pickedArtists.isNotEmpty()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AllInclusive, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.melody_starter_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.melody_starter_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.melody_starter_moods),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                selectableMoods.forEach { m ->
                    val selected = m in pickedMoods
                    FilterChip(
                        selected = selected,
                        onClick = {
                            pickedMoods = if (selected) pickedMoods - m else pickedMoods + m
                        },
                        label = { Text(m.localizedLabel()) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.melody_starter_artists),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MELODY_STARTER_ARTISTS.forEach { artist ->
                    val selected = artist in pickedArtists
                    FilterChip(
                        selected = selected,
                        onClick = {
                            pickedArtists = if (selected) pickedArtists - artist else pickedArtists + artist
                        },
                        label = { Text(artist) },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = { onBuildFromStarters(pickedMoods, pickedArtists) },
                    enabled = !rebuilding && (hasPicks || status.unlocked),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.melody_starter_build))
                }
                OutlinedButton(
                    onClick = onSeedQuickPicks,
                    enabled = !rebuilding,
                ) {
                    Text(stringResource(R.string.melody_starter_quick_picks))
                }
            }
        }
    }
}

@Composable
private fun MelodyBans(
    bannedIds: Set<String>,
    onUnban: (String) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Block,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.melody_banned),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(8.dp))
            bannedIds.take(20).forEach { id ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        id.take(11),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    AssistChip(onClick = { onUnban(id) }, label = { Text(stringResource(R.string.remove)) })
                }
            }
        }
    }
}

/** Name prompt for saving the temporary Melody mix to a playlist. */
@Composable
fun MelodySaveDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.melody_save_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(stringResource(R.string.playlist_name)) },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = name.isNotBlank(),
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
