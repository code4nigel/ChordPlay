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

import com.music.bitchord.sharedui.resources.*
import org.jetbrains.compose.resources.stringResource

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.FilterCenterFocus
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import java.util.Locale

/**
 * Quick mood filter categories for ChordPlay Living Home Screen.
 * Pure text labels without emojis for a clean, professional aesthetic.
 */
enum class HomeMood(
    val label: String,
    val keywords: List<String>,
) {
    ALL("All", emptyList()),
    CHILL("Chill", listOf("chill", "relax", "lo-fi", "lofi", "peaceful", "calm", "acoustic", "ambient", "soft", "jazz", "mellow")),
    SAD("Sad", listOf("sad", "heartbreak", "cry", "tear", "emotional", "melancholy", "breakup", "lonely", "grief", "pain", "acoustic", "slow", "blue")),
    ENERGY("Energy", listOf("energy", "upbeat", "dance", "electronic", "hype", "boost", "rock", "hits", "edm", "pop", "power")),
    FOCUS("Focus", listOf("focus", "study", "work", "instrumental", "concentration", "deep", "piano", "classical", "reading")),
    WORKOUT("Workout", listOf("workout", "gym", "fitness", "running", "cardio", "power", "pump", "training", "exercise", "motivation")),
    PARTY("Party", listOf("party", "club", "dance", "celebration", "remix", "weekend", "banger", "festival", "night")),
    SLEEP("Sleep", listOf("sleep", "night", "bedtime", "dream", "meditation", "gentle", "rain", "lullaby", "serene"));

    /**
     * Filters home shelves based on keywords matching shelf titles, subtitles, or items.
     * Guarantees a non-empty result by falling back gracefully.
     */
    fun filterShelves(shelves: List<HomeShelf>): List<HomeShelf> {
        if (this == ALL || keywords.isEmpty()) return shelves

        val filtered = shelves.mapNotNull { shelf ->
            val titleMatches = keywords.any { kw ->
                shelf.title.lowercase(Locale.ROOT).contains(kw) ||
                    shelf.subtitle.lowercase(Locale.ROOT).contains(kw)
            }

            if (titleMatches) {
                shelf
            } else {
                val matchingItems = shelf.items.filter { item ->
                    keywords.any { kw ->
                        item.title.lowercase(Locale.ROOT).contains(kw) ||
                            item.subtitle.lowercase(Locale.ROOT).contains(kw)
                    }
                }
                if (matchingItems.isNotEmpty()) {
                    shelf.copy(items = matchingItems)
                } else {
                    null
                }
            }
        }

        return if (filtered.isNotEmpty()) filtered else shelves
    }
}

@Composable
fun HomeMood.localizedLabel(): String = when (this) {
    HomeMood.ALL -> stringResource(Res.string.mood_all)
    HomeMood.CHILL -> stringResource(Res.string.mood_chill)
    HomeMood.SAD -> stringResource(Res.string.mood_sad)
    HomeMood.ENERGY -> stringResource(Res.string.mood_energy)
    HomeMood.FOCUS -> stringResource(Res.string.mood_focus)
    HomeMood.WORKOUT -> stringResource(Res.string.mood_workout)
    HomeMood.PARTY -> stringResource(Res.string.mood_party)
    HomeMood.SLEEP -> stringResource(Res.string.mood_sleep)
}

private fun HomeMood.icon(): ImageVector = when (this) {
    HomeMood.ALL -> Icons.Rounded.AutoAwesome
    HomeMood.CHILL -> Icons.Outlined.Spa
    HomeMood.SAD -> Icons.Outlined.Bedtime
    HomeMood.ENERGY -> Icons.Outlined.Bolt
    HomeMood.FOCUS -> Icons.Outlined.FilterCenterFocus
    HomeMood.WORKOUT -> Icons.Outlined.FitnessCenter
    HomeMood.PARTY -> Icons.Rounded.MusicNote
    HomeMood.SLEEP -> Icons.Outlined.NightsStay
}

/**
 * Horizontally scrollable glass mood filter chips for quick vibes on the Home screen.
 */
@Composable
fun MoodFilterChips(
    selectedMood: HomeMood,
    onMoodSelect: (HomeMood) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = PAGE_GUTTER, vertical = 6.dp),
    accentColors: List<Color> = emptyList(),
) {
    val rowState = rememberLazyListState()
    val haptics = rememberHaptics()
    val fadeLeft by remember { derivedStateOf { rowState.canScrollBackward } }
    val fadeRight by remember { derivedStateOf { rowState.canScrollForward } }

    LazyRow(
        state = rowState,
        modifier = modifier
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                val fadePx = 22.dp.toPx().coerceAtMost(size.width * 0.12f)
                if (fadePx > 1f) {
                    if (fadeLeft) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                0f to Color.Transparent,
                                1f to Color.Black,
                                startX = 0f,
                                endX = fadePx,
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    }
                    if (fadeRight) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                0f to Color.Black,
                                1f to Color.Transparent,
                                startX = size.width - fadePx,
                                endX = size.width,
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    }
                }
            },
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(HomeMood.entries, key = { it.name }) { mood ->
            MoodChip(
                mood = mood,
                isSelected = mood == selectedMood,
                accentColors = accentColors,
                onClick = {
                    if (mood != selectedMood) {
                        haptics.play(Haptic.Tap)
                    }
                    onMoodSelect(mood)
                },
            )
        }
    }
}

