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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAddCheck
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.bitchord.data.model.ROW_ART_PX
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.data.taste.SongTagDetails
import com.music.bitchord.data.taste.TagResolver
import com.music.bitchord.data.taste.TasteCategory
import com.music.bitchord.data.taste.TasteProfileRepository
import com.music.bitchord.data.taste.TasteTag
import com.music.bitchord.sharedui.resources.Res
import com.music.bitchord.sharedui.resources.chord_wave
import com.music.bitchord.sharedui.resources.taste_action_less
import com.music.bitchord.sharedui.resources.taste_action_more
import com.music.bitchord.sharedui.resources.taste_action_never
import com.music.bitchord.sharedui.resources.taste_artist_label
import com.music.bitchord.sharedui.resources.taste_audio_choices
import com.music.bitchord.sharedui.resources.taste_detecting
import com.music.bitchord.sharedui.resources.taste_genre_label
import com.music.bitchord.sharedui.resources.taste_language_label
import com.music.bitchord.sharedui.resources.taste_mark_ai
import com.music.bitchord.sharedui.resources.taste_no_song_hint
import com.music.bitchord.sharedui.resources.taste_song_label
import com.music.bitchord.sharedui.resources.taste_theme_label
import com.music.bitchord.sharedui.resources.taste_tune_subtitle
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import com.music.bitchord.ui.player.PlayerPlatform
import com.music.bitchord.ui.theme.rememberArtworkPalette
import dev.chrisbanes.haze.HazeState
import org.jetbrains.compose.resources.stringResource

enum class TasteTuneAction {
    MORE,
    LESS,
    NEVER,
}

