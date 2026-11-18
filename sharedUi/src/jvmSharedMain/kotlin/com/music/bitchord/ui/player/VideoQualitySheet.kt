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
package com.music.bitchord.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.settings.VideoQuality
import com.music.bitchord.ui.components.optimizedHazeEffect
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.HazeMaterials

private val QualityCardShape = RoundedCornerShape(24.dp)

/**
 * Unified floating translucent/Haze-blurred Video Playback Quality selector.
 * Used by both `VideoControlsDock` and the 3-dot `IosMenuPopover` / `SongActionsSheet`.
 */
@Composable
fun VideoQualitySheet(
    hazeState: HazeState?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color? = null,
) {
    val haptics = rememberHaptics()
    val activeQuality by PlayerSettings.videoQuality.collectAsStateWithLifecycle()
    val effectiveAccent = accentColor ?: MaterialTheme.colorScheme.primary

    val cardContent: @Composable (Modifier) -> Unit = { cardModifier ->
        val cardSurface = remember(hazeState) {
            Brush.verticalGradient(
                listOf(
                    Color(0xFF28282E).copy(alpha = if (hazeState != null) 0.56f else 0.94f),
                    Color(0xFF18181C).copy(alpha = if (hazeState != null) 0.72f else 0.96f),
                ),
            )
        }
        Column(
            modifier = cardModifier
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .clip(QualityCardShape)
                .then(
                    if (hazeState != null) {
                        Modifier.optimizedHazeEffect(
                            state = hazeState,
                            style = HazeMaterials.ultraThin(),
                        )
                    } else {
                        Modifier
                    },
                )
                .background(cardSurface)
                .border(0.8.dp, Color.White.copy(alpha = 0.14f), QualityCardShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(horizontal = 22.dp, vertical = 20.dp),
        ) {
            Text(
                text = "Video Playback Quality",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Select preferred maximum video resolution for playback:",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.65f),
            )
            Spacer(Modifier.height(14.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                VideoQuality.entries.forEach { quality ->
                    val isSelected = quality == activeQuality
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) effectiveAccent.copy(alpha = 0.18f)
                                else Color.White.copy(alpha = 0.06f),
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                haptics.play(Haptic.Select)
                                PlayerPlatform.host.settings.setVideoQuality(quality)
                                onDismiss()
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = quality.label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) effectiveAccent else Color.White.copy(alpha = 0.92f),
                            modifier = Modifier.weight(1f),
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Selected",
                                tint = effectiveAccent,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Close",
                        color = effectiveAccent,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }

    if (hazeState != null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.42f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                )
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            cardContent(Modifier)
        }
    } else {
        cardContent(modifier.padding(horizontal = 24.dp))
    }
}
