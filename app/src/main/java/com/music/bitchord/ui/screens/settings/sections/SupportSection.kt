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

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.components.CommunitySupportDialog
import com.music.bitchord.ui.components.GitHubIconBadge
import com.music.bitchord.ui.components.GLASS_EDGE_WIDTH
import com.music.bitchord.ui.components.RedditIconBadge
import com.music.bitchord.ui.components.isGlassSupported
import com.music.bitchord.ui.components.lightweightLiquidGlass
import com.music.bitchord.ui.onboarding.OnboardingState
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.ui.screens.GROUP_INSET
import com.music.bitchord.ui.screens.RowDivider
import com.music.bitchord.ui.screens.SettingsGroup
import com.music.bitchord.ui.screens.SettingsRow
import com.music.bitchord.ui.screens.SettingsSearchEmpty
import com.music.bitchord.ui.screens.SettingsSubPageHeader
import com.music.bitchord.ui.screens.matchesSearch

/**
 * Dedicated sub-page for ChordPlay support, developer identity card,
 * community links, and Extra options (ChordPlay Message and Welcome Setup).
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun SupportSection(
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    initialSearchQuery: String = "",
    onShowUpdate: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var searchQuery by remember(initialSearchQuery) { mutableStateOf(initialSearchQuery) }
    var showCommunityDialog by remember { mutableStateOf(false) }
    val reduceAnimation by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    val disableInfiniteLoops by AppSettings.disableInfiniteLoops.collectAsStateWithLifecycle()
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val glassCard = isGlassSupported() && !reduceDynamicBlur

    val version = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "2.0.4"
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(contentPadding),
    ) {
        SettingsSubPageHeader(
            title = stringResource(R.string.support_community),
            onBack = onBack,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            scrollState = scrollState,
        )

        val showMessage = matchesSearch(
            searchQuery,
            stringResource(R.string.community_hub),
            stringResource(R.string.community_hub_subtitle),
            stringResource(R.string.extra_section),
            "message", "chordplay", "hub", "community",
        )
        val showReddit = matchesSearch(
            searchQuery,
            stringResource(R.string.reddit_community),
            stringResource(R.string.reddit_community_subtitle),
            "reddit", "r/chordplay", "community", "social",
        )
        val showGithub = matchesSearch(
            searchQuery,
            stringResource(R.string.github_repository),
            stringResource(R.string.chordplay_repo_tile_title),
            stringResource(R.string.chordplay_repo_tile_subtitle),
            "github.com/code4nigel/ChordPlay",
            "source", "github", "issues", "bugs", "repo", "repository", "chordplay",
        )
        val showDevProfile = matchesSearch(
            searchQuery,
            stringResource(R.string.developer_profile),
            stringResource(R.string.dev_profile_tile_title),
            "Shivanshu Yadav",
            "github.com/code4nigel",
            "shows dedication", "luck really", "exams",
            "developer", "profile", "author", "shivanshu", "nigel", "code4nigel",
        )
        val showReplaySetup = matchesSearch(
            searchQuery,
            stringResource(R.string.replay_welcome_setup),
            stringResource(R.string.replay_welcome_setup_subtitle),
            stringResource(R.string.extra_section),
            "welcome", "setup", "onboarding", "pass", "wizard", "replay",
        )

        val showHeroCard = searchQuery.isBlank() || showDevProfile || showReddit || showGithub
        val anyExtraMatch = showMessage || showReplaySetup
        val anySupportMatch = showHeroCard || anyExtraMatch

        // Joined Hero Card: Shivanshu Yadav + Quote + 3 compact square tiles (Reddit, ChordPlay Repo, Dev Profile)
        if (showHeroCard) {
            DeveloperHeroCard(
                version = version,
                reducedMotion = reduceAnimation || disableInfiniteLoops,
                useGlass = glassCard,
                showRedditTile = searchQuery.isBlank() || showReddit,
                showRepoTile = searchQuery.isBlank() || showGithub,
                showDevTile = searchQuery.isBlank() || showDevProfile,
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/code4nigel")))
                    }
                },
                onQuoteTap = {
                    Toast.makeText(context, "Shows dedication, but he needs luck really! 🚀", Toast.LENGTH_SHORT).show()
                },
                onRedditClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.reddit.com/r/ChordPlay/")))
                    }
                },
                onRepoClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/code4nigel/ChordPlay")))
                    }
                },
                onDevProfileClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/code4nigel")))
                    }
                },
            )
        }

        if (searchQuery.isNotBlank() && !anySupportMatch) {
            SettingsSearchEmpty(query = searchQuery)
        } else if (anyExtraMatch) {
            // Extra section: ChordPlay Message and Replay Welcome Setup
            SettingsGroup(
                header = stringResource(R.string.extra_section),
                footer = null,
            ) {
                var hasPreceding = false

                if (showMessage) {
                    SettingsRow(
                        leading = {
                            Image(
                                painter = painterResource(R.drawable.ic_chordplay_squircle),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                            )
                        },
                        title = stringResource(R.string.community_hub),
                        subtitle = stringResource(R.string.community_hub_subtitle),
                        trailing = {
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp),
                            )
                        },
                        onClick = { showCommunityDialog = true },
                    )
                    hasPreceding = true
                }

                if (showReplaySetup) {
                    if (hasPreceding) RowDivider()
                    SettingsRow(
                        leading = {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                                    .border(0.75.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                        },
                        title = stringResource(R.string.replay_welcome_setup),
                        subtitle = stringResource(R.string.replay_welcome_setup_subtitle),
                        onClick = { OnboardingState.requestReplay() },
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }

    if (showCommunityDialog) {
        CommunitySupportDialog(
            onDismiss = {
                showCommunityDialog = false
                AppSettings.recordCommunityPopupDismissed()
            },
        )
    }
}

@Composable
private fun DeveloperHeroCard(
    version: String,
    reducedMotion: Boolean,
    useGlass: Boolean,
    showRedditTile: Boolean,
    showRepoTile: Boolean,
    showDevTile: Boolean,
    onClick: () -> Unit,
    onQuoteTap: () -> Unit,
    onRedditClick: () -> Unit,
    onRepoClick: () -> Unit,
    onDevProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardShape = RoundedCornerShape(22.dp)
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = GROUP_INSET, vertical = 8.dp)
            .clip(cardShape)
            .then(
                if (useGlass) {
                    Modifier.lightweightLiquidGlass(
                        shape = cardShape,
                        fallbackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                } else {
                    Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    primary.copy(alpha = 0.14f),
                                    MaterialTheme.colorScheme.surfaceVariant,
                                ),
                            ),
                        )
                        .border(GLASS_EDGE_WIDTH, Color.White.copy(alpha = 0.10f), cardShape)
                },
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            primary.copy(alpha = 0.14f),
                            secondary.copy(alpha = 0.10f),
                            Color.Transparent,
                        ),
                    ),
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                DeveloperAvatarWithFloatingNotes(reducedMotion = reducedMotion)

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Shivanshu Yadav",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(Modifier.height(3.dp))

                    Text(
                        text = "Lead Architect & Developer • ChordPlay v$version",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_github),
                            contentDescription = null,
                            tint = primary,
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "github.com/code4nigel",
                            style = MaterialTheme.typography.labelSmall,
                            color = primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Rounded.OpenInNew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                .clickable(onClick = onQuoteTap)
                .padding(horizontal = 12.dp, vertical = 9.dp),
        ) {
            Text(
                text = "💡 Legend has it Shivanshu spent his time building ChordPlay instead of prepping for exams... shows dedication, but he needs luck really!",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                lineHeight = 16.sp,
            )
        }

        val visibleTiles = (if (showRedditTile) 1 else 0) +
            (if (showRepoTile) 1 else 0) +
            (if (showDevTile) 1 else 0)
        if (visibleTiles > 0) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val tileModifier = if (visibleTiles == 3) {
                    Modifier.weight(1f)
                } else {
                    Modifier.widthIn(max = 112.dp).weight(1f, fill = false)
                }
                if (showRedditTile) {
                    CommunitySquareTile(
                        title = "Reddit",
                        handle = "r/ChordPlay",
                        accent = Color(0xFFFF4500),
                        useGlass = useGlass,
                        onClick = onRedditClick,
                        modifier = tileModifier,
                    ) {
                        RedditIconBadge(size = 32.dp)
                    }
                }
                if (showRepoTile) {
                    CommunitySquareTile(
                        title = stringResource(R.string.chordplay_repo_tile_title),
                        handle = stringResource(R.string.chordplay_repo_tile_subtitle),
                        accent = primary,
                        useGlass = useGlass,
                        onClick = onRepoClick,
                        modifier = tileModifier,
                    ) {
                        GitHubIconBadge(size = 32.dp)
                    }
                }
                if (showDevTile) {
                    CommunitySquareTile(
                        title = stringResource(R.string.dev_profile_tile_title),
                        handle = "code4nigel",
                        accent = secondary,
                        useGlass = useGlass,
                        onClick = onDevProfileClick,
                        modifier = tileModifier,
                    ) {
                        GitHubIconBadge(size = 32.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunitySquareTile(
    title: String,
    handle: String,
    accent: Color,
    useGlass: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: @Composable () -> Unit,
) {
    val tileShape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clip(tileShape)
            .then(
                if (useGlass) {
                    Modifier.lightweightLiquidGlass(
                        shape = tileShape,
                        fallbackColor = accent.copy(alpha = 0.10f),
                    )
                } else {
                    Modifier
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f))
                        .border(0.75.dp, accent.copy(alpha = 0.22f), tileShape)
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        badge()
        Spacer(Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(1.dp))
        Text(
            text = handle,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DeveloperAvatarWithFloatingNotes(
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val bobbingY: Float
    val breathingScale: Float
    val note1Progress: Float
    val note2Progress: Float
    val note3Progress: Float
    val note4Progress: Float

    if (!reducedMotion) {
        val infiniteTransition = rememberInfiniteTransition(label = "DevAvatarAnimation")
        bobbingY = infiniteTransition.animateFloat(
            initialValue = -3.5f,
            targetValue = 3.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(2200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "bobbingY",
        ).value
        breathingScale = infiniteTransition.animateFloat(
            initialValue = 0.98f,
            targetValue = 1.02f,
            animationSpec = infiniteRepeatable(
                animation = tween(2800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "breathingScale",
        ).value
        note1Progress = infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "note1",
        ).value
        note2Progress = infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(3200, delayMillis = 650, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "note2",
        ).value
        note3Progress = infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2900, delayMillis = 1300, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "note3",
        ).value
        note4Progress = infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(3400, delayMillis = 1950, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "note4",
        ).value
    } else {
        bobbingY = 0f
        breathingScale = 1f
        note1Progress = 0.35f
        note2Progress = 0.55f
        note3Progress = 0.45f
        note4Progress = 0.65f
    }

    Box(
        modifier = modifier.size(72.dp),
        contentAlignment = Alignment.Center,
    ) {
        FloatingMusicNote(
            symbol = "♫",
            progress = note1Progress,
            startX = 16f,
            startY = -10f,
            driftX = 14f,
            driftY = -24f,
            color = MaterialTheme.colorScheme.primary,
        )

        FloatingMusicNote(
            symbol = "♪",
            progress = note2Progress,
            startX = -18f,
            startY = -8f,
            driftX = -12f,
            driftY = -26f,
            color = Color(0xFF8B5CF6),
        )

        FloatingMusicNote(
            symbol = "♬",
            progress = note3Progress,
            startX = -20f,
            startY = 12f,
            driftX = -10f,
            driftY = -22f,
            color = MaterialTheme.colorScheme.secondary,
        )

        FloatingMusicNote(
            symbol = "♪",
            progress = note4Progress,
            startX = 18f,
            startY = 10f,
            driftX = 12f,
            driftY = -20f,
            color = Color(0xFF38BDF8),
        )

        Box(
            modifier = Modifier
                .size(54.dp)
                .graphicsLayer {
                    translationY = bobbingY * density
                    scaleX = breathingScale * 1.05f
                    scaleY = breathingScale * 1.05f
                }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
        )

        Image(
            painter = painterResource(R.drawable.dev_avatar),
            contentDescription = stringResource(R.string.developer_avatar),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(52.dp)
                .graphicsLayer {
                    translationY = bobbingY * density
                    scaleX = breathingScale
                    scaleY = breathingScale
                }
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape),
        )
    }
}

@Composable
private fun FloatingMusicNote(
    symbol: String,
    progress: Float,
    startX: Float,
    startY: Float,
    driftX: Float,
    driftY: Float,
    color: Color,
) {
    val alpha = when {
        progress < 0.22f -> (progress / 0.22f)
        progress > 0.68f -> (1f - (progress - 0.68f) / 0.32f).coerceIn(0f, 1f)
        else -> 1f
    }
    val currentX = startX + (driftX * progress)
    val currentY = startY + (driftY * progress)
    val scale = 0.75f + (0.45f * progress)
    val rotation = (progress * 30f) - 15f

    Text(
        text = symbol,
        color = color.copy(alpha = alpha * 0.95f),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.graphicsLayer {
            translationX = currentX * density
            translationY = currentY * density
            scaleX = scale
            scaleY = scale
            rotationZ = rotation
            this.alpha = alpha
        },
    )
}

