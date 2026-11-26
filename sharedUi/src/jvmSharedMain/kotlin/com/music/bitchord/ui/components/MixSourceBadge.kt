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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.spotify.SPOTIFY_PAGE_PREFIX
import com.music.bitchord.ui.graphics.ColorUtils
import com.music.bitchord.ui.theme.rememberArtworkDominant

enum class MixBadgeSource {
    SPOTIFY,
    YT_MUSIC,
}

fun mixBadgeSourceOf(item: ShelfItem): MixBadgeSource =
    if (item.browseId?.startsWith(SPOTIFY_PAGE_PREFIX) == true) {
        MixBadgeSource.SPOTIFY
    } else {
        MixBadgeSource.YT_MUSIC
    }

/**
 * Derives a rich dark ground from [seedArgb] clamped to guarantee WCAG AA contrast (>= 4.5:1)
 * behind a white source glyph.
 */
internal fun computeMixBadgeGroundArgb(seedArgb: Int): Int {
    val hsl = FloatArray(3).also { ColorUtils.colorToHSL(seedArgb, it) }
    hsl[1] = hsl[1].coerceIn(0f, MAX_BADGE_SATURATION)
    hsl[2] = hsl[2].coerceIn(MIN_BADGE_LIGHTNESS, MAX_BADGE_LIGHTNESS)
    var ground = ColorUtils.HSLToColor(hsl)
    while (whiteContrastRatio(ground) < MIN_BADGE_CONTRAST && hsl[2] > MIN_BADGE_LIGHTNESS) {
        hsl[2] = (hsl[2] - 0.02f).coerceAtLeast(MIN_BADGE_LIGHTNESS)
        ground = ColorUtils.HSLToColor(hsl)
    }
    return ground or (0xFF shl 24)
}

internal fun whiteContrastRatio(argb: Int): Double =
    1.05 / (ColorUtils.calculateLuminance(argb) + 0.05)

/**
 * Artwork-tinted top-start corner badge identifying whether a Mixes Dial tile comes from
 * Spotify or YouTube Music.
 */
@Composable
fun MixSourceBadge(
    source: MixBadgeSource,
    artworkUrl: String?,
    modifier: Modifier = Modifier,
) {
    val dominant = rememberArtworkDominant(artworkUrl)
    val ground by animateColorAsState(
        targetValue = dominant?.let { Color(computeMixBadgeGroundArgb(it.toArgb())) } ?: UNREAD_BADGE_GROUND,
        animationSpec = tween(BADGE_FADE_MS),
        label = "mixBadgeGround",
    )
    Canvas(
        modifier = modifier
            .size(BADGE_SIZE)
            .clip(RoundedCornerShape(bottomEnd = 10.dp))
            .background(ground),
    ) {
        when (source) {
            MixBadgeSource.SPOTIFY -> drawSpotifyMark()
            MixBadgeSource.YT_MUSIC -> drawYtMusicMark()
        }
    }
}

private val BADGE_SIZE = 24.dp
private val UNREAD_BADGE_GROUND = Color(0xFF22232A)
private const val BADGE_FADE_MS = 240
private const val MAX_BADGE_SATURATION = 0.6f
private const val MIN_BADGE_LIGHTNESS = 0.16f
private const val MAX_BADGE_LIGHTNESS = 0.30f
private const val MIN_BADGE_CONTRAST = 4.5
private const val GLYPH_GRID = 24f

private fun DrawScope.drawSpotifyMark() {
    val u = size.minDimension / GLYPH_GRID
    fun arc(fromX: Float, fromY: Float, bowY: Float, toX: Float, toY: Float, width: Float) {
        val path = Path().apply {
            moveTo(fromX * u, fromY * u)
            quadraticTo(12f * u, bowY * u, toX * u, toY * u)
        }
        drawPath(path, Color.White, style = Stroke(width = width * u, cap = StrokeCap.Round))
    }
    arc(5.4f, 9.0f, 5.6f, 18.6f, 10.2f, 2.3f)
    arc(6.3f, 12.7f, 9.9f, 17.4f, 13.7f, 2.0f)
    arc(7.2f, 16.1f, 13.9f, 16.2f, 16.9f, 1.7f)
}

private fun DrawScope.drawYtMusicMark() {
    val u = size.minDimension / GLYPH_GRID
    val center = Offset(size.width / 2f, size.height / 2f)
    drawCircle(Color.White, radius = 6.6f * u, center = center, style = Stroke(width = 1.5f * u))
    val triangle = Path().apply {
        moveTo(10.1f * u, 8.6f * u)
        lineTo(15.4f * u, 12f * u)
        lineTo(10.1f * u, 15.4f * u)
        close()
    }
    drawPath(triangle, Color.White)
}
