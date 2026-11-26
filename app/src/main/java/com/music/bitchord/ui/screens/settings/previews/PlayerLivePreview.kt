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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.ChordPlayPalette
import com.music.bitchord.data.settings.PlayerLayoutMode
import com.music.bitchord.data.settings.PlayerWaveStyle
import com.music.bitchord.ui.icons.BitChordIcons
import com.music.bitchord.ui.screens.settings.localizedLabel
import com.music.bitchord.ui.theme.fontFamilyFor

/**
 * Live Playback preview mirroring ChordPlay's actual player settings:
 * layout mode (Modern Ambient vs Vinyl Turntable), breathing aura, legacy mesh gradient,
 * wave style (OG Wave / Aurora Silk / Audio Graph), song status pill, lyrics preview chip,
 * nerd stats badge, and active typography & color palette.
 */
@Composable
fun PlayerLivePreview(
    modifier: Modifier = Modifier,
) {
    val showLivePreviews by AppSettings.showSettingsLivePreviews.collectAsStateWithLifecycle()
    if (!showLivePreviews) return

    val appFontChoice by AppSettings.appFontChoice.collectAsStateWithLifecycle()
    val palette by AppSettings.colorPalette.collectAsStateWithLifecycle()
    val playerLayoutMode by AppSettings.playerLayoutMode.collectAsStateWithLifecycle()
    val playerWaveStyle by AppSettings.playerWaveStyle.collectAsStateWithLifecycle()
    val enableBreathingAura by AppSettings.enableBreathingAura.collectAsStateWithLifecycle()
    val legacyMeshGradient by AppSettings.legacyMeshGradient.collectAsStateWithLifecycle()
    val hideSongStatus by AppSettings.hideSongStatus.collectAsStateWithLifecycle()
    val hideLyricsPreview by AppSettings.hideLyricsPreview.collectAsStateWithLifecycle()
    val showNerdStats by AppSettings.showNerdStats.collectAsStateWithLifecycle()
    val playerAnimationSpeed by AppSettings.playerAnimationSpeed.collectAsStateWithLifecycle()
    val activeArtworkUrl by AppSettings.activeTrackArtworkUrl.collectAsStateWithLifecycle()
    val activeTitle by AppSettings.activeTrackTitle.collectAsStateWithLifecycle()
    val activeArtist by AppSettings.activeTrackArtist.collectAsStateWithLifecycle()
    val customPalettes by AppSettings.customPalettes.collectAsStateWithLifecycle()
    val activeCustomPaletteId by AppSettings.activeCustomPaletteId.collectAsStateWithLifecycle()
    val activeCustomPalette = activeCustomPaletteId?.let { id ->
        customPalettes.firstOrNull { it.id == id }
    }

    val fontFamily = fontFamilyFor(appFontChoice)

    val activePrimary = if (activeCustomPalette != null) {
        Color(activeCustomPalette.primaryHex)
    } else if (palette == ChordPlayPalette.DYNAMIC_MONET) {
        MaterialTheme.colorScheme.primary
    } else {
        Color(palette.primaryColorHex)
    }

    val activeSecondary = if (activeCustomPalette != null) {
        Color(activeCustomPalette.secondaryHex)
    } else if (palette == ChordPlayPalette.DYNAMIC_MONET) {
        MaterialTheme.colorScheme.secondary
    } else {
        Color(palette.secondaryColorHex)
    }

    val animatedPrimary by animateColorAsState(targetValue = activePrimary, animationSpec = tween(280), label = "playerPrimary")

    val isVinyl = playerLayoutMode == PlayerLayoutMode.VINYL_RECORD
    val waveLabel = stringResource(
        when (playerWaveStyle) {
            PlayerWaveStyle.OG_WAVE -> R.string.player_wave_og
            PlayerWaveStyle.AURORA_SILK -> R.string.player_wave_aurora_silk
            PlayerWaveStyle.AUDIO_GRAPH -> R.string.player_wave_audio_graph
        },
    )

    val textColor = Color.White
    val subtextColor = Color.White.copy(alpha = 0.65f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(16.dp, RoundedCornerShape(22.dp), spotColor = animatedPrimary.copy(alpha = 0.20f))
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF0C0D11))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(22.dp)),
    ) {
        // Backdrop reflects real `legacyMeshGradient` toggle
        Canvas(modifier = Modifier.matchParentSize()) {
            if (legacyMeshGradient) {
                drawRect(
                    Brush.verticalGradient(
                        listOf(
                            animatedPrimary.copy(alpha = 0.32f),
                            activeSecondary.copy(alpha = 0.18f),
                            Color(0xFF0C0D11),
                        ),
                    ),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(activeSecondary.copy(alpha = 0.28f), Color.Transparent),
                        center = Offset(size.width * 0.82f, size.height * 0.24f),
                        radius = size.maxDimension * 0.45f,
                    ),
                    radius = size.maxDimension * 0.45f,
                    center = Offset(size.width * 0.82f, size.height * 0.24f),
                )
            } else {
                drawRect(
                    Brush.verticalGradient(
                        listOf(
                            animatedPrimary.copy(alpha = 0.16f),
                            Color(0xFF0C0D11).copy(alpha = 0.78f),
                            Color(0xFF0C0D11),
                        ),
                    ),
                )
            }
        }

        CompositionLocalProvider(LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = fontFamily)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Header reflects real Layout + Wave Style + Animation Speed
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(animatedPrimary),
                    )
                    Text(
                        text = "PREVIEW",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = fontFamily,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                        ),
                        color = animatedPrimary,
                    )
                    Text(
                        text = "• ${playerLayoutMode.localizedLabel()} • $waveLabel • ${String.format("%.1fx", playerAnimationSpeed)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = fontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = subtextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Song status strip (respects `hideSongStatus`)
                if (!hideSongStatus) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Playing from Queue",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = fontFamily, fontSize = 9.sp),
                        color = subtextColor,
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Artwork Stage (Modern Ambient vs Vinyl Turntable + Breathing Aura)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(118.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isVinyl) {
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(102.dp)
                                    .offset(x = 24.dp)
                                    .shadow(10.dp, CircleShape, spotColor = Color.Black)
                                    .clip(CircleShape)
                                    .background(Color(0xFF101114)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val center = this.center
                                    val maxR = size.minDimension / 2f
                                    var r = maxR * 0.38f + 4f
                                    while (r < maxR - 6f) {
                                        drawCircle(
                                            color = Color(0x18FFFFFF),
                                            radius = r,
                                            center = center,
                                            style = Stroke(width = 1f),
                                        )
                                        r += 5f
                                    }
                                    drawCircle(
                                        color = Color(0x30FFFFFF),
                                        radius = maxR - 2f,
                                        center = center,
                                        style = Stroke(width = 1.2f),
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(animatedPrimary, activeSecondary))),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (!activeArtworkUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = activeArtworkUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF08090C)),
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(108.dp)
                                    .offset(x = (-22).dp)
                                    .shadow(12.dp, RoundedCornerShape(12.dp), spotColor = Color.Black)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Brush.linearGradient(listOf(animatedPrimary, activeSecondary)))
                                    .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (!activeArtworkUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = activeArtworkUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_logo),
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.90f),
                                        modifier = Modifier.size(38.dp),
                                    )
                                }
                            }
                        }
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            if (enableBreathingAura) {
                                Box(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .blur(22.dp)
                                        .clip(RoundedCornerShape(26.dp))
                                        .background(animatedPrimary.copy(alpha = 0.42f)),
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(108.dp)
                                    .shadow(14.dp, RoundedCornerShape(18.dp), spotColor = Color.Black)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Brush.linearGradient(listOf(animatedPrimary, activeSecondary)))
                                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(18.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (!activeArtworkUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = activeArtworkUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_logo),
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.90f),
                                        modifier = Modifier.size(40.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                // Inline 1-line lyrics preview strip (respects `hideLyricsPreview`)
                if (!hideLyricsPreview) {
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = "♪ Before the moment fades away",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = fontFamily, fontSize = 9.5.sp),
                            color = animatedPrimary.copy(alpha = 0.92f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Metadata Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeTitle ?: "Now Playing",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = fontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = textColor,
                        )
                        Text(
                            text = activeArtist ?: "ChordPlay",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = fontFamily, fontSize = 10.5.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = subtextColor,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = BitChordIcons.HeartFilled,
                            contentDescription = null,
                            tint = animatedPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                        Icon(
                            imageVector = BitChordIcons.PlaylistAdd,
                            contentDescription = null,
                            tint = subtextColor,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Real Seekbar matching `playerWaveStyle` (`OG_WAVE` / `AURORA_SILK` / `AUDIO_GRAPH`)
                when (playerWaveStyle) {
                    PlayerWaveStyle.AUDIO_GRAPH -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val bars = listOf(
                                0.35f, 0.70f, 0.45f, 0.90f, 0.60f, 1.0f, 0.72f, 0.40f,
                                0.80f, 0.92f, 0.65f, 0.82f, 0.50f, 0.38f, 0.68f, 0.48f,
                                0.76f, 0.56f, 0.40f, 0.62f, 0.30f, 0.46f, 0.35f, 0.28f,
                            )
                            bars.forEachIndexed { idx, h ->
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height((16 * h).dp)
                                        .clip(RoundedCornerShape(1.5.dp))
                                        .background(if (idx < 9) animatedPrimary else Color.White.copy(alpha = 0.22f)),
                                )
                            }
                        }
                    }
                    PlayerWaveStyle.AURORA_SILK -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White.copy(alpha = 0.14f)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.42f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Brush.horizontalGradient(listOf(activeSecondary, animatedPrimary, Color.White))),
                            )
                        }
                    }
                    PlayerWaveStyle.OG_WAVE -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(Color.White.copy(alpha = 0.22f)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.42f)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(animatedPrimary),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Technical badge respects `showNerdStats`
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "1:24",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = fontFamily, fontSize = 9.5.sp),
                        color = subtextColor,
                    )
                    if (showNerdStats) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                        ) {
                            Text(
                                text = "FLAC • 24-bit • 48 kHz",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = fontFamily,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = Color.White.copy(alpha = 0.88f),
                            )
                        }
                    }
                    Text(
                        text = "-2:18",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = fontFamily, fontSize = 9.5.sp),
                        color = subtextColor,
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Transport Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Icon(
                        imageVector = BitChordIcons.PlayerShuffle,
                        contentDescription = null,
                        tint = animatedPrimary.copy(alpha = 0.85f),
                        modifier = Modifier.size(17.dp),
                    )
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(22.dp),
                    )
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .shadow(8.dp, CircleShape, spotColor = animatedPrimary)
                            .clip(CircleShape)
                            .background(animatedPrimary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(22.dp),
                    )
                    Icon(
                        imageVector = BitChordIcons.PlayerRepeatOne,
                        contentDescription = null,
                        tint = subtextColor,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }
    }
}