@Composable
private fun MoodChip(
    mood: HomeMood,
    isSelected: Boolean,
    accentColors: List<Color>,
    onClick: () -> Unit,
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = accentColors.getOrNull(1)
        ?: accentColors.firstOrNull()?.let { lerp(primaryColor, it, 0.45f) }
        ?: MaterialTheme.colorScheme.tertiary

    val activeStart = remember(accentColors, primaryColor, isDarkTheme) {
        val raw = accentColors.firstOrNull()?.let { lerp(primaryColor, it, 0.35f) } ?: primaryColor
        if (isDarkTheme && raw.luminance() < 0.14f) lerp(raw, primaryColor, 0.55f) else raw
    }
    val activeEnd = remember(secondaryColor, primaryColor, isDarkTheme) {
        val raw = lerp(primaryColor, secondaryColor, 0.52f)
        if (isDarkTheme && raw.luminance() < 0.14f) lerp(raw, primaryColor, 0.55f) else raw
    }
    val activeMeanLuminance = (activeStart.luminance() + activeEnd.luminance()) * 0.5f
    val isLightPrimary = activeMeanLuminance > 0.48f

    val unselectedGlassFill = if (isDarkTheme) {
        Color.White.copy(alpha = 0.075f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    }

    val startBg by animateColorAsState(
        targetValue = if (isSelected) activeStart else unselectedGlassFill,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "moodChipStart",
    )
    val endBg by animateColorAsState(
        targetValue = if (isSelected) activeEnd else unselectedGlassFill,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "moodChipEnd",
    )

    val selectedContentColor = if (isLightPrimary) Color(0xFF0F080B) else Color.White
    val unselectedContentColor = if (isDarkTheme) {
        Color.White.copy(alpha = 0.84f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
    }

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) selectedContentColor else unselectedContentColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "moodChipContent",
    )

    val topBorderColor by animateColorAsState(
        targetValue = if (isSelected) {
            if (isLightPrimary) Color.Black.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.38f)
        } else {
            if (isDarkTheme) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.10f)
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "moodChipTopBorder",
    )
    val bottomBorderColor by animateColorAsState(
        targetValue = if (isSelected) {
            if (isLightPrimary) Color.Black.copy(alpha = 0.08f) else activeEnd.copy(alpha = 0.45f)
        } else {
            if (isDarkTheme) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.05f)
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "moodChipBottomBorder",
    )

    val interactionSource = remember { MutableInteractionSource() }
    val chipLabel = mood.localizedLabel()

    Box(
        modifier = Modifier
            .defaultMinSize(minHeight = 36.dp)
            .then(
                if (isSelected) {
                    Modifier.shadow(
                        elevation = if (isLightPrimary) 4.dp else 10.dp,
                        shape = CircleShape,
                        spotColor = if (isLightPrimary) Color.Black.copy(alpha = 0.25f) else activeStart.copy(alpha = 0.55f),
                        ambientColor = if (isLightPrimary) Color.Black.copy(alpha = 0.12f) else activeEnd.copy(alpha = 0.30f),
                    )
                } else {
                    Modifier
                },
            )
            .clip(CircleShape)
            .background(Brush.horizontalGradient(listOf(startBg, endBg)))
            .then(
                if (!isSelected) {
                    Modifier.background(
                        Brush.verticalGradient(
                            0f to Color.White.copy(alpha = if (isDarkTheme) 0.07f else 0.18f),
                            0.55f to Color.Transparent,
                        ),
                    )
                } else {
                    Modifier.background(
                        Brush.verticalGradient(
                            0f to Color.White.copy(alpha = if (isLightPrimary) 0.10f else 0.18f),
                            0.5f to Color.Transparent,
                        ),
                    )
                },
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(listOf(topBorderColor, bottomBorderColor)),
                shape = CircleShape,
            )
            .semantics {
                selected = isSelected
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(horizontal = 13.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = mood.icon(),
                contentDescription = null,
                tint = contentColor.copy(alpha = if (isSelected) 0.96f else 0.78f),
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = chipLabel,
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = 0.15.sp,
                maxLines = 1,
            )
        }
    }
}
