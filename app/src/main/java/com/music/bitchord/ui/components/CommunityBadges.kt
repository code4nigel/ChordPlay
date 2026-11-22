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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.music.bitchord.R

/** Glossy gradient badge icon for GitHub links (shared by Welcome & Settings). */
@Composable
fun GitHubIconBadge(
    size: Dp = 44.dp,
    modifier: Modifier = Modifier,
) {
    val badgeShape = RoundedCornerShape(size * 0.32f)
    Box(
        modifier = modifier
            .size(size)
            .clip(badgeShape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF343B4E), Color(0xFF1F2430)),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.26f), badgeShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_github),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.57f),
        )
    }
}

/** Glossy gradient badge icon for Reddit links (shared by Welcome & Settings). */
@Composable
fun RedditIconBadge(
    size: Dp = 44.dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFFF6B35), Color(0xFFFF4500)),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.26f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_reddit),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.57f),
        )
    }
}