/**
 * Frosted-glass Chord Wave long-press tuning sheet.
 *
 * Displays resolved Song, Artist, Genre, Theme, and Language dimensions with instant
 * [More], [Less], and [Never] controls plus optional AI artist flagging and shortcut
 * to `Settings -> Audio -> Audio Choices`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChordWaveTuneSheet(
    visible: Boolean,
    song: Song?,
    onDismiss: () -> Unit,
    onSongAction: (Song, TasteTuneAction) -> Unit,
    onArtistAction: (artistName: String, artistId: String?, TasteTuneAction) -> Unit,
    onTagAction: (TasteTag, TasteTuneAction) -> Unit,
    onMarkArtistAi: ((artistName: String, artistId: String?) -> Unit)? = null,
    onOpenAudioChoices: (() -> Unit)? = null,
    hazeState: HazeState? = null,
) {
    if (!visible) return

    val snapshot by TasteProfileRepository.snapshot.collectAsStateWithLifecycle()
    val tagRevision by TasteProfileRepository.tagRevision.collectAsStateWithLifecycle()
    val reduceDynamicBlur by PlayerPlatform.host.settings.reduceDynamicBlur.collectAsStateWithLifecycle()

    val details: SongTagDetails? = remember(song, snapshot, tagRevision) {
        song?.let { TagResolver.songDetails(it, snapshot) }
    }

    val haptics = rememberHaptics()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val palette = rememberArtworkPalette(song?.thumbnailUrl, dark = isDark)
    val accent = palette.accent
    val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val baseSurface = remember(isDark, accent) {
        val ground = if (isDark) Color(0xFF14161D) else Color(0xFFF6F7FA)
        lerp(ground, accent, if (isDark) 0.14f else 0.08f)
    }
    val borderColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = sheetShape,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.32f),
        dragHandle = null,
    ) {
        val frostModifier = if (hazeState != null && !reduceDynamicBlur) {
            Modifier
                .clip(sheetShape)
                .optimizedHazeEffect(
                    state = hazeState,
                    style = menuFrost(baseSurface, alpha = 0.80f),
                )
        } else {
            Modifier
                .clip(sheetShape)
                .background(baseSurface.copy(alpha = 0.96f))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(frostModifier)
                .border(1.dp, borderColor, sheetShape)
                .navigationBarsPadding()
                .padding(bottom = 18.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)),
                )
            }

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (song?.thumbnailUrl != null) {
                    AsyncImage(
                        model = song.artworkAt(ROW_ART_PX),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(SmoothCornerShape(12.dp))
                            .border(0.75.dp, Color.White.copy(alpha = 0.14f), SmoothCornerShape(12.dp)),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(SmoothCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(accent.copy(alpha = 0.35f), accent.copy(alpha = 0.14f)),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Waves,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.chord_wave),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(Res.string.taste_tune_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (onOpenAudioChoices != null) {
                    TextButton(
                        onClick = {
                            haptics.play(Haptic.Tap)
                            onDismiss()
                            onOpenAudioChoices()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(Res.string.taste_audio_choices),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                thickness = 0.5.dp,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (details == null || song == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(SmoothCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(18.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.taste_no_song_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    val detectingLabel = stringResource(Res.string.taste_detecting)

                    // 1. Song
                    TuneDimensionCard(
                        icon = Icons.Rounded.MusicNote,
                        categoryLabel = stringResource(Res.string.taste_song_label),
                        valueLabel = song.title,
                        accent = accent,
                        enabled = true,
                        onAction = { action ->
                            haptics.play(if (action == TasteTuneAction.NEVER) Haptic.Select else Haptic.Tap)
                            onSongAction(song, action)
                            if (action == TasteTuneAction.NEVER) onDismiss()
                        },
                    )

                    // 2. Artist (guaranteed fallback)
                    TuneDimensionCard(
                        icon = Icons.Rounded.Person,
                        categoryLabel = stringResource(Res.string.taste_artist_label),
                        valueLabel = details.artistName,
                        accent = accent,
                        enabled = true,
                        extraActionLabel = if (onMarkArtistAi != null && snapshot.hideAiMusic) {
                            stringResource(Res.string.taste_mark_ai)
                        } else {
                            null
                        },
                        onExtraAction = if (onMarkArtistAi != null && snapshot.hideAiMusic) {
                            {
                                haptics.play(Haptic.Select)
                                onMarkArtistAi(details.artistName, details.artistId)
                                onDismiss()
                            }
                        } else {
                            null
                        },
                        onAction = { action ->
                            haptics.play(if (action == TasteTuneAction.NEVER) Haptic.Select else Haptic.Tap)
                            onArtistAction(details.artistName, details.artistId, action)
                            if (action == TasteTuneAction.NEVER) onDismiss()
                        },
                    )

                    // 3. Genre
                    val genre = details.genre
                    TuneDimensionCard(
                        icon = Icons.AutoMirrored.Rounded.PlaylistAddCheck,
                        categoryLabel = stringResource(Res.string.taste_genre_label),
                        valueLabel = genre?.displayName ?: if (details.isDetecting) detectingLabel else "—",
                        accent = accent,
                        enabled = genre != null,
                        onAction = { action ->
                            if (genre != null) {
                                haptics.play(if (action == TasteTuneAction.NEVER) Haptic.Select else Haptic.Tap)
                                onTagAction(genre, action)
                                if (action == TasteTuneAction.NEVER) onDismiss()
                            }
                        },
                    )

                    // 4. Theme
                    val theme = details.theme
                    TuneDimensionCard(
                        icon = Icons.Rounded.Waves,
                        categoryLabel = stringResource(Res.string.taste_theme_label),
                        valueLabel = theme?.displayName ?: if (details.isDetecting) detectingLabel else "—",
                        accent = accent,
                        enabled = theme != null,
                        onAction = { action ->
                            if (theme != null) {
                                haptics.play(if (action == TasteTuneAction.NEVER) Haptic.Select else Haptic.Tap)
                                onTagAction(theme, action)
                                if (action == TasteTuneAction.NEVER) onDismiss()
                            }
                        },
                    )

                    // 5. Language
                    val language = details.language
                    TuneDimensionCard(
                        icon = Icons.Rounded.Language,
                        categoryLabel = stringResource(Res.string.taste_language_label),
                        valueLabel = language?.displayName ?: if (details.isDetecting) detectingLabel else "—",
                        accent = accent,
                        enabled = language != null,
                        onAction = { action ->
                            if (language != null) {
                                haptics.play(if (action == TasteTuneAction.NEVER) Haptic.Select else Haptic.Tap)
                                onTagAction(language, action)
                                if (action == TasteTuneAction.NEVER) onDismiss()
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TuneDimensionCard(
    icon: ImageVector,
    categoryLabel: String,
    valueLabel: String,
    accent: Color,
    enabled: Boolean,
    onAction: (TasteTuneAction) -> Unit,
    extraActionLabel: String? = null,
    onExtraAction: (() -> Unit)? = null,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardBg = if (isDark) {
        Color.White.copy(alpha = 0.06f)
    } else {
        Color.Black.copy(alpha = 0.04f)
    }
    val shape = SmoothCornerShape(16.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cardBg)
            .border(0.75.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f), shape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "$categoryLabel: ",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (extraActionLabel != null && onExtraAction != null) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.14f))
                        .clickable(onClick = onExtraAction)
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SmartToy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = extraActionLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TunePillButton(
                label = stringResource(Res.string.taste_action_more),
                icon = Icons.Rounded.ThumbUp,
                tint = accent,
                enabled = enabled,
                onClick = { onAction(TasteTuneAction.MORE) },
                modifier = Modifier.weight(1f),
            )
            TunePillButton(
                label = stringResource(Res.string.taste_action_less),
                icon = Icons.Rounded.ThumbDown,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                enabled = enabled,
                onClick = { onAction(TasteTuneAction.LESS) },
                modifier = Modifier.weight(1f),
            )
            TunePillButton(
                label = stringResource(Res.string.taste_action_never),
                icon = Icons.Rounded.Block,
                tint = MaterialTheme.colorScheme.error,
                enabled = enabled,
                onClick = { onAction(TasteTuneAction.NEVER) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TunePillButton(
    label: String,
    icon: ImageVector,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = tint.copy(alpha = if (enabled) 0.13f else 0.05f)
    val fg = tint.copy(alpha = if (enabled) 1f else 0.35f)
    Row(
        modifier = modifier
            .heightIn(min = 42.dp)
            .clip(SmoothCornerShape(12.dp))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
