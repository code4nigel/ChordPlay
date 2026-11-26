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
package com.music.bitchord.ui.screens.settings.previews

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.ThemeMode
import com.music.bitchord.ui.player.LyricsHighlightPosition
import com.music.bitchord.ui.player.LyricsStyleManager
import com.music.bitchord.ui.player.rememberLyricsFontFamily
import com.music.bitchord.ui.screens.settings.localizedLabel

/**
 * Live interactive Lyrics preview reflecting the user's real font family, scale,
 * weight, alignment, highlight position (`TOP` / `UPPER_CENTER` / `CENTER`),
 * synced lyrics state, unfocused lyrics blur, and translation setting.
 */
@Composable
fun LyricsLivePreview(
    modifier: Modifier = Modifier,
) {
    val showLivePreviews by AppSettings.showSettingsLivePreviews.collectAsStateWithLifecycle()
    if (!showLivePreviews) return

    val customStyle by LyricsStyleManager.style.collectAsStateWithLifecycle()
    val syncedLyrics by AppSettings.syncedLyrics.collectAsStateWithLifecycle()
    val lyricsBlur by AppSettings.lyricsBlur.collectAsStateWithLifecycle()
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val translationLanguage by AppSettings.translationLanguage.collectAsStateWithLifecycle()
    val fullscreenStyle by AppSettings.fullscreenArtworkLyricsStyle.collectAsStateWithLifecycle()
    val themeMode by AppSettings.themeMode.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    val activeTitle by AppSettings.activeTrackTitle.collectAsStateWithLifecycle()

    val isLight = when (themeMode) {
        ThemeMode.LIGHT -> true
        ThemeMode.DARK -> false
        ThemeMode.SYSTEM -> !systemDark
    }

    val fontFamily = rememberLyricsFontFamily(customStyle)
    val scale = customStyle.sizeScale.scale
    val baseFontSize = 16.5.sp * scale
    val baseLineHeight = 23.sp * scale
    val textAlign = if (customStyle.centerAligned) TextAlign.Center else TextAlign.Start

    val activePrimary = MaterialTheme.colorScheme.primary
    val baseInk = if (isLight) Color(0xFF14151A) else Color.White
    val subtextColor = MaterialTheme.colorScheme.onSurfaceVariant

    val lines = listOf(
        "Hold on to what you love",
        "Before the moment fades away",
        "And the music takes you home",
        "Under the quiet city lights",
    )
    // Shift active index vertically to match real Highlight Position setting
    val activeIndex = when (customStyle.highlightPosition) {
        LyricsHighlightPosition.TOP -> 0
        LyricsHighlightPosition.UPPER_CENTER -> 1
        LyricsHighlightPosition.CENTER -> 2
    }

    val syncStatus = if (syncedLyrics) customStyle.highlightPosition.localizedLabel() else "Unsynced"
    val fontLabel = customStyle.customFontName ?: customStyle.fontChoice.localizedLabel()
    val titleSuffix = activeTitle?.takeIf { it.isNotBlank() }?.let { " • $it" }.orEmpty()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
            .padding(14.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = if (customStyle.centerAligned) Alignment.CenterHorizontally else Alignment.Start,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(activePrimary),
                )
                Text(
                    text = "PREVIEW",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    ),
                    color = activePrimary,
                )
                Text(
                    text = "• $fontLabel • ${customStyle.sizeScale.localizedLabel()} • $syncStatus$titleSuffix",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = subtextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(12.dp))

            lines.forEachIndexed { idx, line ->
                val isCurrent = syncedLyrics && idx == activeIndex
                val blurUnfocused = syncedLyrics && !isCurrent && lyricsBlur && !reduceDynamicBlur
                val lineModifier = Modifier
                    .fillMaxWidth()
                    .then(if (blurUnfocused) Modifier.blur(1.4.dp) else Modifier)

                Text(
                    text = line,
                    fontFamily = fontFamily,
                    fontSize = if (isCurrent || !syncedLyrics) baseFontSize else baseFontSize * 0.88f,
                    lineHeight = if (isCurrent || !syncedLyrics) baseLineHeight else baseLineHeight * 0.88f,
                    fontWeight = if (isCurrent) customStyle.weightChoice.weight else FontWeight.Normal,
                    textAlign = textAlign,
                    color = when {
                        !syncedLyrics -> baseInk.copy(alpha = 0.85f)
                        isCurrent -> activePrimary
                        else -> baseInk.copy(alpha = if (lyricsBlur) 0.30f else 0.55f)
                    },
                    modifier = lineModifier,
                )

                if (isCurrent && translationLanguage.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.translation_language) + " (${translationLanguage.uppercase()})",
                        fontFamily = fontFamily,
                        fontSize = (11.5 * scale).sp,
                        lineHeight = (15 * scale).sp,
                        textAlign = textAlign,
                        color = subtextColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                    )
                }

                if (idx < lines.lastIndex) {
                    Spacer(Modifier.height(5.dp))
                }
            }

            if (syncedLyrics) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = fullscreenStyle.label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                    color = subtextColor.copy(alpha = 0.8f),
                )
            }
        }
    }
}
